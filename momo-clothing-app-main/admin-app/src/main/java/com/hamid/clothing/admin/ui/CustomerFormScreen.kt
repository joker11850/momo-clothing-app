@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.CreditPolicy
import com.hamid.clothing.core.Customer
import com.hamid.clothing.core.CustomerType
import com.hamid.clothing.core.Validators
import com.hamid.clothing.core.WholesaleStatus

@Composable
fun CustomerFormScreen(
    store: AdminStore,
    padding: PaddingValues,
    customerId: String?,
    onDone: (String) -> Unit,
    onBack: () -> Unit
) {
    val existing = remember(customerId) { store.customers.firstOrNull { it.id == customerId } }

    var name by remember(customerId) { mutableStateOf(existing?.fullName ?: "") }
    var phone by remember(customerId) { mutableStateOf(existing?.phone ?: "") }
    var address by remember(customerId) { mutableStateOf(existing?.address ?: "") }
    var type by remember(customerId) { mutableStateOf(existing?.type ?: CustomerType.RETAIL) }
    var status by remember(customerId) { mutableStateOf(existing?.wholesaleStatus ?: WholesaleStatus.NONE) }
    var policy by remember(customerId) { mutableStateOf(existing?.creditPolicy ?: CreditPolicy.NO_CREDIT) }
    var limitText by remember(customerId) {
        mutableStateOf(if ((existing?.creditLimit ?: 0L) > 0L) existing?.creditLimit.toString() else "")
    }
    var daysText by remember(customerId) { mutableStateOf((existing?.settlementDays ?: 0).toString()) }
    var blocked by remember(customerId) { mutableStateOf(existing?.blocked ?: false) }
    var groupsText by remember(customerId) { mutableStateOf(existing?.groups?.joinToString("، ") ?: "") }
    var note by remember(customerId) { mutableStateOf(existing?.adminNote ?: "") }
    var errors by remember(customerId) { mutableStateOf<List<String>>(emptyList()) }

    fun save() {
        val isWholesale = type == CustomerType.WHOLESALE
        val id = existing?.id ?: store.newId()
        val customer = Customer(
            id = id,
            fullName = name.trim(),
            phone = Validators.normalizeMobile(phone) ?: phone.trim(),
            address = address.trim(),
            type = type,
            wholesaleStatus = if (isWholesale) (if (status == WholesaleStatus.NONE) WholesaleStatus.APPROVED else status) else WholesaleStatus.NONE,
            joinedAt = existing?.joinedAt ?: System.currentTimeMillis(),
            creditPolicy = if (isWholesale) policy else CreditPolicy.NO_CREDIT,
            creditLimit = if (isWholesale && policy != CreditPolicy.NO_CREDIT) parseMoney(limitText) else 0L,
            settlementDays = parseInt(daysText),
            blocked = blocked,
            groups = groupsText.split(',', '،').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            adminNote = note.trim()
        )
        val result = store.saveCustomer(customer)
        if (result.isEmpty()) onDone(id) else errors = result
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader(
            title = if (existing == null) "مشتری جدید" else "ویرایش مشتری",
            onBack = onBack,
            actions = { TextButton(onClick = { save() }) { Text("ذخیره") } }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ErrorBox(errors)

            SectionLabel("مشخصات")
            AdminTextField("نام و نام خانوادگی", name, { name = it })
            AdminTextField("شماره موبایل", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            AdminTextField("آدرس", address, { address = it }, singleLine = false)

            SectionLabel("نوع مشتری")
            ChoiceChips(
                options = listOf(CustomerType.RETAIL to "خرده", CustomerType.WHOLESALE to "عمده"),
                selected = type,
                onSelect = { type = it }
            )

            if (type == CustomerType.WHOLESALE) {
                SectionLabel("وضعیت عضویت عمده")
                ChoiceChips(
                    options = listOf(
                        WholesaleStatus.APPROVED to "تأییدشده",
                        WholesaleStatus.PENDING to "در انتظار تأیید",
                        WholesaleStatus.REJECTED to "ردشده"
                    ),
                    selected = if (status == WholesaleStatus.NONE) WholesaleStatus.APPROVED else status,
                    onSelect = { status = it }
                )

                SectionLabel("خرید دفتری")
                ChoiceChips(
                    options = CreditPolicy.values().map { it to it.label() },
                    selected = policy,
                    onSelect = { policy = it }
                )
                if (policy != CreditPolicy.NO_CREDIT) {
                    MoneyField("سقف اعتبار", limitText, { limitText = it })
                }
                IntField("مهلت تسویه (روز)", daysText, { daysText = it }, Modifier.fillMaxWidth())
            }

            SectionLabel("سایر")
            AdminTextField("گروه‌ها (با ویرگول جدا کنید)", groupsText, { groupsText = it })
            AdminTextField("یادداشت مدیر", note, { note = it }, singleLine = false)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("مسدود", modifier = Modifier.weight(1f))
                Switch(checked = blocked, onCheckedChange = { blocked = it })
            }

            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره مشتری") }
            Column(modifier = Modifier.padding(bottom = 24.dp)) {}
        }
    }
}
