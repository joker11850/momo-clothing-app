package com.hamid.clothing.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublishingTest {
    private val now = Fixtures.NOW

    private fun snapshot(): StoreSnapshot {
        val base = SampleData.snapshot(now)
        val withImages = base.products.map {
            when (it.id) {
                "p-1" -> it.copy(imageUris = listOf("p_one.jpg", "p_two.jpg", "../evil.jpg", "dir/x.jpg"))
                "p-2" -> it.copy(imageUris = listOf("p_three.jpg"))
                "p-3" -> it.copy(active = false, name = "محصول مخفی")
                else -> it
            }
        }
        return base.copy(
            products = withImages,
            bankAccounts = base.bankAccounts + BankAccount(
                "b-hidden", "بانک مخفی", "نام", "6037991899999993", "IR050170000000123456789012", visibleToCustomers = false
            ),
            settings = StoreSettings(
                shopName = "فروشگاه من", phone = "09121234567", address = "اصفهان",
                telegram = "myshop", instagram = "my.shop", courierCost = 60_000L, postCost = 40_000L
            )
        )
    }

    @Test fun builderKeepsOnlyPublicData() {
        val s = snapshot()
        val data = CatalogBuilder.fromSnapshot(s, now)
        assertTrue(data.products.none { it.name == "محصول مخفی" }) // inactive product
        assertEquals(s.products.count { it.active }, data.products.size)
        assertTrue(data.products.all { it.purchasePrice == 0L && it.landedCost == 0L })
        // only retail/wholesale prices survive
        assertEquals(1_530_000L, data.products.first { it.id == "p-1" }.retailPrice)
        assertEquals(1_250_000L, data.products.first { it.id == "p-1" }.wholesalePrice)
        // unsafe image names are dropped, safe ones kept in order
        assertEquals(listOf("p_one.jpg", "p_two.jpg"), data.products.first { it.id == "p-1" }.imageUris)
        // only categories in use
        val used = data.products.map { it.categoryId }.toSet()
        assertEquals(used, data.categories.map { it.id }.toSet())
        // only the bank account the owner chose to show
        assertEquals(listOf("b-1"), data.bankAccounts.map { it.id })
        assertEquals("فروشگاه من", data.store.shopName)
        assertEquals(60_000L, data.store.courierCost)
    }

    @Test fun encodedCatalogNeverContainsCostsOrHiddenData() {
        val data = CatalogBuilder.fromSnapshot(snapshot(), now)
        val json = DataCodec.encodeCatalog(data)
        assertFalse(json.contains("purchasePrice"))
        assertFalse(json.contains("landedCost"))
        assertFalse(json.contains("محصول مخفی"))
        assertFalse(json.contains("بانک مخفی"))
        assertFalse(json.contains("c-2")) // no customers
        assertFalse(json.contains("ledger"))
        assertFalse(json.contains("cheque", ignoreCase = true))
    }

    @Test fun catalogJsonRoundTrips() {
        val data = CatalogBuilder.fromSnapshot(snapshot(), now, SampleData.discounts())
        assertEquals(data, DataCodec.decodeCatalog(DataCodec.encodeCatalog(data)))
        assertEquals(4, data.discounts.size)
    }

    @Test fun invalidCatalogFilesAreRejected() {
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeCatalog("""{"version":1,"kind":"other"}""") }
        assertFailsWith<IllegalArgumentException> { DataCodec.decodeCatalog("""{"version":7,"kind":"catalog"}""") }
    }

    @Test fun packageRoundTripWithImages() {
        val data = CatalogBuilder.fromSnapshot(snapshot(), now)
        val images = mapOf(
            "p_one.jpg" to byteArrayOf(1, 2, 3),
            "p_two.jpg" to ByteArray(5000) { (it % 251).toByte() },
            "p_three.jpg" to byteArrayOf(9)
        )
        val out = ByteArrayOutputStream()
        val written = CatalogPackage.write(out, data) { images[it] }
        assertEquals(3, written)

        val received = mutableMapOf<String, ByteArray>()
        val back = CatalogPackage.read(ByteArrayInputStream(out.toByteArray())) { name, bytes -> received[name] = bytes }
        assertEquals(data, back)
        assertEquals(images.keys, received.keys)
        for ((name, bytes) in images) assertTrue(bytes.contentEquals(received[name]!!), name)
    }

    @Test fun missingImagesAreSkippedWhenWriting() {
        val data = CatalogBuilder.fromSnapshot(snapshot(), now)
        val out = ByteArrayOutputStream()
        val written = CatalogPackage.write(out, data) { if (it == "p_one.jpg") byteArrayOf(1) else null }
        assertEquals(1, written)
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        val zip = ZipOutputStream(out)
        for ((name, bytes) in entries) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
        zip.finish()
        return out.toByteArray()
    }

    @Test fun unsafeEntryNamesAreIgnored() {
        val json = DataCodec.encodeCatalog(CatalogBuilder.fromSnapshot(snapshot(), now)).toByteArray()
        val bytes = zipOf(
            "catalog.json" to json,
            "images/ok.png" to byteArrayOf(1),
            "images/../evil.png" to byteArrayOf(2),
            "images/sub/nested.png" to byteArrayOf(3),
            "../outside.png" to byteArrayOf(4),
            "other/file.txt" to byteArrayOf(5),
            "images/bad name.png" to byteArrayOf(6)
        )
        val received = mutableListOf<String>()
        CatalogPackage.read(ByteArrayInputStream(bytes)) { name, _ -> received.add(name) }
        assertEquals(listOf("ok.png"), received)
    }

    @Test fun packageWithoutJsonIsRejected() {
        val bytes = zipOf("images/a.png" to byteArrayOf(1))
        assertFailsWith<IllegalArgumentException> { CatalogPackage.read(ByteArrayInputStream(bytes)) { _, _ -> } }
    }

    @Test fun oversizedEntriesAreRejected() {
        val json = DataCodec.encodeCatalog(CatalogBuilder.fromSnapshot(snapshot(), now)).toByteArray()
        val huge = ByteArray((CatalogPackage.MAX_ENTRY_BYTES + 1).toInt())
        val bytes = zipOf("catalog.json" to json, "images/big.png" to huge)
        assertFailsWith<IllegalArgumentException> { CatalogPackage.read(ByteArrayInputStream(bytes)) { _, _ -> } }
    }

    @Test fun imageNameRules() {
        assertTrue(CatalogPackage.isSafeImageName("p_abc-123.jpg"))
        assertFalse(CatalogPackage.isSafeImageName("../a.jpg"))
        assertFalse(CatalogPackage.isSafeImageName("a/b.jpg"))
        assertFalse(CatalogPackage.isSafeImageName("a..b.jpg"))
        assertFalse(CatalogPackage.isSafeImageName(""))
        assertFalse(CatalogPackage.isSafeImageName("عکس.jpg"))
        assertFalse(CatalogPackage.isSafeImageName("x".repeat(81)))
    }

    @Test fun warningsListUnfinishedProducts() {
        val s = snapshot().copy(
            products = listOf(
                Fixtures.product("a").copy(imageUris = listOf("p_a.jpg"), description = "خوب"),
                Fixtures.product("b").copy(name = "ب", description = "", variants = listOf(Variant("x", "M", 0))),
                Fixtures.product("c").copy(active = false)
            )
        )
        val w = CatalogBuilder.warnings(s)
        assertTrue("«ب» عکس ندارد" in w)
        assertTrue("«ب» توضیحات ندارد" in w)
        assertTrue("«ب» موجودی ندارد" in w)
        assertTrue(w.none { it.contains("«شومیز c»") }) // inactive products are not checked
        assertTrue(w.none { it.contains("«شومیز a»") && it.contains("عکس") })
    }

    @Test fun newSettingsFieldsRoundTrip() {
        val s = SampleData.snapshot(now).copy(
            settings = StoreSettings(
                "فروشگاه", "0912", "آدرس", 12, 48, "98912", "tg", "ig", "rb", "ei", "ba", 60_000L, 40_000L
            )
        )
        val back = DataCodec.decode(DataCodec.encode(s))
        assertEquals(s.settings, back.settings)
        assertEquals("ig", back.settings.instagram)
        assertEquals(40_000L, back.settings.postCost)
    }
}
