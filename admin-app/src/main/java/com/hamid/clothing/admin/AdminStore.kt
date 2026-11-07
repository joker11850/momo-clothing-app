package com.hamid.clothing.admin

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hamid.clothing.core.BackupCrypto
import com.hamid.clothing.core.BackupPackage
import com.hamid.clothing.core.BankAccount
import com.hamid.clothing.core.BulkEdit
import com.hamid.clothing.core.BulkEditEngine
import com.hamid.clothing.core.BulkResult
import com.hamid.clothing.core.CustomerPrice
import com.hamid.clothing.core.InvoiceBuilder
import com.hamid.clothing.core.InvoiceModel
import com.hamid.clothing.core.PriceBook
import com.hamid.clothing.core.CatalogBuilder
import com.hamid.clothing.core.CatalogPackage
import com.hamid.clothing.core.JalaliCalendar
import com.hamid.clothing.core.Category
import com.hamid.clothing.core.CheckoutService
import com.hamid.clothing.core.Cheque
import com.hamid.clothing.core.ChequeDirection
import com.hamid.clothing.core.ChequeService
import com.hamid.clothing.core.ChequeStatus
import com.hamid.clothing.core.CreditDecision
import com.hamid.clothing.core.CreditService
import com.hamid.clothing.core.Customer
import com.hamid.clothing.core.CustomerValidator
import com.hamid.clothing.core.DataCodec
import com.hamid.clothing.core.DraftLine
import com.hamid.clothing.core.EntryType
import com.hamid.clothing.core.Expense
import com.hamid.clothing.core.ExpenseCategory
import com.hamid.clothing.core.InventoryLedger
import com.hamid.clothing.core.LedgerEntry
import com.hamid.clothing.core.LedgerMath
import com.hamid.clothing.core.Order
import com.hamid.clothing.core.OrderBuilder
import com.hamid.clothing.core.OrderProcessor
import com.hamid.clothing.core.OrderResult
import com.hamid.clothing.core.OrderStatus
import com.hamid.clothing.core.PartyLedger
import com.hamid.clothing.core.PaymentMethod
import com.hamid.clothing.core.Product
import com.hamid.clothing.core.ProductValidator
import com.hamid.clothing.core.PurchaseLine
import com.hamid.clothing.core.PurchaseMath
import com.hamid.clothing.core.SampleData
import com.hamid.clothing.core.ShippingMethod
import com.hamid.clothing.core.StockKey
import com.hamid.clothing.core.StoreSettings
import com.hamid.clothing.core.StoreSnapshot
import com.hamid.clothing.core.Supplier
import com.hamid.clothing.core.Validators
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

data class OrderCreation(val errors: List<String>, val order: Order?)

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class PublishResult(val file: File, val products: Int, val images: Int, val bytes: Long)

data class BackupFile(val file: File, val images: Int, val bytes: Long)

/** A backup that was decrypted and checked; nothing is replaced until [AdminStore.applyRestore]. */
class PreparedRestore(val snapshot: StoreSnapshot, val imagesDir: File, val createdAt: Long)

/**
 * Single source of truth for the admin app. Data is local and encrypted; the same API will later
 * be backed by a server. Every change is written to disk immediately.
 */
