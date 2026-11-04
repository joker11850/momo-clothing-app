package com.hamid.clothing.admin.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold = Color(0xFFD6B46A)
val Ivory = Color(0xFFF2EBDD)
val Muted = Color(0xFFA8A29A)
val Bg = Color(0xFF101010)
val SurfaceDark = Color(0xFF1B1B1B)
val SurfaceHigh = Color(0xFF262626)
val Danger = Color(0xFFE57373)
val Success = Color(0xFF7FC48A)
val OutlineDark = Color(0xFF3A3A3A)

@Composable
fun AdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Color(0xFF1A1405),
            secondary = Gold,
            background = Bg,
            onBackground = Ivory,
            surface = SurfaceDark,
            onSurface = Ivory,
            surfaceVariant = SurfaceHigh,
            onSurfaceVariant = Muted,
            error = Danger,
            outline = OutlineDark
        ),
        content = content
    )
}
