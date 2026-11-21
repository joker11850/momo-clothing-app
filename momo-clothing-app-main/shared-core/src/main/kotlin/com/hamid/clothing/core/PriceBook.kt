package com.hamid.clothing.core

/** A special price for one customer and one product. It replaces every other price and discount. */
data class CustomerPrice(val customerId: String, val productId: String, val price: Long) {
    init { require(price > 0) { "price must be positive" } }
}

object PriceBook {
    /** productId -> price for this customer. */
    fun forCustomer(prices: List<CustomerPrice>, customerId: String): Map<String, Long> =
        prices.filter { it.customerId == customerId }.associate { it.productId to it.price }

    /** Adds or replaces the price of [productId] for [customerId]. */
    fun set(prices: List<CustomerPrice>, customerId: String, productId: String, price: Long): List<CustomerPrice> {
        require(price > 0) { "price must be positive" }
        val rest = prices.filter { !(it.customerId == customerId && it.productId == productId) }
        return rest + CustomerPrice(customerId, productId, price)
    }

    fun remove(prices: List<CustomerPrice>, customerId: String, productId: String): List<CustomerPrice> =
        prices.filter { !(it.customerId == customerId && it.productId == productId) }

    fun removeProduct(prices: List<CustomerPrice>, productId: String): List<CustomerPrice> =
        prices.filter { it.productId != productId }

    fun removeCustomer(prices: List<CustomerPrice>, customerId: String): List<CustomerPrice> =
        prices.filter { it.customerId != customerId }
}

sealed class BulkEdit {
    /** Raises (positive) or lowers (negative) prices by [percent] percent, then rounds to [roundTo] Toman. */
    data class PriceChange(val percent: Int, val retail: Boolean, val wholesale: Boolean, val roundTo: Long = 1000L) : BulkEdit()
    data class SetCategory(val categoryId: String) : BulkEdit()
    data class SetActive(val active: Boolean) : BulkEdit()
    data class SetLowStockThreshold(val threshold: Int) : BulkEdit()
}

data class BulkResult(val products: List<Product>, val changed: Int, val clamped: Int)

object BulkEditEngine {
    fun validate(edit: BulkEdit, categories: List<Category>): List<String> {
        val errors = mutableListOf<String>()
        when (edit) {
            is BulkEdit.PriceChange -> {
                if (edit.percent == 0) errors.add("درصد تغییر را وارد کنید")
                if (edit.percent < -90 || edit.percent > 500) errors.add("درصد تغییر باید بین ۹۰- و ۵۰۰ باشد")
                if (!edit.retail && !edit.wholesale) errors.add("حداقل یکی از قیمت خرده یا عمده را انتخاب کنید")
                if (edit.roundTo < 1) errors.add("گرد کردن نامعتبر است")
            }
            is BulkEdit.SetCategory ->
                if (categories.none { it.id == edit.categoryId }) errors.add("دسته‌بندی را انتخاب کنید")
            is BulkEdit.SetActive -> Unit
            is BulkEdit.SetLowStockThreshold ->
                if (edit.threshold < 0) errors.add("حد هشدار نباید منفی باشد")
        }
        return errors
    }

    /** New price after a percent change, rounded to the nearest [roundTo]; a price of 0 (not set) stays 0. */
    fun adjustPrice(price: Long, percent: Int, roundTo: Long): Long {
        if (price <= 0) return price
        val raw = (price * (100 + percent) + 50) / 100
        val rounded = if (roundTo > 1) ((raw + roundTo / 2) / roundTo) * roundTo else raw
        return rounded.coerceAtLeast(1L)
    }

    fun apply(products: List<Product>, ids: Set<String>, edit: BulkEdit): BulkResult {
        var changed = 0
        var clamped = 0
        val result = products.map { p ->
            if (p.id !in ids) {
                p
            } else {
                val updated = when (edit) {
                    is BulkEdit.PriceChange -> {
                        val retail = if (edit.retail) adjustPrice(p.retailPrice, edit.percent, edit.roundTo) else p.retailPrice
                        var wholesale = if (edit.wholesale) adjustPrice(p.wholesalePrice, edit.percent, edit.roundTo) else p.wholesalePrice
                        if (wholesale > 0 && wholesale > retail) {
                            wholesale = retail
                            clamped++
                        }
                        p.copy(retailPrice = retail, wholesalePrice = wholesale)
                    }
                    is BulkEdit.SetCategory -> p.copy(categoryId = edit.categoryId)
                    is BulkEdit.SetActive -> p.copy(active = edit.active)
                    is BulkEdit.SetLowStockThreshold -> p.copy(lowStockThreshold = edit.threshold)
                }
                if (updated != p) changed++
                updated
            }
        }
        return BulkResult(result, changed, clamped)
    }
}
