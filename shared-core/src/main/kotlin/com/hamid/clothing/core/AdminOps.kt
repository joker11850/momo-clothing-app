package com.hamid.clothing.core

data class DraftLine(
    val productId: String,
    val color: String,
    val size: String,
    val quantity: Int,
    /** When set, replaces the automatic price (a special price agreed with the customer). */
    val unitPriceOverride: Long? = null
)

object OrderBuilder {
    /** Prices every draft line. Lines whose product does not exist are skipped. */
    fun priceLines(
        customer: Customer?,
        lines: List<DraftLine>,
        products: List<Product>,
        discounts: List<Discount>,
        now: Long,
        /** productId -> special price of this customer */
        customPrices: Map<String, Long> = emptyMap()
    ): List<OrderItem> {
        val byId = products.associateBy { it.id }
        return lines.mapNotNull { line ->
            val product = byId[line.productId]
            if (product == null || line.quantity <= 0) {
                null
            } else {
                val quote = PricingEngine.quote(product, customer, customPrices, discounts, null, now)
                val unit = line.unitPriceOverride ?: quote.unitPrice
                OrderItem(
                    productId = product.id,
                    productName = product.name,
                    color = line.color,
                    size = line.size,
                    quantity = line.quantity,
                    unitPrice = unit,
                    unitCost = product.landedCost,
                    discountPercent = if (line.unitPriceOverride == null) quote.discountPercent else 0
                )
            }
        }
    }

    fun validate(
        customer: Customer?,
        lines: List<DraftLine>,
        items: List<OrderItem>,
        products: List<Product>,
        shipping: ShippingMethod,
        payment: PaymentMethod,
        receiverName: String,
        receiverPhone: String,
        address: String,
        extraDiscount: Long,
        shippingCost: Long,
        balance: Long,
        available: (StockKey) -> Int
    ): List<String> {
        val errors = mutableListOf<String>()
        if (customer == null) errors.add("مشتری را انتخاب کنید") else if (customer.blocked) errors.add("این مشتری مسدود است")
        if (lines.isEmpty()) errors.add("حداقل یک کالا اضافه کنید")

        val byId = products.associateBy { it.id }
        for (line in lines) {
            val product = byId[line.productId]
            if (product == null) {
                errors.add("کالای انتخاب‌شده پیدا نشد")
            } else if (product.variants.none { it.color == line.color && it.size == line.size }) {
                errors.add("رنگ یا سایز «${product.name}» نامعتبر است")
            }
        }
        val demand = lines.groupBy({ StockKey(it.productId, it.color, it.size) }, { it.quantity }).mapValues { it.value.sum() }
        for ((key, qty) in demand) {
            val product = byId[key.productId] ?: continue
            if (available(key) < qty) errors.add("موجودی کافی نیست: ${product.name} (${key.color} / ${key.size})")
        }

        if (receiverName.isBlank()) errors.add("نام تحویل‌گیرنده را وارد کنید")
        if (Validators.normalizeMobile(receiverPhone) == null) errors.add("شماره تماس تحویل‌گیرنده معتبر نیست")
        if (shipping != ShippingMethod.PICKUP && address.isBlank()) errors.add("آدرس تحویل را وارد کنید")

        val subtotal = items.sumOf { it.lineTotal }
        if (extraDiscount < 0 || extraDiscount > subtotal) errors.add("مبلغ تخفیف نامعتبر است")
        val total = (subtotal - extraDiscount + shippingCost).coerceAtLeast(0L)

        if (payment == PaymentMethod.ON_ACCOUNT && customer != null) {
            when (CreditService.evaluate(customer, balance, total)) {
                CreditDecision.ALLOWED, CreditDecision.NEEDS_ADMIN_APPROVAL -> Unit
                CreditDecision.NOT_WHOLESALE -> errors.add("خرید دفتری فقط برای مشتری عمده‌ی تأییدشده است")
                CreditDecision.BLOCKED -> errors.add("این مشتری مسدود است")
                CreditDecision.NO_CREDIT -> errors.add("برای این مشتری خرید دفتری فعال نیست")
                CreditDecision.LIMIT_EXCEEDED -> errors.add("مبلغ از سقف اعتبار مشتری بیشتر است")
            }
        }
        return errors
    }

    fun build(
        id: String,
        number: String,
        customer: Customer,
        items: List<OrderItem>,
        shipping: ShippingMethod,
        shippingCost: Long,
        payment: PaymentMethod,
        extraDiscount: Long,
        receiverName: String,
        receiverPhone: String,
        address: String,
        now: Long
    ): Order = Order(
        id = id,
        number = number,
        customerId = customer.id,
        channel = if (customer.canSeeWholesale) Channel.WHOLESALE else Channel.RETAIL,
        items = items,
        paymentMethod = payment,
        shippingMethod = shipping,
        createdAt = now,
        shippingCost = shippingCost,
        extraDiscount = extraDiscount,
        receiverName = receiverName.trim(),
        receiverPhone = Validators.normalizeMobile(receiverPhone) ?: receiverPhone.trim(),
        address = if (shipping == ShippingMethod.PICKUP) "" else address.trim()
    )
}

