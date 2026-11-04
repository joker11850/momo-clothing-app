package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PricingTest {
    private val p = Fixtures.product()
    private val now = Fixtures.NOW

    @Test fun guestAndRetailCustomerPayRetail() {
        val q1 = PricingEngine.quote(p, null, emptyMap(), emptyList(), null, now)
        assertEquals(PriceSource.RETAIL, q1.source)
        assertEquals(1_000_000L, q1.unitPrice)
        val q2 = PricingEngine.quote(p, Fixtures.retailCustomer(), emptyMap(), emptyList(), null, now)
        assertEquals(1_000_000L, q2.unitPrice)
    }

    @Test fun approvedWholesalePaysWholesale() {
        val q = PricingEngine.quote(p, Fixtures.wholesaleCustomer(), emptyMap(), emptyList(), null, now)
        assertEquals(PriceSource.WHOLESALE, q.source)
        assertEquals(800_000L, q.unitPrice)
    }

    @Test fun pendingOrBlockedWholesaleDoesNotSeeWholesale() {
        val pending = Fixtures.wholesaleCustomer().copy(wholesaleStatus = WholesaleStatus.PENDING)
        assertEquals(1_000_000L, PricingEngine.quote(p, pending, emptyMap(), emptyList(), null, now).unitPrice)
        val blocked = Fixtures.wholesaleCustomer().copy(blocked = true)
        assertEquals(1_000_000L, PricingEngine.quote(p, blocked, emptyMap(), emptyList(), null, now).unitPrice)
    }

    @Test fun wholesaleFallsBackToRetailWhenNotSet() {
        val noWholesale = Fixtures.product(wholesale = 0L)
        val q = PricingEngine.quote(noWholesale, Fixtures.wholesaleCustomer(), emptyMap(), emptyList(), null, now)
        assertEquals(PriceSource.RETAIL, q.source)
    }

    @Test fun customPriceWinsAndIsNotDiscountedAgain() {
        val discount = Discount("d1", "عمومی", 20)
        val q = PricingEngine.quote(
            p, Fixtures.wholesaleCustomer(), mapOf("p1" to 700_000L), listOf(discount), null, now
        )
        assertEquals(PriceSource.CUSTOM, q.source)
        assertEquals(700_000L, q.unitPrice)
        assertNull(q.discountId)
    }

    @Test fun bestSingleDiscountIsAppliedWithoutStacking() {
        val d = listOf(Discount("d10", "ده", 10), Discount("d15", "پانزده", 15))
        val q = PricingEngine.quote(p, null, emptyMap(), d, null, now)
        assertEquals(15, q.discountPercent)
        assertEquals("d15", q.discountId)
        assertEquals(850_000L, q.unitPrice)
    }

    @Test fun scopesAndSchedule() {
        val forOtherProduct = Discount("a", "x", 30, DiscountScope.PRODUCTS, setOf("zzz"))
        val forThisProduct = Discount("b", "y", 10, DiscountScope.PRODUCTS, setOf("p1"))
        val expired = Discount("c", "z", 50, endsAt = now - 1)
        val future = Discount("d", "w", 50, startsAt = now + 1)
        val inactive = Discount("e", "v", 50, active = false)
        val q = PricingEngine.quote(p, null, emptyMap(), listOf(forOtherProduct, forThisProduct, expired, future, inactive), null, now)
        assertEquals("b", q.discountId)
        assertEquals(900_000L, q.unitPrice)
    }

    @Test fun customerAndGroupDiscounts() {
        val c = Fixtures.retailCustomer("u9").copy(groups = setOf("vip"))
        val byCustomer = Discount("x", "x", 5, DiscountScope.CUSTOMERS, setOf("u9"))
        val byGroup = Discount("y", "y", 12, DiscountScope.CUSTOMER_GROUPS, setOf("vip"))
        assertEquals("y", PricingEngine.quote(p, c, emptyMap(), listOf(byCustomer, byGroup), null, now).discountId)
        assertNull(PricingEngine.quote(p, null, emptyMap(), listOf(byCustomer, byGroup), null, now).discountId)
    }

    @Test fun couponNeedsCode() {
        val coupon = Discount("k", "کد", 20, code = "SPRING")
        assertEquals(1_000_000L, PricingEngine.quote(p, null, emptyMap(), listOf(coupon), null, now).unitPrice)
        assertEquals(1_000_000L, PricingEngine.quote(p, null, emptyMap(), listOf(coupon), "WRONG", now).unitPrice)
        assertEquals(800_000L, PricingEngine.quote(p, null, emptyMap(), listOf(coupon), " spring ", now).unitPrice)
    }
}
