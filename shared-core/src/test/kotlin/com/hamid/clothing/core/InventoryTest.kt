package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryTest {
    private val key = Fixtures.keyM

    @Test fun reserveReducesAvailableButNotOnHand() {
        val inv = Fixtures.inventory(10)
        val r = inv.reserveOrder("o1", Channel.RETAIL, listOf(key to 4), Long.MAX_VALUE)
        assertTrue(r.ok)
        assertEquals(10, inv.onHand(key))
        assertEquals(6, inv.available(key))
    }

    @Test fun reservationIsAllOrNothing() {
        val inv = Fixtures.inventory(5)
        val other = StockKey("p1", "کرم", "L")
        val r = inv.reserveOrder("o1", Channel.RETAIL, listOf(key to 3, other to 9), Long.MAX_VALUE)
        assertFalse(r.ok)
        assertEquals(listOf(other), r.shortages)
        assertEquals(5, inv.available(key))
        assertEquals(5, inv.available(other))
    }

    @Test fun sameKeyInTwoLinesIsAggregated() {
        val inv = Fixtures.inventory(5)
        assertFalse(inv.reserveOrder("o1", Channel.RETAIL, listOf(key to 3, key to 3), Long.MAX_VALUE).ok)
        assertTrue(inv.reserveOrder("o2", Channel.RETAIL, listOf(key to 3, key to 2), Long.MAX_VALUE).ok)
        assertEquals(0, inv.available(key))
    }

    @Test fun channelsAreTrackedSeparately() {
        val inv = Fixtures.inventory(10)
        inv.reserveOrder("r1", Channel.RETAIL, listOf(key to 2), Long.MAX_VALUE)
        inv.reserveOrder("w1", Channel.WHOLESALE, listOf(key to 5), Long.MAX_VALUE)
        assertEquals(2, inv.reserved(key, Channel.RETAIL))
        assertEquals(5, inv.reserved(key, Channel.WHOLESALE))
        assertEquals(7, inv.reserved(key))
        assertEquals(3, inv.available(key))
    }

    @Test fun releaseCommitAndRestock() {
        val inv = Fixtures.inventory(10)
        inv.reserveOrder("o1", Channel.RETAIL, listOf(key to 4), Long.MAX_VALUE)
        inv.release("o1")
        assertEquals(10, inv.available(key))

        inv.reserveOrder("o2", Channel.RETAIL, listOf(key to 4), Long.MAX_VALUE)
        assertTrue(inv.commit("o2"))
        assertEquals(6, inv.onHand(key))
        assertEquals(6, inv.available(key))
        assertFalse(inv.commit("o2"))

        inv.restock(key, 4)
        assertEquals(10, inv.onHand(key))
    }

    @Test fun expiryDropsOnlyExpiredReservations() {
        val inv = Fixtures.inventory(10)
        inv.reserveOrder("short", Channel.RETAIL, listOf(key to 2), 1_000L)
        inv.reserveOrder("forever", Channel.WHOLESALE, listOf(key to 3), Long.MAX_VALUE)
        assertEquals(listOf("short"), inv.expire(1_000L))
        assertEquals(7, inv.available(key))
    }

    @Test fun duplicateReservationAndBadQuantityRejected() {
        val inv = Fixtures.inventory(10)
        inv.reserveOrder("o1", Channel.RETAIL, listOf(key to 1), Long.MAX_VALUE)
        assertFailsWith<IllegalArgumentException> {
            inv.reserveOrder("o1", Channel.RETAIL, listOf(key to 1), Long.MAX_VALUE)
        }
        assertFailsWith<IllegalArgumentException> {
            inv.reserveOrder("o2", Channel.RETAIL, listOf(key to 0), Long.MAX_VALUE)
        }
    }

    @Test fun lowStockUsesAvailableAndThreshold() {
        val inv = Fixtures.inventory(5)
        val products = listOf(Fixtures.product(stock = 5)) // threshold 3
        assertTrue(inv.lowStock(products).isEmpty())
        inv.reserveOrder("o1", Channel.RETAIL, listOf(key to 3), Long.MAX_VALUE)
        assertEquals(listOf(key), inv.lowStock(products))
    }
}
