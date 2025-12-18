package com.erela.fixme.services

import android.util.Log
import com.erela.fixme.helpers.NotificationsHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "FixMe"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You have a new notification"
        val caseId = remoteMessage.data["caseId"]?.toIntOrNull() ?: 0

        NotificationsHelper.generateNotification(title, body, this, caseId)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // TODO: Implement this method to send token to your app server.
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // This is where you would send the token to your backend.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
