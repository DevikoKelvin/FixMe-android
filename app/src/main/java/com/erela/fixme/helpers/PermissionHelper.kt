package com.erela.fixme.helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    const val READ_MEDIA_IMAGES = Manifest.permission.READ_MEDIA_IMAGES

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    const val READ_MEDIA_VIDEO = Manifest.permission.READ_MEDIA_VIDEO

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS
    const val READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE

    const val CAMERA = Manifest.permission.CAMERA
    const val REQUEST_CODE_GALLERY = 100
    const val REQUEST_CODE_CAMERA = 101
    const val REQUEST_CODE_NOTIFICATION = 102

    fun isPermissionGranted(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
}