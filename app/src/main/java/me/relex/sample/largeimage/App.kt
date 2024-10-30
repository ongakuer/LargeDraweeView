package me.relex.sample.largeimage

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.facebook.drawee.backends.pipeline.Fresco

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Fresco.initialize(applicationContext)
    }
}

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingView: (view: android.view.View) -> T
) = lazy(LazyThreadSafetyMode.NONE) {
    val rootView = this.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
        ?: throw IllegalArgumentException("Use AppCompatActivity(@LayoutRes int contentLayoutId)")
    bindingView.invoke(rootView)
}
