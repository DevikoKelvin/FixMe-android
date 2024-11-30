package com.erela.fixme.custom_views.zoom_helper

import android.app.Activity
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.ImageView
import com.erela.fixme.custom_views.ActivityContainer
import com.erela.fixme.custom_views.zoom_helper.ZoomObjectHelper.DoubleTapListener
import com.erela.fixme.custom_views.zoom_helper.ZoomObjectHelper.ImageZoomConfig
import com.erela.fixme.custom_views.zoom_helper.ZoomObjectHelper.LongPressListener
import com.erela.fixme.custom_views.zoom_helper.ZoomObjectHelper.TapListener
import com.erela.fixme.custom_views.zoom_helper.ZoomObjectHelper.TargetContainer
import com.erela.fixme.custom_views.zoom_helper.ZoomObjectHelper.ZoomListener

class ImageZoomHelper(activity: Activity, view: View) : ScaleGestureDetector.OnScaleGestureListener,
    ZoomListener, TapListener, DoubleTapListener, LongPressListener {
    companion object {
        private const val STATE_IDLE = 0
        private const val STATE_POINTER_DOWN = 1
        private const val STATE_ZOOMING = 2
        private const val MIN_SCALE_FACTOR = 1f
        private const val MAX_SCALE_FACTOR = 5f
    }

    private val mTapListener: TapListener = this
    private val mLongPressListener: LongPressListener = this
    private val mDoubleTapListener: DoubleTapListener = this
    private var mState = STATE_IDLE
    private val mTargetContainer: TargetContainer = ActivityContainer(activity)
    private val mTarget: View = view
    private var mZoomableView: ImageView? = null
    private var mShadow: View? = null
    private val mScaleGestureDetector: ScaleGestureDetector =
        ScaleGestureDetector(view.context, this)
    private val mGestureDetector: GestureDetector =
        GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                mTapListener.onTap(mTarget)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                mLongPressListener.onLongPress(mTarget)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                mDoubleTapListener.onDoubleTap(mTarget)
                return true
            }
        })
    private var mScaleFactor = 1f
    private val mCurrentMovementMidPoint = PointF()
    private val mInitialPinchMidPoint = PointF()
    private var mTargetViewCords = Point()
    private var mAnimatingZoomEnding = false
    private val mEndZoomingInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    private val mConfig: ImageZoomConfig = ImageZoomConfig()
    private val mZoomListener: ZoomListener = this
    private val mEndingZoomAction = Runnable {
        removeFromDecorView(mShadow)
        removeFromDecorView(mZoomableView)
        mTarget.visibility = View.VISIBLE
        mZoomableView = null
        mCurrentMovementMidPoint.set(0f, 0f)
        mInitialPinchMidPoint.set(0f, 0f)
        mAnimatingZoomEnding = false
        mState = STATE_IDLE

        mZoomListener.onViewEndedZooming(mTarget)

        if (mConfig.immersiveModeEnabled) showSystemUI()
    }

    fun init(ev: MotionEvent) {
        mScaleGestureDetector.onTouchEvent(ev)
        mGestureDetector.onTouchEvent(ev)
        val action = ev.action and MotionEvent.ACTION_MASK

        when (action) {
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN -> {
                when (mState) {
                    STATE_IDLE -> mState = STATE_POINTER_DOWN
                    STATE_POINTER_DOWN -> {
                        mState = STATE_ZOOMING
                        ZoomObjectHelper.midPointOfEvent(mInitialPinchMidPoint, ev)
                        startZoomingView(mTarget)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (mState == STATE_ZOOMING) {
                    ZoomObjectHelper.midPointOfEvent(mCurrentMovementMidPoint, ev)
                    mCurrentMovementMidPoint.x -= mInitialPinchMidPoint.x
                    mCurrentMovementMidPoint.y -= mInitialPinchMidPoint.y
                    mCurrentMovementMidPoint.x += mTargetViewCords.x.toFloat()
                    mCurrentMovementMidPoint.y += mTargetViewCords.y.toFloat()
                    mZoomableView?.apply {
                        x = mCurrentMovementMidPoint.x
                        y = mCurrentMovementMidPoint.y
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                when (mState) {
                    STATE_ZOOMING -> endZoomingView()
                    STATE_POINTER_DOWN -> mState = STATE_IDLE
                }
            }
        }
    }

    private fun endZoomingView() {
        if (mConfig.zoomAnimationEnabled) {
            mAnimatingZoomEnding = true
            mZoomableView?.animate()
                ?.x(mTargetViewCords.x.toFloat())
                ?.y(mTargetViewCords.y.toFloat())
                ?.scaleX(1f)
                ?.scaleY(1f)
                ?.setInterpolator(mEndZoomingInterpolator)
                ?.withEndAction(mEndingZoomAction)
                ?.start()
        } else {
            mEndingZoomAction.run()
        }
    }

    private fun startZoomingView(view: View) {
        mZoomableView = ImageView(mTarget.context).apply {
            layoutParams = ViewGroup.LayoutParams(mTarget.width, mTarget.height)
            setImageBitmap(ZoomObjectHelper.getBitmapFromView(view))
        }

        mTargetViewCords = ZoomObjectHelper.getViewAbsoluteCords(view)

        mZoomableView?.apply {
            x = mTargetViewCords.x.toFloat()
            y = mTargetViewCords.y.toFloat()
        }

        if (mShadow == null) mShadow = View(mTarget.context)
        mShadow?.setBackgroundResource(0)

        addToDecorView(mShadow!!)
        addToDecorView(mZoomableView!!)

        disableParentTouch(mTarget.parent)
        mTarget.visibility = View.INVISIBLE

        if (mConfig.immersiveModeEnabled) hideSystemUI()
        mZoomListener.onViewStartedZooming(mTarget)
    }

    private fun addToDecorView(v: View) {
        mTargetContainer.getDecorView().addView(v)
    }

    private fun removeFromDecorView(v: View?) {
        mTargetContainer.getDecorView().removeView(v)
    }

    private fun obscureDecorView(factor: Float) {
        val normalizedValue = (factor - MIN_SCALE_FACTOR) / (MAX_SCALE_FACTOR - MIN_SCALE_FACTOR)
        val clampedValue = 0.75f.coerceAtMost(normalizedValue * 2)
        val obscure = Color.argb((clampedValue * 255).toInt(), 0, 0, 0)
        mShadow?.setBackgroundColor(obscure)
    }

    private fun disableParentTouch(view: ViewParent?) {
        view?.requestDisallowInterceptTouchEvent(true)
        if (view?.parent != null) disableParentTouch(view.parent)
    }

    private fun hideSystemUI() {
        // mTargetContainer.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        //     or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
        //     or View.SYSTEM_UI_FLAG_FULLSCREEN // hide approve bar
    }

    private fun showSystemUI() {
        // mTargetContainer.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        if (mZoomableView == null) return false

        mScaleFactor *= detector.scaleFactor

        mScaleFactor = MIN_SCALE_FACTOR.coerceAtLeast(mScaleFactor.coerceAtMost(MAX_SCALE_FACTOR))

        mZoomableView?.apply {
            scaleX = mScaleFactor
            scaleY = mScaleFactor
        }
        obscureDecorView(mScaleFactor)
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return mZoomableView != null
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        mScaleFactor = 1f
    }

    override fun onViewStartedZooming(view: View) {}

    override fun onViewEndedZooming(view: View) {}

    override fun onDoubleTap(view: View) {}

    override fun onLongPress(view: View) {}

    override fun onTap(view: View) {}
}