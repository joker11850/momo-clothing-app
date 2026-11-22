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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Person
import com.hamid.clothing.core.CustomerType
import com.hamid.clothing.core.FaLabels
import com.hamid.clothing.core.InvoiceText
import com.hamid.clothing.core.JalaliCalendar
import com.hamid.clothing.core.OrderStatus
import com.hamid.clothing.core.PaymentMethod
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.core.Validators
import com.hamid.clothing.core.WholesaleStatus
import com.hamid.clothing.customer.CustomerStore
import com.hamid.clothing.customer.StoreConfig

private fun faDate(millis: Long): String = PersianFormat.digits(JalaliCalendar.fromEpochMillis(millis).format())

private fun WholesaleStatus.fa(): String = when (this) {
    WholesaleStatus.NONE -> "ثبت‌نشده"
    WholesaleStatus.PENDING -> "در انتظار تأیید"
    WholesaleStatus.APPROVED -> "تأییدشده"
    WholesaleStatus.REJECTED -> "ردشده"
}

private val AccBurgundy = Color(0xFF7D1634)
private val AccBurgundyDeep = Color(0xFF5A0F26)
private val AccGold = Color(0xFFD9A441)
private val AccTitle = Color(0xFF3A0F24)
private val AccChevron = Color(0xFFB9772E)
private val AccPink = Brush.verticalGradient(listOf(Color(0xFFFFE3E8), Color(0xFFF9C9D3)))
private val AccPeach = Brush.verticalGradient(listOf(Color(0xFFFFEEDD), Color(0xFFF8D9BE)))
private val AccRowBg = Brush.horizontalGradient(listOf(Color(0xFFFBEAE2), Color(0xFFFFFBF7)))
private val AccGoldBorder = Brush.horizontalGradient(listOf(Color(0xFFE2B66A), Color(0xFFF3DDB0), Color(0xFFE2B66A)))

