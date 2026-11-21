@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.admin.PublishResult
import com.hamid.clothing.core.CatalogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PublishScreen(store: AdminStore, padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var building by remember { mutableStateOf(false) }
    var last by remember { mutableStateOf<PublishResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val data = CatalogBuilder.fromSnapshot(store.snapshot, System.currentTimeMillis())
    val photoCount = data.products.sumOf { it.imageUris.size }
    val warnings = CatalogBuilder.warnings(store.snapshot)

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("انتشار کاتالوگ", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminCard {
                Text("کاتالوگ برای مشتریان", fontWeight = FontWeight.Bold, color = Gold)
                Text(
                    "${faInt(data.products.size)} کالا · ${faInt(photoCount)} عکس · ${faInt(data.categories.size)} دسته · ${faInt(data.bankAccounts.size)} حساب بانکی قابل نمایش",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (store.lastPublishedAt > 0) "آخرین انتشار: ${faDate(store.lastPublishedAt)}" else "هنوز کاتالوگی منتشر نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }

            AdminCard {
                Text("چه چیزی منتشر می‌شود؟", fontWeight = FontWeight.Bold)
                Text(
                    "فقط کالاهای «فعال» با نام، توضیحات، عکس‌ها، رنگ و سایز، موجودی و قیمت خرده و عمده، به‌همراه مشخصات فروشگاه، راه‌های ارتباطی، هزینه ارسال و حساب‌های بانکی که «نمایش به مشتری» دارند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Text(
                    "قیمت خرید، قیمت تمام‌شده، مشتریان، سفارش‌ها، چک‌ها و حساب‌ها هرگز در کاتالوگ نیست.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Success,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (warnings.isNotEmpty()) {
                AdminCard {
                    Text("پیشنهاد قبل از انتشار", fontWeight = FontWeight.Bold, color = Danger)
                    for (w in warnings.take(10)) Text("• $w", style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (warnings.size > 10) Text("و ${faInt(warnings.size - 10)} مورد دیگر…", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }

            AdminCard {
                Text("روش ارسال به مشتری", fontWeight = FontWeight.Bold)
                Text(
                    "بدون سرور، کاتالوگ به‌صورت یک فایل ساخته می‌شود. آن را از طریق تلگرام، ایتا، بله، واتساپ یا هر پیام‌رسانی برای مشتری‌ها بفرستید. مشتری فایل را با اپ فروشگاه باز می‌کند (یا در اپ، «دریافت کاتالوگ» را می‌زند) و کالاها و عکس‌ها به‌روز می‌شوند. بعد از راه‌اندازی سرور، این مرحله خودکار می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }

            if (error != null) ErrorBox(listOf(error ?: ""))
            val result = last
            if (result != null) {
                AdminCard {
                    Text("آماده شد", fontWeight = FontWeight.Bold, color = Success)
                    Text(
                        "${result.file.name} — ${faInt(result.products)} کالا، ${faInt(result.images)} عکس، ${PersianSize.format(result.bytes)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            fun publish() {
                building = true
                error = null
                scope.launch {
                    val built = withContext(Dispatchers.IO) {
                        try {
                            store.buildCatalogFile()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    building = false
                    if (built == null) {
                        error = "ساخت فایل کاتالوگ ناموفق بود"
                    } else {
                        last = built
                        shareFile(context, built.file, "application/zip", "ارسال کاتالوگ")
                    }
                }
            }
            Button(
                onClick = { publish() },
                enabled = !building && data.products.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (building) "در حال ساخت فایل…" else "ساخت و ارسال کاتالوگ") }
            if (data.products.isEmpty()) {
                Text("کالای فعالی برای انتشار وجود ندارد.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

private object PersianSize {
    fun format(bytes: Long): String {
        val kb = bytes / 1024.0
        return if (kb < 1024) faDigitsOf("%.0f".format(java.util.Locale.ROOT, kb)) + " کیلوبایت"
        else faDigitsOf("%.1f".format(java.util.Locale.ROOT, kb / 1024.0)) + " مگابایت"
    }

    private fun faDigitsOf(s: String): String = com.hamid.clothing.core.PersianFormat.digits(s)
}
