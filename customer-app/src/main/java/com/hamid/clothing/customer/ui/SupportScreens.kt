@file:OptIn(ExperimentalMaterial3Api::class)
package com.hamid.clothing.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class LocalChatMessage(val text: String, val fromStore: Boolean)

@Composable
fun SupportChatScreen(nav: Nav, padding: PaddingValues) {
    val messages = remember { mutableStateListOf<LocalChatMessage>() }
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
        ScreenTitle("چت با فروشگاه", onBack = { nav.back() })
        if (messages.isEmpty()) {
            SoftCard(color = Beige, modifier = Modifier.padding(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("پشتیبانی MOMO", fontWeight = FontWeight.ExtraBold)
                    Text("سؤال خود را درباره سایز، موجودی یا سفارش بنویسید. پس از اتصال سرور، پاسخ فروشگاه همین‌جا نمایش داده می‌شود.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { message ->
                    SoftCard(color = if (message.fromStore) Beige else RoseLight) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(if (message.fromStore) "فروشگاه MOMO" else "شما", fontWeight = FontWeight.Bold)
                            Text(message.text)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(text, { text = it.take(1000) }, label = { Text("پیام شما") }, modifier = Modifier.weight(1f), maxLines = 4)
            Button(onClick = {
                if (text.isNotBlank()) {
                    messages.add(LocalChatMessage(text.trim(), false))
                    messages.add(LocalChatMessage("پیام شما ثبت شد؛ پاسخ فروشگاه پس از اتصال سرور ارسال می‌شود.", true))
                    text = ""
                }
            }, enabled = text.isNotBlank()) { Text("ارسال") }
        }
    }
}

@Composable
fun NotificationInboxScreen(nav: Nav, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("اعلان‌های فروشگاه", onBack = { nav.back() })
        SoftCard(color = Beige, modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("هنوز اعلانی دریافت نشده است", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text("اعلان تخفیف، محصول جدید و وضعیت سفارش بعد از اتصال سرور در این بخش نمایش داده می‌شود.", color = Muted)
            }
        }
    }
}
