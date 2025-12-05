package myedu.oshsu.kg

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TITLE") ?: context.getString(R.string.notif_default_title)
        val message = intent.getStringExtra("MESSAGE") ?: context.getString(R.string.notif_default_msg)
        val id = intent.getIntExtra("ID", 0)

        showAlarmNotification(context, title, message, id)
    }

    private fun showAlarmNotification(context: Context, title: String, message: String, id: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // CRITICAL: Changed ID to force Android to register new settings.
        // If you keep the old ID, Android ignores changes to sound/vibration.
        val channelId = "myedu_alarm_channel_v3" 

        // Use TYPE_ALARM to get the loud alarm sound instead of a short beep
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )

                // 1. Force the sound to play through the ALARM stream (bypasses silent mode)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM) 
                    .build()

                channel.setSound(soundUri, audioAttributes)
                
                // 2. Enable vibration and lights
                channel.enableVibration(true)
                channel.vibrationPattern = longArrayOf(0, 500, 200, 500)
                channel.enableLights(true)
                
                // 3. Bypass Do Not Disturb (Optional, requires special permission on some devices, 
                // but IMPORTANCE_HIGH + USAGE_ALARM usually works)
                // channel.setBypassDnd(true) 
                
                manager.createNotificationChannel(channel)
            }
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX) // Max priority for pre-Oreo
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Tells system this is an alarm
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri, AudioManager.STREAM_ALARM) // Force Stream for pre-Oreo
            .setVibrate(longArrayOf(0, 500, 200, 500))

        manager.notify(id, builder.build())
    }
}
