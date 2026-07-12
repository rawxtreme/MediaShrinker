package com.aaditya.mediashrinker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * A Foreground Service used to keep heavy work (bulk compression, PDF creation)
 * alive even if the user leaves the app (home button, switches apps, etc).
 *
 * Without this, aggressive battery optimizers on some phones (Xiaomi, Vivo, Oppo)
 * can silently kill the app's background work mid-task, leaving a half-finished
 * PDF or an incomplete batch compression with no error shown to the user.
 *
 * A Foreground Service tells Android "this is important, don't kill it" and shows
 * a persistent notification with live progress while it runs.
 */
class ProcessingService : Service() {

    companion object {
        private const val CHANNEL_ID = "media_shrinker_processing_channel"
        private const val NOTIFICATION_ID = 9001

        // Holds a reference to the currently running service instance (if any),
        // so MainActivity can push progress updates into the notification
        // without needing to bind/unbind the service.
        private var instance: ProcessingService? = null

        fun start(context: Context, title: String) {
            val intent = Intent(context, ProcessingService::class.java)
            intent.putExtra("title", title)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(current: Int, total: Int, subText: String) {
            instance?.updateNotificationProgress(current, total, subText)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProcessingService::class.java))
        }

        // Posts a separate, dismissible notification once work finishes.
        // Unlike the ongoing progress notification, this one survives after
        // the service stops and stays until the user taps or swipes it away.
        fun showCompletionNotification(context: Context, title: String, message: String, taskType: String) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("completed_task", taskType)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID + 1, notification)
        }
    }

    private var notificationTitle = "MediaShrinker"

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationTitle = intent?.getStringExtra("title") ?: "MediaShrinker"
        startForeground(NOTIFICATION_ID, buildNotification(0, 100, "Starting..."))
        return START_NOT_STICKY
    }

    private fun updateNotificationProgress(current: Int, total: Int, subText: String) {
        val notification = buildNotification(current, total, subText)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(current: Int, total: Int, subText: String): Notification {
        // Tapping this notification brings the app back to the exact screen
        // where the process is running, instead of just launching a blank app icon.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(subText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(total.coerceAtLeast(1), current, false)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MediaShrinker Processing",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Shows progress while compressing images or creating PDFs"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
