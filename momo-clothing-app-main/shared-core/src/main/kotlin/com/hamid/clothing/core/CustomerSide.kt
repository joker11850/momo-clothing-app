package com.hamid.clothing.core

data class CartLine(val productId: String, val color: String, val size: String, val quantity: Int) {
    init { require(quantity > 0) { "quantity must be positive" } }

    fun key(): StockKey = StockKey(productId, color, size)
}

data class WholesaleRequest(val businessName: String, val city: String, val note: String, val at: Long)

/** Everything the customer app keeps on the phone until a server exists. */
data class CustomerAppData(
    val customer: Customer? = null,
    val addresses: List<String> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val cart: List<CartLine> = emptyList(),
    val orders: List<Order> = emptyList(),
    val ledger: List<LedgerEntry> = emptyList(),
    val wholesaleRequest: WholesaleRequest? = null,
    /** false after sign-out; the data stays on the phone so the same number can sign in again. */
    val loggedIn: Boolean = false
)

object CartMath {
    /** Adds a line, merging with an existing one for the same product/color/size and capping at [maxQty]. */
    fun add(cart: List<CartLine>, line: CartLine, maxQty: Int): List<CartLine> {
        val existing = cart.firstOrNull { it.key() == line.key() }
        val newQty = ((existing?.quantity ?: 0) + line.quantity).coerceAtMost(maxQty.coerceAtLeast(0))
        if (newQty <= 0) return cart
        return if (existing == null) {
            cart + line.copy(quantity = newQty)
        } else {
            cart.map { if (it.key() == line.key()) it.copy(quantity = newQty) else it }
        }
    }

    fun setQuantity(cart: List<CartLine>, key: StockKey, quantity: Int): List<CartLine> =
        if (quantity <= 0) cart.filter { it.key() != key }
        else cart.map { if (it.key() == key) it.copy(quantity = quantity) else it }

    fun remove(cart: List<CartLine>, key: StockKey): List<CartLine> = cart.filter { it.key() != key }
}

data class PricedLine(
    val product: Product,
    val line: CartLine,
    val quote: PriceQuote,
    val available: Int
) {
    val lineTotal: Long get() = quote.unitPrice * line.quantity
    val inStock: Boolean get() = available >= line.quantity
}

data class PricedCart(val lines: List<PricedLine>) {
    val subtotal: Long get() = lines.sumOf { it.lineTotal }
    val itemCount: Int get() = lines.sumOf { it.line.quantity }
    val hasStockProblem: Boolean get() = lines.any { !it.inStock }
    /** How much cheaper the cart is than paying the undiscounted price. */
    val savings: Long get() = lines.sumOf { (it.quote.basePrice - it.quote.unitPrice) * it.line.quantity }
}

object CartPricing {
    fun price(
        cart: List<CartLine>,
        products: List<Product>,
        inventory: InventoryLedger,
        customer: Customer?,
        discounts: List<Discount>,
        coupon: String?,
        now: Long
    ): PricedCart {
        val byId = products.associateBy { it.id }
        val lines = cart.mapNotNull { line ->
            val product = byId[line.productId]
            if (product == null || !product.active) {
                null
            } else {
                val quote = PricingEngine.quote(product, customer, emptyMap(), discounts, coupon, now)
                PricedLine(product, line, quote, inventory.available(line.key()))
            }
        }
        return PricedCart(lines)
    }
}

data class CheckoutInput(
    val orderId: String,
    val number: String,
    val customer: Customer,
    val receiverName: String,
    val receiverPhone: String,
    val address: String,
    val shipping: ShippingMethod,
    val shippingCost: Long,
    val payment: PaymentMethod,
    val now: Long
)

object CheckoutService {
    fun nextNumber(existing: List<Order>): String = "ORD-" + (1001 + existing.size)

    fun allowedPayments(customer: Customer, balance: Long, total: Long): List<PaymentMethod> {
        val list = mutableListOf(PaymentMethod.CARD_TO_CARD, PaymentMethod.ONLINE)
        val decision = CreditService.evaluate(customer, balance, total)
        if (decision == CreditDecision.ALLOWED || decision == CreditDecision.NEEDS_ADMIN_APPROVAL) {
            list.add(PaymentMethod.ON_ACCOUNT)
        }
        return list
    }

    /** Persian error messages; empty means the order can be placed. */
    fun validate(
        priced: PricedCart,
        input: CheckoutInput,
        balance: Long,
        onlineEnabled: Boolean = false
    ): List<String> {
        val errors = mutableListOf<String>()
        if (priced.lines.isEmpty()) errors.add("سبد خرید خالی است")
        if (priced.hasStockProblem) errors.add("موجودی بعضی کالاها کافی نیست؛ سبد خرید را اصلاح کنید")
        if (input.customer.blocked) errors.add("حساب کاربری شما مسدود است")
        if (input.receiverName.isBlank()) errors.add("نام تحویل‌گیرنده را وارد کنید")
        if (Validators.normalizeMobile(input.receiverPhone) == null) errors.add("شماره تماس تحویل‌گیرنده معتبر نیست")
        if (input.shipping != ShippingMethod.PICKUP && input.address.isBlank()) errors.add("آدرس تحویل را وارد کنید")
        if (input.payment == PaymentMethod.ONLINE && !onlineEnabled) {
            errors.add("پرداخت آنلاین هنوز فعال نیست؛ کارت‌به‌کارت را انتخاب کنید")
        } else if (input.payment !in allowedPayments(input.customer, balance, priced.subtotal + input.shippingCost)) {
            errors.add("روش پرداخت انتخاب‌شده برای شما مجاز نیست")
        }
        return errors
    }

