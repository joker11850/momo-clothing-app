package com.hamid.clothing.core

data class OrderFileLine(
    val productId: String,
    val productName: String,
    val color: String,
    val size: String,
    val quantity: Int,
    /** Price the customer saw. The shop never trusts it; it only uses it to point out differences. */
    val unitPrice: Long
)

/** An order sent by the customer app (as a file) to the admin app. */
data class OrderFile(
    val clientOrderId: String,
    val clientNumber: String,
    val createdAt: Long,
    val customerName: String,
    val customerPhone: String,
    val receiverName: String,
    val receiverPhone: String,
    val address: String,
    val paymentMethod: PaymentMethod,
    val shippingMethod: ShippingMethod,
    val lines: List<OrderFileLine>,
    val note: String = ""
)

/** Status of an order, sent back from the admin app to the customer app. */
data class OrderUpdateFile(
    val clientOrderId: String,
    val adminNumber: String,
    val status: OrderStatus,
    val paidAmount: Long,
    val total: Long,
    val shippingCost: Long,
    val trackingCode: String?,
    val at: Long
)

object OrderExchange {
    const val MAX_CHARS = 256 * 1024
    const val MAX_TEXT = 400
    const val MAX_LINES = 100
    const val MAX_QUANTITY = 1000

    /** Customer side: the order as a file for the shop. */
    fun fromOrder(order: Order, customerName: String, customerPhone: String, note: String = ""): OrderFile = OrderFile(
        clientOrderId = order.id,
        clientNumber = order.number,
        createdAt = order.createdAt,
        customerName = customerName,
        customerPhone = customerPhone,
        receiverName = order.receiverName,
        receiverPhone = order.receiverPhone,
        address = order.address,
        paymentMethod = order.paymentMethod,
        shippingMethod = order.shippingMethod,
        lines = order.items.map { OrderFileLine(it.productId, it.productName, it.color, it.size, it.quantity, it.unitPrice) },
        note = note
    )

    /** Admin side: the current state of an order that came from a customer, or null for orders typed in by the shop. */
    fun updateFor(order: Order, now: Long): OrderUpdateFile? {
        if (order.externalRef.isBlank()) return null
        return OrderUpdateFile(order.externalRef, order.number, order.status, order.paidAmount, order.total, order.shippingCost, order.trackingCode, now)
    }

    /**
     * Customer side: copies the shop's status, payment and tracking into the local order.
     * The shop's final amount wins: the difference is shown as discount (or added to shipping when the shop charged more).
     */
    fun applyUpdate(local: Order, update: OrderUpdateFile): Order {
        val base = local.subtotal
        val diff = base + update.shippingCost - update.total
        val priced = if (diff >= 0) {
            local.copy(extraDiscount = diff, shippingCost = update.shippingCost)
        } else {
            local.copy(extraDiscount = 0L, shippingCost = update.shippingCost - diff)
        }
        val history = if (priced.status != update.status) {
            priced.history + StatusChange(priced.status, update.status, update.at, "از فروشگاه (سفارش ${update.adminNumber})")
        } else {
            priced.history
        }
        return priced.copy(
            status = update.status,
            paidAmount = update.paidAmount,
            trackingCode = update.trackingCode ?: priced.trackingCode,
            history = history
        )
    }
}

data class ImportPlan(
    /** Existing customer with the same phone number. */
    val customer: Customer?,
    /** Customer that will be created when the phone number is new. */
    val newCustomer: Customer?,
    val lines: List<DraftLine>,
    /** Lines that could not be matched with a product of the shop. */
    val skipped: List<String>
) {
    val canImport: Boolean get() = (customer != null || newCustomer != null) && lines.isNotEmpty()
}

object OrderImporter {
    fun plan(file: OrderFile, customers: List<Customer>, products: List<Product>, now: Long, newId: () -> String): ImportPlan {
        val phone = Validators.normalizeMobile(file.customerPhone) ?: Validators.normalizeMobile(file.receiverPhone)
        val existing = if (phone == null) null else customers.firstOrNull { it.phone == phone }
        val created = if (existing == null && phone != null) {
            Customer(
                id = newId(),
                fullName = file.customerName.ifBlank { file.receiverName }.ifBlank { "مشتری جدید" },
                phone = phone,
                address = file.address,
                joinedAt = now,
                adminNote = "ثبت‌شده از اپ مشتری"
            )
        } else {
            null
        }

        val byId = products.associateBy { it.id }
        val lines = mutableListOf<DraftLine>()
        val skipped = mutableListOf<String>()
        for (l in file.lines) {
            val product = byId[l.productId]
            if (product == null || !product.active || product.variants.none { it.color == l.color && it.size == l.size }) {
                skipped.add("«${l.productName.ifBlank { l.productId }}» (${l.color} / ${l.size}) در فروشگاه نیست")
            } else {
                lines.add(DraftLine(l.productId, l.color, l.size, l.quantity))
            }
        }
        return ImportPlan(existing, created, lines, skipped)
    }
}
