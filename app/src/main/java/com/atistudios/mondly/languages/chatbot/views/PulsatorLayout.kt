package com.atistudios.mondly.languages.chatbot.views

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.*
import android.widget.RelativeLayout
import com.atistudios.mondly.languages.chatbot.R
import java.util.*

/***
 * fork from here https://github.com/booncol/Pulsator4Droid
 * changed scaling type
 */
class PulsatorLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    RelativeLayout(context, attrs, defStyleAttr) {

    private var mCount: Int = 0
    private var mDuration: Int = 0
    private var mRepeat: Int = 0
    private var mStartFromScratch: Boolean = false
    private var mColor: Int = 0
    private var mInterpolator: Int = 0

    private val mViews = ArrayList<View>()
    private var mAnimatorSet: AnimatorSet? = null
    private val mPaint: Paint?
    private var mRadius: Float = 0.toFloat()
    private var mCenterX: Float = 0.toFloat()
    private var mCenterY: Float = 0.toFloat()
    private var mIsStarted: Boolean = false

    val isStarted: Boolean
        @Synchronized get() = mAnimatorSet != null && mIsStarted

    /**
     * Get number of pulses.
     *
     * @return Number of pulses
     */
    /**
     * Set number of pulses.
     *
     * @param count Number of pulses
     */
    var count: Int
        get() = mCount
        set(count) {
            if (count < 0) {
                throw IllegalArgumentException("Count cannot be negative")
            }

            if (count != mCount) {
                mCount = count
                reset()
                invalidate()
            }
        }

    /**
     * Get pulse duration.
     *
     * @return Duration of single pulse in milliseconds
     */
    /**
     * Set single pulse duration.
     *
     * @param millis Pulse duration in milliseconds
     */
    var duration: Int
        get() = mDuration
        set(millis) {
            if (millis < 0) {
                throw IllegalArgumentException("Duration cannot be negative")
            }

            if (millis != mDuration) {
                mDuration = millis
                reset()
                invalidate()
            }
        }

    /**
     * Gets the current color of the pulse effect in integer
     * Defaults to Color.rgb(0, 116, 193);
     * @return an integer representation of color
     */
    /**
     * Sets the current color of the pulse effect in integer
     * Takes effect immediately
     * Usage: Color.parseColor("<hex-value>") or getResources().getColor(R.color.colorAccent)
     * @param color : an integer representation of color
    </hex-value> */
    var color: Int
        get() = mColor
        set(color) {
            if (color != mColor) {
                this.mColor = color

                if (mPaint != null) {
                    mPaint.color = color
                }
            }
        }

    /**
     * Get current interpolator type used for animating.
     *
     * @return Interpolator type as int
     */
    /**
     * Set current interpolator used for animating.
     *
     * @param type Interpolator type as int
     */
    var interpolator: Int
        get() = mInterpolator
        set(type) {
            if (type != mInterpolator) {
                mInterpolator = type
                reset()
                invalidate()
            }
        }

    private val mAnimatorListener = object : Animator.AnimatorListener {

        override fun onAnimationStart(animator: Animator) {
            mIsStarted = true
        }

        override fun onAnimationEnd(animator: Animator) {
            mIsStarted = false
        }

        override fun onAnimationCancel(animator: Animator) {
            mIsStarted = false
        }

        override fun onAnimationRepeat(animator: Animator) {}

    }

    init {

        // get attributes
        val attr = context.theme.obtainStyledAttributes(
            attrs, R.styleable.Pulsator4Droid, 0, 0
        )

        mCount = DEFAULT_COUNT
        mDuration = DEFAULT_DURATION
        mRepeat = DEFAULT_REPEAT
        mStartFromScratch =
            DEFAULT_START_FROM_SCRATCH
        mColor = DEFAULT_COLOR
        mInterpolator = DEFAULT_INTERPOLATOR

        try {
            mCount = attr.getInteger(
                R.styleable.Pulsator4Droid_pulse_count,
                DEFAULT_COUNT
            )
            mDuration = attr.getInteger(
                R.styleable.Pulsator4Droid_pulse_duration,
                DEFAULT_DURATION
            )
            mRepeat = attr.getInteger(
                R.styleable.Pulsator4Droid_pulse_repeat,
                DEFAULT_REPEAT
            )
            mStartFromScratch = attr.getBoolean(
                R.styleable.Pulsator4Droid_pulse_startFromScratch,
                DEFAULT_START_FROM_SCRATCH
            )
            mColor = attr.getColor(
                R.styleable.Pulsator4Droid_pulse_color,
                DEFAULT_COLOR
            )
            mInterpolator = attr.getInteger(
                R.styleable.Pulsator4Droid_pulse_interpolator,
                DEFAULT_INTERPOLATOR
            )
        } finally {
            attr.recycle()
        }

        // create paint
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.color = mColor

        // create views
        build()
    }

