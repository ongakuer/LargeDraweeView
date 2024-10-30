package me.relex.largeimage

import android.net.Uri
import com.facebook.imageformat.ImageFormat

interface LoaderCallback {
    fun onLoaderStart() = Unit

    fun onLoaderProgress(progress: Int) = Unit

    fun onLoaderSuccess(uri: Uri, imageFormat: ImageFormat) = Unit

    fun onLoaderFailure(error: Throwable?) = Unit

    fun onLoaderFinish() = Unit
}