    fun buildOrder(priced: PricedCart, input: CheckoutInput): Order = Order(
        id = input.orderId,
        number = input.number,
        customerId = input.customer.id,
        channel = if (input.customer.canSeeWholesale) Channel.WHOLESALE else Channel.RETAIL,
        items = priced.lines.map {
            OrderItem(
                productId = it.product.id,
                productName = it.product.name,
                color = it.line.color,
                size = it.line.size,
                quantity = it.line.quantity,
                unitPrice = it.quote.unitPrice,
                unitCost = it.product.landedCost,
                discountPercent = it.quote.discountPercent
            )
        },
        paymentMethod = input.payment,
        shippingMethod = input.shipping,
        createdAt = input.now,
        shippingCost = input.shippingCost,
        receiverName = input.receiverName.trim(),
        receiverPhone = Validators.normalizeMobile(input.receiverPhone) ?: input.receiverPhone.trim(),
        address = if (input.shipping == ShippingMethod.PICKUP) "" else input.address.trim()
    )
}

object FaLabels {
    fun status(s: OrderStatus): String = when (s) {
        OrderStatus.NEW -> "جدید"
        OrderStatus.AWAITING_PAYMENT -> "در انتظار پرداخت"
        OrderStatus.PAID -> "پرداخت‌شده"
        OrderStatus.AWAITING_SELLER_APPROVAL -> "در انتظار تأیید فروشنده"
        OrderStatus.PREPARING -> "در حال آماده‌سازی"
        OrderStatus.SHIPPED -> "ارسال‌شده"
        OrderStatus.DELIVERED -> "تحویل‌شده"
        OrderStatus.CANCELLED -> "لغوشده"
        OrderStatus.RETURNED -> "مرجوع‌شده"
    }

    fun payment(p: PaymentMethod): String = when (p) {
        PaymentMethod.ONLINE -> "پرداخت آنلاین"
        PaymentMethod.CARD_TO_CARD -> "کارت‌به‌کارت"
        PaymentMethod.CASH -> "نقدی"
        PaymentMethod.ON_ACCOUNT -> "خرید دفتری"
    }

    fun shipping(s: ShippingMethod): String = when (s) {
        ShippingMethod.COURIER -> "پیک"
        ShippingMethod.POST -> "پست"
        ShippingMethod.PICKUP -> "تحویل حضوری"
    }
}

/** Plain-text invoice that can be shared through any messenger. */
object InvoiceText {
    fun build(order: Order, customerName: String, storeName: String): String {
        val sb = StringBuilder()
        sb.append("فاکتور ").append(storeName).append('\n')
        sb.append("شماره سفارش: ").append(order.number).append('\n')
        sb.append("تاریخ: ").append(PersianFormat.digits(JalaliCalendar.fromEpochMillis(order.createdAt).format())).append('\n')
        sb.append("مشتری: ").append(customerName).append('\n')
        if (order.receiverName.isNotBlank()) sb.append("تحویل‌گیرنده: ").append(order.receiverName).append(" - ").append(PersianFormat.digits(order.receiverPhone)).append('\n')
        if (order.address.isNotBlank()) sb.append("آدرس: ").append(order.address).append('\n')
        sb.append("-----\n")
        for ((index, item) in order.items.withIndex()) {
            sb.append(PersianFormat.digits((index + 1).toString())).append(") ")
                .append(item.productName).append(" | ").append(item.color).append(" / ").append(item.size)
                .append(" | ").append(PersianFormat.digits(item.quantity.toString())).append(" × ")
                .append(PersianFormat.money(item.unitPrice)).append(" = ")
                .append(PersianFormat.money(item.lineTotal)).append('\n')
        }
        sb.append("-----\n")
        sb.append("جمع کالاها: ").append(PersianFormat.money(order.subtotal)).append(" تومان\n")
        if (order.extraDiscount > 0) sb.append("تخفیف: ").append(PersianFormat.money(order.extraDiscount)).append(" تومان\n")
        if (order.shippingCost > 0) sb.append("هزینه ارسال: ").append(PersianFormat.money(order.shippingCost)).append(" تومان\n")
        sb.append("مبلغ کل: ").append(PersianFormat.money(order.total)).append(" تومان\n")
        sb.append("پرداخت‌شده: ").append(PersianFormat.money(order.paidAmount)).append(" تومان\n")
        sb.append("مانده: ").append(PersianFormat.money(order.remaining)).append(" تومان\n")
        sb.append("روش پرداخت: ").append(FaLabels.payment(order.paymentMethod)).append('\n')
        sb.append("روش ارسال: ").append(FaLabels.shipping(order.shippingMethod)).append('\n')
        sb.append("وضعیت: ").append(FaLabels.status(order.status))
        return sb.toString()
    }
}
