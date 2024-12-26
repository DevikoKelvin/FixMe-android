package com.erela.fixme.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent(context, NotificationService::class.java).also {
                context!!.startForegroundService(it)
            }
        }
    }
}