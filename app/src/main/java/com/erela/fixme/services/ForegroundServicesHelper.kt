package com.erela.fixme.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.erela.fixme.R
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.WebSocketClient
import com.erela.fixme.objects.NotificationResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import org.json.JSONObject

@OptIn(DelicateCoroutinesApi::class)
class ForegroundServicesHelper : Service() {
    private lateinit var webSocketClient: WebSocketClient

    companion object {
        const val CHANNEL_ID = "Foreground Service ID"
        const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            while (true) {
                Log.e("SERVICES", "Service is running..")
                webSocketClient = WebSocketClient.getInstance()
                webSocketClient.setSocketUrl(InitAPI.SOCKET_URL)
                webSocketClient.setListener(object : WebSocketClient.SocketListener {
                    override fun onMessage(message: String) {
                        Log.e("Message", message)
                        val jsonObject = JSONObject(message)
                        val notification: NotificationResponse = NotificationResponse(
                            jsonObject.getInt("expires") ?: null,
                            jsonObject.getString("topic"),
                            jsonObject.getString("id"),
                            jsonObject.getInt("time"),
                            jsonObject.getString("event"),
                            jsonObject.getString("message") ?: null
                        )
                        showNotification(notification.message.toString())
                    }
                })
                webSocketClient.connect()
                Thread.sleep(30000)
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "Erela FixMe", NotificationManager.IMPORTANCE_DEFAULT
            ).also {
                it.description = "Description"
            }
        )
    }

    private fun showNotification(message: String) {
        createNotification()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentText(message)
            .setContentTitle("Erela FixMe")
            .setSmallIcon(R.drawable.fixme_logo)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .build()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(
                NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        }
        /*val notification =
            NotificationCompat.Builder(this@ForegroundServicesHelper, CHANNEL_ID)
                .setSmallIcon(R.drawable.fixme_logo)
                .setContentTitle("Erela FixMe")
                .setPriority(Notification.PRIORITY_DEFAULT)

        if (ActivityCompat.checkSelfPermission(
                this@ForegroundServicesHelper, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification)
            } else {
                startForeground(
                    NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                )
            }
        }
        NotificationManagerCompat.from(this@ForegroundServicesHelper)
            .notify(1, notification.build())*/
    }
}