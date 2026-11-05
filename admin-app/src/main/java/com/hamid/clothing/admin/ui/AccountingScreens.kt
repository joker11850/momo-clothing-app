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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.hamid.clothing.core.Cheque
import com.hamid.clothing.core.ChequeDirection
import com.hamid.clothing.core.ChequeService
import com.hamid.clothing.core.ChequeStatus
import com.hamid.clothing.core.EntryType
import com.hamid.clothing.core.Expense
import com.hamid.clothing.core.PartyLedger
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.core.Periods
import com.hamid.clothing.core.PurchaseLine
import com.hamid.clothing.core.PurchaseMath
import com.hamid.clothing.core.Supplier

@Composable
fun AccountingHubScreen(store: AdminStore, padding: PaddingValues, go: (Screen) -> Unit) {
    val now = System.currentTimeMillis()
    val receivedOpen = ChequeService.pendingTotal(store.cheques, ChequeDirection.RECEIVED)
    val paidOpen = ChequeService.pendingTotal(store.cheques, ChequeDirection.PAID)
    val dueSoon = store.cheques.count { it.isDueSoon(now, 7) }
    val payable = store.suppliers.sumOf { (store.balanceOf(it.id)).coerceAtLeast(0L) }
    val (monthStart, monthEnd) = Periods.month(now)
    val monthExpenses = store.expenses.filter { it.at >= monthStart && it.at < monthEnd && !it.isPersonal }.sumOf { it.amount }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("حسابداری", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("چک‌های دریافتی باز", formatToman(receivedOpen), Modifier.weight(1f))
            StatCard("چک‌های پرداختی باز", formatToman(paidOpen), Modifier.weight(1f), valueColor = if (paidOpen > 0) Danger else Gold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("بدهی به تأمین‌کنندگان", formatToman(payable), Modifier.weight(1f), valueColor = if (payable > 0) Danger else Gold)
            StatCard("هزینه‌های کاری این ماه", formatToman(monthExpenses), Modifier.weight(1f))
        }
        if (dueSoon > 0) {
            AdminCard(onClick = { go(Screen.Cheques(ChequeDirection.RECEIVED)) }) {
                Text("${faInt(dueSoon)} چک تا ۷ روز آینده سررسید دارد یا سررسیدش گذشته", color = Danger, fontWeight = FontWeight.Bold)
            }
        }
        HubCard("چک‌ها", "دریافتی و پرداختی، وصول و برگشت") { go(Screen.Cheques(ChequeDirection.RECEIVED)) }
        HubCard("تأمین‌کنندگان", "خرید کالا، پرداخت و مانده حساب") { go(Screen.Suppliers) }
        HubCard("هزینه‌ها", "هزینه‌های کاری و شخصی") { go(Screen.Expenses) }
        HubCard("حساب‌های بانکی", "کارت و شبای فروشگاه") { go(Screen.BankAccounts) }
        HubCard("گزارش‌ها", "فروش، سود، پرفروش‌ها و بدهکاران") { go(Screen.Reports) }
        HubCard("تنظیمات", "مشخصات فروشگاه و مدت رزرو موجودی") { go(Screen.Settings) }
    }
}

