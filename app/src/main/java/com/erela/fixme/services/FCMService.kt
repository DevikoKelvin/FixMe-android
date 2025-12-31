package com.erela.fixme.services

import android.content.Intent
import android.util.Log
import com.erela.fixme.R
import com.erela.fixme.helpers.NotificationsHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.NotificationData
import com.erela.fixme.objects.UserData
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FCMService : FirebaseMessagingService() {
    private val userData: UserData by lazy {
        UserDataHelper(applicationContext).getUserData()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            Log.e(TAG, "Received Message Data: ${remoteMessage.data}")

            val notificationData =
                Gson().fromJson(Gson().toJson(remoteMessage.data), NotificationData::class.java)

            if (notificationData.relatedUserId == userData.id) {
                val title =
                    if (getString(R.string.lang) == "in") "Kamu mendapatkan notifikasi baru!" else "You have a new notification!"
                val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
                ?: ""

                if (UserDataHelper(applicationContext).isUserDataExist() && userData.id == notificationData.relatedUserId) {
                    /*stopService(Intent(this, SseService::class.java))*/
                    lastNotificationId = notificationData.idGaProjects
                    NotificationsHelper.generateNotification(
                        title,
                        body,
                        this,
                        notificationData.idGaProjects
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onMessageReceived error: ${e.message}", e)
            startService(Intent(this, SseService::class.java))
        }
    }

    override fun onNewToken(token: String) {
        Log.e(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "sendRegistrationToServer: token is null or empty")
            return
        }

        val userDataHelper = UserDataHelper(applicationContext)
        if (userDataHelper.isUserDataExist()) {
            InitAPI.getEndpoint.updateFcmToken(userData.id, token)
                .enqueue(object : Callback<GenericSimpleResponse> {
                    override fun onResponse(
                        call: Call<GenericSimpleResponse>,
                        response: Response<GenericSimpleResponse>
                    ) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "FCM token updated successfully")
                        } else {
                            Log.e(
                                TAG,
                                "Failed to update FCM token: ${response.code()} ${response.message()}"
                            )
                        }
                    }

                    override fun onFailure(call: Call<GenericSimpleResponse>, t: Throwable) {
                        Log.e(TAG, "Failed to update FCM token", t)
                    }
                })
        }
    }

    companion object {
        private const val TAG = "Firebase Messaging Service"
        var lastNotificationId: Int? = null
    }
}
