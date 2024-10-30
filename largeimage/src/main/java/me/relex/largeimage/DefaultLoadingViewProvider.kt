package me.relex.largeimage

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar

internal class DefaultLoadingViewProvider : LoadingViewProvider {

    override fun createLoadingView(context: Context, container: ViewGroup): View {
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        return ProgressBar(context).apply { setLayoutParams(layoutParams) }
    }

    override fun progress(progress: Int) {
    }
}
