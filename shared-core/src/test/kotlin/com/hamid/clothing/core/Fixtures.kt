package com.hamid.clothing.core

internal object Fixtures {
    const val NOW = 1_800_000_000_000L
    const val MINUTE = 60_000L
    const val DAY = 86_400_000L

    fun product(
        id: String = "p1",
        retail: Long = 1_000_000L,
        wholesale: Long = 800_000L,
        cost: Long = 600_000L,
        stock: Int = 10
    ) = Product(
        id = id, code = "C-$id", name = "شومیز $id", categoryId = "c1",
        purchasePrice = cost - 50_000L, landedCost = cost,
        retailPrice = retail, wholesalePrice = wholesale,
        variants = listOf(Variant("کرم", "M", stock), Variant("کرم", "L", stock))
    )

    fun retailCustomer(id: String = "u1") = Customer(id = id, fullName = "مشتری $id", phone = "09120000000")

    fun wholesaleCustomer(
        id: String = "w1",
        policy: CreditPolicy = CreditPolicy.WITHIN_LIMIT,
        limit: Long = 10_000_000L
    ) = Customer(
        id = id, fullName = "عمده $id", phone = "09121111111",
        type = CustomerType.WHOLESALE, wholesaleStatus = WholesaleStatus.APPROVED,
        creditPolicy = policy, creditLimit = limit
    )

    fun order(
        id: String = "o1",
        channel: Channel = Channel.RETAIL,
        payment: PaymentMethod = PaymentMethod.ONLINE,
        shipping: ShippingMethod = ShippingMethod.COURIER,
        qty: Int = 2,
        unitPrice: Long = 1_000_000L,
        unitCost: Long = 600_000L,
        shippingCost: Long = 0L,
        productId: String = "p1"
    ) = Order(
        id = id, number = "ORD-$id", customerId = "u1", channel = channel,
        items = listOf(OrderItem(productId, "شومیز", "کرم", "M", qty, unitPrice, unitCost)),
        paymentMethod = payment, shippingMethod = shipping, createdAt = NOW,
        shippingCost = shippingCost
    )

    fun inventory(stock: Int = 10): InventoryLedger {
        val inv = InventoryLedger()
        inv.loadFrom(listOf(product(stock = stock)))
        return inv
    }

    val keyM = StockKey("p1", "کرم", "M")
}