/** Burgundy pill header with a gold outline ("حساب من"). */
@Composable
private fun AccountHeader(title: String) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(AccBurgundyDeep, AccBurgundy)))
            .border(1.5.dp, AccGoldBorder, shape)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFFF3DDB0), modifier = Modifier.size(30.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Menu row: round icon on the right, title + subtitle, chevron on the left (RTL). */
@Composable
private fun AccountMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    pinkIcon: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(34.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clip(shape)
            .background(AccRowBg)
            .border(1.dp, AccGoldBorder, shape)
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (pinkIcon) AccPink else AccPeach)
                .border(1.dp, AccGoldBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AccBurgundy, modifier = Modifier.size(28.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AccTitle, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AccChevron, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun MenuRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    SoftCard(onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
fun AccountScreen(store: CustomerStore, nav: Nav, padding: PaddingValues, onToggleDarkMode: () -> Unit = {}) {
    var confirmLogout by remember { mutableStateOf(false) }
    val customer = store.customer
    val importCatalog = rememberCatalogImporter(store)
    val catalogText = if (store.isDemoCatalog) "نمونه — برای دریافت کاتالوگ فروشگاه بزنید" else "آخرین به‌روزرسانی: ${faDate(store.catalogDate)}"

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        AccountHeader("حساب من")
        if (customer == null) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountMenuRow(Icons.Filled.ShoppingBag, "سفارش‌های من", "مشاهده و پیگیری آخرین سفارش‌ها", true) { nav.go(Screen.Orders) }
                AccountMenuRow(Icons.Filled.Favorite, "علاقه‌مندی‌ها", "محصولات مورد علاقه شما", true) { nav.go(Screen.Favorites) }
                AccountMenuRow(Icons.Filled.Groups, "عضویت عمده", "ثبت درخواست عضویت در خرید عمده", false) { nav.go(Screen.Wholesale) }
                AccountMenuRow(Icons.Filled.AccountBalanceWallet, "حساب‌های فروشگاه", "برای پرداخت کارت به کارت", false) { nav.go(Screen.BankInfo) }
                AccountMenuRow(Icons.Filled.MenuBook, "دریافت کاتالوگ فروشگاه", if (store.isDemoCatalog) "نمونه محصولات و کاتالوگ فروشگاه" else catalogText, true) { importCatalog() }
                AccountMenuRow(Icons.Filled.Notifications, "اعلان‌های فروشگاه", "مشاهده پیام‌ها و اطلاعیه‌ها", false) { nav.go(Screen.Notifications) }
                AccountMenuRow(Icons.Filled.VerifiedUser, "سیاست حفظ حریم خصوصی", "قوانین و حریم خصوصی اپلیکیشن", true) { nav.go(Screen.Privacy) }
            }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SoftCard(color = AccBurgundy) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = CircleShape, color = Gold, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                customer.fullName.trim().take(1).ifEmpty { "؟" },
                                color = GoldInk,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Column {
                        Text(customer.fullName.ifBlank { "نام شما ثبت نشده" }, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.ExtraBold)
                        Text(PersianFormat.digits(customer.phone), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        if (customer.canSeeWholesale) Text("مشتری عمده", color = Gold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (customer.canSeeWholesale) {
                SoftCard(color = Mint) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("مانده حساب شما", style = MaterialTheme.typography.bodySmall, color = MintDark)
                        Text(
                            if (store.balance > 0) "بدهکار: ${formatToman(store.balance)}"
                            else if (store.balance < 0) "بستانکار: ${formatToman(-store.balance)}"
                            else "تسویه",
                            fontWeight = FontWeight.ExtraBold,
                            color = MintDark
                        )
                    }
                }
            }

            AccountMenuRow(Icons.Filled.ShoppingBag, "سفارش‌های من", "مشاهده و پیگیری آخرین سفارش‌ها", true) { nav.go(Screen.Orders) }
            AccountMenuRow(Icons.Filled.Favorite, "علاقه‌مندی‌ها", "محصولات مورد علاقه شما", true) { nav.go(Screen.Favorites) }
            AccountMenuRow(Icons.Filled.Person, "مشخصات و آدرس‌ها", "اطلاعات تماس و آدرس‌های شما", false) { nav.go(Screen.Profile) }
            AccountMenuRow(Icons.Filled.Groups, "عضویت عمده", customer.wholesaleStatus.fa(), false) { nav.go(Screen.Wholesale) }
            AccountMenuRow(Icons.Filled.AccountBalanceWallet, "حساب‌های فروشگاه", "برای پرداخت کارت به کارت", true) { nav.go(Screen.BankInfo) }
            AccountMenuRow(Icons.Filled.MenuBook, "دریافت کاتالوگ فروشگاه", if (store.isDemoCatalog) "نمونه محصولات و کاتالوگ فروشگاه" else catalogText, true) { importCatalog() }
            AccountMenuRow(Icons.Filled.Notifications, "اعلان‌های فروشگاه", "مشاهده پیام‌ها و اطلاعیه‌ها", false) { nav.go(Screen.Notifications) }
            AccountMenuRow(Icons.Filled.VerifiedUser, "سیاست حفظ حریم خصوصی", "قوانین و حریم خصوصی اپلیکیشن", true) { nav.go(Screen.Privacy) }
            OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) { Text("خروج از حساب", color = Danger) }
            Box(modifier = Modifier.height(24.dp))
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "خروج از حساب",
            text = "از حساب خود خارج می‌شوید. اطلاعات و سفارش‌ها روی این گوشی می‌مانند و با ورود دوباره با همین شماره در دسترس‌اند.",
            confirmLabel = "خروج",
            onConfirm = { confirmLogout = false; store.logout() },
            onDismiss = { confirmLogout = false }
        )
    }
}

@Composable
fun LoginScreen(store: CustomerStore, nav: Nav, padding: PaddingValues, next: Screen?) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("ورود و ثبت‌نام", onBack = { nav.back() })
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (step == 0) "شماره موبایل خود را وارد کنید" else "کد تأیید را وارد کنید",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            if (!StoreConfig.SMS_LIVE) {
                SoftCard(color = Beige) {
                    Text(
                        "حالت آزمایشی: سرویس پیامک هنوز متصل نیست و پیامک واقعی ارسال نمی‌شود. برای ورود، کد آزمایشی ${PersianFormat.digits(StoreConfig.TEST_CODE)} را وارد کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldInk,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            if (step == 0) {
                Field("شماره موبایل", phone, { phone = it; error = null }, keyboardType = KeyboardType.Phone)
            } else {
                Text("شماره: ${PersianFormat.digits(Validators.normalizeMobile(phone) ?: phone)}", color = Muted)
                Field("کد تأیید", code, { code = it; error = null }, keyboardType = KeyboardType.Number)
            }
            if (error != null) Text(error ?: "", color = Danger)

            if (step == 0) {
                GoldButton("دریافت کد", {
                    if (Validators.normalizeMobile(phone) == null) error = "شماره موبایل معتبر نیست" else { step = 1; error = null }
                }, Modifier.fillMaxWidth())
            } else {
                GoldButton("ورود", {
                    if (StoreConfig.SMS_LIVE) {
                        error = "تأیید پیامکی هنوز متصل نیست"
                    } else if (Validators.toLatinDigits(code.trim()) != StoreConfig.TEST_CODE) {
                        error = "کد واردشده درست نیست"
                    } else if (!store.login(phone)) {
                        error = "شماره موبایل معتبر نیست"
                    } else if (next == null || next == Screen.Account) {
                        nav.back()
                    } else {
                        nav.replace(next)
                    }
                }, Modifier.fillMaxWidth())
                OutlinedButton(onClick = { step = 0; code = ""; error = null }, modifier = Modifier.fillMaxWidth()) { Text("تغییر شماره") }
            }
        }
    }
}

