package com.hamid.clothing.core

/**
 * All money values in the suite are whole Toman stored as Long.
 * Never use Double/Float for money.
 */
object Money {
    /** percent of amount, rounded half up. */
    fun percentOf(amount: Long, percent: Int): Long {
        require(percent in 0..100) { "percent must be between 0 and 100" }
        require(amount >= 0) { "amount must not be negative" }
        return (amount * percent + 50) / 100
    }

    fun afterDiscount(amount: Long, percent: Int): Long = amount - percentOf(amount, percent)
}

object PersianFormat {
    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    fun digits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') sb.append(FA_DIGITS[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    /** 12500000 -> ۱۲,۵۰۰,۰۰۰ */
    fun money(amount: Long): String {
        val negative = amount < 0
        val raw = if (negative) (-amount).toString() else amount.toString()
        val grouped = raw.reversed().chunked(3).joinToString(",").reversed()
        return digits((if (negative) "-" else "") + grouped)
    }
}
