package com.erela.fixme.helpers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.erela.fixme.R
import com.erela.fixme.activities.LaundryCheckInActivity
import com.erela.fixme.activities.LaundryCounterActivity
import com.erela.fixme.activities.MainActivity
import com.erela.fixme.activities.SubmissionDetailActivity

object NotificationsHelper {
    const val CHANNEL_ID = "FixMe Notification Channel"
    private const val CHANNEL_NAME = "Erela FixMe"

    /**
     * Smart Wash notifications are namespaced on the wire [D-10].
     *
     * `client:status` is one flat field shared with every module in FixMe - 'hold', 'completed',
     * 'approve_dept_tujuan' and thirty others live in it - so the server prefixes its own:
     * `laundry.ready`, `laundry.uncollected`, `laundry.held_garment`. Matching the PREFIX rather
     * than the three names means a fourth kind of laundry reminder needs no client release.
     */
    private const val LAUNDRY_PREFIX = "laundry."

    /** True when this notification belongs to Smart Wash. */
    fun isLaundry(status: String?) = status?.startsWith(LAUNDRY_PREFIX) == true

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
        caseId: Int,
        status: String? = null
    ) {
        createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = System.currentTimeMillis().toInt()

        val resultIntent: Intent? = if (UserDataHelper(context).isUserDataExist()) {
            when {
                // A laundry reminder carries no case, so before this it landed on the main menu
                // and the recipient had to find Smart Wash themselves - which is the whole
                // difference between being told and being taken there.
                //
                // WHICH of the two screens is the same decision the menu makes, from the same
                // server-sent flag: the counter operator gets the counter, everybody else gets
                // check-in. Re-deriving it here from departments is the drift that gate exists
                // to avoid.
                isLaundry(status) -> Intent(
                    context,
                    if (UserDataHelper(context).getUserData().isLaundryCounter)
                        LaundryCounterActivity::class.java
                    else LaundryCheckInActivity::class.java
                )

                caseId != 0 -> Intent(context, SubmissionDetailActivity::class.java).apply {
                    putExtra(SubmissionDetailActivity.DETAIL_ID, caseId.toString())
                }

                else -> Intent(context, MainActivity::class.java)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }

        if (caseId == 0) {
            resultIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent!!)
            getPendingIntent(
                notificationId,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
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
