@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.CreditPolicy
import com.hamid.clothing.core.CustomerType
import com.hamid.clothing.core.EntryType
import com.hamid.clothing.core.PartyLedger
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.core.WholesaleStatus

@Composable
fun CustomerDetailScreen(
    store: AdminStore,
    padding: PaddingValues,
    customerId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val customer = store.customers.firstOrNull { it.id == customerId }
    if (customer == null) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader("مشتری", onBack = onBack)
            EmptyState("مشتری پیدا نشد.")
        }
        return
    }

    val book = PartyLedger(customer.id)
    for (e in store.ledger.filter { it.partyId == customer.id }) book.add(e)
    val balance = book.balance
    val rows = book.statement().reversed()

    var entryType by remember { mutableStateOf<EntryType?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader(
            title = customer.fullName,
            onBack = onBack,
            actions = {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "ویرایش") }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminCard {
                Text("مانده حساب", style = MaterialTheme.typography.bodySmall, color = Muted)
                val (text, color) = when {
                    balance > 0 -> "بدهکار: ${formatToman(balance)}" to Danger
                    balance < 0 -> "بستانکار: ${formatToman(-balance)}" to Success
                    else -> "تسویه" to Gold
                }
                Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    Button(onClick = { entryType = EntryType.PAYMENT }, modifier = Modifier.weight(1f)) { Text("ثبت پرداخت") }
                    OutlinedButton(onClick = { entryType = EntryType.PURCHASE }, modifier = Modifier.weight(1f)) { Text("ثبت خرید/بدهی") }
                }
            }

            if (customer.wholesaleStatus == WholesaleStatus.PENDING) {
                AdminCard {
                    Text("درخواست عضویت عمده", fontWeight = FontWeight.Bold, color = Gold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(
                            onClick = {
                                store.saveCustomer(
                                    customer.copy(type = CustomerType.WHOLESALE, wholesaleStatus = WholesaleStatus.APPROVED)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("تأیید") }
                        OutlinedButton(
                            onClick = { store.saveCustomer(customer.copy(wholesaleStatus = WholesaleStatus.REJECTED)) },
                            modifier = Modifier.weight(1f)
                        ) { Text("رد") }
                    }
                }
            }

            AdminCard {
                InfoLine("موبایل", PersianFormat.digits(customer.phone))
                if (customer.address.isNotBlank()) InfoLine("آدرس", customer.address)
                InfoLine("نوع", if (customer.blocked) "${customer.type.label()} (مسدود)" else customer.type.label())
                if (customer.type == CustomerType.WHOLESALE) {
                    InfoLine("عضویت عمده", customer.wholesaleStatus.label())
                    InfoLine("خرید دفتری", customer.creditPolicy.label())
                    if (customer.creditPolicy != CreditPolicy.NO_CREDIT && customer.creditLimit > 0) {
                        InfoLine("سقف اعتبار", formatToman(customer.creditLimit))
                    }
                    if (customer.settlementDays > 0) InfoLine("مهلت تسویه", "${faInt(customer.settlementDays)} روز")
                }
                if (customer.groups.isNotEmpty()) InfoLine("گروه‌ها", customer.groups.joinToString("، "))
                InfoLine("تاریخ عضویت", faDate(customer.joinedAt))
                if (customer.adminNote.isNotBlank()) InfoLine("یادداشت", customer.adminNote)
            }

            SectionLabel("گردش حساب")
            if (rows.isEmpty()) {
                Text("هنوز تراکنشی ثبت نشده است.", color = Muted)
            }
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
                    Text(
                        "مانده پس از تراکنش: ${formatToman(row.balanceAfter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }

            if (deleteError != null) Text(deleteError ?: "", color = Danger)
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("حذف مشتری", color = Danger)
            }
            Column(modifier = Modifier.padding(bottom = 24.dp)) {}
        }
    }

    val pendingType = entryType
    if (pendingType != null) {
        var amountText by remember(pendingType) { mutableStateOf("") }
        var noteText by remember(pendingType) { mutableStateOf("") }
        var error by remember(pendingType) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { entryType = null },
            title = { Text(if (pendingType == EntryType.PAYMENT) "ثبت پرداخت مشتری" else "ثبت خرید / بدهی") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoneyField("مبلغ", amountText, { amountText = it })
                    AdminTextField("توضیح (اختیاری)", noteText, { noteText = it })
                    if (error != null) Text(error ?: "", color = Danger)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val result = store.addLedgerEntry(customer.id, pendingType, parseMoney(amountText), noteText)
                    if (result == null) entryType = null else error = result
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { entryType = null }) { Text("انصراف") } }
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "حذف مشتری",
            text = "«${customer.fullName}» حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = {
                confirmDelete = false
                val result = store.deleteCustomer(customer.id)
                if (result == null) onDeleted() else deleteError = result
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.35f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.65f))
    }
}
