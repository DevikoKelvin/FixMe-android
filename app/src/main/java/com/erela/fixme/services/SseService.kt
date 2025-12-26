package com.erela.fixme.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.erela.fixme.R
import com.erela.fixme.helpers.NotificationsHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject

class SseService : Service() {

    private lateinit var eventSource: EventSource

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url("http://192.168.3.109:81/fixme/ntfy/show-notif")
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
                    // The response is nested inside a "data:" field, so we need to extract it.
                    val jsonData = data.substringAfter("data: ")
                    try {
                        val outerJson = JSONObject(jsonData)
                        val messageString = outerJson.getString("message")
                        val messageJson = JSONObject(messageString)
                        val notificationId = messageJson.getInt("id_ga_projects")

                        if (FCMService.lastNotificationId != notificationId) {
                            val title =
                                if (getString(R.string.lang) == "in") "Kamu mendapatkan notifikasi baru!" else "You have a new notification!"
                            val body = messageJson.getString("body")
                            NotificationsHelper.generateNotification(
                                title,
                                body,
                                applicationContext,
                                notificationId
                            )
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: okhttp3.Response?
                ) {
                    // handle failure
                }

                override fun onClosed(eventSource: EventSource) {
                    // handle closed
                }
            })

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        eventSource.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
