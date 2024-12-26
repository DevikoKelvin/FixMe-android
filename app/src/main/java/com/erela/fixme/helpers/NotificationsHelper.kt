package com.erela.fixme.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.erela.fixme.R
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.NotificationService
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange

object NotificationsHelper {
    fun receiveNotifications(context: Context, userData: UserData) {
        val options = PusherOptions()
        options.setCluster("ap1")
        val pusher = Pusher("4ae6ab89bbc42534b759", options)

        pusher.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                Log.e(
                    "PUSHER",
                    "State changed from ${change.previousState} to ${change.currentState}"
                )
            }

            override fun onError(message: String, code: String, e: Exception?) {
                if (e != null) {
                    Log.e(
                        "PUSHER",
                        "There was a problem connecting! code ($code), message ($message), exception($e)"
                    )
                } else {
                    Log.e(
                        "PUSHER",
                        "There was a problem connecting! code ($code), message ($message)"
                    )
                }
            }
        }, ConnectionState.ALL)
        val channel = pusher.subscribe("my-channel")
        channel.bind("my-event") { event ->
            val pusherData = parseToJson(event.data)
            Log.e("Logged In User", userData.id.toString())
            Log.e("Pusher Data User", pusherData.idUser.toString())
            if (pusherData.idUser == userData.id) {
                generateNotification(pusherData.message, context)
            }
        }
    }

    private data class PusherData(
        @field:SerializedName("title")
        val title: String,
        @field:SerializedName("message")
        val message: String,
        @field:SerializedName("id_user")
        val idUser: Int
    )

    private fun parseToJson(jsonString: String): PusherData =
        Gson().fromJson(jsonString, PusherData::class.java)

    private fun generateNotification(message: String, context: Context) {
        val notificationBuilder =
            NotificationCompat.Builder(context, NotificationService.CHANNEL_ID)
                .setSmallIcon(R.drawable.fixme_logo)
                .setContentTitle(NotificationService.CHANNEL_NAME)
                .setContentText(message)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NotificationService.CHANNEL_ID,
            NotificationService.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(NotificationService.NOTIFICATION_ID, notificationBuilder.build())
    }
}