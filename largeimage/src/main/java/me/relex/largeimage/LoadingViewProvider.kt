package me.relex.largeimage

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange

interface LoadingViewProvider {

    fun createLoadingView(context: Context, container: ViewGroup): View

    fun progress(@IntRange(from = 0, to = 100) progress: Int)
}
