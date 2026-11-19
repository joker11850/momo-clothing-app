package com.hamid.clothing.customer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hamid.clothing.customer.R
import kotlin.math.roundToInt

/** Single, soft animated splash for the customer app. The approved MOMO artwork is preserved. */
@Composable
fun MomoSplash(title: String = "", subtitle: String = "") {
    val entrance by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1150, easing = FastOutSlowInEasing),
        label = "momo entrance"
    )
    val motion = rememberInfiniteTransition(label = "momo logo motion")
    val breathe by motion.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "momo breathing"
    )
    val drift by motion.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "momo drift"
    )
    val shine by motion.animateFloat(
        initialValue = -1.3f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "momo shine"
    )
    val background = Brush.radialGradient(
        colors = listOf(Color(0xFF34152E), Color(0xFF150A16), Color.Black),
        radius = 980f
    )
    Box(Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(310.dp)
                .offset { IntOffset(0, drift.roundToInt()) }
                .graphicsLayer {
                    alpha = entrance
                    scaleX = breathe * (0.88f + entrance * 0.12f)
                    scaleY = breathe * (0.88f + entrance * 0.12f)
                }
                .shadow(30.dp, RoundedCornerShape(56.dp), ambientColor = Color(0x66FF4652), spotColor = Color(0x99FFB38A))
                .clip(RoundedCornerShape(56.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.brand_logo),
                contentDescription = "لوگوی MOMO",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            // A narrow, low-contrast light sweep adds motion without changing the artwork.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = shine * 520f; rotationZ = 16f }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.42f)
                .height(3.dp)
                .offset(y = (-92).dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.13f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = entrance; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                    .background(Color(0xFFFFB38A))
            )
        }
    }
}
