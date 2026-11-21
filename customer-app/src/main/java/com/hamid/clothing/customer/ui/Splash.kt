package com.hamid.clothing.customer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hamid.clothing.customer.R
import kotlin.math.roundToInt

/** Single full-screen customer splash. No second/legacy splash is used. */
@Composable
fun MomoSplash() {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(3000, easing = FastOutSlowInEasing),
        label = "splash progress"
    )
    val letters = listOf(
        Triple("پ", Color(0xFFFF4B98), 0),
        Triple("و", Color(0xFFFFB83D), 180),
        Triple("ش", Color(0xFF9B3DFF), 360),
        Triple("ا", Color(0xFF39D6B0), 540),
        Triple("ک", Color(0xFFFF6B72), 720)
    )
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0212))) {
        Image(
            painter = painterResource(R.drawable.momo_splash_background),
            contentDescription = "اسپلش MOMO",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // The letters fall over the word position in the full-screen artwork.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.align(Alignment.Center).offset(y = 138.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                letters.forEach { (letter, color, delay) ->
                    val settled by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(1050, delayMillis = delay, easing = FastOutSlowInEasing),
                        label = "fall $letter"
                    )
                    Text(
                        text = letter,
                        color = color,
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier
                            .offset { IntOffset(0, (-260f * (1f - settled)).roundToInt()) }
                            .graphicsLayer {
                                alpha = settled
                                rotationZ = (1f - settled) * if (delay % 2 == 0) -8f else 8f
                                shadowElevation = 14f
                            }
                    )
                }
            }
        }
        // Bottom overlay keeps the loader readable while the artwork fills the entire screen.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 42.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "در حال آماده‌سازی فروشگاه…",
                    color = Color(0xFFFFDBB5),
                    style = MaterialTheme.typography.bodyMedium
                )
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(260.dp)
                        .height(7.dp)
                        .background(Color(0xFF42104E), RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(7.dp)
                            .background(Color(0xFFF51EAE), RoundedCornerShape(8.dp))
                    )
                }
                Text(
                    faInt((progress * 100f).roundToInt()) + "٪",
                    color = Color(0xFFFFD070),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 7.dp)
                )
            }
        }
    }
}
