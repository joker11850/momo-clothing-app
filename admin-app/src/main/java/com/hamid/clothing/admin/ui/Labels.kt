package com.hamid.clothing.admin.ui

import androidx.compose.ui.graphics.Color
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.ChequeDirection
import com.hamid.clothing.core.ChequeStatus
import com.hamid.clothing.core.CreditPolicy
import com.hamid.clothing.core.ExpenseCategory
import com.hamid.clothing.core.JalaliDate
import com.hamid.clothing.core.OrderStatus
import com.hamid.clothing.core.CustomerType
import com.hamid.clothing.core.EntryType
import com.hamid.clothing.core.JalaliCalendar
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.core.Validators
import com.hamid.clothing.core.WholesaleStatus

fun CustomerType.label(): String = when (this) {
    CustomerType.RETAIL -> "خرده"
    CustomerType.WHOLESALE -> "عمده"
}

fun WholesaleStatus.label(): String = when (this) {
    WholesaleStatus.NONE -> "ثبت‌نشده"
    WholesaleStatus.PENDING -> "در انتظار تأیید"
    WholesaleStatus.APPROVED -> "تأییدشده"
    WholesaleStatus.REJECTED -> "ردشده"
}

fun CreditPolicy.label(): String = when (this) {
    CreditPolicy.NO_CREDIT -> "بدون خرید دفتری"
    CreditPolicy.WITHIN_LIMIT -> "تا سقف اعتبار"
    CreditPolicy.APPROVAL_REQUIRED -> "با تأیید مدیر"
}

fun EntryType.label(): String = when (this) {
    EntryType.OPENING_DEBT -> "مانده اول دوره"
    EntryType.PURCHASE -> "خرید / بدهی"
    EntryType.CHEQUE_BOUNCED -> "چک برگشتی"
    EntryType.ADJUSTMENT_UP -> "تعدیل افزایشی"
    EntryType.PAYMENT -> "پرداخت"
    EntryType.CHEQUE_CLEARED -> "چک وصول‌شده"
    EntryType.RETURN_CREDIT -> "مرجوعی"
    EntryType.ADJUSTMENT_DOWN -> "تعدیل کاهشی"
}

fun formatToman(amount: Long): String = PersianFormat.money(amount) + " تومان"

fun faInt(n: Int): String = PersianFormat.digits(n.toString())

fun faDate(millis: Long): String = PersianFormat.digits(JalaliCalendar.fromEpochMillis(millis).format())

/** Digits only, Persian digits accepted, at most 12 digits. */
fun parseMoney(text: String): Long =
    Validators.toLatinDigits(text).filter { it in '0'..'9' }.take(12).toLongOrNull() ?: 0L

fun parseInt(text: String, default: Int = 0): Int =
    Validators.toLatinDigits(text).filter { it in '0'..'9' }.take(6).toIntOrNull() ?: default

fun ChequeStatus.label(): String = when (this) {
    ChequeStatus.PENDING -> "در دست"
    ChequeStatus.DEPOSITED -> "به صندوق رفته"
    ChequeStatus.CLEARED -> "وصول‌شده"
    ChequeStatus.BOUNCED -> "برگشتی"
}

fun ChequeDirection.label(): String = when (this) {
    ChequeDirection.RECEIVED -> "دریافتی"
    ChequeDirection.PAID -> "پرداختی"
}

fun ExpenseCategory.label(): String = when (this) {
    ExpenseCategory.SHIPPING -> "حمل"
    ExpenseCategory.PACKAGING -> "بسته‌بندی"
    ExpenseCategory.RENT -> "اجاره"
    ExpenseCategory.SALARY -> "حقوق"
    ExpenseCategory.STORE -> "هزینه‌های فروشگاه"
    ExpenseCategory.PURCHASE_OVERHEAD -> "هزینه‌های جانبی خرید"
    ExpenseCategory.PERSONAL -> "شخصی"
    ExpenseCategory.OTHER -> "سایر"
}

fun orderStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.CANCELLED, OrderStatus.RETURNED -> Danger
    OrderStatus.DELIVERED, OrderStatus.PAID -> Success
    else -> Gold
}

/** Name of a customer or supplier by id. */
fun partyName(store: AdminStore, id: String): String =
    store.customers.firstOrNull { it.id == id }?.fullName
        ?: store.suppliers.firstOrNull { it.id == id }?.name
        ?: "نامشخص"

/** Parses a Jalali date typed as three numbers; null when it is not a real date. */
fun jalaliToMillis(year: String, month: String, day: String): Long? {
    val y = parseInt(year, -1)
    val m = parseInt(month, -1)
    val d = parseInt(day, -1)
    if (y < 1300 || y > 1500) return null
    return try {
        JalaliCalendar.toEpochMillis(JalaliDate(y, m, d))
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun todayParts(offsetDays: Int = 0): Triple<String, String, String> {
    val d = JalaliCalendar.fromEpochMillis(System.currentTimeMillis() + offsetDays * 86_400_000L)
    return Triple(d.year.toString(), d.month.toString(), d.day.toString())
}
