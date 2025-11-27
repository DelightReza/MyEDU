package myedu.oshsu.kg

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class BackgroundSyncWorker(
    appContext: Context, 
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            val prefs = PrefsManager(context)
            val token = prefs.getToken()

            // 1. Check Auth
            if (token.isNullOrBlank()) {
                return@withContext Result.failure()
            }

            // 2. Setup Network
            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token)

            // --- STEP A: CORE DATA (User & Profile) ---
            val userResponse = try { NetworkClient.api.getUser() } catch (e: Exception) { return@withContext Result.retry() }
            val profile = try { NetworkClient.api.getProfile() } catch (e: Exception) { return@withContext Result.retry() }

            prefs.saveData("user_data", userResponse.user)
            prefs.saveData("profile_data", profile)

            val activeSemester = profile.active_semester ?: 1
            val mov = profile.studentMovement

            // --- STEP B: LIGHTWEIGHT DATA ---
            try { val news = NetworkClient.api.getNews(); prefs.saveList("news_list", news) } catch (_: Exception) { }
            try { val pay = NetworkClient.api.getPayStatus(); prefs.saveData("pay_status", pay) } catch (_: Exception) { }

            // --- STEP C: GRADES (Session) ---
            try {
                val oldSession = prefs.loadList<SessionResponse>("session_list")
                val newSession = NetworkClient.api.getSession(activeSemester)

                if (oldSession.isNotEmpty() && newSession.isNotEmpty()) {
                    // Check updates using localized context
                    val localizedContext = getLocalizedContext(context, prefs)
                    val updates = checkForUpdates(oldSession, newSession, localizedContext)
                    if (updates.isNotEmpty()) {
                        sendNotification(localizedContext, updates)
                    }
                }
                prefs.saveList("session_list", newSession)
            } catch (e: Exception) { e.printStackTrace() }

            // --- STEP D: SCHEDULE ---
            if (mov != null) {
                try {
                    val years = NetworkClient.api.getYears()
                    val activeYearId = years.find { it.active }?.id ?: 25
                    val times = try { NetworkClient.api.getLessonTimes(mov.id_speciality!!, mov.id_edu_form!!, activeYearId) } catch (e: Exception) { emptyList() }
                    val wrappers = NetworkClient.api.getSchedule(mov.id_speciality!!, mov.id_edu_form!!, activeYearId, activeSemester)
                    val fullSchedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }

                    if (fullSchedule.isNotEmpty()) {
                        prefs.saveList("schedule_list", fullSchedule)
                        if (times.isNotEmpty()) {
                            val timeMap = times.associate { (it.lesson?.num ?: 0) to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                            // Update Alarms using Localized Context
                            val localizedContext = getLocalizedContext(context, prefs)
                            val alarmManager = ScheduleAlarmManager(localizedContext)
                            // Pass language to scheduleNotifications so it can resolve NameObj correctly
                            val lang = prefs.loadData("language_pref", String::class.java)?.replace("\"", "") ?: "en"
                            alarmManager.scheduleNotifications(fullSchedule, timeMap, lang)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            return@withContext Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    private fun getLocalizedContext(context: Context, prefs: PrefsManager): Context {
        val lang = prefs.loadData("language_pref", String::class.java)?.replace("\"", "") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun checkForUpdates(oldList: List<SessionResponse>, newList: List<SessionResponse>, context: Context): List<String> {
        val updates = ArrayList<String>()
        val lang = context.resources.configuration.locales.get(0).language

        val oldMap = oldList.flatMap { it.subjects ?: emptyList() }
            .associate { (it.subject?.get(lang) ?: "Unknown") to it.marklist }

        newList.flatMap { it.subjects ?: emptyList() }.forEach { newSub ->
            val name = newSub.subject?.get(lang) ?: return@forEach
            val oldMarks = oldMap[name]
            val newMarks = newSub.marklist

            fun check(label: String, oldVal: Double?, newVal: Double?) {
                val v1 = oldVal ?: 0.0
                val v2 = newVal ?: 0.0
                if (v2 > v1 && v2 > 0) {
                    updates.add(context.getString(R.string.notif_grades_msg_single, name, label, v2.toInt()))
                }
            }

            if (oldMarks != null && newMarks != null) {
                check("M1", oldMarks.point1, newMarks.point1)
                check("M2", oldMarks.point2, newMarks.point2)
                check(context.getString(R.string.exam_short), oldMarks.finally, newMarks.finally)
            }
        }
        return updates
    }

    private fun sendNotification(context: Context, updates: List<String>) {
        val title = context.getString(R.string.notif_new_grades_title)
        val message = if (updates.size > 4) {
            context.getString(R.string.notif_grades_msg_multiple, updates.size)
        } else {
            updates.joinToString("\n")
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TITLE", title)
            putExtra("MESSAGE", message)
            putExtra("ID", 777)
        }
        context.sendBroadcast(intent)
    }
}
