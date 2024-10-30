package me.relex.largeimage

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class PullDownGestureHelper(
    context: Context, listener: GestureDetector.OnGestureListener
) {
    private val _touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val _gestureDetector: GestureDetector = GestureDetector(context, listener)

    var listener: PullDownListener? = null
    var enablePullDown = false
    var resetDuration = LargeDraweeView.DEFAULT_ANIMATION_DURATION_MS

    private var _velocityTracker: VelocityTracker? = null
    private var _downX = 0f
    private var _downY = 0f
    private var _currentState = STATE_IDLE

    fun onInterceptTouchEvent(ev: MotionEvent, targetView: View?): Boolean {
        if (targetView == null) {
            return false
        }
        if (_currentState == STATE_RESETTING) {
            return true
        }
        _gestureDetector.onTouchEvent(ev)

        if (!enablePullDown) {
            return false
        }

        val action = ev.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                _downX = ev.rawX
                _downY = ev.rawY
                targetView.parent.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs((ev.rawX - _downX))
                val deltaY = (ev.rawY - _downY)
                if (deltaY > _touchSlop && deltaX <= _touchSlop) {
                    targetView.parent.requestDisallowInterceptTouchEvent(true)
                    return true
                } else {
                    targetView.parent.requestDisallowInterceptTouchEvent(false)
                }
            }

            MotionEvent.ACTION_UP, //
            MotionEvent.ACTION_CANCEL -> {
                targetView.parent.requestDisallowInterceptTouchEvent(false)
            }

            else -> {}
        }
        return false
    }

    fun onTouchEvent(ev: MotionEvent, targetView: View?): Boolean {
        if (targetView == null || _currentState == STATE_RESETTING || !enablePullDown) {
            return false
        }
        val action = ev.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                addVelocityMovement(ev)
                targetView.parent.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                addVelocityMovement(ev)
                val deltaY = (ev.rawY - _downY)
                if ((deltaY > _touchSlop || _currentState == STATE_DRAGGING)) {
                    targetView.parent.requestDisallowInterceptTouchEvent(true)
                    _currentState = STATE_DRAGGING
                    moveView(targetView, ev.rawX, ev.rawY, _downX, _downY)
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                targetView.parent.requestDisallowInterceptTouchEvent(false)
                if (_currentState != STATE_DRAGGING) {
                    return false
                }
                val upX = ev.rawX
                val upY = ev.rawY

                val velocity = computeVelocity()
                if (velocity >= 1000 || abs((upY - _downY)) > targetView.height / 4f) {
                    _currentState = STATE_RESETTING
                    resetView(targetView, upX, upY, resetDuration.toLong())
                    listener?.onPullDownConfirm()
                } else {
                    _currentState = STATE_RESETTING
                    resetView(targetView, upX, upY, resetDuration.toLong())
                }
            }

            else -> {}
        }
        return false
    }

    private fun addVelocityMovement(event: MotionEvent) {
        var velocityTracker = _velocityTracker
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
            _velocityTracker = velocityTracker
        }
        velocityTracker.addMovement(event)
    }

    private fun computeVelocity(): Float {
        var yVelocity = 0f
        var velocityTracker = _velocityTracker
        if (velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(1000)
            yVelocity = velocityTracker.yVelocity
            releaseVelocity()
        }
        return yVelocity
    }

    private fun releaseVelocity() {
        _velocityTracker?.clear()
        _velocityTracker?.recycle()
        _velocityTracker = null
    }

    private fun moveView(
        targetView: View, movingX: Float, movingY: Float, originalDownX: Float, originalDownY: Float
    ) {
        val deltaX = movingX - originalDownX
        val deltaY = movingY - originalDownY
        var percent: Float
        var scale = 1f
        if (deltaY > 0) {
            percent = 1 - abs(deltaY) / targetView.height
            scale = min(max(percent, MIN_SCALE_SIZE), 1.0f)
            listener?.onDragPercent(percent)
        }
        targetView.translationX = deltaX
        targetView.translationY = deltaY
        targetView.scaleX = scale
        targetView.scaleY = scale
    }

    private fun resetView(targetView: View, upX: Float, upY: Float, animateDuration: Long) {
        val originalDownY = _downY
        val originalDownX = _downX

        if (!isEqual(upY, originalDownY)) {
            val valueAnimator = ValueAnimator.ofFloat(upY, originalDownY)
            valueAnimator.setDuration(animateDuration)
            valueAnimator.addUpdateListener(AnimatorUpdateListener {
                val y = it.getAnimatedValue() as Float
                val percent = (y - originalDownY) / (upY - originalDownY)
                val x = percent * (upX - originalDownX) + originalDownX

                moveView(targetView, x, y, originalDownX, originalDownY)
                if (isEqual(y, originalDownY)) {
                    _downY = 0f
                    _downX = 0f
                    _currentState = STATE_IDLE
                }
            })
            valueAnimator.interpolator = LinearOutSlowInInterpolator()
            valueAnimator.start()
        } else if (!isEqual(upX, originalDownX)) {
            val valueAnimator = ValueAnimator.ofFloat(upX, originalDownX)
            valueAnimator.setDuration(animateDuration)
            valueAnimator.addUpdateListener(AnimatorUpdateListener {
                val x = it.getAnimatedValue() as Float
                val percent = (x - originalDownX) / (upX - originalDownX)
                val y = percent * (upY - originalDownY) + originalDownY
                moveView(targetView, x, y, originalDownX, originalDownY)
                if (isEqual(x, originalDownX)) {
                    _downY = 0f
                    _downX = 0f
                    _currentState = STATE_IDLE
                }
            })
            valueAnimator.interpolator = LinearOutSlowInInterpolator()
            valueAnimator.start()
        } else {
            _currentState = STATE_IDLE
        }
    }

    companion object {
        private const val STATE_IDLE = 0
        private const val STATE_DRAGGING = 1
        private const val STATE_RESETTING = 2
        private const val MIN_SCALE_SIZE = 0.3f

        private fun isEqual(a: Float, b: Float): Boolean {
            return (abs((a - b)) < 0.00000001f)
        }
    }
}
