package kg.oshsu.myedu

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class MainViewModel : ViewModel() {
    // --- STATE: APP STATUS ---
    var appState by mutableStateOf("STARTUP")
    var currentTab by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    
    // --- STATE: USER DATA ---
    var userData by mutableStateOf<UserData?>(null)
    var profileData by mutableStateOf<StudentInfoResponse?>(null)
    var payStatus by mutableStateOf<PayStatusResponse?>(null)
    var newsList by mutableStateOf<List<NewsItem>>(emptyList())
    
    // --- STATE: SCHEDULE DATA ---
    var fullSchedule by mutableStateOf<List<ScheduleItem>>(emptyList())
    var todayClasses by mutableStateOf<List<ScheduleItem>>(emptyList())
    var timeMap by mutableStateOf<Map<Int, String>>(emptyMap())
    var todayDayName by mutableStateOf("Today")
    var determinedStream by mutableStateOf<Int?>(null)
    var determinedGroup by mutableStateOf<Int?>(null)
    var selectedClass by mutableStateOf<ScheduleItem?>(null)
    
    // --- STATE: GRADES DATA ---
    var sessionData by mutableStateOf<List<SessionResponse>>(emptyList())
    var isGradesLoading by mutableStateOf(false)
    
    // --- STATE: DOCUMENTS & NAVIGATION ---
    // Native Preview Flags
    var showTranscriptScreen by mutableStateOf(false)
    var showReferenceScreen by mutableStateOf(false)
    
    // Web Redirect URL (When user clicks Print)
    var webDocumentUrl by mutableStateOf<String?>(null)
    
    // Native Preview Data
    var transcriptData by mutableStateOf<List<TranscriptYear>>(emptyList())
    var isTranscriptLoading by mutableStateOf(false)

    private var prefs: PrefsManager? = null

    // --- HELPER: Get Token for WebView ---
    fun getAuthToken(): String? {
        return prefs?.getToken()
    }

    // --- INIT: CHECK SESSION ---
    fun initSession(context: Context) {
        if (prefs == null) {
            prefs = PrefsManager(context)
        }
        val token = prefs?.getToken()
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

    // --- AUTH: LOGIN LOGIC ---
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

    // --- AUTH: LOGOUT LOGIC ---
    fun logout() {
        appState = "LOGIN"; currentTab = 0; userData = null; profileData = null; payStatus = null; newsList = emptyList(); fullSchedule = emptyList(); sessionData = emptyList(); transcriptData = emptyList()
        prefs?.clearAll(); NetworkClient.cookieJar.clear(); NetworkClient.interceptor.authToken = null
    }

    // --- DATA: REFRESH ALL ---
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

    // --- SCHEDULE: FETCH ---
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

    // --- SCHEDULE: PROCESS LOCAL ---
    private fun processScheduleLocally() {
        if (fullSchedule.isEmpty()) return
        determinedStream = fullSchedule.asSequence().filter { it.subject_type?.get() == "Lecture" }.mapNotNull { it.stream?.numeric }.firstOrNull()
        determinedGroup = fullSchedule.asSequence().filter { it.subject_type?.get() == "Practical Class" }.mapNotNull { it.stream?.numeric }.firstOrNull()
        val cal = Calendar.getInstance()
        todayDayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Today"
        val apiDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6 else cal.get(Calendar.DAY_OF_WEEK) - 2
        todayClasses = fullSchedule.filter { it.day == apiDay }
    }

    // --- GRADES: FETCH SESSION ---
    private fun fetchSession(profile: StudentInfoResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isGradesLoading = true }
                val session = NetworkClient.api.getSession(profile.active_semester ?: 1)
                withContext(Dispatchers.Main) { sessionData = session; prefs?.saveList("session_list", session) }
            } catch (_: Exception) {} finally { withContext(Dispatchers.Main) { isGradesLoading = false } }
        }
    }

    // --- DOCUMENTS: TRANSCRIPT FETCH (FOR NATIVE PREVIEW) ---
    fun fetchTranscript() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { 
                    isTranscriptLoading = true
                    showTranscriptScreen = true
                    // Load cache first
                    transcriptData = prefs?.loadList<TranscriptYear>("transcript_list") ?: emptyList() 
                }
                val uid = userData?.id ?: return@launch
                val movId = profileData?.studentMovement?.id ?: return@launch 
                val transcript = NetworkClient.api.getTranscript(uid, movId)
                withContext(Dispatchers.Main) { 
                    transcriptData = transcript
                    prefs?.saveList("transcript_list", transcript) 
                }
            } catch (_: Exception) {} finally { withContext(Dispatchers.Main) { isTranscriptLoading = false } }
        }
    }
    
    fun getTimeString(lessonId: Int) = timeMap[lessonId] ?: "Pair $lessonId"
}
