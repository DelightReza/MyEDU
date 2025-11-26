package myedu.oshsu.kg

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class MainViewModel : ViewModel() {
    // --- STATE: APP STATUS ---
    var appState by mutableStateOf("STARTUP")
    var currentTab by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    
    // --- STATE: THEME ---
    var themeMode by mutableStateOf("SYSTEM")
    val isGlass: Boolean get() = themeMode == "GLASS"

    // --- STATE: SETTINGS (DOCS) ---
    var downloadMode by mutableStateOf("IN_APP") // "IN_APP" or "WEBSITE"
    
    // --- STATE: USER DATA ---
    var userData by mutableStateOf<UserData?>(null)
    var profileData by mutableStateOf<StudentInfoResponse?>(null)
    var payStatus by mutableStateOf<PayStatusResponse?>(null)
    var newsList by mutableStateOf<List<NewsItem>>(emptyList())
    
    // --- STATE: SCHEDULE ---
    var fullSchedule by mutableStateOf<List<ScheduleItem>>(emptyList())
    var todayClasses by mutableStateOf<List<ScheduleItem>>(emptyList())
    var timeMap by mutableStateOf<Map<Int, String>>(emptyMap())
    var todayDayName by mutableStateOf("Today")
    var determinedStream by mutableStateOf<Int?>(null)
    var determinedGroup by mutableStateOf<Int?>(null)
    var selectedClass by mutableStateOf<ScheduleItem?>(null)
    
    // NEW: Persist the selected day index (0=Mon, 5=Sat) so it doesn't reset on back press
    var selectedScheduleDay by mutableStateOf(run {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Convert Sunday(1)..Saturday(7) to Mon(0)..Sat(5). Sunday defaults to Mon.
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
    var webDocumentUrl by mutableStateOf<String?>(null)
    
    // --- STATE: PDF GENERATION ---
    var isPdfGenerating by mutableStateOf(false)
    var pdfStatusMessage by mutableStateOf<String?>(null)

    // --- SETTINGS: DICTIONARY ---
    var dictionaryUrl by mutableStateOf("https://gist.githubusercontent.com/Placeholder6/71c6a6638faf26c7858d55a1e73b7aef/raw/myedudictionary.json")
    private var cachedDictionary: Map<String, String> = emptyMap()

    private var prefs: PrefsManager? = null
    
    // --- RESOURCES ---
    private val jsFetcher = JsResourceFetcher()
    private val refFetcher = ReferenceJsFetcher()
    private val dictUtils = DictionaryUtils()
    private var cachedResourcesRu: PdfResources? = null
    private var cachedResourcesEn: PdfResources? = null
    private var cachedRefResourcesRu: ReferenceResources? = null
    private var cachedRefResourcesEn: ReferenceResources? = null

    // --- HELPER ---
    fun getAuthToken(): String? = prefs?.getToken()

    // --- INIT ---
    fun initSession(context: Context) {
        if (prefs == null) prefs = PrefsManager(context)
        val token = prefs?.getToken()
        
        // Load Settings
        val savedTheme = prefs?.loadData("theme_mode_pref", String::class.java)
        if (savedTheme != null) themeMode = savedTheme
        
        val savedDocMode = prefs?.loadData("doc_download_mode", String::class.java)
        if (savedDocMode != null) downloadMode = savedDocMode

        if (token != null) {
            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token)
            loadOfflineData()
            appState = "APP"
            refreshAllData()
        } else {
            appState = "LOGIN"
        }
    }

    private fun loadOfflineData() {
        prefs?.let { p ->
            userData = p.loadData("user_data", UserData::class.java)
            profileData = p.loadData("profile_data", StudentInfoResponse::class.java)
            payStatus = p.loadData("pay_status", PayStatusResponse::class.java)
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

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true; errorMsg = null; NetworkClient.cookieJar.clear(); NetworkClient.interceptor.authToken = null
            try {
                val resp = withContext(Dispatchers.IO) { NetworkClient.api.login(LoginRequest(email.trim(), pass.trim())) }
                val token = resp.authorisation?.token
                if (token != null) {
                    prefs?.saveToken(token)
                    NetworkClient.interceptor.authToken = token
                    NetworkClient.cookieJar.injectSessionCookies(token)
                    refreshAllData()
                    appState = "APP"
                } else errorMsg = "Incorrect credentials"
            } catch (e: Exception) { errorMsg = "Login Failed: ${e.message}" }
            isLoading = false
        }
    }

    fun logout() {
        appState = "LOGIN"; currentTab = 0; userData = null; profileData = null; payStatus = null
        newsList = emptyList(); fullSchedule = emptyList(); sessionData = emptyList(); transcriptData = emptyList()
        prefs?.clearAll(); NetworkClient.cookieJar.clear(); NetworkClient.interceptor.authToken = null
        // Restore settings
        prefs?.saveData("theme_mode_pref", themeMode)
        prefs?.saveData("doc_download_mode", downloadMode)
    }

    private fun refreshAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isLoading = true }
            try {
                val user = NetworkClient.api.getUser().user
                val profile = NetworkClient.api.getProfile()
                withContext(Dispatchers.Main) {
                    userData = user; profileData = profile
                    prefs?.saveData("user_data", user); prefs?.saveData("profile_data", profile)
                }
                if (profile != null) {
                    try { val news = NetworkClient.api.getNews(); withContext(Dispatchers.Main) { newsList = news; prefs?.saveList("news_list", news) } } catch (_: Exception) {}
                    try { val pay = NetworkClient.api.getPayStatus(); withContext(Dispatchers.Main) { payStatus = pay; prefs?.saveData("pay_status", pay) } } catch (_: Exception) {}
                    loadScheduleNetwork(profile)
                    fetchSession(profile)
                }
            } catch (e: Exception) {
                if (e.message?.contains("401") == true) { withContext(Dispatchers.Main) { logout() } }
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
        determinedStream = fullSchedule.asSequence().filter { it.subject_type?.get() == "Lecture" }.mapNotNull { it.stream?.numeric }.firstOrNull()
        determinedGroup = fullSchedule.asSequence().filter { it.subject_type?.get() == "Practical Class" }.mapNotNull { it.stream?.numeric }.firstOrNull()
        val cal = Calendar.getInstance()
        todayDayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Today"
        val apiDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6 else cal.get(Calendar.DAY_OF_WEEK) - 2
        todayClasses = fullSchedule.filter { it.day == apiDay }
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
            } catch (_: Exception) {} finally { withContext(Dispatchers.Main) { isTranscriptLoading = false } }
        }
    }
    
    fun getTimeString(lessonId: Int) = timeMap[lessonId] ?: "Pair $lessonId"

    private suspend fun fetchDictionaryIfNeeded() {
        if (cachedDictionary.isEmpty() && dictionaryUrl.isNotBlank()) {
            cachedDictionary = dictUtils.fetchDictionary(dictionaryUrl)
        }
    }
    
    private fun getFormattedFileName(docType: String, lang: String? = null): String {
        val last = userData?.last_name ?: ""
        val first = userData?.name ?: ""
        val cleanName = "$last $first".trim().replace(" ", "_").replace(".", "")
        val suffix = if (lang != null) "_$lang" else ""
        return "${cleanName}_${docType}${suffix}.pdf"
    }

    fun generateTranscriptPdf(context: Context, language: String) {
        if (isPdfGenerating) return
        val studentId = userData?.id ?: return
        viewModelScope.launch {
            isPdfGenerating = true
            pdfStatusMessage = "Preparing Transcript ($language)..."
            try {
                fetchDictionaryIfNeeded()
                var resources = if (language == "en") cachedResourcesEn else cachedResourcesRu
                if (resources == null) {
                    pdfStatusMessage = "Fetching Scripts..."
                    resources = jsFetcher.fetchResources({ println(it) }, language, cachedDictionary)
                    if (language == "en") cachedResourcesEn = resources else cachedResourcesRu = resources
                }
                val infoRaw = withContext(Dispatchers.IO) { NetworkClient.api.getStudentInfoRaw(studentId).string() }
                val infoJson = JSONObject(infoRaw)
                infoJson.put("fullName", "${infoJson.optString("last_name")} ${infoJson.optString("name")} ${infoJson.optString("father_name")}".replace("null", "").trim())
                
                val movId = profileData?.studentMovement?.id ?: 0L
                val transcriptRaw = withContext(Dispatchers.IO) { NetworkClient.api.getTranscriptDataRaw(studentId, movId).string() }
                val keyRaw = withContext(Dispatchers.IO) { NetworkClient.api.getTranscriptLink(DocIdRequest(studentId)).string() }
                val keyObj = JSONObject(keyRaw)
                
                pdfStatusMessage = "Generating PDF..."
                val bytes = WebPdfGenerator(context).generatePdf(infoJson.toString(), transcriptRaw, keyObj.optLong("id"), keyObj.optString("url"), resources!!, language, cachedDictionary) { println(it) }
                
                // SAVE AND OPEN
                val filename = getFormattedFileName("Transcript", language)
                saveToDownloads(context, bytes, filename)
                
                pdfStatusMessage = null
            } catch (e: Exception) {
                pdfStatusMessage = "Error: ${e.message}"; e.printStackTrace(); delay(3000); pdfStatusMessage = null
            } finally { isPdfGenerating = false }
        }
    }

    fun generateReferencePdf(context: Context, language: String) {
        if (isPdfGenerating) return
        val studentId = userData?.id ?: return
        viewModelScope.launch {
            isPdfGenerating = true
            pdfStatusMessage = "Preparing Reference ($language)..."
            try {
                fetchDictionaryIfNeeded()
                var resources = if (language == "en") cachedRefResourcesEn else cachedRefResourcesRu
                if (resources == null) {
                    pdfStatusMessage = "Fetching Scripts..."
                    resources = refFetcher.fetchResources({ println(it) }, language, cachedDictionary)
                    if (language == "en") cachedRefResourcesEn = resources else cachedRefResourcesRu = resources
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
                
                pdfStatusMessage = "Generating PDF..."
                val bytes = ReferencePdfGenerator(context).generatePdf(infoJson.toString(), licenseRaw, univRaw, linkObj.optLong("id"), linkObj.optString("url"), resources!!, prefs?.getToken() ?: "", language, cachedDictionary) { println(it) }
                
                // SAVE AND OPEN
                val filename = getFormattedFileName("Reference", language)
                saveToDownloads(context, bytes, filename)
                
                pdfStatusMessage = null
            } catch (e: Exception) {
                pdfStatusMessage = "Error: ${e.message}"; e.printStackTrace(); delay(3000); pdfStatusMessage = null
            } finally { isPdfGenerating = false }
        }
    }

    private suspend fun saveToDownloads(context: Context, bytes: ByteArray, filename: String) {
        try {
            pdfStatusMessage = "Saving to Downloads..."
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            // Handle duplicate filenames
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

            // --- OPEN PDF AUTOMATICALLY ---
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
                try {
                    // SECURELY GET URI via FileProvider
                    val authority = "${context.packageName}.provider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "application/pdf")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            pdfStatusMessage = "Save Failed: ${e.message}"
            delay(2000)
        }
    }
}
