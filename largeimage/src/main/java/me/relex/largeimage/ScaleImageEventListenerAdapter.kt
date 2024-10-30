package me.relex.largeimage

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.lang.Exception

internal open class ScaleImageEventListenerAdapter :
    SubsamplingScaleImageView.OnImageEventListener {
    override fun onReady() = Unit

    override fun onImageLoaded() = Unit

    override fun onPreviewLoadError(e: Exception?) = Unit

    override fun onImageLoadError(e: Exception?) = Unit

    override fun onTileLoadError(e: Exception?) = Unit

    override fun onPreviewReleased() = Unit
}
