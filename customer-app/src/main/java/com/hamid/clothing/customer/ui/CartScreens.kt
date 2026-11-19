@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hamid.clothing.core.FaLabels
import com.hamid.clothing.core.InvoiceText
import com.hamid.clothing.core.PaymentMethod
import com.hamid.clothing.core.ShippingMethod
import com.hamid.clothing.customer.CustomerStore
import com.hamid.clothing.customer.StoreConfig

private const val LOCAL_NOTICE =
    "سفارش‌ها فعلاً فقط روی همین گوشی ذخیره می‌شوند. بعد از ثبت، سفارش را با دکمه‌ی «ارسال سفارش برای فروشگاه»، سفارش را به‌صورت فایل برای فروشگاه بفرستید؛ فروشنده بعد از بررسی، وضعیت را برایتان برمی‌گرداند."

@Composable
fun CartScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val context = LocalContext.current
    val priced = store.pricedCart()
    var couponText by remember { mutableStateOf(store.coupon) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("سبد خرید")
        if (priced.lines.isEmpty()) {
            EmptyState("سبد خرید شما خالی است.")
            GoldButton("رفتن به فروشگاه", { nav.tab(Screen.Shop()) }, Modifier.padding(horizontal = 32.dp).fillMaxWidth())
            return@Column
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (line in priced.lines) {
                SoftCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProductArt(store, line.product, Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)), maxPx = 240)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(line.product.name, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("${line.line.color} / ${line.line.size}", style = MaterialTheme.typography.bodySmall, color = Muted)
                            Text(formatToman(line.quote.unitPrice), color = Burgundy, fontWeight = FontWeight.ExtraBold)
                            if (!line.inStock) {
                                Text("فقط ${faInt(line.available)} عدد موجود است", color = Danger, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                QuantityStepper(
                                    quantity = line.line.quantity,
                                    onChange = { store.setCartQuantity(line.line.key(), it) },
                                    max = line.available.coerceAtLeast(1)
                                )
                                Box(modifier = Modifier.weight(1f))
                                IconButton(onClick = { store.removeFromCart(line.line.key()) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Danger)
                                }
                            }
                        }
                    }
                }
            }

            SoftCard {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("کد تخفیف", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Field("کد تخفیف (اختیاری)", couponText, { couponText = it }, Modifier.weight(1f))
                        PillButton("اعمال", {
                            val ok = store.applyCoupon(couponText)
                            toast(context, if (!ok) "کد تخفیف معتبر نیست" else if (couponText.isBlank()) "کد تخفیف حذف شد" else "کد تخفیف اعمال شد")
                        })
                    }
                }
            }

            SoftCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("تعداد کالا", faInt(priced.itemCount))
                    if (priced.savings > 0) {
                        SummaryRow("جمع بدون تخفیف", formatToman(priced.subtotal + priced.savings))
                        SummaryRow("سود شما از تخفیف", formatToman(priced.savings), MintDark)
                    }
                    SummaryRow("مبلغ کالاها", formatToman(priced.subtotal), Burgundy, bold = true)
                    Text("هزینه ارسال در مرحله‌ی بعد مشخص می‌شود.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }

            if (priced.hasStockProblem) {
                Text("موجودی بعضی کالاها کافی نیست؛ تعداد را اصلاح کنید.", color = Danger)
            }
            GoldButton(
                "ادامه و ثبت سفارش",
                onClick = { if (store.isLoggedIn) nav.go(Screen.Checkout) else nav.go(Screen.Login(Screen.Checkout)) },
                enabled = !priced.hasStockProblem,
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal)
    }
}

