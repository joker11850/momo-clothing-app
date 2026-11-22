@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.customer.ui


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import com.hamid.clothing.customer.CustomerStore
import com.hamid.clothing.customer.R

private data class ContactChannel(val key: String, val title: String, val handle: String, val url: String)


@Composable
fun ContactScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val context = LocalContext.current
    val info = store.info
    fun clean(value: String): String = value.trim().removePrefix("@")

    val channels = listOf(
        ContactChannel("phone", "تماس تلفنی", info.phone, "tel:" + clean(info.phone)),
        ContactChannel("phone", "تماس تلفنی", info.phone2, "tel:" + clean(info.phone2)).takeIf { info.phone2.isNotBlank() },
        ContactChannel("whatsapp", "واتساپ", info.whatsapp, "https://wa.me/" + clean(info.whatsapp)),
        ContactChannel("telegram", "تلگرام", info.telegram, "https://t.me/" + clean(info.telegram)),
        ContactChannel("instagram", "اینستاگرام", "poshake_momo", "https://instagram.com/poshake_momo"),
        ContactChannel("rubika", "روبیکا", info.rubika, "https://rubika.ir/" + clean(info.rubika)),
        ContactChannel("eitaa", "ایتا", info.eitaa, "https://eitaa.com/" + clean(info.eitaa)),
        ContactChannel("bale", "بله", info.bale, "https://ble.ir/" + clean(info.bale))
    ).filterNotNull()

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScreenTitle("راه‌های ارتباط با MOMO")
        Image(
            painter = painterResource(R.drawable.banner_momo_support),
            contentDescription = "پشتیبانی MOMO - کنار شما هستیم",
            modifier = Modifier.fillMaxWidth().aspectRatio(1.8018f).clip(RoundedCornerShape(24.dp))
        )

        if (channels.all { it.handle.isBlank() }) {
            SoftCard(color = Beige) {
                Text(
                    "راه‌های ارتباطی فروشگاه هنوز تنظیم نشده است و بعد از تنظیم، اینجا نمایش داده می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldInk,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }


        channels.forEach { channel ->
            val ready = channel.handle.isNotBlank()
            val subtitle = if (channel.key == "bale") "جهت خرید عمده و تکی" else "درصورت نداشتن بله راه ارتباطی"
            ContactRow(channel.key, channel.title, subtitle, ready) {
                if (ready) openUrl(context, channel.url)
                else toast(context, "${channel.title} هنوز تنظیم نشده است")
            }
        }


        Image(
            painter = painterResource(R.drawable.banner_momo_store_location),
            contentDescription = "فروشگاه حضوری MOMO",
            modifier = Modifier.fillMaxWidth().aspectRatio(2.0864f).clip(RoundedCornerShape(24.dp))
        )
    }
}


private val RowBg = Brush.verticalGradient(listOf(Color(0xFFFFFBF7), Color(0xFFFFF1EC), Color(0xFFFBDDE4)))
private val RowBorder = Brush.horizontalGradient(listOf(Color(0xFFE2B66A), Color(0xFFF3DDB0), Color(0xFFE2B66A)))

/** Pill row: logo on the right, title + subtitle, chevron on the left (RTL). */
@Composable
private fun ContactRow(key: String, title: String, subtitle: String, ready: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(36.dp)
    val icon = drawableId("contact_icon_$key")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clip(shape)
            .background(RowBg)
            .border(1.dp, RowBorder, shape)
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFEEE6))))
                .border(1.dp, Color(0xFFF1DCC8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(46.dp).alpha(if (ready) 1f else 0.4f)
                )
            } else {
                BrandIcon(key, size = 46.dp, enabled = ready)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFF3A0F24), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Color(0xFF8C6F6F), style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF7D1634), modifier = Modifier.size(28.dp))
    }
}
