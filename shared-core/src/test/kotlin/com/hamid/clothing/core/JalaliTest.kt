package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JalaliTest {
    @Test fun knownDates() {
        assertEquals(JalaliDate(1405, 8, 12), JalaliCalendar.fromGregorian(2026, 11, 3))
        assertEquals(JalaliDate(1405, 1, 1), JalaliCalendar.fromGregorian(2026, 3, 21))
        assertEquals(JalaliDate(1404, 1, 1), JalaliCalendar.fromGregorian(2025, 3, 21))
        assertEquals(JalaliDate(1403, 12, 30), JalaliCalendar.fromGregorian(2025, 3, 20))
        assertEquals(JalaliDate(1348, 10, 11), JalaliCalendar.fromGregorian(1970, 1, 1))
    }

    @Test fun leapYears() {
        assertTrue(JalaliCalendar.isLeap(1403))
        assertFalse(JalaliCalendar.isLeap(1404))
        assertFalse(JalaliCalendar.isLeap(1405))
        assertTrue(JalaliCalendar.isLeap(1408))
        assertEquals(30, JalaliCalendar.monthLength(1403, 12))
        assertEquals(29, JalaliCalendar.monthLength(1404, 12))
        assertEquals(31, JalaliCalendar.monthLength(1405, 6))
        assertEquals(30, JalaliCalendar.monthLength(1405, 7))
    }

    @Test fun roundTripGregorian() {
        val g = JalaliCalendar.toGregorian(JalaliDate(1405, 8, 12))
        assertEquals(Triple(2026, 11, 3), g)
        val g2 = JalaliCalendar.toGregorian(JalaliDate(1403, 12, 30))
        assertEquals(Triple(2025, 3, 20), g2)
    }

    @Test fun epochMillisConversion() {
        assertEquals(JalaliDate(1348, 10, 11), JalaliCalendar.fromEpochMillis(0L))
        val d = JalaliDate(1405, 8, 12)
        val start = JalaliCalendar.toEpochMillis(d)
        assertEquals(d, JalaliCalendar.fromEpochMillis(start))
        assertEquals(d, JalaliCalendar.fromEpochMillis(start + 86_400_000L - 1))
        assertEquals(JalaliDate(1405, 8, 13), JalaliCalendar.fromEpochMillis(start + 86_400_000L))
    }

    @Test fun everyDayRoundTripsAndIsConsecutive() {
        var date = JalaliCalendar.fromGregorian(1990, 1, 1)
        var millis = JalaliCalendar.toEpochMillis(date)
        repeat(365 * 40) {
            val next = JalaliCalendar.fromEpochMillis(millis + 86_400_000L)
            val sameMonth = next.year == date.year && next.month == date.month && next.day == date.day + 1
            val nextMonth = next.day == 1 && (
                (next.year == date.year && next.month == date.month + 1) ||
                    (date.month == 12 && next.year == date.year + 1 && next.month == 1)
                )
            assertTrue(sameMonth || nextMonth, "not consecutive: $date -> $next")
            assertEquals(next, JalaliCalendar.fromEpochMillis(JalaliCalendar.toEpochMillis(next)))
            date = next
            millis += 86_400_000L
        }
    }

    @Test fun invalidDateRejected() {
        assertFailsWith<IllegalArgumentException> { JalaliDate(1404, 12, 30) } // 1404 is not leap
        assertFailsWith<IllegalArgumentException> { JalaliDate(1404, 13, 1) }
    }

    @Test fun formatting() {
        assertEquals("1405/08/12", JalaliDate(1405, 8, 12).format())
        assertEquals("آبان", JalaliDate(1405, 8, 12).monthName())
    }
}
