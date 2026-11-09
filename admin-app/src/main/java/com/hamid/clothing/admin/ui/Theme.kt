package com.hamid.clothing.admin.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hamid.clothing.admin.ThemeMode

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
    gold = Color(0xFFD6B46A),
    text = Color(0xFFF2EBDD),
    muted = Color(0xFFA8A29A),
    bg = Color(0xFF101010),
    surface = Color(0xFF1B1B1B),
    surfaceHigh = Color(0xFF262626),
    danger = Color(0xFFE57373),
    success = Color(0xFF7FC48A),
    outline = Color(0xFF3A3A3A),
    errorBg = Color(0xFF3A1F1F)
)

val LightPalette = Palette(
    dark = false,
    gold = Color(0xFFA97A1C),
    text = Color(0xFF1E1A14),
    muted = Color(0xFF6D655A),
    bg = Color(0xFFF7F3EC),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEFE8DA),
    danger = Color(0xFFC0392B),
    success = Color(0xFF2E7D4F),
    outline = Color(0xFFDDD3C3),
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
            primary = palette.gold,
            onPrimary = Color(0xFF1A1405),
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
            primary = palette.gold,
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
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
