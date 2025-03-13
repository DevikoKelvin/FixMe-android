package com.erela.fixme.helpers

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.erela.fixme.R
import com.erela.fixme.objects.PusherData
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.NotificationService
import com.google.gson.Gson
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange

object NotificationsHelper {
    private var pusher: Pusher? = null

    fun disconnectPusher() {
        pusher?.disconnect()
        pusher = null
    }

    fun receiveNotifications(context: Context, userData: UserData) {
        pusher = Pusher("4ae6ab89bbc42534b759", PusherOptions().setCluster("ap1"))

        if (pusher?.connection?.state == ConnectionState.DISCONNECTED) {
            pusher?.connect(object : ConnectionEventListener {
                override fun onConnectionStateChange(change: ConnectionStateChange) {
                    Log.e(
                        "PUSHER",
                        "State changed from ${change.previousState} to ${change.currentState}"
                    )
                }

                override fun onError(message: String?, code: String?, e: Exception?) {
                    if (code != null) {
                        if (message != null) {
                            if (e != null) {
                                Log.e(
                                    "PUSHER",
                                    "There was a problem on connecting! Error code: $code, message: $message, error: $e"
                                )
                            } else {
                                Log.e(
                                    "PUSHER",
                                    "There was a problem on connecting! Error code: $code, message: $message"
                                )
                            }
                        } else {
                            if (e != null) {
                                Log.e(
                                    "PUSHER",
                                    "There was a problem on connecting! Error code: $code, error: $e"
                                )
                            } else {
                                Log.e(
                                    "PUSHER",
                                    "There was a problem on connecting! Error code: $code"
                                )
                            }
                        }
                    } else {
                        if (message != null) {
                            if (e != null) {
                                Log.e(
                                    "PUSHER",
                                    "There was a problem on connecting! Error message: $message, error: $e"
                                )
                            } else {
                                Log.e(
                                    "PUSHER",
                                    "There was a problem on connecting! Error message: $message"
                                )
                            }
                        } else {
                            if (e != null) {
                                Log.e(
                                    "PUSHER",
                                    "There was a problem on connecting! Error exception: $e"
                                )
                            } else {
                                Log.e(
                                    "PUSHER",
                                    "There was a problem on connecting!"
                                )
                            }
                        }
                    }
                }
            }, ConnectionState.CONNECTED)
        }
        val channel = pusher?.subscribe("my-channel")
        channel?.bind("my-event") { event ->
            if (context is Activity && !context.isFinishing) {
                val pusherData = parseToJson(event.data)
                if (pusherData.idUser == userData.id) {
                    generateNotification(pusherData.message, context)
                }
            }
        }
    }

    private fun parseToJson(jsonString: String): PusherData =
        Gson().fromJson(jsonString, PusherData::class.java)

    private fun generateNotification(message: String, context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder =
            NotificationCompat.Builder(context, NotificationService.CHANNEL_ID)
                .setSmallIcon(R.drawable.fixme_logo)
                .setContentTitle(NotificationService.CHANNEL_NAME)
                .setContentText(message)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
        notificationManager.notify(NotificationService.NOTIFICATION_ID, notificationBuilder.build())
    }
}