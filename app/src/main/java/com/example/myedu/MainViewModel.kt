package com.example.myedu

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class MainViewModel : ViewModel() {
    // --- APP STATE ---
    var appState by mutableStateOf("LOGIN")
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

    // --- ACTIONS ---

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
                    NetworkClient.interceptor.authToken = token
                    NetworkClient.cookieJar.injectSessionCookies(token)
                    
                    userData = withContext(Dispatchers.IO) { NetworkClient.api.getUser().user }
                    profileData = withContext(Dispatchers.IO) { NetworkClient.api.getProfile() }
                    
                    if (profileData != null) {
                        loadDashboardData(profileData!!)
                        fetchSession(profileData!!)
                    }
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

    private fun loadDashboardData(profile: StudentInfoResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load independent data
                try { newsList = NetworkClient.api.getNews() } catch(e:Exception) {}
                try { payStatus = NetworkClient.api.getPayStatus() } catch(e:Exception) {}

                // Load Schedule
                val mov = profile.studentMovement
                if (mov?.id_speciality != null && mov.id_edu_form != null && profile.active_semester != null) {
                    val years = NetworkClient.api.getYears()
                    val activeYearId = years.find { it.active }?.id ?: 25
                    
                    val times = try {
                        NetworkClient.api.getLessonTimes(mov.id_speciality, mov.id_edu_form, activeYearId)
                    } catch (e:Exception) { emptyList() }
                    
                    timeMap = times.associate { 
                        (it.lesson?.num ?: 0) to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" 
                    }

                    val wrappers = NetworkClient.api.getSchedule(mov.id_speciality, mov.id_edu_form, activeYearId, profile.active_semester)
                    val allItems = wrappers.flatMap { it.schedule_items ?: emptyList() }
                    fullSchedule = allItems.sortedBy { it.id_lesson }
                    
                    // Heuristics for Group/Stream
                    determinedStream = fullSchedule.asSequence().filter { it.subject_type?.get() == "Lecture" }.mapNotNull { it.stream?.numeric }.firstOrNull()
                    determinedGroup = fullSchedule.asSequence().filter { it.subject_type?.get() == "Practical Class" }.mapNotNull { it.stream?.numeric }.firstOrNull()
                    
                    // Today's Classes
                    val cal = Calendar.getInstance()
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) 
                    todayDayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Today"
                    val apiDay = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                    todayClasses = fullSchedule.filter { it.day == apiDay }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    private fun fetchSession(profile: StudentInfoResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isGradesLoading = true
                val semId = profile.active_semester ?: return@launch
                sessionData = NetworkClient.api.getSession(semId)
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally { 
                isGradesLoading = false 
            }
        }
    }

    // --- TRANSCRIPT FETCH ---
    fun fetchTranscript() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isTranscriptLoading = true
                withContext(Dispatchers.Main) { showTranscriptScreen = true }
                
                val uid = userData?.id ?: return@launch
                val movId = profileData?.studentMovement?.id ?: return@launch 
                
                transcriptData = NetworkClient.api.getTranscript(uid, movId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isTranscriptLoading = false
            }
        }
    }
    
    fun getTimeString(lessonId: Int): String {
        return timeMap[lessonId] ?: "Pair $lessonId"
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
        NetworkClient.cookieJar.clear()
        NetworkClient.interceptor.authToken = null
    }
}
