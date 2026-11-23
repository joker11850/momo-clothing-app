package com.hamid.clothing.core

enum class PriceSource { CUSTOM, WHOLESALE, RETAIL }
enum class DiscountScope { ALL, PRODUCTS, CUSTOMERS, CUSTOMER_GROUPS }

data class Discount(
    val id: String,
    val title: String,
    val percent: Int,
    val scope: DiscountScope = DiscountScope.ALL,
    val targetIds: Set<String> = emptySet(),
    /** When set, the discount applies only if the customer enters this code. */
    val code: String? = null,
    val startsAt: Long = 0L,
    val endsAt: Long = Long.MAX_VALUE,
    val active: Boolean = true
) {
    init { require(percent in 0..100) { "percent must be between 0 and 100" } }
}

data class PriceQuote(
    val source: PriceSource,
    val basePrice: Long,
    val discountPercent: Int,
    val discountId: String?,
    val unitPrice: Long
)

object PricingEngine {
    /**
     * Price resolution order:
     * 1. customer-specific price (final, no extra discount on top)
     * 2. wholesale price, only for approved wholesale customers (falls back to retail when not set)
     * 3. retail price
     * Then the single best applicable discount (no stacking) is applied to 2 and 3.
     *
     * @param customPrices productId -> special price for this customer
     */
    fun quote(
        product: Product,
        customer: Customer?,
        customPrices: Map<String, Long>,
        discounts: List<Discount>,
        couponCode: String?,
        now: Long
    ): PriceQuote {
        val custom = customPrices[product.id]
        if (customer != null && custom != null && custom > 0) {
            return PriceQuote(PriceSource.CUSTOM, custom, 0, null, custom)
        }
        val useWholesale = customer?.canSeeWholesale == true && product.wholesalePrice > 0
        val base = if (useWholesale) product.wholesalePrice else product.retailPrice
        val source = if (useWholesale) PriceSource.WHOLESALE else PriceSource.RETAIL

        val best = discounts
            .filter { applies(it, product, customer, couponCode, now) }
            .maxByOrNull { it.percent }
        val percent = best?.percent ?: 0
        val discounted = Money.afterDiscount(base, percent)

        // Shop-set sale: retailPrice is what the customer pays, compareAtPrice is shown struck through.
        // Retail only (wholesale prices never show a struck price). No stacking: the lower final price wins.
        val manualSale = !useWholesale && product.retailPrice > 0 && product.compareAtPrice > product.retailPrice
        if (manualSale && product.retailPrice <= discounted) {
            val salePercent = (((product.compareAtPrice - product.retailPrice) * 100 + product.compareAtPrice / 2) / product.compareAtPrice)
                .toInt().coerceIn(1, 99)
            return PriceQuote(source, product.compareAtPrice, salePercent, null, product.retailPrice)
        }
        return PriceQuote(source, base, percent, best?.id, discounted)
    }

    private fun applies(d: Discount, product: Product, customer: Customer?, coupon: String?, now: Long): Boolean {
        if (!d.active || now < d.startsAt || now >= d.endsAt) return false
        if (d.code != null && (coupon == null || !d.code.equals(coupon.trim(), ignoreCase = true))) return false
        return when (d.scope) {
            DiscountScope.ALL -> true
            DiscountScope.PRODUCTS -> product.id in d.targetIds
            DiscountScope.CUSTOMERS -> customer != null && customer.id in d.targetIds
            DiscountScope.CUSTOMER_GROUPS -> customer != null && customer.groups.any { it in d.targetIds }
        }
    }
}
