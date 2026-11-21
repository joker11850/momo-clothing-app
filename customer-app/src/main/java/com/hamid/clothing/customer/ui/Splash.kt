package com.hamid.clothing.customer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hamid.clothing.customer.R
import kotlin.math.roundToInt

/** Single customer splash: complete artwork, correct Persian order, no crop. */
@Composable
fun MomoSplash() {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(3000, easing = FastOutSlowInEasing),
        label = "splash progress"
    )
    // LTR visual order is reversed so the RTL word reads correctly from right to left: پوشاک.
    val letters = listOf(
        Triple("ک", Color(0xFFFF6B72), 720),
        Triple("ا", Color(0xFF39D6B0), 540),
        Triple("ش", Color(0xFF9B3DFF), 360),
        Triple("و", Color(0xFFFFB83D), 180),
        Triple("پ", Color(0xFFFF4B98), 0)
    )
    val lowerBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF250526), Color(0xFF18031F), Color(0xFF0A0212))
    )
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0212))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(719f / 749f)
        ) {
            // Fit keeps the complete supplied artwork visible instead of zooming/cropping it.
            Image(
                painter = painterResource(R.drawable.momo_splash_background),
                contentDescription = "اسپلش MOMO",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier.align(Alignment.Center).offset(y = 68.dp),
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(lowerBackground)
                .drawBehind {
                    drawCircle(Color(0x44D20A94), radius = size.width * .48f, center = androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .18f))
                    drawCircle(Color(0x334C08A8), radius = size.width * .58f, center = androidx.compose.ui.geometry.Offset(size.width * .92f, size.height * .72f))
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier.padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
