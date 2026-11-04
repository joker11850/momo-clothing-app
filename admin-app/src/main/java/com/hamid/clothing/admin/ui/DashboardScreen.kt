@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.hamid.clothing.core.DashboardCalculator

@Composable
fun DashboardScreen(
    store: AdminStore,
    padding: PaddingValues,
    onOpenLowStock: () -> Unit,
    onOpenWholesaleRequests: () -> Unit
) {
    val stats = DashboardCalculator.compute(store.products, store.customers, store.balances)
    val lowItems = store.products.flatMap { p -> p.lowStockVariants.map { v -> p to v } }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmSample by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("مدیریت فروشگاه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("نمای کلی وضعیت فروشگاه", style = MaterialTheme.typography.bodyMedium, color = Muted)
        }

        if (store.isSample) {
            item {
                AdminCard {
                    Text("داده‌های فعلی آزمایشی هستند", fontWeight = FontWeight.Bold, color = Gold)
                    Text(
                        "برای شروع کار واقعی، داده‌های آزمایشی را پاک کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("پاک‌کردن داده‌های آزمایشی")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("تعداد کالا", faInt(stats.productCount), Modifier.weight(1f))
                StatCard("موجودی کل (عدد)", faInt(stats.totalUnits), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("ارزش موجودی (قیمت تمام‌شده)", formatToman(stats.stockValueAtCost), Modifier.weight(1f))
                StatCard(
                    "بدهی مشتریان",
                    formatToman(stats.totalReceivable),
                    Modifier.weight(1f),
                    valueColor = if (stats.totalReceivable > 0) Danger else Gold
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("مشتریان", faInt(stats.customerCount), Modifier.weight(1f))
                StatCard("مشتریان بدهکار", faInt(stats.debtorCount), Modifier.weight(1f))
            }
        }

        if (stats.pendingWholesaleRequests > 0) {
            item {
                AdminCard(onClick = onOpenWholesaleRequests) {
                    Text("درخواست عضویت عمده", fontWeight = FontWeight.Bold, color = Gold)
                    Text(
                        "${faInt(stats.pendingWholesaleRequests)} درخواست در انتظار بررسی شما است",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item { SectionLabel("هشدار کمبود موجودی") }
        if (lowItems.isEmpty()) {
            item { Text("همه‌ی موجودی‌ها در حد مطلوب هستند", color = Muted) }
        } else {
            item {
                AdminCard(onClick = onOpenLowStock) {
                    Text(
                        "${faInt(stats.lowStockVariantCount)} رنگ/سایز کم‌موجودی",
                        fontWeight = FontWeight.Bold,
                        color = Danger
                    )
                    for ((p, v) in lowItems.take(6)) {
                        Text(
                            "${p.name} — ${v.color} / ${v.size}: ${faInt(v.stock)} عدد",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (lowItems.size > 6) {
                        Text("و ${faInt(lowItems.size - 6)} مورد دیگر…", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { SectionLabel("بخش‌های در راه") }
        item {
            AdminCard {
                Text(
                    "سفارش‌ها و ارسال، حسابداری تأمین‌کنندگان و چک، هزینه‌ها، گزارش‌های فروش و سود، فاکتور، تخفیف و اعلان‌ها در مراحل بعدی اضافه می‌شوند. فروش امروز و ماه هم بعد از ثبت سفارش‌ها نمایش داده می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }

        item {
            OutlinedButton(onClick = { confirmSample = true }, modifier = Modifier.fillMaxWidth()) {
                Text("بارگذاری مجدد داده‌های آزمایشی")
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "پاک‌کردن داده‌ها",
            text = "همه‌ی کالاها، مشتریان و گردش حساب‌ها حذف می‌شوند. این کار قابل بازگشت نیست.",
            confirmLabel = "پاک شود",
            onConfirm = { store.clearAllData(); confirmClear = false },
            onDismiss = { confirmClear = false }
        )
    }
    if (confirmSample) {
        ConfirmDialog(
            title = "بارگذاری داده‌ی آزمایشی",
            text = "داده‌های فعلی با داده‌ی آزمایشی جایگزین می‌شوند.",
            confirmLabel = "جایگزین شود",
            onConfirm = { store.loadSampleData(); confirmSample = false },
            onDismiss = { confirmSample = false }
        )
    }
}
