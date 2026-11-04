package com.hamid.clothing.core

/** Self-contained Jalali (Shamsi) calendar. No java.time, so it is safe on Android API 23. */
data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    init {
        require(month in 1..12) { "invalid Jalali month $month" }
        require(day in 1..JalaliCalendar.monthLength(year, month)) { "invalid Jalali day $day" }
    }

    fun format(): String =
        year.toString().padStart(4, '0') + "/" +
            month.toString().padStart(2, '0') + "/" +
            day.toString().padStart(2, '0')

    fun monthName(): String = JalaliCalendar.MONTH_NAMES[month - 1]
}

object JalaliCalendar {
    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private const val DAY_MILLIS = 86_400_000L
    private const val EPOCH_JDN = 2_440_588L // JDN of 1970-01-01
    const val IRAN_OFFSET_MINUTES = 210 // UTC+03:30, Iran has no DST since 2022

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635,
        2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private class Cal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): Cal {
        require(jy >= breaks[0] && jy < breaks[breaks.size - 1]) { "Jalali year out of range: $jy" }
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        var i = 1
        while (i < breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
            i++
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return Cal(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = ((gy + (gm - 8) / 6 + 100100) * 1461) / 4 +
            (153 * ((gm + 9) % 12) + 2) / 5 + gd - 34840408
        d = d - (((gy + 100100 + (gm - 8) / 6) / 100) * 3) / 4 + 752
        return d
    }

    private fun d2g(jdn: Int): IntArray {
        var j = 4 * jdn + 139361631
        j += (((4 * jdn + 183187720) / 146097) * 3 / 4) * 4 - 3908
        val i = ((j % 1461) / 4) * 5 + 308
        val gd = (i % 153) / 5 + 1
        val gm = (i / 153) % 12 + 1
        val gy = j / 1461 - 100100 + (8 - gm) / 6
        return intArrayOf(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): JalaliDate {
        val gy = d2g(jdn)[0]
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }

    fun isLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    fun fromGregorian(gy: Int, gm: Int, gd: Int): JalaliDate = d2j(g2d(gy, gm, gd))

    fun toGregorian(date: JalaliDate): Triple<Int, Int, Int> {
        val g = d2g(j2d(date.year, date.month, date.day))
        return Triple(g[0], g[1], g[2])
    }

    fun fromEpochMillis(millis: Long, offsetMinutes: Int = IRAN_OFFSET_MINUTES): JalaliDate {
        val local = millis + offsetMinutes * 60_000L
        val epochDay = local.floorDiv(DAY_MILLIS)
        return d2j((epochDay + EPOCH_JDN).toInt())
    }

    /** Start of the given Jalali day (00:00 local time) as epoch millis. */
    fun toEpochMillis(date: JalaliDate, offsetMinutes: Int = IRAN_OFFSET_MINUTES): Long {
        val epochDay = j2d(date.year, date.month, date.day) - EPOCH_JDN
        return epochDay * DAY_MILLIS - offsetMinutes * 60_000L
    }
}
