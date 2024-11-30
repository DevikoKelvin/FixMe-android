package com.erela.fixme.custom_views.zoom_helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

object ZoomObjectHelper {
    interface ZoomListener {
        fun onViewStartedZooming(view: View)
        fun onViewEndedZooming(view: View)
    }

    interface TapListener {
        fun onTap(view: View)
    }

    interface DoubleTapListener {
        fun onDoubleTap(view: View)
    }

    interface LongPressListener {
        fun onLongPress(view: View)
    }

    interface TargetContainer {
        fun getDecorView(): ViewGroup
    }

    data class ImageZoomConfig(
        var zoomAnimationEnabled: Boolean = true,
        var immersiveModeEnabled: Boolean = true
    )

    fun midPointOfEvent(point: PointF, event: MotionEvent) {
        if (event.pointerCount == 2) {
            val x = event.getX(0) + event.getX(1)
            val y = event.getY(0) + event.getY(1)
            point.set(x / 2, y / 2)
        }
    }

    fun getBitmapFromView(view: View): Bitmap {
        // Define a bitmap with the same size as the view
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        // Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        // Draw the view on the canvas
        view.draw(canvas)
        // Return the bitmap
        return returnedBitmap
    }

    fun getViewAbsoluteCords(v: View): Point {
        val location = IntArray(2)
        v.getLocationInWindow(location)
        val x = location[0]
        val y = location[1]

        return Point(x, y)
    }

    /*fun viewMidPoint(point: PointF, v: View) {
        val x = v.width.toFloat()
        val y = v.height.toFloat()
        point.set(x / 2, y / 2)
    }*/
}