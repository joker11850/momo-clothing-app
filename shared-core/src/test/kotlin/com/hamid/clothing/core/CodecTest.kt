package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CodecTest {
    private val now = Fixtures.NOW

    @Test fun sampleSnapshotRoundTrips() {
        val original = SampleData.snapshot(now)
        val decoded = DataCodec.decode(DataCodec.encode(original))
        assertEquals(original, decoded)
        assertTrue(decoded.sample)
    }

    @Test fun emptySnapshotRoundTrips() {
        val empty = StoreSnapshot()
        assertEquals(empty, DataCodec.decode(DataCodec.encode(empty)))
    }

    @Test fun specialCharactersSurvive() {
        val note = "خط اول\nخط دوم \"نقل‌قول\" و \\ بک‌اسلش و emoji 😀"
        val c = Customer("c-x", "علی \"آقا\" رضایی", "09123334444", note, adminNote = note, groups = setOf("الف", "ب"))
        val e = LedgerEntry("l-x", "c-x", EntryType.PAYMENT, 1_500L, now, refId = "ord-1", note = note)
        val s = StoreSnapshot(false, emptyList(), emptyList(), listOf(c), listOf(e))
        assertEquals(s, DataCodec.decode(DataCodec.encode(s)))
    }

    @Test fun optionalFieldsAndImagesRoundTrip() {
        val p = Fixtures.product().copy(barcode = null, imageUris = listOf("a.jpg", "b.jpg"), description = "توضیح", active = false)
        val s = StoreSnapshot(false, listOf(Category("k", "دسته", "parent")), listOf(p))
        val back = DataCodec.decode(DataCodec.encode(s))
        assertEquals(s, back)
        assertEquals(null, back.products[0].barcode)
        assertEquals("parent", back.categories[0].parentId)
    }

    @Test fun unknownVersionIsRejected() {
        assertFailsWith<IllegalArgumentException> { DataCodec.decode("""{"version": 99}""") }
        assertFailsWith<IllegalArgumentException> { DataCodec.decode("""{}""") }
    }
}
