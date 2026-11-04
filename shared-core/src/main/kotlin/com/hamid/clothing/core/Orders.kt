package com.hamid.clothing.core

enum class OrderStatus {
    NEW, AWAITING_PAYMENT, PAID, AWAITING_SELLER_APPROVAL,
    PREPARING, SHIPPED, DELIVERED, CANCELLED, RETURNED
}

enum class PaymentMethod { ONLINE, CARD_TO_CARD, CASH, ON_ACCOUNT }
enum class ShippingMethod { COURIER, POST, PICKUP }

data class OrderItem(
    val productId: String,
    val productName: String,
    val color: String,
    val size: String,
    val quantity: Int,
    /** Final unit price after discount, as quoted by PricingEngine. */
    val unitPrice: Long,
    /** Landed cost per unit at the time of sale, used for profit reports. */
    val unitCost: Long,
    val discountPercent: Int = 0
) {
    init {
        require(quantity > 0) { "quantity must be positive" }
        require(unitPrice >= 0 && unitCost >= 0) { "prices must not be negative" }
    }

    val lineTotal: Long get() = unitPrice * quantity
    fun stockKey(): StockKey = StockKey(productId, color, size)
}

data class StatusChange(val from: OrderStatus?, val to: OrderStatus, val at: Long, val note: String = "")

data class Order(
    val id: String,
    val number: String,
    val customerId: String,
    val channel: Channel,
    val items: List<OrderItem>,
    val paymentMethod: PaymentMethod,
    val shippingMethod: ShippingMethod,
    val createdAt: Long,
    val status: OrderStatus = OrderStatus.NEW,
    val shippingCost: Long = 0L,
    val extraDiscount: Long = 0L,
    val paidAmount: Long = 0L,
    val trackingCode: String? = null,
    val stockCommitted: Boolean = false,
    val history: List<StatusChange> = emptyList(),
    val receiverName: String = "",
    val receiverPhone: String = "",
    val address: String = ""
) {
    val subtotal: Long get() = items.sumOf { it.lineTotal }
    val total: Long get() = (subtotal - extraDiscount + shippingCost).coerceAtLeast(0L)
    val remaining: Long get() = (total - paidAmount).coerceAtLeast(0L)
    val costOfGoods: Long get() = items.sumOf { it.unitCost * it.quantity }

    /** Orders that count as realised sales in reports. */
    val countsAsSale: Boolean
        get() = status == OrderStatus.PAID || status == OrderStatus.PREPARING ||
            status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED
}

object OrderStateMachine {
    private val retail: Map<OrderStatus, Set<OrderStatus>> = mapOf(
        OrderStatus.NEW to setOf(OrderStatus.AWAITING_PAYMENT, OrderStatus.CANCELLED),
        OrderStatus.AWAITING_PAYMENT to setOf(OrderStatus.PAID, OrderStatus.CANCELLED),
        OrderStatus.PAID to setOf(OrderStatus.PREPARING, OrderStatus.CANCELLED),
        OrderStatus.PREPARING to setOf(OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.CANCELLED),
        OrderStatus.SHIPPED to setOf(OrderStatus.DELIVERED, OrderStatus.RETURNED),
        OrderStatus.DELIVERED to setOf(OrderStatus.RETURNED)
    )

    private val wholesale: Map<OrderStatus, Set<OrderStatus>> = mapOf(
        OrderStatus.NEW to setOf(OrderStatus.AWAITING_SELLER_APPROVAL, OrderStatus.CANCELLED),
        OrderStatus.AWAITING_SELLER_APPROVAL to setOf(
            OrderStatus.AWAITING_PAYMENT, OrderStatus.PREPARING, OrderStatus.CANCELLED
        ),
        OrderStatus.AWAITING_PAYMENT to setOf(OrderStatus.PAID, OrderStatus.CANCELLED),
        OrderStatus.PAID to setOf(OrderStatus.PREPARING, OrderStatus.CANCELLED),
        OrderStatus.PREPARING to setOf(OrderStatus.SHIPPED, OrderStatus.DELIVERED, OrderStatus.CANCELLED),
        OrderStatus.SHIPPED to setOf(OrderStatus.DELIVERED, OrderStatus.RETURNED),
        OrderStatus.DELIVERED to setOf(OrderStatus.RETURNED)
    )

    fun allowed(channel: Channel, from: OrderStatus): Set<OrderStatus> =
        (if (channel == Channel.RETAIL) retail else wholesale)[from] ?: emptySet()

    fun canMove(channel: Channel, from: OrderStatus, to: OrderStatus): Boolean = to in allowed(channel, from)
}

sealed class OrderResult {
    data class Success(val order: Order) : OrderResult()
    data class Failure(val reason: String) : OrderResult()
}

/**
 * Drives orders through the state machine and keeps inventory in sync.
 * - retail orders reserve stock for [retailHoldMillis] and go straight to payment
 * - wholesale orders wait for seller approval; reservation expires after [wholesaleHoldMillis] (null = never)
 * - stock is deducted when the order ships (or is handed over for pickup)
 */
