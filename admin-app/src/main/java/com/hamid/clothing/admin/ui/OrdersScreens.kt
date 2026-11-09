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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.hamid.clothing.admin.InvoiceRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.CatalogSearch
import com.hamid.clothing.core.Channel
import com.hamid.clothing.core.DraftLine
import com.hamid.clothing.core.FaLabels
import com.hamid.clothing.core.InvoiceText
import com.hamid.clothing.core.OrderBuilder
import com.hamid.clothing.core.OrderStateMachine
import com.hamid.clothing.core.OrderStatus
import com.hamid.clothing.core.PaymentMethod
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.core.ShippingMethod
import com.hamid.clothing.core.TextNorm

enum class OrderFilter { ALL, ACTION, UNPAID, PREPARING, SHIPPED, DONE, CLOSED }

private fun OrderFilter.title(): String = when (this) {
    OrderFilter.ALL -> "همه"
    OrderFilter.ACTION -> "نیازمند اقدام"
    OrderFilter.UNPAID -> "در انتظار پرداخت"
    OrderFilter.PREPARING -> "آماده‌سازی"
    OrderFilter.SHIPPED -> "ارسال‌شده"
    OrderFilter.DONE -> "تحویل‌شده"
    OrderFilter.CLOSED -> "لغو / مرجوع"
}

fun OrderFilter.matches(status: OrderStatus): Boolean = when (this) {
    OrderFilter.ALL -> true
    OrderFilter.ACTION -> status == OrderStatus.AWAITING_SELLER_APPROVAL || status == OrderStatus.PAID
    OrderFilter.UNPAID -> status == OrderStatus.AWAITING_PAYMENT
    OrderFilter.PREPARING -> status == OrderStatus.PREPARING
    OrderFilter.SHIPPED -> status == OrderStatus.SHIPPED
    OrderFilter.DONE -> status == OrderStatus.DELIVERED
    OrderFilter.CLOSED -> status == OrderStatus.CANCELLED || status == OrderStatus.RETURNED
}

