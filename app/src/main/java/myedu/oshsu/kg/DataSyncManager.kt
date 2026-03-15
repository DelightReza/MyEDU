package myedu.oshsu.kg

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import myedu.oshsu.kg.database.MyEduRepository
import java.io.File
import java.util.Calendar
import java.util.Locale

data class OfflineData(
    val userData: UserData?,
    val profileData: StudentInfoResponse?,
    val payStatus: PayStatusResponse?,
    val newsList: List<NewsItem>,
    val fullSchedule: List<ScheduleItem>,
    val timeMap: Map<Int, String>,
    val sessionData: List<SessionResponse>,
    val transcriptData: List<TranscriptYear>
)

class DataSyncManager(private val prefs: PrefsManager) {

    private val repository: MyEduRepository get() = prefs.getRepository()

    // --- OFFLINE DATA LOADING (Room-primary) ---
    suspend fun loadOfflineData(): OfflineData = withContext(Dispatchers.IO) {
        try {
            val roomUserData = repository.getUserDataSync()
            val roomProfileData = repository.getProfileDataSync()
            val roomPayStatus = repository.getPayStatusSync()
            val roomNews = repository.getAllNewsSync()
            val roomSchedule = repository.getAllSchedulesSync()
            val roomTimeMap = repository.getTimeMapSync()
            val roomGrades = repository.getAllGradesSync()
            val roomTranscript = repository.getTranscriptSync()

            OfflineData(
                userData = roomUserData,
                profileData = roomProfileData,
                payStatus = roomPayStatus,
                newsList = roomNews,
                fullSchedule = roomSchedule,
                timeMap = roomTimeMap,
                sessionData = roomGrades,
                transcriptData = roomTranscript
            )
        } catch (e: Exception) {
            DebugLogger.log("DATA", "Error loading from Room: ${e.message}")
            OfflineData(null, null, null, emptyList(), emptyList(), emptyMap(), emptyList(), emptyList())
        }
    }

    // --- SILENT LOGIN ---
    suspend fun performSilentLogin(email: String, pass: String): Boolean {
        if (email.isBlank() || pass.isBlank()) return false
        DebugLogger.log("AUTH", "Silent login attempt...")
        return try {
            val resp = NetworkClient.api.login(LoginRequest(email.trim(), pass.trim()))
            val token = resp.authorisation?.token
            if (token != null) {
                prefs.saveToken(token)
                NetworkClient.interceptor.authToken = token
                NetworkClient.cookieJar.injectSessionCookies(token)
                true
            } else false
        } catch (e: Exception) {
            DebugLogger.log("AUTH", "Silent login failed: ${e.message}")
            false
        }
    }

    // --- FULL DATA REFRESH ---
    data class RefreshResult(
        val userData: UserData?,
        val profileData: StudentInfoResponse?,
        val verify2FA: Verify2FAResponse?,
        val newsList: List<NewsItem>?,
        val payStatus: PayStatusResponse?,
        val isAuthError: Boolean
    )

    suspend fun refreshCoreData(): RefreshResult = withContext(Dispatchers.IO) {
        try {
            val user = NetworkClient.api.getUser().user
            val profile = NetworkClient.api.getProfile()

            // Save to Room
            if (user != null) repository.updateUserData(user)
            repository.updateProfileData(profile)

            // 2FA
            val v2fa = try { NetworkClient.api.verify2FA() } catch (e: Exception) { null }

            // News
            val news = try {
                val n = NetworkClient.api.getNews()
                repository.updateNews(n)
                n
            } catch (e: Exception) { null }

            // Pay status
            val pay = try {
                val p = NetworkClient.api.getPayStatus()
                repository.updatePayStatus(p)
                p
            } catch (e: Exception) { null }

            RefreshResult(user, profile, v2fa, news, pay, isAuthError = false)
        } catch (e: Exception) {
            val isAuthError = e.message?.contains("401") == true || e.message?.contains("Unauthenticated") == true
            if (isAuthError) {
                throw e
            }
            DebugLogger.log("DATA", "Network unavailable, loading cached core data")
            val cachedUser = repository.getUserDataSync()
            val cachedProfile = repository.getProfileDataSync()
            val cachedPay = repository.getPayStatusSync()
            val cachedNews = repository.getAllNewsSync()
            RefreshResult(cachedUser, cachedProfile, null, cachedNews, cachedPay, isAuthError = false)
        }
    }

    // --- SCHEDULE ---
    data class ScheduleResult(val schedule: List<ScheduleItem>, val timeMap: Map<Int, String>)

