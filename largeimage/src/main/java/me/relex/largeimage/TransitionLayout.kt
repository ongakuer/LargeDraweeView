package me.relex.largeimage

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.Interpolator
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator

class TransitionLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val _transitionRect = Rect()
    private val _imageSize = Rect()
    private val _matrix = Matrix()
    private val _clipRect = Rect()

    private var _enterListener: EnterAnimationListener? = null
    private var _exitListener: ExitAnimationListener? = null
    private var _interpolatorProvider: InterpolatorProvider? = null
    private var _isInExiting = false

    private var _enterDuration: Int = LargeDraweeView.DEFAULT_ANIMATION_DURATION_MS
    private var _exitDuration: Int = LargeDraweeView.DEFAULT_ANIMATION_DURATION_MS

    init {
        context.withStyledAttributes(attrs, R.styleable.TransitionLayout, defStyleAttr) {
            _enterDuration = getInt(
                R.styleable.TransitionLayout_enter_duration,
                LargeDraweeView.DEFAULT_ANIMATION_DURATION_MS
            )
            _exitDuration = getInt(
                R.styleable.TransitionLayout_exit_duration,
                LargeDraweeView.DEFAULT_ANIMATION_DURATION_MS
            )
        }
    }

    fun startShareTransition(transitionRect: Rect, imageSize: Rect) {
        if (!transitionRect.isEmpty) {
            _transitionRect.set(transitionRect)
            _imageSize.set(imageSize)
        } else {
            _transitionRect.setEmpty()
            _imageSize.setEmpty()
            _matrix.reset()
        }
        if (measuredWidth == 0) {
            getViewTreeObserver().addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    runEnterAnimation()
                }
            })
        } else {
            runEnterAnimation()
        }
    }

    private data class TransitionInfo(
        val scale: Float, val translateX: Float, val translateY: Float, val rect: Rect
    )

    private fun calculateTranslate(transitionRect: Rect, imageSize: Rect): TransitionInfo? {
        if (transitionRect.isEmpty) return null

        val calculateRect = Rect(transitionRect)
        val location = IntArray(2)
        getLocationOnScreen(location)
        calculateRect.offset(-location[0], -location[1])

        val viewWidth = measuredWidth.toFloat()
        val viewHeight = measuredHeight.toFloat()

        val realImageRect = RectF()

        if (!imageSize.isEmpty) {
            var imageWidthInset: Float
            var imageHeightInset: Float
            realImageRect.set(calculateRect)

            if (imageSize.width() * calculateRect.height() > calculateRect.width() * imageSize.height()) {
                /// Height Fixed
                val imageWidth = imageSize.width().toFloat() / imageSize.height()
                    .toFloat() * calculateRect.height()
                imageWidthInset = (calculateRect.width() - imageWidth) / 2f
                realImageRect.inset(imageWidthInset, 0f)
            } else {
                /// Width Fixed
                val imageHeight = imageSize.height().toFloat() / imageSize.width()
                    .toFloat() * calculateRect.width()
                imageHeightInset = (calculateRect.height() - imageHeight) / 2f
                realImageRect.inset(0f, imageHeightInset)
            }
        } else {
            realImageRect.set(calculateRect)
        }

        val imageWidth = realImageRect.width()
        val imageHeight = realImageRect.height()

        var viewScale: Float
        var translateX: Float
        var translateY: Float

        if (imageWidth / imageHeight > viewWidth / viewHeight) {
            // Width Match Parent
            viewScale = imageWidth / viewWidth
            translateX = realImageRect.left - (viewWidth * viewScale - imageWidth) / 2f
            translateY = realImageRect.top - (viewHeight * viewScale - imageHeight) / 2f
        } else {
            // Height Match Parent
            viewScale = imageHeight / viewHeight
            translateX = realImageRect.left - (viewWidth * viewScale - imageWidth) / 2f
            translateY = realImageRect.top - (viewHeight * viewScale - imageHeight) / 2f
        }

        return TransitionInfo(viewScale, translateX, translateY, calculateRect)
    }

    private fun runEnterAnimation() {
        val transitionInfo = calculateTranslate(_transitionRect, _imageSize)

        var interpolator = _interpolatorProvider?.getEnterInterpolator()
        if (interpolator == null) {
            interpolator = LinearOutSlowInInterpolator()
        }

        var animator: ValueAnimator
        if (transitionInfo == null) {
            // Use Fade Animation
            _matrix.reset()
            animator = ObjectAnimator.ofFloat(this, ALPHA, 0f, 1f)
        } else {
            val viewWidth = measuredWidth.toFloat()
            val viewHeight = measuredHeight.toFloat()
            animator = ValueAnimator.ofFloat(0f, 1f)
            animator.addUpdateListener(AnimatorUpdateListener {
                val fraction = it.animatedFraction
                val animatorScale = transitionInfo.scale + (1 - transitionInfo.scale) * fraction
                _matrix.reset()
                _matrix.postScale(animatorScale, animatorScale)
                _matrix.postTranslate(
                    transitionInfo.translateX - transitionInfo.translateX * fraction,
                    transitionInfo.translateY - transitionInfo.translateY * fraction
                )
                _clipRect.left = (transitionInfo.rect.left - transitionInfo.rect.left * fraction).toInt()
                _clipRect.top = (transitionInfo.rect.top - transitionInfo.rect.top * fraction).toInt()
                _clipRect.right = (viewWidth - (1 - fraction) * (viewWidth - transitionInfo.rect.right)).toInt()
                _clipRect.bottom = (viewHeight - (1 - fraction) * (viewHeight - transitionInfo.rect.bottom)).toInt()
                invalidate()
            })
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                _enterListener?.onEnterAnimationComplete()
            }
        })
        animator.interpolator = interpolator
        animator.setDuration(_enterDuration.toLong())
        animator.start()
    }

    fun exitShareTransition(rect: Rect = _transitionRect, imageSize: Rect = _imageSize) {
        if (_isInExiting) {
            return
        }
        _isInExiting = true
        val transitionInfo = calculateTranslate(rect, imageSize)
        var interpolator = _interpolatorProvider?.getExitInterpolator()
        if (interpolator == null) {
            interpolator = FastOutSlowInInterpolator()
        }

        var animator: ValueAnimator

        if (transitionInfo == null) {
            // Use Fade Animation
            _matrix.reset()
            animator = ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f)
        } else {
            val viewWidth = measuredWidth.toFloat()
            val viewHeight = measuredHeight.toFloat()

            animator = ValueAnimator.ofFloat(0f, 1f)
            animator.addUpdateListener(AnimatorUpdateListener {
                val fraction = 1 - it.animatedFraction
                val animatorScale = transitionInfo.scale + (1 - transitionInfo.scale) * fraction
                _matrix.reset()
                _matrix.postScale(animatorScale, animatorScale)
                _matrix.postTranslate(
                    transitionInfo.translateX - transitionInfo.translateX * fraction,
                    transitionInfo.translateY - transitionInfo.translateY * fraction
                )

                _clipRect.left = (transitionInfo.rect.left - transitionInfo.rect.left * fraction).toInt()
                _clipRect.top = (transitionInfo.rect.top - transitionInfo.rect.top * fraction).toInt()
                _clipRect.right = (viewWidth - (1 - fraction) * (viewWidth - transitionInfo.rect.right)).toInt()
                _clipRect.bottom = (viewHeight - (1 - fraction) * (viewHeight - transitionInfo.rect.bottom)).toInt()
                invalidate()
            })
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                _isInExiting = false
                _exitListener?.onExitAnimationComplete()
            }
        })
        animator.interpolator = interpolator
        animator.setDuration(_exitDuration.toLong())
        animator.start()
    }

    override fun draw(canvas: Canvas) {
        if (!_clipRect.isEmpty) {
            canvas.clipRect(_clipRect)
        }
        canvas.setMatrix(_matrix)
        super.draw(canvas)
    }

    fun setEnterAnimationListener(listener: EnterAnimationListener?) {
        _enterListener = listener
    }

    fun setExitAnimationListener(listener: ExitAnimationListener?) {
        _exitListener = listener
    }

    fun setInterpolatorProvider(interpolatorProvider: InterpolatorProvider?) {
        _interpolatorProvider = interpolatorProvider
    }

    interface EnterAnimationListener {
        fun onEnterAnimationComplete()
    }

    interface InterpolatorProvider {
        fun getEnterInterpolator(): Interpolator

        fun getExitInterpolator(): Interpolator
    }

    interface ExitAnimationListener {
        fun onExitAnimationComplete()
    }
}