@Composable
fun OrdersScreen(
    store: AdminStore,
    padding: PaddingValues,
    initialFilter: OrderFilter,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember(initialFilter) { mutableStateOf(initialFilter) }
    val q = TextNorm.normalize(query)
    val visible = store.orders.reversed().filter { o ->
        filter.matches(o.status) && (
            q.isEmpty() ||
                TextNorm.normalize(o.number).contains(q) ||
                TextNorm.normalize(partyName(store, o.customerId)).contains(q)
            )
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader("سفارش‌ها")
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("شماره سفارش یا نام مشتری") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(OrderFilter.values().toList(), key = { it.name }) { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.title()) })
                }
            }
            if (visible.isEmpty()) EmptyState(if (store.orders.isEmpty()) "هنوز سفارشی ثبت نشده است." else "سفارشی با این شرایط پیدا نشد.")
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { o ->
                    AdminCard(onClick = { onOpen(o.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(o.number, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            StatusText(FaLabels.status(o.status), orderStatusColor(o.status))
                        }
                        Text(
                            "${partyName(store, o.customerId)} · ${if (o.channel == Channel.WHOLESALE) "عمده" else "خرده"} · ${faDate(o.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                        Text(
                            "مبلغ: ${formatToman(o.total)}" + if (o.remaining > 0 && o.status != OrderStatus.CANCELLED) " · مانده: ${formatToman(o.remaining)}" else "",
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
            text = { Text("سفارش جدید") },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }
}

/**
 * Adds one line. Order mode picks an existing color/size and may set a special price;
 * purchase mode accepts any color/size and needs the purchase price.
 */
@Composable
fun LineDialog(
    store: AdminStore,
    purchaseMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (productId: String, color: String, size: String, quantity: Int, price: Long?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var productId by remember { mutableStateOf<String?>(null) }
    val product = store.products.firstOrNull { it.id == productId }
    var color by remember(productId) { mutableStateOf(product?.variants?.firstOrNull()?.color ?: "") }
    var size by remember(productId, color) {
        mutableStateOf(product?.variants?.firstOrNull { it.color == color }?.size ?: "")
    }
    var qtyText by remember(productId) { mutableStateOf("1") }
    var priceText by remember(productId) { mutableStateOf(if (purchaseMode) (product?.landedCost?.toString() ?: "") else "") }
    var error by remember(productId) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product?.name ?: "انتخاب کالا") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (product == null) {
                    AdminTextField("جست‌وجوی نام، کد یا بارکد", query, { query = it })
                    val matches = CatalogSearch.products(store.products, query, null, false).take(8)
                    if (matches.isEmpty()) Text("کالایی پیدا نشد", color = Muted)
                    for (p in matches) {
                        TextButton(onClick = { productId = p.id }, modifier = Modifier.fillMaxWidth()) {
                            Text("${p.name} (${p.code})", modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    if (purchaseMode) {
                        AdminTextField("رنگ", color, { color = it })
                        AdminTextField("سایز", size, { size = it })
                    } else {
                        Text("رنگ", style = MaterialTheme.typography.bodySmall, color = Muted)
                        ChoiceChips(
                            options = product.variants.map { it.color }.distinct().map { it to it },
                            selected = color,
                            onSelect = { color = it }
                        )
                        Text("سایز", style = MaterialTheme.typography.bodySmall, color = Muted)
                        ChoiceChips(
                            options = product.variants.filter { it.color == color }.map { v ->
                                v.size to "${v.size} (${faInt(store.availableFor(product.id, color, v.size))})"
                            },
                            selected = size,
                            onSelect = { size = it }
                        )
                    }
                    IntField("تعداد", qtyText, { qtyText = it }, Modifier.fillMaxWidth())
                    MoneyField(
                        if (purchaseMode) "قیمت خرید هر عدد" else "قیمت دلخواه (اختیاری؛ خالی = قیمت خودکار)",
                        priceText,
                        { priceText = it }
                    )
                    if (error != null) Text(error ?: "", color = Danger)
                }
            }
        },
        confirmButton = {
            if (product != null) {
                TextButton(onClick = {
                    val qty = parseInt(qtyText)
                    val price = parseMoney(priceText).takeIf { it > 0 }
                    if (qty <= 0) {
                        error = "تعداد باید بیشتر از صفر باشد"
                    } else if (color.isBlank() || size.isBlank()) {
                        error = "رنگ و سایز را انتخاب کنید"
                    } else if (purchaseMode && price == null) {
                        error = "قیمت خرید را وارد کنید"
                    } else {
                        onConfirm(product.id, color.trim(), size.trim(), qty, price)
                    }
                }) { Text("افزودن") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun OrderFormScreen(
    store: AdminStore,
    padding: PaddingValues,
    onDone: (String) -> Unit,
    onBack: () -> Unit
) {
    var customerId by remember { mutableStateOf<String?>(null) }
    val lines = remember { mutableStateListOf<DraftLine>() }
    var shipping by remember { mutableStateOf(ShippingMethod.COURIER) }
    var payment by remember { mutableStateOf(PaymentMethod.CARD_TO_CARD) }
    var shippingText by remember { mutableStateOf("") }
    var discountText by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var pickCustomer by remember { mutableStateOf(false) }
    var addLine by remember { mutableStateOf(false) }

    val customer = store.customers.firstOrNull { it.id == customerId }
    val priced = OrderBuilder.priceLines(customer, lines.toList(), store.products, emptyList(), System.currentTimeMillis())
    val subtotal = priced.sumOf { it.lineTotal }
    val shippingCost = parseMoney(shippingText)
    val discount = parseMoney(discountText)
    val total = (subtotal - discount + shippingCost).coerceAtLeast(0L)

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("سفارش جدید", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ErrorBox(errors)

            SectionLabel("مشتری")
            AdminCard(onClick = { pickCustomer = true }) {
                Text(customer?.fullName ?: "انتخاب مشتری…", fontWeight = FontWeight.Bold, color = if (customer == null) Gold else MaterialTheme.colorScheme.onSurface)
                if (customer != null) {
                    Text(
                        "${customer.type.label()} · ${PersianFormat.digits(customer.phone)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }

            SectionLabel("کالاها")
            for ((index, line) in lines.withIndex()) {
                val item = priced.getOrNull(index)
                AdminCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            val productName = store.products.firstOrNull { it.id == line.productId }?.name ?: "کالا"
                            Text(productName, fontWeight = FontWeight.Bold)
                            Text("${line.color} / ${line.size} · ${faInt(line.quantity)} عدد", style = MaterialTheme.typography.bodySmall, color = Muted)
                            if (item != null) {
                                Text(
                                    "${faInt(line.quantity)} × ${PersianFormat.money(item.unitPrice)} = ${formatToman(item.lineTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gold
                                )
                            }
                        }
                        IconButton(onClick = { lines.removeAt(index) }) { Icon(Icons.Default.Close, contentDescription = "حذف") }
                    }
                }
            }
            OutlinedButton(onClick = { addLine = true }, modifier = Modifier.fillMaxWidth()) { Text("+ افزودن کالا") }

            SectionLabel("ارسال و پرداخت")
            ChoiceChips(options = ShippingMethod.values().map { it to FaLabels.shipping(it) }, selected = shipping, onSelect = { shipping = it })
            MoneyField("هزینه ارسال", shippingText, { shippingText = it })
            ChoiceChips(options = PaymentMethod.values().map { it to FaLabels.payment(it) }, selected = payment, onSelect = { payment = it })
            MoneyField("تخفیف روی کل سفارش", discountText, { discountText = it })

            SectionLabel("تحویل‌گیرنده")
            AdminTextField("نام", name, { name = it })
            AdminTextField("شماره تماس", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            if (shipping != ShippingMethod.PICKUP) AdminTextField("آدرس", address, { address = it }, singleLine = false)

            AdminCard {
                Text("جمع کالاها: ${formatToman(subtotal)}", style = MaterialTheme.typography.bodyMedium)
                if (discount > 0) Text("تخفیف: ${formatToman(discount)}", style = MaterialTheme.typography.bodyMedium)
                if (shippingCost > 0) Text("ارسال: ${formatToman(shippingCost)}", style = MaterialTheme.typography.bodyMedium)
                Text("مبلغ کل: ${formatToman(total)}", fontWeight = FontWeight.Bold, color = Gold, modifier = Modifier.padding(top = 4.dp))
            }

            Button(
                onClick = {
                    val result = store.createOrder(
                        customerId, lines.toList(), shipping, shippingCost, payment, discount, name, phone, address
                    )
                    val order = result.order
                    if (order == null) errors = result.errors else onDone(order.id)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ثبت سفارش") }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (pickCustomer) {
        PickerDialog(
            title = "انتخاب مشتری",
            options = store.customers.map { it.id to "${it.fullName} — ${PersianFormat.digits(it.phone)}" },
            onPick = { id ->
                customerId = id
                val c = store.customers.firstOrNull { it.id == id }
                if (c != null) {
                    name = c.fullName
                    phone = c.phone
                    address = c.address
                }
                pickCustomer = false
            },
            onDismiss = { pickCustomer = false }
        )
    }
    if (addLine) {
        LineDialog(
            store = store,
            purchaseMode = false,
            onDismiss = { addLine = false },
            onConfirm = { productId, color, size, quantity, price ->
                lines.add(DraftLine(productId, color, size, quantity, price))
                addLine = false
            }
        )
    }
}

@Composable
fun OrderDetailScreen(store: AdminStore, padding: PaddingValues, orderId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val order = store.order(orderId)
    if (order == null) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader("سفارش", onBack = onBack)
            EmptyState("سفارش پیدا نشد.")
        }
        return
    }
    var payDialog by remember { mutableStateOf(false) }
    var shipDialog by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var confirmReturn by remember { mutableStateOf(false) }

    fun run(error: String?, success: String) {
        toast(context, error ?: success)
    }

    val allowed = OrderStateMachine.allowed(order.channel, order.status)
    val customerName = partyName(store, order.customerId)

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("سفارش ${order.number}", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(customerName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    StatusText(FaLabels.status(order.status), orderStatusColor(order.status))
                }
                InfoRow("تاریخ", faDate(order.createdAt))
                InfoRow("نوع", if (order.channel == Channel.WHOLESALE) "عمده" else "خرده")
                InfoRow("پرداخت", FaLabels.payment(order.paymentMethod))
                InfoRow("ارسال", FaLabels.shipping(order.shippingMethod))
                if (order.receiverName.isNotBlank()) InfoRow("تحویل‌گیرنده", order.receiverName)
                if (order.receiverPhone.isNotBlank()) InfoRow("تماس", PersianFormat.digits(order.receiverPhone))
                if (order.address.isNotBlank()) InfoRow("آدرس", order.address)
                if (!order.trackingCode.isNullOrBlank()) InfoRow("کد رهگیری", order.trackingCode ?: "")
                if (order.note.isNotBlank()) InfoRow("یادداشت", order.note)
            }

            AdminCard {
                SectionLabel("اقلام")
                for (item in order.items) {
                    Text(item.productName, fontWeight = FontWeight.Bold)
                    Text(
                        "${item.color} / ${item.size} · ${faInt(item.quantity)} × ${PersianFormat.money(item.unitPrice)} = ${formatToman(item.lineTotal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            AdminCard {
                InfoRow("جمع کالاها", formatToman(order.subtotal))
                if (order.extraDiscount > 0) InfoRow("تخفیف", formatToman(order.extraDiscount))
                if (order.shippingCost > 0) InfoRow("ارسال", formatToman(order.shippingCost))
                InfoRow("مبلغ کل", formatToman(order.total))
                InfoRow("پرداخت‌شده", formatToman(order.paidAmount))
                InfoRow("مانده", formatToman(order.remaining))
                InfoRow("سود ناخالص", formatToman(order.total - order.costOfGoods))
            }

            SectionLabel("اقدام‌ها")
            if (order.status == OrderStatus.AWAITING_SELLER_APPROVAL) {
                Button(onClick = { run(store.approveOrder(order.id), "سفارش تأیید شد") }, modifier = Modifier.fillMaxWidth()) { Text("تأیید سفارش عمده") }
            }
            if (order.status == OrderStatus.AWAITING_PAYMENT) {
                Button(onClick = { payDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("ثبت پرداخت") }
            }
            if (order.status == OrderStatus.PAID) {
                Button(onClick = { run(store.startPreparing(order.id), "آماده‌سازی شروع شد") }, modifier = Modifier.fillMaxWidth()) { Text("شروع آماده‌سازی") }
            }
            if (order.status == OrderStatus.PREPARING) {
                if (order.shippingMethod == ShippingMethod.PICKUP) {
                    Button(onClick = { run(store.deliverOrder(order.id), "تحویل ثبت شد") }, modifier = Modifier.fillMaxWidth()) { Text("تحویل حضوری انجام شد") }
                } else {
                    Button(onClick = { shipDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("ثبت ارسال و کد رهگیری") }
                }
            }
            if (order.status == OrderStatus.SHIPPED) {
                Button(onClick = { run(store.deliverOrder(order.id), "تحویل ثبت شد") }, modifier = Modifier.fillMaxWidth()) { Text("تحویل مشتری شد") }
            }
            if (OrderStatus.RETURNED in allowed) {
                OutlinedButton(onClick = { confirmReturn = true }, modifier = Modifier.fillMaxWidth()) { Text("ثبت مرجوعی", color = Danger) }
            }
            if (OrderStatus.CANCELLED in allowed) {
                OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth()) { Text("لغو سفارش", color = Danger) }
            }
            Button(
                onClick = {
                    scope.launch {
                        val model = store.invoiceModel(order.id)
                        if (model == null) {
                            toast(context, "فاکتور ساخته نشد")
                        } else {
                            val file = try {
                                withContext(Dispatchers.Default) { InvoiceRenderer.renderToFile(context, model) }
                            } catch (e: Exception) {
                                null
                            }
                            if (file == null) toast(context, "ساخت تصویر فاکتور ناموفق بود")
                            else shareFile(context, file, "image/png", "ارسال فاکتور")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("فاکتور تصویری (ارسال برای مشتری)") }
            if (order.externalRef.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        val file = store.statusFileFor(order.id)
                        if (file == null) toast(context, "فایل وضعیت ساخته نشد")
                        else sendFile(context, file, "application/octet-stream", CUSTOMER_PACKAGE, "ارسال وضعیت به مشتری")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ارسال وضعیت سفارش به مشتری") }
            }
            OutlinedButton(
                onClick = {
                    shareText(context, InvoiceText.build(order, customerName, store.settings.shopName.ifBlank { "فروشگاه" }))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("فاکتور به‌صورت متن") }

            if (order.history.isNotEmpty()) {
                SectionLabel("روند سفارش")
                for (h in order.history) {
                    Text(
                        "${faDate(h.at)} — ${FaLabels.status(h.to)}" + if (h.note.isNotBlank()) " (${h.note})" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (payDialog) {
        var amountText by remember { mutableStateOf(order.remaining.toString()) }
        AlertDialog(
            onDismissRequest = { payDialog = false },
            title = { Text("ثبت پرداخت") },
            text = { MoneyField("مبلغ پرداختی", amountText, { amountText = it }) },
            confirmButton = {
                TextButton(onClick = {
                    run(store.recordOrderPayment(order.id, parseMoney(amountText)), "پرداخت ثبت شد")
                    payDialog = false
                }) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { payDialog = false }) { Text("انصراف") } }
        )
    }
    if (shipDialog) {
        var tracking by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { shipDialog = false },
            title = { Text("ثبت ارسال") },
            text = { AdminTextField("کد رهگیری (اختیاری)", tracking, { tracking = it }) },
            confirmButton = {
                TextButton(onClick = {
                    run(store.shipOrder(order.id, tracking), "ارسال ثبت شد")
                    shipDialog = false
                }) { Text("ثبت ارسال") }
            },
            dismissButton = { TextButton(onClick = { shipDialog = false }) { Text("انصراف") } }
        )
    }
    if (confirmCancel) {
        ConfirmDialog(
            title = "لغو سفارش",
            text = "سفارش ${order.number} لغو شود؟ موجودی رزروشده آزاد می‌شود." +
                if (order.paidAmount > 0) " توجه: بازپرداخت مبلغ پرداخت‌شده به‌صورت دستی انجام می‌شود." else "",
            confirmLabel = "لغو شود",
            onConfirm = { confirmCancel = false; run(store.cancelOrder(order.id), "سفارش لغو شد") },
            onDismiss = { confirmCancel = false }
        )
    }
    if (confirmReturn) {
        ConfirmDialog(
            title = "مرجوعی",
            text = "کالاهای سفارش ${order.number} به انبار برگردانده می‌شوند. بازپرداخت مبلغ به‌صورت دستی انجام می‌شود.",
            confirmLabel = "ثبت مرجوعی",
            onConfirm = { confirmReturn = false; run(store.returnOrder(order.id), "مرجوعی ثبت شد") },
            onDismiss = { confirmReturn = false }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.35f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.65f))
    }
}
