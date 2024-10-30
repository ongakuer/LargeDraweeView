package me.relex.sample.largeimage.loading

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import me.relex.largeimage.LoadingViewProvider

class TextViewProvider : LoadingViewProvider {
    private var _textView: TextView? = null

    override fun createLoadingView(context: Context, container: ViewGroup): View {
        val textView = TextView(context).apply {
            setTextColor(Color.BLACK)
            textSize = 16f
        }
        _textView = textView

        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        return textView.apply {
            setLayoutParams(layoutParams)
        }
    }

    override fun progress(progress: Int) {
        _textView?.text = "Loading ${progress}%"
    }
}
