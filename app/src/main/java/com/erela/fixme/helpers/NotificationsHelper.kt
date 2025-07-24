package com.erela.fixme.helpers

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.erela.fixme.R
import com.erela.fixme.activities.SubmissionDetailActivity
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.NewNotificationResponse
import com.erela.fixme.objects.PusherData
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.NotificationService
import com.google.gson.Gson
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NotificationsHelper {
    private var pusher: Pusher? = null
    private const val LAST_NOTIFICATION_ID = "last_notification_id"

    private fun getLastNotificationId(context: Context): Int {
        val prefs = SharedPreferencesHelper.getSharedPreferences(context)
        return prefs.getInt(LAST_NOTIFICATION_ID, -1)
    }

    private fun saveLastNotificationId(context: Context, id: Int) {
        val prefs = SharedPreferencesHelper.getSharedPreferences(context)
        prefs.edit { putInt(LAST_NOTIFICATION_ID, id) }
    }

    fun disconnectPusher() {
        pusher?.disconnect()
        pusher = null
    }

    fun callNewNotification(context: Context, userData: UserData) {
        try {
            InitAPI.getAPI.getNotificationCall(userData.id).enqueue(
                object : Callback<NewNotificationResponse> {
                    override fun onResponse(
                        call: Call<NewNotificationResponse>,
                        response: Response<NewNotificationResponse>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val result = response.body()!!
                            if (result.notifications!!.isNotEmpty()) {
                                val latestNotification = result.notifications.first()
                                val lastShownId = getLastNotificationId(context)

                                if (latestNotification?.idNotif?.toInt() != lastShownId) {
                                    Log.e(
                                        "NOTIFICATION",
                                        "New notification received: $latestNotification"
                                    )
                                    generateNotification(
                                        latestNotification?.actions
                                            ?: "You have a new notification",
                                        context,
                                        if (latestNotification?.caseId == null)
                                            0
                                        else
                                            latestNotification.caseId
                                    )

                                    saveLastNotificationId(
                                        context,
                                        latestNotification?.idNotif!!.toInt()
                                    )
                                }
                            }
                        } else {
                            Log.e("NOTIFICATION", "Response not successful")
                        }
                    }

                    override fun onFailure(
                        call: Call<NewNotificationResponse>,
                        throwable: Throwable
                    ) {
                        throwable.printStackTrace()
                        Log.e("NOTIFICATION", "Error: ${throwable.message}")
                    }

                }
            )
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
        }
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
                    generateNotification(pusherData.message, context, 0)
                }
            }
        }
    }

    private fun parseToJson(jsonString: String): PusherData =
        Gson().fromJson(jsonString, PusherData::class.java)

    private fun generateNotification(message: String, context: Context, caseId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, SubmissionDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (caseId != 0) {
                putExtra(SubmissionDetailActivity.DETAIL_ID, caseId.toString())
                Log.e("NOTIFICATION", "Passing DETAIL_ID: $caseId")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.e("NOTIFICATION", "Generating notification with message: $message, caseId: $caseId")

        val notificationBuilder =
            NotificationCompat.Builder(context, NotificationService.CHANNEL_ID)
                .setSmallIcon(R.drawable.fixme_logo)
                .setContentTitle(NotificationService.CHANNEL_NAME)
                .setContentText(message)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
                .also {
                    with(it) {
                        if (caseId != 0)
                            setContentIntent(pendingIntent)
                    }
                }

        notificationManager.notify(NotificationService.NOTIFICATION_ID, notificationBuilder.build())
    }
}