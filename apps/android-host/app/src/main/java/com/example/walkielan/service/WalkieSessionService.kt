package com.example.walkielan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.walkielan.R

class WalkieSessionService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        runCatching {
            startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.walkie_notification_text)))
        }.onFailure { error ->
            Log.e(TAG, "Nao foi possivel iniciar o foreground service.", error)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_UPDATE -> {
                val roomName = intent.getStringExtra(EXTRA_ROOM_NAME).orEmpty()
                val notificationText = if (roomName.isBlank()) {
                    getString(R.string.walkie_notification_text)
                } else {
                    "Sala ativa: $roomName"
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(notificationText))
            }
        }
        return START_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.walkie_notification_title),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setContentTitle(getString(R.string.walkie_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "WalkieSessionService"
        private const val CHANNEL_ID = "walkie-session"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_UPDATE = "walkie.update"
        private const val ACTION_STOP = "walkie.stop"
        private const val EXTRA_ROOM_NAME = "room_name"

        fun start(context: Context, roomName: String) {
            val intent = Intent(context, WalkieSessionService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_ROOM_NAME, roomName)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WalkieSessionService::class.java))
        }
    }
}
