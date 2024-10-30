package me.relex.largeimage

import android.graphics.Rect
import android.net.Uri
import android.os.Parcelable
import android.view.View
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize

@Parcelize
data class LargeImageInfo(
    var imageUri: Uri = Uri.EMPTY,
    var lowImageUri: Uri = Uri.EMPTY,
    var sharedTransitionRect: Rect = Rect(),
    var imageSize: Rect = Rect()
) : Parcelable {

    class Builder {
        private val info = LargeImageInfo()

        fun imageUri(imageUri: Uri): Builder {
            info.imageUri = imageUri
            return this
        }

        fun imageUrl(imageUrl: String?): Builder {
            info.imageUri = imageUrl?.toUri() ?: Uri.EMPTY
            return this
        }

        fun lowImageUri(lowImageUri: Uri): Builder {
            info.lowImageUri = lowImageUri
            return this
        }

        fun lowImageUrl(lowImageUrl: String?): Builder {
            info.lowImageUri = lowImageUrl?.toUri() ?: Uri.EMPTY
            return this
        }

        fun sharedTransitionRect(rect: Rect): Builder {
            info.sharedTransitionRect = rect
            return this
        }

        fun imageSize(width: Int, height: Int): Builder {
            info.imageSize = Rect(0, 0, width, height)
            return this
        }

        fun sharedTransitionView(view: View): Builder {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            info.sharedTransitionRect = Rect(
                location[0], location[1], location[0] + view.width, location[1] + view.height
            )
            return this
        }

        fun build(): LargeImageInfo = info
    }
}
