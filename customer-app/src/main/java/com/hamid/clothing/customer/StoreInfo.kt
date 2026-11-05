package com.hamid.clothing.customer

import com.hamid.clothing.core.CatalogStoreInfo
import com.hamid.clothing.core.ShippingMethod

/** Shop details shown to the customer: the published catalogue wins, StoreConfig is the fallback. */
data class StoreInfo(
    val name: String,
    val brandLetter: String,
    val tagline: String,
    val address: String,
    val phone: String,
    val whatsapp: String,
    val telegram: String,
    val instagram: String,
    val rubika: String,
    val eitaa: String,
    val bale: String,
    val courierCost: Long,
    val postCost: Long
) {
    fun shippingCost(method: ShippingMethod): Long = when (method) {
        ShippingMethod.COURIER -> courierCost
        ShippingMethod.POST -> postCost
        ShippingMethod.PICKUP -> 0L
    }

    companion object {
        fun from(published: CatalogStoreInfo?): StoreInfo {
            fun pick(fromCatalog: String?, fallback: String): String =
                if (fromCatalog.isNullOrBlank()) fallback else fromCatalog
            val name = pick(published?.shopName, StoreConfig.STORE_NAME)
            return StoreInfo(
                name = name,
                brandLetter = name.trim().take(1).ifEmpty { StoreConfig.BRAND_LETTER },
                tagline = StoreConfig.TAGLINE,
                address = published?.address ?: "",
                phone = pick(published?.phone, StoreConfig.PHONE_NUMBER),
                whatsapp = pick(published?.whatsapp, StoreConfig.WHATSAPP_NUMBER),
                telegram = pick(published?.telegram, StoreConfig.TELEGRAM_USER),
                instagram = pick(published?.instagram, StoreConfig.INSTAGRAM_USER),
                rubika = pick(published?.rubika, StoreConfig.ROBIKA_USER),
                eitaa = pick(published?.eitaa, StoreConfig.EITAA_USER),
                bale = pick(published?.bale, StoreConfig.BALE_USER),
                courierCost = if (published != null && published.courierCost > 0) published.courierCost else StoreConfig.COURIER_COST,
                postCost = if (published != null && published.postCost > 0) published.postCost else StoreConfig.POST_COST
            )
        }
    }
}
