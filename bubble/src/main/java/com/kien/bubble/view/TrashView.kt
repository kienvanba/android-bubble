package com.kien.bubble.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.contains
import androidx.core.view.setPadding
import com.kien.bubble.R
import com.kien.bubble.databinding.LayoutTrashBinding
import kotlin.math.pow
import kotlin.math.sqrt

class TrashView: FrameLayout {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    lateinit var windowManager: WindowManager
    lateinit var layoutParams: WindowManager.LayoutParams
    var huggingBubble: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                hugBubble()
            }
        }

    private var _showing = false

    private val trashBinding: LayoutTrashBinding = LayoutInflater.from(context).let {
        LayoutTrashBinding.inflate(it)
    }

    init {
        setPadding(resources.getDimensionPixelSize(R.dimen.dp_8))
        clipToPadding = false
        addView(trashBinding.root)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        alpha = 0.0f
    }

    fun show(point: Point) {
        if (_showing) {
            follow(point)
            return
        }
        layoutParams.x = resources.displayMetrics.widthPixels/2 - measuredWidth/2
        val destination = Point(layoutParams.x, layoutParams.y - measuredHeight*3/2)
        animateTo(destination)
        alpha = 1.0f
        _showing = true
    }

    private fun follow(point: Point) {
        val screenRoot = resources.displayMetrics.let {
            Point(it.widthPixels/2, it.heightPixels/2)
        }
        val trashRoot = resources.displayMetrics.let {
            Point(it.widthPixels/2 - measuredWidth/2, it.heightPixels + resources.getDimensionPixelOffset(R.dimen.dp_24) - measuredHeight*3/2)
        }
        val ratio = 15

        val newX = (point.x - screenRoot.x)/ratio + trashRoot.x
        val newY = (point.y - screenRoot.y)/ratio + trashRoot.y
        layoutParams.x = newX
        layoutParams.y = newY
        windowManager.updateViewLayout(this, layoutParams)
    }

    fun shouldSwallowBubble(point: Point): Boolean {
        val location = IntArray(2)
        this.getLocationOnScreen(location)
        val width = this.measuredWidth
        val height = this.measuredHeight

        // get center of trash view
        val center = Point(location[0] + width/2, location[1] + height/2)
        val swallowRadius = width * 1.5

        return center.distance(point) <= swallowRadius
    }

    private fun hugBubble() {
        val anim = if (huggingBubble)
            AnimationUtils.loadAnimation(context, R.anim.anim_scale_down)
        else
            AnimationUtils.loadAnimation(context, R.anim.anim_scale_up)
        trashBinding.root.startAnimation(anim)
    }

    fun hide() {
        if (!_showing) return
        val destination = Point(layoutParams.x, resources.displayMetrics.heightPixels + resources.getDimensionPixelOffset(R.dimen.dp_24))
        animateTo(destination)
        _showing = false
    }

    private fun animateTo(point: Point) {
        ValueAnimator.ofInt(layoutParams.y, point.y).apply {
            duration = resources.getInteger(R.integer.medium_duration).toLong()
            interpolator = OvershootInterpolator()
            addUpdateListener {
                val newY = it.animatedValue as Int
                layoutParams.y = newY
                windowManager.updateViewLayout(this@TrashView, layoutParams)
            }
        }.start()
    }

    private fun Point.distance(other: Point): Float {
        val deltaX = this.x - other.x
        val deltaY = this.y - other.y

        // Use the Pythagorean theorem to calculate the distance
        return sqrt((deltaX.toDouble().pow(2) + deltaY.toDouble().pow(2)).toFloat())
    }
}