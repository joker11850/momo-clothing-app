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
import com.hamid.clothing.customer.CustomerStore

private data class Channel(val title: String, val handle: String, val url: String)

@Composable
fun ContactScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val context = LocalContext.current
    val info = store.info
    val channels = listOf(
        Channel("تماس تلفنی", info.phone, "tel:" + info.phone),
        Channel("واتساپ", info.whatsapp, "https://wa.me/" + info.whatsapp),
        Channel("تلگرام", info.telegram, "https://t.me/" + info.telegram),
        Channel("اینستاگرام", info.instagram, "https://instagram.com/" + info.instagram),
        Channel("روبیکا", info.rubika, "https://rubika.ir/" + info.rubika),
        Channel("ایتا", info.eitaa, "https://eitaa.com/" + info.eitaa),
        Channel("بله", info.bale, "https://ble.ir/" + info.bale)
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