@Composable
private fun HubCard(title: String, subtitle: String, onClick: () -> Unit) {
    AdminCard(onClick = onClick) {
        Text(title, fontWeight = FontWeight.Bold, color = Gold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

// ---------------------------------------------------------------- cheques

private enum class ChequeView { OPEN, CLEARED, BOUNCED, ALL }

@Composable
fun ChequesScreen(
    store: AdminStore,
    padding: PaddingValues,
    initialDirection: ChequeDirection,
    onAdd: (ChequeDirection) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var direction by remember(initialDirection) { mutableStateOf(initialDirection) }
    var view by remember { mutableStateOf(ChequeView.OPEN) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    val now = System.currentTimeMillis()

    val visible = store.cheques
        .filter { it.direction == direction }
        .filter {
            when (view) {
                ChequeView.OPEN -> it.isOpen
                ChequeView.CLEARED -> it.status == ChequeStatus.CLEARED
                ChequeView.BOUNCED -> it.status == ChequeStatus.BOUNCED
                ChequeView.ALL -> true
            }
        }
        .sortedBy { it.dueDate }
    val openTotal = ChequeService.pendingTotal(store.cheques, direction)

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader("چک‌ها", onBack = onBack)
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChips(
                    options = listOf(ChequeDirection.RECEIVED to "دریافتی", ChequeDirection.PAID to "پرداختی"),
                    selected = direction,
                    onSelect = { direction = it }
                )
                ChoiceChips(
                    options = listOf(ChequeView.OPEN to "باز", ChequeView.CLEARED to "وصول‌شده", ChequeView.BOUNCED to "برگشتی", ChequeView.ALL to "همه"),
                    selected = view,
                    onSelect = { view = it }
                )
                Text("جمع چک‌های باز: ${formatToman(openTotal)}", color = Gold, fontWeight = FontWeight.Bold)
            }
            if (visible.isEmpty()) EmptyState("چکی در این بخش نیست.")
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { c ->
                    ChequeCard(store, c, now,
                        onDeposit = { toast(context, store.depositCheque(c.id) ?: "ثبت شد") },
                        onClear = { toast(context, store.clearCheque(c.id) ?: "چک وصول شد") },
                        onBounce = { toast(context, store.bounceCheque(c.id) ?: "چک برگشتی ثبت شد") },
                        onDelete = { confirmDeleteId = c.id }
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onAdd(direction) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("چک جدید") },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }

    val deleteId = confirmDeleteId
    if (deleteId != null) {
        ConfirmDialog(
            title = "حذف چک",
            text = "این چک حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = { confirmDeleteId = null; toast(context, store.deleteCheque(deleteId) ?: "حذف شد") },
            onDismiss = { confirmDeleteId = null }
        )
    }
}

@Composable
private fun ChequeCard(
    store: AdminStore,
    c: Cheque,
    now: Long,
    onDeposit: () -> Unit,
    onClear: () -> Unit,
    onBounce: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = c.isOpen && c.dueDate < now
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(partyName(store, c.partyId), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            StatusText(
                c.status.label(),
                when (c.status) {
                    ChequeStatus.CLEARED -> Success
                    ChequeStatus.BOUNCED -> Danger
                    else -> Gold
                }
            )
        }
        Text(formatToman(c.amount), color = Gold, fontWeight = FontWeight.Bold)
        Text(
            "سررسید: ${faDate(c.dueDate)}" + if (overdue) " (گذشته)" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (overdue) Danger else Muted
        )
        Text(
            "صیاد: ${PersianFormat.digits(c.sayadId)}" + if (c.bankName.isNotBlank()) " · ${c.bankName}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        if (c.note.isNotBlank()) Text(c.note, style = MaterialTheme.typography.bodySmall, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            if (c.status == ChequeStatus.PENDING) TextButton(onClick = onDeposit) { Text("به صندوق") }
            if (c.isOpen) TextButton(onClick = onClear) { Text("وصول شد") }
            if (c.status != ChequeStatus.BOUNCED) TextButton(onClick = onBounce) { Text("برگشت خورد", color = Danger) }
            if (c.status == ChequeStatus.PENDING) TextButton(onClick = onDelete) { Text("حذف", color = Danger) }
        }
    }
}

@Composable
fun ChequeFormScreen(
    store: AdminStore,
    padding: PaddingValues,
    direction: ChequeDirection,
    partyId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var dir by remember { mutableStateOf(direction) }
    var party by remember { mutableStateOf(partyId) }
    var amountText by remember { mutableStateOf("") }
    var sayad by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val start = remember { todayParts(30) }
    var year by remember { mutableStateOf(start.first) }
    var month by remember { mutableStateOf(start.second) }
    var day by remember { mutableStateOf(start.third) }
    var error by remember { mutableStateOf<String?>(null) }
    var pick by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("چک جدید", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (error != null) ErrorBox(listOf(error ?: ""))
            ChoiceChips(
                options = listOf(ChequeDirection.RECEIVED to "دریافتی (از مشتری)", ChequeDirection.PAID to "پرداختی (به تأمین‌کننده)"),
                selected = dir,
                onSelect = { dir = it; party = null }
            )
            AdminCard(onClick = { pick = true }) {
                Text(
                    if (party == null) (if (dir == ChequeDirection.RECEIVED) "انتخاب مشتری…" else "انتخاب تأمین‌کننده…") else partyName(store, party ?: ""),
                    fontWeight = FontWeight.Bold,
                    color = if (party == null) Gold else MaterialTheme.colorScheme.onSurface
                )
            }
            MoneyField("مبلغ چک", amountText, { amountText = it })
            AdminTextField("شناسه‌ی صیاد (۱۶ رقم)", sayad, { sayad = it }, keyboardType = KeyboardType.Number)
            AdminTextField("بانک (اختیاری)", bank, { bank = it })
            JalaliDateField("تاریخ سررسید", year, month, day) { y, m, d -> year = y; month = m; day = d }
            AdminTextField("توضیح (اختیاری)", note, { note = it })
            Button(
                onClick = {
                    val due = jalaliToMillis(year, month, day)
                    val chosen = party
                    error = if (chosen == null) {
                        "طرف حساب را انتخاب کنید"
                    } else if (due == null) {
                        "تاریخ سررسید معتبر نیست"
                    } else {
                        store.addCheque(dir, chosen, parseMoney(amountText), sayad, due, bank, note)
                    }
                    if (error == null) onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ثبت چک") }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (pick) {
        PickerDialog(
            title = if (dir == ChequeDirection.RECEIVED) "انتخاب مشتری" else "انتخاب تأمین‌کننده",
            options = if (dir == ChequeDirection.RECEIVED) store.customers.map { it.id to it.fullName } else store.suppliers.map { it.id to it.name },
            onPick = { party = it; pick = false },
            onDismiss = { pick = false }
        )
    }
}

// ---------------------------------------------------------------- suppliers

@Composable
fun SuppliersScreen(
    store: AdminStore,
    padding: PaddingValues,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader("تأمین‌کنندگان", onBack = onBack)
            if (store.suppliers.isEmpty()) EmptyState("هنوز تأمین‌کننده‌ای ثبت نشده است.")
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(store.suppliers, key = { it.id }) { s ->
                    val balance = store.balanceOf(s.id)
                    AdminCard(onClick = { onOpen(s.id) }) {
                        Text(s.name, fontWeight = FontWeight.Bold)
                        if (s.phone.isNotBlank()) Text(PersianFormat.digits(s.phone), style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(
                            if (balance > 0) "بدهی ما: ${formatToman(balance)}" else if (balance < 0) "طلب ما: ${formatToman(-balance)}" else "تسویه",
                            color = if (balance > 0) Danger else if (balance < 0) Success else Muted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("تأمین‌کننده جدید") },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }
}

@Composable
fun SupplierFormScreen(
    store: AdminStore,
    padding: PaddingValues,
    supplierId: String?,
    onDone: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val existing = remember(supplierId) { store.suppliers.firstOrNull { it.id == supplierId } }
    var name by remember(supplierId) { mutableStateOf(existing?.name ?: "") }
    var phone by remember(supplierId) { mutableStateOf(existing?.phone ?: "") }
    var note by remember(supplierId) { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader(if (existing == null) "تأمین‌کننده جدید" else "ویرایش تأمین‌کننده", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (error != null) ErrorBox(listOf(error ?: ""))
            AdminTextField("نام تأمین‌کننده", name, { name = it })
            AdminTextField("شماره تماس", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            AdminTextField("توضیحات", note, { note = it }, singleLine = false)
            Button(
                onClick = {
                    val id = existing?.id ?: store.newId()
                    error = store.saveSupplier(Supplier(id, name.trim(), phone.trim(), note.trim()))
                    if (error == null) onDone(id)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ذخیره") }
            if (existing != null) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("حذف تأمین‌کننده", color = Danger) }
            }
        }
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "حذف تأمین‌کننده",
            text = "«${existing.name}» حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = {
                confirmDelete = false
                val result = store.deleteSupplier(existing.id)
                if (result == null) onBack() else toast(context, result)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
fun SupplierDetailScreen(
    store: AdminStore,
    padding: PaddingValues,
    supplierId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPurchase: () -> Unit,
    onCheque: () -> Unit
) {
    val supplier = store.suppliers.firstOrNull { it.id == supplierId }
    if (supplier == null) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader("تأمین‌کننده", onBack = onBack)
            EmptyState("تأمین‌کننده پیدا نشد.")
        }
        return
    }
    val book = PartyLedger(supplier.id)
    for (e in store.ledger.filter { it.partyId == supplier.id }) book.add(e)
    val balance = book.balance
    val rows = book.statement().reversed()
    var payDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader(
            title = supplier.name,
            onBack = onBack,
            actions = { IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "ویرایش") } }
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminCard {
                Text("مانده حساب", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text(
                    if (balance > 0) "بدهی ما: ${formatToman(balance)}" else if (balance < 0) "طلب ما: ${formatToman(-balance)}" else "تسویه",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (balance > 0) Danger else if (balance < 0) Success else Gold
                )
                if (supplier.phone.isNotBlank()) Text(PersianFormat.digits(supplier.phone), color = Muted, style = MaterialTheme.typography.bodySmall)
                if (supplier.note.isNotBlank()) Text(supplier.note, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onPurchase, modifier = Modifier.fillMaxWidth()) { Text("ثبت خرید کالا") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { payDialog = true }, modifier = Modifier.weight(1f)) { Text("ثبت پرداخت") }
                OutlinedButton(onClick = onCheque, modifier = Modifier.weight(1f)) { Text("ثبت چک پرداختی") }
            }

            SectionLabel("گردش حساب")
            if (rows.isEmpty()) Text("هنوز تراکنشی ثبت نشده است.", color = Muted)
            for (row in rows) {
                AdminCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.entry.type.label(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            (if (row.entry.type.increasesBalance) "+ " else "− ") + formatToman(row.entry.amount),
                            color = if (row.entry.type.increasesBalance) Danger else Success
                        )
                    }
                    Text(
                        faDate(row.entry.at) + if (row.entry.note.isNotBlank()) " · ${row.entry.note}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    Text("مانده: ${formatToman(row.balanceAfter)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (payDialog) {
        var amountText by remember { mutableStateOf("") }
        var noteText by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { payDialog = false },
            title = { Text("ثبت پرداخت به تأمین‌کننده") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyField("مبلغ", amountText, { amountText = it })
                    AdminTextField("توضیح (اختیاری)", noteText, { noteText = it })
                    if (error != null) Text(error ?: "", color = Danger)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val result = store.addLedgerEntry(supplier.id, EntryType.PAYMENT, parseMoney(amountText), noteText)
                    if (result == null) payDialog = false else error = result
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { payDialog = false }) { Text("انصراف") } }
        )
    }
}

@Composable
fun PurchaseFormScreen(
    store: AdminStore,
    padding: PaddingValues,
    supplierId: String,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val lines = remember { mutableStateListOf<PurchaseLine>() }
    var extraText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var addLine by remember { mutableStateOf(false) }
    val extra = parseMoney(extraText)
    val goods = lines.sumOf { it.unitPrice * it.quantity }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("خرید از ${store.suppliers.firstOrNull { it.id == supplierId }?.name ?: "تأمین‌کننده"}", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ErrorBox(errors)
            for ((index, line) in lines.withIndex()) {
                val name = store.products.firstOrNull { it.id == line.productId }?.name ?: "کالا"
                AdminCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text("${line.color} / ${line.size}", style = MaterialTheme.typography.bodySmall, color = Muted)
                            Text(
                                "${faInt(line.quantity)} × ${PersianFormat.money(line.unitPrice)} = ${formatToman(line.unitPrice * line.quantity)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gold
                            )
                        }
                        IconButton(onClick = { lines.removeAt(index) }) { Icon(Icons.Default.Close, contentDescription = "حذف") }
                    }
                }
            }
            OutlinedButton(onClick = { addLine = true }, modifier = Modifier.fillMaxWidth()) { Text("+ افزودن کالا") }
            MoneyField("هزینه‌های جانبی خرید (حمل، بسته‌بندی…)", extraText, { extraText = it })
            AdminTextField("توضیح (اختیاری)", note, { note = it })
            AdminCard {
                Text("جمع کالاها: ${formatToman(goods)}")
                Text("جمع کل با هزینه‌های جانبی: ${formatToman(goods + extra)}", fontWeight = FontWeight.Bold, color = Gold)
                Text(
                    "با ثبت خرید، موجودی افزایش می‌یابد و قیمت خرید و تمام‌شده‌ی هر کالا برابر هزینه‌ی همین خرید می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Button(
                onClick = {
                    val result = store.recordPurchase(supplierId, lines.toList(), extra, note)
                    if (result.isEmpty()) onDone() else errors = result
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ثبت خرید") }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
    if (addLine) {
        LineDialog(
            store = store,
            purchaseMode = true,
            onDismiss = { addLine = false },
            onConfirm = { productId, color, size, quantity, price ->
                lines.add(PurchaseLine(productId, color, size, quantity, price ?: 0L))
                addLine = false
            }
        )
    }
}
