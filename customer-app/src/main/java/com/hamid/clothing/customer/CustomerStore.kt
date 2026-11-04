package com.hamid.clothing.customer

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hamid.clothing.core.CartLine
import com.hamid.clothing.core.CartMath
import com.hamid.clothing.core.CartPricing
import com.hamid.clothing.core.Category
import com.hamid.clothing.core.CheckoutInput
import com.hamid.clothing.core.CheckoutService
import com.hamid.clothing.core.Customer
import com.hamid.clothing.core.CustomerAppData
import com.hamid.clothing.core.CustomerType
import com.hamid.clothing.core.DataCodec
import com.hamid.clothing.core.Discount
import com.hamid.clothing.core.InventoryLedger
import com.hamid.clothing.core.LedgerMath
import com.hamid.clothing.core.Order
import com.hamid.clothing.core.OrderProcessor
import com.hamid.clothing.core.OrderResult
import com.hamid.clothing.core.OrderStatus
import com.hamid.clothing.core.PaymentMethod
import com.hamid.clothing.core.PriceQuote
import com.hamid.clothing.core.PricedCart
import com.hamid.clothing.core.PricingEngine
import com.hamid.clothing.core.Product
import com.hamid.clothing.core.SampleData
import com.hamid.clothing.core.ShippingMethod
import com.hamid.clothing.core.StockKey
import com.hamid.clothing.core.Validators
import com.hamid.clothing.core.WholesaleRequest
import com.hamid.clothing.core.WholesaleStatus
import java.io.File

data class PlaceResult(val errors: List<String>, val order: Order?)

/**
 * State of the customer app. The catalogue is the built-in demo catalogue and orders stay on this
 * phone until a server exists; the same API will later be backed by the server.
 */
class CustomerStore(context: Context) {
    private val storage = SecureFile(File(context.filesDir, "customer-data.bin"))

    val categories: List<Category> = SampleData.categories()
    val products: List<Product> = SampleData.products().filter { it.active }
    val discounts: List<Discount> = SampleData.discounts()

    private val inventory = InventoryLedger()
    private val processor = OrderProcessor(inventory, retailHoldMillis = 24 * 3_600_000L)

    var data by mutableStateOf(CustomerAppData())
        private set

    /** Coupon code the customer applied in the cart (empty = none). */
    var coupon by mutableStateOf("")
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    /** Bumped whenever stock changes so screens re-read availability. */
    private var revision by mutableStateOf(0)

    val isLoggedIn: Boolean get() = data.loggedIn && data.customer != null
    val customer: Customer? get() = if (isLoggedIn) data.customer else null

    init {
        load()
    }

    private fun now(): Long = System.currentTimeMillis()
    private fun newId(): String = java.util.UUID.randomUUID().toString()

    fun dismissNotice() {
        notice = null
    }

    // ---------- loading / saving ----------

    private fun load() {
        val raw: String? = try {
            storage.read()
        } catch (e: Exception) {
            storage.quarantine()
            notice = "خواندن اطلاعات ذخیره‌شده ممکن نشد و فایل قبلی کنار گذاشته شد."
            null
        }
        val decoded: CustomerAppData? = if (raw == null) {
            null
        } else {
            try {
                DataCodec.decodeCustomerApp(raw)
            } catch (e: Exception) {
                storage.quarantine()
                notice = "ساختار اطلاعات ذخیره‌شده معتبر نبود و فایل قبلی کنار گذاشته شد."
                null
            }
        }
        data = decoded ?: CustomerAppData()
        rebuildInventory()
        expireOrders()
    }

    private fun persist() {
        try {
            storage.write(DataCodec.encodeCustomerApp(data))
        } catch (e: Exception) {
            notice = "ذخیره‌ی اطلاعات ناموفق بود: " + (e.message ?: e.javaClass.simpleName)
        }
    }

    private fun update(change: (CustomerAppData) -> CustomerAppData) {
        data = change(data)
        persist()
    }

