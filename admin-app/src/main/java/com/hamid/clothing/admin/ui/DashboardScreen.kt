@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import com.hamid.clothing.admin.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
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
import com.hamid.clothing.core.ChequeService
import com.hamid.clothing.core.ChequeDirection
import com.hamid.clothing.core.DashboardCalculator
import com.hamid.clothing.core.OrderStatus
import com.hamid.clothing.core.Periods
import com.hamid.clothing.core.ReportEngine

@Composable
fun DashboardScreen(
    store: AdminStore,
    padding: PaddingValues,
    onOpenLowStock: () -> Unit,
    onOpenWholesaleRequests: () -> Unit,
    onOpenOrders: (OrderFilter) -> Unit,
    onOpenCheques: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPublish: () -> Unit,
    onOpenBackup: () -> Unit
) {
    val now = System.currentTimeMillis()
    val (dayStart, dayEnd) = Periods.day(now)
    val (monthStart, monthEnd) = Periods.month(now)
    val today = ReportEngine.profit(store.orders, store.expenses, dayStart, dayEnd)
    val month = ReportEngine.profit(store.orders, store.expenses, monthStart, monthEnd)
    val stats = DashboardCalculator.compute(store.products, store.customers, store.balances)
    val lowItems = store.products.flatMap { p -> p.lowStockVariants.map { v -> p to v } }
    val needAction = store.orders.count { it.status == OrderStatus.AWAITING_SELLER_APPROVAL || it.status == OrderStatus.PAID }
    val unpaid = store.orders.count { it.status == OrderStatus.AWAITING_PAYMENT }
    val dueCheques = store.cheques.filter { it.isDueSoon(now, 7) }
    val dueTotal = dueCheques.sumOf { it.amount }
    val receivedOpen = ChequeService.pendingTotal(store.cheques, ChequeDirection.RECEIVED)
    var confirmClear by remember { mutableStateOf(false) }
    var confirmSample by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painter = painterResource(R.drawable.brand_logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                )
                Column {
                    Text(
                        store.settings.shopName.ifBlank { "پوشاک MOMO" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("مدیریت فروشگاه", style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
            }
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

        if (store.lastBackupAt == 0L || now - store.lastBackupAt > 7 * 86_400_000L) {
            item {
                AdminCard(onClick = onOpenBackup) {
                    Text("از اطلاعات پشتیبان بگیرید", fontWeight = FontWeight.Bold, color = Danger)
                    Text(
                        if (store.lastBackupAt == 0L) "هنوز پشتیبان‌گیری نکرده‌اید. اگر گوشی گم یا ریست شود، اطلاعات از بین می‌رود."
                        else "آخرین پشتیبان: ${faDate(store.lastBackupAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }
        }

        // reminders first: what needs the owner's attention
        if (needAction > 0 || unpaid > 0 || dueCheques.isNotEmpty() || stats.pendingWholesaleRequests > 0) {
            item { SectionLabel("نیازمند اقدام") }
        }
        if (needAction > 0) {
            item {
                AdminCard(onClick = { onOpenOrders(OrderFilter.ACTION) }) {
                    Text("${faInt(needAction)} سفارش منتظر اقدام شما است", fontWeight = FontWeight.Bold, color = Gold)
                    Text("تأیید سفارش عمده یا شروع آماده‌سازی", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
        if (unpaid > 0) {
            item {
                AdminCard(onClick = { onOpenOrders(OrderFilter.UNPAID) }) {
                    Text("${faInt(unpaid)} سفارش پرداخت‌نشده", fontWeight = FontWeight.Bold)
                    Text("موجودی این سفارش‌ها رزرو شده است", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
        if (dueCheques.isNotEmpty()) {
            item {
                AdminCard(onClick = onOpenCheques) {
                    Text("${faInt(dueCheques.size)} چک تا ۷ روز آینده سررسید دارد", fontWeight = FontWeight.Bold, color = Danger)
                    Text("جمع مبلغ: ${formatToman(dueTotal)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
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

        item { SectionLabel("فروش و سود") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("فروش امروز", formatToman(today.salesTotal), Modifier.weight(1f))
                StatCard("فروش این ماه", formatToman(month.salesTotal), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("سود ناخالص ماه", formatToman(month.grossProfit), Modifier.weight(1f))
                StatCard(
                    "سود خالص ماه",
                    formatToman(month.netProfit),
                    Modifier.weight(1f),
                    valueColor = if (month.netProfit < 0) Danger else Success
                )
            }
        }
        item { AdminCard { SectionLabel("فروش ۱۴ روز اخیر"); SalesBars(dailySales(store, now, 14)) } }
        item { OutlinedButton(onClick = onOpenReports, modifier = Modifier.fillMaxWidth()) { Text("گزارش‌های کامل") } }

        item { SectionLabel("انبار و حساب‌ها") }
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
                StatCard("چک‌های دریافتی باز", formatToman(receivedOpen), Modifier.weight(1f))
            }
        }

        item {
            AdminCard(onClick = onOpenPublish) {
                Text("انتشار کاتالوگ برای مشتریان", fontWeight = FontWeight.Bold, color = Gold)
                Text(
                    if (store.lastPublishedAt > 0) "آخرین انتشار: ${faDate(store.lastPublishedAt)}" else "هنوز کاتالوگی منتشر نشده است",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
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

        item {
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("تنظیمات فروشگاه") }
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
            text = "همه‌ی کالاها، مشتریان، سفارش‌ها، چک‌ها، تأمین‌کنندگان، هزینه‌ها و حساب‌ها حذف می‌شوند. این کار قابل بازگشت نیست.",
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
