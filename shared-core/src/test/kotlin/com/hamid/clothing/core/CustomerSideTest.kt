package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomerSideTest {
    private val now = Fixtures.NOW
    private val products = SampleData.products()
    private val discounts = SampleData.discounts()

    private fun inventory() = InventoryLedger().also { it.loadFrom(products) }

    private fun price(
        cart: List<CartLine>,
        customer: Customer? = null,
        coupon: String? = null,
        inv: InventoryLedger = inventory()
    ) = CartPricing.price(cart, products, inv, customer, discounts, coupon, now)

    @Test fun cartMergesAndCaps() {
        val line = CartLine("p-1", "کرم", "M", 2)
        var cart = CartMath.add(emptyList(), line, 6)
        cart = CartMath.add(cart, CartLine("p-1", "کرم", "M", 5), 6)
        assertEquals(1, cart.size)
        assertEquals(6, cart[0].quantity) // capped at stock
        assertEquals(emptyList(), CartMath.add(emptyList(), line, 0))
        cart = CartMath.setQuantity(cart, line.key(), 3)
        assertEquals(3, cart[0].quantity)
        cart = CartMath.setQuantity(cart, line.key(), 0)
        assertTrue(cart.isEmpty())
    }

    @Test fun cartKeepsDifferentVariantsSeparate() {
        var cart = CartMath.add(emptyList(), CartLine("p-1", "کرم", "M", 1), 5)
        cart = CartMath.add(cart, CartLine("p-1", "کرم", "L", 1), 5)
        assertEquals(2, cart.size)
        cart = CartMath.remove(cart, StockKey("p-1", "کرم", "M"))
        assertEquals(listOf("L"), cart.map { it.size })
    }

    @Test fun pricedCartAppliesProductDiscount() {
        val priced = price(listOf(CartLine("p-1", "کرم", "M", 2), CartLine("p-4", "بژ", "S", 1)))
        // p-1: 1,530,000 - 15% = 1,300,500 ; p-4: 1,190,000 (no discount)
        assertEquals(2 * 1_300_500L + 1_190_000L, priced.subtotal)
        assertEquals(2 * 229_500L, priced.savings)
        assertEquals(3, priced.itemCount)
        assertFalse(priced.hasStockProblem)
    }

    @Test fun couponAppliesOnlyWhenEntered() {
        val cart = listOf(CartLine("p-4", "بژ", "S", 1))
        assertEquals(1_190_000L, price(cart).subtotal)
        assertEquals(1_071_000L, price(cart, coupon = "salam10").subtotal)
        assertEquals(1_190_000L, price(cart, coupon = "wrong").subtotal)
    }

    @Test fun wholesaleCustomerSeesWholesalePrice() {
        val c2 = SampleData.customers(now).first { it.id == "c-2" }
        val priced = price(listOf(CartLine("p-1", "کرم", "M", 1)), customer = c2)
        assertEquals(PriceSource.WHOLESALE, priced.lines[0].quote.source)
        assertEquals(1_062_500L, priced.subtotal) // 1,250,000 - 15%
    }

    @Test fun stockProblemIsDetectedAndUnknownProductsAreSkipped() {
        val priced = price(listOf(CartLine("p-1", "کرم", "M", 7), CartLine("zzz", "x", "y", 1)))
        assertEquals(1, priced.lines.size)
        assertTrue(priced.hasStockProblem) // only 6 in stock
    }

    @Test fun checkoutValidatesAndBuildsRetailOrder() {
        val inv = inventory()
        val customer = Customer("u-1", "مریم", "09120001111")
        val priced = price(listOf(CartLine("p-4", "بژ", "S", 2)), customer, inv = inv)
        val input = CheckoutInput(
            "o-1", "ORD-1001", customer, "مریم", "۰۹۱۲۰۰۰۱۱۱۱", "اصفهان، خیابان نمونه",
            ShippingMethod.COURIER, 0L, PaymentMethod.CARD_TO_CARD, now
        )
        assertEquals(emptyList(), CheckoutService.validate(priced, input, 0L))

        val order = CheckoutService.buildOrder(priced, input)
        assertEquals(Channel.RETAIL, order.channel)
        assertEquals(2_380_000L, order.total)
        assertEquals("09120001111", order.receiverPhone)
        assertEquals("اصفهان، خیابان نمونه", order.address)
        assertEquals(600_000L + 140_000L, order.items[0].unitCost) // landed cost of p-4

        val result = OrderProcessor(inv).submit(order, now)
        assertIs<OrderResult.Success>(result)
        assertEquals(OrderStatus.AWAITING_PAYMENT, result.order.status)
        assertEquals(7, inv.available(StockKey("p-4", "بژ", "S")))
    }

    @Test fun checkoutValidationErrors() {
        val customer = Customer("u-1", "مریم", "09120001111")
        val priced = price(listOf(CartLine("p-4", "بژ", "S", 1)), customer)
        val base = CheckoutInput(
            "o-1", "ORD-1001", customer, "مریم", "09120001111", "آدرس",
            ShippingMethod.COURIER, 0L, PaymentMethod.CARD_TO_CARD, now
        )
        val bad = base.copy(receiverName = " ", receiverPhone = "123", address = "", payment = PaymentMethod.ONLINE)
        val errors = CheckoutService.validate(priced, bad, 0L)
        assertTrue("نام تحویل‌گیرنده را وارد کنید" in errors)
        assertTrue("شماره تماس تحویل‌گیرنده معتبر نیست" in errors)
        assertTrue("آدرس تحویل را وارد کنید" in errors)
        assertTrue("پرداخت آنلاین هنوز فعال نیست؛ کارت‌به‌کارت را انتخاب کنید" in errors)

        val pickup = base.copy(shipping = ShippingMethod.PICKUP, address = "")
        assertEquals(emptyList(), CheckoutService.validate(priced, pickup, 0L))

        val onAccount = CheckoutService.validate(priced, base.copy(payment = PaymentMethod.ON_ACCOUNT), 0L)
        assertTrue("روش پرداخت انتخاب‌شده برای شما مجاز نیست" in onAccount)

        val empty = CheckoutService.validate(price(emptyList(), customer), base, 0L)
        assertTrue("سبد خرید خالی است" in empty)
        val blocked = CheckoutService.validate(priced, base.copy(customer = customer.copy(blocked = true)), 0L)
        assertTrue("حساب کاربری شما مسدود است" in blocked)
    }

    @Test fun onAccountPaymentFollowsCreditRules() {
        val c2 = SampleData.customers(now).first { it.id == "c-2" } // wholesale, limit 20M
        assertTrue(PaymentMethod.ON_ACCOUNT in CheckoutService.allowedPayments(c2, 7_000_000L, 3_000_000L))
        assertFalse(PaymentMethod.ON_ACCOUNT in CheckoutService.allowedPayments(c2, 7_000_000L, 14_000_000L))
        val c3 = SampleData.customers(now).first { it.id == "c-3" } // approval required
        assertTrue(PaymentMethod.ON_ACCOUNT in CheckoutService.allowedPayments(c3, 0L, 1L))
        val c6 = SampleData.customers(now).first { it.id == "c-6" } // no credit
        assertFalse(PaymentMethod.ON_ACCOUNT in CheckoutService.allowedPayments(c6, 0L, 1L))
        assertFalse(PaymentMethod.ON_ACCOUNT in CheckoutService.allowedPayments(Fixtures.retailCustomer(), 0L, 1L))
    }

    @Test fun wholesaleCheckoutCreatesWholesaleOrderWaitingForApproval() {
        val inv = inventory()
        val c3 = SampleData.customers(now).first { it.id == "c-3" }
        val priced = price(listOf(CartLine("p-4", "بژ", "M", 3)), c3, inv = inv)
        val input = CheckoutInput(
            "o-2", "ORD-1002", c3, "آرتا", "09120000003", "نجف‌آباد",
            ShippingMethod.POST, 0L, PaymentMethod.ON_ACCOUNT, now
        )
        assertEquals(emptyList(), CheckoutService.validate(priced, input, 8_500_000L))
        val order = CheckoutService.buildOrder(priced, input)
        assertEquals(Channel.WHOLESALE, order.channel)
        val result = OrderProcessor(inv).submit(order, now)
        assertIs<OrderResult.Success>(result)
        assertEquals(OrderStatus.AWAITING_SELLER_APPROVAL, result.order.status)
    }

    @Test fun orderNumbersAreSequential() {
        assertEquals("ORD-1001", CheckoutService.nextNumber(emptyList()))
        assertEquals("ORD-1003", CheckoutService.nextNumber(listOf(Fixtures.order("a"), Fixtures.order("b"))))
    }

    @Test fun invoiceTextContainsTheImportantLines() {
        val order = Fixtures.order("o9", qty = 2, unitPrice = 1_190_000L, shippingCost = 50_000L)
            .copy(receiverName = "مریم", receiverPhone = "09120001111", address = "اصفهان")
        val text = InvoiceText.build(order, "مریم احمدی", "فروشگاه نمونه")
        assertTrue("ORD-o9" in text)
        assertTrue("مبلغ کل: ۲,۴۳۰,۰۰۰ تومان" in text)
        assertTrue("هزینه ارسال: ۵۰,۰۰۰ تومان" in text)
        assertTrue("۲ × ۱,۱۹۰,۰۰۰ = ۲,۳۸۰,۰۰۰" in text)
        assertTrue("آدرس: اصفهان" in text)
    }

    @Test fun everyStatusHasAPersianLabel() {
        assertTrue(OrderStatus.values().all { FaLabels.status(it).isNotBlank() })
        assertTrue(PaymentMethod.values().all { FaLabels.payment(it).isNotBlank() })
        assertTrue(ShippingMethod.values().all { FaLabels.shipping(it).isNotBlank() })
    }

    @Test fun customerAppDataRoundTrips() {
        val inv = inventory()
        val customer = Customer("u-1", "مریم", "09120001111", "اصفهان", groups = setOf("vip"))
        val priced = price(listOf(CartLine("p-4", "بژ", "S", 2)), customer, inv = inv)
        val input = CheckoutInput(
            "o-1", "ORD-1001", customer, "مریم", "09120001111", "اصفهان \"خیابان\"\nپلاک ۱",
            ShippingMethod.COURIER, 50_000L, PaymentMethod.CARD_TO_CARD, now
        )
        val submitted = (OrderProcessor(inv).submit(CheckoutService.buildOrder(priced, input), now) as OrderResult.Success).order
        val withTracking = submitted.copy(trackingCode = "TRK1", history = submitted.history + StatusChange(null, OrderStatus.NEW, now, "یادداشت"))
        val data = CustomerAppData(
            customer = customer,
            addresses = listOf("اصفهان", "تهران"),
            favorites = setOf("p-1", "p-3"),
            cart = listOf(CartLine("p-1", "کرم", "L", 1)),
            orders = listOf(withTracking),
            ledger = listOf(LedgerEntry("l-1", "u-1", EntryType.PURCHASE, 5_000L, now, "o-1", "n")),
            wholesaleRequest = WholesaleRequest("بوتیک", "یزد", "توضیح", now),
            loggedIn = true
        )
        val back = DataCodec.decodeCustomerApp(DataCodec.encodeCustomerApp(data))
        assertEquals(data, back)
        assertEquals("TRK1", back.orders[0].trackingCode)
        assertNull(back.orders[0].history.last().from)
    }

    @Test fun emptyCustomerAppDataRoundTrips() {
        val data = CustomerAppData()
        assertEquals(data, DataCodec.decodeCustomerApp(DataCodec.encodeCustomerApp(data)))
    }
}