    /**
     * Start pulse animation.
     */
    @Synchronized
    fun start() {
        if (mAnimatorSet == null || mIsStarted) {
            return
        }

        mAnimatorSet!!.start()

        if (!mStartFromScratch) {
            val animators = mAnimatorSet!!.childAnimations
            for (animator in animators) {
                val objectAnimator = animator as ObjectAnimator

                val delay = objectAnimator.startDelay
                objectAnimator.startDelay = 0
                objectAnimator.currentPlayTime = mDuration - delay
            }
        }
    }

    /**
     * Stop pulse animation.
     */
    @Synchronized
    fun stop() {
        if (mAnimatorSet == null || !mIsStarted) {
            return
        }

        mAnimatorSet!!.end()
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val height = View.MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom

        mCenterX = width * 0.5f
        mCenterY = height * 0.5f
        mRadius = Math.min(width, height) * 0.5f

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Remove all views and animators.
     */
    private fun clear() {
        // remove animators
        stop()

        // remove old views
        for (view in mViews) {
            removeView(view)
        }
        mViews.clear()
    }

    /**
     * Build pulse views and animators.
     */
    private fun build() {
        // create views and animators
        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )

        val repeatCount = if (mRepeat == INFINITE) ObjectAnimator.INFINITE else mRepeat

        val animators = ArrayList<Animator>()
        val pulseView = PulseView(context)
        pulseView.scaleX = 0.66f
        pulseView.scaleY = 0.66f
        pulseView.alpha = 0.66f

        addView(pulseView, 0, layoutParams)
        mViews.add(pulseView)
        for (index in 0 until mCount - 1) {
            // setup view
            val pulseView = PulseView(context)
            pulseView.scaleX = 0.66f
            pulseView.scaleY = 0.66f
            pulseView.alpha = 0.66f

            addView(pulseView, index + 1, layoutParams)
            mViews.add(pulseView)

            val delay = (index * mDuration / mCount).toLong()

            // setup animators
            val scaleXAnimator = ObjectAnimator.ofFloat(pulseView, "ScaleX", 0.66f, 1f)
            scaleXAnimator.repeatCount = repeatCount
            scaleXAnimator.repeatMode = ObjectAnimator.RESTART
            scaleXAnimator.startDelay = delay
            animators.add(scaleXAnimator)

            val scaleYAnimator = ObjectAnimator.ofFloat(pulseView, "ScaleY", 0.66f, 1f)
            scaleYAnimator.repeatCount = repeatCount
            scaleYAnimator.repeatMode = ObjectAnimator.RESTART
            scaleYAnimator.startDelay = delay
            animators.add(scaleYAnimator)

            val alphaAnimator = ObjectAnimator.ofFloat(pulseView, "Alpha", 1f, 0f)
            alphaAnimator.repeatCount = repeatCount
            alphaAnimator.repeatMode = ObjectAnimator.RESTART
            alphaAnimator.startDelay = delay
            animators.add(alphaAnimator)
        }

        mAnimatorSet = AnimatorSet()
        mAnimatorSet!!.playTogether(animators)
        mAnimatorSet!!.interpolator =
            createInterpolator(mInterpolator)
        mAnimatorSet!!.duration = mDuration.toLong()
        mAnimatorSet!!.addListener(mAnimatorListener)
    }

    /**
     * Reset views and animations.
     */
    private fun reset() {
        val isStarted = isStarted

        clear()
        build()

        if (isStarted) {
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (mAnimatorSet != null) {
            mAnimatorSet!!.cancel()
            mAnimatorSet = null
        }
    }

    private inner class PulseView(context: Context) : View(context) {

        override fun onDraw(canvas: Canvas) {
            canvas.drawCircle(mCenterX, mCenterY, mRadius, mPaint!!)
        }

    }

    companion object {

        val INFINITE = 0

        val INTERP_LINEAR = 0
        val INTERP_ACCELERATE = 1
        val INTERP_DECELERATE = 2
        val INTERP_ACCELERATE_DECELERATE = 3

        private val DEFAULT_COUNT = 4
        private val DEFAULT_COLOR = Color.rgb(0, 116, 193)
        private val DEFAULT_DURATION = 3000
        private val DEFAULT_REPEAT = INFINITE
        private val DEFAULT_START_FROM_SCRATCH = true
        private val DEFAULT_INTERPOLATOR =
            INTERP_LINEAR

        /**
         * Create interpolator from type.
         *
         * @param type Interpolator type as int
         * @return Interpolator object of type
         */
        private fun createInterpolator(type: Int): Interpolator {
            when (type) {
                INTERP_ACCELERATE -> return AccelerateInterpolator()
                INTERP_DECELERATE -> return DecelerateInterpolator()
                INTERP_ACCELERATE_DECELERATE -> return AccelerateDecelerateInterpolator()
                else -> return LinearInterpolator()
            }
        }
    }

}