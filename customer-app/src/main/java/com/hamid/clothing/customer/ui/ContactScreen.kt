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
        ContactChannel("phone", "تماس تلفنی", info.phone2, "tel:" + clean(info.phone2)),
        ContactChannel("whatsapp", "واتساپ", info.whatsapp, "https://wa.me/" + clean(info.whatsapp)),
        ContactChannel("telegram", "تلگرام", info.telegram, "https://t.me/" + clean(info.telegram)),
        ContactChannel("instagram", "اینستاگرام", "poshake_momo", "https://instagram.com/poshake_momo"),
        ContactChannel("rubika", "روبیکا", info.rubika, "https://rubika.ir/" + clean(info.rubika)),
        ContactChannel("eitaa", "ایتا", info.eitaa, "https://eitaa.com/" + clean(info.eitaa)),
        ContactChannel("bale", "بله", info.bale, "https://ble.ir/" + clean(info.bale))
    )

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
            SoftCard(onClick = {
                if (ready) openUrl(context, channel.url)
                else toast(context, "${channel.title} هنوز تنظیم نشده است")
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BrandIcon(channel.key, size = 48.dp, enabled = ready)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(channel.title, fontWeight = FontWeight.ExtraBold)
                        Text(subtitle, color = Burgundy, style = MaterialTheme.typography.bodySmall)
                    }
                    if (ready) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted)
                }
            }
        }


        Image(
            painter = painterResource(R.drawable.banner_momo_store_location),
            contentDescription = "فروشگاه حضوری MOMO",
            modifier = Modifier.fillMaxWidth().aspectRatio(2.0864f).clip(RoundedCornerShape(24.dp))
        )
    }
}
