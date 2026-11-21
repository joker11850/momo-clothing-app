@file:OptIn(ExperimentalMaterial3Api::class)
package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore

@Composable
fun NotificationsScreen(store: AdminStore, padding: PaddingValues, onBack: () -> Unit) {
    var title by remember { mutableStateOf(store.pendingNotification?.title ?: "") }
    var body by remember { mutableStateOf(store.pendingNotification?.body ?: "") }
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("اطلاع‌رسانی به مشتریان", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminCard {
                Text("ساخت پیام اطلاع‌رسانی", fontWeight = FontWeight.Bold, color = Gold)
                Text(
                    "عنوان و متن پیام را آماده کنید. فعلاً پیام در صف محلی ذخیره می‌شود؛ بعد از اتصال سرور، همین صف برای اعلان درون‌برنامه‌ای و اعلان بالای گوشی ارسال خواهد شد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120); saved = false },
                label = { Text("عنوان اعلان") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it.take(2000); saved = false },
                label = { Text("متن پیام") },
                minLines = 5,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { store.queueNotification(title, body); saved = true },
                enabled = title.isNotBlank() && body.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیره در صف ارسال") }
            if (saved) {
                AdminCard {
                    Text("پیام آماده‌ی اتصال به سرور است", fontWeight = FontWeight.Bold, color = Success)
                    Text("با خرید سرور، لایه‌ی ارسال REST/FCM به همین صف متصل می‌شود.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            store.pendingNotification?.let { pending ->
                AdminCard {
                    Text("آخرین پیام صف‌شده", fontWeight = FontWeight.Bold)
                    Text(pending.title, style = MaterialTheme.typography.titleMedium)
                    Text(pending.body, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
