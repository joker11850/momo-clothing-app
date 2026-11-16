package com.hamid.clothing.customer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Cream = Color(0xFFFFF8F3)
val CardColor = Color(0xFFFFFFFF)
val Burgundy = Color(0xFFFF4652)
val BurgundyDeep = Color(0xFF4A123B)
val BurgundyBright = Color(0xFFFF6B73)
val Ink = Color(0xFF281D27)
val Brown = Color(0xFF8A4F42)
val Gold = Color(0xFFFFB38A)
val GoldInk = Color(0xFF4A123B)
val Rose = Color(0xFFFFB38A)
val RoseLight = Color(0xFFFFE8DE)
val Mint = Color(0xFFDCE6DE)
val MintDark = Color(0xFF2D5F48)
val Beige = Color(0xFFFFF0E8)
val Muted = Color(0xFF887B80)
val Danger = Color(0xFFC73645)
val BadgeRose = Color(0xFFFF4652)
val Line = Color(0xFFF0D5CF)

private val CustomerShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

private val CustomerTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.15).sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.05).sp),
        bodyLarge = bodyLarge.copy(lineHeight = 25.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 22.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp)
    )
}

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
            error = Danger,
            surfaceTint = Burgundy
        ),
        shapes = CustomerShapes,
        typography = CustomerTypography,
        content = content
    )
}
