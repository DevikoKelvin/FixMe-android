package com.erela.fixme.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.erela.fixme.R
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.FirebaseMessagingService.Companion.CHANNEL_ID
import com.erela.fixme.services.FirebaseMessagingService.Companion.CHANNEL_NAME
import com.erela.fixme.services.FirebaseMessagingService.Companion.NOTIFICATION_ID
import com.google.gson.Gson
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

            override fun onError(message: String, code: String, e: Exception) {
                Log.e(
                    "PUSHER",
                    "There was a problem connecting! code ($code), message ($message), exception($e)"
                )
            }
        }, ConnectionState.ALL)
        val channel = pusher.subscribe("my-channel")
        channel.bind("my-event") { event ->
            val pusherData = parseToJson(event.data)
            Log.e("Logged In User", userData.id.toString())
            Log.e("Pusher Data User", pusherData.id_user.toString())
            if (pusherData.id_user == userData.id) {
                generateNotification(pusherData.message, context)
            }
        }
    }

    private data class PusherData(val title: String, val message: String, val id_user: Int)

    private fun parseToJson(jsonString: String): PusherData =
        Gson().fromJson(jsonString, PusherData::class.java)

    private fun generateNotification(message: String, context: Context) {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.fixme_logo)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setOnlyAlertOnce(true)
            .setContentTitle(CHANNEL_NAME)
            .setContentText(message)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
}