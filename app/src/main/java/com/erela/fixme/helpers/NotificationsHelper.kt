package com.erela.fixme.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.erela.fixme.R
import com.erela.fixme.activities.MainActivity
import com.erela.fixme.activities.SubmissionDetailActivity

object NotificationsHelper {
    const val CHANNEL_ID = "FixMe Notification Channel"
    private const val CHANNEL_NAME = "Erela FixMe"

    private fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun generateNotification(
        title: String,
        message: String,
        context: Context,
        caseId: Int
    ) {
        createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = System.currentTimeMillis().toInt()

        val resultIntent: Intent? = if (UserDataHelper(context).isUserDataExist()) {
            if (caseId != 0) {
                Intent(context, SubmissionDetailActivity::class.java).apply {
                    putExtra(SubmissionDetailActivity.DETAIL_ID, caseId.toString())
                }
            } else {
                Intent(context, MainActivity::class.java)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }

        if (caseId == 0) {
            resultIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent!!)
            getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notificationBuilder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.fixme_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
