package me.relex.sample.largeimage.hero

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import me.relex.largeimage.LargeDraweeView
import me.relex.largeimage.LargeImageInfo
import me.relex.largeimage.PullDownListener
import me.relex.largeimage.TransitionLayout.ExitAnimationListener
import me.relex.sample.largeimage.R
import me.relex.sample.largeimage.databinding.ActivityLargeGalleryBinding
import me.relex.sample.largeimage.databinding.ItemLargeGalleryBinding
import me.relex.sample.largeimage.viewBinding
import kotlin.math.max
import kotlin.math.min

class LargeGalleryActivity : AppCompatActivity(R.layout.activity_large_gallery) {
    private val binding by viewBinding(ActivityLargeGalleryBinding::bind)

    private lateinit var list: List<LargeImageInfo>
    private var currentPosition = 0
    private val imageAdapter by lazy(LazyThreadSafetyMode.NONE) { ImagePagerAdapter() }
    private val onBackPressedCallback: OnBackPressedCallback = object :
        OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            binding.backgroundView.animate().alpha(0f).setDuration(300L).start()
            imageAdapter.getCurrentView(currentPosition).reset()
            val imageInfo = list[currentPosition]
            binding.transitionLayout.exitShareTransition(
                imageInfo.sharedTransitionRect, imageInfo.imageSize
            )
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
            currentPosition = intent.getIntExtra(BUNDLE_POSITION, 0)
            list = requireNotNull(
                IntentCompat.getParcelableArrayListExtra(
                    intent, BUNDLE_LARGE_IMAGE_INFO_LIST, LargeImageInfo::class.java
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.root,
            OnApplyWindowInsetsListener { v, insets ->
                binding.indicatorView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                }
                return@OnApplyWindowInsetsListener insets
            })

        binding.viewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
            }
        })
        binding.viewPager.adapter = imageAdapter
        binding.indicatorView.setViewPager(binding.viewPager)
        binding.indicatorView.isVisible = list.size > 1
        binding.viewPager.currentItem = currentPosition

        val imageInfo = list[currentPosition]
        binding.transitionLayout.apply {
            setExitAnimationListener(object : ExitAnimationListener {
                override fun onExitAnimationComplete() {
                    finish()
                }
            })
            startShareTransition(
                imageInfo.sharedTransitionRect, imageInfo.imageSize
            )
        }
        binding.backgroundView.animate().alpha(1f).setDuration(250L).start()
    }

    private inner class ImagePagerAdapter : PagerAdapter() {
        private val _sparseArray = SparseArray<LargeDraweeView>()

        override fun getCount(): Int {
            return if (list.isEmpty()) 0 else list.size
        }

        override fun isViewFromObject(view: View, targetObject: Any): Boolean {
            return view === targetObject
        }

        override fun destroyItem(
            container: ViewGroup, position: Int, targetObject: Any
        ) {
            _sparseArray.remove(position)
            container.removeView(targetObject as View)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val imageView = ItemLargeGalleryBinding.inflate(
                LayoutInflater.from(container.context), container, false
            ).root
            imageView.apply {
                setInternalLoadingEnable(true)
                setPullDownGestureEnable(true)
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
            }
            container.addView(
                imageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            imageView.load(list[position])
            _sparseArray.put(position, imageView)
            return imageView
        }

        fun getCurrentView(position: Int): LargeDraweeView {
            return _sparseArray[position]
        }
    }

    companion object {
        private const val BUNDLE_LARGE_IMAGE_INFO_LIST = "large_image_info_list"
        private const val BUNDLE_POSITION = "bundle_position"

        fun startActivity(context: Context, infoList: List<LargeImageInfo>, position: Int = 0) {
            context.startActivity(Intent(context, LargeGalleryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putParcelableArrayListExtra(BUNDLE_LARGE_IMAGE_INFO_LIST, ArrayList(infoList))
                putExtra(BUNDLE_POSITION, position)
            })
        }
    }
}