    private fun rebuildInventory() {
        inventory.loadFrom(products)
        for (order in data.orders) {
            val reserving = !order.stockCommitted && (
                order.status == OrderStatus.AWAITING_PAYMENT ||
                    order.status == OrderStatus.AWAITING_SELLER_APPROVAL ||
                    order.status == OrderStatus.PAID ||
                    order.status == OrderStatus.PREPARING
                )
            if (reserving) {
                val expires = if (order.status == OrderStatus.AWAITING_PAYMENT) order.createdAt + 24 * 3_600_000L else Long.MAX_VALUE
                inventory.reserveOrder(order.id, order.channel, order.items.map { it.stockKey() to it.quantity }, expires)
            }
        }
        revision++
    }

    /** Cancels unpaid orders whose reservation time ran out. */
    private fun expireOrders() {
        val before = data.orders
        val after = processor.expireUnpaid(before, now())
        if (after != before) update { it.copy(orders = after) }
        revision++
    }

    // ---------- catalogue ----------

    fun availableFor(productId: String, color: String, size: String): Int {
        revision // read so screens refresh after stock changes
        return inventory.available(StockKey(productId, color, size))
    }

    fun quoteFor(product: Product): PriceQuote =
        PricingEngine.quote(product, customer, emptyMap(), discounts, null, now())

    fun product(id: String): Product? = products.firstOrNull { it.id == id }

    // ---------- account ----------

    /** Returns false when the phone number is invalid. */
    fun login(phone: String): Boolean {
        val normalized = Validators.normalizeMobile(phone) ?: return false
        val current = data.customer
        if (current != null && current.phone == normalized) {
            update { it.copy(loggedIn = true) }
        } else {
            val fresh = Customer("u-$normalized", "", normalized, joinedAt = now())
            // a different number starts a clean account; the guest cart and favourites are kept
            update { CustomerAppData(customer = fresh, favorites = it.favorites, cart = it.cart, loggedIn = true) }
        }
        return true
    }

    fun logout() {
        update { it.copy(loggedIn = false) }
    }

    fun saveName(name: String) {
        val c = data.customer ?: return
        update { it.copy(customer = c.copy(fullName = name.trim())) }
    }

    fun addAddress(address: String) {
        val clean = address.trim()
        if (clean.isEmpty() || data.addresses.contains(clean)) return
        update { it.copy(addresses = it.addresses + clean) }
    }

    fun removeAddress(address: String) {
        update { it.copy(addresses = it.addresses.filter { a -> a != address }) }
    }

    fun toggleFavorite(productId: String) {
        update {
            if (productId in it.favorites) it.copy(favorites = it.favorites - productId)
            else it.copy(favorites = it.favorites + productId)
        }
    }

    fun isFavorite(productId: String): Boolean = productId in data.favorites

    /** Returns an error message, or null when the request was recorded. */
    fun requestWholesale(businessName: String, city: String, note: String): String? {
        val c = data.customer ?: return "ابتدا وارد حساب کاربری شوید"
        if (businessName.trim().isEmpty()) return "نام فروشگاه یا کسب‌وکار را وارد کنید"
        val request = WholesaleRequest(businessName.trim(), city.trim(), note.trim(), now())
        update {
            it.copy(
                customer = c.copy(type = CustomerType.WHOLESALE, wholesaleStatus = WholesaleStatus.PENDING),
                wholesaleRequest = request
            )
        }
        return null
    }

    val balance: Long
        get() {
            val c = data.customer ?: return 0L
            return LedgerMath.balances(data.ledger)[c.id] ?: 0L
        }

    // ---------- cart ----------

    val cartCount: Int get() = data.cart.sumOf { it.quantity }

    /** Returns an error message, or null when the item was added. */
    fun addToCart(product: Product, color: String, size: String, quantity: Int): String? {
        val key = StockKey(product.id, color, size)
        val available = availableFor(product.id, color, size)
        if (available <= 0) return "این رنگ و سایز ناموجود است"
        val inCart = data.cart.firstOrNull { it.key() == key }?.quantity ?: 0
        if (inCart >= available) return "همه‌ی موجودی این مورد در سبد شما است"
        update { it.copy(cart = CartMath.add(it.cart, CartLine(product.id, color, size, quantity), available)) }
        return null
    }

