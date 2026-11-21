package com.hamid.clothing.customer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Round icon of a contact channel: phone, whatsapp, telegram, instagram, rubika, eitaa, bale.
 * If res/drawable has brand_<key> (for example brand_rubika.png) that picture is used instead of the drawn icon.
 */
@Composable
fun BrandIcon(key: String, modifier: Modifier = Modifier, size: Dp = 48.dp, enabled: Boolean = true) {
    val custom = drawableId("brand_$key")
    val plain = modifier.size(size).alpha(if (enabled) 1f else 0.4f)
    // a real logo is shown as it is (its own shape); the drawn fallbacks below are round
    if (custom != null) {
        Image(painter = painterResource(custom), contentDescription = null, contentScale = ContentScale.Fit, modifier = plain)
        return
    }
    val base = plain.clip(CircleShape)
    when (key) {
        "instagram" -> Box(
            modifier = base.background(Brush.linearGradient(listOf(Color(0xFF8A3AB9), Color(0xFFE1306C), Color(0xFFFCAF45)))),
            contentAlignment = Alignment.Center
        ) { InstagramGlyph(size * 0.5f) }
        "telegram" -> Box(modifier = base.background(Color(0xFF229ED9)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.48f).rotate(-25f))
        }
        "whatsapp" -> Box(modifier = base.background(Color(0xFF25D366)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
        }
        "phone" -> Box(modifier = base.background(MintDark), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
        }
        "rubika" -> LetterIcon("ر", base.background(Brush.linearGradient(listOf(Color(0xFF7B2FF7), Color(0xFFD946EF)))), size)
        "eitaa" -> LetterIcon("ا", base.background(Color(0xFFF57C00)), size)
        "bale" -> LetterIcon("ب", base.background(Color(0xFF00A896)), size)
        else -> LetterIcon("؟", base.background(Muted), size)
    }
}

@Composable
private fun LetterIcon(letter: String, modifier: Modifier, size: Dp) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(letter, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.46f).sp)
    }
}

/** Camera outline: rounded square, lens and flash dot. */
@Composable
private fun InstagramGlyph(glyph: Dp) {
    Canvas(modifier = Modifier.size(glyph)) {
        val w = this.size.width
        val stroke = w * 0.11f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(w - stroke, w - stroke),
            cornerRadius = CornerRadius(w * 0.3f),
            style = Stroke(width = stroke)
        )
        drawCircle(color = Color.White, radius = w * 0.2f, center = Offset(w / 2, w / 2), style = Stroke(width = stroke))
        drawCircle(color = Color.White, radius = w * 0.055f, center = Offset(w * 0.76f, w * 0.24f))
    }
}
