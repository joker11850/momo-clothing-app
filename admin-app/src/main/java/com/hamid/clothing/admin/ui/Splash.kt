package com.hamid.clothing.admin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.hamid.clothing.admin.R

@Composable
fun MomoSplash(title: String, subtitle: String) {
    Image(
        painter = painterResource(R.drawable.momo_splash),
        contentDescription = "اسپلش پوشاک MOMO",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