@Composable
fun CheckoutScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val customer = store.customer
    if (customer == null) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenTitle("ثبت سفارش", onBack = { nav.back() })
            EmptyState("برای ثبت سفارش ابتدا وارد شوید.")
            GoldButton("ورود", { nav.replace(Screen.Login(Screen.Checkout)) }, Modifier.padding(horizontal = 32.dp).fillMaxWidth())
        }
        return
    }

    val priced = store.pricedCart()
    var name by remember { mutableStateOf(customer.fullName) }
    var phone by remember { mutableStateOf(customer.phone) }
    var address by remember { mutableStateOf(store.data.addresses.firstOrNull() ?: "") }
    var shipping by remember { mutableStateOf(ShippingMethod.COURIER) }
    var payment by remember { mutableStateOf(PaymentMethod.CARD_TO_CARD) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }

    val shippingCost = store.shippingCost(shipping)
    val total = priced.subtotal + shippingCost
    val allowed = store.allowedPayments(total)

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("ثبت سفارش", onBack = { nav.back() })
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ErrorList(errors)

            Text("تحویل‌گیرنده", fontWeight = FontWeight.ExtraBold)
            Field("نام و نام خانوادگی", name, { name = it })
            Field("شماره تماس", phone, { phone = it }, keyboardType = KeyboardType.Phone)

            Text("روش ارسال", fontWeight = FontWeight.ExtraBold)
            ChoiceChips(
                options = ShippingMethod.values().map { Triple(it, FaLabels.shipping(it), true) },
                selected = shipping,
                onSelect = { shipping = it }
            )
            Text(
                if (shipping == ShippingMethod.PICKUP) "تحویل حضوری بدون هزینه‌ی ارسال است."
                else if (shippingCost > 0) "هزینه‌ی ارسال: ${formatToman(shippingCost)}"
                else "هزینه‌ی ارسال بعد از ثبت سفارش توسط فروشگاه اعلام می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )

            if (shipping != ShippingMethod.PICKUP) {
                Text("آدرس", fontWeight = FontWeight.ExtraBold)
                if (store.data.addresses.isNotEmpty()) {
                    ChoiceChips(
                        options = store.data.addresses.map { Triple(it, if (it.length > 28) it.take(28) + "…" else it, true) },
                        selected = address,
                        onSelect = { address = it }
                    )
                }
                Field("آدرس کامل", address, { address = it }, singleLine = false)
            }

            Text("روش پرداخت", fontWeight = FontWeight.ExtraBold)
            ChoiceChips(
                options = allowed.map { m ->
                    val enabled = m != PaymentMethod.ONLINE || StoreConfig.ONLINE_PAYMENT_ENABLED
                    Triple(m, FaLabels.payment(m) + if (enabled) "" else " (به‌زودی)", enabled)
                },
                selected = payment,
                onSelect = { payment = it }
            )
            if (payment == PaymentMethod.CARD_TO_CARD) {
                Text("بعد از ثبت سفارش، مبلغ را به حساب فروشگاه واریز و رسید را برای فروشگاه ارسال کنید.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (payment == PaymentMethod.ON_ACCOUNT) {
                Text("خرید دفتری بعد از تأیید فروشنده نهایی می‌شود.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }

            SoftCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("مبلغ کالاها", formatToman(priced.subtotal))
                    if (priced.savings > 0) SummaryRow("سود شما از تخفیف", formatToman(priced.savings), MintDark)
                    SummaryRow("هزینه ارسال", if (shippingCost > 0) formatToman(shippingCost) else "اعلام توسط فروشگاه")
                    SummaryRow("قابل پرداخت", formatToman(total), Burgundy, bold = true)
                }
            }

            SoftCard(color = Beige) {
                Text(LOCAL_NOTICE, style = MaterialTheme.typography.bodySmall, color = GoldInk, modifier = Modifier.padding(14.dp))
            }

            GoldButton(
                "ثبت نهایی سفارش",
                onClick = {
                    val result = store.placeOrder(name, phone, address, shipping, payment)
                    val order = result.order
                    if (order == null) errors = result.errors else nav.replace(Screen.OrderDone(order.id))
                },
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OrderDoneScreen(store: CustomerStore, nav: Nav, padding: PaddingValues, orderId: String) {
    val context = LocalContext.current
    val order = store.order(orderId)
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (order == null) {
            EmptyState("سفارش پیدا نشد.")
            GoldButton("بازگشت", { nav.tab(Screen.Home) })
            return@Column
        }
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintDark, modifier = Modifier.size(84.dp))
        Text("سفارش شما ثبت شد", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text("شماره سفارش: ${order.number}", color = Burgundy, fontWeight = FontWeight.Bold)
        Text("وضعیت: ${FaLabels.status(order.status)}", color = Muted)
        Text("مبلغ کل: ${formatToman(order.total)}", fontWeight = FontWeight.Bold)

        SoftCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (order.paymentMethod) {
                    PaymentMethod.CARD_TO_CARD -> {
                        Text("مرحله‌ی بعد: پرداخت", fontWeight = FontWeight.Bold)
                        Text("مبلغ سفارش را به یکی از حساب‌های فروشگاه واریز کنید و رسید را برای فروشگاه بفرستید.")
                        OutlinedButton(onClick = { nav.go(Screen.BankInfo) }, modifier = Modifier.fillMaxWidth()) { Text("حساب‌های فروشگاه") }
                    }
                    PaymentMethod.ON_ACCOUNT -> {
                        Text("مرحله‌ی بعد: تأیید فروشنده", fontWeight = FontWeight.Bold)
                        Text("سفارش خرید دفتری شما پس از تأیید فروشنده آماده‌سازی می‌شود.")
                    }
                    else -> Text("سفارش شما در انتظار بررسی است.")
                }
            }
        }
        SoftCard(color = Beige) {
            Text(LOCAL_NOTICE, style = MaterialTheme.typography.bodySmall, color = GoldInk, modifier = Modifier.padding(14.dp))
        }
        ReceiptWarrantyCard(order) { shareDigitalReceipt(context, order) }

        GoldButton(
            "ارسال سفارش برای فروشگاه",
            onClick = { sendOrderFile(context, store, order.id) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(onClick = { nav.replace(Screen.OrderDetail(order.id)) }, modifier = Modifier.fillMaxWidth()) { Text("مشاهده‌ی سفارش و فاکتور") }
        OutlinedButton(onClick = { nav.tab(Screen.Home) }, modifier = Modifier.fillMaxWidth()) { Text("بازگشت به خانه") }
    }
}
