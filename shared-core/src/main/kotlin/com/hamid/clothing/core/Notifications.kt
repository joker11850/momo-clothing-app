package com.hamid.clothing.core

import org.json.JSONObject
import java.util.UUID

enum class NotificationStatus { QUEUED, SENT, FAILED }

/** Server-ready notification payload. No transport is coupled to the local apps yet. */
data class NotificationDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: NotificationStatus = NotificationStatus.QUEUED
)

object NotificationCodec {
    fun encode(draft: NotificationDraft): String = JSONObject().apply {
        put("version", 1)
        put("kind", "notification")
        put("id", draft.id)
        put("title", draft.title)
        put("body", draft.body)
        put("createdAt", draft.createdAt)
        put("status", draft.status.name)
    }.toString()

    fun decode(text: String): NotificationDraft {
        val o = JSONObject(text)
        require(o.optString("kind") == "notification") { "not a notification" }
        val status = NotificationStatus.values().firstOrNull { it.name == o.optString("status") }
            ?: NotificationStatus.QUEUED
        return NotificationDraft(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = o.optString("title").take(120),
            body = o.optString("body").take(2000),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            status = status
        )
    }
}
