package com.example.myedu

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
    // --- APP STATE ---
    // CHANGED: Start with "STARTUP" to prevent Login UI flash
    var appState by mutableStateOf("STARTUP") 
    var currentTab by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    
    // --- USER DATA ---
    var userData by mutableStateOf<UserData?>(null)
    var profileData by mutableStateOf<StudentInfoResponse?>(null)
    
    // --- DASHBOARD ---
    var payStatus by mutableStateOf<PayStatusResponse?>(null)
    var newsList by mutableStateOf<List<NewsItem>>(emptyList())
    var fullSchedule by mutableStateOf<List<ScheduleItem>>(emptyList())
    var todayClasses by mutableStateOf<List<ScheduleItem>>(emptyList())
    var timeMap by mutableStateOf<Map<Int, String>>(emptyMap())
    var todayDayName by mutableStateOf("Today")
    
    // --- SCHEDULE HELPERS ---
    var determinedStream by mutableStateOf<Int?>(null)
    var determinedGroup by mutableStateOf<Int?>(null)
    var selectedClass by mutableStateOf<ScheduleItem?>(null)

    // --- GRADES / SESSION STATE ---
    var sessionData by mutableStateOf<List<SessionResponse>>(emptyList())
    var isGradesLoading by mutableStateOf(false)
    
    // --- TRANSCRIPT STATE ---
    var transcriptData by mutableStateOf<List<TranscriptYear>>(emptyList())
    var isTranscriptLoading by mutableStateOf(false)
    var showTranscriptScreen by mutableStateOf(false)

    // --- PERSISTENCE MANAGER ---
    private var prefs: PrefsManager? = null

    // ============================================================
    // 1. INITIALIZATION
    // ============================================================
    fun initSession(context: Context) {
        if (prefs == null) {
            prefs = PrefsManager(context)
            checkAutoLogin()
        }
    }

    private fun checkAutoLogin() {
        val token = prefs?.getToken()
        
        if (token != null) {
            // 1. Restore Network State
            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token)
            
            // 2. Load Offline Data
            loadOfflineData()
            
            // 3. Switch to APP (User sees dashboard immediately)
            appState = "APP"
            
            // 4. Fetch Fresh Data
            refreshAllData()
        } else {
            // CHANGED: No token found, NOW we show the login screen
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

    // ============================================================
    // 2. ACTIONS
    // ============================================================

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true
            errorMsg = null
            NetworkClient.cookieJar.clear()
            NetworkClient.interceptor.authToken = null
            try {
                val resp = withContext(Dispatchers.IO) { NetworkClient.api.login(LoginRequest(email.trim(), pass.trim())) }
                val token = resp.authorisation?.token
                
                if (token != null) {
                    prefs?.saveToken(token)
                    NetworkClient.interceptor.authToken = token
                    NetworkClient.cookieJar.injectSessionCookies(token)
                    
                    refreshAllData()
                    appState = "APP"
                } else {
                    errorMsg = "Incorrect credentials"
                }
            } catch (e: Exception) {
                errorMsg = "Login Failed: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun logout() {
        appState = "LOGIN"
        currentTab = 0
        userData = null
        profileData = null
        payStatus = null
        newsList = emptyList()
        fullSchedule = emptyList()
        timeMap = emptyMap()
        sessionData = emptyList()
        transcriptData = emptyList()
        selectedClass = null
        determinedStream = null
        determinedGroup = null
        
        prefs?.clearAll()
        NetworkClient.cookieJar.clear()
        NetworkClient.interceptor.authToken = null
    }

    // ============================================================
    // 3. DATA FETCHING
    // ============================================================

    private fun refreshAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = NetworkClient.api.getUser().user
                val profile = NetworkClient.api.getProfile()
                
                withContext(Dispatchers.Main) {
                    userData = user
                    profileData = profile
                    prefs?.saveData("user_data", user)
                    prefs?.saveData("profile_data", profile)
                }

                if (profile != null) {
                    try {
                        val news = NetworkClient.api.getNews()
                        withContext(Dispatchers.Main) { 
                            newsList = news 
                            prefs?.saveList("news_list", news)
                        }
                    } catch (e: Exception) { }

                    try {
                        val pay = NetworkClient.api.getPayStatus()
                        withContext(Dispatchers.Main) { 
                            payStatus = pay 
                            prefs?.saveData("pay_status", pay)
                        }
                    } catch (e: Exception) { }

                    loadScheduleNetwork(profile)
                    fetchSession(profile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadScheduleNetwork(profile: StudentInfoResponse) {
        val mov = profile.studentMovement ?: return
        if (mov.id_speciality != null && mov.id_edu_form != null && profile.active_semester != null) {
            try {
                val years = NetworkClient.api.getYears()
                val activeYearId = years.find { it.active }?.id ?: 25
                
                val times = try {
                    NetworkClient.api.getLessonTimes(mov.id_speciality, mov.id_edu_form, activeYearId)
                } catch (e: Exception) { emptyList() }

                val wrappers = NetworkClient.api.getSchedule(mov.id_speciality, mov.id_edu_form, activeYearId, profile.active_semester)
                val allItems = wrappers.flatMap { it.schedule_items ?: emptyList() }
                
                withContext(Dispatchers.Main) {
                    timeMap = times.associate { (it.lesson?.num ?: 0) to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                    fullSchedule = allItems.sortedBy { it.id_lesson }
                    prefs?.saveList("schedule_list", fullSchedule)
                    processScheduleLocally()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun processScheduleLocally() {
        if (fullSchedule.isEmpty()) return
        determinedStream = fullSchedule.asSequence().filter { it.subject_type?.get() == "Lecture" }.mapNotNull { it.stream?.numeric }.firstOrNull()
        determinedGroup = fullSchedule.asSequence().filter { it.subject_type?.get() == "Practical Class" }.mapNotNull { it.stream?.numeric }.firstOrNull()
        
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) 
        todayDayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Today"
        val apiDay = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
        todayClasses = fullSchedule.filter { it.day == apiDay }
    }
    
    private fun fetchSession(profile: StudentInfoResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isGradesLoading = true }
                val semId = profile.active_semester ?: return@launch
                val session = NetworkClient.api.getSession(semId)
                withContext(Dispatchers.Main) {
                    sessionData = session
                    prefs?.saveList("session_list", session)
                }
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally { 
                withContext(Dispatchers.Main) { isGradesLoading = false } 
            }
        }
    }

    fun fetchTranscript() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { 
                    isTranscriptLoading = true
                    showTranscriptScreen = true
                    val cached = prefs?.loadList<TranscriptYear>("transcript_list")
                    if (!cached.isNullOrEmpty()) transcriptData = cached
                }
                
                val uid = userData?.id ?: return@launch
                val movId = profileData?.studentMovement?.id ?: return@launch 
                
                val transcript = NetworkClient.api.getTranscript(uid, movId)
                withContext(Dispatchers.Main) {
                    transcriptData = transcript
                    prefs?.saveList("transcript_list", transcript)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { isTranscriptLoading = false }
            }
        }
    }
    
    fun getTimeString(lessonId: Int): String {
        return timeMap[lessonId] ?: "Pair $lessonId"
    }
}
