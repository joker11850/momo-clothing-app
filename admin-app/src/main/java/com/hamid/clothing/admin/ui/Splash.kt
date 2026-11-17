package com.hamid.clothing.admin.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamid.clothing.admin.R

@Composable
fun MomoSplash(title: String, subtitle: String) {
    val logoProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(720, easing = FastOutSlowInEasing),
        label = "logo entrance"
    )
    val transition = rememberInfiniteTransition(label = "loading dots")
    val dotProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot pulse"
    )
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF4A123B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .graphicsLayer {
                        alpha = logoProgress
                        scaleX = 0.82f + (0.18f * logoProgress)
                        scaleY = 0.82f + (0.18f * logoProgress)
                    }
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0xFFFFF8F3))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.brand_logo),
                    contentDescription = "لوگوی MOMO",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.graphicsLayer { alpha = logoProgress }
            )
            Text(
                subtitle,
                color = Color(0xFFFFB38A),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                modifier = Modifier.graphicsLayer { alpha = logoProgress }
            )
            LoadingDots(progress = dotProgress)
        }
    }
}

@Composable
private fun LoadingDots(progress: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val phase = ((progress + index * 0.22f) % 1f)
            val scale = 0.72f + (0.28f * (0.5f + 0.5f * kotlin.math.sin(phase * Math.PI * 2).toFloat()))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; alpha = 0.55f + 0.45f * scale }
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFFB38A))
            )
        }
    }
}
