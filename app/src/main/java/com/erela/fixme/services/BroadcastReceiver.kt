package com.erela.fixme.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ForegroundServicesHelper::class.java)
            context?.startForegroundService(serviceIntent)
        }
    }
}