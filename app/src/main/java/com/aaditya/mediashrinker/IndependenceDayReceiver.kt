package com.aaditya.mediashrinker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

// TEMPORARY — this whole file exists only for the 15th August greeting
// notifications. Safe to delete this file (and its manifest <receiver> entry
// and the scheduleIndependenceDayNotifications() call in MainActivity) in a
// future update once no longer needed.
class IndependenceDayReceiver : BroadcastReceiver() {

    companion object {
        // Same channel as ProcessingService's alerts channel, so this behaves
        // identically (heads-up / slide-down) as the compress/PDF notifications.
        private const val CHANNEL_ID = "media_shrinker_alerts_channel"
        private const val NOTIFICATION_ID_BASE = 9500

        fun scheduleIndependenceDayNotifications(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // (hour, minute, unique slot number)
            val times = listOf(
                Triple(0, 0, 1),   // 12:00 AM
                Triple(9, 0, 2),   // 9:00 AM
                Triple(15, 0, 3)   // 3:00 PM
            )

            for ((hour, minute, slot) in times) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.MONTH, Calendar.AUGUST)
                calendar.set(Calendar.DAY_OF_MONTH, 15)
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                // Skip scheduling anything that's already in the past (e.g. app
                // is opened for the first time after 15th August has passed).
                if (calendar.timeInMillis <= System.currentTimeMillis()) continue

                val intent = Intent(context, IndependenceDayReceiver::class.java)
                intent.putExtra("slot", slot)

                val pendingIntent = PendingIntent.getBroadcast(
                    context, slot, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Safety check — only show it if today really is 15th August. Protects
        // against a delayed alarm firing a bit late into the next day.
        val today = Calendar.getInstance()
        if (today.get(Calendar.MONTH) != Calendar.AUGUST || today.get(Calendar.DAY_OF_MONTH) != 15) {
            return
        }

        createChannelIfNeeded(context)

        val slot = intent.getIntExtra("slot", 0)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 100 + slot, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Happy Independence Day! 🇮🇳")
            .setContentText("Wishing you a proud and joyful Independence Day from MediaShrinker!")
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF3B82F6.toInt())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + slot, notification)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MediaShrinker Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
