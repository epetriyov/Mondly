package com.atistudios.mondly.languages.chatbot.views


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import com.atistudios.mondly.languages.chatbot.R
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * fork from https://github.com/silvestrpredko/DotProgressBarExample
 * removed animation from onAttachedToWindow
 */
class DotProgressBar : View {

    /**
     * Dots amount
     */
    private var dotAmount: Int = 0

    /**
     * Drawing tools
     */
    private var primaryPaint: Paint? = null
    private var startPaint: Paint? = null
    private var endPaint: Paint? = null

    /**
     * Animation tools
     */
    private var animationTime: Long = 0
    private var animatedRadius: Float = 0.toFloat()
    private var isFirstLaunch = true
    private var startValueAnimator: ValueAnimator? = null
    private var endValueAnimator: ValueAnimator? = null

    /**
     * Circle size
     */
    private var dotRadius: Float = 0.toFloat()
    private var bounceDotRadius: Float = 0.toFloat()
    /**
     * Circle coordinates
     */
    private var xCoordinate: Float = 0.toFloat()
    private var dotPosition: Int = 0

    /**
     * Colors
     */
    private var startColor: Int = 0
    private var endColor: Int = 0

    /**
     * This value detect direction of circle animation direction
     * [DotProgressBar.RIGHT_DIRECTION] and [DotProgressBar.LEFT_DIRECTION]
     */
    var animationDirection: Int = 0
        internal set

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        initializeAttributes(attrs)
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeAttributes(attrs)
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializeAttributes(attrs)
        init()
    }

    constructor(context: Context) : super(context) {
        initializeAttributes(null)
        init()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)

        if (measuredHeight > measuredWidth) {
            dotRadius = (measuredWidth / dotAmount / 4).toFloat()
        } else {
            dotRadius = (measuredHeight / 4).toFloat()
        }

        bounceDotRadius = dotRadius + dotRadius / 3
        val circlesWidth = dotAmount * (dotRadius * 2) + dotRadius * (dotAmount - 1)
        xCoordinate = (measuredWidth - circlesWidth) / 2 + dotRadius
    }

    private fun initializeAttributes(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.DotProgressBar,
                0, 0
            )

            try {
                setDotAmount(a.getInteger(R.styleable.DotProgressBar_amount, 5))
                setAnimationTime(
                    animationTime = a.getInteger(
                        R.styleable.DotProgressBar_duration,
                        resources.getInteger(android.R.integer.config_mediumAnimTime)
                    ).toLong()
                )
                setStartColor(
                    a.getInteger(
                        R.styleable.DotProgressBar_startColor,
                        ContextCompat.getColor(context, android.R.color.black)
                    )
                )
                setEndColor(
                    a.getInteger(
                        R.styleable.DotProgressBar_endColor,
                        ContextCompat.getColor(context, android.R.color.black)
                    )
                )
                animationDirection = a.getInt(R.styleable.DotProgressBar_animationDirection, 1)
            } finally {
                a.recycle()
            }

        } else {
            setDotAmount(5)
            setAnimationTime(resources.getInteger(android.R.integer.config_mediumAnimTime).toLong())
            setStartColor(ContextCompat.getColor(context, android.R.color.black))
            setEndColor(ContextCompat.getColor(context, android.R.color.black))
            animationDirection = 1
        }
    }

    private fun init() {
        primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        primaryPaint!!.color = startColor
        primaryPaint!!.strokeJoin = Paint.Join.ROUND
        primaryPaint!!.strokeCap = Paint.Cap.ROUND
        primaryPaint!!.strokeWidth = 20f

        startPaint = Paint(primaryPaint)
        endPaint = Paint(primaryPaint)

        startValueAnimator = ValueAnimator.ofInt(startColor, endColor)
        startValueAnimator!!.duration = animationTime
        startValueAnimator!!.setEvaluator(ArgbEvaluator())
        startValueAnimator!!.addUpdateListener { animation -> startPaint!!.color = animation.animatedValue as Int }

        endValueAnimator = ValueAnimator.ofInt(endColor, startColor)
        endValueAnimator!!.duration = animationTime
        endValueAnimator!!.setEvaluator(ArgbEvaluator())
        endValueAnimator!!.addUpdateListener { animation -> endPaint!!.color = animation.animatedValue as Int }
    }

    /**
     * setters
     */
    internal fun setDotAmount(amount: Int) {
        this.dotAmount = amount
    }

    internal fun setStartColor(@ColorInt color: Int) {
        this.startColor = color
    }

    internal fun setEndColor(@ColorInt color: Int) {
        this.endColor = color
    }

    internal fun setAnimationTime(animationTime: Long) {
        this.animationTime = animationTime
    }

    private fun setDotPosition(dotPosition: Int) {
        this.dotPosition = dotPosition
    }

    /**
     * Set amount of dots, it will be restarted your view
     * @param amount number of dots, dot size automatically adjust
     */
    fun changeDotAmount(amount: Int) {
        stopAnimation()
        setDotAmount(amount)
        setDotPosition(0)
        reinitialize()
    }

    /**
     * It will be restarted your view
     */
    fun changeStartColor(@ColorInt color: Int) {
        stopAnimation()
        setStartColor(color)
        reinitialize()
    }

    /**
     * It will be restarted your view
     */
    fun changeEndColor(@ColorInt color: Int) {
        stopAnimation()
        setEndColor(color)
        reinitialize()
    }

    /**
     * It will be restarted your view
     */
    fun changeAnimationTime(animationTime: Long) {
        stopAnimation()
        setAnimationTime(animationTime)
        reinitialize()
    }

    /**
     * Change animation direction, doesn't restart view.
     * @param animationDirection left or right animation direction
     */
    fun changeAnimationDirection(@AnimationDirection animationDirection: Int) {
        this@DotProgressBar.animationDirection = animationDirection
    }

    /**
     * Reinitialize animators instances
     */
    internal fun reinitialize() {
        init()
        requestLayout()
        startAnimation()
    }

    private fun drawCirclesLeftToRight(canvas: Canvas, radius: Float) {
        var step = 0f
        for (i in 0 until dotAmount) {
            drawCircles(canvas, i, step, radius)
            step += dotRadius * 3
        }
    }

    private fun drawCirclesRightToLeft(canvas: Canvas, radius: Float) {
        var step = 0f
        for (i in dotAmount - 1 downTo 0) {
            drawCircles(canvas, i, step, radius)
            step += dotRadius * 3
        }
    }

    private fun drawCircles(canvas: Canvas, i: Int, step: Float, radius: Float) {
        if (dotPosition == i) {
            drawCircleUp(canvas, step, radius)
        } else {
            if (i == dotAmount - 1 && dotPosition == 0 && !isFirstLaunch || dotPosition - 1 == i) {
                drawCircleDown(canvas, step, radius)
            } else {
                drawCircle(canvas, step)
            }
        }
    }

    /**
     * Circle radius is grow
     */
    private fun drawCircleUp(canvas: Canvas, step: Float, radius: Float) {
        canvas.drawCircle(
            xCoordinate + step,
            (measuredHeight / 2).toFloat(),
            dotRadius + radius,
            startPaint!!
        )
    }

    private fun drawCircle(canvas: Canvas, step: Float) {
        canvas.drawCircle(
            xCoordinate + step,
            (measuredHeight / 2).toFloat(),
            dotRadius,
            primaryPaint!!
        )
    }

    /**
     * Circle radius is decrease
     */
    private fun drawCircleDown(canvas: Canvas, step: Float, radius: Float) {
        canvas.drawCircle(
            xCoordinate + step,
            (measuredHeight / 2).toFloat(),
            bounceDotRadius - radius,
            endPaint!!
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (animationDirection < 0) {
            drawCirclesRightToLeft(canvas, animatedRadius)
        } else {
            drawCirclesLeftToRight(canvas, animatedRadius)
        }
    }

    private fun stopAnimation() {
        this.clearAnimation()
        postInvalidate()
    }

    private fun startAnimation() {
        val bounceAnimation = BounceAnimation()
        bounceAnimation.duration = animationTime
        bounceAnimation.repeatCount = Animation.INFINITE
        bounceAnimation.interpolator = LinearInterpolator()
        bounceAnimation.setAnimationListener(object : AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
            }

            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation) {
                dotPosition++
                if (dotPosition == dotAmount) {
                    dotPosition = 0
                }

                startValueAnimator!!.start()

                if (!isFirstLaunch) {
                    endValueAnimator!!.start()
                }

                isFirstLaunch = false
            }
        })
        startAnimation(bounceAnimation)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)

        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            stopAnimation()
        } else {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(RIGHT_DIRECTION, LEFT_DIRECTION)
    annotation class AnimationDirection

    private inner class BounceAnimation : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            super.applyTransformation(interpolatedTime, t)
            animatedRadius = (bounceDotRadius - dotRadius) * interpolatedTime
            invalidate()
        }
    }

    companion object {

        const val RIGHT_DIRECTION = 1
        const val LEFT_DIRECTION = -1

        fun darker(color: Int, factor: Float): Int {
            val a = Color.alpha(color)
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            return Color.argb(
                a,
                Math.max((r * factor).toInt(), 0),
                Math.max((g * factor).toInt(), 0),
                Math.max((b * factor).toInt(), 0)
            )
        }
    }
}