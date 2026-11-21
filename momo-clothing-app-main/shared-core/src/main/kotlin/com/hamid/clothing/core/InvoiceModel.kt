package com.hamid.clothing.core

data class InvoiceRow(
    val index: String,
    val name: String,
    val variant: String,
    val quantity: String,
    val unitPrice: String,
    val total: String
)

/** Everything an invoice picture needs, already formatted as Persian text. */
data class InvoiceModel(
    val shopName: String,
    val shopLines: List<String>,
    val title: String,
    val number: String,
    val date: String,
    val customerName: String,
    val details: List<Pair<String, String>>,
    val rows: List<InvoiceRow>,
    val totals: List<Pair<String, String>>,
    val footer: String
)

object InvoiceBuilder {
    private fun fa(n: Long) = PersianFormat.digits(n.toString())

    /**
     * @param accountBalance customer's account balance after the order (positive = owes), or null to leave it out
     */
    fun build(order: Order, customerName: String, shop: StoreSettings, accountBalance: Long?): InvoiceModel {
        val shopLines = mutableListOf<String>()
        if (shop.phone.isNotBlank()) shopLines.add("تلفن: " + PersianFormat.digits(shop.phone))
        if (shop.address.isNotBlank()) shopLines.add(shop.address)

        val details = mutableListOf<Pair<String, String>>()
        if (order.receiverName.isNotBlank()) details.add("تحویل‌گیرنده" to order.receiverName)
        if (order.receiverPhone.isNotBlank()) details.add("تماس" to PersianFormat.digits(order.receiverPhone))
        if (order.address.isNotBlank()) details.add("آدرس" to order.address)
        details.add("روش پرداخت" to FaLabels.payment(order.paymentMethod))
        details.add("روش ارسال" to FaLabels.shipping(order.shippingMethod))
        if (!order.trackingCode.isNullOrBlank()) details.add("کد رهگیری" to (order.trackingCode ?: ""))
        details.add("وضعیت" to FaLabels.status(order.status))

        val rows = order.items.mapIndexed { index, item ->
            InvoiceRow(
                index = fa((index + 1).toLong()),
                name = item.productName,
                variant = "${item.color} / ${item.size}",
                quantity = fa(item.quantity.toLong()),
                unitPrice = PersianFormat.money(item.unitPrice),
                total = PersianFormat.money(item.lineTotal)
            )
        }

        val totals = mutableListOf<Pair<String, String>>()
        totals.add("جمع کالاها" to PersianFormat.money(order.subtotal))
        if (order.extraDiscount > 0) totals.add("تخفیف" to PersianFormat.money(order.extraDiscount))
        if (order.shippingCost > 0) totals.add("هزینه ارسال" to PersianFormat.money(order.shippingCost))
        totals.add("مبلغ کل (تومان)" to PersianFormat.money(order.total))
        totals.add("پرداخت‌شده" to PersianFormat.money(order.paidAmount))
        totals.add("مانده این فاکتور" to PersianFormat.money(order.remaining))
        if (accountBalance != null) {
            val text = when {
                accountBalance > 0 -> "بدهکار " + PersianFormat.money(accountBalance)
                accountBalance < 0 -> "بستانکار " + PersianFormat.money(-accountBalance)
                else -> "تسویه"
            }
            totals.add("مانده حساب مشتری" to text)
        }

        return InvoiceModel(
            shopName = shop.shopName.ifBlank { "فروشگاه" },
            shopLines = shopLines,
            title = "فاکتور فروش",
            number = order.number,
            date = PersianFormat.digits(JalaliCalendar.fromEpochMillis(order.createdAt).format()),
            customerName = customerName,
            details = details,
            rows = rows,
            totals = totals,
            footer = "با تشکر از خرید شما"
        )
    }
}
