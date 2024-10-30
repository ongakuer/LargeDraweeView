package me.relex.sample.largeimage

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updatePadding
import com.facebook.common.util.UriUtil
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import me.relex.largeimage.LargeImageInfo
import me.relex.sample.largeimage.databinding.ActivityHomeBinding
import me.relex.sample.largeimage.hero.LargeGalleryActivity
import me.relex.sample.largeimage.hero.LargePhotoActivity

class HomeActivity : AppCompatActivity(R.layout.activity_home) {

    private val binding by viewBinding(ActivityHomeBinding::bind)

    private val pickPhoto = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            val info = LargeImageInfo.Builder().imageUri(uri).build()
            binding.largeDraweeView.load(info)
            binding.drawerLayout.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initWindows()
        initDrawer()
        initSamples()
        initPhotoHeroAnimations()
        initGalleryHeroAnimations()

        binding.largeDraweeView.load(
            LargeImageInfo.Builder().imageUri(Uri.parse("asset:///photo/default.jpg")).build()
        )
    }

    private fun initWindows() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
            isAppearanceLightNavigationBars = true
            isAppearanceLightStatusBars = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root,
            OnApplyWindowInsetsListener { v, insets ->
                binding.root.updatePadding(
                    top = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()).top
                )
                binding.menuLayout.updatePadding(
                    bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                )
                return@OnApplyWindowInsetsListener insets
            })
    }

    private fun initDrawer() {
        binding.toolbarView.setNavigationOnClickListener {
            if (binding.drawerLayout.isOpen) {
                binding.drawerLayout.close()
            } else {
                binding.drawerLayout.open()
            }
        }
        binding.drawerLayout.openDrawer(GravityCompat.START, false)
    }

    private fun initSamples() {
        binding.webView.setOnClickListener {
            val info = LargeImageInfo.Builder().apply {
                lowImageUrl("https://picsum.photos/id/20/367/246")
                imageUrl("https://picsum.photos/id/20/3670/2462")
            }.build()
            binding.largeDraweeView.load(info)
            binding.drawerLayout.close()
        }

        binding.longView.setOnClickListener {
            val info = LargeImageInfo.Builder().apply {
                imageUri(Uri.parse("asset:///photo/long.png"))
            }.build()
            binding.largeDraweeView.load(info)
            binding.drawerLayout.close()
        }

        binding.animatedView.setOnClickListener {
            val info = LargeImageInfo.Builder().apply {
                imageUri(Uri.parse("asset:///animated/wukong.gif"))
            }.build()
            binding.largeDraweeView.load(info)
            binding.drawerLayout.close()
        }

        binding.contentUriView.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }

        binding.resourceView.setOnClickListener {
            val info = LargeImageInfo.Builder().apply {
                imageUri(UriUtil.getUriForResourceId(R.drawable.photo))
            }.build()
            binding.largeDraweeView.load(info)
            binding.drawerLayout.close()
        }

        binding.assetsView.setOnClickListener {
            val info = LargeImageInfo.Builder().apply {
                imageUri(Uri.parse("asset:///photo/mobile.jpg"))
            }.build()
            binding.largeDraweeView.load(info)
            binding.drawerLayout.close()
        }

        binding.cleanCacheView.setOnClickListener {
            Fresco.getImagePipeline().clearCaches()
            binding.drawerLayout.close()
        }
    }

    private fun initPhotoHeroAnimations() {
        val lowUrl = "https://picsum.photos/id/33/500/333"
        val heightUrl = "https://picsum.photos/id/33/5000/3333"
        binding.photoHeroView.setImageURI(lowUrl)
        binding.photoHeroView.setOnClickListener {
            val info = LargeImageInfo.Builder().apply {
                lowImageUrl(lowUrl)
                imageUrl(heightUrl)
                imageSize(5000, 3333)
                sharedTransitionView(it)
            }.build()
            LargePhotoActivity.startActivity(this@HomeActivity, info)
        }
    }

    private fun initGalleryHeroAnimations() {
        val imageDataList = listOf(
            ImageData(
                lowUrl = "https://picsum.photos/id/48/5000/3333",
                url = "https://picsum.photos/id/48/5000/3333",
                width = 5000,
                height = 3333
            ),
            ImageData(
                lowUrl = "https://picsum.photos/id/146/5000/3333",
                url = "https://picsum.photos/id/146/5000/3333",
                width = 5000,
                height = 3333
            ),
            ImageData(
                lowUrl = "https://picsum.photos/id/201/5000/3333",
                url = "https://picsum.photos/id/201/5000/3333",
                width = 5000,
                height = 3333
            ),
        )
        val imageViews = binding.galleryHeroLayout.children.filterIsInstance(SimpleDraweeView::class.java)
            .toList()

        imageViews.forEachIndexed { index, view ->
            view.setImageURI(imageDataList[index].lowUrl)
            view.setOnClickListener {
                val infoList = createLargeImageInfoList(imageDataList, imageViews)
                LargeGalleryActivity.startActivity(this@HomeActivity, infoList, index)
            }
        }
    }

    data class ImageData(val url: String, val lowUrl: String, val width: Int, val height: Int)

    private fun createLargeImageInfoList(
        imageDataList: List<ImageData>, views: List<View>
    ): List<LargeImageInfo> {
        return imageDataList.mapIndexed { index, imageData ->
            LargeImageInfo.Builder().apply {
                lowImageUrl(imageData.lowUrl)
                imageUrl(imageData.url)
                imageSize(imageData.width, imageData.height)
                val targetView = views.getOrNull(index)
                if (targetView != null) {
                    sharedTransitionView(targetView)
                }
            }.build()
        }
    }
}