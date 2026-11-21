package com.hamid.clothing.customer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationSetup {
    const val CHANNEL_ID = "momo_store_updates"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "اطلاع‌رسانی فروشگاه MOMO",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تخفیف‌ها، موجودی جدید و اطلاعیه‌های فروشگاه"
            }
        )
    }

    /** The actual POST_NOTIFICATIONS request will be triggered with the server/FCM onboarding. */
    const val POST_NOTIFICATIONS_PERMISSION = Manifest.permission.POST_NOTIFICATIONS
}
