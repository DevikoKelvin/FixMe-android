package com.erela.fixme.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.erela.fixme.R
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions

class ForegroundServicesHelper : Service() {
    private lateinit var pusher: Pusher
    private var data: String? = null

    companion object {
        const val CHANNEL_ID = "Foreground Service ID"
        const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            while (true) {
                Log.e("SERVICES", "Service is running..")
                pusher = Pusher("f8229f5de3071d56fadf", PusherOptions().setCluster("ap1"))
                pusher.connect()
                val channel = pusher.subscribe("my-channel")
                channel.bind("my-event") { event ->
                    Log.e("Event", event.data.toString())
                    data = event.data
                }
                if (data != null) {
                    val notification = createNotification(data!!)
                    startForeground(NOTIFICATION_ID, notification)
                } else {
                    val notification = createNotification("Service is running.")
                    startForeground(NOTIFICATION_ID, notification)
                }
                Thread.sleep(30000)
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /*override fun onCreate() {
        super.onCreate()

        pusher = Pusher("f8229f5de3071d56fadf", PusherOptions().setCluster("ap1"))
        pusher.connect()
        val channel = pusher.subscribe("my-channel")
        channel.bind("my-event") { event ->
            Log.e("Event", event.data.toString())
            data = event.data
        }
        if (data != null) {
            val notification = createNotification(data!!)
            startForeground(NOTIFICATION_ID, notification)
        } else {
            val notification = createNotification("Service is running.")
            startForeground(NOTIFICATION_ID, notification)
        }
    }*/

    private fun createNotification(message: String): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Erela FixMe",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Erela FixMe")
            .setContentText(message)
            .setSmallIcon(R.drawable.fixme_logo)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

        return notificationBuilder.build()
    }
}