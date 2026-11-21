package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidatorsTest {
    @Test fun mobileNormalization() {
        assertEquals("09123456789", Validators.normalizeMobile("09123456789"))
        assertEquals("09123456789", Validators.normalizeMobile("+98 912 345 6789"))
        assertEquals("09123456789", Validators.normalizeMobile("00989123456789"))
        assertEquals("09123456789", Validators.normalizeMobile("9123456789"))
        assertEquals("09123456789", Validators.normalizeMobile("۰۹۱۲۳۴۵۶۷۸۹"))
        assertNull(Validators.normalizeMobile("0212345678"))
        assertNull(Validators.normalizeMobile("0912345"))
        assertNull(Validators.normalizeMobile(""))
    }

    @Test fun cardNumber() {
        assertTrue(Validators.isValidCardNumber("6037991899999993"))
        assertTrue(Validators.isValidCardNumber("6037-9918-9999-9993"))
        assertFalse(Validators.isValidCardNumber("6037991899999994"))
        assertFalse(Validators.isValidCardNumber("60379918"))
    }

    @Test fun sheba() {
        assertTrue(Validators.isValidSheba("IR050170000000123456789012"))
        assertTrue(Validators.isValidSheba("ir05 0170 0000 0012 3456 7890 12"))
        assertFalse(Validators.isValidSheba("IR060170000000123456789012"))
        assertFalse(Validators.isValidSheba("IR05017000000012345678901"))
        assertFalse(Validators.isValidSheba("XX050170000000123456789012"))
    }

    @Test fun sayad() {
        assertTrue(Validators.isValidSayadId("1234567890123456"))
        assertTrue(Validators.isValidSayadId("۱۲۳۴۵۶۷۸۹۰۱۲۳۴۵۶"))
        assertFalse(Validators.isValidSayadId("12345"))
    }
}
