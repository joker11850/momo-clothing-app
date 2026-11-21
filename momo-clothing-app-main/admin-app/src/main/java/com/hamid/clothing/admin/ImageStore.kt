package com.hamid.clothing.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.hamid.clothing.core.CatalogPackage
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Product photos kept inside the app. Files are named p_<random>.jpg and referenced from Product.imageUris. */
class ImageStore(context: Context) {
    private val dir = File(context.filesDir, "product-images").also { it.mkdirs() }
    private val resolver = context.contentResolver

    /** Only safe names are ever turned into files. */
    fun file(name: String): File = File(dir, if (CatalogPackage.isSafeImageName(name)) name else "invalid")

    fun read(name: String): ByteArray? {
        val f = file(name)
        return if (f.isFile) f.readBytes() else null
    }

    fun delete(name: String) {
        if (CatalogPackage.isSafeImageName(name)) File(dir, name).delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /** Empty folder next to the photos where a restore can unpack before anything is replaced. */
    fun newStaging(): File {
        val staging = File(dir.parentFile, "product-images-new")
        staging.deleteRecursively()
        staging.mkdirs()
        return staging
    }

    /** Replaces all photos with the files of [staging] (then removes it). */
    fun replaceWith(staging: File) {
        deleteAll()
        staging.listFiles()?.forEach { f ->
            if (CatalogPackage.isSafeImageName(f.name) && !f.renameTo(File(dir, f.name))) {
                f.copyTo(File(dir, f.name), overwrite = true)
            }
        }
        staging.deleteRecursively()
    }

    /**
     * Reads a photo chosen by the user, fixes its rotation, scales it to at most [maxSide] pixels and saves
     * it as a JPEG. Returns the new file name, or null when the photo could not be read. Call off the main thread.
     */
    fun import(uri: Uri, maxSide: Int = 1024, quality: Int = 82): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

            val longest = max(bitmap.width, bitmap.height)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                bitmap = Bitmap.createScaledBitmap(
                    bitmap, (bitmap.width * scale).roundToInt().coerceAtLeast(1), (bitmap.height * scale).roundToInt().coerceAtLeast(1), true
                )
            }
            val rotation = readRotation(uri)
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            if (bitmap.hasAlpha()) {
                // JPEG has no transparency: put the picture on white
                val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(flat)
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                bitmap = flat
            }

            val name = "p_" + UUID.randomUUID().toString().replace("-", "").take(16) + ".jpg"
            File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            name
        } catch (e: Exception) {
            null
        }
    }

    private fun readRotation(uri: Uri): Int {
        if (Build.VERSION.SDK_INT < 24) return 0
        return try {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
