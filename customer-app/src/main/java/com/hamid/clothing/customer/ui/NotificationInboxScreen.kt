package com.hamid.clothing.customer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NotificationInboxScreen(nav: Nav, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("اعلان‌های فروشگاه", onBack = { nav.back() })
        SoftCard(color = Beige, modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("هنوز اعلانی دریافت نشده است", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text("اعلان تخفیف، محصول جدید و وضعیت سفارش بعد از اتصال سرور در این بخش نمایش داده می‌شود.", color = Muted)
            }
        }
    }
}
