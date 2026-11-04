package com.hamid.clothing.core

object Validators {
    /** Converts Persian/Arabic-Indic digits to ASCII digits; other characters are kept. */
    fun toLatinDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
                in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun onlyDigits(input: String): String =
        toLatinDigits(input).filter { it in '0'..'9' }

    /** Returns 09xxxxxxxxx or null when the input is not an Iranian mobile number. */
    fun normalizeMobile(input: String): String? {
        val d = onlyDigits(input)
        val n = when {
            d.startsWith("0098") -> d.substring(4)
            d.startsWith("98") -> d.substring(2)
            d.startsWith("0") -> d.substring(1)
            else -> d
        }
        return if (n.length == 10 && n.startsWith("9")) "0$n" else null
    }

    /** 16 digits and a valid Luhn checksum. */
    fun isValidCardNumber(input: String): Boolean {
        val d = onlyDigits(input)
        if (d.length != 16) return false
        var sum = 0
        for ((index, ch) in d.reversed().withIndex()) {
            var v = ch - '0'
            if (index % 2 == 1) {
                v *= 2
                if (v > 9) v -= 9
            }
            sum += v
        }
        return sum % 10 == 0
    }

    /** IR + 24 digits, ISO 7064 mod 97-10. */
    fun isValidSheba(input: String): Boolean {
        val s = toLatinDigits(input).replace(" ", "").uppercase()
        if (!Regex("^IR[0-9]{24}$").matches(s)) return false
        val rearranged = s.substring(4) + s.substring(0, 4)
        var rem = 0
        for (ch in rearranged) {
            rem = if (ch in '0'..'9') {
                (rem * 10 + (ch - '0')) % 97
            } else {
                (rem * 100 + (ch - 'A' + 10)) % 97
            }
        }
        return rem == 1
    }

    /** Sayad cheque id: exactly 16 digits. */
    fun isValidSayadId(input: String): Boolean = onlyDigits(input).length == 16
}