@Composable
fun ProfileScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val context = LocalContext.current
    val customer = store.customer
    if (customer == null) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenTitle("مشخصات", onBack = { nav.back() })
            EmptyState("ابتدا وارد شوید.")
        }
        return
    }
    var name by remember { mutableStateOf(customer.fullName) }
    var newAddress by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("مشخصات و آدرس‌ها", onBack = { nav.back() })
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Field("نام و نام خانوادگی", name, { name = it })
            Text("شماره موبایل: ${PersianFormat.digits(customer.phone)}", color = Muted)
            GoldButton("ذخیره نام", { store.saveName(name); toast(context, "ذخیره شد") }, Modifier.fillMaxWidth())

            Text("آدرس‌های من", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
            if (store.data.addresses.isEmpty()) Text("هنوز آدرسی ثبت نشده است.", color = Muted)
            for (a in store.data.addresses) {
                SoftCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(a, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                        IconButton(onClick = { store.removeAddress(a) }) { Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Danger) }
                    }
                }
            }
            Field("آدرس جدید", newAddress, { newAddress = it }, singleLine = false)
            OutlinedButton(
                onClick = { store.addAddress(newAddress); newAddress = "" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("افزودن آدرس") }
            Box(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OrdersScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val orders = store.data.orders.reversed()
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("سفارش‌های من", onBack = { nav.back() })
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SoftCard(color = Beige) {
                Text(
                    "سفارش‌ها فعلاً فقط روی همین گوشی ذخیره می‌شوند و خودکار برای فروشگاه ارسال نمی‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldInk,
                    modifier = Modifier.padding(14.dp)
                )
            }
            if (orders.isEmpty()) EmptyState("هنوز سفارشی ثبت نکرده‌اید.")
            for (o in orders) {
                SoftCard(onClick = { nav.go(Screen.OrderDetail(o.id)) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(o.number, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Text(
                                FaLabels.status(o.status),
                                color = if (o.status == OrderStatus.CANCELLED) Danger else Burgundy,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("${faDate(o.createdAt)} · ${faInt(o.items.sumOf { it.quantity })} کالا", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(formatToman(o.total), color = Burgundy, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OrderDetailScreen(store: CustomerStore, nav: Nav, padding: PaddingValues, orderId: String) {
    val context = LocalContext.current
    val order = store.order(orderId)
    var confirmCancel by remember { mutableStateOf(false) }
    if (order == null) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenTitle("سفارش", onBack = { nav.back() })
            EmptyState("سفارش پیدا نشد.")
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("فاکتور ${order.number}", onBack = { nav.back() })
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SoftCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryRow("وضعیت", FaLabels.status(order.status), Burgundy, bold = true)
                    SummaryRow("تاریخ", faDate(order.createdAt))
                    SummaryRow("روش پرداخت", FaLabels.payment(order.paymentMethod))
                    SummaryRow("روش ارسال", FaLabels.shipping(order.shippingMethod))
                    if (order.receiverName.isNotBlank()) SummaryRow("تحویل‌گیرنده", order.receiverName)
                    if (order.receiverPhone.isNotBlank()) SummaryRow("تماس", PersianFormat.digits(order.receiverPhone))
                    if (order.address.isNotBlank()) SummaryRow("آدرس", order.address)
                    if (!order.trackingCode.isNullOrBlank()) SummaryRow("کد رهگیری", order.trackingCode ?: "")
                }
            }
            SoftCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اقلام", fontWeight = FontWeight.ExtraBold)
                    for (item in order.items) {
                        Column {
                            Text(item.productName, fontWeight = FontWeight.Bold)
                            Text(
                                "${item.color} / ${item.size} · ${faInt(item.quantity)} × ${PersianFormat.money(item.unitPrice)} = ${formatToman(item.lineTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                    }
                }
            }
            SoftCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryRow("جمع کالاها", formatToman(order.subtotal))
                    if (order.extraDiscount > 0) SummaryRow("تخفیف", formatToman(order.extraDiscount))
                    SummaryRow("هزینه ارسال", if (order.shippingCost > 0) formatToman(order.shippingCost) else "اعلام توسط فروشگاه")
                    SummaryRow("مبلغ کل", formatToman(order.total), Burgundy, bold = true)
                    SummaryRow("پرداخت‌شده", formatToman(order.paidAmount))
                    SummaryRow("مانده", formatToman(order.remaining))
                }
            }
            ReceiptWarrantyCard(order) { shareDigitalReceipt(context, order) }
            if (order.history.isNotEmpty()) {
                SoftCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("روند سفارش", fontWeight = FontWeight.ExtraBold)
                        for (h in order.history) {
                            Text("${faDate(h.at)} — ${FaLabels.status(h.to)}" + if (h.note.isNotBlank()) " (${h.note})" else "", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                }
            }

            GoldButton(
                "ارسال سفارش برای فروشگاه",
                onClick = { sendOrderFile(context, store, order.id) },
                modifier = Modifier.fillMaxWidth()
            )
            if (order.paymentMethod == PaymentMethod.CARD_TO_CARD && order.status == OrderStatus.AWAITING_PAYMENT) {
                OutlinedButton(onClick = { nav.go(Screen.BankInfo) }, modifier = Modifier.fillMaxWidth()) { Text("حساب‌های فروشگاه برای پرداخت") }
            }
            if (store.canCancel(order)) {
                OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth()) { Text("لغو سفارش", color = Danger) }
            }
            Box(modifier = Modifier.height(24.dp))
        }
    }
    if (confirmCancel) {
        ConfirmDialog(
            title = "لغو سفارش",
            text = "سفارش ${order.number} لغو شود؟",
            confirmLabel = "لغو شود",
            onConfirm = {
                confirmCancel = false
                val error = store.cancelOrder(order.id)
                toast(context, error ?: "سفارش لغو شد")
            },
            onDismiss = { confirmCancel = false }
        )
    }
}

@Composable
fun WholesaleScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val context = LocalContext.current
    val customer = store.customer
    var business by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val request = store.data.wholesaleRequest

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("عضویت عمده", onBack = { nav.back() })
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SoftCard(color = Beige) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("درخواست عضویت عمده", fontWeight = FontWeight.ExtraBold, color = GoldInk)
                    Text(
                        "برای درخواست، شماره تلفن لازم نیست. پس از بررسی فروشگاه، تأیید عضویت و قیمت عمده از سمت مدیریت انجام می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldInk
                    )
                }
            }

            if (customer?.wholesaleStatus == WholesaleStatus.APPROVED && customer.type == CustomerType.WHOLESALE) {
                Text("عضویت عمده شما تأیید شده است؛ قیمت‌های عمده برای حساب شما فعال است.", color = MintDark, fontWeight = FontWeight.Bold)
            } else if (request != null && customer?.wholesaleStatus == WholesaleStatus.PENDING) {
                SoftCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("درخواست ثبت‌شده · در انتظار بررسی", fontWeight = FontWeight.ExtraBold)
                        Text("نام فروشگاه: ${request.businessName}")
                        if (request.city.isNotBlank()) Text("شهر: ${request.city}")
                        if (request.note.isNotBlank()) Text("توضیحات: ${request.note}")
                        Text("ثبت: ${faDate(request.at)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    "در حالت بدون سرور، درخواست روی همین گوشی ذخیره می‌شود و برای رسیدن به فروشگاه باید آن را به‌صورت دستی ارسال کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                GoldButton("ارسال درخواست برای فروشگاه", {
                    shareText(context, "درخواست عضویت عمده MOMO\nنام فروشگاه: ${request.businessName}\nشهر: ${request.city}\nتوضیحات: ${request.note}\nزمان ثبت: ${faDate(request.at)}")
                }, Modifier.fillMaxWidth())
            } else if (request != null && customer == null) {
                SoftCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("درخواست عمده روی این گوشی ثبت شده", fontWeight = FontWeight.ExtraBold)
                        Text("نام فروشگاه: ${request.businessName}")
                        if (request.city.isNotBlank()) Text("شهر: ${request.city}")
                        if (request.note.isNotBlank()) Text("توضیحات: ${request.note}")
                        Text("ثبت: ${faDate(request.at)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("این درخواست هنوز به اپ مدیریت منتقل نشده است؛ برای ارسال دستی از دکمه زیر استفاده کنید.", style = MaterialTheme.typography.bodySmall, color = Muted)
                GoldButton("ارسال درخواست برای فروشگاه", {
                    shareText(context, "درخواست عضویت عمده MOMO\nنام فروشگاه: ${request.businessName}\nشهر: ${request.city}\nتوضیحات: ${request.note}\nزمان ثبت: ${faDate(request.at)}")
                }, Modifier.fillMaxWidth())
            } else {
                Field("نام فروشگاه یا کسب‌وکار", business, { business = it })
                Field("شهر", city, { city = it })
                Field("توضیحات (اختیاری)", note, { note = it }, singleLine = false)
                if (error != null) Text(error ?: "", color = Danger)
                GoldButton("ثبت درخواست عضویت عمده", {
                    error = store.requestWholesale(business, city, note)
                }, Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun BankInfoScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val accounts = store.bankAccounts.filter { it.active }
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("حساب‌های فروشگاه", onBack = { nav.back() })
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (accounts.isEmpty()) {
                EmptyState("اطلاعات حساب‌های فروشگاه هنوز ثبت نشده است. برای هماهنگی پرداخت با فروشگاه تماس بگیرید.")
                GoldButton("تماس با فروشگاه", { nav.tab(Screen.Contact) }, Modifier.fillMaxWidth())
            }
            for (a in accounts) {
                SoftCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(a.bankName, fontWeight = FontWeight.ExtraBold)
                        Text("به نام: ${a.ownerName}", color = Muted)
                        if (a.cardNumber.isNotBlank()) {
                            Text("کارت: ${PersianFormat.digits(a.cardNumber)}")
                            OutlinedButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(a.cardNumber)); toast(context, "شماره کارت کپی شد") }) { Text("کپی شماره کارت") }
                        }
                        if (a.sheba.isNotBlank()) {
                            Text("شبا: ${PersianFormat.digits(a.sheba)}")
                            OutlinedButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(a.sheba)); toast(context, "شماره شبا کپی شد") }) { Text("کپی شماره شبا") }
                        }
                    }
                }
            }
        }
    }
}
