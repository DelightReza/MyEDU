package myedu.oshsu.kg

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {
    // --- STATE: APP STATUS ---
    var appState by mutableStateOf("STARTUP")
    var currentTab by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    
    // --- STATE: LOGIN CREDENTIALS ---
    var loginEmail by mutableStateOf("")
    var loginPass by mutableStateOf("")
    var rememberMe by mutableStateOf(false)

    // --- REFRESH LOGIC ---
    private var lastRefreshTime: Long = 0
    private val refreshCooldownMs = TimeUnit.MINUTES.toMillis(5)

    // --- STATE: THEME ---
    var themeMode by mutableStateOf("SYSTEM")
    val isGlass: Boolean get() = themeMode == "GLASS" || themeMode == "AQUA"

    // --- STATE: SETTINGS ---
    var downloadMode by mutableStateOf("IN_APP") 
    var language by mutableStateOf("en") 
    
    // --- STATE: DEBUG TOOLS ---
    var isDebugPipVisible by mutableStateOf(false)
    var isDebugConsoleOpen by mutableStateOf(false)

    // --- STATE: DATA ---
    var userData by mutableStateOf<UserData?>(null)
    var profileData by mutableStateOf<StudentInfoResponse?>(null)
    var payStatus by mutableStateOf<PayStatusResponse?>(null)
    var newsList by mutableStateOf<List<NewsItem>>(emptyList())
    var verify2FAStatus by mutableStateOf<Verify2FAResponse?>(null)

    // --- STATE: SCHEDULE ---
    var fullSchedule by mutableStateOf<List<ScheduleItem>>(emptyList())
    var todayClasses by mutableStateOf<List<ScheduleItem>>(emptyList())
    var timeMap by mutableStateOf<Map<Int, String>>(emptyMap())
    
    var todayDayName by mutableStateOf("")

    var determinedStream by mutableStateOf<Int?>(null)
    var determinedGroup by mutableStateOf<Int?>(null)
    var selectedClass by mutableStateOf<ScheduleItem?>(null)
    
    var selectedScheduleDay by mutableStateOf(run {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) 0 else (dow - 2).coerceIn(0, 5)
    })
    
    // --- STATE: GRADES ---
    var sessionData by mutableStateOf<List<SessionResponse>>(emptyList())
    var isGradesLoading by mutableStateOf(false)
    
    // --- STATE: DOCS UI ---
    var transcriptData by mutableStateOf<List<TranscriptYear>>(emptyList())
    var isTranscriptLoading by mutableStateOf(false)
    var showTranscriptScreen by mutableStateOf(false)
    var showReferenceScreen by mutableStateOf(false)
    var showSettingsScreen by mutableStateOf(false)
    
    // --- DICTIONARY UI STATE ---
    var showDictionaryScreen by mutableStateOf(false)
    var dictionaryMap by mutableStateOf<Map<String, String>>(emptyMap())

    var webDocumentUrl by mutableStateOf<String?>(null)
    
    var isPdfGenerating by mutableStateOf(false)
    var pdfStatusMessage by mutableStateOf<String?>(null)

    private var prefs: PrefsManager? = null
    private var jsFetcher: JsResourceFetcher? = null
    private var refFetcher: ReferenceJsFetcher? = null
    private val dictUtils = DictionaryUtils()
    
    private var cachedResourcesRu: PdfResources? = null
    private var cachedResourcesEn: PdfResources? = null
    private var cachedRefResourcesRu: ReferenceResources? = null
    private var cachedRefResourcesEn: ReferenceResources? = null
    
    private var appContext: Context? = null

    fun getAuthToken(): String? = prefs?.getToken()

    fun initSession(context: Context) {
        DebugLogger.log("APP", "Session Init Started")
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
        
        // Load Dictionary from Prefs or Default
        loadLocalDictionary()

        if (token != null) {
            DebugLogger.log("AUTH", "Token found, loading offline data")
            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token)
            loadOfflineData()
            appState = "APP"
            refreshAllData(force = true)
        } else {
            DebugLogger.log("AUTH", "No token, showing login")
            appState = "LOGIN"
        }
    }

    // --- DICTIONARY METHODS ---
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
    }

    fun onNetworkAvailable() {
        attemptAutoRefresh()
    }

    private fun attemptAutoRefresh() {
        if (appState != "APP" || isLoading) return
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime > refreshCooldownMs) {
            DebugLogger.log("SYNC", "Auto-refresh triggered")
            refreshAllData(force = false)
        }
    }

    private fun loadOfflineData() {
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
            DebugLogger.log("AUTH", "Login attempt: $email")
            isLoading = true; errorMsg = null; NetworkClient.cookieJar.clear(); NetworkClient.interceptor.authToken = null
            
            if (rememberMe) {
                prefs?.saveData("pref_remember_me", true)
                prefs?.saveData("pref_saved_email", email)
                prefs?.saveData("pref_saved_pass", pass)
            } else {
                prefs?.saveData("pref_remember_me", false)
                prefs?.saveData("pref_saved_email", "")
                prefs?.saveData("pref_saved_pass", "")
            }

            try {
                val resp = withContext(Dispatchers.IO) { NetworkClient.api.login(LoginRequest(email.trim(), pass.trim())) }
                val token = resp.authorisation?.token
                if (token != null) {
                    DebugLogger.log("AUTH", "Login successful")
                    prefs?.saveToken(token)
                    NetworkClient.interceptor.authToken = token
                    NetworkClient.cookieJar.injectSessionCookies(token)
                    refreshAllData(force = true)
                    appState = "APP"
                } else errorMsg = appContext?.getString(R.string.error_credentials) ?: "Incorrect credentials"
            } catch (e: Exception) { 
                DebugLogger.log("AUTH", "Login error: ${e.message}")
                errorMsg = appContext?.getString(R.string.error_login_failed, e.message) ?: "Login Failed: ${e.message}" 
            }
            isLoading = false
        }
    }

    private suspend fun performSilentLogin(): Boolean {
        if (!rememberMe || loginEmail.isBlank() || loginPass.isBlank()) return false
        DebugLogger.log("AUTH", "Silent login attempt")
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
            false
        }
    }

    fun logout() {
        DebugLogger.log("AUTH", "Logging out")
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
        // Keep dictionary
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
            withContext(Dispatchers.Main) { isLoading = true }
            try {
                val user = NetworkClient.api.getUser().user
                val profile = NetworkClient.api.getProfile()
                withContext(Dispatchers.Main) {
                    userData = user; profileData = profile
                    prefs?.saveData("user_data", user); prefs?.saveData("profile_data", profile)
                }
                
                try {
                    val v2fa = NetworkClient.api.verify2FA()
                    withContext(Dispatchers.Main) {
                        verify2FAStatus = v2fa
                        prefs?.saveData("verify_2fa_status", v2fa)
                    }
                } catch (e: Exception) {}
                
                if (profile != null) {
                    try { val news = NetworkClient.api.getNews(); withContext(Dispatchers.Main) { newsList = news; prefs?.saveList("news_list", news) } } catch (_: Exception) {}
                    try { val pay = NetworkClient.api.getPayStatus(); withContext(Dispatchers.Main) { payStatus = pay; prefs?.saveData("pay_status", pay) } } catch (_: Exception) {}
                    loadScheduleNetwork(profile)
                    fetchSession(profile)
                }
                lastRefreshTime = System.currentTimeMillis()
            } catch (e: Exception) {
                DebugLogger.log("SYNC", "Refresh error: ${e.message}")
                val isAuthError = e.message?.contains("401") == true || e.message?.contains("HTTP 401") == true
                
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
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    private suspend fun loadScheduleNetwork(profile: StudentInfoResponse) {
        val mov = profile.studentMovement ?: return
        try {
            val years = NetworkClient.api.getYears()
            val activeYearId = years.find { it.active }?.id ?: 25
            val times = try { NetworkClient.api.getLessonTimes(mov.id_speciality!!, mov.id_edu_form!!, activeYearId) } catch (e: Exception) { emptyList() }
            val wrappers = NetworkClient.api.getSchedule(mov.id_speciality!!, mov.id_edu_form!!, activeYearId, profile.active_semester ?: 1)
            withContext(Dispatchers.Main) {
                timeMap = times.associate { (it.lesson?.num ?: 0) to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                fullSchedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }
                prefs?.saveList("schedule_list", fullSchedule)
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
        var dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, loc) 
            ?: appContext?.getString(R.string.today) ?: "Today"
            
        if (dayName.isNotEmpty()) {
            dayName = dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(loc) else it.toString() }
        }
        todayDayName = dayName

        val apiDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6 else cal.get(Calendar.DAY_OF_WEEK) - 2
        todayClasses = fullSchedule.filter { it.day == apiDay }
    }
    
    fun getSubjectTypeResId(item: ScheduleItem): Int? {
        val rawEn = item.subject_type?.name_en ?: ""
        val rawRu = item.subject_type?.name_ru ?: ""
        
        return when {
            rawEn.contains("Lection", ignoreCase = true) || 
            rawEn.contains("Lecture", ignoreCase = true) || 
            rawRu.equals("Лекция", ignoreCase = true) -> R.string.type_lecture

            rawEn.contains("Practical", ignoreCase = true) || 
            rawRu.contains("Практические", ignoreCase = true) -> R.string.type_practice
            
            rawEn.contains("Lab", ignoreCase = true) || 
            rawRu.contains("Лаборатор", ignoreCase = true) -> R.string.type_lab
            
            else -> null
        }
    }

    private fun fetchSession(profile: StudentInfoResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isGradesLoading = true }
                val session = NetworkClient.api.getSession(profile.active_semester ?: 1)
                withContext(Dispatchers.Main) { sessionData = session; prefs?.saveList("session_list", session) }
            } catch (_: Exception) {} finally { withContext(Dispatchers.Main) { isGradesLoading = false } }
        }
    }

    fun fetchTranscript() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { 
                    isTranscriptLoading = true
                    showTranscriptScreen = true
                    transcriptData = prefs?.loadList<TranscriptYear>("transcript_list") ?: emptyList() 
                }
                val uid = userData?.id ?: return@launch
                val movId = profileData?.studentMovement?.id ?: return@launch 
                val transcript = NetworkClient.api.getTranscript(uid, movId)
                withContext(Dispatchers.Main) { transcriptData = transcript; prefs?.saveList("transcript_list", transcript) }
            } catch (e: Exception) { } finally { withContext(Dispatchers.Main) { isTranscriptLoading = false } }
        }
    }
    
    fun getTimeString(lessonId: Int): String = timeMap[lessonId] ?: "$lessonId"

    private fun getFormattedFileName(docType: String, lang: String? = null): String {
        val last = userData?.last_name ?: ""
        val first = userData?.name ?: ""
        val cleanName = "$last $first".trim().replace(" ", "_").replace(".", "")
        val suffix = if (lang != null) "_$lang" else ""
        return "${cleanName}_${docType}${suffix}.pdf"
    }

    fun generateTranscriptPdf(context: Context, lang: String) {
        if (isPdfGenerating) return
        DebugLogger.log("PDF", "Starting Transcript Gen ($lang)")
        val studentId = userData?.id ?: return
        viewModelScope.launch {
            isPdfGenerating = true
            pdfStatusMessage = context.getString(R.string.status_preparing_transcript)
            try {
                // Ensure we have the latest dictionary
                if (dictionaryMap.isEmpty()) loadLocalDictionary()
                
                var resources = if (lang == "en") cachedResourcesEn else cachedResourcesRu
                if (resources == null) {
                    pdfStatusMessage = context.getString(R.string.status_fetching_scripts)
                    val fetcher = jsFetcher ?: JsResourceFetcher(context)
                    // Pass current dictionary to fetcher
                    resources = fetcher.fetchResources({ DebugLogger.log("JS_FETCHER", it) }, lang, dictionaryMap)
                    if (lang == "en") cachedResourcesEn = resources else cachedResourcesRu = resources
                }
                val infoRaw = withContext(Dispatchers.IO) { NetworkClient.api.getStudentInfoRaw(studentId).string() }
                val infoJson = JSONObject(infoRaw)
                infoJson.put("fullName", "${infoJson.optString("last_name")} ${infoJson.optString("name")} ${infoJson.optString("father_name")}".replace("null", "").trim())
                
                val movId = profileData?.studentMovement?.id ?: 0L
                val transcriptRaw = withContext(Dispatchers.IO) { NetworkClient.api.getTranscriptDataRaw(studentId, movId).string() }
                val keyRaw = withContext(Dispatchers.IO) { NetworkClient.api.getTranscriptLink(DocIdRequest(studentId)).string() }
                val keyObj = JSONObject(keyRaw)
                
                pdfStatusMessage = context.getString(R.string.generating_pdf)
                val bytes = WebPdfGenerator(context).generatePdf(infoJson.toString(), transcriptRaw, keyObj.optLong("id"), keyObj.optString("url"), resources!!, lang, dictionaryMap) { 
                    println(it)
                    DebugLogger.log("JS_TRANSCRIPT", it)
                }
                
                val filename = getFormattedFileName("Transcript", lang)
                saveToDownloads(context, bytes, filename)
                
                pdfStatusMessage = null
            } catch (e: Exception) {
                DebugLogger.log("PDF_ERR", "Transcript Gen Failed: ${e.message}")
                pdfStatusMessage = context.getString(R.string.error_generic, e.message)
                e.printStackTrace()
                delay(3000)
                pdfStatusMessage = null
            } finally { isPdfGenerating = false }
        }
    }

    fun generateReferencePdf(context: Context, lang: String) {
        if (isPdfGenerating) return
        DebugLogger.log("PDF", "Starting Reference Gen ($lang)")
        val studentId = userData?.id ?: return
        viewModelScope.launch {
            isPdfGenerating = true
            pdfStatusMessage = context.getString(R.string.status_preparing_reference)
            try {
                // Ensure we have the latest dictionary
                if (dictionaryMap.isEmpty()) loadLocalDictionary()

                var resources = if (lang == "en") cachedRefResourcesEn else cachedRefResourcesRu
                if (resources == null) {
                    pdfStatusMessage = context.getString(R.string.status_fetching_scripts)
                    val fetcher = refFetcher ?: ReferenceJsFetcher(context)
                    resources = fetcher.fetchResources({ DebugLogger.log("JS_FETCHER", it) }, lang, dictionaryMap)
                    if (lang == "en") cachedRefResourcesEn = resources else cachedRefResourcesRu = resources
                }
                val infoRaw = withContext(Dispatchers.IO) { NetworkClient.api.getStudentInfoRaw(studentId).string() }
                val infoJson = JSONObject(infoRaw)
                infoJson.put("fullName", "${infoJson.optString("last_name")} ${infoJson.optString("name")} ${infoJson.optString("father_name")}".replace("null", "").trim())
                
                var specId = infoJson.optJSONObject("speciality")?.optInt("id") ?: infoJson.optJSONObject("lastStudentMovement")?.optJSONObject("speciality")?.optInt("id") ?: 0
                var eduFormId = infoJson.optJSONObject("lastStudentMovement")?.optJSONObject("edu_form")?.optInt("id") ?: infoJson.optJSONObject("edu_form")?.optInt("id") ?: 0
                
                val licenseRaw = withContext(Dispatchers.IO) { NetworkClient.api.getSpecialityLicense(specId, eduFormId).string() }
                val univRaw = withContext(Dispatchers.IO) { NetworkClient.api.getUniversityInfo().string() }
                val linkRaw = withContext(Dispatchers.IO) { NetworkClient.api.getReferenceLink(DocIdRequest(studentId)).string() }
                val linkObj = JSONObject(linkRaw)
                
                pdfStatusMessage = context.getString(R.string.generating_pdf)
                val bytes = ReferencePdfGenerator(context).generatePdf(infoJson.toString(), licenseRaw, univRaw, linkObj.optLong("id"), linkObj.optString("url"), resources!!, prefs?.getToken() ?: "", lang, dictionaryMap) { 
                    println(it)
                    DebugLogger.log("JS_REF", it)
                }
                
                val filename = getFormattedFileName("Reference", lang)
                saveToDownloads(context, bytes, filename)
                
                pdfStatusMessage = null
            } catch (e: Exception) {
                DebugLogger.log("PDF_ERR", "Reference Gen Failed: ${e.message}")
                pdfStatusMessage = context.getString(R.string.error_generic, e.message)
                e.printStackTrace()
                delay(3000)
                pdfStatusMessage = null
            } finally { isPdfGenerating = false }
        }
    }

    private suspend fun saveToDownloads(context: Context, bytes: ByteArray, filename: String) {
        try {
            pdfStatusMessage = context.getString(R.string.status_saving)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var file = File(downloadsDir, filename)
            var counter = 1
            while (file.exists()) {
                val name = filename.substringBeforeLast(".")
                val ext = filename.substringAfterLast(".")
                file = File(downloadsDir, "$name ($counter).$ext")
                counter++
            }

            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { it.write(bytes) }
            }
            
            DebugLogger.log("PDF", "File saved: ${file.name}")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.status_saved, file.name), Toast.LENGTH_SHORT).show()
                try {
                    val authority = "${context.packageName}.provider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "application/pdf")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.error_no_pdf_viewer), Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            DebugLogger.log("PDF_ERR", "Save Failed: ${e.message}")
            pdfStatusMessage = context.getString(R.string.status_save_failed, e.message)
            delay(2000)
        }
    }
}
