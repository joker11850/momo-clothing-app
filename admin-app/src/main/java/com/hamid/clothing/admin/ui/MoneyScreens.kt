@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.BankAccount
import com.hamid.clothing.core.ChequeDirection
import com.hamid.clothing.core.ChequeService
import com.hamid.clothing.core.ExpenseCategory
import com.hamid.clothing.core.JalaliCalendar
import com.hamid.clothing.core.Periods
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.core.ReportEngine
import com.hamid.clothing.core.SalesStats
import com.hamid.clothing.core.StoreSettings

// ---------------------------------------------------------------- expenses

private enum class ExpenseView { MONTH, ALL }
private enum class ExpenseKind { ALL, BUSINESS, PERSONAL }

@Composable
fun ExpensesScreen(store: AdminStore, padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    var view by remember { mutableStateOf(ExpenseView.MONTH) }
    var kind by remember { mutableStateOf(ExpenseKind.ALL) }
    var addDialog by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    val now = System.currentTimeMillis()
    val (monthStart, monthEnd) = Periods.month(now)

    val inView = store.expenses.filter { view == ExpenseView.ALL || (it.at >= monthStart && it.at < monthEnd) }
    val visible = inView
        .filter { kind == ExpenseKind.ALL || (kind == ExpenseKind.PERSONAL) == it.isPersonal }
        .sortedByDescending { it.at }
    val business = inView.filter { !it.isPersonal }.sumOf { it.amount }
    val personal = inView.filter { it.isPersonal }.sumOf { it.amount }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader("هزینه‌ها", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChips(
                    options = listOf(ExpenseView.MONTH to "این ماه", ExpenseView.ALL to "همه"),
                    selected = view,
                    onSelect = { view = it }
                )
                ChoiceChips(
                    options = listOf(ExpenseKind.ALL to "همه", ExpenseKind.BUSINESS to "کاری", ExpenseKind.PERSONAL to "شخصی"),
                    selected = kind,
                    onSelect = { kind = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("هزینه‌های کاری", formatToman(business), Modifier.weight(1f))
                    StatCard("هزینه‌های شخصی", formatToman(personal), Modifier.weight(1f), valueColor = Muted)
                }
                Text("هزینه‌ی شخصی در سود فروشگاه حساب نمی‌شود.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (visible.isEmpty()) EmptyState("هزینه‌ای ثبت نشده است.")
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { e ->
                    AdminCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(e.category.label(), fontWeight = FontWeight.Bold, color = if (e.isPersonal) Muted else MaterialTheme.colorScheme.onSurface)
                                Text(
                                    faDate(e.at) + if (e.note.isNotBlank()) " · ${e.note}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                            Text(formatToman(e.amount), color = Gold, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { deleteId = e.id }) { Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Danger) }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { addDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("هزینه جدید") },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }

    if (addDialog) {
        var category by remember { mutableStateOf(ExpenseCategory.STORE) }
        var amountText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }
        val today = remember { todayParts() }
        var year by remember { mutableStateOf(today.first) }
        var month by remember { mutableStateOf(today.second) }
        var day by remember { mutableStateOf(today.third) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("هزینه‌ی جدید") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChips(options = ExpenseCategory.values().map { it to it.label() }, selected = category, onSelect = { category = it })
                    MoneyField("مبلغ", amountText, { amountText = it })
                    AdminTextField("توضیح (اختیاری)", noteText, { noteText = it })
                    JalaliDateField("تاریخ", year, month, day) { y, m, d -> year = y; month = m; day = d }
                    if (error != null) Text(error ?: "", color = Danger)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val at = jalaliToMillis(year, month, day)
                    if (at == null) {
                        error = "تاریخ معتبر نیست"
                    } else {
                        val result = store.addExpense(category, parseMoney(amountText), noteText, at + 12 * 3_600_000L)
                        if (result == null) addDialog = false else error = result
                    }
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text("انصراف") } }
        )
    }
    val pendingDelete = deleteId
    if (pendingDelete != null) {
        ConfirmDialog(
            title = "حذف هزینه",
            text = "این هزینه حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = { store.deleteExpense(pendingDelete); deleteId = null; toast(context, "حذف شد") },
            onDismiss = { deleteId = null }
        )
    }
}

// ---------------------------------------------------------------- bank accounts

@Composable
fun BankAccountsScreen(
    store: AdminStore,
    padding: PaddingValues,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader("حساب‌های بانکی", onBack = onBack)
            if (store.bankAccounts.isEmpty()) EmptyState("هنوز حسابی ثبت نشده است.")
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(store.bankAccounts, key = { it.id }) { b ->
                    AdminCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(b.bankName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onEdit(b.id) }) { Icon(Icons.Default.Edit, contentDescription = "ویرایش") }
                        }
                        Text(b.ownerName, style = MaterialTheme.typography.bodySmall, color = Muted)
                        if (b.cardNumber.isNotBlank()) Text("کارت: ${PersianFormat.digits(b.cardNumber)}", style = MaterialTheme.typography.bodyMedium)
                        if (b.sheba.isNotBlank()) Text("شبا: ${PersianFormat.digits(b.sheba)}", style = MaterialTheme.typography.bodyMedium)
                        if (b.accountNumber.isNotBlank()) Text("حساب: ${PersianFormat.digits(b.accountNumber)}", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Text("فعال", modifier = Modifier.weight(1f))
                            Switch(checked = b.active, onCheckedChange = { store.saveBankAccount(b.copy(active = it)) })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("نمایش به مشتری", modifier = Modifier.weight(1f))
                            Switch(checked = b.visibleToCustomers, onCheckedChange = { store.saveBankAccount(b.copy(visibleToCustomers = it)) })
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("حساب جدید") },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }
}

