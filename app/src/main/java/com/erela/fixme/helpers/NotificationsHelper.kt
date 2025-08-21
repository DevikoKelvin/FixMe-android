package com.erela.fixme.helpers

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
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.NotificationService
import com.pusher.client.Pusher
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NotificationsHelper {
    private var pusher: Pusher? = null
    private const val LAST_NOTIFICATION_ID = "last_notification_id"
    private var pusherNotificationCounter = 1000 // Counter for Pusher and fallback notification IDs

    private fun getLastNotificationId(context: Context): Int {
        val prefs = SharedPreferencesHelper.getSharedPreferences(context)
        return prefs.getInt(LAST_NOTIFICATION_ID, -1) // -1 can indicate no ID stored yet
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
                            Log.d("NOTIFICATION", "API response received. Body: $result")
                            val notificationsFromServer = result.notifications

                            if (!notificationsFromServer.isNullOrEmpty()) {
                                val latestNotificationInBatch =
                                    notificationsFromServer.first() // Assuming newest is first
                                val lastShownApiNotificationId = getLastNotificationId(context)

                                var processThisBatch = false
                                var idToSaveAsLast: Int? = null

                                // Determine if this batch should be processed
                                if (latestNotificationInBatch?.idNotif != null) {
                                    if (latestNotificationInBatch.idNotif != lastShownApiNotificationId) {
                                        processThisBatch = true
                                        idToSaveAsLast = latestNotificationInBatch.idNotif
                                        Log.d(
                                            "NOTIFICATION",
                                            "New batch. Latest ID: ${latestNotificationInBatch.idNotif}, Last shown: $lastShownApiNotificationId"
                                        )
                                    } else {
                                        Log.d(
                                            "NOTIFICATION",
                                            "Latest notification in batch (ID: ${latestNotificationInBatch.idNotif}) is same as last shown ($lastShownApiNotificationId). Assuming batch already processed."
                                        )
                                    }
                                } else {
                                    // If latest in batch has null ID, we process it as potentially new,
                                    // but we can't update lastShownApiNotificationId with a null.
                                    processThisBatch = true
                                    idToSaveAsLast = null // Cannot save a null ID
                                    Log.d(
                                        "NOTIFICATION",
                                        "Latest notification in batch has null ID. Processing batch, but lastNotificationId won't be updated from this batch's latest."
                                    )
                                }

                                if (processThisBatch) {
                                    Log.d(
                                        "NOTIFICATION",
                                        "Processing batch of ${notificationsFromServer.size} notifications."
                                    )
                                    notificationsFromServer.forEach { notificationItem ->
                                        // Use server-provided ID if available, otherwise use a local counter for uniqueness.
                                        val notificationIdForManager =
                                            notificationItem?.idNotif ?: pusherNotificationCounter++

                                        generateNotification(
                                            notificationItem?.actions
                                                ?: "You have a new notification",
                                            context,
                                            notificationIdForManager, // This is now unique per notification
                                            notificationItem?.caseId ?: 0
                                        )
                                    }
                                    // Save the ID of the latest notification from THIS batch, if it was non-null
                                    if (idToSaveAsLast != null) {
                                        saveLastNotificationId(context, idToSaveAsLast)
                                        Log.d(
                                            "NOTIFICATION",
                                            "Saved $idToSaveAsLast as last shown API notification ID."
                                        )
                                    }
                                }
                            } else {
                                Log.d(
                                    "NOTIFICATION",
                                    "Received empty or null list of notifications from API."
                                )
                            }
                        } else {
                            Log.e(
                                "NOTIFICATION",
                                "Response not successful or body is null. Code: ${response.code()}, Message: ${response.message()}"
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<NewNotificationResponse>,
                        throwable: Throwable
                    ) {
                        throwable.printStackTrace()
                        Log.e("NOTIFICATION", "Error: ${throwable.message}", throwable)
                    }
                }
            )
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
            Log.e("NOTIFICATION", "JSON Exception: ${jsonException.message}", jsonException)
        }
    }

    /*fun receiveNotifications(context: Context, userData: UserData) {
        pusher = Pusher("4ae6ab89bbc42534b759", PusherOptions().setCluster("ap1"))

        if (pusher?.connection?.state == ConnectionState.DISCONNECTED) {
            pusher?.connect(object : ConnectionEventListener {
                override fun onConnectionStateChange(change: ConnectionStateChange) {
                    Log.e( // Consider changing to Log.i or Log.d for non-error states
                        "PUSHER",
                        "State changed from ${change.previousState} to ${change.currentState}"
                    )
                }

                override fun onError(message: String?, code: String?, e: Exception?) {
                    val errorMessage = "PUSHER: There was a problem connecting! " +
                            "Code: ${code ?: "N/A"}, Message: ${message ?: "N/A"}, Exception: ${e?.toString() ?: "N/A"}"
                    Log.e("PUSHER", errorMessage, e)
                }
            }, ConnectionState.CONNECTED)
        }
        val channel = pusher?.subscribe("my-channel")
        channel?.bind("my-event") { event ->
            if (context is Activity && !context.isFinishing) {
                try {
                    val pusherData = parseToJson(event.data)
                    if (pusherData.idUser == userData.id) {
                        val uniquePusherNotificationId =
                            pusherNotificationCounter++ // Generate unique ID for Pusher notification
                        generateNotification(
                            pusherData.message,
                            context,
                            uniquePusherNotificationId,
                            0
                        ) // caseId is 0 for Pusher notifs
                    }
                } catch (e: Exception) {
                    Log.e("PUSHER_EVENT", "Error processing Pusher event: ${event.data}", e)
                }
            }
        }
    }

    private fun parseToJson(jsonString: String): PusherData =
        Gson().fromJson(jsonString, PusherData::class.java)*/

    private fun generateNotification(
        message: String,
        context: Context,
        notificationId: Int, // This ID will now be used and should be unique
        caseId: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, SubmissionDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (caseId != 0) {
                putExtra(SubmissionDetailActivity.DETAIL_ID, caseId.toString())
                putExtra(SubmissionDetailActivity.NOTIFICATION_ID, notificationId)
            }
        }

        // Using a unique request code for PendingIntent for each notification
        val pendingIntentRequestCode = notificationId

        val pendingIntent = PendingIntent.getActivity(
            context,
            pendingIntentRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder =
            NotificationCompat.Builder(context, NotificationService.CHANNEL_ID)
                .setSmallIcon(R.drawable.fixme_logo)
                .setContentTitle(NotificationService.CHANNEL_NAME)
                .setContentText(message)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Ensure visibility for important notifications
                .also {
                    // Only set content intent if there's a caseId to navigate to
                    if (caseId != 0) {
                        it.setContentIntent(pendingIntent)
                    }
                }

        Log.d(
            "NOTIFICATION_GEN",
            "Generating notification. ID: $notificationId, Message: $message, CaseID: $caseId"
        )
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
