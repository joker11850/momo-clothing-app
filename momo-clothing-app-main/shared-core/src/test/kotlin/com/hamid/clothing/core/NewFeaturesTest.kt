package com.hamid.clothing.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewFeaturesTest {
    private val now = Fixtures.NOW
    private val products = SampleData.products()
    private val fastIterations = 1_000

    // ---------------------------------------------------------------- special prices

    @Test fun priceBookSetReplaceRemove() {
        var prices = emptyList<CustomerPrice>()
        prices = PriceBook.set(prices, "c-1", "p-1", 900_000L)
        prices = PriceBook.set(prices, "c-1", "p-2", 800_000L)
        prices = PriceBook.set(prices, "c-2", "p-1", 950_000L)
        prices = PriceBook.set(prices, "c-1", "p-1", 880_000L) // replaces, does not duplicate
        assertEquals(3, prices.size)
        assertEquals(mapOf("p-1" to 880_000L, "p-2" to 800_000L), PriceBook.forCustomer(prices, "c-1"))
        assertEquals(mapOf("p-1" to 950_000L), PriceBook.forCustomer(prices, "c-2"))
        assertTrue(PriceBook.forCustomer(prices, "zzz").isEmpty())

        assertEquals(2, PriceBook.remove(prices, "c-1", "p-1").size)
        assertEquals(1, PriceBook.removeCustomer(prices, "c-1").size)
        assertEquals(1, PriceBook.removeProduct(prices, "p-1").size)
        assertFailsWith<IllegalArgumentException> { PriceBook.set(prices, "c-1", "p-3", 0L) }
    }

    @Test fun specialPriceWinsOverEverythingInOrders() {
        val c2 = SampleData.customers(now).first { it.id == "c-2" }
        val line = DraftLine("p-1", "کرم", "M", 2)
        // p-1: wholesale 1,250,000 and a 15% discount -> 1,062,500 without a special price
        val normal = OrderBuilder.priceLines(c2, listOf(line), products, SampleData.discounts(), now)
        assertEquals(1_062_500L, normal[0].unitPrice)
        val special = OrderBuilder.priceLines(c2, listOf(line), products, SampleData.discounts(), now, mapOf("p-1" to 1_100_000L))
        assertEquals(1_100_000L, special[0].unitPrice) // special price is final, discounts do not apply on top
        assertEquals(0, special[0].discountPercent)
        // an explicit price typed on the order still wins
        val typed = OrderBuilder.priceLines(c2, listOf(line.copy(unitPriceOverride = 990_000L)), products, emptyList(), now, mapOf("p-1" to 1_100_000L))
        assertEquals(990_000L, typed[0].unitPrice)
        // other products are unaffected
        val other = OrderBuilder.priceLines(c2, listOf(DraftLine("p-4", "بژ", "S", 1)), products, emptyList(), now, mapOf("p-1" to 1_100_000L))
        assertEquals(960_000L, other[0].unitPrice)
    }

    @Test fun customPricesSurviveSaving() {
        val s = SampleData.snapshot(now).copy(customPrices = listOf(CustomerPrice("c-1", "p-4", 1_000_000L), CustomerPrice("c-2", "p-1", 1_100_000L)))
        assertEquals(s.customPrices, DataCodec.decode(DataCodec.encode(s)).customPrices)
    }

    // ---------------------------------------------------------------- bulk edit

    private val categories = SampleData.categories()

    @Test fun adjustPriceRoundsAndKeepsZero() {
        assertEquals(1_300_000L, BulkEditEngine.adjustPrice(1_190_000L, 10, 1000L) - 9_000L) // 1,309,000 -> rounds to 1,309,000
        assertEquals(1_309_000L, BulkEditEngine.adjustPrice(1_190_000L, 10, 1000L))
        assertEquals(1_071_000L, BulkEditEngine.adjustPrice(1_190_000L, -10, 1000L))
        assertEquals(1_310_000L, BulkEditEngine.adjustPrice(1_190_000L, 10, 10_000L))
        assertEquals(0L, BulkEditEngine.adjustPrice(0L, 10, 1000L)) // price not set stays not set
        assertEquals(1_000L, BulkEditEngine.adjustPrice(1_000L, 0, 1000L))
        assertTrue(BulkEditEngine.adjustPrice(5L, -90, 1L) >= 1L)
    }

    @Test fun bulkPriceChangeOnSelectedProductsOnly() {
        val edit = BulkEdit.PriceChange(10, retail = true, wholesale = true, roundTo = 1000L)
        val result = BulkEditEngine.apply(products, setOf("p-4", "p-5"), edit)
        assertEquals(2, result.changed)
        assertEquals(0, result.clamped)
        val p4 = result.products.first { it.id == "p-4" }
        assertEquals(1_309_000L, p4.retailPrice) // 1,190,000 + 10%
        assertEquals(1_056_000L, p4.wholesalePrice) // 960,000 + 10%
        // not selected -> unchanged
        assertEquals(products.first { it.id == "p-1" }, result.products.first { it.id == "p-1" })
        assertEquals(products.size, result.products.size)
    }

    @Test fun bulkRetailOnlyLowerKeepsWholesaleBelowRetail() {
        // lowering only retail by 30% would push retail under wholesale: wholesale is clamped to retail
        val edit = BulkEdit.PriceChange(-30, retail = true, wholesale = false, roundTo = 1000L)
        val result = BulkEditEngine.apply(products, setOf("p-4"), edit)
        val p4 = result.products.first { it.id == "p-4" }
        assertEquals(833_000L, p4.retailPrice)
        assertEquals(833_000L, p4.wholesalePrice)
        assertEquals(1, result.clamped)
        assertTrue(p4.wholesalePrice <= p4.retailPrice)
    }

    @Test fun bulkOtherEdits() {
        val ids = setOf("p-1", "p-2")
        val cat = BulkEditEngine.apply(products, ids, BulkEdit.SetCategory("cat-5"))
        assertEquals(setOf("cat-5"), cat.products.filter { it.id in ids }.map { it.categoryId }.toSet())
        assertEquals(2, cat.changed)

        val hidden = BulkEditEngine.apply(products, ids, BulkEdit.SetActive(false))
        assertTrue(hidden.products.filter { it.id in ids }.none { it.active })
        // same value again changes nothing
        assertEquals(0, BulkEditEngine.apply(hidden.products, ids, BulkEdit.SetActive(false)).changed)

        val low = BulkEditEngine.apply(products, ids, BulkEdit.SetLowStockThreshold(7))
        assertEquals(setOf(7), low.products.filter { it.id in ids }.map { it.lowStockThreshold }.toSet())
    }

    @Test fun bulkValidation() {
        assertTrue(BulkEditEngine.validate(BulkEdit.PriceChange(0, true, true), categories).isNotEmpty())
        assertTrue(BulkEditEngine.validate(BulkEdit.PriceChange(600, true, true), categories).isNotEmpty())
        assertTrue(BulkEditEngine.validate(BulkEdit.PriceChange(-95, true, true), categories).isNotEmpty())
        assertTrue(BulkEditEngine.validate(BulkEdit.PriceChange(5, false, false), categories).isNotEmpty())
        assertEquals(emptyList(), BulkEditEngine.validate(BulkEdit.PriceChange(5, true, false), categories))
        assertTrue(BulkEditEngine.validate(BulkEdit.SetCategory("nope"), categories).isNotEmpty())
        assertEquals(emptyList(), BulkEditEngine.validate(BulkEdit.SetCategory("cat-1"), categories))
        assertTrue(BulkEditEngine.validate(BulkEdit.SetLowStockThreshold(-1), categories).isNotEmpty())
        assertEquals(emptyList(), BulkEditEngine.validate(BulkEdit.SetActive(true), categories))
    }

    // ---------------------------------------------------------------- invoice picture model

    @Test fun invoiceModelHasEverythingFormatted() {
        val order = Fixtures.order("o9", qty = 2, unitPrice = 1_190_000L, shippingCost = 50_000L)
            .copy(
                receiverName = "مریم", receiverPhone = "09120001111", address = "اصفهان", paidAmount = 1_000_000L,
                extraDiscount = 30_000L, trackingCode = "TRK9", status = OrderStatus.SHIPPED
            )
        val shop = StoreSettings(shopName = "فروشگاه من", phone = "03112345678", address = "اصفهان، خیابان نمونه")
        val m = InvoiceBuilder.build(order, "مریم احمدی", shop, accountBalance = 2_000_000L)

        assertEquals("فروشگاه من", m.shopName)
        assertEquals(listOf("تلفن: ۰۳۱۱۲۳۴۵۶۷۸", "اصفهان، خیابان نمونه"), m.shopLines)
        assertEquals("ORD-o9", m.number)
        assertEquals("مریم احمدی", m.customerName)
        assertEquals(1, m.rows.size)
        assertEquals("۱", m.rows[0].index)
        assertEquals("۲", m.rows[0].quantity)
        assertEquals("۱,۱۹۰,۰۰۰", m.rows[0].unitPrice)
        assertEquals("۲,۳۸۰,۰۰۰", m.rows[0].total)

        val totals = m.totals.toMap()
        assertEquals("۲,۳۸۰,۰۰۰", totals["جمع کالاها"])
        assertEquals("۳۰,۰۰۰", totals["تخفیف"])
        assertEquals("۵۰,۰۰۰", totals["هزینه ارسال"])
        assertEquals("۲,۴۰۰,۰۰۰", totals["مبلغ کل (تومان)"])
        assertEquals("۱,۰۰۰,۰۰۰", totals["پرداخت‌شده"])
        assertEquals("۱,۴۰۰,۰۰۰", totals["مانده این فاکتور"])
        assertEquals("بدهکار ۲,۰۰۰,۰۰۰", totals["مانده حساب مشتری"])

        val details = m.details.toMap()
        assertEquals("TRK9", details["کد رهگیری"])
        assertEquals("ارسال‌شده", details["وضعیت"])
        assertEquals("پیک", details["روش ارسال"])
    }

    @Test fun invoiceLeavesOutEmptyParts() {
        val order = Fixtures.order("o1", qty = 1, unitPrice = 500_000L)
        val m = InvoiceBuilder.build(order, "علی", StoreSettings(), accountBalance = null)
        assertEquals("فروشگاه", m.shopName) // default when no name is set
        assertTrue(m.shopLines.isEmpty())
        val labels = m.totals.map { it.first }
        assertFalse("تخفیف" in labels)
        assertFalse("هزینه ارسال" in labels)
        assertFalse("مانده حساب مشتری" in labels)
        assertEquals("تسویه", InvoiceBuilder.build(order, "علی", StoreSettings(), 0L).totals.toMap()["مانده حساب مشتری"])
        assertEquals("بستانکار ۱۰۰", InvoiceBuilder.build(order, "علی", StoreSettings(), -100L).totals.toMap()["مانده حساب مشتری"])
    }

    // ---------------------------------------------------------------- backup encryption

    private fun roundTrip(plain: ByteArray, password: String = "رمز-آزمایشی ۱۲۳"): ByteArray {
        val encrypted = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(plain), encrypted, password.toCharArray(), fastIterations)
        val out = ByteArrayOutputStream()
        BackupCrypto.decrypt(ByteArrayInputStream(encrypted.toByteArray()), out, password.toCharArray())
        return out.toByteArray()
    }

    @Test fun encryptionRoundTripsAtChunkBoundaries() {
        for (size in listOf(0, 1, 100, 65_535, 65_536, 65_537, 131_072, 200_000)) {
            val plain = ByteArray(size) { (it * 31 % 251).toByte() }
            assertTrue(plain.contentEquals(roundTrip(plain)), "size $size")
        }
    }

    @Test fun encryptedFileHidesTheContent() {
        val plain = "کاملاً محرمانه ".repeat(500).toByteArray(Charsets.UTF_8)
        val encrypted = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(plain), encrypted, "pw1234".toCharArray(), fastIterations)
        val bytes = encrypted.toByteArray()
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("محرمانه"))
        assertTrue(String(bytes, Charsets.US_ASCII).startsWith("CSBK1"))
    }

    @Test fun sameDataEncryptsDifferentlyEachTime() {
        val plain = ByteArray(1000) { it.toByte() }
        val a = ByteArrayOutputStream()
        val b = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(plain), a, "pw1234".toCharArray(), fastIterations)
        BackupCrypto.encrypt(ByteArrayInputStream(plain), b, "pw1234".toCharArray(), fastIterations)
        assertFalse(a.toByteArray().contentEquals(b.toByteArray()))
    }

    @Test fun wrongPasswordIsRejected() {
        val encrypted = BackupPackage.encryptBytes(ByteArray(5000) { it.toByte() }, "right-pass".toCharArray(), fastIterations)
        assertFailsWith<WrongPasswordException> {
            BackupCrypto.decrypt(ByteArrayInputStream(encrypted), ByteArrayOutputStream(), "wrong-pass".toCharArray())
        }
    }

    @Test fun tamperedOrTruncatedFilesAreRejected() {
        val plain = ByteArray(150_000) { (it % 200).toByte() }
        val encrypted = BackupPackage.encryptBytes(plain, "pw1234".toCharArray(), fastIterations)

        val flipped = encrypted.copyOf()
        flipped[flipped.size / 2] = (flipped[flipped.size / 2].toInt() xor 0x01).toByte()
        assertFailsWith<Exception> { BackupCrypto.decrypt(ByteArrayInputStream(flipped), ByteArrayOutputStream(), "pw1234".toCharArray()) }

        // cutting the end off must not look like a complete (shorter) backup
        val cut = encrypted.copyOf(encrypted.size - 40)
        assertFailsWith<Exception> { BackupCrypto.decrypt(ByteArrayInputStream(cut), ByteArrayOutputStream(), "pw1234".toCharArray()) }

        // dropping a whole trailing chunk is detected through the authenticated last-chunk flag
        val firstChunkEnd = 5 + 1 + 4 + 16 + 8 + (1 + 4 + 65_536 + 16)
        val droppedLast = encrypted.copyOf(firstChunkEnd)
        assertFailsWith<Exception> { BackupCrypto.decrypt(ByteArrayInputStream(droppedLast), ByteArrayOutputStream(), "pw1234".toCharArray()) }

        // trailing garbage
        assertFailsWith<Exception> {
            BackupCrypto.decrypt(ByteArrayInputStream(encrypted + byteArrayOf(1, 2, 3)), ByteArrayOutputStream(), "pw1234".toCharArray())
        }
    }

    @Test fun notABackupFileIsRejected() {
        assertFailsWith<IOException> {
            BackupCrypto.decrypt(ByteArrayInputStream("hello world, not a backup".toByteArray()), ByteArrayOutputStream(), "x".toCharArray())
        }
        assertFailsWith<IOException> { BackupCrypto.decrypt(ByteArrayInputStream(ByteArray(3)), ByteArrayOutputStream(), "x".toCharArray()) }
        assertFailsWith<IllegalArgumentException> {
            BackupCrypto.encrypt(ByteArrayInputStream(ByteArray(1)), ByteArrayOutputStream(), CharArray(0), fastIterations)
        }
    }

    // ---------------------------------------------------------------- backup package

    @Test fun fullBackupRestoresEverything() {
        val base = SampleData.snapshot(now)
        val snapshot = base.copy(
            products = base.products.map { if (it.id == "p-1") it.copy(imageUris = listOf("p_a.jpg", "p_b.jpg")) else it },
            settings = StoreSettings(shopName = "فروشگاه من", telegram = "myshop", courierCost = 60_000L)
        )
        val images = mapOf("p_a.jpg" to byteArrayOf(1, 2, 3), "p_b.jpg" to ByteArray(70_000) { (it % 7).toByte() })

        val zip = ByteArrayOutputStream()
        val stored = BackupPackage.write(zip, snapshot, now) { images[it] }
        assertEquals(2, stored)

        val encrypted = BackupPackage.encryptBytes(zip.toByteArray(), "پشتیبان۱۲۳".toCharArray(), fastIterations)
        val decrypted = ByteArrayOutputStream()
        BackupCrypto.decrypt(ByteArrayInputStream(encrypted), decrypted, "پشتیبان۱۲۳".toCharArray())

        val got = mutableMapOf<String, ByteArray>()
        val restored = BackupPackage.read(ByteArrayInputStream(decrypted.toByteArray())) { n, b -> got[n] = b }
        assertEquals(snapshot, restored.snapshot) // orders, cheques, ledger, prices, settings: everything
        assertEquals(now, restored.createdAt)
        assertEquals(images.keys, got.keys)
        for ((name, bytes) in images) assertTrue(bytes.contentEquals(got[name]!!), name)
        // costs ARE part of a private backup (unlike the public catalogue)
        assertEquals(960_000L, restored.snapshot.products.first { it.id == "p-1" }.landedCost)
    }

    @Test fun backupReadIgnoresUnsafeNamesAndNeedsData() {
        val noData = ByteArrayOutputStream().also {
            val z = java.util.zip.ZipOutputStream(it)
            z.putNextEntry(java.util.zip.ZipEntry("images/a.jpg"))
            z.write(1)
            z.closeEntry()
            z.finish()
        }
        assertFailsWith<IllegalArgumentException> { BackupPackage.read(ByteArrayInputStream(noData.toByteArray())) { _, _ -> } }

        val snapshot = SampleData.snapshot(now)
        val out = ByteArrayOutputStream()
        val z = java.util.zip.ZipOutputStream(out)
        z.putNextEntry(java.util.zip.ZipEntry("data.json"))
        z.write(DataCodec.encode(snapshot).toByteArray())
        z.closeEntry()
        z.putNextEntry(java.util.zip.ZipEntry("images/../evil.jpg"))
        z.write(1)
        z.closeEntry()
        z.putNextEntry(java.util.zip.ZipEntry("images/good.jpg"))
        z.write(2)
        z.closeEntry()
        z.finish()
        val names = mutableListOf<String>()
        BackupPackage.read(ByteArrayInputStream(out.toByteArray())) { n, _ -> names.add(n) }
        assertEquals(listOf("good.jpg"), names)
    }
}
