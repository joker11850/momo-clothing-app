package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyAndFormatTest {
    @Test fun percentRoundsHalfUp() {
        assertEquals(187_500L, Money.percentOf(1_250_000L, 15))
        assertEquals(150L, Money.percentOf(999L, 15)) // 149.85 -> 150
        assertEquals(0L, Money.percentOf(1_000L, 0))
        assertEquals(1_000L, Money.percentOf(1_000L, 100))
    }

    @Test fun afterDiscount() {
        assertEquals(1_062_500L, Money.afterDiscount(1_250_000L, 15))
    }

    @Test fun invalidPercentFails() {
        assertFailsWith<IllegalArgumentException> { Money.percentOf(1_000L, 101) }
        assertFailsWith<IllegalArgumentException> { Money.percentOf(-1L, 10) }
    }

    @Test fun persianMoneyFormat() {
        assertEquals("۱۲,۵۰۰,۰۰۰", PersianFormat.money(12_500_000L))
        assertEquals("۰", PersianFormat.money(0L))
        assertEquals("۹۹۹", PersianFormat.money(999L))
        assertEquals("۱,۰۰۰", PersianFormat.money(1_000L))
        assertEquals("-۱,۵۰۰", PersianFormat.money(-1_500L))
    }
}