@Composable
fun BankFormScreen(
    store: AdminStore,
    padding: PaddingValues,
    accountId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val existing = remember(accountId) { store.bankAccounts.firstOrNull { it.id == accountId } }
    var bank by remember(accountId) { mutableStateOf(existing?.bankName ?: "") }
    var owner by remember(accountId) { mutableStateOf(existing?.ownerName ?: "") }
    var card by remember(accountId) { mutableStateOf(existing?.cardNumber ?: "") }
    var sheba by remember(accountId) { mutableStateOf(existing?.sheba ?: "") }
    var number by remember(accountId) { mutableStateOf(existing?.accountNumber ?: "") }
    var active by remember(accountId) { mutableStateOf(existing?.active ?: true) }
    var visible by remember(accountId) { mutableStateOf(existing?.visibleToCustomers ?: false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader(if (existing == null) "حساب جدید" else "ویرایش حساب", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (error != null) ErrorBox(listOf(error ?: ""))
            AdminTextField("نام بانک", bank, { bank = it })
            AdminTextField("نام صاحب حساب", owner, { owner = it })
            AdminTextField("شماره کارت (۱۶ رقم)", card, { card = it }, keyboardType = KeyboardType.Number)
            AdminTextField("شماره شبا (IR…)", sheba, { sheba = it })
            AdminTextField("شماره حساب", number, { number = it }, keyboardType = KeyboardType.Number)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("فعال", modifier = Modifier.weight(1f))
                Switch(checked = active, onCheckedChange = { active = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("نمایش به مشتری", modifier = Modifier.weight(1f))
                Switch(checked = visible, onCheckedChange = { visible = it })
            }
            Text("شماره کارت و شبا فقط به‌صورت رمزنگاری‌شده روی گوشی ذخیره می‌شوند.", style = MaterialTheme.typography.bodySmall, color = Muted)
            Button(
                onClick = {
                    error = store.saveBankAccount(
                        BankAccount(
                            id = existing?.id ?: store.newId(),
                            bankName = bank.trim(),
                            ownerName = owner.trim(),
                            cardNumber = com.hamid.clothing.core.Validators.toLatinDigits(card).filter { it in '0'..'9' },
                            sheba = com.hamid.clothing.core.Validators.toLatinDigits(sheba).replace(" ", "").uppercase(),
                            accountNumber = com.hamid.clothing.core.Validators.toLatinDigits(number).trim(),
                            active = active,
                            visibleToCustomers = visible
                        )
                    )
                    if (error == null) onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیره") }
            if (existing != null) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("حذف حساب", color = Danger) }
            }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "حذف حساب",
            text = "این حساب حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = { confirmDelete = false; store.deleteBankAccount(existing.id); onDone() },
            onDismiss = { confirmDelete = false }
        )
    }
}

// ---------------------------------------------------------------- reports

private enum class ReportPeriod { THIS_MONTH, LAST_MONTH }

@Composable
fun ReportsScreen(store: AdminStore, padding: PaddingValues, onBack: () -> Unit) {
    var period by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    val now = System.currentTimeMillis()
    val thisMonth = Periods.month(now)
    val range = if (period == ReportPeriod.THIS_MONTH) thisMonth else Periods.month(thisMonth.first - 1)
    val (from, to) = range

    val report = ReportEngine.profit(store.orders, store.expenses, from, to)
    val best = SalesStats.byProduct(store.orders, from, to)
    val soldIds = best.map { it.productId }.toSet()
    val slow = store.products.filter { it.id !in soldIds && it.totalStock > 0 }.take(5)
    val debtors = store.customers
        .map { it to store.balanceOf(it.id) }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(10)
    val payable = store.suppliers.map { it to store.balanceOf(it.id) }.filter { it.second > 0 }
    val receivedOpen = ChequeService.pendingTotal(store.cheques, ChequeDirection.RECEIVED)
    val paidOpen = ChequeService.pendingTotal(store.cheques, ChequeDirection.PAID)
    val stockValue = store.products.sumOf { p -> p.variants.sumOf { v -> p.landedCost * v.stock } }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("گزارش‌ها", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChoiceChips(
                options = listOf(ReportPeriod.THIS_MONTH to "این ماه", ReportPeriod.LAST_MONTH to "ماه قبل"),
                selected = period,
                onSelect = { period = it }
            )
            Text(
                "از ${faDate(from)} تا ${faDate(to - 1)}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("فروش", formatToman(report.salesTotal), Modifier.weight(1f))
                StatCard("بهای تمام‌شده", formatToman(report.costOfGoods), Modifier.weight(1f), valueColor = Muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("سود ناخالص", formatToman(report.grossProfit), Modifier.weight(1f))
                StatCard("هزینه‌های کاری", formatToman(report.businessExpenses), Modifier.weight(1f), valueColor = Muted)
            }
            StatCard(
                "سود خالص",
                formatToman(report.netProfit),
                Modifier.fillMaxWidth(),
                valueColor = if (report.netProfit < 0) Danger else Success
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("سود خرده‌فروشی", formatToman(report.retailGrossProfit), Modifier.weight(1f))
                StatCard("سود عمده‌فروشی", formatToman(report.wholesaleGrossProfit), Modifier.weight(1f))
            }
            Text("هزینه‌های شخصی این دوره: ${formatToman(report.personalExpenses)} (در سود حساب نمی‌شود)", style = MaterialTheme.typography.bodySmall, color = Muted)

            SectionLabel("فروش ۱۴ روز اخیر")
            val days = dailySales(store, now, 14)
            AdminCard { SalesBars(days) }

            SectionLabel("پرفروش‌ترین کالاها")
            if (best.isEmpty()) Text("در این دوره فروشی ثبت نشده است.", color = Muted)
            for (s in best.take(5)) {
                AdminCard {
                    Text(s.name, fontWeight = FontWeight.Bold)
                    Text("${faInt(s.quantity)} عدد · ${formatToman(s.revenue)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }

            SectionLabel("کم‌فروش‌ها (بدون فروش در این دوره)")
            if (slow.isEmpty()) Text("موردی نیست.", color = Muted)
            for (p in slow) Text("${p.name} — موجودی ${faInt(p.totalStock)} عدد", style = MaterialTheme.typography.bodyMedium)

            SectionLabel("بدهکاران")
            if (debtors.isEmpty()) Text("بدهکاری وجود ندارد.", color = Muted)
            for ((c, balance) in debtors) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(c.fullName, modifier = Modifier.weight(1f))
                    Text(formatToman(balance), color = Danger)
                }
            }

            SectionLabel("بدهی به تأمین‌کنندگان")
            if (payable.isEmpty()) Text("بدهی وجود ندارد.", color = Muted)
            for ((s, balance) in payable) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(s.name, modifier = Modifier.weight(1f))
                    Text(formatToman(balance), color = Danger)
                }
            }

            SectionLabel("چک‌ها و موجودی")
            Text("چک‌های دریافتی باز: ${formatToman(receivedOpen)}")
            Text("چک‌های پرداختی باز: ${formatToman(paidOpen)}")
            Text("ارزش موجودی انبار (قیمت تمام‌شده): ${formatToman(stockValue)}")
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

// ---------------------------------------------------------------- settings

@Composable
fun SettingsScreen(store: AdminStore, padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = store.settings
    var name by remember { mutableStateOf(s.shopName) }
    var phone by remember { mutableStateOf(s.phone) }
    var address by remember { mutableStateOf(s.address) }
    var retailHours by remember { mutableStateOf(s.retailHoldHours.toString()) }
    var wholesaleHours by remember { mutableStateOf(s.wholesaleHoldHours.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("تنظیمات", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("مشخصات فروشگاه (روی فاکتور می‌آید)")
            AdminTextField("نام فروشگاه", name, { name = it })
            AdminTextField("تلفن", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            AdminTextField("آدرس", address, { address = it }, singleLine = false)

            SectionLabel("مدت رزرو موجودی")
            IntField("سفارش خرده‌ی پرداخت‌نشده (ساعت)", retailHours, { retailHours = it }, Modifier.fillMaxWidth())
            IntField("سفارش عمده‌ی در انتظار تأیید (ساعت؛ ۰ = بدون انقضا)", wholesaleHours, { wholesaleHours = it }, Modifier.fillMaxWidth())
            Text(
                "بعد از این مدت، سفارش پرداخت‌نشده خودکار لغو و موجودی آزاد می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Button(
                onClick = {
                    store.saveSettings(
                        StoreSettings(
                            shopName = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            retailHoldHours = parseInt(retailHours, 24).coerceAtLeast(1),
                            wholesaleHoldHours = parseInt(wholesaleHours, 0)
                        )
                    )
                    toast(context, "تنظیمات ذخیره شد")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیره تنظیمات") }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

/** Sales per Jalali day for the last [days] days (oldest first), labelled with the day of month. */
fun dailySales(store: AdminStore, now: Long, days: Int): List<Pair<String, Long>> =
    (days - 1 downTo 0).map { back ->
        val (dayStart, dayEnd) = Periods.day(now - back * 86_400_000L)
        val total = store.orders.filter { it.countsAsSale && it.createdAt >= dayStart && it.createdAt < dayEnd }.sumOf { it.total }
        PersianFormat.digits(JalaliCalendar.fromEpochMillis(dayStart).day.toString()) to total
    }
