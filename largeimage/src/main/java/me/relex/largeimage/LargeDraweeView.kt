package me.relex.largeimage

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.drawable.Animatable
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.facebook.common.util.UriUtil
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import kotlin.math.min

class LargeDraweeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private lateinit var _draweeView: PreviewDraweeView
    private lateinit var _scaleImageView: SubsamplingScaleImageView
    private var _loadingView: View? = null
    private var _loadingViewProvider: LoadingViewProvider? = null
    private var _currentTargetView: View? = null

    private var _imageLoader = FrescoImageLoader()
    private var _imageRequestListener: ImageRequestListener? = null
    private var _scaleValueHook: ScaleValueHook? = null
    private var _scaleImageOnStateChangedListener: SubsamplingScaleImageView.OnStateChangedListener? = null

    private var _longImageAnimation = false
    private var _longImageRatio = LONG_IMAGE_RATIO
    private var _longImageMinWidth = LONG_IMAGE_MIN_WIDTH
    private val _pullDownGestureHelper: PullDownGestureHelper

    private var _animationDuration = DEFAULT_ANIMATION_DURATION_MS
    private var _enableInternalLoading = false

    init {
        _pullDownGestureHelper = PullDownGestureHelper(context, DraweeGestureListener())
        configAttribute(context, attrs, defStyleAttr)
        initViews(context)
    }

    private fun configAttribute(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        var showLoading = true
        var enablePullDownGesture = false
        var loadingViewProviderName = DefaultLoadingViewProvider::class.java.getName()

        context.withStyledAttributes(attrs, R.styleable.LargeDraweeView, defStyleAttr, 0) {
            showLoading = getBoolean(R.styleable.LargeDraweeView_show_loading, true)
            loadingViewProviderName = getString(R.styleable.LargeDraweeView_loading_view_provider)
                ?: DefaultLoadingViewProvider::class.java.getName()
            enablePullDownGesture = getBoolean(
                R.styleable.LargeDraweeView_enable_pull_down_gesture, false
            )
            _animationDuration = getInt(
                R.styleable.LargeDraweeView_exit_duration, DEFAULT_ANIMATION_DURATION_MS
            )
            _longImageAnimation = getBoolean(
                R.styleable.LargeDraweeView_long_image_animation, false
            )
            _longImageRatio = getFloat(
                R.styleable.LargeDraweeView_long_image_ratio, LONG_IMAGE_RATIO
            )
            _longImageMinWidth = getInt(
                R.styleable.LargeDraweeView_long_image_min_width, LONG_IMAGE_MIN_WIDTH
            )
        }


        _loadingViewProvider = parseProvider(context, loadingViewProviderName)
        setInternalLoadingEnable(showLoading)
        setPullDownGestureEnable(enablePullDownGesture)
        setPullDownExitDuration(_animationDuration)
    }

    private fun initViews(context: Context) {
        // Bottom Layer , SubsamplingScaleImageView
        _scaleImageView = SubsamplingScaleImageView(context).apply {
            setMinimumTileDpi(160)
            setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
            setDoubleTapZoomDuration(200)
            setOnClickListener { this@LargeDraweeView.performClick() }
            setOnLongClickListener {
                this@LargeDraweeView.performLongClick()
                return@setOnLongClickListener true
            }
        }
        addView(_scaleImageView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // Middle Layer , Fresco DraweeView
        _draweeView = PreviewDraweeView(context).apply {
            hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
            setOnClickListener { this@LargeDraweeView.performClick() }
            setOnLongClickListener {
                this@LargeDraweeView.performLongClick()
                return@setOnLongClickListener true
            }
        }
        addView(_draweeView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // Top Loading , Loading
        val loadingViewProvider = _loadingViewProvider
        if (_enableInternalLoading && loadingViewProvider != null) {
            val loadingView = loadingViewProvider.createLoadingView(context, this).apply {
                isVisible = false
            }
            addView(loadingView)
            _loadingView = loadingView
        }
    }

    fun setInternalLoadingEnable(enable: Boolean) {
        _enableInternalLoading = enable
        if (!enable) {
            _loadingView?.isVisible = false
        }
    }

    fun setPullDownGestureEnable(enable: Boolean) {
        _pullDownGestureHelper.enablePullDown = enable
    }

    fun setPullDownListener(listener: PullDownListener) {
        _pullDownGestureHelper.listener = listener
    }

    fun setPullDownExitDuration(duration: Int) {
        _pullDownGestureHelper.resetDuration = duration
    }

    fun setImageRequestListener(imageRequestListener: ImageRequestListener?) {
        _imageRequestListener = imageRequestListener
    }

    fun setScaleValueHook(scaleValueHook: ScaleValueHook?) {
        _scaleValueHook = scaleValueHook
    }

    fun setScaleImageOnStateChangedListener(scaleImageOnStateChangedListener: SubsamplingScaleImageView.OnStateChangedListener?) {
        _scaleImageOnStateChangedListener = scaleImageOnStateChangedListener
    }

    fun load(info: LargeImageInfo) {
        val lowImageRequest = if (info.lowImageUri !== Uri.EMPTY) ImageRequest.fromUri(info.lowImageUri) else null
        load(ImageRequestBuilder.newBuilderWithSource(info.imageUri).build(), lowImageRequest)
    }

    fun load(imageRequest: ImageRequest, lowImageRequest: ImageRequest? = null) {
        _currentTargetView = null
        fetchLowImageRequest(lowImageRequest)
        fetchImageRequest(imageRequest)
    }

    fun reset() {
        if (_scaleImageView.isVisible) {
            resetScaleImageScaleAndCenter()
        }
    }

    private fun fetchLowImageRequest(imageRequest: ImageRequest?) {
        if (imageRequest == null) {
            _draweeView.setImageURI(Uri.EMPTY)
            _draweeView.isVisible = false
        } else {
            _draweeView.setImageRequest(imageRequest)
            _draweeView.isVisible = true
            _currentTargetView = _draweeView
        }
    }

    private fun fetchImageRequest(imageRequest: ImageRequest) {
        _imageLoader.cancel()

        val loadingView = _loadingView
        val loadingViewProvider = _loadingViewProvider

        if (_enableInternalLoading && loadingView != null && loadingViewProvider != null) {
            loadingView.isVisible = true
            loadingViewProvider.progress(0)
        }
        _scaleImageView.isVisible = false
        _scaleImageView.setOnImageEventListener(null)
        _imageLoader.loadImage(context, imageRequest, InternalLoaderCallback())
    }

    private inner class InternalLoaderCallback : LoaderCallback {
        override fun onLoaderStart() {
            _imageRequestListener?.onRequestStart()
        }

        override fun onLoaderProgress(progress: Int) {
            _imageRequestListener?.onRequestProgress(progress)
            if (_enableInternalLoading) {
                _loadingViewProvider?.progress(progress)
            }
        }

        override fun onLoaderSuccess(uri: Uri, imageFormat: ImageFormat) {
            loadImageUri(uri, imageFormat)
            _imageRequestListener?.onRequestSuccess(uri, imageFormat)
        }

        override fun onLoaderFailure(error: Throwable?) {
            error?.printStackTrace()
            _imageRequestListener?.onRequestFailure(error)
        }

        override fun onLoaderFinish() {
        }

        private fun loadImageUri(uri: Uri, imageFormat: ImageFormat) {
            if (imageFormat === DefaultImageFormats.GIF || imageFormat === DefaultImageFormats.WEBP_ANIMATED) {
                val controller = Fresco.newDraweeControllerBuilder().setUri(uri)
                    .setOldController(_draweeView.controller).setAutoPlayAnimations(true)
                    .setControllerListener(object : BaseControllerListener<ImageInfo?>() {
                        override fun onFinalImageSet(
                            id: String?, imageInfo: ImageInfo?, animatable: Animatable?
                        ) {
                            super.onFinalImageSet(id, imageInfo, animatable)
                            _loadingView?.isVisible = false
                            _imageRequestListener?.onImageLoaded()
                            animatable?.start()
                        }
                    }).build()
                _draweeView.isVisible = true
                _draweeView.setController(controller)
                _scaleImageView.isVisible = false
                _currentTargetView = _draweeView
            } else {
                _scaleImageView.isVisible = true
                _scaleImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF)
                _scaleImageView.setOnImageEventListener(InternalScaleEventListener())
                _scaleImageView.setOnStateChangedListener(_scaleImageOnStateChangedListener)

                var imageSource = if (UriUtil.isLocalResourceUri(uri)) {
                    ImageSource.resource(InternalUtil.getResourceIdFromUri(uri))
                } else if (UriUtil.isLocalAssetUri(uri)) {
                    ImageSource.asset(InternalUtil.getAssetNameFromUri(uri))
                } else {
                    ImageSource.uri(uri)
                }
                _scaleImageView.setImage(imageSource)
            }
        }
    }

    private var fitCenterScale = 0f
    private var fitCenterPoint: PointF? = null

    private var longScale = 0f
    private var longMaxScale = 0f
    private var longCenterPoint: PointF? = null

    private fun resetScaleImageScaleAndCenter() {
        _scaleImageView.minScale = fitCenterScale
        val animationBuilder = _scaleImageView.animateScaleAndCenter(fitCenterScale, fitCenterPoint)
        if (animationBuilder != null) {
            animationBuilder.withInterruptible(false).withDuration(_animationDuration.toLong())
                .start()
        } else {
            _scaleImageView.resetScaleAndCenter()
        }
    }

    private fun animateLongImageScale() {
        if (longScale == 0f || longMaxScale == 0f || longCenterPoint == null) {
            return
        }

        val animationBuilder = _scaleImageView.animateScaleAndCenter(longScale, longCenterPoint)
        if (animationBuilder != null && _longImageAnimation) {
            animationBuilder.withInterruptible(false).withDuration(_animationDuration.toLong())
                .withOnAnimationEventListener(object :
                    SubsamplingScaleImageView.DefaultOnAnimationEventListener() {
                    override fun onComplete() {
                        _scaleImageView.minScale = longScale
                        _scaleImageView.maxScale = longMaxScale
                        _scaleImageView.setDoubleTapZoomScale(longMaxScale)
                    }
                }).start()
        } else {
            _scaleImageView.minScale = longScale
            _scaleImageView.maxScale = longMaxScale
            _scaleImageView.setDoubleTapZoomScale(longMaxScale)
            _scaleImageView.setScaleAndCenter(longScale, longCenterPoint)
        }
    }

    private inner class InternalScaleEventListener : ScaleImageEventListenerAdapter() {
        override fun onReady() {
            val viewWidth = _scaleImageView.width
            val viewHeight = _scaleImageView.height
            val orientation = _scaleImageView.appliedOrientation

            var imageWidth: Int
            var imageHeight: Int
            if (orientation == SubsamplingScaleImageView.ORIENTATION_90 || orientation == SubsamplingScaleImageView.ORIENTATION_270) {
                imageWidth = _scaleImageView.sHeight
                imageHeight = _scaleImageView.sWidth
            } else {
                imageWidth = _scaleImageView.sWidth
                imageHeight = _scaleImageView.sHeight
            }

            if (imageWidth == 0 || imageHeight == 0 || viewWidth == 0 || viewHeight == 0) {
                return
            }

            val scaleValueHook = _scaleValueHook

            if (scaleValueHook != null) {
                fitCenterScale = scaleValueHook.getMinScale()
                fitCenterPoint = scaleValueHook.getCenter()
                scaleValueHook.initializeValue(imageWidth, imageHeight, viewWidth, viewHeight)

                _scaleImageView.minScale = scaleValueHook.getMinScale()
                _scaleImageView.maxScale = scaleValueHook.getMaxScale()
                _scaleImageView.setScaleAndCenter(
                    scaleValueHook.getMinScale(), scaleValueHook.getCenter()
                )
                _scaleImageView.setDoubleTapZoomScale(scaleValueHook.getMaxScale())
                return
            }

            val widthScale = viewWidth.toFloat() / imageWidth
            val heightScale = viewHeight.toFloat() / imageHeight
            // Fit_center
            var minScale: Float
            var maxScale: Float

            minScale = min(widthScale, heightScale)
            maxScale = minScale * 2f

            fitCenterScale = minScale
            fitCenterPoint = PointF(imageWidth / 2f, imageHeight / 2f)

            _scaleImageView.minScale = fitCenterScale
            _scaleImageView.maxScale = maxScale
            _scaleImageView.setScaleAndCenter(fitCenterScale, fitCenterPoint)
            _scaleImageView.setDoubleTapZoomScale(maxScale)

            if (imageHeight.toFloat() / imageWidth > _longImageRatio && imageWidth > _longImageMinWidth) {
                val defaultScale = if (imageWidth <= imageHeight) {
                    viewWidth.toFloat() / imageWidth
                } else {
                    viewHeight.toFloat() / imageHeight
                }
                longScale = defaultScale
                longMaxScale = defaultScale * 2f
                longCenterPoint = PointF(imageWidth / 2f, 0f)
                _scaleImageView.maxScale = longMaxScale
            } else {
                longScale = 0f
                longMaxScale = 0f
                longCenterPoint = null
            }
        }

        override fun onImageLoaded() {
            _draweeView.isVisible = false
            _loadingView?.isVisible = false
            _imageRequestListener?.onImageLoaded()
            animateLongImageScale()
            _currentTargetView = _scaleImageView
        }
    }

    override fun onDetachedFromWindow() {
        _imageLoader.cancel()
        super.onDetachedFromWindow()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (_pullDownGestureHelper.onInterceptTouchEvent(ev, _currentTargetView)) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (_pullDownGestureHelper.onTouchEvent(ev, _currentTargetView)) {
            return true
        }
        return super.onTouchEvent(ev)
    }

    private inner class DraweeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (_draweeView.isVisible) {
                performClick()
                return true
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            if (_draweeView.isVisible) {
                performLongClick()
            }
        }
    }

    interface ScaleValueHook {
        fun initializeValue(imageWidth: Int, imageHeight: Int, viewWidth: Int, viewHeight: Int)

        fun getMinScale(): Float

        fun getMaxScale(): Float

        fun getCenter(): PointF
    }

    interface ImageRequestListener {
        fun onRequestStart()

        fun onRequestProgress(progress: Int)

        fun onRequestFailure(error: Throwable?)

        fun onRequestSuccess(uri: Uri, imageFormat: ImageFormat)

        fun onImageLoaded()
    }

    companion object {
        var DEFAULT_ANIMATION_DURATION_MS = 300

        private const val LONG_IMAGE_RATIO = 2.5f
        private const val LONG_IMAGE_MIN_WIDTH = 400

        private fun parseProvider(context: Context, name: String?): LoadingViewProvider {
            if (name.isNullOrEmpty()) {
                return DefaultLoadingViewProvider()
            }
            var className = if (name.startsWith(".")) {
                context.packageName + name
            } else {
                name
            }
            val providerInstance = runCatching {
                val providerClass = context.classLoader.loadClass(className)
                providerClass.getDeclaredConstructor().newInstance() as? LoadingViewProvider
            }.onFailure { it.printStackTrace() }.getOrNull()

            return if (providerInstance != null) {
                providerInstance
            } else {
                DefaultLoadingViewProvider()
            }
        }
    }
}
