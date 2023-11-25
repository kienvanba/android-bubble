package com.kien.bubble.service

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.view.animation.OvershootInterpolator
import androidx.core.animation.addListener
import com.kien.bubble.R
import com.kien.bubble.view.BubbleView
import com.kien.bubble.view.TrashView

class BubbleForegroundService: Service() {
    private lateinit var windowManager: WindowManager
    private val bubbleView: BubbleView by lazy { BubbleView(this) }
    private val trashView: TrashView by lazy { TrashView(this) }
    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onBind(p0: Intent?): IBinder {
        return BubbleServiceBinder(p0)
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initView()
    }

    private fun initView() {
        addBubbleView()
        addTrashView()
    }

    private fun addTrashView() {
        val layoutParams = getTrashLayoutParams()
        trashView.layoutParams = layoutParams
        trashView.windowManager = windowManager
        handler.post {
            windowManager.addView(trashView, trashView.layoutParams)
        }
    }

    private fun addBubbleView() {
        val initialPoint = Point(-30, resources.displayMetrics.heightPixels/3)
        val layoutParams = getBubbleLayoutParams(initialPoint)
        bubbleView.layoutParams = layoutParams
        bubbleView.windowManager = windowManager
        bubbleView.setOnEventListener(object : BubbleView.BubbleEventListener {
            override fun onClick() {
                stopSelf()
            }

            override fun onMove(point: Point) {
                trashView.show(point)
                trashView.huggingBubble = trashView.shouldSwallowBubble(point)
                if (trashView.huggingBubble) {
                    val trashViewPosition = Point(trashView.layoutParams.x, trashView.layoutParams.y)
                    val withDiff = (trashView.measuredWidth - bubbleView.measuredWidth)/2
                    val heightDiff = (trashView.measuredHeight - bubbleView.measuredHeight)/2
                    val bubbleViewPosition = Point(trashViewPosition.x + withDiff, trashViewPosition.y + heightDiff)
                    bubbleView.moveToTrash(bubbleViewPosition)
                } else {
                    bubbleView.moveOutOfTrash()
                }
            }

            override fun onIdle(point: Point): Boolean {
                val swallowed = trashView.shouldSwallowBubble(point)
                if (swallowed) {
                    stopService()
                } else {
                    trashView.hide()
                }
                return swallowed
            }
        })
        handler.post {
            windowManager.addView(bubbleView, bubbleView.layoutParams)
        }
    }

    private fun stopService() {
        val hideY = resources.displayMetrics.heightPixels + resources.getDimensionPixelOffset(R.dimen.dp_24)
        val bubble = ValueAnimator.ofInt(bubbleView.layoutParams.y, hideY).apply {
            addUpdateListener {
                val newY = it.animatedValue as Int
                bubbleView.layoutParams.y = newY
                windowManager.updateViewLayout(bubbleView, bubbleView.layoutParams)
            }
        }
        val trash = ValueAnimator.ofInt(trashView.layoutParams.y, hideY).apply {
            addUpdateListener {
                val newY = it.animatedValue as Int
                trashView.layoutParams.y = newY
                windowManager.updateViewLayout(trashView, bubbleView.layoutParams)
            }
        }
        AnimatorSet().apply {
            playTogether(bubble, trash)
            duration = resources.getInteger(R.integer.medium_duration).toLong()
            interpolator = OvershootInterpolator()
            start()
            addListener(onEnd = {
                stopSelf()
            })
        }
    }

    private fun getTrashLayoutParams(): WindowManager.LayoutParams {
        val lpType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            lpType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.y = resources.displayMetrics.heightPixels + resources.getDimensionPixelOffset(R.dimen.dp_24)
        params.windowAnimations = android.R.anim.accelerate_decelerate_interpolator
        return params
    }

    private fun getBubbleLayoutParams(at: Point = Point(0, 0)): WindowManager.LayoutParams {
        val lpType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            lpType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.windowAnimations = android.R.anim.accelerate_decelerate_interpolator
        at.also { params.x = it.x; params.y = it.y }
        return params
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        initData()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initData() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        windowManager.removeView(bubbleView)
        windowManager.removeView(trashView)
        super.onDestroy()
    }
}