package myedu.oshsu.kg

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class SortOption { DEFAULT, ALPHABETICAL, UPDATED_TIME, LOWEST_FIRST, HIGHEST_FIRST }

class MainViewModel : ViewModel() {
    var appState by mutableStateOf("STARTUP")
    var currentTab by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    
    var loginEmail by mutableStateOf("")
    var loginPass by mutableStateOf("")
    var rememberMe by mutableStateOf(false)

    // --- NAVIGATION STATES ---
    var showPersonalInfoScreen by mutableStateOf(false)
    var showEditProfileScreen by mutableStateOf(false)
    var showTranscriptScreen by mutableStateOf(false)
    var showReferenceScreen by mutableStateOf(false)
    var showSettingsScreen by mutableStateOf(false)
    var showDictionaryScreen by mutableStateOf(false)
    var selectedClass by mutableStateOf<ScheduleItem?>(null)
    var webDocumentUrl by mutableStateOf<String?>(null)
    var addWidgetRequestPending by mutableStateOf(false)

    // --- REFRESH LOGIC ---
    private var lastRefreshTime: Long = 0
    private val refreshCooldownMs = TimeUnit.MINUTES.toMillis(5)

    // --- THEME ---
    var themeMode by mutableStateOf("SYSTEM")
    
    // Computed property: glassmorphism is enabled when theme is GLASS or GLASS_DARK
    val glassmorphismEnabled: Boolean
        get() = themeMode == "GLASS" || themeMode == "GLASS_DARK"

    // --- SETTINGS ---
    var downloadMode by mutableStateOf("IN_APP") 
    var language by mutableStateOf("en") 
    
    // --- DEBUG ---
    var isDebugPipVisible by mutableStateOf(false)
    var isDebugConsoleOpen by mutableStateOf(false)

    // --- DATA ---
    var userData by mutableStateOf<UserData?>(null)
    var profileData by mutableStateOf<StudentInfoResponse?>(null)
    var payStatus by mutableStateOf<PayStatusResponse?>(null)
    var newsList by mutableStateOf<List<NewsItem>>(emptyList())
    var verify2FAStatus by mutableStateOf<Verify2FAResponse?>(null)

    // --- LOCAL EDIT STATE ---
    var customName by mutableStateOf<String?>(null)
    var customPhotoUri by mutableStateOf<String?>(null)
    var avatarRefreshTrigger by mutableStateOf(0)
    var areDictionariesLoaded by mutableStateOf(false)

    // --- SCHEDULE ---
    var fullSchedule by mutableStateOf<List<ScheduleItem>>(emptyList())
    var todayClasses by mutableStateOf<List<ScheduleItem>>(emptyList())
    var timeMap by mutableStateOf<Map<Int, String>>(emptyMap())
    var todayDayName by mutableStateOf("")
    var determinedStream by mutableStateOf<Int?>(null)
    var determinedGroup by mutableStateOf<Int?>(null)
    
