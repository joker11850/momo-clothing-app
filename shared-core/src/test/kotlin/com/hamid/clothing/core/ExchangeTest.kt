package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExchangeTest {
    private val now = Fixtures.NOW
    private val products = SampleData.products()
    private val customers = SampleData.customers(now)

    private fun customerOrder() = Fixtures.order("c-ord-1", qty = 2, unitPrice = 1_000_000L).copy(
        receiverName = "مریم", receiverPhone = "09120000001", address = "اصفهان", status = OrderStatus.AWAITING_PAYMENT
    )

    private fun sampleFile(): OrderFile =
        OrderExchange.fromOrder(customerOrder(), "مریم احمدی", "09120000001", "لطفاً زودتر")

    @Test fun orderFileRoundTrips() {
        val f = sampleFile()
        assertEquals("c-ord-1", f.clientOrderId)
        assertEquals("ORD-c-ord-1", f.clientNumber)
        assertEquals(1, f.lines.size)
        assertEquals(f, DataCodec.decodeOrderFile(DataCodec.encodeOrderFile(f)))
    }

    @Test fun kindDetection() {
        val f = sampleFile()
        assertEquals("order", DataCodec.exchangeKind(DataCodec.encodeOrderFile(f)))
        val u = OrderUpdateFile("c-ord-1", "ORD-1005", OrderStatus.SHIPPED, 0L, 2_000_000L, 0L, "TRK1", now)
        assertEquals("order-update", DataCodec.exchangeKind(DataCodec.encodeOrderUpdate(u)))
        assertEquals("catalog", DataCodec.exchangeKind(DataCodec.encodeCatalog(CatalogBuilder.fromSnapshot(SampleData.snapshot(now), now))))
        assertNull(DataCodec.exchangeKind("hello"))
        assertNull(DataCodec.exchangeKind("""{"version":9,"kind":"order"}"""))
        assertNull(DataCodec.exchangeKind(""))
    }

    @Test fun updateFileRoundTrips() {
        val u = OrderUpdateFile("id-1", "ORD-1005", OrderStatus.SHIPPED, 500_000L, 2_000_000L, 60_000L, "TRK-99", now)
        assertEquals(u, DataCodec.decodeOrderUpdate(DataCodec.encodeOrderUpdate(u)))
        val noTracking = u.copy(trackingCode = null)
        assertEquals(noTracking, DataCodec.decodeOrderUpdate(DataCodec.encodeOrderUpdate(noTracking)))
    }

    @Test fun badOrderFilesAreRejected() {
        val good = DataCodec.encodeOrderFile(sampleFile())
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeOrderFile(good.replace("\"kind\":\"order\"", "\"kind\":\"other\"")) }
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeOrderFile(good.replace("\"quantity\":2", "\"quantity\":0")) }
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeOrderFile(good.replace("\"quantity\":2", "\"quantity\":1001")) }
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeOrderFile(good.replace(Regex("\"lines\":\\[.*?\\]"), "\"lines\":[]")) }
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeOrderFile(" ".repeat(OrderExchange.MAX_CHARS + 1)) }
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeOrderUpdate(good) }
        // too many lines
        val many = sampleFile().copy(lines = List(OrderExchange.MAX_LINES + 1) { sampleFile().lines[0] })
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeOrderFile(DataCodec.encodeOrderFile(many)) }
    }

    @Test fun longTextIsClipped() {
        val f = sampleFile().copy(address = "الف".repeat(2000), customerName = "  نام  ")
        val back = DataCodec.decodeOrderFile(DataCodec.encodeOrderFile(f))
        assertEquals(OrderExchange.MAX_TEXT, back.address.length)
        assertEquals("نام", back.customerName)
    }

    @Test fun plannerMatchesCustomerByPhone() {
        val file = sampleFile().copy(
            customerPhone = "۰۹۱۲۰۰۰۰۰۰۱",
            lines = listOf(OrderFileLine("p-4", "شلوار کتان راسته", "بژ", "S", 2, 1_190_000L))
        )
        val plan = OrderImporter.plan(file, customers, products, now) { "new-id" }
        assertEquals("c-1", plan.customer?.id)
        assertNull(plan.newCustomer)
        assertEquals(listOf(DraftLine("p-4", "بژ", "S", 2)), plan.lines)
        assertTrue(plan.skipped.isEmpty())
        assertTrue(plan.canImport)
    }

    @Test fun plannerCreatesCustomerForNewPhone() {
        val file = sampleFile().copy(
            customerName = "علی نو", customerPhone = "09129998877",
            lines = listOf(OrderFileLine("p-4", "x", "بژ", "S", 1, 1L))
        )
        val plan = OrderImporter.plan(file, customers, products, now) { "new-id" }
        assertNull(plan.customer)
        val created = plan.newCustomer
        assertEquals("new-id", created?.id)
        assertEquals("علی نو", created?.fullName)
        assertEquals("09129998877", created?.phone)
        assertEquals(CustomerType.RETAIL, created?.type) // wholesale status is never granted by a file
        assertEquals(WholesaleStatus.NONE, created?.wholesaleStatus)
        assertEquals(emptyList(), CustomerValidator.validate(created!!, customers))
    }

    @Test fun wholesaleCannotBeClaimedThroughAFile() {
        // an unknown phone never becomes wholesale, whatever the file says
        val file = sampleFile().copy(customerPhone = "09127776655", paymentMethod = PaymentMethod.ON_ACCOUNT)
        val plan = OrderImporter.plan(file, customers, products, now) { "x" }
        assertEquals(WholesaleStatus.NONE, plan.newCustomer?.wholesaleStatus)
        assertEquals(CreditPolicy.NO_CREDIT, plan.newCustomer?.creditPolicy)
    }

    @Test fun plannerSkipsUnknownLines() {
        val file = sampleFile().copy(
            lines = listOf(
                OrderFileLine("p-4", "شلوار", "بژ", "S", 1, 1L),
                OrderFileLine("zzz", "ناموجود", "x", "y", 1, 1L),
                OrderFileLine("p-4", "شلوار", "بژ", "XXL", 1, 1L)
            )
        )
        val plan = OrderImporter.plan(file, customers, products, now) { "x" }
        assertEquals(1, plan.lines.size)
        assertEquals(2, plan.skipped.size)
        val inactive = products.map { if (it.id == "p-4") it.copy(active = false) else it }
        assertTrue(OrderImporter.plan(file.copy(lines = listOf(file.lines[0])), customers, inactive, now) { "x" }.lines.isEmpty())
    }

    @Test fun badPhoneMeansNoCustomer() {
        val file = sampleFile().copy(customerPhone = "123", receiverPhone = "456")
        val plan = OrderImporter.plan(file, customers, products, now) { "x" }
        assertNull(plan.customer)
        assertNull(plan.newCustomer)
        assertFalse(plan.canImport)
    }

    @Test fun updateFileForOnlyForExternalOrders() {
        val typedIn = Fixtures.order("a")
        assertNull(OrderExchange.updateFor(typedIn, now))
        val external = typedIn.copy(externalRef = "c-ord-1", status = OrderStatus.SHIPPED, trackingCode = "T1")
        val u = OrderExchange.updateFor(external, now)
        assertEquals("c-ord-1", u?.clientOrderId)
        assertEquals(OrderStatus.SHIPPED, u?.status)
        assertEquals("T1", u?.trackingCode)
    }

    @Test fun customerAppAppliesTheShopsUpdate() {
        val local = customerOrder() // subtotal 2,000,000, awaiting payment
        val paid = OrderUpdateFile("c-ord-1", "ORD-1009", OrderStatus.PAID, 1_950_000L, 1_950_000L, 0L, null, now)
        val a = OrderExchange.applyUpdate(local, paid)
        assertEquals(OrderStatus.PAID, a.status)
        assertEquals(1_950_000L, a.total) // the shop's amount wins, shown as a discount
        assertEquals(50_000L, a.extraDiscount)
        assertEquals(0L, a.remaining)
        assertEquals(OrderStatus.PAID, a.history.last().to)
        assertTrue(a.history.last().note.contains("ORD-1009"))

        val shipped = OrderUpdateFile("c-ord-1", "ORD-1009", OrderStatus.SHIPPED, 2_100_000L, 2_100_000L, 100_000L, "TRK-7", now + 1)
        val b = OrderExchange.applyUpdate(a, shipped)
        assertEquals(100_000L, b.shippingCost)
        assertEquals(0L, b.extraDiscount)
        assertEquals(2_100_000L, b.total)
        assertEquals("TRK-7", b.trackingCode)

        // the shop charged more than the customer's items: the extra is shown with shipping so totals still match
        val more = OrderUpdateFile("c-ord-1", "ORD-1009", OrderStatus.PAID, 0L, 2_300_000L, 100_000L, null, now)
        assertEquals(2_300_000L, OrderExchange.applyUpdate(local, more).total)

        // same status again adds no history entry
        val same = OrderExchange.applyUpdate(b, shipped)
        assertEquals(b.history.size, same.history.size)
    }

    @Test fun externalRefAndNoteSurviveSaving() {
        val o = customerOrder().copy(externalRef = "c-ord-77", note = "از اپ مشتری ORD-1001")
        val s = SampleData.snapshot(now).copy(orders = listOf(o))
        val back = DataCodec.decode(DataCodec.encode(s)).orders[0]
        assertEquals("c-ord-77", back.externalRef)
        assertEquals("از اپ مشتری ORD-1001", back.note)
        val c = CustomerAppData(orders = listOf(o), loggedIn = true)
        assertEquals(c, DataCodec.decodeCustomerApp(DataCodec.encodeCustomerApp(c)))
    }
}
