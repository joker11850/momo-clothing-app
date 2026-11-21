package com.hamid.clothing.core

/**
 * Server-ready contracts for the offline-first app.
 * No network code is included yet; the server adapter can serialize these DTOs later.
 */
data class CartSyncLine(
    val productId: String,
    val color: String,
    val size: String,
    val quantity: Int
)

data class CartSyncRequest(
    val deviceId: String,
    val customerPhone: String?,
    val lines: List<CartSyncLine>,
    val coupon: String?,
    val updatedAt: Long
)

data class NotificationMessage(
    val id: String,
    val title: String,
    val body: String,
    val deepLink: String? = null,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val audience: String = "all"
)

data class Campaign(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val bannerImage: String? = null,
    val productIds: List<String> = emptyList(),
    val discountPercent: Int = 0,
    val couponCode: String? = null,
    val startsAt: Long = 0L,
    val endsAt: Long? = null,
    val enabled: Boolean = true
)

data class HomeBanner(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val imageUrl: String? = null,
    val action: String = "shop",
    val actionValue: String? = null,
    val priority: Int = 0,
    val startsAt: Long = 0L,
    val endsAt: Long? = null,
    val enabled: Boolean = true
)

data class ServerFeatureFlags(
    val baseUrl: String = "",
    val cartSyncEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val campaignsEnabled: Boolean = false
)


