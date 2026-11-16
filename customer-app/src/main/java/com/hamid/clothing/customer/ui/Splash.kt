package com.hamid.clothing.customer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamid.clothing.customer.R

@Composable
fun MomoSplash(title: String, subtitle: String) {
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
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, color = Color(0xFFFFB38A), style = MaterialTheme.typography.bodyMedium, fontSize = 15.sp)
        }
    }
}
