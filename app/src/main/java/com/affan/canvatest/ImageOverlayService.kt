package com.affan.canvatest

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView

class ImageOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ImageView? = null
    private var params: WindowManager.LayoutParams? = null // Define params here

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0.0f
    private var initialTouchY = 0.0f
    private var isResizing = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = ImageView(this)
        overlayView?.setImageResource(R.drawable.clock)
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params?.gravity = Gravity.TOP or Gravity.START
        params?.x = 0
        params?.y = 0

        overlayView?.setOnTouchListener(onTouchListener)

        windowManager?.addView(overlayView, params)
    }

    private val onTouchListener = View.OnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params?.x ?: 0
                initialY = params?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isResizing = isResizing(event)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isResizing) {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    val newWidth = (overlayView!!.width + deltaX).toInt()
                    val newHeight = (overlayView!!.height + deltaY).toInt()
                    params?.width = newWidth
                    params?.height = newHeight
                    windowManager?.updateViewLayout(overlayView, params)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                } else {
                    val xDiff = (event.rawX - initialTouchX).toInt()
                    val yDiff = (event.rawY - initialTouchY).toInt()
                    params?.x = initialX + xDiff
                    params?.y = initialY + yDiff
                    windowManager?.updateViewLayout(overlayView, params)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isResizing) {
                    view.performClick()
                }
            }
        }
        true
    }

    private fun isResizing(event: MotionEvent): Boolean {
        val view = overlayView ?: return false
        val viewBounds = Rect()
        view.getGlobalVisibleRect(viewBounds)
        val touchBounds = Rect(viewBounds)
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        touchBounds.inset(-touchSlop, -touchSlop)
        return !touchBounds.contains(event.rawX.toInt(), event.rawY.toInt())
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager?.removeView(overlayView)
    }
}
