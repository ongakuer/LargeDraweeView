package me.relex.largeimage

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.common.SourceUriType
import com.facebook.imagepipeline.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.File
import java.lang.IllegalStateException

class FrescoImageLoader {

    private var _currentDataSource: DataSource<*>? = null
    private var _tempFileDirectory: File? = null

    fun cancel() {
        _currentDataSource?.close()
    }

    fun loadImage(context: Context, imageRequest: ImageRequest, loaderCallback: LoaderCallback) {
        cancel()

        val uriType = imageRequest.sourceUriType
        val dataSource = when (uriType) {
            SourceUriType.SOURCE_TYPE_LOCAL_FILE, //
            SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE, //
            SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE -> {
                loaderCallback.onLoaderStart()
                InternalUtil.fetchLocalFileInfo(imageRequest.sourceUri.toFile(), ioExecutor).apply {
                    subscribe(
                        ImageFileSubscriber(loaderCallback),
                        UiThreadImmediateExecutorService.getInstance()
                    )
                }
            }

            SourceUriType.SOURCE_TYPE_LOCAL_CONTENT,  //
            SourceUriType.SOURCE_TYPE_QUALIFIED_RESOURCE -> {
                loaderCallback.onLoaderStart()
                InternalUtil.fetchLocalUriInfo(context, imageRequest.sourceUri, ioExecutor).apply {
                    subscribe(
                        ImageUriSubscriber(loaderCallback),
                        UiThreadImmediateExecutorService.getInstance()
                    )
                }
            }

            SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE -> {
                loaderCallback.onLoaderStart()
                InternalUtil.fetchLocalResourceInfo(imageRequest.sourceUri).apply {
                    subscribe(
                        ImageUriSubscriber(loaderCallback),
                        UiThreadImmediateExecutorService.getInstance()
                    )
                }
            }

            SourceUriType.SOURCE_TYPE_LOCAL_ASSET -> {
                loaderCallback.onLoaderStart()
                InternalUtil.fetchLocalAssetInfo(context, imageRequest.sourceUri, ioExecutor)
                    .apply {
                        subscribe(
                            ImageUriSubscriber(loaderCallback),
                            UiThreadImmediateExecutorService.getInstance()
                        )
                    }
            }

            SourceUriType.SOURCE_TYPE_NETWORK -> {
                loaderCallback.onLoaderStart()
                var cacheDirectory = _tempFileDirectory
                if (cacheDirectory == null || !cacheDirectory.exists()) {
                    cacheDirectory = InternalUtil.getDefaultTempDirectory(context)
                    _tempFileDirectory = cacheDirectory
                }
                val cacheKey = InternalUtil.getCacheKey(imageRequest)
                val resourceId = InternalUtil.getResourceId(cacheKey)
                val localCacheFile = InternalUtil.getLocalCacheFile(cacheKey, imageRequest)

                if (localCacheFile != null && localCacheFile.exists()) {
                    InternalUtil.fetchNetworkCacheFileInfo(
                        localCacheFile, cacheDirectory, resourceId, ioExecutor
                    )
                } else {
                    InternalUtil.fetchNetworkFileInfo(
                        imageRequest, cacheDirectory, resourceId, ioExecutor
                    )
                }.apply {
                    subscribe(
                        ImageFileSubscriber(loaderCallback),
                        UiThreadImmediateExecutorService.getInstance()
                    )
                }
            }

            else -> {
                loaderCallback.onLoaderFailure(
                    IllegalStateException(
                        "Unsupported uri scheme : " + imageRequest.sourceUri
                    )
                )
                loaderCallback.onLoaderFinish()
                return
            }
        }
        _currentDataSource = dataSource
    }

    private abstract class BaseImageInfoSubscriber<T>(val loaderCallback: LoaderCallback) :
        BaseDataSubscriber<Pair<T, ImageFormat>>() {
        var currentProgress: Int = 0

        override fun onProgressUpdate(dataSource: DataSource<Pair<T, ImageFormat>>) {
            val progress = (dataSource.progress * 100).toInt()
            if (currentProgress != progress) {
                currentProgress = progress
                loaderCallback.onLoaderProgress(progress)
            }
        }

        override fun onFailureImpl(dataSource: DataSource<Pair<T, ImageFormat>>) {
            loaderCallback.onLoaderFailure(dataSource.failureCause)
            loaderCallback.onLoaderFinish()
        }
    }

    private class ImageFileSubscriber(loaderCallback: LoaderCallback) :
        BaseImageInfoSubscriber<File>(loaderCallback) {
        override fun onNewResultImpl(dataSource: DataSource<Pair<File, ImageFormat>>) {
            if (!dataSource.isFinished) return
            val result = dataSource.getResult() ?: return
            loaderCallback.onLoaderSuccess(Uri.fromFile(result.first), result.second)
            loaderCallback.onLoaderFinish()
        }
    }

    private class ImageUriSubscriber(loaderCallback: LoaderCallback) :
        BaseImageInfoSubscriber<Uri>(loaderCallback) {
        override fun onNewResultImpl(dataSource: DataSource<Pair<Uri, ImageFormat>>) {
            if (!dataSource.isFinished) return
            val result = dataSource.getResult() ?: return
            loaderCallback.onLoaderSuccess(result.first, result.second)
            loaderCallback.onLoaderFinish()
        }
    }

    companion object {
        private val ioExecutor by lazy { Dispatchers.IO.asExecutor() }
    }
}


