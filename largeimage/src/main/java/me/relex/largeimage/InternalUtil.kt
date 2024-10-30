package me.relex.largeimage

import android.content.Context
import android.net.Uri
import com.facebook.binaryresource.FileBinaryResource
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.CacheKeyUtil
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferInputStream
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.datasource.SimpleDataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imageformat.ImageFormat
import com.facebook.imageformat.ImageFormatChecker
import com.facebook.imagepipeline.core.ImagePipelineFactory
import com.facebook.imagepipeline.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.concurrent.Executor

internal object InternalUtil {

    private const val NETWORK_CACHE_DIRECTORY_NAME = "large_image_cache"

    fun getResourceId(key: CacheKey): String = CacheKeyUtil.getFirstResourceId(key)

    fun getCacheKey(request: ImageRequest): CacheKey = Fresco.getImagePipeline().cacheKeyFactory.getEncodedCacheKey(
        request, null
    )

    fun getLocalCacheFile(cacheKey: CacheKey?, request: ImageRequest): File? {
        var cacheFile = request.getSourceFile()
        val mainFileCache = ImagePipelineFactory.getInstance().diskCachesStoreSupplier.get().mainFileCache
        val resource = mainFileCache.getResource(cacheKey)
        if (mainFileCache.hasKey(cacheKey) && resource is FileBinaryResource) {
            cacheFile = resource.file
        }
        return cacheFile
    }

    fun fetchLocalFileInfo(
        localFile: File, executor: Executor
    ): DataSource<Pair<File, ImageFormat>> {
        val dataSource = SimpleDataSource.create<Pair<File, ImageFormat>>()
        executor.execute {
            dataSource.result = Pair(
                localFile, ImageFormatChecker.getImageFormat(localFile.absolutePath)
            )
        }
        return dataSource
    }

    fun fetchLocalUriInfo(
        context: Context, uri: Uri, executor: Executor
    ): DataSource<Pair<Uri, ImageFormat>> {
        val dataSource = SimpleDataSource.create<Pair<Uri, ImageFormat>>()
        executor.execute {
            try {
                val imageFormat: ImageFormat? = context.contentResolver.openInputStream(uri)?.use {
                    ImageFormatChecker.getImageFormat(it)
                }
                checkNotNull(imageFormat)
                dataSource.result = Pair(uri, imageFormat)
            } catch (e: Exception) {
                dataSource.setFailure(e)
            }
        }
        return dataSource
    }

    fun fetchLocalResourceInfo(uri: Uri): DataSource<Pair<Uri, ImageFormat>> = SimpleDataSource.create<Pair<Uri, ImageFormat>>()
        .apply {
            result = Pair(uri, ImageFormat.UNKNOWN)
        }

    fun fetchLocalAssetInfo(
        context: Context, uri: Uri, executor: Executor
    ): DataSource<Pair<Uri, ImageFormat>> {
        val dataSource = SimpleDataSource.create<Pair<Uri, ImageFormat>>()
        executor.execute {
            try {
                val imageFormat = context.assets.open(getAssetNameFromUri(uri)).use {
                    ImageFormatChecker.getImageFormat(it)
                }
                dataSource.result = Pair(uri, imageFormat)
            } catch (e: Exception) {
                dataSource.setFailure(e)
            }
        }

        return dataSource
    }

    fun fetchNetworkCacheFileInfo(
        localFile: File, tempDirectory: File?, tempFileId: String?, executor: Executor
    ): DataSource<Pair<File, ImageFormat>> {
        val dataSource = SimpleDataSource.create<Pair<File, ImageFormat>>()
        executor.execute {
            // clean temp file
            if (!tempFileId.isNullOrEmpty() && tempDirectory != null && tempDirectory.exists()) {
                deleteFile(File(tempDirectory, tempFileId))
            }
            dataSource.result = Pair(
                localFile, ImageFormatChecker.getImageFormat(localFile.absolutePath)
            )
        }
        return dataSource
    }

    fun fetchNetworkFileInfo(
        imageRequest: ImageRequest, tempDirectory: File, tempFileId: String, executor: Executor
    ): DataSource<Pair<File, ImageFormat>> {
        val finalDataSource = SimpleDataSource.create<Pair<File, ImageFormat>>()
        val source = ImagePipelineFactory.getInstance().getImagePipeline()
            .fetchEncodedImage(imageRequest, null)
        source.subscribe(object : BaseDataSubscriber<CloseableReference<PooledByteBuffer>>() {
            override fun onNewResultImpl(dataSource: DataSource<CloseableReference<PooledByteBuffer>>) {
                if (!dataSource.isFinished) {
                    if (finalDataSource.isClosed) {
                        dataSource.close()
                    }
                    return
                }

                val buffer = dataSource.getResult()?.get() ?: return
                val tempFile = File(tempDirectory, tempFileId)
                try {
                    PooledByteBufferInputStream(buffer).use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    finalDataSource.progress = 1f
                    finalDataSource.result = Pair(
                        tempFile, ImageFormatChecker.getImageFormat(tempFile.absolutePath)
                    )
                } catch (e: Exception) {
                    deleteFile(tempFile)
                    finalDataSource.setFailure(e)
                }
            }

            override fun onFailureImpl(
                dataSource: DataSource<CloseableReference<PooledByteBuffer>>
            ) {
                val throwable = dataSource.failureCause
                if (throwable != null) {
                    throwable.printStackTrace()
                    finalDataSource.setFailure(throwable)
                }
            }

            override fun onProgressUpdate(dataSource: DataSource<CloseableReference<PooledByteBuffer>>) {
                if (!dataSource.isFinished) {
                    finalDataSource.progress = dataSource.progress * 0.98f
                }
            }
        }, executor)

        return finalDataSource
    }

    fun getAssetNameFromUri(uri: Uri): String {
        return uri.path!!.substring(1)
    }

    fun getResourceIdFromUri(uri: Uri): Int {
        return uri.path!!.substring(1).toInt()
    }

    fun getDefaultTempDirectory(context: Context): File {
        var appCacheDir = context.externalCacheDir
        if (appCacheDir == null) {
            appCacheDir = context.cacheDir
        }
        val tempDirectory = File(appCacheDir, NETWORK_CACHE_DIRECTORY_NAME)
        if (!tempDirectory.exists()) {
            tempDirectory.mkdirs()
            val noMediaFile = File(tempDirectory, ".nomedia")
            if (!noMediaFile.exists()) {
                runCatching { noMediaFile.createNewFile() }
            }
        }
        return tempDirectory
    }

    private fun deleteFile(file: File) = runCatching {
        if (file.exists()) {
            file.delete()
        }
    }
}


