package myedu.oshsu.kg

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            // We fetch these first because we need the IDs (Semester/Speciality) for the rest.
            
            val userResponse = try {
                NetworkClient.api.getUser()
            } catch (e: Exception) {
                // If core auth fails, likely token expired or server down. Retry later.
                return@withContext Result.retry()
            }
            
            val profile = try {
                NetworkClient.api.getProfile()
            } catch (e: Exception) {
                return@withContext Result.retry()
            }

            // Save Core Data
            prefs.saveData("user_data", userResponse.user)
            prefs.saveData("profile_data", profile)

            val activeSemester = profile.active_semester ?: 1
            val mov = profile.studentMovement

            // --- STEP B: LIGHTWEIGHT DATA (News & Pay) ---
            // We wrap these in try-catch blocks so a failure here doesn't stop the important stuff (Grades/Schedule)
            
            try {
                val news = NetworkClient.api.getNews()
                prefs.saveList("news_list", news)
            } catch (_: Exception) { }

            try {
                val pay = NetworkClient.api.getPayStatus()
                prefs.saveData("pay_status", pay)
            } catch (_: Exception) { }


            // --- STEP C: GRADES (Session) ---
            try {
                // 1. Load Old Grades (to compare)
                val oldSession = prefs.loadList<SessionResponse>("session_list")

                // 2. Fetch New Grades
                val newSession = NetworkClient.api.getSession(activeSemester)

                // 3. Compare and Notify
                if (oldSession.isNotEmpty() && newSession.isNotEmpty()) {
                    val updates = checkForUpdates(oldSession, newSession)
                    if (updates.isNotEmpty()) {
                        sendNotification(context, updates)
                    }
                }

                // 4. Save New Grades
                prefs.saveList("session_list", newSession)
            } catch (e: Exception) {
                e.printStackTrace()
            }


            // --- STEP D: SCHEDULE ---
            if (mov != null) {
                try {
                    // 1. Get Active Year
                    val years = NetworkClient.api.getYears()
                    val activeYearId = years.find { it.active }?.id ?: 25

                    // 2. Get Lesson Times (Required for Alarms)
                    val times = try {
                        NetworkClient.api.getLessonTimes(mov.id_speciality!!, mov.id_edu_form!!, activeYearId)
                    } catch (e: Exception) { emptyList() }

                    // 3. Get Schedule
                    val wrappers = NetworkClient.api.getSchedule(mov.id_speciality!!, mov.id_edu_form!!, activeYearId, activeSemester)
                    val fullSchedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }

                    // 4. Save & Update Alarms
                    if (fullSchedule.isNotEmpty()) {
                        // Save to Cache
                        prefs.saveList("schedule_list", fullSchedule)

                        // Update Alarms immediately (This cancels old ones and sets new ones)
                        if (times.isNotEmpty()) {
                            val timeMap = times.associate { (it.lesson?.num ?: 0) to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                            val alarmManager = ScheduleAlarmManager(context)
                            alarmManager.scheduleNotifications(fullSchedule, timeMap)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return@withContext Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    // --- HELPER: Compare Grade Lists ---
    private fun checkForUpdates(oldList: List<SessionResponse>, newList: List<SessionResponse>): List<String> {
        val updates = ArrayList<String>()

        val oldMap = oldList.flatMap { it.subjects ?: emptyList() }
            .associate { (it.subject?.get() ?: "Unknown") to it.marklist }

        newList.flatMap { it.subjects ?: emptyList() }.forEach { newSub ->
            val name = newSub.subject?.get() ?: return@forEach
            val oldMarks = oldMap[name]
            val newMarks = newSub.marklist

            fun check(label: String, oldVal: Double?, newVal: Double?) {
                val v1 = oldVal ?: 0.0
                val v2 = newVal ?: 0.0
                if (v2 > v1 && v2 > 0) {
                    updates.add("$name: $label ${v2.toInt()}")
                }
            }

            if (oldMarks != null && newMarks != null) {
                check("M1", oldMarks.point1, newMarks.point1)
                check("M2", oldMarks.point2, newMarks.point2)
                check("Exam", oldMarks.finally, newMarks.finally)
            }
        }
        return updates
    }

    // --- HELPER: Send Notification ---
    private fun sendNotification(context: Context, updates: List<String>) {
        val title = "New Grades Posted"
        val message = if (updates.size > 4) {
            "${updates.size} subjects have updated scores."
        } else {
            updates.joinToString("\n")
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TITLE", title)
            putExtra("MESSAGE", message)
            putExtra("ID", 777) // Unique ID for Grades
        }
        context.sendBroadcast(intent)
    }
}
