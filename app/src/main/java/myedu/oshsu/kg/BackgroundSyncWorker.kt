package myedu.oshsu.kg

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class BackgroundSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val accountManager = AccountManager(context)
        val prefs = PrefsManager(context, accountManager.getActiveAccountId() ?: "default")

        val accounts = accountManager.getAllAccounts()

        if (accounts.isEmpty()) {
            // Legacy path: no saved accounts yet — sync the single active token as before
            val success = runSyncForActiveAccount(prefs, retryAuth = true)
            return@withContext if (success) Result.success() else Result.retry()
        }

        // Multi-account path: sync every account sequentially
        var atLeastOneSuccess = false
        val activeAccountId = accountManager.getActiveAccountId()

        for ((accountIndex, account) in accounts.withIndex()) {
            val accountPrefs = accountManager.getAccountPrefs(account.id)
            val isActive = account.id == activeAccountId
            val (success, freshToken) = syncAccount(
                context = context,
                account = account,
                accountPrefs = accountPrefs,
                mainPrefs = if (isActive) prefs else null
            )
            if (success) atLeastOneSuccess = true

            // Update cached token in AccountManager if re-login refreshed it
            if (freshToken != null && freshToken != account.token) {
                accountManager.saveOrUpdateAccount(account.copy(token = freshToken))
                if (isActive) prefs.saveToken(freshToken)
            }
        }

        // Restore the active account's credentials in the shared NetworkClient
        val activeAccount = accountManager.getActiveAccount()
        if (activeAccount != null) {
            val activeToken = activeAccount.token ?: prefs.getToken()
            if (activeToken != null) {
                NetworkClient.interceptor.authToken = activeToken
                NetworkClient.cookieJar.injectSessionCookies(activeToken)
            }
        }

        if (atLeastOneSuccess) Result.success() else Result.retry()
    }

    // ── Single-account sync (used when no AccountManager accounts exist) ──────

    private suspend fun runSyncForActiveAccount(prefs: PrefsManager, retryAuth: Boolean): Boolean {
        try {
            val context = applicationContext
            var token = prefs.getToken()

            if (token == null && retryAuth) {
                if (attemptLoginWithPrefs(prefs)) token = prefs.getToken() else return false
            } else if (token == null) return false

            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token!!)

            val userResponse = try { NetworkClient.api.getUser() } catch (e: Exception) {
                if (retryAuth && e.message?.contains("401") == true) {
                    if (attemptLoginWithPrefs(prefs)) return runSyncForActiveAccount(prefs, retryAuth = false)
                }
                return false
            }
            val profile = try { NetworkClient.api.getProfile() } catch (e: Exception) { return false }

            prefs.saveData("user_data", userResponse.user)
            prefs.saveData("profile_data", profile)
            if (userResponse.user != null) prefs.getRepository().updateUserData(userResponse.user)
            prefs.getRepository().updateProfileData(profile)

            try { val news = NetworkClient.api.getNews(); prefs.saveList("news_list", news); prefs.getRepository().updateNews(news) } catch (_: Exception) {}
            try { val pay = NetworkClient.api.getPayStatus(); prefs.saveData("pay_status", pay); prefs.getRepository().updatePayStatus(pay) } catch (_: Exception) {}

            try {
                val oldSession = prefs.loadList<SessionResponse>("session_list")
                val activeSemester = profile.active_semester ?: 1
                val newSession = NetworkClient.api.getSession(activeSemester)
                if (oldSession.isNotEmpty() && newSession.isNotEmpty()) {
                    val lCtx = NotificationHelper.getLocalizedContext(context, prefs)
                    val (grades, portals) = NotificationHelper.checkForUpdates(oldSession, newSession, lCtx)
                    if (grades.isNotEmpty()) NotificationHelper.sendNotification(lCtx, grades, isPortalOpening = false)
                    if (portals.isNotEmpty()) NotificationHelper.sendNotification(lCtx, portals, isPortalOpening = true)
                }
                prefs.saveList("session_list", newSession)
                if (newSession.isNotEmpty()) prefs.getRepository().updateGrades(newSession.first())
            } catch (e: Exception) { e.printStackTrace() }

            syncSchedule(context, profile, prefs)
            return true
        } catch (e: Exception) { return false }
    }

    // ── Per-account sync ──────────────────────────────────────────────────────

    /**
     * Syncs a single [account]. Uses [accountPrefs] for per-account session
     * comparison. Writes full data (schedule, Room) only when [mainPrefs] is
     * non-null (i.e. this is the currently active account).
     */
    private suspend fun syncAccount(
        context: Context,
        account: SavedAccount,
        accountPrefs: SharedPreferences,
        mainPrefs: PrefsManager?
    ): Pair<Boolean, String?> {
        try {
            var token = account.token

            // Try the cached token first; re-login if it is missing or stale
            if (token != null) {
                NetworkClient.interceptor.authToken = token
                NetworkClient.cookieJar.injectSessionCookies(token)
            }

            val userResponse = try {
                if (token == null) throw Exception("no token")
                NetworkClient.api.getUser()
            } catch (e: Exception) {
                val is401 = e.message?.contains("401") == true || token == null
                if (is401) {
                    val newToken = attemptLoginWithCredentials(account.email, account.password)
                        ?: return Pair(false, null)
                    token = newToken
                } else {
                    return Pair(false, null)
                }
                NetworkClient.api.getUser()
            }

            val profile = try { NetworkClient.api.getProfile() } catch (e: Exception) { return Pair(false, null) }

            // If this is the active account, keep Room + main prefs up to date
            if (mainPrefs != null) {
                mainPrefs.saveData("user_data", userResponse.user)
                mainPrefs.saveData("profile_data", profile)
                if (userResponse.user != null) mainPrefs.getRepository().updateUserData(userResponse.user)
                mainPrefs.getRepository().updateProfileData(profile)
                try { val news = NetworkClient.api.getNews(); mainPrefs.saveList("news_list", news); mainPrefs.getRepository().updateNews(news) } catch (_: Exception) {}
                try { val pay = NetworkClient.api.getPayStatus(); mainPrefs.saveData("pay_status", pay); mainPrefs.getRepository().updatePayStatus(pay) } catch (_: Exception) {}
            } else {
                // Non-active account: persist user + profile to per-account prefs so the
                // account is available offline when the user switches to it later.
                val gson = Gson()
                accountPrefs.edit()
                    .putString("user_data", gson.toJson(userResponse.user))
                    .putString("profile_data", gson.toJson(profile))
                    .apply()
            }

            // Grade/portal change detection — per-account cache
            try {
                val gson = Gson()
                val sessionType = object : TypeToken<List<SessionResponse>>() {}.type
                val oldSessionJson = accountPrefs.getString("session_list", null)
                val oldSession: List<SessionResponse> = if (oldSessionJson != null) {
                    try { gson.fromJson(oldSessionJson, sessionType) } catch (_: Exception) { emptyList() }
                } else emptyList()

                val activeSemester = profile.active_semester ?: 1
                val newSession = NetworkClient.api.getSession(activeSemester)

                if (oldSession.isNotEmpty() && newSession.isNotEmpty()) {
                    val lCtx = NotificationHelper.getLocalizedContext(context, mainPrefs ?: PrefsManager(context, account.id))
                    val studentName = account.displayName
                    val (grades, portals) = NotificationHelper.checkForUpdates(oldSession, newSession, lCtx)
                    // Use a stable, per-account notification ID derived from the account's
                    // unique ID so IDs never shift when the account list order changes.
                    // Mask to 24 bits (max 16 777 215) so gradeNotifId never approaches
                    // Integer.MAX_VALUE even after multiplying by 2 and adding the base.
                    val stableAccountSlot = account.id.hashCode() and 0x00FFFFFF
                    val gradeNotifId  = 10000 + stableAccountSlot * 2
                    val portalNotifId = gradeNotifId + 1
                    if (grades.isNotEmpty()) NotificationHelper.sendNotification(
                        lCtx, grades,
                        isPortalOpening = false,
                        studentName = studentName,
                        notificationId = gradeNotifId
                    )
                    if (portals.isNotEmpty()) NotificationHelper.sendNotification(
                        lCtx, portals,
                        isPortalOpening = true,
                        studentName = studentName,
                        notificationId = portalNotifId
                    )
                }

                // Persist new session for next comparison
                accountPrefs.edit().putString("session_list", gson.toJson(newSession)).apply()

                // If active account, keep main prefs + Room in sync too
                if (mainPrefs != null) {
                    mainPrefs.saveList("session_list", newSession)
                    if (newSession.isNotEmpty()) mainPrefs.getRepository().updateGrades(newSession.first())
                }
            } catch (e: Exception) { e.printStackTrace() }

            // Schedule sync — only for active account (affects alarms and widget)
            if (mainPrefs != null) {
                syncSchedule(context, profile, mainPrefs)
            }

            return Pair(true, token)
        } catch (e: Exception) {
            return Pair(false, null)
        }
    }

    // ── Schedule sync (active account only) ──────────────────────────────────

    private suspend fun syncSchedule(context: Context, profile: StudentInfoResponse, prefs: PrefsManager) {
        val mov = profile.studentMovement ?: return
        try {
            val years = NetworkClient.api.getYears()
            val activeYearId = years.find { it.active }?.id ?: AcademicYearHelper.getDefaultActiveYearId()
            val times = try { NetworkClient.api.getLessonTimes(mov.id_speciality!!, mov.id_edu_form!!, activeYearId) } catch (_: Exception) { emptyList() }
            val wrappers = NetworkClient.api.getSchedule(mov.id_speciality!!, mov.id_edu_form!!, activeYearId, profile.active_semester ?: 1)
            val fullSchedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }

            prefs.saveList("schedule_list", fullSchedule)
            prefs.getRepository().updateSchedules(fullSchedule)

            val lCtx = NotificationHelper.getLocalizedContext(context, prefs)
            if (fullSchedule.isNotEmpty() && times.isNotEmpty()) {
                val timeMap = times.associate { it.id_lesson to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                prefs.saveData("time_map", timeMap)
                prefs.getRepository().updateTimeMap(timeMap)
                ScheduleAlarmManager(lCtx).scheduleNotifications(
                    fullSchedule, timeMap,
                    prefs.loadData("language_pref", String::class.java)?.replace("\"", "") ?: "en"
                )
            } else {
                ScheduleAlarmManager(lCtx).cancelAll()
            }

            try { myedu.oshsu.kg.widget.ScheduleWidgetUpdater.updateWidget(context) } catch (_: Exception) {}
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    private suspend fun attemptLoginWithPrefs(prefs: PrefsManager): Boolean {
        val isRemember = prefs.loadData("pref_remember_me", Boolean::class.java) ?: false
        if (!isRemember) return false
        val email = prefs.loadData("pref_saved_email", String::class.java) ?: ""
        val pass = prefs.loadData("pref_saved_pass", String::class.java) ?: ""
        if (email.isBlank() || pass.isBlank()) return false
        val newToken = attemptLoginWithCredentials(email.trim(), pass.trim()) ?: return false
        prefs.saveToken(newToken)
        return true
    }

    private suspend fun attemptLoginWithCredentials(email: String, password: String): String? {
        if (email.isBlank() || password.isBlank()) return null
        return try {
            val resp = NetworkClient.api.login(LoginRequest(email, password))
            val token = resp.authorisation?.token
            if (token != null) {
                NetworkClient.interceptor.authToken = token
                NetworkClient.cookieJar.injectSessionCookies(token)
            }
            token
        } catch (e: Exception) { null }
    }
}

