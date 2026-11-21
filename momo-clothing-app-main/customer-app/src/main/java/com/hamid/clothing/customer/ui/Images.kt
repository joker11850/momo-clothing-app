package com.hamid.clothing.customer.ui

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
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

private val ImageDecodeDispatcher = Dispatchers.IO.limitedParallelism(2)

private object ImageCache {
    private const val MIN_BYTES = 12 * 1024 * 1024
    private const val DEFAULT_BYTES = 20 * 1024 * 1024
    private const val LARGE_BYTES = 36 * 1024 * 1024
    private var maxBytes = DEFAULT_BYTES
    private var cache = createCache(maxBytes)

    private fun createCache(limit: Int) = object : LruCache<String, ImageBitmap>(limit) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    @Synchronized fun configure(context: Context) {
        val memoryClassMb = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.memoryClass ?: 128
        val target = when {
            memoryClassMb <= 128 -> MIN_BYTES
            memoryClassMb >= 512 -> LARGE_BYTES
            else -> DEFAULT_BYTES
        }
        if (target != maxBytes) {
            maxBytes = target
            cache.evictAll()
            cache = createCache(maxBytes)
        }
    }

    @Synchronized fun get(key: String): ImageBitmap? = cache.get(key)

    @Synchronized fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }

    @Synchronized fun trim(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> cache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.trimToSize(maxBytes / 2)
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> cache.trimToSize(maxBytes / 3)
        }
    }
}

private var imageMemoryCallbacksRegistered = false

/** Registers one process-wide memory callback for the customer app image cache. */
fun installImageMemoryCallbacks(context: Context) {
    if (imageMemoryCallbacksRegistered) return
    synchronized(ImageCache) {
        if (imageMemoryCallbacksRegistered) return
        val appContext = context.applicationContext
        ImageCache.configure(appContext)
        appContext.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) = ImageCache.trim(level)
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() = ImageCache.trim(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        })
        imageMemoryCallbacksRegistered = true
    }
}
private fun decode(file: File, maxPx: Int): ImageBitmap? {
    if (!file.isFile) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        inScaled = false
    }
    return runCatching { BitmapFactory.decodeFile(file.path, options)?.asImageBitmap() }.getOrNull()
}

/** Shows a photo from a file, decoding it off the main thread. [placeholder] is shown until it is ready or when missing. */
@Composable
fun LocalImage(
    file: File?,
    modifier: Modifier = Modifier,
    maxPx: Int = 360,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {}
) {
    if (file == null) {
        Box(modifier) { placeholder() }
        return
    }
    val key = file.path + "|" + file.lastModified() + "|" + maxPx
    val image by produceState<ImageBitmap?>(ImageCache.get(key), key) {
        if (value == null) {
            value = withContext(ImageDecodeDispatcher) { decode(file, maxPx)?.also { ImageCache.put(key, it) } }
        }
    }
    val bitmap = image
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier) { placeholder() }
    }
}
