@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamid.clothing.customer.CustomerStore

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
        ContactChannel("instagram", "اینستاگرام", info.instagram, "https://instagram.com/" + clean(info.instagram)),
        ContactChannel("rubika", "روبیکا", info.rubika, "https://rubika.ir/" + clean(info.rubika)),
        ContactChannel("eitaa", "ایتا", info.eitaa, "https://eitaa.com/" + clean(info.eitaa)),
        ContactChannel("bale", "بله", info.bale, "https://ble.ir/" + clean(info.bale))
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BrandIcon(c.key, size = 48.dp, enabled = ready)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.title, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (ready) "برای ارتباط لمس کنید" else "تنظیم نشده",
                                color = if (ready) Burgundy else Muted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (ready) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted)
                    }
                }
            }
            Column(modifier = Modifier.height(24.dp)) {}
        }
    }
}
