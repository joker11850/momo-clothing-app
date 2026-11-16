package com.hamid.clothing.customer

import com.hamid.clothing.core.BankAccount
import com.hamid.clothing.core.ShippingMethod

/**
 * Shop settings. Until the server exists, edit the values here and rebuild.
 * Leave a value empty to hide that channel (the app shows "not set up yet").
 */
object StoreConfig {
    const val STORE_NAME = "پوشاک MOMO"
    const val BRAND_LETTER = "M"
    const val TAGLINE = "فروشگاه آنلاین پوشاک"

    // Contact channels. Usernames without @ ; WhatsApp number in international form without +, e.g. 989121234567
    const val PHONE_NUMBER = "09372228071"
    const val WHATSAPP_NUMBER = "09372228071"
    const val TELEGRAM_USER = "09372228071"
    const val INSTAGRAM_USER = ""
    const val ROBIKA_USER = "09372228071"
    const val EITAA_USER = "09372228071"
    const val BALE_USER = "09372228071"

    // Shipping costs in Toman. 0 means "announced by the shop after the order".
    const val COURIER_COST = 0L
    const val POST_COST = 0L

    fun shippingCost(method: ShippingMethod): Long = when (method) {
        ShippingMethod.COURIER -> COURIER_COST
        ShippingMethod.POST -> POST_COST
        ShippingMethod.PICKUP -> 0L
    }

    /** Bank accounts shown to customers for card-to-card payment. Empty until the shop adds them. */
    val BANK_ACCOUNTS: List<BankAccount> = emptyList()

    /** No SMS service is connected yet, so sign-in uses a visible test code. Set true only when a real service exists. */
    const val SMS_LIVE = false
    const val TEST_CODE = "12345"

    /** The online payment gateway is not connected yet. */
    const val ONLINE_PAYMENT_ENABLED = false
}
