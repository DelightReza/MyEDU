package myedu.oshsu.kg

import android.content.Context
import android.content.res.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class BackgroundSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val success = runSyncTask(retryAuth = true)
        if (success) Result.success() else Result.retry()
    }

    private suspend fun runSyncTask(retryAuth: Boolean): Boolean {
        try {
            val context = applicationContext
            val prefs = PrefsManager(context)
            var token = prefs.getToken()

            if (token == null && retryAuth) {
                if (attemptBgLogin(prefs)) token = prefs.getToken() else return false
            } else if (token == null) return false

            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token!!)

            val userResponse = try { NetworkClient.api.getUser() } catch (e: Exception) {
                if (retryAuth && (e.message?.contains("401") == true)) {
                    if (attemptBgLogin(prefs)) return runSyncTask(retryAuth = false)
                }
                return false
            }
            val profile = try { NetworkClient.api.getProfile() } catch (e: Exception) { return false }

            prefs.saveData("user_data", userResponse.user)
            prefs.saveData("profile_data", profile)

            try { val news = NetworkClient.api.getNews(); prefs.saveList("news_list", news) } catch (_: Exception) { }
            try { val pay = NetworkClient.api.getPayStatus(); prefs.saveData("pay_status", pay) } catch (_: Exception) { }

            try {
                val oldSession = prefs.loadList<SessionResponse>("session_list")
                val activeSemester = profile.active_semester ?: 1
                val newSession = NetworkClient.api.getSession(activeSemester)

                if (oldSession.isNotEmpty() && newSession.isNotEmpty()) {
                    val localizedContext = NotificationHelper.getLocalizedContext(context, prefs)
                    val (gradeUpdates, portalUpdates) = NotificationHelper.checkForUpdates(oldSession, newSession, localizedContext)
                    if (gradeUpdates.isNotEmpty()) NotificationHelper.sendNotification(localizedContext, gradeUpdates, isPortalOpening = false)
                    if (portalUpdates.isNotEmpty()) NotificationHelper.sendNotification(localizedContext, portalUpdates, isPortalOpening = true)
                }
                prefs.saveList("session_list", newSession)
            } catch (e: Exception) { e.printStackTrace() }

            val mov = profile.studentMovement
            if (mov != null) {
                try {
                    val years = NetworkClient.api.getYears()
                    val activeYearId = years.find { it.active }?.id ?: AcademicYearHelper.getDefaultActiveYearId()
                    val times = try { NetworkClient.api.getLessonTimes(mov.id_speciality!!, mov.id_edu_form!!, activeYearId) } catch (e: Exception) { emptyList() }
                    val wrappers = NetworkClient.api.getSchedule(mov.id_speciality!!, mov.id_edu_form!!, activeYearId, profile.active_semester ?: 1)
                    val fullSchedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }

                    if (fullSchedule.isNotEmpty()) {
                        prefs.saveList("schedule_list", fullSchedule)
                        if (times.isNotEmpty()) {
                            val timeMap = times.associate { (it.lesson?.num ?: 0) to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                            val localizedContext = NotificationHelper.getLocalizedContext(context, prefs)
                            ScheduleAlarmManager(localizedContext).scheduleNotifications(fullSchedule, timeMap, prefs.loadData("language_pref", String::class.java)?.replace("\"", "") ?: "en")
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            return true
        } catch (e: Exception) { return false }
    }

    private suspend fun attemptBgLogin(prefs: PrefsManager): Boolean {
        val isRemember = prefs.loadData("pref_remember_me", Boolean::class.java) ?: false
        if (!isRemember) return false
        val email = prefs.loadData("pref_saved_email", String::class.java) ?: ""
        val pass = prefs.loadData("pref_saved_pass", String::class.java) ?: ""
        if (email.isBlank() || pass.isBlank()) return false
        return try {
            val resp = NetworkClient.api.login(LoginRequest(email.trim(), pass.trim()))
            val token = resp.authorisation?.token
            if (token != null) {
                prefs.saveToken(token)
                NetworkClient.interceptor.authToken = token
                NetworkClient.cookieJar.injectSessionCookies(token)
                true
            } else false
        } catch (e: Exception) { false }
    }
}
