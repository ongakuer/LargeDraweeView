package me.relex.sample.largeimage.loading

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import me.relex.largeimage.LoadingViewProvider

class ImageLoadingViewProvider : LoadingViewProvider {

    override fun createLoadingView(context: Context, container: ViewGroup): View {
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        }
        return ImageLoadingView(context).apply {
            setLayoutParams(layoutParams)
        }
    }

    override fun progress(progress: Int) {
    }
}