    fun setCartQuantity(key: StockKey, quantity: Int) {
        val available = availableFor(key.productId, key.color, key.size)
        update { it.copy(cart = CartMath.setQuantity(it.cart, key, quantity.coerceAtMost(available))) }
    }

    fun removeFromCart(key: StockKey) {
        update { it.copy(cart = CartMath.remove(it.cart, key)) }
    }

    /** Returns true when the code exists and is currently valid. */
    fun applyCoupon(code: String): Boolean {
        val clean = code.trim()
        if (clean.isEmpty()) {
            coupon = ""
            return true
        }
        val t = now()
        val valid = discounts.any {
            it.active && it.code != null && it.code.equals(clean, ignoreCase = true) && t >= it.startsAt && t < it.endsAt
        }
        coupon = if (valid) clean else ""
        return valid
    }

    fun pricedCart(): PricedCart {
        revision
        return CartPricing.price(data.cart, products, inventory, customer, discounts, coupon.ifBlank { null }, now())
    }

    // ---------- orders ----------

    private fun checkoutInput(
        customer: Customer,
        name: String,
        phone: String,
        address: String,
        shipping: ShippingMethod,
        payment: PaymentMethod,
        orderId: String,
        number: String
    ) = CheckoutInput(
        orderId, number, customer, name, phone, address, shipping,
        StoreConfig.shippingCost(shipping), payment, now()
    )

    fun allowedPayments(total: Long): List<PaymentMethod> {
        val c = customer ?: return listOf(PaymentMethod.CARD_TO_CARD, PaymentMethod.ONLINE)
        return CheckoutService.allowedPayments(c, balance, total)
    }

    fun placeOrder(
        receiverName: String,
        receiverPhone: String,
        address: String,
        shipping: ShippingMethod,
        payment: PaymentMethod
    ): PlaceResult {
        val c = customer ?: return PlaceResult(listOf("ابتدا وارد حساب کاربری شوید"), null)
        val priced = pricedCart()
        val input = checkoutInput(
            c, receiverName, receiverPhone, address, shipping, payment,
            newId(), CheckoutService.nextNumber(data.orders)
        )
        val errors = CheckoutService.validate(priced, input, balance, StoreConfig.ONLINE_PAYMENT_ENABLED)
        if (errors.isNotEmpty()) return PlaceResult(errors, null)

        val order = CheckoutService.buildOrder(priced, input)
        return when (val result = processor.submit(order, now())) {
            is OrderResult.Failure -> PlaceResult(listOf(result.reason), null)
            is OrderResult.Success -> {
                val saved = result.order
                update {
                    it.copy(
                        orders = it.orders + saved,
                        cart = emptyList(),
                        customer = (it.customer ?: c).let { cur -> if (cur.fullName.isBlank()) cur.copy(fullName = saved.receiverName) else cur },
                        addresses = if (saved.address.isNotBlank() && saved.address !in it.addresses) it.addresses + saved.address else it.addresses
                    )
                }
                coupon = ""
                revision++
                PlaceResult(emptyList(), saved)
            }
        }
    }

    fun order(id: String): Order? = data.orders.firstOrNull { it.id == id }

    fun canCancel(order: Order): Boolean =
        order.status == OrderStatus.AWAITING_PAYMENT || order.status == OrderStatus.AWAITING_SELLER_APPROVAL

    /** Returns an error message, or null when the order was cancelled. */
    fun cancelOrder(orderId: String): String? {
        val order = order(orderId) ?: return "سفارش پیدا نشد"
        if (!canCancel(order)) return "این سفارش دیگر قابل لغو نیست"
        return when (val result = processor.cancel(order, "لغو توسط مشتری", now())) {
            is OrderResult.Failure -> result.reason
            is OrderResult.Success -> {
                update { d -> d.copy(orders = d.orders.map { if (it.id == orderId) result.order else it }) }
                revision++
                null
            }
        }
    }
}
