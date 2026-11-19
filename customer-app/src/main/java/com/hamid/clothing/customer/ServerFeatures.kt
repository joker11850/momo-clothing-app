package com.hamid.clothing.customer

import com.hamid.clothing.core.ServerFeatureFlags

/**
 * Local feature switches. They are deliberately disabled until the shop server is ready.
 */
object ServerFeatures {
    val flags = ServerFeatureFlags(
        baseUrl = "",
        cartSyncEnabled = false,
        notificationsEnabled = false,
        campaignsEnabled = false
    )
}