class AdminStore(context: Context) {
    private val appContext: Context = context.applicationContext
    private val storage = SecureFile(File(appContext.filesDir, "admin-data.bin"))
    private val inventory = InventoryLedger()
    private val prefs = appContext.getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)

    /** Product photos. */
    val images = ImageStore(appContext)

    var themeMode by mutableStateOf(readThemeMode())
        private set

    var lastPublishedAt by mutableStateOf(prefs.getLong("catalog_published_at", 0L))
        private set

    var lastBackupAt by mutableStateOf(prefs.getLong("last_backup_at", 0L))
        private set

    var snapshot by mutableStateOf(StoreSnapshot())
        private set

    /** One-time message for the user (for example when stored data could not be read). */
    var notice by mutableStateOf<String?>(null)
        private set

    /** Bumped when stock changes so screens re-read availability. */
    private var revision by mutableStateOf(0)

    val categories: List<Category> get() = snapshot.categories
    val products: List<Product> get() = snapshot.products
    val customers: List<Customer> get() = snapshot.customers
    val ledger: List<LedgerEntry> get() = snapshot.ledger
    val orders: List<Order> get() = snapshot.orders
    val suppliers: List<Supplier> get() = snapshot.suppliers
    val cheques: List<Cheque> get() = snapshot.cheques
    val expenses: List<Expense> get() = snapshot.expenses
    val bankAccounts: List<BankAccount> get() = snapshot.bankAccounts
    val settings: StoreSettings get() = snapshot.settings
    val customPrices: List<CustomerPrice> get() = snapshot.customPrices
    val isSample: Boolean get() = snapshot.sample
    val balances: Map<String, Long> get() = LedgerMath.balances(snapshot.ledger)

    init {
        load()
    }

    private fun now(): Long = System.currentTimeMillis()

    fun newId(): String = UUID.randomUUID().toString()

    fun dismissNotice() {
        notice = null
    }

    // ---------- loading / saving ----------

    private fun load() {
        val raw: String? = try {
            storage.read()
        } catch (e: Exception) {
            storage.quarantine()
            notice = "خواندن داده‌های ذخیره‌شده ممکن نشد. فایل قبلی کنار گذاشته شد و داده‌ی آزمایشی بارگذاری شد."
            null
        }
        val decoded: StoreSnapshot? = if (raw == null) {
            null
        } else {
            try {
                DataCodec.decode(raw)
            } catch (e: Exception) {
                storage.quarantine()
                notice = "ساختار داده‌های ذخیره‌شده معتبر نبود. فایل قبلی کنار گذاشته شد و داده‌ی آزمایشی بارگذاری شد."
                null
            }
        }
        snapshot = decoded ?: SampleData.snapshot(now())
        rebuildInventory()
        expireOrders()
        if (decoded == null) persist()
    }

    private fun persist() {
        try {
            storage.write(DataCodec.encode(snapshot))
        } catch (e: Exception) {
            notice = "ذخیره‌ی داده‌ها ناموفق بود: " + (e.message ?: e.javaClass.simpleName)
        }
    }

    private fun update(change: (StoreSnapshot) -> StoreSnapshot) {
        snapshot = change(snapshot)
        persist()
    }

    // ---------- inventory ----------

    private fun holdMillis(hours: Int): Long? = if (hours > 0) hours * 3_600_000L else null

    private fun processor(): OrderProcessor = OrderProcessor(
        inventory,
        retailHoldMillis = (holdMillis(settings.retailHoldHours) ?: 24 * 3_600_000L),
        wholesaleHoldMillis = holdMillis(settings.wholesaleHoldHours)
    )

    private fun reservationExpiry(order: Order): Long = when {
        order.channel == com.hamid.clothing.core.Channel.RETAIL && order.status == OrderStatus.AWAITING_PAYMENT ->
            order.createdAt + (holdMillis(settings.retailHoldHours) ?: 24 * 3_600_000L)
        order.channel == com.hamid.clothing.core.Channel.WHOLESALE && order.status == OrderStatus.AWAITING_SELLER_APPROVAL ->
            holdMillis(settings.wholesaleHoldHours)?.let { order.createdAt + it } ?: Long.MAX_VALUE
        else -> Long.MAX_VALUE
    }

    /** Recreates stock and reservations from saved products and open orders. */
    private fun rebuildInventory() {
        for (o in orders) inventory.release(o.id)
        inventory.loadFrom(products)
        for (o in orders) {
            val reserving = !o.stockCommitted && (
                o.status == OrderStatus.AWAITING_PAYMENT ||
                    o.status == OrderStatus.AWAITING_SELLER_APPROVAL ||
                    o.status == OrderStatus.PAID ||
                    o.status == OrderStatus.PREPARING
                )
            if (reserving) {
                inventory.reserveOrder(o.id, o.channel, o.items.map { it.stockKey() to it.quantity }, reservationExpiry(o))
            }
        }
        revision++
    }

    /** Cancels unpaid orders whose reservation time ran out. */
    private fun expireOrders() {
        val before = orders
        val after = processor().expireUnpaid(before, now())
        if (after != before) update { it.copy(orders = after) }
        revision++
    }

    /** Copies real stock (after shipped/returned orders) back into the saved products. */
    private fun syncStock(source: StoreSnapshot): StoreSnapshot = source.copy(
        products = source.products.map { p ->
            p.copy(variants = p.variants.map { v -> v.copy(stock = inventory.onHand(StockKey(p.id, v.color, v.size))) })
        }
    )

    fun availableFor(productId: String, color: String, size: String): Int {
        revision
        return inventory.available(StockKey(productId, color, size))
    }

    fun reservedFor(productId: String, color: String, size: String): Int {
        revision
        return inventory.reserved(StockKey(productId, color, size))
    }

    // ---------- categories ----------

    /** Returns the new category, or null when the name is blank or already used. */
    fun addCategory(name: String): Category? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        if (categories.any { it.name == clean }) return null
        val category = Category(newId(), clean)
        update { it.copy(categories = it.categories + category) }
        return category
    }

    // ---------- products ----------

    /** Returns validation errors; an empty list means the product was saved. */
    fun saveProduct(product: Product): List<String> {
        val errors = ProductValidator.validate(product, products)
        if (errors.isNotEmpty()) return errors
        val exists = products.any { it.id == product.id }
        val list = if (exists) products.map { if (it.id == product.id) product else it } else products + product
        update { it.copy(products = list) }
        rebuildInventory()
        return emptyList()
    }

    /** Returns an error message, or null when deleted. */
    fun deleteProduct(id: String): String? {
        if (orders.any { o -> o.items.any { it.productId == id } }) {
            return "این کالا در سفارش‌ها استفاده شده است و حذف نمی‌شود. می‌توانید آن را غیرفعال کنید."
        }
        val removed = products.firstOrNull { it.id == id }
        update { it.copy(products = it.products.filter { p -> p.id != id }, customPrices = PriceBook.removeProduct(it.customPrices, id)) }
        removed?.imageUris?.forEach { images.delete(it) }
        rebuildInventory()
        return null
    }

    // ---------- customers ----------

    fun saveCustomer(customer: Customer): List<String> {
        val errors = CustomerValidator.validate(customer, customers)
        if (errors.isNotEmpty()) return errors
        val exists = customers.any { it.id == customer.id }
        val list = if (exists) customers.map { if (it.id == customer.id) customer else it } else customers + customer
        update { it.copy(customers = list) }
        return emptyList()
    }

    /** Returns an error message, or null when the customer was deleted. */
    fun deleteCustomer(id: String): String? {
        if (ledger.any { it.partyId == id } || orders.any { it.customerId == id } || cheques.any { it.partyId == id }) {
            return "این مشتری گردش حساب، سفارش یا چک دارد و حذف نمی‌شود. می‌توانید او را مسدود کنید."
        }
        update { it.copy(customers = it.customers.filter { c -> c.id != id }, customPrices = PriceBook.removeCustomer(it.customPrices, id)) }
        return null
    }

    // ---------- accounts (customers and suppliers) ----------

    private fun partyLedger(partyId: String): PartyLedger {
        val book = PartyLedger(partyId)
        for (e in ledger.filter { it.partyId == partyId }) book.add(e)
        return book
    }

    fun balanceOf(partyId: String): Long = balances[partyId] ?: 0L

    /** Returns an error message, or null when the entry was recorded. */
    fun addLedgerEntry(partyId: String, type: EntryType, amount: Long, note: String): String? {
        if (amount <= 0) return "مبلغ باید بیشتر از صفر باشد"
        if (customers.none { it.id == partyId } && suppliers.none { it.id == partyId }) return "طرف حساب پیدا نشد"
        val entry = LedgerEntry(newId(), partyId, type, amount, now(), null, note.trim())
        update { it.copy(ledger = it.ledger + entry) }
        return null
    }

    // ---------- orders ----------

    fun order(id: String): Order? = orders.firstOrNull { it.id == id }

    fun createOrder(
        customerId: String?,
        lines: List<DraftLine>,
        shipping: ShippingMethod,
        shippingCost: Long,
        payment: PaymentMethod,
        extraDiscount: Long,
        receiverName: String,
        receiverPhone: String,
        address: String
    ): OrderCreation {
        val customer = customers.firstOrNull { it.id == customerId }
        val items = OrderBuilder.priceLines(customer, lines, products, emptyList(), now(), customPricesOf(customer?.id ?: ""))
        val errors = OrderBuilder.validate(
            customer, lines, items, products, shipping, payment, receiverName, receiverPhone, address,
            extraDiscount, shippingCost, balanceOf(customer?.id ?: "")
        ) { inventory.available(it) }
        if (errors.isNotEmpty() || customer == null) return OrderCreation(errors, null)

        val draft = OrderBuilder.build(
            newId(), CheckoutService.nextNumber(orders), customer, items, shipping, shippingCost, payment,
            extraDiscount, receiverName, receiverPhone, address, now()
        )
        val p = processor()
        var result = p.submit(draft, now())
        // an order entered by the shop owner counts as approved
        if (result is OrderResult.Success && result.order.channel == com.hamid.clothing.core.Channel.WHOLESALE) {
            result = p.approveWholesale(result.order, now(), creditApproved = payment == PaymentMethod.ON_ACCOUNT)
        }
        return when (result) {
            is OrderResult.Failure -> OrderCreation(listOf(result.reason), null)
            is OrderResult.Success -> {
                val saved = result.order
                val extraEntries = if (payment == PaymentMethod.ON_ACCOUNT) listOf(purchaseEntry(saved)) else emptyList()
                update { it.copy(orders = it.orders + saved, ledger = it.ledger + extraEntries) }
                revision++
                OrderCreation(emptyList(), saved)
            }
        }
    }

    private fun purchaseEntry(order: Order) = LedgerEntry(
        "ord-${order.id}-purchase", order.customerId, EntryType.PURCHASE, order.total, now(), order.id, "سفارش ${order.number}"
    )

    private fun replaceOrder(updated: Order, extraEntries: List<LedgerEntry> = emptyList()) {
        update {
            syncStock(it.copy(orders = it.orders.map { o -> if (o.id == updated.id) updated else o }, ledger = it.ledger + extraEntries))
        }
        revision++
    }

    /** Credits the customer's account back when an on-account order is cancelled or returned. */
    private fun creditBackEntries(order: Order, note: String): List<LedgerEntry> {
        val bought = ledger.any { it.id == "ord-${order.id}-purchase" }
        val already = ledger.any { it.id == "ord-${order.id}-return" }
        if (!bought || already) return emptyList()
        return listOf(LedgerEntry("ord-${order.id}-return", order.customerId, EntryType.RETURN_CREDIT, order.total, now(), order.id, note))
    }

    private fun runOrderAction(orderId: String, extra: (Order) -> List<LedgerEntry> = { emptyList() }, action: (OrderProcessor, Order) -> OrderResult): String? {
        val order = order(orderId) ?: return "سفارش پیدا نشد"
        return when (val result = action(processor(), order)) {
            is OrderResult.Failure -> result.reason
            is OrderResult.Success -> {
                replaceOrder(result.order, extra(order))
                null
            }
        }
    }

    /** Approves a wholesale order. On-account orders are checked against the customer's credit. */
    fun approveOrder(orderId: String): String? {
        val order = order(orderId) ?: return "سفارش پیدا نشد"
        val customer = customers.firstOrNull { it.id == order.customerId } ?: return "مشتری پیدا نشد"
        if (order.paymentMethod == PaymentMethod.ON_ACCOUNT) {
            // the order itself is not part of the balance yet
            when (CreditService.evaluate(customer, balanceOf(customer.id), order.total)) {
                CreditDecision.ALLOWED, CreditDecision.NEEDS_ADMIN_APPROVAL -> Unit
                CreditDecision.LIMIT_EXCEEDED -> return "مبلغ از سقف اعتبار مشتری بیشتر است؛ ابتدا سقف را در پرونده‌ی مشتری تغییر دهید"
                else -> return "خرید دفتری برای این مشتری مجاز نیست"
            }
        }
        return runOrderAction(orderId, extra = { o -> if (o.paymentMethod == PaymentMethod.ON_ACCOUNT) listOf(purchaseEntry(o)) else emptyList() }) { p, o ->
            p.approveWholesale(o, now(), creditApproved = o.paymentMethod == PaymentMethod.ON_ACCOUNT)
        }
    }

    fun recordOrderPayment(orderId: String, amount: Long): String? = runOrderAction(orderId) { p, o -> p.recordPayment(o, amount, now()) }
    fun startPreparing(orderId: String): String? = runOrderAction(orderId) { p, o -> p.startPreparing(o, now()) }
    fun shipOrder(orderId: String, tracking: String): String? =
        runOrderAction(orderId) { p, o -> p.ship(o, tracking.trim().ifBlank { null }, now()) }
    fun deliverOrder(orderId: String): String? = runOrderAction(orderId) { p, o -> p.deliver(o, now()) }
    fun cancelOrder(orderId: String): String? =
        runOrderAction(orderId, extra = { o -> creditBackEntries(o, "لغو سفارش ${o.number}") }) { p, o -> p.cancel(o, "لغو توسط فروشنده", now()) }
    fun returnOrder(orderId: String): String? =
        runOrderAction(orderId, extra = { o -> creditBackEntries(o, "مرجوعی سفارش ${o.number}") }) { p, o -> p.markReturned(o, "مرجوعی", now()) }

    // ---------- cheques ----------

    /** Returns an error message, or null when the cheque was saved. */
    fun addCheque(
        direction: ChequeDirection,
        partyId: String,
        amount: Long,
        sayadId: String,
        dueDate: Long,
        bankName: String,
        note: String
    ): String? {
        if (amount <= 0) return "مبلغ چک را وارد کنید"
        if (!Validators.isValidSayadId(sayadId)) return "شناسه‌ی صیاد باید ۱۶ رقم باشد"
        if (customers.none { it.id == partyId } && suppliers.none { it.id == partyId }) return "طرف حساب پیدا نشد"
        val cheque = Cheque(
            newId(), direction, partyId, amount, Validators.toLatinDigits(sayadId).filter { it in '0'..'9' },
            dueDate, bankName.trim(), ChequeStatus.PENDING, note.trim()
        )
        update { it.copy(cheques = it.cheques + cheque) }
        return null
    }

    private fun changeCheque(id: String, change: (Cheque, PartyLedger) -> Cheque): String? {
        val cheque = cheques.firstOrNull { it.id == id } ?: return "چک پیدا نشد"
        val book = partyLedger(cheque.partyId)
        val before = book.entries.size
        val updated = try {
            change(cheque, book)
        } catch (e: IllegalArgumentException) {
            return "این تغییر وضعیت برای چک مجاز نیست"
        }
        val newEntries = book.entries.drop(before)
        update {
            it.copy(cheques = it.cheques.map { c -> if (c.id == id) updated else c }, ledger = it.ledger + newEntries)
        }
        return null
    }

    fun depositCheque(id: String): String? = changeCheque(id) { c, _ -> ChequeService.deposit(c) }
    fun clearCheque(id: String): String? = changeCheque(id) { c, book -> ChequeService.clear(c, book, now()) }
    fun bounceCheque(id: String): String? = changeCheque(id) { c, book -> ChequeService.bounce(c, book, now()) }

    fun deleteCheque(id: String): String? {
        val cheque = cheques.firstOrNull { it.id == id } ?: return "چک پیدا نشد"
        if (cheque.status != ChequeStatus.PENDING) return "فقط چک وصول‌نشده و صندوق‌نرفته حذف می‌شود"
        update { it.copy(cheques = it.cheques.filter { c -> c.id != id }) }
        return null
    }

    // ---------- suppliers ----------

    fun saveSupplier(supplier: Supplier): String? {
        if (supplier.name.isBlank()) return "نام تأمین‌کننده را وارد کنید"
        val phone = supplier.phone.trim()
        if (phone.isNotEmpty() && Validators.normalizeMobile(phone) == null && phone.filter { it.isDigit() }.length < 5) {
            return "شماره تماس معتبر نیست"
        }
        val exists = suppliers.any { it.id == supplier.id }
        val list = if (exists) suppliers.map { if (it.id == supplier.id) supplier else it } else suppliers + supplier
        update { it.copy(suppliers = list) }
        return null
    }

    fun deleteSupplier(id: String): String? {
        if (ledger.any { it.partyId == id } || cheques.any { it.partyId == id }) {
            return "این تأمین‌کننده گردش حساب یا چک دارد و حذف نمی‌شود."
        }
        update { it.copy(suppliers = it.suppliers.filter { s -> s.id != id }) }
        return null
    }

    /** Buys goods from a supplier: stock goes up, costs are refreshed and the debt is recorded. */
    fun recordPurchase(supplierId: String, lines: List<PurchaseLine>, extraCost: Long, note: String): List<String> {
        if (suppliers.none { it.id == supplierId }) return listOf("تأمین‌کننده پیدا نشد")
        val errors = PurchaseMath.validate(lines, products)
        if (errors.isNotEmpty()) return errors
        val result = PurchaseMath.apply(lines, extraCost, products)
        val summary = "خرید ${result.totalUnits} عدد کالا" + if (note.isNotBlank()) " — ${note.trim()}" else ""
        val entry = LedgerEntry(newId(), supplierId, EntryType.PURCHASE, result.total, now(), null, summary)
        update { it.copy(products = result.products, ledger = it.ledger + entry) }
        rebuildInventory()
        return emptyList()
    }

    // ---------- expenses ----------

    fun addExpense(category: ExpenseCategory, amount: Long, note: String, at: Long): String? {
        if (amount <= 0) return "مبلغ هزینه را وارد کنید"
        val expense = Expense(newId(), category, amount, at, note.trim())
        update { it.copy(expenses = it.expenses + expense) }
        return null
    }

    fun deleteExpense(id: String) {
        update { it.copy(expenses = it.expenses.filter { e -> e.id != id }) }
    }

    // ---------- bank accounts ----------

    /** Returns an error message, or null when saved. */
    fun saveBankAccount(account: BankAccount): String? {
        if (account.bankName.isBlank()) return "نام بانک را وارد کنید"
        if (account.ownerName.isBlank()) return "نام صاحب حساب را وارد کنید"
        if (account.cardNumber.isBlank() && account.sheba.isBlank() && account.accountNumber.isBlank()) {
            return "حداقل یکی از شماره کارت، شبا یا شماره حساب را وارد کنید"
        }
        if (account.cardNumber.isNotBlank() && !Validators.isValidCardNumber(account.cardNumber)) return "شماره کارت معتبر نیست"
        if (account.sheba.isNotBlank() && !Validators.isValidSheba(account.sheba)) return "شماره شبا معتبر نیست"
        val exists = bankAccounts.any { it.id == account.id }
        val list = if (exists) bankAccounts.map { if (it.id == account.id) account else it } else bankAccounts + account
        update { it.copy(bankAccounts = list) }
        return null
    }

    fun deleteBankAccount(id: String) {
        update { it.copy(bankAccounts = it.bankAccounts.filter { b -> b.id != id }) }
    }

    // ---------- special prices ----------

    fun customPricesOf(customerId: String): Map<String, Long> = PriceBook.forCustomer(customPrices, customerId)

    /** Returns an error message, or null when saved. */
    fun setCustomPrice(customerId: String, productId: String, price: Long): String? {
        if (price <= 0) return "قیمت را وارد کنید"
        if (customers.none { it.id == customerId }) return "مشتری پیدا نشد"
        if (products.none { it.id == productId }) return "کالا پیدا نشد"
        update { it.copy(customPrices = PriceBook.set(it.customPrices, customerId, productId, price)) }
        return null
    }

    fun removeCustomPrice(customerId: String, productId: String) {
        update { it.copy(customPrices = PriceBook.remove(it.customPrices, customerId, productId)) }
    }

    // ---------- bulk edit ----------

    /** Returns validation errors, or the result of the edit when none. */
    fun bulkEdit(ids: Set<String>, edit: BulkEdit): Pair<List<String>, BulkResult?> {
        if (ids.isEmpty()) return listOf("هیچ کالایی انتخاب نشده است") to null
        val errors = BulkEditEngine.validate(edit, categories)
        if (errors.isNotEmpty()) return errors to null
        val result = BulkEditEngine.apply(products, ids, edit)
        update { it.copy(products = result.products) }
        rebuildInventory()
        return emptyList<String>() to result
    }

    /** Deletes the products that are not used by any order. Returns (deleted, skipped). */
    fun bulkDelete(ids: Set<String>): Pair<Int, Int> {
        var deleted = 0
        var skipped = 0
        for (id in ids) {
            if (deleteProduct(id) == null) deleted++ else skipped++
        }
        return deleted to skipped
    }

    // ---------- invoice picture ----------

    fun invoiceModel(orderId: String): InvoiceModel? {
        val order = order(orderId) ?: return null
        val name = partyDisplayName(order.customerId)
        return InvoiceBuilder.build(order, name, settings, balanceOf(order.customerId))
    }

    private fun partyDisplayName(id: String): String =
        customers.firstOrNull { it.id == id }?.fullName ?: suppliers.firstOrNull { it.id == id }?.name ?: "نامشخص"

    // ---------- backup and restore ----------

    /** Writes backup-<date>.csbk (encrypted with [password]) in the cache folder. Call off the main thread. */
    fun createBackup(password: CharArray): BackupFile {
        val createdAt = now()
        val dir = File(appContext.cacheDir, "backups")
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val d = JalaliCalendar.fromEpochMillis(createdAt)
        val name = "backup-" + d.year + "-" + d.month.toString().padStart(2, '0') + "-" + d.day.toString().padStart(2, '0') + ".csbk"
        val plain = File(dir, "plain.tmp")
        var storedImages = 0
        try {
            plain.outputStream().buffered().use { out ->
                storedImages = BackupPackage.write(out, snapshot, createdAt) { imageName -> images.read(imageName) }
            }
            val target = File(dir, name)
            FileInputStream(plain).use { input ->
                FileOutputStream(target).buffered().use { out -> BackupCrypto.encrypt(input, out, password) }
            }
            prefs.edit().putLong("last_backup_at", createdAt).apply()
            lastBackupAt = createdAt
            return BackupFile(target, storedImages, target.length())
        } finally {
            plain.delete()
        }
    }

    /**
     * Decrypts and checks a backup without touching current data.
     * @throws com.hamid.clothing.core.WrongPasswordException when the password is wrong or the file is damaged
     */
    fun prepareRestore(uri: Uri, password: CharArray): PreparedRestore {
        val cache = File(appContext.cacheDir, "restore")
        cache.mkdirs()
        val zip = File(cache, "restore.zip")
        val staging = images.newStaging()
        try {
            val input = appContext.contentResolver.openInputStream(uri) ?: throw java.io.IOException("cannot open file")
            input.use { src -> FileOutputStream(zip).buffered().use { out -> BackupCrypto.decrypt(src, out, password) } }
            val data = FileInputStream(zip).buffered().use { zin ->
                BackupPackage.read(zin) { imageName, bytes -> File(staging, imageName).writeBytes(bytes) }
            }
            return PreparedRestore(data.snapshot, staging, data.createdAt)
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw e
        } finally {
            zip.delete()
        }
    }

    /** Replaces every piece of data with the prepared backup. Call on the main thread. */
    fun applyRestore(prepared: PreparedRestore) {
        images.replaceWith(prepared.imagesDir)
        snapshot = prepared.snapshot
        persist()
        rebuildInventory()
        expireOrders()
    }

    fun cancelRestore(prepared: PreparedRestore) {
        prepared.imagesDir.deleteRecursively()
    }

    // ---------- appearance ----------

    private fun readThemeMode(): ThemeMode {
        val saved = prefs.getString("theme_mode", null)
        return ThemeMode.values().firstOrNull { it.name == saved } ?: ThemeMode.DARK
    }

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    // ---------- publishing the catalogue ----------

    /**
     * Builds catalog-<date>.zip (products without costs, photos, shop details) in the cache folder.
     * Reads photos from disk, so call it off the main thread.
     */
    fun buildCatalogFile(): PublishResult {
        val publishedAt = now()
        val data = CatalogBuilder.fromSnapshot(snapshot, publishedAt)
        val dir = File(appContext.cacheDir, "catalog")
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val d = JalaliCalendar.fromEpochMillis(publishedAt)
        val name = "catalog-" + d.year + "-" + d.month.toString().padStart(2, '0') + "-" + d.day.toString().padStart(2, '0') + ".zip"
        val file = File(dir, name)
        var storedImages = 0
        file.outputStream().buffered().use { out ->
            storedImages = CatalogPackage.write(out, data) { imageName -> images.read(imageName) }
        }
        prefs.edit().putLong("catalog_published_at", publishedAt).apply()
        lastPublishedAt = publishedAt
        return PublishResult(file, data.products.size, storedImages, file.length())
    }

    // ---------- settings / bulk ----------

    fun saveSettings(settings: StoreSettings) {
        update { it.copy(settings = settings) }
        rebuildInventory()
    }

    /** Removes business data. Categories and settings are kept. */
    fun clearAllData() {
        update { StoreSnapshot(sample = false, categories = it.categories, settings = it.settings) }
        images.deleteAll()
        rebuildInventory()
    }

    fun loadSampleData() {
        update { SampleData.snapshot(now()) }
        images.deleteAll()
        rebuildInventory()
    }
}
