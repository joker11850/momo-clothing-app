package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OrderFlowTest {
    private val now = Fixtures.NOW
    private val key = Fixtures.keyM

    private fun ok(r: OrderResult): Order {
        assertIs<OrderResult.Success>(r)
        return r.order
    }

    @Test fun totalsAndRemaining() {
        val o = Fixtures.order(qty = 3, unitPrice = 500_000L, shippingCost = 80_000L).copy(extraDiscount = 30_000L, paidAmount = 600_000L)
        assertEquals(1_500_000L, o.subtotal)
        assertEquals(1_550_000L, o.total)
        assertEquals(950_000L, o.remaining)
        assertEquals(1_800_000L, o.costOfGoods)
    }

    @Test fun retailHappyPath() {
        val inv = Fixtures.inventory(10)
        val p = OrderProcessor(inv)
        var o = ok(p.submit(Fixtures.order(qty = 2), now))
        assertEquals(OrderStatus.AWAITING_PAYMENT, o.status)
        assertEquals(8, inv.available(key))
        assertEquals(10, inv.onHand(key))

        o = ok(p.recordPayment(o, o.total, now))
        assertEquals(OrderStatus.PAID, o.status)
        o = ok(p.startPreparing(o, now))
        o = ok(p.ship(o, "TRK123", now))
        assertEquals(OrderStatus.SHIPPED, o.status)
        assertEquals("TRK123", o.trackingCode)
        assertEquals(8, inv.onHand(key))
        o = ok(p.deliver(o, now))
        assertEquals(OrderStatus.DELIVERED, o.status)
        assertEquals(8, inv.onHand(key))
        assertEquals(8, inv.available(key))
        assertTrue(o.countsAsSale)
        assertEquals(
            listOf(
                OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID, OrderStatus.PREPARING,
                OrderStatus.SHIPPED, OrderStatus.DELIVERED
            ),
            o.history.map { it.to }
        )
    }

    @Test fun partialPaymentKeepsAwaitingPayment() {
        val p = OrderProcessor(Fixtures.inventory(10))
        var o = ok(p.submit(Fixtures.order(qty = 2), now))
        o = ok(p.recordPayment(o, 500_000L, now))
        assertEquals(OrderStatus.AWAITING_PAYMENT, o.status)
        assertEquals(1_500_000L, o.remaining)
        o = ok(p.recordPayment(o, 1_500_000L, now))
        assertEquals(OrderStatus.PAID, o.status)
    }

    @Test fun insufficientStockFailsWithoutReserving() {
        val inv = Fixtures.inventory(1)
        val p = OrderProcessor(inv)
        assertIs<OrderResult.Failure>(p.submit(Fixtures.order(qty = 2), now))
        assertEquals(1, inv.available(key))
    }

    @Test fun cancelReleasesReservation() {
        val inv = Fixtures.inventory(10)
        val p = OrderProcessor(inv)
        var o = ok(p.submit(Fixtures.order(qty = 4), now))
        o = ok(p.cancel(o, "انصراف مشتری", now))
        assertEquals(OrderStatus.CANCELLED, o.status)
        assertEquals(10, inv.available(key))
        assertFalse(o.countsAsSale)
    }

    @Test fun cannotSkipSteps() {
        val p = OrderProcessor(Fixtures.inventory(10))
        val o = ok(p.submit(Fixtures.order(), now))
        assertIs<OrderResult.Failure>(p.startPreparing(o, now)) // not paid yet
        assertIs<OrderResult.Failure>(p.ship(o, null, now))
        assertIs<OrderResult.Failure>(p.deliver(o, now))
        assertIs<OrderResult.Failure>(p.submit(o, now)) // already submitted
    }

    @Test fun retailCannotBuyOnAccount() {
        val p = OrderProcessor(Fixtures.inventory(10))
        assertIs<OrderResult.Failure>(p.submit(Fixtures.order(payment = PaymentMethod.ON_ACCOUNT), now))
    }

    @Test fun unpaidRetailOrderExpires() {
        val inv = Fixtures.inventory(10)
        val p = OrderProcessor(inv, retailHoldMillis = 30 * Fixtures.MINUTE)
        val o = ok(p.submit(Fixtures.order(qty = 5), now))
        val before = p.expireUnpaid(listOf(o), now + 29 * Fixtures.MINUTE)
        assertEquals(OrderStatus.AWAITING_PAYMENT, before[0].status)
        val after = p.expireUnpaid(listOf(o), now + 30 * Fixtures.MINUTE)
        assertEquals(OrderStatus.CANCELLED, after[0].status)
        assertEquals(10, inv.available(key))
    }

    @Test fun wholesaleNeedsApprovalThenPayment() {
        val inv = Fixtures.inventory(10)
        val p = OrderProcessor(inv)
        var o = ok(p.submit(Fixtures.order(channel = Channel.WHOLESALE, qty = 5), now))
        assertEquals(OrderStatus.AWAITING_SELLER_APPROVAL, o.status)
        assertEquals(5, inv.reserved(key, Channel.WHOLESALE))
        assertIs<OrderResult.Failure>(p.recordPayment(o, 1L, now))

        o = ok(p.approveWholesale(o, now))
        assertEquals(OrderStatus.AWAITING_PAYMENT, o.status)
        o = ok(p.recordPayment(o, o.total, now))
        assertEquals(OrderStatus.PAID, o.status)
    }

    @Test fun wholesaleOnAccountRequiresCreditApproval() {
        val inv = Fixtures.inventory(10)
        val p = OrderProcessor(inv)
        var o = ok(p.submit(Fixtures.order(channel = Channel.WHOLESALE, payment = PaymentMethod.ON_ACCOUNT, qty = 5), now))
        assertIs<OrderResult.Failure>(p.approveWholesale(o, now, creditApproved = false))
        o = ok(p.approveWholesale(o, now, creditApproved = true))
        assertEquals(OrderStatus.PREPARING, o.status)
        assertTrue(o.countsAsSale)
    }

    @Test fun wholesaleReservationCanExpireWhenConfigured() {
        val inv = Fixtures.inventory(10)
        val p = OrderProcessor(inv, wholesaleHoldMillis = Fixtures.DAY)
        val o = ok(p.submit(Fixtures.order(channel = Channel.WHOLESALE, qty = 5), now))
        val after = p.expireUnpaid(listOf(o), now + Fixtures.DAY)
        assertEquals(OrderStatus.CANCELLED, after[0].status)
        assertEquals(10, inv.available(key))
    }

    @Test fun pickupHasNoShippingStep() {
        val inv = Fixtures.inventory(10)
        val p = OrderProcessor(inv)
        var o = ok(p.submit(Fixtures.order(shipping = ShippingMethod.PICKUP, qty = 2), now))
        o = ok(p.recordPayment(o, o.total, now))
        o = ok(p.startPreparing(o, now))
        assertIs<OrderResult.Failure>(p.ship(o, null, now))
        o = ok(p.deliver(o, now))
        assertEquals(OrderStatus.DELIVERED, o.status)
        assertEquals(8, inv.onHand(key))
    }

    @Test fun courierOrderCannotBeDeliveredBeforeShipping() {
        val p = OrderProcessor(Fixtures.inventory(10))
        var o = ok(p.submit(Fixtures.order(), now))
        o = ok(p.recordPayment(o, o.total, now))
        o = ok(p.startPreparing(o, now))
        assertIs<OrderResult.Failure>(p.deliver(o, now))
    }

    @Test fun returnRestocksShippedGoods() {
        val inv = Fixtures.inventory(10)
        val p = OrderProcessor(inv)
        var o = ok(p.submit(Fixtures.order(qty = 3), now))
        o = ok(p.recordPayment(o, o.total, now))
        o = ok(p.startPreparing(o, now))
        o = ok(p.ship(o, null, now))
        assertEquals(7, inv.onHand(key))
        o = ok(p.markReturned(o, "مرجوعی", now))
        assertEquals(OrderStatus.RETURNED, o.status)
        assertEquals(10, inv.onHand(key))
        assertFalse(o.countsAsSale)
    }

    @Test fun cannotCancelAfterShipping() {
        val p = OrderProcessor(Fixtures.inventory(10))
        var o = ok(p.submit(Fixtures.order(), now))
        o = ok(p.recordPayment(o, o.total, now))
        o = ok(p.startPreparing(o, now))
        o = ok(p.ship(o, null, now))
        assertIs<OrderResult.Failure>(p.cancel(o, "x", now))
    }

    @Test fun stateMachineTables() {
        assertTrue(OrderStateMachine.canMove(Channel.RETAIL, OrderStatus.NEW, OrderStatus.AWAITING_PAYMENT))
        assertFalse(OrderStateMachine.canMove(Channel.RETAIL, OrderStatus.NEW, OrderStatus.AWAITING_SELLER_APPROVAL))
        assertTrue(OrderStateMachine.canMove(Channel.WHOLESALE, OrderStatus.NEW, OrderStatus.AWAITING_SELLER_APPROVAL))
        assertFalse(OrderStateMachine.canMove(Channel.WHOLESALE, OrderStatus.NEW, OrderStatus.AWAITING_PAYMENT))
        assertTrue(OrderStateMachine.allowed(Channel.RETAIL, OrderStatus.CANCELLED).isEmpty())
        assertTrue(OrderStateMachine.allowed(Channel.RETAIL, OrderStatus.RETURNED).isEmpty())
    }
}
