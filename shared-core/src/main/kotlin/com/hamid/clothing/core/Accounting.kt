package com.hamid.clothing.core

enum class EntryType(val increasesBalance: Boolean) {
    OPENING_DEBT(true),
    PURCHASE(true),
    CHEQUE_BOUNCED(true),
    ADJUSTMENT_UP(true),
    PAYMENT(false),
    CHEQUE_CLEARED(false),
    RETURN_CREDIT(false),
    ADJUSTMENT_DOWN(false)
}

data class LedgerEntry(
    val id: String,
    val partyId: String,
    val type: EntryType,
    val amount: Long,
    val at: Long,
    val refId: String? = null,
    val note: String = ""
) {
    init { require(amount > 0) { "amount must be positive" } }

    val signed: Long get() = if (type.increasesBalance) amount else -amount
}

data class StatementRow(val entry: LedgerEntry, val balanceAfter: Long)

/**
 * Account book for one party.
 * Customer: positive balance = customer owes the shop.
 * Supplier: positive balance = the shop owes the supplier.
 */
class PartyLedger(val partyId: String) {
    private val items = mutableListOf<LedgerEntry>()

    val entries: List<LedgerEntry> get() = items.toList()
    val balance: Long get() = items.sumOf { it.signed }

    fun add(entry: LedgerEntry) {
        require(entry.partyId == partyId) { "entry belongs to another party" }
        require(items.none { it.id == entry.id }) { "duplicate entry id ${entry.id}" }
        items.add(entry)
    }

    fun statement(): List<StatementRow> {
        var running = 0L
        return items.sortedBy { it.at }.map {
            running += it.signed
            StatementRow(it, running)
        }
    }
}

enum class ChequeDirection { RECEIVED, PAID }
enum class ChequeStatus { PENDING, DEPOSITED, CLEARED, BOUNCED }

data class Cheque(
    val id: String,
    val direction: ChequeDirection,
    val partyId: String,
    val amount: Long,
    val sayadId: String,
    val dueDate: Long,
    val bankName: String = "",
    val status: ChequeStatus = ChequeStatus.PENDING,
    val note: String = ""
) {
    init {
        require(amount > 0) { "amount must be positive" }
        require(Validators.isValidSayadId(sayadId)) { "sayad id must be 16 digits" }
    }

    val isOpen: Boolean get() = status == ChequeStatus.PENDING || status == ChequeStatus.DEPOSITED

    /** Open cheque that is overdue or due within [days] days. */
    fun isDueSoon(now: Long, days: Int): Boolean = isOpen && dueDate <= now + days * 86_400_000L
}

object ChequeService {
    private fun entryId(cheque: Cheque, suffix: String) = "chq-${cheque.id}-$suffix"

    fun deposit(cheque: Cheque): Cheque {
        require(cheque.status == ChequeStatus.PENDING) { "only a pending cheque can be deposited" }
        return cheque.copy(status = ChequeStatus.DEPOSITED)
    }

    /** Marks the cheque cleared and records it in the party's account. */
    fun clear(cheque: Cheque, ledger: PartyLedger, now: Long): Cheque {
        require(cheque.isOpen) { "cheque is not open" }
        ledger.add(LedgerEntry(entryId(cheque, "clear"), cheque.partyId, EntryType.CHEQUE_CLEARED, cheque.amount, now, cheque.id))
        return cheque.copy(status = ChequeStatus.CLEARED)
    }

    /** A bounced open cheque never touched the account; a bounced cleared cheque is reversed. */
    fun bounce(cheque: Cheque, ledger: PartyLedger, now: Long): Cheque {
        require(cheque.status != ChequeStatus.BOUNCED) { "cheque already bounced" }
        if (cheque.status == ChequeStatus.CLEARED) {
            ledger.add(LedgerEntry(entryId(cheque, "bounce"), cheque.partyId, EntryType.CHEQUE_BOUNCED, cheque.amount, now, cheque.id))
        }
        return cheque.copy(status = ChequeStatus.BOUNCED)
    }

    fun pendingTotal(cheques: List<Cheque>, direction: ChequeDirection, partyId: String? = null): Long =
        cheques.filter { it.direction == direction && it.isOpen && (partyId == null || it.partyId == partyId) }
            .sumOf { it.amount }
}

enum class CreditDecision {
    ALLOWED, NEEDS_ADMIN_APPROVAL, NOT_WHOLESALE, BLOCKED, NO_CREDIT, LIMIT_EXCEEDED
}

object CreditService {
    /**
     * @param currentBalance what the customer already owes (open cheques are not deducted, on purpose)
     */
    fun evaluate(customer: Customer, currentBalance: Long, orderTotal: Long): CreditDecision {
        if (customer.blocked) return CreditDecision.BLOCKED
        if (!customer.canSeeWholesale) return CreditDecision.NOT_WHOLESALE
        return when (customer.creditPolicy) {
            CreditPolicy.NO_CREDIT -> CreditDecision.NO_CREDIT
            CreditPolicy.WITHIN_LIMIT ->
                if (currentBalance + orderTotal <= customer.creditLimit) CreditDecision.ALLOWED
                else CreditDecision.LIMIT_EXCEEDED
            CreditPolicy.APPROVAL_REQUIRED -> CreditDecision.NEEDS_ADMIN_APPROVAL
        }
    }

    /** Books an on-account order as customer debt. */
    fun postCreditPurchase(order: Order, ledger: PartyLedger, now: Long) {
        require(order.paymentMethod == PaymentMethod.ON_ACCOUNT) { "order is not on account" }
        ledger.add(LedgerEntry("ord-${order.id}-purchase", ledger.partyId, EntryType.PURCHASE, order.total, now, order.id))
    }
}

enum class ExpenseCategory { SHIPPING, PACKAGING, RENT, SALARY, STORE, PURCHASE_OVERHEAD, PERSONAL, OTHER }

data class Expense(
    val id: String,
    val category: ExpenseCategory,
    val amount: Long,
    val at: Long,
    val note: String = ""
) {
    init { require(amount > 0) { "amount must be positive" } }

    val isPersonal: Boolean get() = category == ExpenseCategory.PERSONAL
}

data class ProfitReport(
    val salesTotal: Long,
    val costOfGoods: Long,
    val grossProfit: Long,
    val retailGrossProfit: Long,
    val wholesaleGrossProfit: Long,
    val businessExpenses: Long,
    val netProfit: Long,
    val personalExpenses: Long
)

object ReportEngine {
    /** Period is [from, to). Personal expenses are reported separately and never reduce shop profit. */
    fun profit(orders: List<Order>, expenses: List<Expense>, from: Long, to: Long): ProfitReport {
        val sales = orders.filter { it.countsAsSale && it.createdAt >= from && it.createdAt < to }
        val salesTotal = sales.sumOf { it.total }
        val cogs = sales.sumOf { it.costOfGoods }
        val retailGross = sales.filter { it.channel == Channel.RETAIL }.sumOf { it.total - it.costOfGoods }
        val wholesaleGross = sales.filter { it.channel == Channel.WHOLESALE }.sumOf { it.total - it.costOfGoods }
        val inPeriod = expenses.filter { it.at >= from && it.at < to }
        val business = inPeriod.filter { !it.isPersonal }.sumOf { it.amount }
        val personal = inPeriod.filter { it.isPersonal }.sumOf { it.amount }
        val gross = salesTotal - cogs
        return ProfitReport(salesTotal, cogs, gross, retailGross, wholesaleGross, business, gross - business, personal)
    }
}
