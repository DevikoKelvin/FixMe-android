package com.erela.fixme.custom_views

import android.app.Activity
import android.view.ViewGroup
import com.erela.fixme.custom_views.zoom_helper.ZoomObjectHelper.TargetContainer

class ActivityContainer(private val activity: Activity) : TargetContainer {
    override fun getDecorView(): ViewGroup {
        return activity.window.decorView as ViewGroup
    }
}