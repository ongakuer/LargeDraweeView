package me.relex.sample.largeimage.loading

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec
import android.view.animation.LinearInterpolator

class ImageLoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var _size: Int
    private var _animateValue: Int = 0
    private var _animator: ValueAnimator? = null
    private var _paint: Paint

    init {
        _size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics
        ).toInt()

        _paint = Paint().apply {
            setColor(Color.LTGRAY)
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeCap = Paint.Cap.ROUND
        }
    }

    fun setColor(color: Int) {
        _paint.setColor(color)
        invalidate()
    }

    fun setSize(size: Int) {
        if (size == _size) return
        _size = size
        requestLayout()
    }

    private val _updateListener = AnimatorUpdateListener {
        _animateValue = it.getAnimatedValue() as Int
        invalidate()
    }

    fun start() {
        var animator = _animator
        if (animator == null) {
            animator = ValueAnimator.ofInt(0, LINE_COUNT - 1).apply {
                addUpdateListener(_updateListener)
                setDuration(600)
                repeatMode = ValueAnimator.RESTART
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
            _animator = animator
            animator.start()
        } else if (!animator.isStarted) {
            animator.start()
        }
    }

    fun stop() {
        var animator = _animator
        if (animator != null) {
            animator.removeUpdateListener(_updateListener)
            animator.removeAllUpdateListeners()
            animator.cancel()
        }
        _animator = null
    }

    private fun drawLoading(canvas: Canvas, rotateDegrees: Int) {
        val lineWidth = _size / 12f
        val lineHeight = _size / 6f
        _paint.strokeWidth = lineWidth

        canvas.rotate(rotateDegrees.toFloat(), _size / 2f, _size / 2f)
        canvas.translate(_size / 2f, _size / 2f)

        for (i in 0 until LINE_COUNT) {
            canvas.rotate(DEGREE_PER_LINE.toFloat())
            _paint.setAlpha((255f * (i + 1) / LINE_COUNT).toInt())
            canvas.translate(0f, -_size / 2f + lineWidth / 2f)
            canvas.drawLine(0f, 0f, 0f, lineHeight, _paint)
            canvas.translate(0f, _size / 2f - lineWidth / 2f)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val width = if (MeasureSpec.EXACTLY == widthSpecMode) {
            MeasureSpec.getSize(widthMeasureSpec)
        } else {
            _size + paddingLeft + paddingRight
        }

        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val height = if (MeasureSpec.EXACTLY == heightSpecMode) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            _size + paddingTop + paddingBottom
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        val offsetX = (width - _size) / 2f + paddingLeft
        val offsetY = (height - _size) / 2f + paddingTop
        canvas.translate(offsetX, offsetY)
        drawLoading(canvas, _animateValue * DEGREE_PER_LINE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            start()
        } else {
            stop()
        }
    }

    companion object {
        private const val LINE_COUNT = 12
        private const val DEGREE_PER_LINE = 360 / LINE_COUNT
    }
}
