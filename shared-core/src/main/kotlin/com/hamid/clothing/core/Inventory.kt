package com.hamid.clothing.core

data class StockKey(val productId: String, val color: String, val size: String)

data class Reservation(
    val orderId: String,
    val key: StockKey,
    val quantity: Int,
    val channel: Channel,
    val expiresAt: Long
)

data class ReserveResult(val ok: Boolean, val shortages: List<StockKey>)

/**
 * Stock per product/color/size with reservations.
 * available = onHand - reserved. Retail and wholesale reservations are tracked separately.
 */
class InventoryLedger {
    private val onHandMap = mutableMapOf<StockKey, Int>()
    private val reservations = mutableListOf<Reservation>()

    fun loadFrom(products: List<Product>) {
        for (p in products) for (v in p.variants) onHandMap[StockKey(p.id, v.color, v.size)] = v.stock
    }

    fun setOnHand(key: StockKey, quantity: Int) {
        require(quantity >= 0) { "quantity must not be negative" }
        onHandMap[key] = quantity
    }

    fun onHand(key: StockKey): Int = onHandMap[key] ?: 0

    fun reserved(key: StockKey, channel: Channel? = null): Int =
        reservations.filter { it.key == key && (channel == null || it.channel == channel) }.sumOf { it.quantity }

    fun available(key: StockKey): Int = onHand(key) - reserved(key)

    fun reservationsOf(orderId: String): List<Reservation> = reservations.filter { it.orderId == orderId }

    /** All-or-nothing reservation for a whole order. */
    fun reserveOrder(
        orderId: String,
        channel: Channel,
        lines: List<Pair<StockKey, Int>>,
        expiresAt: Long
    ): ReserveResult {
        require(lines.isNotEmpty()) { "order has no lines" }
        require(lines.all { it.second > 0 }) { "quantity must be positive" }
        require(reservations.none { it.orderId == orderId }) { "order already has a reservation" }
        val demand = lines.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        val shortages = demand.filter { (key, qty) -> available(key) < qty }.keys.toList()
        if (shortages.isNotEmpty()) return ReserveResult(false, shortages)
        for ((key, qty) in demand) reservations.add(Reservation(orderId, key, qty, channel, expiresAt))
        return ReserveResult(true, emptyList())
    }

    fun release(orderId: String) {
        reservations.removeAll { it.orderId == orderId }
    }

    fun extend(orderId: String, newExpiresAt: Long) {
        for (i in reservations.indices) {
            if (reservations[i].orderId == orderId) {
                reservations[i] = reservations[i].copy(expiresAt = newExpiresAt)
            }
        }
    }

    /** Turns the reservation into a real stock deduction. Returns false when nothing was reserved. */
    fun commit(orderId: String): Boolean {
        val mine = reservations.filter { it.orderId == orderId }
        if (mine.isEmpty()) return false
        for (r in mine) onHandMap[r.key] = (onHand(r.key) - r.quantity).coerceAtLeast(0)
        reservations.removeAll { it.orderId == orderId }
        return true
    }

    fun restock(key: StockKey, quantity: Int) {
        require(quantity > 0) { "quantity must be positive" }
        onHandMap[key] = onHand(key) + quantity
    }

    /** Drops expired reservations and returns the affected order ids. */
    fun expire(now: Long): List<String> {
        val expired = reservations.filter { it.expiresAt <= now }
        reservations.removeAll { it.expiresAt <= now }
        return expired.map { it.orderId }.distinct()
    }

    /** Keys whose available stock is at or below the threshold. */
    fun lowStock(products: List<Product>): List<StockKey> {
        val result = mutableListOf<StockKey>()
        for (p in products) for (v in p.variants) {
            val key = StockKey(p.id, v.color, v.size)
            if (available(key) <= p.lowStockThreshold) result.add(key)
        }
        return result
    }
}
