package com.hamid.clothing.customer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hamid.clothing.core.BankAccount
import com.hamid.clothing.core.CartLine
import com.hamid.clothing.core.CatalogData
import com.hamid.clothing.core.CatalogPackage
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlaceResult(val errors: List<String>, val order: Order?)

data class ImportResult(val ok: Boolean, val message: String)

/**
 * State of the customer app. The catalogue is the built-in demo catalogue and orders stay on this
 * phone until a server exists; the same API will later be backed by the server.
 */
class CustomerStore(context: Context) {
    private val storage = SecureFile(File(context.applicationContext.filesDir, "customer-data.bin"))

    private val appContext: Context = context.applicationContext
    private val catalogDir = File(appContext.filesDir, "catalog")

    // built-in demo catalogue until the shop's catalogue file is imported
    var categories: List<Category> by mutableStateOf(SampleData.categories())
        private set
    var products: List<Product> by mutableStateOf(SampleData.products().filter { it.active })
        private set
    var discounts: List<Discount> by mutableStateOf(SampleData.discounts())
        private set
    var bankAccounts: List<BankAccount> by mutableStateOf(StoreConfig.BANK_ACCOUNTS)
        private set
    var info: StoreInfo by mutableStateOf(StoreInfo.from(null))
        private set

    /** When the imported catalogue was published; 0 while the demo catalogue is shown. */
    var catalogDate: Long by mutableStateOf(0L)
        private set
    val isDemoCatalog: Boolean get() = catalogDate == 0L

    var importing: Boolean by mutableStateOf(false)
        private set

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
        loadCatalog()
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

    // ---------- imported catalogue ----------

    private fun applyCatalog(c: CatalogData) {
        categories = c.categories
        products = c.products.filter { it.active }
        discounts = c.discounts
        bankAccounts = c.bankAccounts
        info = StoreInfo.from(c.store)
        catalogDate = if (c.publishedAt > 0) c.publishedAt else 1L
    }

    private fun loadCatalog() {
        val file = File(catalogDir, "catalog.json")
        if (!file.isFile) return
        try {
            applyCatalog(DataCodec.decodeCatalog(file.readText()))
        } catch (e: Exception) {
            notice = "کاتالوگ ذخیره‌شده خوانده نشد؛ کاتالوگ نمونه نمایش داده می‌شود."
        }
    }

    /** Photo of an imported product, or null for the demo catalogue. */
    fun imageFile(name: String): File? =
        if (CatalogPackage.isSafeImageName(name)) File(File(catalogDir, "images"), name) else null

    fun coverFile(product: Product): File? = product.imageUris.firstOrNull()?.let { imageFile(it) }

    fun imageFiles(product: Product): List<File> = product.imageUris.mapNotNull { imageFile(it) }.filter { it.isFile }

    /**
     * Replaces the catalogue with the one inside a catalog zip chosen by the user (or opened from a messenger).
     * Cart lines for products that no longer exist are removed; orders stay untouched.
     */
    suspend fun importCatalog(uri: Uri): ImportResult {
        importing = true
        try {
            val staged = withContext(Dispatchers.IO) { stage(uri) }
            if (staged.first == null) return ImportResult(false, staged.second)
            val catalog = staged.first ?: return ImportResult(false, staged.second)
            applyCatalog(catalog)
            val valid = data.cart.filter { line ->
                products.any { p -> p.id == line.productId && p.variants.any { it.color == line.color && it.size == line.size } }
            }
            val removed = data.cart.size - valid.size
            if (removed > 0) update { it.copy(cart = valid) }
            rebuildInventory()
            notice = "کاتالوگ فروشگاه به‌روز شد: ${PersianNumbers.digits(catalog.products.size)} کالا." +
                if (removed > 0) " ${PersianNumbers.digits(removed)} کالای سبد خرید دیگر موجود نبود و حذف شد." else ""
            return ImportResult(true, "ok")
        } finally {
            importing = false
        }
    }

    /** Reads the zip into a temporary folder and swaps it in. Returns the catalogue or an error message. */
    private fun stage(uri: Uri): Pair<CatalogData?, String> {
        val tmp = File(appContext.filesDir, "catalog_new")
        val old = File(appContext.filesDir, "catalog_old")
        tmp.deleteRecursively()
        File(tmp, "images").mkdirs()
        return try {
            val stream = appContext.contentResolver.openInputStream(uri) ?: return null to "فایل باز نشد"
            val parsed = stream.use { input ->
                CatalogPackage.read(input) { name, bytes -> File(File(tmp, "images"), name).writeBytes(bytes) }
            }
            if (parsed.products.isEmpty()) {
                tmp.deleteRecursively()
                return null to "کاتالوگ خالی است"
            }
            File(tmp, "catalog.json").writeText(DataCodec.encodeCatalog(parsed))
            old.deleteRecursively()
            if (catalogDir.exists() && !catalogDir.renameTo(old)) {
                tmp.deleteRecursively()
                return null to "جایگزینی کاتالوگ ممکن نشد"
            }
            if (!tmp.renameTo(catalogDir)) {
                if (old.exists()) old.renameTo(catalogDir)
                tmp.deleteRecursively()
                return null to "جایگزینی کاتالوگ ممکن نشد"
            }
            old.deleteRecursively()
            parsed to "ok"
        } catch (e: Exception) {
            tmp.deleteRecursively()
            null to "این فایل کاتالوگ فروشگاه معتبر نیست"
        }
    }

    fun shippingCost(method: ShippingMethod): Long = info.shippingCost(method)

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
        info.shippingCost(shipping), payment, now()
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
