package com.hamid.clothing.admin.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.hamid.clothing.admin.ThemeMode
val Burgundy = Color(0xFF6E1835)
val BurgundyDeep = Color(0xFF461024)

/** Colors of one theme. Screens read them through the Gold, Muted, ... properties below. */
class Palette(
    val dark: Boolean,
    val gold: Color,
    val text: Color,
    val muted: Color,
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val danger: Color,
    val success: Color,
    val outline: Color,
    val errorBg: Color
)

val DarkPalette = Palette(
    dark = true,
    gold = Color(0xFFE0B866),
    text = Color(0xFFF2EBDD),
    muted = Color(0xFFA8A29A),
    bg = Color(0xFF0F0E0D),
    surface = Color(0xFF1A1816),
    surfaceHigh = Color(0xFF28231F),
    danger = Color(0xFFE57373),
    success = Color(0xFF7FC48A),
    outline = Color(0xFF453C33),
    errorBg = Color(0xFF3A1F1F)
)

val LightPalette = Palette(
    dark = false,
    gold = Color(0xFF9A6B16),
    text = Color(0xFF211A14),
    muted = Color(0xFF756A5E),
    bg = Color(0xFFF5F0E8),
    surface = Color(0xFFFFFCF7),
    surfaceHigh = Color(0xFFF0E6D7),
    danger = Color(0xFFC0392B),
    success = Color(0xFF2E7D4F),
    outline = Color(0xFFE0D4C2),
    errorBg = Color(0xFFFBE3E0)
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

val Gold: Color @Composable get() = LocalPalette.current.gold
val Ivory: Color @Composable get() = LocalPalette.current.text
val Muted: Color @Composable get() = LocalPalette.current.muted
val Bg: Color @Composable get() = LocalPalette.current.bg
val SurfaceDark: Color @Composable get() = LocalPalette.current.surface
val SurfaceHigh: Color @Composable get() = LocalPalette.current.surfaceHigh
val Danger: Color @Composable get() = LocalPalette.current.danger
val Success: Color @Composable get() = LocalPalette.current.success
val OutlineDark: Color @Composable get() = LocalPalette.current.outline
val ErrorBg: Color @Composable get() = LocalPalette.current.errorBg

private val AdminShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
)
private val AdminTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.1).sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        bodyMedium = bodyMedium.copy(lineHeight = 22.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold)
    )
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun AdminTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = Burgundy,
            onPrimary = Color.White,
            secondary = palette.gold,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.muted,
            error = palette.danger,
            outline = palette.outline
        )
    } else {
        lightColorScheme(
            primary = Burgundy,
            onPrimary = Color(0xFFFFFFFF),
            secondary = palette.gold,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.muted,
            error = palette.danger,
            outline = palette.outline
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                window.setBackgroundDrawable(ColorDrawable(palette.bg.toArgb()))
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, shapes = AdminShapes, typography = AdminTypography, content = content)
    }
}
