package me.relex.largeimage

import androidx.annotation.FloatRange

interface PullDownListener {
    fun onDragPercent(@FloatRange(from = 0.0, to = 1.0) percent: Float)
    fun onPullDownConfirm()
}