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

val Cream = Color(0xFFF5F0E9)
val CardColor = Color(0xFFFFFCF8)
val Burgundy = Color(0xFF6E1835)
val Ink = Color(0xFF241C1B)
val Brown = Color(0xFF8B6849)
val Gold = Color(0xFFC89A42)
val GoldInk = Color(0xFF33230D)
val Rose = Color(0xFFE2B2B5)
val RoseLight = Color(0xFFF0DDE0)
val Mint = Color(0xFFDCE6DE)
val MintDark = Color(0xFF2D5F48)
val Beige = Color(0xFFEDE0C8)
val Muted = Color(0xFF786D69)
val Danger = Color(0xFFAE302B)
val BadgeRose = Color(0xFF9D3F59)
val Line = Color(0xFFE0D5CC)

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
            surfaceTint = Gold
        ),
        shapes = CustomerShapes,
        typography = CustomerTypography,
        content = content
    )
}
