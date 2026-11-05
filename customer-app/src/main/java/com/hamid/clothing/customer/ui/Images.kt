package com.hamid.clothing.customer.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ImageCache {
    private const val MAX_BYTES = 32 * 1024 * 1024
    val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }
}

private fun decode(file: File, maxPx: Int): ImageBitmap? {
    if (!file.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
}

/** Shows a photo from a file, decoding it off the main thread. [placeholder] is shown until it is ready or when missing. */
@Composable
fun LocalImage(
    file: File?,
    modifier: Modifier = Modifier,
    maxPx: Int = 600,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {}
) {
    if (file == null) {
        Box(modifier) { placeholder() }
        return
    }
    val key = file.path + "|" + file.lastModified() + "|" + maxPx
    val image by produceState<ImageBitmap?>(ImageCache.cache.get(key), key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { decode(file, maxPx)?.also { ImageCache.cache.put(key, it) } }
        }
    }
    val bitmap = image
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier) { placeholder() }
    }
}