class OrderProcessor(
    private val inventory: InventoryLedger,
    private val retailHoldMillis: Long = 30 * 60_000L,
    private val wholesaleHoldMillis: Long? = null,
    private val wholesalePaymentHoldMillis: Long = 24 * 3_600_000L
) {
    private fun fail(reason: String): OrderResult = OrderResult.Failure(reason)

    private fun transition(
        order: Order,
        to: OrderStatus,
        now: Long,
        note: String = "",
        edit: (Order) -> Order = { it }
    ): OrderResult {
        if (!OrderStateMachine.canMove(order.channel, order.status, to)) {
            return fail("تغییر وضعیت از ${order.status} به $to مجاز نیست")
        }
        val edited = edit(order)
        val moved = edited.copy(
            status = to,
            history = order.history + StatusChange(order.status, to, now, note)
        )
        return OrderResult.Success(moved)
    }

    fun submit(order: Order, now: Long): OrderResult {
        if (order.status != OrderStatus.NEW) return fail("سفارش قبلاً ثبت شده است")
        if (order.items.isEmpty()) return fail("سفارش بدون کالا است")
        if (order.channel == Channel.RETAIL && order.paymentMethod == PaymentMethod.ON_ACCOUNT) {
            return fail("خرید دفتری فقط برای مشتری عمده مجاز است")
        }
        val target = if (order.channel == Channel.RETAIL) {
            OrderStatus.AWAITING_PAYMENT
        } else {
            OrderStatus.AWAITING_SELLER_APPROVAL
        }
        val expiresAt = if (order.channel == Channel.RETAIL) {
            now + retailHoldMillis
        } else {
            wholesaleHoldMillis?.let { now + it } ?: Long.MAX_VALUE
        }
        val reserve = inventory.reserveOrder(
            order.id, order.channel, order.items.map { it.stockKey() to it.quantity }, expiresAt
        )
        if (!reserve.ok) return fail("موجودی کافی نیست")
        return transition(order, target, now)
    }

    /** Seller approves a wholesale order. For ON_ACCOUNT orders the caller must confirm credit first. */
    fun approveWholesale(order: Order, now: Long, creditApproved: Boolean = false): OrderResult {
        if (order.channel != Channel.WHOLESALE) return fail("این سفارش عمده نیست")
        if (order.status != OrderStatus.AWAITING_SELLER_APPROVAL) return fail("سفارش در انتظار تأیید نیست")
        return if (order.paymentMethod == PaymentMethod.ON_ACCOUNT) {
            if (!creditApproved) return fail("اعتبار مشتری تأیید نشده است")
            val result = transition(order, OrderStatus.PREPARING, now, "تأیید خرید دفتری")
            if (result is OrderResult.Success) inventory.extend(order.id, Long.MAX_VALUE)
            result
        } else {
            val result = transition(order, OrderStatus.AWAITING_PAYMENT, now, "تأیید فروشنده")
            if (result is OrderResult.Success) inventory.extend(order.id, now + wholesalePaymentHoldMillis)
            result
        }
    }

    fun recordPayment(order: Order, amount: Long, now: Long): OrderResult {
        if (order.status != OrderStatus.AWAITING_PAYMENT) return fail("سفارش در انتظار پرداخت نیست")
        if (amount <= 0) return fail("مبلغ پرداخت نامعتبر است")
        val updated = order.copy(paidAmount = order.paidAmount + amount)
        return if (updated.paidAmount >= updated.total) {
            transition(updated, OrderStatus.PAID, now, "پرداخت کامل")
        } else {
            OrderResult.Success(updated)
        }
    }

    fun startPreparing(order: Order, now: Long): OrderResult = transition(order, OrderStatus.PREPARING, now)

    fun ship(order: Order, trackingCode: String?, now: Long): OrderResult {
        if (order.shippingMethod == ShippingMethod.PICKUP) return fail("تحویل حضوری ارسال ندارد")
        val result = transition(order, OrderStatus.SHIPPED, now) {
            it.copy(trackingCode = trackingCode, stockCommitted = true)
        }
        if (result is OrderResult.Success && !order.stockCommitted) inventory.commit(order.id)
        return result
    }

    fun deliver(order: Order, now: Long): OrderResult {
        if (order.status == OrderStatus.PREPARING && order.shippingMethod != ShippingMethod.PICKUP) {
            return fail("ابتدا سفارش باید ارسال شود")
        }
        val result = transition(order, OrderStatus.DELIVERED, now) { it.copy(stockCommitted = true) }
        if (result is OrderResult.Success && !order.stockCommitted) inventory.commit(order.id)
        return result
    }

    fun cancel(order: Order, note: String, now: Long): OrderResult {
        val result = transition(order, OrderStatus.CANCELLED, now, note)
        if (result is OrderResult.Success) inventory.release(order.id)
        return result
    }

    fun markReturned(order: Order, note: String, now: Long): OrderResult {
        val result = transition(order, OrderStatus.RETURNED, now, note) { it.copy(stockCommitted = false) }
        if (result is OrderResult.Success && order.stockCommitted) {
            for (item in order.items) inventory.restock(item.stockKey(), item.quantity)
        }
        return result
    }

    /** Cancels orders whose stock reservation has expired. */
    fun expireUnpaid(orders: List<Order>, now: Long): List<Order> {
        val expiredIds = inventory.expire(now).toSet()
        return orders.map { order ->
            if (order.id in expiredIds && !order.stockCommitted) {
                val result = transition(order, OrderStatus.CANCELLED, now, "انقضای مهلت رزرو")
                if (result is OrderResult.Success) result.order else order
            } else {
                order
            }
        }
    }
}
