@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.FaLabels
import com.hamid.clothing.core.OrderBuilder
import com.hamid.clothing.core.PaymentMethod
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.core.ShippingMethod

/** Asks the owner to confirm an order that arrived from the customer app. Prices are always the shop's own. */
@Composable
fun IncomingOrderDialog(store: AdminStore, onCreated: (String) -> Unit) {
    val file = store.pendingOrder ?: return
    val now = System.currentTimeMillis()
    val plan = remember(file, store.customers) { store.incomingPlan() } ?: return

    var shipping by remember(file) { mutableStateOf(file.shippingMethod) }
    var shippingText by remember(file) { mutableStateOf("") }
    var payment by remember(file) { mutableStateOf(file.paymentMethod) }
    var errors by remember(file) { mutableStateOf<List<String>>(emptyList()) }

    val customer = plan.customer ?: plan.newCustomer
    val items = OrderBuilder.priceLines(customer, plan.lines, store.products, emptyList(), now, store.customPricesOf(plan.customer?.id ?: ""))
    val subtotal = items.sumOf { it.lineTotal }
    val shippingCost = parseMoney(shippingText)

    AlertDialog(
        onDismissRequest = {},
        title = { Text("سفارش جدید از اپ مشتری") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (plan.customer != null) "مشتری: ${plan.customer?.fullName}" else "مشتری جدید: ${plan.newCustomer?.fullName ?: "نامشخص"}",
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
                Text(
                    PersianFormat.digits(customer?.phone ?: file.customerPhone) +
                        if (plan.customer == null && plan.newCustomer != null) " (با ثبت سفارش، مشتری خرده ساخته می‌شود)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Text("شماره سفارش مشتری: ${file.clientNumber}", style = MaterialTheme.typography.bodySmall, color = Muted)
                if (file.note.isNotBlank()) Text("توضیح مشتری: ${file.note}", style = MaterialTheme.typography.bodySmall)

                SectionLabel("اقلام (با قیمت فروشگاه)")
                for (item in items) {
                    val sent = file.lines.firstOrNull { it.productId == item.productId && it.color == item.color && it.size == item.size }
                    Text(item.productName, fontWeight = FontWeight.Bold)
                    Text(
                        "${item.color} / ${item.size} · ${faInt(item.quantity)} × ${PersianFormat.money(item.unitPrice)} = ${formatToman(item.lineTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                    if (sent != null && sent.unitPrice != item.unitPrice) {
                        Text(
                            "قیمت در اپ مشتری ${formatToman(sent.unitPrice)} بود؛ قیمت فروشگاه اعمال شد",
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger
                        )
                    }
                }
                for (w in plan.skipped) Text("• $w", style = MaterialTheme.typography.bodySmall, color = Danger)

                SectionLabel("ارسال")
                ChoiceChips(options = ShippingMethod.values().map { it to FaLabels.shipping(it) }, selected = shipping, onSelect = { shipping = it })
                MoneyField("هزینه ارسال", shippingText, { shippingText = it })
                SectionLabel("پرداخت")
                ChoiceChips(options = PaymentMethod.values().map { it to FaLabels.payment(it) }, selected = payment, onSelect = { payment = it })
                Text(
                    "مبلغ کل: ${formatToman(subtotal + shippingCost)}",
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errors.isNotEmpty()) ErrorBox(errors)
            }
        },
        confirmButton = {
            TextButton(
                enabled = plan.canImport,
                onClick = {
                    val result = store.confirmIncomingOrder(plan, shipping, shippingCost, payment)
                    val order = result.order
                    if (order == null) errors = result.errors else onCreated(order.id)
                }
            ) { Text("ثبت سفارش") }
        },
        dismissButton = { TextButton(onClick = { store.discardIncoming() }) { Text("رد کردن") } }
    )
}
