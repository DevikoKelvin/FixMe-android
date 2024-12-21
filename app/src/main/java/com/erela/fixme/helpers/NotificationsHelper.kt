package com.erela.fixme.helpers

import android.app.Activity
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.pusher.pushnotifications.PushNotificationReceivedListener
import com.pusher.pushnotifications.PushNotifications

object NotificationsHelper {
    fun receiveNotifications(activity: Activity) {
        PushNotifications.setOnMessageReceivedListenerForVisibleActivity(activity, object: PushNotificationReceivedListener {
            override fun onMessageReceived(remoteMessage: RemoteMessage) {
                Log.e("Message", remoteMessage.data.toString())
            }
        })
    }
}