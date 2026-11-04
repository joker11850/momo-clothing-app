@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamid.clothing.customer.StoreConfig

private data class Channel(val title: String, val handle: String, val url: String)

@Composable
fun ContactScreen(nav: Nav, padding: PaddingValues) {
    val context = LocalContext.current
    val channels = listOf(
        Channel("تماس تلفنی", StoreConfig.PHONE_NUMBER, "tel:" + StoreConfig.PHONE_NUMBER),
        Channel("واتساپ", StoreConfig.WHATSAPP_NUMBER, "https://wa.me/" + StoreConfig.WHATSAPP_NUMBER),
        Channel("تلگرام", StoreConfig.TELEGRAM_USER, "https://t.me/" + StoreConfig.TELEGRAM_USER),
        Channel("اینستاگرام", StoreConfig.INSTAGRAM_USER, "https://instagram.com/" + StoreConfig.INSTAGRAM_USER),
        Channel("روبیکا", StoreConfig.ROBIKA_USER, "https://rubika.ir/" + StoreConfig.ROBIKA_USER),
        Channel("ایتا", StoreConfig.EITAA_USER, "https://eitaa.com/" + StoreConfig.EITAA_USER),
        Channel("بله", StoreConfig.BALE_USER, "https://ble.ir/" + StoreConfig.BALE_USER)
    )
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("تماس با ما")
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (channels.all { it.handle.isEmpty() }) {
                SoftCard(color = Beige) {
                    Text(
                        "راه‌های ارتباطی فروشگاه هنوز تنظیم نشده است و بعد از تنظیم، اینجا نمایش داده می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldInk,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            for (c in channels) {
                val ready = c.handle.isNotEmpty()
                SoftCard(onClick = { if (ready) openUrl(context, c.url) else toast(context, "${c.title} هنوز تنظیم نشده است") }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.title, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Text(
                            if (ready) faDigits(c.handle) else "تنظیم نشده",
                            color = if (ready) Burgundy else Muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
