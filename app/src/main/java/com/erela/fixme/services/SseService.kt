package com.erela.fixme.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.erela.fixme.BuildConfig
import com.erela.fixme.R
import com.erela.fixme.helpers.NotificationsHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.UserData
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SseService : Service() {
    private lateinit var eventSource: EventSource
    private val userData: UserData by lazy {
        UserDataHelper(applicationContext).getUserData()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, getNotification())
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "FixMe Background Service",
                android.app.NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(): android.app.Notification {
        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FixMe Service")
            .setContentText("Listening for updates...")
            .setSmallIcon(R.drawable.fixme_logo)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            initSse()
            isRunning = true
        }
        return START_STICKY
    }

    private fun initSse() {
        val client = InitAPI.getUnsafeOkHttpClient()
            .callTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()
        val request = Request.Builder()
            .url(BuildConfig.SSE_URL)
            .header("Accept", "text/event-stream")
            .build()

        eventSource = EventSources.createFactory(client)
            .newEventSource(request, object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    try {
                        // The data might come with a "data: " prefix.
                        val cleanData = if (data.startsWith("data:")) {
                            data.substring(5).trim()
                        } else {
                            data
                        }

                        if (!cleanData.startsWith("{")) {
                            // Not a JSON object, ignore.
                            Log.d(TAG, "Received non-JSON event data: $cleanData")
                            return
                        }

                        val outerJson = JSONObject(cleanData)

                        if (!outerJson.has("message")) {
                            Log.d(TAG, "Received JSON without a message field: $cleanData")
                            return
                        }

                        val message = outerJson.get("message")
                        val messageJson: JSONObject = when (message) {
                            is String -> {
                                var jsonString = message.trim()
                                if (!jsonString.endsWith("}")) {
                                    jsonString += "}"
                                }
                                try {
                                    JSONObject(jsonString)
                                } catch (e: JSONException) {
                                    Log.e(TAG, "Could not parse message JSON: $jsonString", e)
                                    return
                                }
                            }

                            is JSONObject -> {
                                message
                            }

                            else -> {
                                Log.e(TAG, "Message field is not a string or JSON object")
                                return
                            }
                        }

                        val notificationId = messageJson.getInt("id_gaprojects")
                        val relatedUserId = messageJson.getInt("id_user")

                        if (UserDataHelper(applicationContext).isUserDataExist() && FCMService.lastNotificationId != notificationId) {
                            val title =
                                if (getString(R.string.lang) == "in") "Kamu mendapatkan notifikasi baru!" else "You have a new notification!"
                            val body = messageJson.getString("body")
                            if (userData.id == relatedUserId) {
                                NotificationsHelper.generateNotification(
                                    title,
                                    body,
                                    applicationContext,
                                    notificationId
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "onEvent error: ${e.toString()}\nData was: $data")
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    Log.e(TAG, "onFailure: ${t?.message}", t)
                }

                override fun onClosed(eventSource: EventSource) {
                    Log.d(TAG, "onClosed: SSE Connection Closed, reconnecting...")
                    initSse()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        eventSource.cancel()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "SseService"
        private const val CHANNEL_ID = "FixMe_SSE_Channel"
        private const val NOTIFICATION_ID = 101
    }
}
