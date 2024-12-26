package com.erela.fixme.services

import com.erela.fixme.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
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
                val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
                    .setSmallIcon(R.drawable.fixme_logo)
                    .setContentText("FixMe is running on background")
                    .setContentTitle(CHANNEL_NAME_FOREGROUND)
                    .setAutoCancel(true)
                    .setSilent(true)
                    .setPriority(Notification.PRIORITY_MIN)
                    .build()
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_NONE
                    )
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                startForeground(NOTIFICATION_ID_FOREGROUND, notificationBuilder)

                NotificationsHelper.receiveNotifications(applicationContext, userData)
                try {
                    Thread.sleep(2000)
                } catch (interruptedException: InterruptedException) {
                    interruptedException.printStackTrace()
                }
            }
        }.start()

        return super.onStartCommand(intent, flags, startId)
    }
}