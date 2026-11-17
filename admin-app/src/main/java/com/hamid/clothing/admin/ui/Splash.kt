package com.hamid.clothing.admin.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamid.clothing.admin.R

@Composable
fun MomoSplash(title: String, subtitle: String) {
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "luxury splash reveal"
    )
    val background = Brush.radialGradient(
        colors = listOf(Color(0xFF6A214F), Color(0xFF3B1833), Color(0xFF1E101D)),
        radius = 920f
    )
    Box(
        modifier = Modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer { alpha = reveal; scaleX = 0.94f + 0.06f * reveal; scaleY = 0.94f + 0.06f * reveal }
        ) {
            Text(
                "MOMO  •  COLLECTION",
                color = Color(0xFFFFB38A),
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .size(174.dp)
                    .shadow(26.dp, RoundedCornerShape(42.dp), ambientColor = Color(0x66FF4652), spotColor = Color(0x99FFB38A))
                    .clip(RoundedCornerShape(42.dp))
                    .background(Color(0xFFFFF8F3))
                    .border(1.dp, Color(0xFFFFB38A).copy(alpha = 0.8f), RoundedCornerShape(42.dp))
                    .padding(17.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.brand_logo),
                    contentDescription = "لوگوی MOMO",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(subtitle, color = Color(0xFFFFD8C5), fontSize = 14.sp, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .width(132.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = reveal; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                        .background(Color(0xFFFFB38A))
                )
            }
            Spacer(Modifier.height(13.dp))
            Text("استایل خودت را بساز", color = Color.White.copy(alpha = 0.58f), fontSize = 11.sp)
        }
    }
}
