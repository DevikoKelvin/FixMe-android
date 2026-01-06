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
    const val WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE

    const val INSTALL_PACKAGES = Manifest.permission.REQUEST_INSTALL_PACKAGES

    const val CAMERA = Manifest.permission.CAMERA
    const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    const val ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    const val REQUEST_CODE_GALLERY = 100
    const val REQUEST_CODE_CAMERA = 101
    const val REQUEST_CODE_NOTIFICATION = 102
    const val REQUEST_WRITE_EXTERNAL_STORAGE = 103
    const val REQUEST_INSTALL_PACKAGES = 104
    const val REQUEST_CODE_LOCATION = 105

    fun isPermissionGranted(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: Activity, permissions: Array<out String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
}