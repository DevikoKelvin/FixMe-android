package com.erela.fixme.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.erela.fixme.R
import com.erela.fixme.helpers.NotificationsHelper
import com.erela.fixme.helpers.UserDataHelper

class NotificationService : Service() {
    companion object {
        const val CHANNEL_ID_FOREGROUND = "Foreground Service ID"
        const val CHANNEL_ID = "FixMe Notification Channel"
        const val CHANNEL_NAME_FOREGROUND = "Erela FixMe Services"
        const val CHANNEL_NAME = "Erela FixMe"
        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID = 1002
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userData = UserDataHelper(this).getUserData()
        Thread {
            while (true) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID_FOREGROUND,
                        CHANNEL_NAME_FOREGROUND,
                        NotificationManager.IMPORTANCE_NONE
                    )
                getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
                val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
                    .setSmallIcon(R.drawable.fixme_logo)
                    .setContentText("FixMe is running on background")
                    .setContentTitle(CHANNEL_NAME_FOREGROUND)
                    .setAutoCancel(true)
                    .setSilent(true)
                    .build()

                startForeground(NOTIFICATION_ID_FOREGROUND, notificationBuilder)

                NotificationsHelper.receiveNotifications(applicationContext, userData)
                try {
                    Thread.sleep(30000)
                } catch (interruptedException: InterruptedException) {
                    interruptedException.printStackTrace()
                }
            }
        }.start()

        return super.onStartCommand(intent, flags, startId)
    }
}