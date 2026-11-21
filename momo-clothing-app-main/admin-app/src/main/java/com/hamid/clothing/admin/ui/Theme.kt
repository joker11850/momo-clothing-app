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
val Burgundy = Color(0xFFFF4652)
val BurgundyDeep = Color(0xFF4A123B)

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
    gold = Color(0xFFFFB38A),
    text = Color(0xFFFFF8F3),
    muted = Color(0xFFD3BFC7),
    bg = Color(0xFF24141F),
    surface = Color(0xFF351B2C),
    surfaceHigh = Color(0xFF4A123B),
    danger = Color(0xFFFF7882),
    success = Color(0xFF72C49A),
    outline = Color(0xFF6A4058),
    errorBg = Color(0xFF542333)
)

val LightPalette = Palette(
    dark = false,
    gold = Color(0xFFFF4652),
    text = Color(0xFF281D27),
    muted = Color(0xFF887B80),
    bg = Color(0xFFFFF8F3),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFFFE8DE),
    danger = Color(0xFFC73645),
    success = Color(0xFF2E9B74),
    outline = Color(0xFFF0D5CF),
    errorBg = Color(0xFFFFE1E4)
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
