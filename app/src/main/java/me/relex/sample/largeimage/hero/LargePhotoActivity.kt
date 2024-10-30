package me.relex.sample.largeimage.hero

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import me.relex.largeimage.LargeImageInfo
import me.relex.largeimage.PullDownListener
import me.relex.largeimage.TransitionLayout.ExitAnimationListener
import me.relex.sample.largeimage.R
import me.relex.sample.largeimage.databinding.ActivityLargePhotoBinding
import me.relex.sample.largeimage.viewBinding
import kotlin.math.max
import kotlin.math.min

class LargePhotoActivity : AppCompatActivity(R.layout.activity_large_photo) {
    private val binding by viewBinding(ActivityLargePhotoBinding::bind)
    private lateinit var largeImageInfo: LargeImageInfo
    private val onBackPressedCallback: OnBackPressedCallback = object :
        OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            binding.backgroundView.animate().alpha(0f).setDuration(300L).start()
            binding.imageView.reset()
            binding.transitionLayout.exitShareTransition()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }

        try {
            largeImageInfo = requireNotNull(
                IntentCompat.getParcelableExtra(
                    intent, BUNDLE_LARGE_IMAGE_INFO, LargeImageInfo::class.java
                )
            )
        } catch (_: Exception) {
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        configWindows()
        initViews()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configWindows()
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overridePendingTransition(0, 0)
        }
    }

    private fun configWindows() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightNavigationBars = false
            isAppearanceLightStatusBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    private fun initViews() {
        binding.imageView.apply {
            setOnClickListener { onBackPressedCallback.handleOnBackPressed() }
            setPullDownListener(object : PullDownListener {
                private var _isPullDownConfirmed = false

                override fun onDragPercent(percent: Float) {
                    if (_isPullDownConfirmed) return
                    binding.backgroundView.alpha = min(max(percent, 0.4f), 1f)
                }

                override fun onPullDownConfirm() {
                    _isPullDownConfirmed = true
                    onBackPressedCallback.handleOnBackPressed()
                }
            })
            load(largeImageInfo)
        }

        binding.transitionLayout.apply {
            setExitAnimationListener(object : ExitAnimationListener {
                override fun onExitAnimationComplete() {
                    finish()
                }
            })
            startShareTransition(
                largeImageInfo.sharedTransitionRect, largeImageInfo.imageSize
            )
        }
        binding.backgroundView.animate().alpha(1f).setDuration(250L).start()
    }

    companion object {
        private const val BUNDLE_LARGE_IMAGE_INFO = "large_image_info"

        fun startActivity(context: Context, info: LargeImageInfo) {
            context.startActivity(Intent(context, LargePhotoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(BUNDLE_LARGE_IMAGE_INFO, info)
            })
        }
    }
}