    var selectedScheduleDay by mutableStateOf(run {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) 0 else (dow - 2).coerceIn(0, 5)
    })
    
    // --- GRADES ---
    var sessionData by mutableStateOf<List<SessionResponse>>(emptyList())
    var isGradesLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var gradesSortOption by mutableStateOf(SortOption.DEFAULT)
    var selectedSemesterId by mutableStateOf<Int?>(null)
    
    // --- JOURNAL ---
    var showJournalSheet by mutableStateOf(false)
    var journalList by mutableStateOf<List<JournalItem>>(emptyList())
    var isJournalLoading by mutableStateOf(false)
    var selectedJournalSubject by mutableStateOf<SessionSubjectWrapper?>(null)
    var selectedJournalType by mutableStateOf(1) // 1=Lecture, 2=Practice, 3=Lab
    
    // --- DOCS UI ---
    var transcriptData by mutableStateOf<List<TranscriptYear>>(emptyList())
    var isTranscriptLoading by mutableStateOf(false)
    
    // --- DICTIONARY UI ---
    var dictionaryMap by mutableStateOf<Map<String, String>>(emptyMap())
    
    // --- PDF ---
    var isPdfGenerating by mutableStateOf(false)
    var pdfStatusMessage by mutableStateOf<String?>(null)
    var generatedPdfUri by mutableStateOf<Uri?>(null) // Track finished PDF
    private var pdfGenerationJob: Job? = null
    
    // --- UPDATE ---
    var updateAvailableRelease by mutableStateOf<GitHubRelease?>(null)
    var downloadId by mutableStateOf<Long?>(null)
    var isUpdateDownloading by mutableStateOf(false)
    var updateProgress by mutableStateOf(0f)
    var isUpdateReady by mutableStateOf(false)
    var updateLocalUri by mutableStateOf<Uri?>(null)
    private var updateDownloadJob: Job? = null

    private var prefs: PrefsManager? = null
    private var jsFetcher: JsResourceFetcher? = null
    private var refFetcher: ReferenceJsFetcher? = null
    private val dictUtils = DictionaryUtils()
    
    // Public methods to access widget promotion preference
    fun loadShowWidgetPromotion(): Boolean = prefs?.loadData("show_widget_promotion", Boolean::class.java) ?: true
    fun saveShowWidgetPromotion(value: Boolean) { prefs?.saveData("show_widget_promotion", value) }
    
    private var cachedResourcesRu: PdfResources? = null
    private var cachedResourcesEn: PdfResources? = null
    private var cachedRefResourcesRu: ReferenceResources? = null
    private var cachedRefResourcesEn: ReferenceResources? = null
    
    private var appContext: Context? = null

    fun getAuthToken(): String? = prefs?.getToken()

    fun initSession(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) prefs = PrefsManager(context)
        if (jsFetcher == null) jsFetcher = JsResourceFetcher(context)
        if (refFetcher == null) refFetcher = ReferenceJsFetcher(context)
        
        val token = prefs?.getToken()
        val savedTheme = prefs?.loadData("theme_mode_pref", String::class.java)
        if (savedTheme != null) themeMode = savedTheme
        
        val savedDocMode = prefs?.loadData("doc_download_mode", String::class.java)
        if (savedDocMode != null) downloadMode = savedDocMode
        
        val savedLang = prefs?.loadData("language_pref", String::class.java)
        if (savedLang != null) language = savedLang.replace("\"", "")

        val isRemember = prefs?.loadData("pref_remember_me", Boolean::class.java) ?: false
        rememberMe = isRemember
        if (isRemember) {
            loginEmail = prefs?.loadData("pref_saved_email", String::class.java) ?: ""
            loginPass = prefs?.loadData("pref_saved_pass", String::class.java) ?: ""
        }
        
        customName = prefs?.loadData("local_custom_name", String::class.java)
        customPhotoUri = prefs?.loadData("local_custom_photo", String::class.java)

        loadLocalDictionary()
        loadDictionaries()

        if (token != null) {
            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token)
            loadOfflineData()
            appState = "APP"
            refreshAllData(force = true)
        } else {
            appState = "LOGIN"
        }
    }

    private fun loadDictionaries() {
        if (areDictionariesLoaded) return
        viewModelScope.launch {
            IdDefinitions.loadAll()
            areDictionariesLoaded = true
        }
    }

    private suspend fun performSilentLogin(): Boolean {
        if (!rememberMe || loginEmail.isBlank() || loginPass.isBlank()) return false
        DebugLogger.log("AUTH", "Silent login attempt...")
        return try {
            val resp = NetworkClient.api.login(LoginRequest(loginEmail.trim(), loginPass.trim()))
            val token = resp.authorisation?.token
            if (token != null) {
                prefs?.saveToken(token)
                NetworkClient.interceptor.authToken = token
                NetworkClient.cookieJar.injectSessionCookies(token)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            DebugLogger.log("AUTH", "Silent login failed: ${e.message}")
            false
        }
    }

    suspend fun getFreshPersonalInfo(retry: Boolean = true): Pair<UserData?, StudentInfoResponse?> = withContext(Dispatchers.IO) {
        IdDefinitions.loadAll()
        withContext(Dispatchers.Main) { areDictionariesLoaded = true }

        try {
            val u = NetworkClient.api.getUser().user
            val p = NetworkClient.api.getProfile()
            
            withContext(Dispatchers.Main) {
                userData = u
                profileData = p
            }
            return@withContext Pair(u, p)
        } catch (e: Exception) {
            val isAuthError = e.message?.contains("401") == true || e.message?.contains("Unauthenticated") == true
            if (isAuthError && retry && performSilentLogin()) {
                return@withContext getFreshPersonalInfo(retry = false)
            }
            throw e
        }
    }

    fun saveLocalProfile(name: String?, photoUri: String?) {
        customName = name
        customPhotoUri = photoUri
        prefs?.saveData("local_custom_name", name)
        prefs?.saveData("local_custom_photo", photoUri)
        avatarRefreshTrigger++
    }

    // --- UPDATER LOGIC ---

    fun checkForUpdates() {
        if (appContext == null) return
        viewModelScope.launch {
            try {
                val apiUrl = appContext!!.getString(R.string.update_repo_path)
                val release = withContext(Dispatchers.IO) { NetworkClient.githubApi.getLatestRelease(apiUrl) }
                val currentVer = BuildConfig.VERSION_NAME
                val remoteVer = release.tagName.replace("v", "")
                val localVer = currentVer.replace("v", "")
                
                if (remoteVer != localVer && isNewerVersion(remoteVer, localVer)) {
                    updateAvailableRelease = release
                }
            } catch (e: Exception) { }
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        try {
            val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val lParts = local.split(".").map { it.toIntOrNull() ?: 0 }
            val length = maxOf(rParts.size, lParts.size)
            for (i in 0 until length) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (e: Exception) { return false }
        return false
    }

    fun downloadUpdate(context: Context) {
        val release = updateAvailableRelease ?: return
        val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: return
        
        try {
            val url = apkAsset.downloadUrl
            val fileName = apkAsset.name
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(context.getString(R.string.update_notif_title))
                .setDescription(context.getString(R.string.update_notif_desc, release.tagName))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType("application/vnd.android.package-archive")

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = manager.enqueue(request)
            
            isUpdateDownloading = true
            updateProgress = 0f
            isUpdateReady = false
            
            monitorUpdateDownload(context, downloadId!!)
            
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.update_error_download, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun monitorUpdateDownload(context: Context, id: Long) {
        updateDownloadJob?.cancel()
        updateDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var downloading = true
            while (downloading && isActive) {
                val query = DownloadManager.Query().setFilterById(id)
                var cursor: Cursor? = null
                try {
                    cursor = manager.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val bytesDl = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTot = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false
                            val uriStr = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            if (uriStr != null) {
                                val file = File(Uri.parse(uriStr).path!!)
                                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                withContext(Dispatchers.Main) {
                                    updateProgress = 1f
                                    isUpdateDownloading = false
                                    isUpdateReady = true
                                    updateLocalUri = contentUri
                                }
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                            withContext(Dispatchers.Main) {
                                isUpdateDownloading = false
                                Toast.makeText(context, context.getString(R.string.update_error_generic), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (bytesTot > 0) {
                                withContext(Dispatchers.Main) { updateProgress = bytesDl.toFloat() / bytesTot.toFloat() }
                            }
                        }
                    } else {
                        downloading = false
                    }
                } catch (e: Exception) {
                    downloading = false
                } finally {
                    cursor?.close()
                }
                delay(500)
            }
        }
    }

    fun cancelUpdate(context: Context) {
        updateDownloadJob?.cancel()
        downloadId?.let { id ->
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.remove(id)
        }
        isUpdateDownloading = false
        updateProgress = 0f
        isUpdateReady = false
        downloadId = null
    }

    fun triggerInstallUpdate(context: Context) {
        updateLocalUri?.let { uri ->
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
            context.startActivity(installIntent)
        }
    }

    // --- END UPDATER LOGIC ---

    private fun loadLocalDictionary() {
        val savedJson = prefs?.loadData("custom_dictionary_json", String::class.java)
        if (savedJson != null) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                dictionaryMap = Gson().fromJson(savedJson, type)
            } catch (e: Exception) {
                dictionaryMap = dictUtils.getDefaultDictionary()
            }
        } else {
            dictionaryMap = dictUtils.getDefaultDictionary()
            saveDictionary()
        }
    }

    private fun saveDictionary() {
        val json = Gson().toJson(dictionaryMap)
        prefs?.saveData("custom_dictionary_json", json)
    }

    fun addOrUpdateDictionaryEntry(key: String, value: String) {
        val mutable = dictionaryMap.toMutableMap()
        mutable[key.trim()] = value.trim()
        dictionaryMap = mutable
        saveDictionary()
    }

    fun removeDictionaryEntry(key: String) {
        val mutable = dictionaryMap.toMutableMap()
        mutable.remove(key)
        dictionaryMap = mutable
        saveDictionary()
    }

    fun resetDictionaryToDefault() {
        dictionaryMap = dictUtils.getDefaultDictionary()
        saveDictionary()
    }

    fun refresh() {
        if (isLoading) return
        refreshAllData(force = true)
    }

    fun onAppResume() {
        attemptAutoRefresh()
        checkForUpdates() 
    }

    fun onNetworkAvailable() {
        attemptAutoRefresh()
    }

    private fun attemptAutoRefresh() {
        if (appState != "APP" || isLoading) return
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime > refreshCooldownMs) {
            refreshAllData(force = false)
        }
    }

    private fun loadFromSharedPreferences() {
        prefs?.let { p ->
            userData = p.loadData("user_data", UserData::class.java)
            profileData = p.loadData("profile_data", StudentInfoResponse::class.java)
            payStatus = p.loadData("pay_status", PayStatusResponse::class.java)
            verify2FAStatus = p.loadData("verify_2fa_status", Verify2FAResponse::class.java)
            newsList = p.loadList("news_list")
            fullSchedule = p.loadList("schedule_list")
            sessionData = p.loadList("session_list")
            transcriptData = p.loadList("transcript_list")
            processScheduleLocally()
        }
    }

    private fun loadOfflineData() {
        viewModelScope.launch {
            try {
                // Try to load from Room Database first
                val repository = prefs?.getRepository()
                if (repository != null) {
                    withContext(Dispatchers.IO) {
                        // Load data from Room
                        val roomUserData = repository.getUserDataSync()
                        val roomProfileData = repository.getProfileDataSync()
                        val roomPayStatus = repository.getPayStatusSync()
                        val roomNews = repository.getAllNewsSync()
                        val roomSchedule = repository.getAllSchedulesSync()
                        val roomTimeMap = repository.getTimeMapSync()
                        val roomGrades = repository.getAllGradesSync()
                        
                        withContext(Dispatchers.Main) {
                            // Use Room data if available, otherwise fall back to SharedPreferences
                            userData = roomUserData ?: prefs?.loadData("user_data", UserData::class.java)
                            profileData = roomProfileData ?: prefs?.loadData("profile_data", StudentInfoResponse::class.java)
                            payStatus = roomPayStatus ?: prefs?.loadData("pay_status", PayStatusResponse::class.java)
                            verify2FAStatus = prefs?.loadData("verify_2fa_status", Verify2FAResponse::class.java)
                            
                            newsList = if (roomNews.isNotEmpty()) roomNews else prefs?.loadList("news_list") ?: emptyList()
                            fullSchedule = if (roomSchedule.isNotEmpty()) roomSchedule else prefs?.loadList("schedule_list") ?: emptyList()
                            timeMap = if (roomTimeMap.isNotEmpty()) roomTimeMap else {
                                val json = prefs?.prefs?.getString("time_map", null)
                                if (json != null) {
                                    try {
                                        val gson = com.google.gson.Gson()
                                        gson.fromJson(json, object : com.google.gson.reflect.TypeToken<Map<Int, String>>() {}.type)
                                    } catch (e: Exception) {
                                        emptyMap()
                                    }
                                } else {
                                    emptyMap()
                                }
                            }
                            
                            // For grades, sessionData and transcriptData come from different sources
                            // SessionData is from session_list, transcriptData is from transcript_list
                            if (roomGrades.subjects?.isNotEmpty() == true) {
                                // roomGrades is a SessionResponse - use it for sessionData
                                sessionData = listOf(roomGrades)
                            } else {
                                sessionData = prefs?.loadList("session_list") ?: emptyList()
                            }
                            
                            // transcriptData is separate - load from SharedPreferences fallback
                            transcriptData = prefs?.loadList("transcript_list") ?: emptyList()
                            
                            processScheduleLocally()
                        }
                    }
                } else {
                    // Fallback to SharedPreferences only
                    loadFromSharedPreferences()
                }
            } catch (e: Exception) {
                DebugLogger.log("DATA", "Error loading from Room, falling back to SharedPreferences: ${e.message}")
                // Fallback to SharedPreferences on error
                loadFromSharedPreferences()
            }
        }
    }

    fun setTheme(mode: String) {
        themeMode = mode
        prefs?.saveData("theme_mode_pref", mode)
    }

    fun setDocMode(mode: String) {
        downloadMode = mode
        prefs?.saveData("doc_download_mode", mode)
    }
    
    fun setAppLanguage(lang: String) {
        language = lang
        prefs?.saveData("language_pref", lang)
        processScheduleLocally()
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true; errorMsg = null; NetworkClient.cookieJar.clear(); NetworkClient.interceptor.authToken = null
            
            // Normalize email to ensure it has @oshsu.kg domain
            val normalizedEmail = EmailHelper.normalizeEmail(email)
            
            if (rememberMe) {
                prefs?.saveData("pref_remember_me", true)
                prefs?.saveData("pref_saved_email", normalizedEmail)
                prefs?.saveData("pref_saved_pass", pass)
            } else {
                prefs?.saveData("pref_remember_me", false)
                prefs?.saveData("pref_saved_email", "")
                prefs?.saveData("pref_saved_pass", "")
            }

            try {
                val resp = withContext(Dispatchers.IO) { NetworkClient.api.login(LoginRequest(normalizedEmail.trim(), pass.trim())) }
                val token = resp.authorisation?.token
                if (token != null) {
                    prefs?.saveToken(token)
                    NetworkClient.interceptor.authToken = token
                    NetworkClient.cookieJar.injectSessionCookies(token)
                    refreshAllData(force = true)
                    appState = "APP"
                } else errorMsg = appContext?.getString(R.string.error_credentials) ?: "Incorrect credentials"
            } catch (e: Exception) { 
                errorMsg = appContext?.getString(R.string.error_login_failed, e.message) ?: "Login Failed: ${e.message}" 
            }
            isLoading = false
        }
    }

    fun logout() {
        val wasRemember = rememberMe
        val savedE = loginEmail
        val savedP = loginPass

        appState = "LOGIN"; currentTab = 0; userData = null; profileData = null; payStatus = null
        newsList = emptyList(); fullSchedule = emptyList(); sessionData = emptyList(); transcriptData = emptyList()
        verify2FAStatus = null
        prefs?.clearAll(); NetworkClient.cookieJar.clear(); NetworkClient.interceptor.authToken = null
        
        prefs?.saveData("theme_mode_pref", themeMode)
        prefs?.saveData("doc_download_mode", downloadMode)
        prefs?.saveData("language_pref", language)
        prefs?.saveData("custom_dictionary_json", Gson().toJson(dictionaryMap))
        
        if (wasRemember) {
            prefs?.saveData("pref_remember_me", true)
            prefs?.saveData("pref_saved_email", savedE)
            prefs?.saveData("pref_saved_pass", savedP)
            loginEmail = savedE
            loginPass = savedP
            rememberMe = true
        }
    }

    private fun refreshAllData(force: Boolean, retryCount: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isRefreshing = true }
            try {
                val user = NetworkClient.api.getUser().user
                val profile = NetworkClient.api.getProfile()
                withContext(Dispatchers.Main) {
                    userData = user; profileData = profile
                    prefs?.saveData("user_data", user); prefs?.saveData("profile_data", profile)
                }
                
                try {
                    val v2fa = NetworkClient.api.verify2FA()
                    withContext(Dispatchers.Main) { verify2FAStatus = v2fa; prefs?.saveData("verify_2fa_status", v2fa) }
                } catch (e: Exception) {}
                
                if (profile != null) {
                    try { val news = NetworkClient.api.getNews(); withContext(Dispatchers.Main) { newsList = news; prefs?.saveList("news_list", news) } } catch (_: Exception) {}
                    try { val pay = NetworkClient.api.getPayStatus(); withContext(Dispatchers.Main) { payStatus = pay; prefs?.saveData("pay_status", pay) } } catch (_: Exception) {}
                    loadScheduleNetwork(profile)
                    fetchSession(profile)
                }
                lastRefreshTime = System.currentTimeMillis()
                checkForUpdates() 
            } catch (e: Exception) {
                // --- RESTORED RETRY LOGIC ---
                val isAuthError = e.message?.contains("401") == true || e.message?.contains("HTTP 401") == true || e.message?.contains("Unauthenticated") == true
                
                if (isAuthError && retryCount == 0) {
                    val reloginSuccess = performSilentLogin()
                    if (reloginSuccess) {
                        refreshAllData(force, retryCount = 1) 
                        return@launch
                    } else {
                        withContext(Dispatchers.Main) { logout() }
                    }
                } else if (isAuthError) {
                    withContext(Dispatchers.Main) { logout() }
                }
            } finally {
                withContext(Dispatchers.Main) { isRefreshing = false; isLoading = false }
            }
        }
    }

    private suspend fun loadScheduleNetwork(profile: StudentInfoResponse) {
        val mov = profile.studentMovement ?: return
        try {
            val years = NetworkClient.api.getYears()
            val activeYearId = years.find { it.active }?.id ?: AcademicYearHelper.getDefaultActiveYearId()
            val times = try { NetworkClient.api.getLessonTimes(mov.id_speciality!!, mov.id_edu_form!!, activeYearId) } catch (e: Exception) { emptyList() }
            val wrappers = NetworkClient.api.getSchedule(mov.id_speciality!!, mov.id_edu_form!!, activeYearId, profile.active_semester ?: 1)
            withContext(Dispatchers.Main) {
                timeMap = times.associate { it.id_lesson to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                fullSchedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }
                prefs?.saveList("schedule_list", fullSchedule)
                prefs?.saveData("time_map", timeMap)
                processScheduleLocally()
            }
        } catch (_: Exception) {}
    }

    private fun processScheduleLocally() {
        if (fullSchedule.isEmpty()) return
        determinedStream = fullSchedule.asSequence()
            .filter { it.subject_type?.name_en?.contains("Lection", ignoreCase = true) == true }
            .mapNotNull { it.stream?.numeric }
            .firstOrNull()
        determinedGroup = fullSchedule.asSequence()
            .filter { it.subject_type?.name_en?.contains("Practical", ignoreCase = true) == true }
            .mapNotNull { it.stream?.numeric }
            .firstOrNull()
            
        val cal = Calendar.getInstance()
        val loc = Locale(language) 
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        
        // After 8 PM, show next day's name
        if (currentHour >= 20) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // If next day is Sunday, skip to Monday to match the class list
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        var dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, loc) ?: appContext?.getString(R.string.today) ?: "Today"
        if (dayName.isNotEmpty()) dayName = dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(loc) else it.toString() }
        todayDayName = dayName

        // Use WidgetHelper to get classes with 8 PM logic
        todayClasses = myedu.oshsu.kg.widget.WidgetHelper.getTodayClasses(fullSchedule)
    }
    
    fun getSubjectTypeResId(item: ScheduleItem): Int? {
        val rawEn = item.subject_type?.name_en ?: ""
        val rawRu = item.subject_type?.name_ru ?: ""
        return when {
            rawEn.contains("Lection", ignoreCase = true) || rawRu.equals("Лекция", ignoreCase = true) -> R.string.type_lecture
            rawEn.contains("Practical", ignoreCase = true) || rawRu.contains("Практические", ignoreCase = true) -> R.string.type_practice
            rawEn.contains("Lab", ignoreCase = true) || rawRu.contains("Лаборатор", ignoreCase = true) -> R.string.type_lab
            else -> null
        }
    }

    private fun fetchSession(profile: StudentInfoResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isGradesLoading = true }
                val oldSession = prefs?.loadList<SessionResponse>("session_list") ?: emptyList()
                val session = NetworkClient.api.getSession(profile.active_semester ?: 1)
                
                // Check for updates and send notifications
                val currentContext = appContext
                val currentPrefs = prefs
                if (oldSession.isNotEmpty() && session.isNotEmpty() && currentContext != null && currentPrefs != null) {
                    val localizedContext = NotificationHelper.getLocalizedContext(currentContext, currentPrefs)
                    val (gradeUpdates, portalUpdates) = NotificationHelper.checkForUpdates(oldSession, session, localizedContext)
                    if (gradeUpdates.isNotEmpty()) NotificationHelper.sendNotification(localizedContext, gradeUpdates, isPortalOpening = false)
                    if (portalUpdates.isNotEmpty()) NotificationHelper.sendNotification(localizedContext, portalUpdates, isPortalOpening = true)
                }
                
                withContext(Dispatchers.Main) { sessionData = session; prefs?.saveList("session_list", session) }
            } catch (_: Exception) {} finally { withContext(Dispatchers.Main) { isGradesLoading = false } }
        }
    }

    // --- JOURNAL FUNCTIONS ---
    fun openJournal(subject: SessionSubjectWrapper, semesterId: Int? = null) {
        selectedJournalSubject = subject
        // If semester is provided, use it; otherwise use current selected or active
        if (semesterId != null) {
            selectedSemesterId = semesterId
        }
        selectedJournalType = 1 // Reset to Lecture
        showJournalSheet = true
        fetchJournal()
    }
    
    fun changeJournalType(typeId: Int) {
        selectedJournalType = typeId
        fetchJournal()
    }
    
    fun fetchJournal() {
        val subject = selectedJournalSubject ?: return
        // Use id_curricula field from API response, fallback to marklist.id, then subject.id
        val curriculaId = subject.idCurricula ?: subject.marklist?.id?.toInt() ?: subject.subject?.id
        
        if (curriculaId == null) {
            DebugLogger.log("JOURNAL", "All ID fields are null, cannot fetch journal")
            viewModelScope.launch(Dispatchers.Main) {
                isJournalLoading = false
                journalList = emptyList()
            }
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isJournalLoading = true }
                
                val semesterId = selectedSemesterId ?: profileData?.active_semester ?: 1
                val activeSemester = profileData?.active_semester ?: semesterId
                
                // Calculate academic year based on semester
                // Each academic year has 2 semesters (odd and even)
                // If current active semester is 9-10, it's year 25
                // If semester is 7-8, it's year 24, etc.
                val currentActiveYear = AcademicYearHelper.getDefaultActiveYearId()
                val semesterDiff = activeSemester - semesterId
                val yearOffset = semesterDiff / 2  // 2 semesters per year
                val eduYearId = currentActiveYear - yearOffset
                
                DebugLogger.log("JOURNAL", "Fetching journal: curricula=$curriculaId, semester=$semesterId, type=$selectedJournalType, year=$eduYearId (active=$activeSemester, offset=$yearOffset)")
                
                // Note: API expects id_curricula from SessionSubjectWrapper (fallback to marklist.id, then subject.id)
                val journal = NetworkClient.api.getJournal(
                    idCurricula = curriculaId,
                    idSemester = semesterId,
                    idSubjectType = selectedJournalType,
                    idEduYear = eduYearId
                )
                
                DebugLogger.log("JOURNAL", "Received ${journal.size} journal entries")
                
                withContext(Dispatchers.Main) { 
                    journalList = journal
                }
            } catch (e: Exception) {
                DebugLogger.log("JOURNAL", "Failed to fetch journal: ${e.message}")
                DebugLogger.log("JOURNAL", "Exception: ${e.stackTraceToString()}")
            } finally {
                withContext(Dispatchers.Main) { isJournalLoading = false }
            }
        }
    }

    fun fetchTranscript() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isTranscriptLoading = true; showTranscriptScreen = true; transcriptData = prefs?.loadList<TranscriptYear>("transcript_list") ?: emptyList() }
                val uid = userData?.id ?: return@launch
                val movId = profileData?.studentMovement?.id ?: return@launch 
                val transcript = NetworkClient.api.getTranscript(uid, movId)
                withContext(Dispatchers.Main) { transcriptData = transcript; prefs?.saveList("transcript_list", transcript) }
            } catch (e: Exception) { } finally { withContext(Dispatchers.Main) { isTranscriptLoading = false } }
        }
    }
    
    fun getTimeString(lessonId: Int): String = timeMap[lessonId] ?: "$lessonId"

    private fun getFormattedFileName(docType: String, lang: String? = null): String {
        val last = userData?.last_name ?: ""; val first = userData?.name ?: ""
        val cleanName = "$last $first".trim().replace(" ", "_").replace(".", "")
        val suffix = if (lang != null) "_$lang" else ""
        return "${cleanName}_${docType}${suffix}.pdf"
    }

    // --- PDF GENERATION LOGIC ---

    fun cancelPdfGeneration() {
        pdfGenerationJob?.cancel()
        isPdfGenerating = false
        pdfStatusMessage = null
    }

    fun clearPdfState() {
        generatedPdfUri = null
        pdfStatusMessage = null
        isPdfGenerating = false
    }

    fun generateTranscriptPdf(context: Context, lang: String) {
        if (isPdfGenerating) return
        val studentId = userData?.id ?: return
        
        pdfGenerationJob = viewModelScope.launch {
            isPdfGenerating = true
            generatedPdfUri = null
            pdfStatusMessage = context.getString(R.string.status_preparing_transcript)
            try {
                if (dictionaryMap.isEmpty()) loadLocalDictionary()
                var resources = if (lang == "en") cachedResourcesEn else cachedResourcesRu
                if (resources == null) {
                    pdfStatusMessage = context.getString(R.string.status_fetching_scripts)
                    val fetcher = jsFetcher ?: JsResourceFetcher(context)
                    resources = fetcher.fetchResources({ }, lang, dictionaryMap)
                    if (lang == "en") cachedResourcesEn = resources else cachedResourcesRu = resources
                }

                ensureActive()

                val (infoJsonString, transcriptRaw, linkId, rawUrl) = withContext(Dispatchers.IO) {
                    val infoRaw = NetworkClient.api.getStudentInfoRaw(studentId).string()
                    val infoJson = JSONObject(infoRaw)
                    infoJson.put("fullName", "${infoJson.optString("last_name")} ${infoJson.optString("name")} ${infoJson.optString("father_name")}".replace("null", "").trim())
                    val movId = profileData?.studentMovement?.id ?: 0L
                    val transcriptRaw = NetworkClient.api.getTranscriptDataRaw(studentId, movId).string()
                    val keyRaw = NetworkClient.api.getTranscriptLink(DocIdRequest(studentId)).string()
                    val keyObj = JSONObject(keyRaw)
                    
                    Quadruple(infoJson.toString(), transcriptRaw, keyObj.optLong("id"), keyObj.optString("url"))
                }
                
                val qrUrl = rawUrl.replace("https::/", "https://")
                
                pdfStatusMessage = context.getString(R.string.generating_pdf)
                val bytes = WebPdfGenerator(context).generatePdf(infoJsonString, transcriptRaw, linkId, qrUrl, resources!!, lang, dictionaryMap) { }
                
                pdfStatusMessage = context.getString(R.string.uploading_pdf)
                try {
                    uploadPdfOnly(linkId, studentId, bytes, getFormattedFileName("Transcript", lang), true)
                } catch (e: Exception) {
                    DebugLogger.log("PDF_UPLOAD", "Failed to upload transcript: ${e.message}")
                }
                
                saveToDownloads(context, bytes, getFormattedFileName("Transcript", lang))
                pdfStatusMessage = null
            } catch (e: CancellationException) {
                pdfStatusMessage = null
            } catch (e: Exception) {
                pdfStatusMessage = context.getString(R.string.error_generic, e.message)
                delay(3000); pdfStatusMessage = null
            } finally { 
                isPdfGenerating = false 
            }
        }
    }

    fun generateReferencePdf(context: Context, lang: String) {
        if (isPdfGenerating) return
        val studentId = userData?.id ?: return
        
        pdfGenerationJob = viewModelScope.launch {
            isPdfGenerating = true
            generatedPdfUri = null
            pdfStatusMessage = context.getString(R.string.status_preparing_reference)
            try {
                if (dictionaryMap.isEmpty()) loadLocalDictionary()
                var resources = if (lang == "en") cachedRefResourcesEn else cachedRefResourcesRu
                if (resources == null) {
                    pdfStatusMessage = context.getString(R.string.status_fetching_scripts)
                    val fetcher = refFetcher ?: ReferenceJsFetcher(context)
                    resources = fetcher.fetchResources({ }, lang, dictionaryMap)
                    if (lang == "en") cachedRefResourcesEn = resources else cachedRefResourcesRu = resources
                }

                ensureActive()

                val (infoJsonString, licenseRaw, univRaw, linkId, rawUrl) = withContext(Dispatchers.IO) {
                    val infoRaw = NetworkClient.api.getStudentInfoRaw(studentId).string()
                    val infoJson = JSONObject(infoRaw)
                    infoJson.put("fullName", "${infoJson.optString("last_name")} ${infoJson.optString("name")} ${infoJson.optString("father_name")}".replace("null", "").trim())
                    var specId = infoJson.optJSONObject("speciality")?.optInt("id") ?: infoJson.optJSONObject("lastStudentMovement")?.optJSONObject("speciality")?.optInt("id") ?: 0
                    var eduFormId = infoJson.optJSONObject("lastStudentMovement")?.optJSONObject("edu_form")?.optInt("id") ?: infoJson.optJSONObject("edu_form")?.optInt("id") ?: 0
                    val licenseRaw = NetworkClient.api.getSpecialityLicense(specId, eduFormId).string()
                    val univRaw = NetworkClient.api.getUniversityInfo().string()
                    val linkRaw = NetworkClient.api.getReferenceLink(DocIdRequest(studentId)).string()
                    val linkObj = JSONObject(linkRaw)
                    
                    Quintuple(infoJson.toString(), licenseRaw, univRaw, linkObj.optLong("id"), linkObj.optString("url"))
                }
                
                val qrUrl = rawUrl.replace("https::/", "https://")
                
                pdfStatusMessage = context.getString(R.string.generating_pdf)
                val bytes = ReferencePdfGenerator(context).generatePdf(infoJsonString, licenseRaw, univRaw, linkId, qrUrl, resources!!, prefs?.getToken() ?: "", lang, dictionaryMap) { }
                
                pdfStatusMessage = context.getString(R.string.uploading_pdf)
                try {
                    uploadPdfOnly(linkId, studentId, bytes, getFormattedFileName("Reference", lang), false)
                } catch (e: Exception) {
                    DebugLogger.log("PDF_UPLOAD", "Failed to upload reference: ${e.message}")
                }
                
                saveToDownloads(context, bytes, getFormattedFileName("Reference", lang))
                pdfStatusMessage = null
            } catch (e: CancellationException) {
                pdfStatusMessage = null
            } catch (e: Exception) {
                pdfStatusMessage = context.getString(R.string.error_generic, e.message)
                delay(3000); pdfStatusMessage = null
            } finally { 
                isPdfGenerating = false 
            }
        }
    }

    private suspend fun uploadPdfOnly(linkId: Long, studentId: Long, bytes: ByteArray, filename: String, isTranscript: Boolean) {
        val plain = "text/plain".toMediaTypeOrNull()
        val pdfType = "application/pdf".toMediaTypeOrNull()
        val bodyId = linkId.toString().toRequestBody(plain)
        val bodyStudent = studentId.toString().toRequestBody(plain)
        val filePart = MultipartBody.Part.createFormData("pdf", filename, bytes.toRequestBody(pdfType))
        
        withContext(Dispatchers.IO) { 
            if (isTranscript) NetworkClient.api.uploadPdf(bodyId, bodyStudent, filePart).string() 
            else NetworkClient.api.uploadReferencePdf(bodyId, bodyStudent, filePart).string() 
        }
    }

    private suspend fun saveToDownloads(context: Context, bytes: ByteArray, filename: String) {
        try {
            pdfStatusMessage = context.getString(R.string.status_saving)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var file = File(downloadsDir, filename)
            var counter = 1
            while (file.exists()) { file = File(downloadsDir, "${filename.substringBeforeLast(".")}($counter).${filename.substringAfterLast(".")}"); counter++ }
            withContext(Dispatchers.IO) { FileOutputStream(file).use { it.write(bytes) } }
            
            // Generate URI
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            
            withContext(Dispatchers.Main) {
                generatedPdfUri = uri // Enable "Open" button
                Toast.makeText(context, context.getString(R.string.status_saved, file.name), Toast.LENGTH_SHORT).show()
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                } catch (e: Exception) { Toast.makeText(context, context.getString(R.string.error_no_pdf_viewer), Toast.LENGTH_LONG).show() }
            }
        } catch (e: Exception) { pdfStatusMessage = context.getString(R.string.status_save_failed, e.message); delay(2000) }
    }
    
    // Helpers
    data class Quadruple(val info: String, val transcript: String, val linkId: Long, val url: String)
    data class Quintuple(val info: String, val license: String, val univ: String, val linkId: Long, val url: String)
    
    // Widget management
    fun requestAddWidget() {
        addWidgetRequestPending = true
    }
}
