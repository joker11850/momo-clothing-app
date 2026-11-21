package com.hamid.clothing.customer.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.viewinterop.AndroidView
import com.hamid.clothing.customer.R

/** Plays the supplied MOMO artwork exactly as delivered, with no Compose overlays or resizing crop. */
@Composable
fun MomoSplash() {
    AndroidView(
        modifier = Modifier.fillMaxSize().background(ComposeColor(0xFF0A0212)),
        factory = { context -> MomoGifView(context) }
    )
}

@Suppress("DEPRECATION")
private class MomoGifView(context: Context) : View(context) {
    private val movie: Movie? = context.resources
        .openRawResource(R.drawable.momo_splash_exact)
        .use { Movie.decodeStream(it) }
    private val startedAt = System.currentTimeMillis()

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gif = movie ?: return
        canvas.drawColor(Color.rgb(10, 2, 18))
        val duration = (gif.duration().takeIf { it > 0 } ?: 3000).toLong()
        val elapsed = (System.currentTimeMillis() - startedAt).toInt() % duration.toInt()
        gif.setTime(elapsed)
        val scale = minOf(width.toFloat() / gif.width().toFloat(), height.toFloat() / gif.height().toFloat())
        val drawWidth = gif.width() * scale
        val drawHeight = gif.height() * scale
        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        gif.draw(canvas, 0f, 0f)
        canvas.restore()
        postInvalidateOnAnimation()
    }
}