data class PurchaseLine(
    val productId: String,
    val color: String,
    val size: String,
    val quantity: Int,
    val unitPrice: Long
)

data class PurchaseResult(val products: List<Product>, val goodsTotal: Long, val total: Long, val totalUnits: Int)

/** Buying goods from a supplier: raises stock and refreshes purchase and landed cost. */
object PurchaseMath {
    fun validate(lines: List<PurchaseLine>, products: List<Product>): List<String> {
        val errors = mutableListOf<String>()
        if (lines.isEmpty()) errors.add("حداقل یک کالا اضافه کنید")
        val ids = products.map { it.id }.toSet()
        if (lines.any { it.productId !in ids }) errors.add("کالای انتخاب‌شده پیدا نشد")
        if (lines.any { it.quantity <= 0 }) errors.add("تعداد باید بیشتر از صفر باشد")
        if (lines.any { it.unitPrice <= 0 }) errors.add("قیمت خرید هر ردیف را وارد کنید")
        if (lines.any { it.color.isBlank() || it.size.isBlank() }) errors.add("رنگ و سایز نباید خالی باشد")
        return errors
    }

    /**
     * Stock is increased (a missing color/size is created). Extra costs (shipping, packaging...) are
     * spread over the lines by value. Purchase and landed cost become the cost of this purchase.
     */
    fun apply(lines: List<PurchaseLine>, extraCost: Long, products: List<Product>): PurchaseResult {
        require(extraCost >= 0) { "extra cost must not be negative" }
        val goods = lines.sumOf { it.unitPrice * it.quantity }
        val units = lines.sumOf { it.quantity }
        fun share(line: PurchaseLine): Long {
            if (extraCost == 0L) return 0L
            return if (goods > 0) extraCost * (line.unitPrice * line.quantity) / goods
            else extraCost * line.quantity / units
        }

        val updated = products.map { product ->
            val mine = lines.filter { it.productId == product.id }
            if (mine.isEmpty()) {
                product
            } else {
                var variants = product.variants
                for (line in mine) {
                    val exists = variants.any { it.color == line.color && it.size == line.size }
                    variants = if (exists) {
                        variants.map {
                            if (it.color == line.color && it.size == line.size) it.copy(stock = it.stock + line.quantity) else it
                        }
                    } else {
                        variants + Variant(line.color, line.size, line.quantity)
                    }
                }
                val qty = mine.sumOf { it.quantity }.toLong()
                val value = mine.sumOf { it.unitPrice * it.quantity }
                val extra = mine.sumOf { share(it) }
                product.copy(
                    variants = variants,
                    purchasePrice = (value + qty / 2) / qty,
                    landedCost = (value + extra + qty / 2) / qty
                )
            }
        }
        return PurchaseResult(updated, goods, goods + extraCost, units)
    }
}

object Periods {
    private const val DAY = 86_400_000L

    /** [start, end) of the Jalali day that contains [now]. */
    fun day(now: Long): Pair<Long, Long> {
        val start = JalaliCalendar.toEpochMillis(JalaliCalendar.fromEpochMillis(now))
        return start to start + DAY
    }

    /** [start, end) of the Jalali month that contains [now]. */
    fun month(now: Long): Pair<Long, Long> {
        val d = JalaliCalendar.fromEpochMillis(now)
        val start = JalaliCalendar.toEpochMillis(JalaliDate(d.year, d.month, 1))
        val nextYear = if (d.month == 12) d.year + 1 else d.year
        val nextMonth = if (d.month == 12) 1 else d.month + 1
        return start to JalaliCalendar.toEpochMillis(JalaliDate(nextYear, nextMonth, 1))
    }
}

data class ProductSales(val productId: String, val name: String, val quantity: Int, val revenue: Long)

object SalesStats {
    /** Best sellers first (by units, then revenue) among orders that count as sales in [from, to). */
    fun byProduct(orders: List<Order>, from: Long, to: Long): List<ProductSales> {
        val sales = orders.filter { it.countsAsSale && it.createdAt >= from && it.createdAt < to }
        return sales.flatMap { it.items }
            .groupBy { it.productId }
            .map { (id, items) ->
                ProductSales(id, items.first().productName, items.sumOf { it.quantity }, items.sumOf { it.lineTotal })
            }
            .sortedWith(compareByDescending<ProductSales> { it.quantity }.thenByDescending { it.revenue })
    }
}
