package com.erela.fixme.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.erela.fixme.R
import com.erela.fixme.helpers.InitAPI
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONException
import org.json.JSONObject

@OptIn(DelicateCoroutinesApi::class)
class ForegroundServicesHelper : Service() {
    private lateinit var webSocket: WebSocket

    companion object {
        const val CHANNEL_ID = "Foreground Service ID"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            while (true) {
                Log.e("SERVICES", "Service is running..")
                val client = OkHttpClient()
                val request = Request.Builder().url(InitAPI.SOCKET_URL).build()
                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        super.onOpen(webSocket, response)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        super.onMessage(webSocket, text)
                        try {
                            Log.e("WebSocket Message", text)
                            showNotification(JSONObject(text).getString("message"))
                        } catch (jsonException: JSONException) {
                            jsonException.printStackTrace()
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        super.onMessage(webSocket, bytes)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        super.onClosing(webSocket, code, reason)
                    }

                    override fun onFailure(
                        webSocket: WebSocket, t: Throwable, response: Response?
                    ) {
                        super.onFailure(webSocket, t, response)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        super.onClosed(webSocket, code, reason)
                    }
                })
                Thread.sleep(2000)
            }
        }.start()

        /*val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_ID,
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentText("Service is running")
            .setContentTitle("Service enabled")
            .setSmallIcon(R.drawable.fixme_logo)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification)
        } else {
            startForeground(
                1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            )
        }*/

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

        val notification =
            NotificationCompat.Builder(this@ForegroundServicesHelper, CHANNEL_ID)
                .setSmallIcon(R.drawable.fixme_logo)
                .setContentTitle("Erela FixMe")
                .setContentText(message)
                .setPriority(Notification.PRIORITY_DEFAULT)
        if (ActivityCompat.checkSelfPermission(
                this@ForegroundServicesHelper, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this@ForegroundServicesHelper)
            .notify(1, notification.build())
    }
}