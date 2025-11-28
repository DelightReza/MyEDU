package myedu.oshsu.kg

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class ScheduleAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotifications(schedule: List<ScheduleItem>, timeMap: Map<Int, String>, language: String) {
        cancelAllAlarms()
        scheduleEveningSummary(schedule, language)
        scheduleClassReminders(schedule, timeMap, language)
    }

    private fun cancelAllAlarms() {
        val intent = Intent(context, NotificationReceiver::class.java)
        
        val summaryPending = PendingIntent.getBroadcast(
            context, 9999, intent, 
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (summaryPending != null) {
            alarmManager.cancel(summaryPending)
            summaryPending.cancel()
        }

        for (day in 0..6) {
            for (lesson in 1..15) {
                val id = day * 100 + lesson
                val pending = PendingIntent.getBroadcast(
                    context, id, intent, 
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pending != null) {
                    alarmManager.cancel(pending)
                    pending.cancel()
                }
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleEveningSummary(schedule: List<ScheduleItem>, language: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 20)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.timeInMillis < System.currentTimeMillis()) return

        val tomorrowCal = Calendar.getInstance()
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
        val javaDay = tomorrowCal.get(Calendar.DAY_OF_WEEK)
        val apiDay = if (javaDay == Calendar.SUNDAY) 6 else javaDay - 2

        val nextDayClasses = schedule.filter { it.day == apiDay }
        
        if (nextDayClasses.isNotEmpty()) {
            val count = nextDayClasses.size
            val firstClass = nextDayClasses.first().subject?.get(language) ?: "Class"
            
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TITLE", context.getString(R.string.notif_tomorrow_title))
                putExtra("MESSAGE", context.getString(R.string.notif_tomorrow_msg, count, firstClass))
                putExtra("ID", 9999) 
            }

            setAlarm(calendar.timeInMillis, 9999, intent)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleClassReminders(schedule: List<ScheduleItem>, timeMap: Map<Int, String>, language: String) {
        val now = System.currentTimeMillis()

        schedule.forEach { item ->
            val timeString = timeMap[item.id_lesson] ?: return@forEach
            val startTime = timeString.split("-").firstOrNull()?.trim() ?: return@forEach
            val parts = startTime.split(":")
            if (parts.size < 2) return@forEach
            
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            val classCal = Calendar.getInstance()
            val currentJavaDay = classCal.get(Calendar.DAY_OF_WEEK) 
            val currentApiDay = if (currentJavaDay == Calendar.SUNDAY) 6 else currentJavaDay - 2
            
            val dayDiff = item.day - currentApiDay
            classCal.add(Calendar.DAY_OF_YEAR, dayDiff)
            classCal.set(Calendar.HOUR_OF_DAY, hour)
            classCal.set(Calendar.MINUTE, minute)
            classCal.set(Calendar.SECOND, 0)

            classCal.add(Calendar.HOUR_OF_DAY, -1)

            if (classCal.timeInMillis < now) {
                classCal.add(Calendar.DAY_OF_YEAR, 7)
            }

            // --- DATA PREPARATION ---
            val subjectName = item.subject?.get(language) ?: "Class"
            val roomName = item.room?.name_en ?: "?"
            
            // Build Location String (Building Name + Address)
            val bName = item.classroom?.building?.getName(language) ?: ""
            val bAddr = item.classroom?.building?.getAddress(language) ?: ""
            
            val locationStr = when {
                bName.isNotBlank() && bAddr.isNotBlank() -> "$bName, $bAddr"
                bName.isNotBlank() -> bName
                bAddr.isNotBlank() -> bAddr
                else -> "Campus"
            }
            // ------------------------

            // Pass 4 arguments to the string resource
            val msg = context.getString(R.string.notif_class_msg, subjectName, startTime, locationStr, roomName)

            val id = item.day * 100 + item.id_lesson

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TITLE", context.getString(R.string.notif_class_title))
                putExtra("MESSAGE", msg)
                putExtra("ID", id)
            }

            setAlarm(classCal.timeInMillis, id, intent)
        }
    }

    private fun setAlarm(timeInMillis: Long, reqCode: Int, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, reqCode, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }
}
