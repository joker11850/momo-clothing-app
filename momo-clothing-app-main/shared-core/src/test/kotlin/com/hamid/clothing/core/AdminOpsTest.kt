package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminOpsTest {
    private val now = Fixtures.NOW
    private val products = SampleData.products()
    private val customers = SampleData.customers(now)
    private val c2 = customers.first { it.id == "c-2" } // wholesale, limit 20M
    private val c1 = customers.first { it.id == "c-1" } // retail

    private fun inv() = InventoryLedger().also { it.loadFrom(products) }

    private fun validate(
        customer: Customer?,
        lines: List<DraftLine>,
        payment: PaymentMethod = PaymentMethod.CASH,
        shipping: ShippingMethod = ShippingMethod.COURIER,
        name: String = "علی",
        phone: String = "09120001234",
        address: String = "اصفهان",
        extraDiscount: Long = 0L,
        shippingCost: Long = 0L,
        balance: Long = 0L,
        inventory: InventoryLedger = inv()
    ): List<String> {
        val items = OrderBuilder.priceLines(customer, lines, products, SampleData.discounts(), now)
        return OrderBuilder.validate(
            customer, lines, items, products, shipping, payment, name, phone, address,
            extraDiscount, shippingCost, balance
        ) { inventory.available(it) }
    }

    @Test fun priceLinesUsesCustomerPriceAndOverride() {
        val line = DraftLine("p-4", "بژ", "S", 2)
        val retail = OrderBuilder.priceLines(c1, listOf(line), products, SampleData.discounts(), now)
        assertEquals(1_190_000L, retail[0].unitPrice)
        val wholesale = OrderBuilder.priceLines(c2, listOf(line), products, SampleData.discounts(), now)
        assertEquals(960_000L, wholesale[0].unitPrice)
        val special = OrderBuilder.priceLines(c2, listOf(line.copy(unitPriceOverride = 900_000L)), products, SampleData.discounts(), now)
        assertEquals(900_000L, special[0].unitPrice)
        assertEquals(0, special[0].discountPercent)
        assertEquals(740_000L, special[0].unitCost)
        // product discount is applied automatically (p-1 has 15%)
        val discounted = OrderBuilder.priceLines(c1, listOf(DraftLine("p-1", "کرم", "M", 1)), products, SampleData.discounts(), now)
        assertEquals(1_300_500L, discounted[0].unitPrice)
        assertEquals(15, discounted[0].discountPercent)
        // unknown product is skipped
        assertTrue(OrderBuilder.priceLines(c1, listOf(DraftLine("zzz", "x", "y", 1)), products, emptyList(), now).isEmpty())
    }

    @Test fun validOrderHasNoErrors() {
        assertEquals(emptyList(), validate(c1, listOf(DraftLine("p-4", "بژ", "S", 2))))
    }

    @Test fun missingPiecesAreReported() {
        val errors = validate(null, emptyList(), name = " ", phone = "12", address = "")
        assertTrue("مشتری را انتخاب کنید" in errors)
        assertTrue("حداقل یک کالا اضافه کنید" in errors)
        assertTrue("نام تحویل‌گیرنده را وارد کنید" in errors)
        assertTrue("شماره تماس تحویل‌گیرنده معتبر نیست" in errors)
        assertTrue("آدرس تحویل را وارد کنید" in errors)
        // pickup does not need an address
        assertFalse("آدرس تحویل را وارد کنید" in validate(c1, listOf(DraftLine("p-4", "بژ", "S", 1)), shipping = ShippingMethod.PICKUP, address = ""))
    }

    @Test fun stockAndVariantChecks() {
        val tooMany = validate(c1, listOf(DraftLine("p-4", "بژ", "S", 10))) // only 9
        assertTrue(tooMany.any { it.startsWith("موجودی کافی نیست") })
        val aggregated = validate(c1, listOf(DraftLine("p-4", "بژ", "S", 5), DraftLine("p-4", "بژ", "S", 5)))
        assertTrue(aggregated.any { it.startsWith("موجودی کافی نیست") })
        val badVariant = validate(c1, listOf(DraftLine("p-4", "بژ", "XXL", 1)))
        assertTrue(badVariant.any { it.contains("نامعتبر") })
        val blocked = validate(c1.copy(blocked = true), listOf(DraftLine("p-4", "بژ", "S", 1)))
        assertTrue("این مشتری مسدود است" in blocked)
    }

    @Test fun reservedStockIsNotAvailable() {
        val inventory = inv()
        inventory.reserveOrder("x", Channel.RETAIL, listOf(StockKey("p-4", "بژ", "S") to 8), Long.MAX_VALUE)
        val errors = validate(c1, listOf(DraftLine("p-4", "بژ", "S", 2)), inventory = inventory)
        assertTrue(errors.any { it.startsWith("موجودی کافی نیست") })
    }

    @Test fun discountMustNotExceedSubtotal() {
        val lines = listOf(DraftLine("p-4", "بژ", "S", 1))
        assertTrue("مبلغ تخفیف نامعتبر است" in validate(c1, lines, extraDiscount = 2_000_000L))
        assertEquals(emptyList(), validate(c1, lines, extraDiscount = 100_000L))
    }

    @Test fun onAccountFollowsCreditRules() {
        val lines = listOf(DraftLine("p-4", "بژ", "S", 2)) // 2 x 960,000 = 1,920,000
        assertEquals(emptyList(), validate(c2, lines, payment = PaymentMethod.ON_ACCOUNT, balance = 7_000_000L))
        val over = validate(c2, lines, payment = PaymentMethod.ON_ACCOUNT, balance = 19_000_000L)
        assertTrue("مبلغ از سقف اعتبار مشتری بیشتر است" in over)
        val retail = validate(c1, lines, payment = PaymentMethod.ON_ACCOUNT)
        assertTrue("خرید دفتری فقط برای مشتری عمده‌ی تأییدشده است" in retail)
        val noCredit = customers.first { it.id == "c-6" }
        assertTrue("برای این مشتری خرید دفتری فعال نیست" in validate(noCredit, lines, payment = PaymentMethod.ON_ACCOUNT))
    }

    @Test fun buildCreatesWholesaleOrderForApprovedCustomer() {
        val items = OrderBuilder.priceLines(c2, listOf(DraftLine("p-4", "بژ", "S", 2)), products, emptyList(), now)
        val order = OrderBuilder.build(
            "o", "ORD-9", c2, items, ShippingMethod.PICKUP, 0L, PaymentMethod.ON_ACCOUNT, 20_000L,
            "علی", "۰۹۱۲۰۰۰۱۲۳۴", "نباید ذخیره شود", now
        )
        assertEquals(Channel.WHOLESALE, order.channel)
        assertEquals("", order.address) // pickup has no address
        assertEquals("09120001234", order.receiverPhone)
        assertEquals(1_900_000L, order.total) // 2 x 960,000 - 20,000
    }

    @Test fun purchaseValidation() {
        assertTrue("حداقل یک کالا اضافه کنید" in PurchaseMath.validate(emptyList(), products))
        val bad = PurchaseMath.validate(listOf(PurchaseLine("zzz", "", "", 0, 0L)), products)
        assertTrue("کالای انتخاب‌شده پیدا نشد" in bad)
        assertTrue("تعداد باید بیشتر از صفر باشد" in bad)
        assertTrue("قیمت خرید هر ردیف را وارد کنید" in bad)
        assertTrue("رنگ و سایز نباید خالی باشد" in bad)
        assertEquals(emptyList(), PurchaseMath.validate(listOf(PurchaseLine("p-4", "بژ", "S", 1, 1L)), products))
    }

    @Test fun purchaseRaisesStockAndRefreshesCosts() {
        val lines = listOf(
            PurchaseLine("p-4", "بژ", "S", 10, 500_000L),
            PurchaseLine("p-4", "بژ", "XL", 5, 600_000L) // new variant
        )
        val result = PurchaseMath.apply(lines, 400_000L, products)
        assertEquals(8_000_000L, result.goodsTotal)
        assertEquals(8_400_000L, result.total)
        assertEquals(15, result.totalUnits)
        val p4 = result.products.first { it.id == "p-4" }
        assertEquals(19, p4.variants.first { it.color == "بژ" && it.size == "S" }.stock) // 9 + 10
        assertEquals(5, p4.variants.first { it.size == "XL" }.stock)
        assertEquals(533_333L, p4.purchasePrice)
        assertEquals(560_000L, p4.landedCost)
        assertTrue(p4.landedCost >= p4.purchasePrice)
        // other products untouched
        assertEquals(products.first { it.id == "p-1" }, result.products.first { it.id == "p-1" })
    }

    @Test fun purchaseWithoutExtraCostKeepsLandedEqualToPurchase() {
        val result = PurchaseMath.apply(listOf(PurchaseLine("p-5", "خاکی", "فری", 4, 650_000L)), 0L, products)
        val p5 = result.products.first { it.id == "p-5" }
        assertEquals(650_000L, p5.purchasePrice)
        assertEquals(650_000L, p5.landedCost)
        assertEquals(16, p5.variants.first().stock) // 12 + 4
    }

    @Test fun periodsCoverJalaliDayAndMonth() {
        val day = JalaliDate(1405, 8, 12)
        val at = JalaliCalendar.toEpochMillis(day) + 5 * 3_600_000L
        val (dayStart, dayEnd) = Periods.day(at)
        assertEquals(JalaliCalendar.toEpochMillis(day), dayStart)
        assertEquals(Fixtures.DAY, dayEnd - dayStart)

        val (mStart, mEnd) = Periods.month(at)
        assertEquals(JalaliCalendar.toEpochMillis(JalaliDate(1405, 8, 1)), mStart)
        assertEquals(30 * Fixtures.DAY, mEnd - mStart) // Aban has 30 days
        assertEquals(JalaliCalendar.toEpochMillis(JalaliDate(1405, 9, 1)), mEnd)

        val esfand = JalaliCalendar.toEpochMillis(JalaliDate(1404, 12, 5))
        val (eStart, eEnd) = Periods.month(esfand)
        assertEquals(29 * Fixtures.DAY, eEnd - eStart) // 1404 is not a leap year
        assertEquals(JalaliCalendar.toEpochMillis(JalaliDate(1405, 1, 1)), eEnd)
    }

    @Test fun salesStatsRankProducts() {
        fun order(id: String, status: OrderStatus, vararg items: OrderItem) =
            Fixtures.order(id).copy(items = items.toList(), status = status)
        val a = OrderItem("a", "الف", "x", "M", 3, 100L, 60L)
        val b = OrderItem("b", "ب", "x", "M", 3, 500L, 300L)
        val c = OrderItem("c", "ج", "x", "M", 9, 10L, 5L)
        val orders = listOf(
            order("1", OrderStatus.DELIVERED, a, b),
            order("2", OrderStatus.PAID, c),
            order("3", OrderStatus.CANCELLED, c),
            order("4", OrderStatus.AWAITING_PAYMENT, c)
        )
        val stats = SalesStats.byProduct(orders, now - Fixtures.DAY, now + Fixtures.DAY)
        assertEquals(listOf("c", "b", "a"), stats.map { it.productId }) // 9 units, then 3 units with higher revenue
        assertEquals(90L, stats[0].revenue)
        assertEquals(1_500L, stats[1].revenue)
        assertTrue(SalesStats.byProduct(orders, now + Fixtures.DAY, now + 2 * Fixtures.DAY).isEmpty())
    }

    @Test fun sampleBusinessDataIsConsistent() {
        val snapshot = SampleData.snapshot(now)
        val productById = snapshot.products.associateBy { it.id }
        for (o in snapshot.orders) {
            assertTrue(snapshot.customers.any { it.id == o.customerId }, o.number)
            for (i in o.items) {
                val p = productById[i.productId]
                assertTrue(p != null && p.variants.any { it.color == i.color && it.size == i.size }, "${o.number} ${i.productName}")
            }
        }
        val parties = snapshot.customers.map { it.id } + snapshot.suppliers.map { it.id }
        assertTrue(snapshot.ledger.all { it.partyId in parties })
        assertTrue(snapshot.cheques.all { it.partyId in parties })
        assertTrue(snapshot.bankAccounts.all { Validators.isValidCardNumber(it.cardNumber) && Validators.isValidSheba(it.sheba) })

        // open orders can reserve their stock on a fresh inventory
        val inventory = InventoryLedger().also { it.loadFrom(snapshot.products) }
        for (o in snapshot.orders.filter { it.status == OrderStatus.AWAITING_SELLER_APPROVAL || it.status == OrderStatus.AWAITING_PAYMENT }) {
            assertTrue(inventory.reserveOrder(o.id, o.channel, o.items.map { it.stockKey() to it.quantity }, Long.MAX_VALUE).ok, o.number)
        }
    }

    @Test fun newSnapshotPartsRoundTrip() {
        val s = SampleData.snapshot(now).copy(
            settings = StoreSettings("فروشگاه \"من\"", "09121234567", "اصفهان\nخیابان", 12, 48)
        )
        val back = DataCodec.decode(DataCodec.encode(s))
        assertEquals(s, back)
        assertEquals(48, back.settings.wholesaleHoldHours)
        assertEquals(3, back.orders.size)
        assertEquals(2, back.cheques.size)
    }

    @Test fun oldFilesWithoutNewPartsStillLoad() {
        val old = """{"version":1,"sample":false,"categories":[],"products":[],"customers":[],"ledger":[]}"""
        val s = DataCodec.decode(old)
        assertEquals(StoreSettings(), s.settings)
        assertTrue(s.orders.isEmpty() && s.cheques.isEmpty() && s.expenses.isEmpty())
    }
}
