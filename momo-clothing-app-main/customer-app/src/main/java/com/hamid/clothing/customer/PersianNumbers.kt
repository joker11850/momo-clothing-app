package com.hamid.clothing.customer

import com.hamid.clothing.core.PersianFormat

object PersianNumbers {
    fun digits(n: Int): String = PersianFormat.digits(n.toString())
}