    suspend fun loadScheduleNetwork(profile: StudentInfoResponse): ScheduleResult? = withContext(Dispatchers.IO) {
        val mov = profile.studentMovement ?: return@withContext null
        try {
            val years = NetworkClient.api.getYears()
            val activeYearId = years.find { it.active }?.id ?: AcademicYearHelper.getDefaultActiveYearId()
            val times = try {
                NetworkClient.api.getLessonTimes(mov.id_speciality!!, mov.id_edu_form!!, activeYearId)
            } catch (e: Exception) { emptyList() }
            val wrappers = NetworkClient.api.getSchedule(mov.id_speciality!!, mov.id_edu_form!!, activeYearId, profile.active_semester ?: 1)
            val fullSchedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }
            val timeMap = times.associate { it.id_lesson to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }

            // Save to Room
            repository.updateSchedules(fullSchedule)
            if (timeMap.isNotEmpty()) repository.updateTimeMap(timeMap)

            ScheduleResult(fullSchedule, timeMap)
        } catch (e: Exception) { null }
    }

    // --- SESSION/GRADES ---
    suspend fun fetchSession(profile: StudentInfoResponse, appContext: android.content.Context?): List<SessionResponse> = withContext(Dispatchers.IO) {
        val oldSession = repository.getAllGradesSync()
        try {
            val session = NetworkClient.api.getSession(profile.active_semester ?: 1)

            // Check for updates and send notifications
            if (oldSession.isNotEmpty() && session.isNotEmpty() && appContext != null) {
                val localizedContext = NotificationHelper.getLocalizedContext(appContext, prefs)
                val (gradeUpdates, portalUpdates) = NotificationHelper.checkForUpdates(oldSession, session, localizedContext)
                if (gradeUpdates.isNotEmpty()) NotificationHelper.sendNotification(localizedContext, gradeUpdates, isPortalOpening = false)
                if (portalUpdates.isNotEmpty()) NotificationHelper.sendNotification(localizedContext, portalUpdates, isPortalOpening = true)
            }

            // Save ALL sessions to Room
            repository.updateGrades(session)

            session
        } catch (e: Exception) {
            DebugLogger.log("DATA", "Network unavailable for session, using cached grades")
            oldSession
        }
    }

    // --- TRANSCRIPT ---
    suspend fun fetchTranscript(uid: Long, movId: Long): List<TranscriptYear> = withContext(Dispatchers.IO) {
        try {
            val transcript = NetworkClient.api.getTranscript(uid, movId)
            repository.updateTranscript(transcript)
            transcript
        } catch (e: Exception) {
            DebugLogger.log("DATA", "Network unavailable for transcript, using cached data")
            repository.getTranscriptSync()
        }
    }

    // --- JOURNAL ---
    suspend fun fetchJournal(
        curriculaId: Int,
        semesterId: Int,
        subjectType: Int,
        activeSemester: Int
    ): List<JournalItem> = withContext(Dispatchers.IO) {
        val currentActiveYear = AcademicYearHelper.getDefaultActiveYearId()
        val semesterDiff = activeSemester - semesterId
        val yearOffset = semesterDiff / 2
        val eduYearId = currentActiveYear - yearOffset

        DebugLogger.log("JOURNAL", "Fetching journal: curricula=$curriculaId, semester=$semesterId, type=$subjectType, year=$eduYearId (active=$activeSemester, offset=$yearOffset)")

        try {
            val journal = NetworkClient.api.getJournal(
                idCurricula = curriculaId,
                idSemester = semesterId,
                idSubjectType = subjectType,
                idEduYear = eduYearId
            )

            DebugLogger.log("JOURNAL", "Received ${journal.size} journal entries")

            // Save to Room
            try {
                repository.updateJournalEntries(curriculaId, semesterId, subjectType, eduYearId, journal)
                DebugLogger.log("JOURNAL", "Saved journal entries to cache")
            } catch (e: Exception) {
                DebugLogger.log("JOURNAL", "Failed to save journal to cache: ${e.message}")
            }

            journal
        } catch (e: Exception) {
            DebugLogger.log("JOURNAL", "Network unavailable for journal, using cached data")
            try {
                repository.getJournalEntriesSync(curriculaId, semesterId, subjectType, eduYearId)
            } catch (cacheEx: Exception) {
                DebugLogger.log("JOURNAL", "Failed to load cached journal: ${cacheEx.message}")
                emptyList()
            }
        }
    }

    suspend fun loadCachedJournal(
        curriculaId: Int,
        semesterId: Int,
        subjectType: Int,
        activeSemester: Int
    ): List<JournalItem> = withContext(Dispatchers.IO) {
        val currentActiveYear = AcademicYearHelper.getDefaultActiveYearId()
        val semesterDiff = activeSemester - semesterId
        val yearOffset = semesterDiff / 2
        val eduYearId = currentActiveYear - yearOffset

        try {
            repository.getJournalEntriesSync(curriculaId, semesterId, subjectType, eduYearId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- TUITION ---
    suspend fun fetchTuitionDetails(): List<PaymentDetail> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.api.getStudentPrice()
            response.flatMap { it.payment ?: emptyList() }.sortedByDescending { it.id_semester ?: 0 }
        } catch (e: Exception) {
            DebugLogger.log("TUITION", "Network unavailable for tuition details: ${e.message}")
            emptyList()
        }
    }

    // --- FRESH PERSONAL INFO ---
    suspend fun getFreshPersonalInfo(retryLogin: suspend () -> Boolean): Pair<UserData?, StudentInfoResponse?> = withContext(Dispatchers.IO) {
        IdDefinitions.loadAll()
        try {
            val u = NetworkClient.api.getUser().user
            val p = NetworkClient.api.getProfile()
            if (u != null) repository.updateUserData(u)
            repository.updateProfileData(p)
            Pair(u, p)
        } catch (e: Exception) {
            val isAuthError = e.message?.contains("401") == true || e.message?.contains("Unauthenticated") == true
            if (isAuthError && retryLogin()) {
                try {
                    val u = NetworkClient.api.getUser().user
                    val p = NetworkClient.api.getProfile()
                    if (u != null) repository.updateUserData(u)
                    repository.updateProfileData(p)
                    Pair(u, p)
                } catch (retryEx: Exception) {
                    fallbackToOfflinePersonalInfo(retryEx)
                }
            } else {
                fallbackToOfflinePersonalInfo(e)
            }
        }
    }

    private suspend fun fallbackToOfflinePersonalInfo(cause: Exception): Pair<UserData?, StudentInfoResponse?> {
        val cachedUser = repository.getUserDataSync()
        val cachedProfile = repository.getProfileDataSync()
        if (cachedUser != null || cachedProfile != null) {
            DebugLogger.log("DATA", "Using cached personal info (offline fallback)")
            return Pair(cachedUser, cachedProfile)
        }
        throw cause
    }

    // --- SCHEDULE LOCAL PROCESSING ---
    data class ScheduleLocalResult(
        val determinedStream: Int?,
        val determinedGroup: Int?,
        val todayDayName: String,
        val todayClasses: List<ScheduleItem>
    )

    fun processScheduleLocally(fullSchedule: List<ScheduleItem>, language: String, appContext: android.content.Context?): ScheduleLocalResult {
        if (fullSchedule.isEmpty()) return ScheduleLocalResult(null, null, "", emptyList())

        val determinedStream = fullSchedule.asSequence()
            .filter { it.subject_type?.name_en?.contains(AppConstants.SUBJECT_LECTION_EN, ignoreCase = true) == true }
            .mapNotNull { it.stream?.numeric }
            .firstOrNull()
        val determinedGroup = fullSchedule.asSequence()
            .filter { it.subject_type?.name_en?.contains(AppConstants.SUBJECT_PRACTICAL_EN, ignoreCase = true) == true }
            .mapNotNull { it.stream?.numeric }
            .firstOrNull()

        val cal = Calendar.getInstance()
        val loc = Locale(language)
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        if (currentHour >= 20) cal.add(Calendar.DAY_OF_YEAR, 1)
        var dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, loc) ?: appContext?.getString(R.string.today) ?: "Today"
        if (dayName.isNotEmpty()) dayName = dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(loc) else it.toString() }

        val todayClasses = myedu.oshsu.kg.widget.WidgetHelper.getTodayClasses(fullSchedule)

        return ScheduleLocalResult(determinedStream, determinedGroup, dayName, todayClasses)
    }

    // --- AVATAR CACHING ---
    suspend fun cacheAvatarImage(context: Context, avatarUrl: String?) {
        if (avatarUrl.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder().url(avatarUrl).build()
                NetworkClient.imageClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { input ->
                            File(context.filesDir, AppConstants.AVATAR_CACHE_FILENAME).outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                DebugLogger.log("DATA", "Avatar cache failed: ${e.message}")
            }
        }
    }
}
