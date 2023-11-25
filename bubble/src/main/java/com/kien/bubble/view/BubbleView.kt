package com.kien.bubble.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.kien.bubble.R
import com.kien.bubble.databinding.LayoutBubbleBinding
import com.kien.bubble.databinding.LayoutBubbleDetailBinding

class BubbleView: CardView {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    private var initialTouchPosition = Point(0, 0)
    private var initialPosition = Point(0, 0)
    private var bubbleLastAction = -999
    private var _eventListener: BubbleEventListener? = null
    private var _movable = true

    lateinit var windowManager: WindowManager
    lateinit var layoutParams: WindowManager.LayoutParams
    private val bubbleBinding: LayoutBubbleBinding = LayoutInflater.from(context).let {
        LayoutBubbleBinding.inflate(it, this, false)
    }
    private val bubbleDetailBinding: LayoutBubbleDetailBinding = LayoutInflater.from(context).let {
        LayoutBubbleDetailBinding.inflate(it, this, false)
    }
    private val container: FrameLayout by lazy { FrameLayout(context) }

    init {
        radius = resources.getDimension(R.dimen.dp_8)
        cardElevation = 0f
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
        initViews()
        initListeners()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animatePopOut()
    }

    private fun initViews() {
        val containerLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        containerLayoutParams.gravity = Gravity.CENTER
        addView(container, containerLayoutParams)

        container.addView(bubbleBinding.root)
    }

    private fun showDetail() {
        layoutParams.apply {
            width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            x = 0
            y = 0
            gravity = Gravity.CENTER
        }
        windowManager.updateViewLayout(this@BubbleView, layoutParams)
        bubbleDetailBinding.root.alpha = 0f
        container.addView(bubbleDetailBinding.root)
        animateInDetail()
    }

    private fun animateInDetail() {
        bubbleBinding.root.animate()
            .setDuration(1200L)
            .alpha(0.0f)
        bubbleBinding.root.isEnabled = false
        bubbleBinding.root.animate()
            .scaleX(50f)
            .scaleY(50f)
            .setDuration(1000L)
            .setListener(object: Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {}

                override fun onAnimationEnd(p0: Animator) {
                    container.removeView(bubbleBinding.root)
                }

                override fun onAnimationCancel(p0: Animator) { }

                override fun onAnimationRepeat(p0: Animator) {}
            })
        bubbleDetailBinding.root.animate().setDuration(700L).alpha(1.0f)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {
        bubbleDetailBinding.apply {
            root.setOnClickListener { _eventListener?.onClick() }
        }
        bubbleBinding.apply {
            root.setOnClickListener {
                showDetail()
            }
            root.setOnTouchListener { view, motionEvent ->
                val x = motionEvent.rawX.toInt()
                val y = motionEvent.rawY.toInt()
                val action = motionEvent.action
                when (action) {
                    MotionEvent.ACTION_UP -> {
                        if (bubbleLastAction == MotionEvent.ACTION_DOWN && initialTouchPosition.equals(x, y)) {
                            view.performClick()
                        } else {
                            val idleOverTrash = _eventListener?.onIdle(Point(x, y)) ?: false
                            if (!idleOverTrash) {
                                animateToRear()
                            }
                        }
                        animateClickUp()
                    }
                    MotionEvent.ACTION_DOWN -> {
                        initialPosition = Point(layoutParams.x, layoutParams.y)
                        initialTouchPosition = Point(x, y)
                        animateClickDown()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newX = initialPosition.x + (x - initialTouchPosition.x)
                        val newY = initialPosition.y + (y - initialTouchPosition.y)
                        if (_movable) {
                            layoutParams.x = newX
                            layoutParams.y = newY
                            windowManager.updateViewLayout(this@BubbleView, layoutParams)
                        }
                        _eventListener?.onMove(Point(x, y))
                    }
                }
                bubbleLastAction = action
                true
            }
        }
    }

    private fun animateToRear() {
        val w = this@BubbleView.width
        val rear = resources.displayMetrics.let {
            if ((layoutParams.x + w/2) >= it.widthPixels/2)
                it.widthPixels - (w*0.85f).toInt()
            else
                0 - (w*0.15f).toInt()
        }
        animateTo(Point(rear, layoutParams.y))
    }

    private fun animateTo(point: Point) {
        animate(Point(layoutParams.x, layoutParams.y), point)
    }

    private fun animate(from: Point, to: Point) {
        if (!isInEditMode) {
            val animX = ValueAnimator.ofInt(from.x, to.x).apply {
                addUpdateListener {
                    val x = it.animatedValue as Int
                    layoutParams.x = x
                    windowManager.updateViewLayout(this@BubbleView, layoutParams)
                }
            }
            val animY = ValueAnimator.ofInt(from.y, to.y).apply {
                addUpdateListener {
                    val y = it.animatedValue as Int
                    layoutParams.y = y
                    windowManager.updateViewLayout(this@BubbleView, layoutParams)
                }
            }
            AnimatorSet().apply {
                playTogether(animX, animY)
                duration = resources.getInteger(R.integer.medium_duration).toLong()
                interpolator = OvershootInterpolator()
                start()
            }
        }
    }

    private fun animatePopOut() {
        val popOut = AnimationUtils.loadAnimation(context, R.anim.anim_pop_out)
        bubbleBinding.root.startAnimation(popOut)
    }

    private fun animateClickDown() {
        if (!isInEditMode) {
            val scaleDown = AnimationUtils.loadAnimation(context, R.anim.anim_scale_down)
            bubbleBinding.root.startAnimation(scaleDown)
        }
    }

    private fun animateClickUp() {
        if (!isInEditMode) {
            val scaleUp = AnimationUtils.loadAnimation(context, R.anim.anim_scale_up)
            bubbleBinding.root.startAnimation(scaleUp)
        }
    }

    // public
    interface BubbleEventListener {
        fun onClick() = Unit
        fun onMove(point: Point) = Unit
        fun onIdle(point: Point): Boolean = false
    }

    fun setOnEventListener(listener: BubbleEventListener) {
        _eventListener = listener
    }

    fun moveToTrash(trashCenter: Point) {
        if (_movable) {
            animateTo(trashCenter)
            _movable = false
        }
    }

    fun moveOutOfTrash() {
        _movable = true
    }
}