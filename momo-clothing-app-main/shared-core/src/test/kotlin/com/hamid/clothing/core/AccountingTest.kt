package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountingTest {
    private val now = Fixtures.NOW

    private fun entry(id: String, type: EntryType, amount: Long, at: Long = now, party: String = "w1") =
        LedgerEntry(id, party, type, amount, at)

    private fun cheque(
        id: String = "c1",
        amount: Long = 5_000_000L,
        due: Long = now + 10 * Fixtures.DAY,
        direction: ChequeDirection = ChequeDirection.RECEIVED
    ) = Cheque(id, direction, "w1", amount, "1234567890123456", due)

    @Test fun ledgerBalanceAndStatement() {
        val l = PartyLedger("w1")
        l.add(entry("1", EntryType.PURCHASE, 10_000_000L, now))
        l.add(entry("2", EntryType.PAYMENT, 4_000_000L, now + 1))
        l.add(entry("3", EntryType.PURCHASE, 2_000_000L, now + 2))
        l.add(entry("4", EntryType.RETURN_CREDIT, 500_000L, now + 3))
        assertEquals(7_500_000L, l.balance)
        assertEquals(listOf(10_000_000L, 6_000_000L, 8_000_000L, 7_500_000L), l.statement().map { it.balanceAfter })
    }

    @Test fun ledgerRejectsForeignAndDuplicateEntries() {
        val l = PartyLedger("w1")
        l.add(entry("1", EntryType.PURCHASE, 100L))
        assertFailsWith<IllegalArgumentException> { l.add(entry("1", EntryType.PAYMENT, 50L)) }
        assertFailsWith<IllegalArgumentException> { l.add(entry("2", EntryType.PAYMENT, 50L, party = "other")) }
        assertFailsWith<IllegalArgumentException> { entry("3", EntryType.PAYMENT, 0L) }
    }

    @Test fun supplierLedgerWorksTheSameWay() {
        val l = PartyLedger("s1")
        l.add(entry("1", EntryType.PURCHASE, 20_000_000L, party = "s1"))
        l.add(entry("2", EntryType.PAYMENT, 5_000_000L, party = "s1"))
        assertEquals(15_000_000L, l.balance)
    }

    @Test fun chequeClearReducesBalance() {
        val l = PartyLedger("w1")
        l.add(entry("1", EntryType.PURCHASE, 8_000_000L))
        val c = ChequeService.clear(cheque(amount = 5_000_000L), l, now)
        assertEquals(ChequeStatus.CLEARED, c.status)
        assertEquals(3_000_000L, l.balance)
    }

    @Test fun clearedChequeThatBouncesIsReversed() {
        val l = PartyLedger("w1")
        l.add(entry("1", EntryType.PURCHASE, 8_000_000L))
        val cleared = ChequeService.clear(cheque(), l, now)
        val bounced = ChequeService.bounce(cleared, l, now + 1)
        assertEquals(ChequeStatus.BOUNCED, bounced.status)
        assertEquals(8_000_000L, l.balance)
    }

    @Test fun openChequeThatBouncesDoesNotTouchLedger() {
        val l = PartyLedger("w1")
        l.add(entry("1", EntryType.PURCHASE, 8_000_000L))
        ChequeService.bounce(cheque(), l, now)
        assertEquals(8_000_000L, l.balance)
    }

    @Test fun chequeStateGuards() {
        val l = PartyLedger("w1")
        val deposited = ChequeService.deposit(cheque())
        assertEquals(ChequeStatus.DEPOSITED, deposited.status)
        assertFailsWith<IllegalArgumentException> { ChequeService.deposit(deposited) }
        val cleared = ChequeService.clear(deposited, l, now)
        assertFailsWith<IllegalArgumentException> { ChequeService.clear(cleared, l, now) }
        assertFailsWith<IllegalArgumentException> {
            Cheque("bad", ChequeDirection.RECEIVED, "w1", 1L, "123", now)
        }
    }

    @Test fun dueSoonAndPendingTotals() {
        val soon = cheque("a", due = now + 3 * Fixtures.DAY)
        val later = cheque("b", due = now + 30 * Fixtures.DAY)
        val overdue = cheque("c", due = now - Fixtures.DAY)
        val paidOne = cheque("d", direction = ChequeDirection.PAID, amount = 1_000_000L)
        val l = PartyLedger("w1")
        val clearedOne = ChequeService.clear(cheque("e"), l, now)
        assertTrue(soon.isDueSoon(now, 7))
        assertFalse(later.isDueSoon(now, 7))
        assertTrue(overdue.isDueSoon(now, 7))
        assertFalse(clearedOne.isDueSoon(now, 7))
        val all = listOf(soon, later, overdue, paidOne, clearedOne)
        assertEquals(15_000_000L, ChequeService.pendingTotal(all, ChequeDirection.RECEIVED))
        assertEquals(1_000_000L, ChequeService.pendingTotal(all, ChequeDirection.PAID))
    }

    @Test fun creditDecisions() {
        val within = Fixtures.wholesaleCustomer(policy = CreditPolicy.WITHIN_LIMIT, limit = 10_000_000L)
        assertEquals(CreditDecision.ALLOWED, CreditService.evaluate(within, 4_000_000L, 6_000_000L))
        assertEquals(CreditDecision.LIMIT_EXCEEDED, CreditService.evaluate(within, 4_000_000L, 6_000_001L))
        val approval = Fixtures.wholesaleCustomer(policy = CreditPolicy.APPROVAL_REQUIRED)
        assertEquals(CreditDecision.NEEDS_ADMIN_APPROVAL, CreditService.evaluate(approval, 0L, 1L))
        val none = Fixtures.wholesaleCustomer(policy = CreditPolicy.NO_CREDIT)
        assertEquals(CreditDecision.NO_CREDIT, CreditService.evaluate(none, 0L, 1L))
        assertEquals(CreditDecision.BLOCKED, CreditService.evaluate(within.copy(blocked = true), 0L, 1L))
        assertEquals(CreditDecision.NOT_WHOLESALE, CreditService.evaluate(Fixtures.retailCustomer(), 0L, 1L))
        val pending = within.copy(wholesaleStatus = WholesaleStatus.PENDING)
        assertEquals(CreditDecision.NOT_WHOLESALE, CreditService.evaluate(pending, 0L, 1L))
    }

    @Test fun postCreditPurchaseBooksOrderTotal() {
        val o = Fixtures.order(channel = Channel.WHOLESALE, payment = PaymentMethod.ON_ACCOUNT, qty = 2, shippingCost = 50_000L)
        val l = PartyLedger("w1")
        CreditService.postCreditPurchase(o, l, now)
        assertEquals(2_050_000L, l.balance)
        assertFailsWith<IllegalArgumentException> {
            CreditService.postCreditPurchase(Fixtures.order(), PartyLedger("w1"), now)
        }
    }

    @Test fun profitReportSeparatesChannelsAndPersonalExpenses() {
        val retail = Fixtures.order("r1", Channel.RETAIL, qty = 2, unitPrice = 1_000_000L, unitCost = 600_000L)
            .copy(status = OrderStatus.DELIVERED)
        val wholesale = Fixtures.order("w1", Channel.WHOLESALE, qty = 10, unitPrice = 800_000L, unitCost = 600_000L)
            .copy(status = OrderStatus.PREPARING)
        val cancelled = Fixtures.order("x1").copy(status = OrderStatus.CANCELLED)
        val returned = Fixtures.order("x2").copy(status = OrderStatus.RETURNED)
        val unpaid = Fixtures.order("x3").copy(status = OrderStatus.AWAITING_PAYMENT)
        val outside = Fixtures.order("x4").copy(status = OrderStatus.DELIVERED, createdAt = now - 100 * Fixtures.DAY)
        val expenses = listOf(
            Expense("e1", ExpenseCategory.RENT, 1_000_000L, now),
            Expense("e2", ExpenseCategory.SHIPPING, 200_000L, now),
            Expense("e3", ExpenseCategory.PERSONAL, 3_000_000L, now),
            Expense("e4", ExpenseCategory.RENT, 9_000_000L, now - 100 * Fixtures.DAY)
        )
        val r = ReportEngine.profit(
            listOf(retail, wholesale, cancelled, returned, unpaid, outside), expenses,
            now - Fixtures.DAY, now + Fixtures.DAY
        )
        assertEquals(10_000_000L, r.salesTotal) // 2,000,000 + 8,000,000
        assertEquals(7_200_000L, r.costOfGoods) // 1,200,000 + 6,000,000
        assertEquals(2_800_000L, r.grossProfit)
        assertEquals(800_000L, r.retailGrossProfit)
        assertEquals(2_000_000L, r.wholesaleGrossProfit)
        assertEquals(1_200_000L, r.businessExpenses)
        assertEquals(1_600_000L, r.netProfit)
        assertEquals(3_000_000L, r.personalExpenses)
    }
}
