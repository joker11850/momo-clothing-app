package com.hamid.clothing.admin.ui

import com.hamid.clothing.core.CreditPolicy
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
