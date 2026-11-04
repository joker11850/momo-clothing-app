package com.hamid.clothing.customer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFF1EAE6)
val CardColor = Color(0xFFFAF7F5)
val Burgundy = Color(0xFF7B1E3C)
val Ink = Color(0xFF2B2430)
val Brown = Color(0xFF9A7650)
val Gold = Color(0xFFDDB45B)
val GoldInk = Color(0xFF3A2A10)
val Rose = Color(0xFFE5A9B3)
val RoseLight = Color(0xFFEBD3D7)
val Mint = Color(0xFFDCE7DE)
val MintDark = Color(0xFF2F6B4F)
val Beige = Color(0xFFF0E2C8)
val Muted = Color(0xFF7A7078)
val Danger = Color(0xFFB3261E)
val BadgeRose = Color(0xFFD96A79)
val Line = Color(0xFFD8CCC8)

@Composable
fun CustomerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Burgundy,
            onPrimary = Color.White,
            secondary = Gold,
            background = Cream,
            onBackground = Ink,
            surface = CardColor,
            onSurface = Ink,
            surfaceVariant = RoseLight,
            onSurfaceVariant = Muted,
            outline = Line,
            error = Danger
        ),
        content = content
    )
}
