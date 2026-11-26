package com.hamid.clothing.core

import org.json.JSONArray
import org.json.JSONObject

/** Everything the admin app keeps locally until a server exists. */
data class StoreSnapshot(
    /** true while the data is the built-in demo data */
    val sample: Boolean = false,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val ledger: List<LedgerEntry> = emptyList(),
    val orders: List<Order> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val cheques: List<Cheque> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList(),
    val settings: StoreSettings = StoreSettings(),
    /** Special prices agreed with single customers. */
    val customPrices: List<CustomerPrice> = emptyList()
)

/** JSON (de)serialisation of [StoreSnapshot]. Uses org.json, which Android provides. */
object DataCodec {
    const val VERSION = 1

    fun encode(s: StoreSnapshot): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("sample", s.sample)
        root.put("categories", jsonArray(s.categories) { category(it) })
        root.put("products", jsonArray(s.products) { product(it) })
        root.put("customers", jsonArray(s.customers) { customer(it) })
        root.put("ledger", jsonArray(s.ledger) { entry(it) })
        root.put("orders", jsonArray(s.orders) { order(it) })
        root.put("suppliers", jsonArray(s.suppliers) { supplier(it) })
        root.put("cheques", jsonArray(s.cheques) { cheque(it) })
        root.put("expenses", jsonArray(s.expenses) { expense(it) })
        root.put("bankAccounts", jsonArray(s.bankAccounts) { bank(it) })
        root.put("settings", settings(s.settings))
        root.put("customPrices", jsonArray(s.customPrices) { c ->
            val o = JSONObject()
            o.put("customerId", c.customerId)
            o.put("productId", c.productId)
            o.put("price", c.price)
            o
        })
        return root.toString()
    }

    fun decode(text: String): StoreSnapshot {
        val root = JSONObject(text)
        val version = root.optInt("version", 0)
        require(version == VERSION) { "unsupported data version $version" }
        return StoreSnapshot(
            sample = root.optBoolean("sample", false),
            categories = root.optJSONArray("categories").objects { readCategory(it) },
            products = root.optJSONArray("products").objects { readProduct(it) },
            customers = root.optJSONArray("customers").objects { readCustomer(it) },
            ledger = root.optJSONArray("ledger").objects { readEntry(it) },
            orders = root.optJSONArray("orders").objects { readOrder(it) },
            suppliers = root.optJSONArray("suppliers").objects { readSupplier(it) },
            cheques = root.optJSONArray("cheques").objects { readCheque(it) },
            expenses = root.optJSONArray("expenses").objects { readExpense(it) },
            bankAccounts = root.optJSONArray("bankAccounts").objects { readBank(it) },
            settings = root.optJSONObject("settings")?.let { readSettings(it) } ?: StoreSettings(),
            customPrices = root.optJSONArray("customPrices").objects {
                CustomerPrice(it.getString("customerId"), it.getString("productId"), it.getLong("price"))
            }
        )
    }

    // ---------- order exchange files (customer app <-> admin app) ----------

    /** "order", "order-update" or null when the text is not one of our exchange files. */
    fun exchangeKind(text: String): String? =
        try {
            val o = JSONObject(text)
            if (o.optInt("version", 0) == VERSION) o.optString("kind", "").ifBlank { null } else null
        } catch (e: Exception) {
            null
        }

    fun encodeOrderFile(f: OrderFile): String {
        val o = JSONObject()
        o.put("version", VERSION)
        o.put("kind", "order")
        o.put("clientOrderId", f.clientOrderId)
        o.put("clientNumber", f.clientNumber)
        o.put("createdAt", f.createdAt)
        o.put("customerName", f.customerName)
        o.put("customerPhone", f.customerPhone)
        o.put("receiverName", f.receiverName)
        o.put("receiverPhone", f.receiverPhone)
        o.put("address", f.address)
        o.put("paymentMethod", f.paymentMethod.name)
        o.put("shippingMethod", f.shippingMethod.name)
        o.put("note", f.note)
        o.put("lines", jsonArray(f.lines) { l ->
            val j = JSONObject()
            j.put("productId", l.productId)
            j.put("productName", l.productName)
            j.put("color", l.color)
            j.put("size", l.size)
            j.put("quantity", l.quantity)
            j.put("unitPrice", l.unitPrice)
            j
        })
        return o.toString()
    }

    fun decodeOrderFile(text: String): OrderFile {
        require(text.length <= OrderExchange.MAX_CHARS) { "order file is too large" }
        val o = JSONObject(text)
        require(o.optInt("version", 0) == VERSION && o.optString("kind", "") == "order") { "not an order file" }
        fun clip(s: String) = s.trim().take(OrderExchange.MAX_TEXT)
        val lines = o.optJSONArray("lines").objects {
            val qty = it.getInt("quantity")
            require(qty in 1..OrderExchange.MAX_QUANTITY) { "bad quantity" }
            OrderFileLine(
                clip(it.getString("productId")), clip(it.optString("productName", "")), clip(it.getString("color")),
                clip(it.getString("size")), qty, it.optLong("unitPrice", 0L).coerceAtLeast(0L)
            )
        }
        require(lines.isNotEmpty() && lines.size <= OrderExchange.MAX_LINES) { "bad number of lines" }
        return OrderFile(
            clientOrderId = clip(o.getString("clientOrderId")),
            clientNumber = clip(o.optString("clientNumber", "")),
            createdAt = o.optLong("createdAt", 0L),
            customerName = clip(o.optString("customerName", "")),
            customerPhone = clip(o.optString("customerPhone", "")),
            receiverName = clip(o.optString("receiverName", "")),
            receiverPhone = clip(o.optString("receiverPhone", "")),
            address = clip(o.optString("address", "")),
            paymentMethod = enumOf(o.optString("paymentMethod", ""), PaymentMethod.CARD_TO_CARD),
            shippingMethod = enumOf(o.optString("shippingMethod", ""), ShippingMethod.COURIER),
            lines = lines,
            note = clip(o.optString("note", ""))
        )
    }

    fun encodeOrderUpdate(u: OrderUpdateFile): String {
        val o = JSONObject()
        o.put("version", VERSION)
        o.put("kind", "order-update")
        o.put("clientOrderId", u.clientOrderId)
        o.put("adminNumber", u.adminNumber)
        o.put("status", u.status.name)
        o.put("paidAmount", u.paidAmount)
        o.put("total", u.total)
        o.put("shippingCost", u.shippingCost)
        if (u.trackingCode != null) o.put("trackingCode", u.trackingCode)
        o.put("at", u.at)
        return o.toString()
    }

    fun decodeOrderUpdate(text: String): OrderUpdateFile {
        require(text.length <= OrderExchange.MAX_CHARS) { "update file is too large" }
        val o = JSONObject(text)
        require(o.optInt("version", 0) == VERSION && o.optString("kind", "") == "order-update") { "not an order update" }
        return OrderUpdateFile(
            clientOrderId = o.getString("clientOrderId").take(OrderExchange.MAX_TEXT),
            adminNumber = o.optString("adminNumber", "").take(OrderExchange.MAX_TEXT),
            status = enumOf(o.getString("status"), OrderStatus.NEW),
            paidAmount = o.optLong("paidAmount", 0L).coerceAtLeast(0L),
            total = o.optLong("total", 0L).coerceAtLeast(0L),
            shippingCost = o.optLong("shippingCost", 0L).coerceAtLeast(0L),
            trackingCode = o.nullableString("trackingCode")?.take(OrderExchange.MAX_TEXT),
            at = o.optLong("at", 0L)
        )
    }

    // ---------- published catalog (what customers receive) ----------

    fun encodeCatalog(c: CatalogData): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("kind", "catalog")
        root.put("publishedAt", c.publishedAt)
        val st = JSONObject()
        st.put("shopName", c.store.shopName)
        st.put("phone", c.store.phone)
        st.put("address", c.store.address)
        st.put("whatsapp", c.store.whatsapp)
        st.put("telegram", c.store.telegram)
        st.put("instagram", c.store.instagram)
        st.put("rubika", c.store.rubika)
        st.put("eitaa", c.store.eitaa)
        st.put("bale", c.store.bale)
        st.put("courierCost", c.store.courierCost)
        st.put("postCost", c.store.postCost)
        root.put("store", st)
        root.put("categories", jsonArray(c.categories) { category(it) })
        root.put("products", jsonArray(c.products) { catalogProduct(it) })
        root.put("discounts", jsonArray(c.discounts) { discount(it) })
        root.put("bankAccounts", jsonArray(c.bankAccounts) { bank(it) })
        return root.toString()
    }

    fun decodeCatalog(text: String): CatalogData {
        val root = JSONObject(text)
        require(root.optInt("version", 0) == VERSION) { "unsupported catalog version" }
        require(root.optString("kind", "") == "catalog") { "not a catalog file" }
        val st = root.optJSONObject("store") ?: JSONObject()
        return CatalogData(
            publishedAt = root.optLong("publishedAt", 0L),
            store = CatalogStoreInfo(
                shopName = st.optString("shopName", ""),
                phone = st.optString("phone", ""),
                address = st.optString("address", ""),
                whatsapp = st.optString("whatsapp", ""),
                telegram = st.optString("telegram", ""),
                instagram = st.optString("instagram", ""),
                rubika = st.optString("rubika", ""),
                eitaa = st.optString("eitaa", ""),
                bale = st.optString("bale", ""),
                courierCost = st.optLong("courierCost", 0L),
                postCost = st.optLong("postCost", 0L)
            ),
            categories = root.optJSONArray("categories").objects { readCategory(it) },
            products = root.optJSONArray("products").objects { readCatalogProduct(it) },
            discounts = root.optJSONArray("discounts").objects { readDiscount(it) },
            bankAccounts = root.optJSONArray("bankAccounts").objects { readBank(it) }
        )
    }

    // ---------- customer app data ----------

    fun encodeCustomerApp(d: CustomerAppData): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("kind", "customer-app")
        root.put("loggedIn", d.loggedIn)
        if (d.customer != null) root.put("customer", customer(d.customer))
        root.put("addresses", strings(d.addresses))
        root.put("favorites", strings(d.favorites))
        root.put("cart", jsonArray(d.cart) { l ->
            val o = JSONObject()
            o.put("productId", l.productId)
            o.put("color", l.color)
            o.put("size", l.size)
            o.put("quantity", l.quantity)
            o
        })
        root.put("orders", jsonArray(d.orders) { order(it) })
        root.put("ledger", jsonArray(d.ledger) { entry(it) })
        val w = d.wholesaleRequest
        if (w != null) {
            val o = JSONObject()
            o.put("businessName", w.businessName)
            o.put("city", w.city)
            o.put("note", w.note)
            o.put("at", w.at)
            root.put("wholesaleRequest", o)
        }
        return root.toString()
    }

    fun decodeCustomerApp(text: String): CustomerAppData {
        val root = JSONObject(text)
        val version = root.optInt("version", 0)
        require(version == VERSION) { "unsupported data version $version" }
        val customerObj = root.optJSONObject("customer")
        val requestObj = root.optJSONObject("wholesaleRequest")
        return CustomerAppData(
            customer = if (customerObj == null) null else readCustomer(customerObj),
            addresses = root.optJSONArray("addresses").strings(),
            favorites = root.optJSONArray("favorites").strings().toSet(),
            cart = root.optJSONArray("cart").objects {
                CartLine(it.getString("productId"), it.getString("color"), it.getString("size"), it.getInt("quantity"))
            },
            orders = root.optJSONArray("orders").objects { readOrder(it) },
            ledger = root.optJSONArray("ledger").objects { readEntry(it) },
            wholesaleRequest = if (requestObj == null) null else WholesaleRequest(
                requestObj.optString("businessName", ""),
                requestObj.optString("city", ""),
                requestObj.optString("note", ""),
                requestObj.optLong("at", 0L)
            ),
            loggedIn = root.optBoolean("loggedIn", false)
        )
    }

    // ---------- helpers ----------

    private fun <T> jsonArray(items: List<T>, f: (T) -> JSONObject): JSONArray {
        val a = JSONArray()
        for (item in items) a.put(f(item))
        return a
    }

    private fun <T> JSONArray?.objects(f: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>()
        for (i in 0 until length()) out.add(f(getJSONObject(i)))
        return out
    }

    private fun strings(items: Collection<String>): JSONArray {
        val a = JSONArray()
        for (s in items) a.put(s)
        return a
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        val out = ArrayList<String>()
        for (i in 0 until length()) out.add(getString(i))
        return out
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private inline fun <reified T : Enum<T>> enumOf(name: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    // ---------- category ----------

    private fun category(c: Category): JSONObject {
        val o = JSONObject()
        o.put("id", c.id)
        o.put("name", c.name)
        if (c.parentId != null) o.put("parentId", c.parentId)
        return o
    }

    private fun readCategory(o: JSONObject) =
        Category(o.getString("id"), o.getString("name"), o.nullableString("parentId"))

    // ---------- product ----------

    private fun product(p: Product): JSONObject {
        val o = JSONObject()
        o.put("id", p.id)
        o.put("code", p.code)
        o.put("name", p.name)
        o.put("categoryId", p.categoryId)
        o.put("purchasePrice", p.purchasePrice)
        o.put("landedCost", p.landedCost)
        o.put("retailPrice", p.retailPrice)
        o.put("wholesalePrice", p.wholesalePrice)
        o.put("compareAtPrice", p.compareAtPrice)
        o.put("description", p.description)
        o.put("imageUris", strings(p.imageUris))
        if (p.barcode != null) o.put("barcode", p.barcode)
        o.put("lowStockThreshold", p.lowStockThreshold)
        o.put("active", p.active)
        o.put("variants", jsonArray(p.variants) { v ->
            val j = JSONObject()
            j.put("color", v.color)
            j.put("size", v.size)
            j.put("stock", v.stock)
            j
        })
        return o
    }

    private fun readProduct(o: JSONObject) = Product(
        id = o.getString("id"),
        code = o.getString("code"),
        name = o.getString("name"),
        categoryId = o.getString("categoryId"),
        purchasePrice = o.optLong("purchasePrice", 0L),
        landedCost = o.optLong("landedCost", 0L),
        retailPrice = o.optLong("retailPrice", 0L),
        wholesalePrice = o.optLong("wholesalePrice", 0L),
        compareAtPrice = o.optLong("compareAtPrice", 0L),
        variants = o.optJSONArray("variants").objects { Variant(it.getString("color"), it.getString("size"), it.optInt("stock", 0)) },
        description = o.optString("description", ""),
        imageUris = o.optJSONArray("imageUris").strings(),
        barcode = o.nullableString("barcode"),
        lowStockThreshold = o.optInt("lowStockThreshold", 3),
        active = o.optBoolean("active", true)
    )

    // ---------- customer ----------

    private fun customer(c: Customer): JSONObject {
        val o = JSONObject()
        o.put("id", c.id)
        o.put("fullName", c.fullName)
        o.put("phone", c.phone)
        o.put("address", c.address)
        o.put("type", c.type.name)
        o.put("wholesaleStatus", c.wholesaleStatus.name)
        o.put("joinedAt", c.joinedAt)
        o.put("creditPolicy", c.creditPolicy.name)
        o.put("creditLimit", c.creditLimit)
        o.put("settlementDays", c.settlementDays)
        o.put("blocked", c.blocked)
        o.put("groups", strings(c.groups))
        o.put("adminNote", c.adminNote)
        return o
    }

    private fun readCustomer(o: JSONObject) = Customer(
        id = o.getString("id"),
        fullName = o.getString("fullName"),
        phone = o.getString("phone"),
        address = o.optString("address", ""),
        type = enumOf(o.optString("type", ""), CustomerType.RETAIL),
        wholesaleStatus = enumOf(o.optString("wholesaleStatus", ""), WholesaleStatus.NONE),
        joinedAt = o.optLong("joinedAt", 0L),
        creditPolicy = enumOf(o.optString("creditPolicy", ""), CreditPolicy.NO_CREDIT),
        creditLimit = o.optLong("creditLimit", 0L),
        settlementDays = o.optInt("settlementDays", 0),
        blocked = o.optBoolean("blocked", false),
        groups = o.optJSONArray("groups").strings().toSet(),
        adminNote = o.optString("adminNote", "")
    )

    // ---------- ledger entry ----------

    private fun entry(e: LedgerEntry): JSONObject {
        val o = JSONObject()
        o.put("id", e.id)
        o.put("partyId", e.partyId)
        o.put("type", e.type.name)
        o.put("amount", e.amount)
        o.put("at", e.at)
        if (e.refId != null) o.put("refId", e.refId)
        o.put("note", e.note)
        return o
    }

    private fun readEntry(o: JSONObject) = LedgerEntry(
        id = o.getString("id"),
        partyId = o.getString("partyId"),
        type = enumOf(o.getString("type"), EntryType.ADJUSTMENT_UP),
        amount = o.getLong("amount"),
        at = o.optLong("at", 0L),
        refId = o.nullableString("refId"),
        note = o.optString("note", "")
    )

    // ---------- order ----------

    private fun order(o: Order): JSONObject {
        val j = JSONObject()
        j.put("id", o.id)
        j.put("number", o.number)
        j.put("customerId", o.customerId)
        j.put("channel", o.channel.name)
        j.put("paymentMethod", o.paymentMethod.name)
        j.put("shippingMethod", o.shippingMethod.name)
        j.put("createdAt", o.createdAt)
        j.put("status", o.status.name)
        j.put("shippingCost", o.shippingCost)
        j.put("extraDiscount", o.extraDiscount)
        j.put("paidAmount", o.paidAmount)
        if (o.trackingCode != null) j.put("trackingCode", o.trackingCode)
        j.put("stockCommitted", o.stockCommitted)
        j.put("receiverName", o.receiverName)
        j.put("receiverPhone", o.receiverPhone)
        j.put("address", o.address)
        if (o.externalRef.isNotBlank()) j.put("externalRef", o.externalRef)
        if (o.note.isNotBlank()) j.put("note", o.note)
        j.put("items", jsonArray(o.items) { i ->
            val x = JSONObject()
            x.put("productId", i.productId)
            x.put("productName", i.productName)
            x.put("color", i.color)
            x.put("size", i.size)
            x.put("quantity", i.quantity)
            x.put("unitPrice", i.unitPrice)
            x.put("unitCost", i.unitCost)
            x.put("discountPercent", i.discountPercent)
            x
        })
        j.put("history", jsonArray(o.history) { h ->
            val x = JSONObject()
            if (h.from != null) x.put("from", h.from.name)
            x.put("to", h.to.name)
            x.put("at", h.at)
            x.put("note", h.note)
            x
        })
        return j
    }

    private fun readOrder(j: JSONObject) = Order(
        id = j.getString("id"),
        number = j.getString("number"),
        customerId = j.getString("customerId"),
        channel = enumOf(j.optString("channel", ""), Channel.RETAIL),
        items = j.optJSONArray("items").objects {
            OrderItem(
                it.getString("productId"), it.getString("productName"), it.getString("color"), it.getString("size"),
                it.getInt("quantity"), it.getLong("unitPrice"), it.optLong("unitCost", 0L), it.optInt("discountPercent", 0)
            )
        },
        paymentMethod = enumOf(j.optString("paymentMethod", ""), PaymentMethod.CARD_TO_CARD),
        shippingMethod = enumOf(j.optString("shippingMethod", ""), ShippingMethod.COURIER),
        createdAt = j.optLong("createdAt", 0L),
        status = enumOf(j.optString("status", ""), OrderStatus.NEW),
        shippingCost = j.optLong("shippingCost", 0L),
        extraDiscount = j.optLong("extraDiscount", 0L),
        paidAmount = j.optLong("paidAmount", 0L),
        trackingCode = j.nullableString("trackingCode"),
        stockCommitted = j.optBoolean("stockCommitted", false),
        history = j.optJSONArray("history").objects {
            val from = it.nullableString("from")
            StatusChange(
                if (from == null) null else enumOf(from, OrderStatus.NEW),
                enumOf(it.optString("to", ""), OrderStatus.NEW),
                it.optLong("at", 0L),
                it.optString("note", "")
            )
        },
        receiverName = j.optString("receiverName", ""),
        receiverPhone = j.optString("receiverPhone", ""),
        address = j.optString("address", ""),
        externalRef = j.optString("externalRef", ""),
        note = j.optString("note", "")
    )

    // ---------- supplier, bank, cheque, expense, settings ----------

    private fun supplier(x: Supplier): JSONObject {
        val o = JSONObject()
        o.put("id", x.id)
        o.put("name", x.name)
        o.put("phone", x.phone)
        o.put("note", x.note)
        return o
    }

    private fun readSupplier(o: JSONObject) =
        Supplier(o.getString("id"), o.getString("name"), o.optString("phone", ""), o.optString("note", ""))

    private fun bank(b: BankAccount): JSONObject {
        val o = JSONObject()
        o.put("id", b.id)
        o.put("bankName", b.bankName)
        o.put("ownerName", b.ownerName)
        o.put("cardNumber", b.cardNumber)
        o.put("sheba", b.sheba)
        o.put("accountNumber", b.accountNumber)
        o.put("active", b.active)
        o.put("visibleToCustomers", b.visibleToCustomers)
        return o
    }

    private fun readBank(o: JSONObject) = BankAccount(
        id = o.getString("id"),
        bankName = o.optString("bankName", ""),
        ownerName = o.optString("ownerName", ""),
        cardNumber = o.optString("cardNumber", ""),
        sheba = o.optString("sheba", ""),
        accountNumber = o.optString("accountNumber", ""),
        active = o.optBoolean("active", true),
        visibleToCustomers = o.optBoolean("visibleToCustomers", false)
    )

    private fun cheque(c: Cheque): JSONObject {
        val o = JSONObject()
        o.put("id", c.id)
        o.put("direction", c.direction.name)
        o.put("partyId", c.partyId)
        o.put("amount", c.amount)
        o.put("sayadId", c.sayadId)
        o.put("dueDate", c.dueDate)
        o.put("bankName", c.bankName)
        o.put("status", c.status.name)
        o.put("note", c.note)
        return o
    }

    private fun readCheque(o: JSONObject) = Cheque(
        id = o.getString("id"),
        direction = enumOf(o.optString("direction", ""), ChequeDirection.RECEIVED),
        partyId = o.getString("partyId"),
        amount = o.getLong("amount"),
        sayadId = o.getString("sayadId"),
        dueDate = o.optLong("dueDate", 0L),
        bankName = o.optString("bankName", ""),
        status = enumOf(o.optString("status", ""), ChequeStatus.PENDING),
        note = o.optString("note", "")
    )

    private fun expense(e: Expense): JSONObject {
        val o = JSONObject()
        o.put("id", e.id)
        o.put("category", e.category.name)
        o.put("amount", e.amount)
        o.put("at", e.at)
        o.put("note", e.note)
        return o
    }

    private fun readExpense(o: JSONObject) = Expense(
        id = o.getString("id"),
        category = enumOf(o.optString("category", ""), ExpenseCategory.OTHER),
        amount = o.getLong("amount"),
        at = o.optLong("at", 0L),
        note = o.optString("note", "")
    )

    private fun settings(s: StoreSettings): JSONObject {
        val o = JSONObject()
        o.put("shopName", s.shopName)
        o.put("phone", s.phone)
        o.put("address", s.address)
        o.put("retailHoldHours", s.retailHoldHours)
        o.put("wholesaleHoldHours", s.wholesaleHoldHours)
        o.put("whatsapp", s.whatsapp)
        o.put("telegram", s.telegram)
        o.put("instagram", s.instagram)
        o.put("rubika", s.rubika)
        o.put("eitaa", s.eitaa)
        o.put("bale", s.bale)
        o.put("courierCost", s.courierCost)
        o.put("postCost", s.postCost)
        return o
    }

    private fun readSettings(o: JSONObject) = StoreSettings(
        shopName = o.optString("shopName", ""),
        phone = o.optString("phone", ""),
        address = o.optString("address", ""),
        retailHoldHours = o.optInt("retailHoldHours", 24),
        wholesaleHoldHours = o.optInt("wholesaleHoldHours", 0),
        whatsapp = o.optString("whatsapp", ""),
        telegram = o.optString("telegram", ""),
        instagram = o.optString("instagram", ""),
        rubika = o.optString("rubika", ""),
        eitaa = o.optString("eitaa", ""),
        bale = o.optString("bale", ""),
        courierCost = o.optLong("courierCost", 0L),
        postCost = o.optLong("postCost", 0L)
    )

    // ---------- catalog product and discount ----------

    /** Customer-facing product: no purchase or landed cost is ever written. */
    private fun catalogProduct(p: Product): JSONObject {
        val o = JSONObject()
        o.put("id", p.id)
        o.put("code", p.code)
        o.put("name", p.name)
        o.put("categoryId", p.categoryId)
        o.put("description", p.description)
        o.put("retailPrice", p.retailPrice)
        o.put("wholesalePrice", p.wholesalePrice)
        o.put("compareAtPrice", p.compareAtPrice)
        o.put("active", p.active)
        if (p.barcode != null) o.put("barcode", p.barcode)
        o.put("lowStockThreshold", p.lowStockThreshold)
        o.put("images", strings(p.imageUris))
        o.put("variants", jsonArray(p.variants) { v ->
            val j = JSONObject()
            j.put("color", v.color)
            j.put("size", v.size)
            j.put("stock", v.stock)
            j
        })
        return o
    }

    private fun readCatalogProduct(o: JSONObject) = Product(
        id = o.getString("id"),
        code = o.getString("code"),
        name = o.getString("name"),
        categoryId = o.getString("categoryId"),
        purchasePrice = 0L,
        landedCost = 0L,
        retailPrice = o.optLong("retailPrice", 0L),
        wholesalePrice = o.optLong("wholesalePrice", 0L),
        compareAtPrice = o.optLong("compareAtPrice", 0L),
        variants = o.optJSONArray("variants").objects { Variant(it.getString("color"), it.getString("size"), it.optInt("stock", 0)) },
        description = o.optString("description", ""),
        imageUris = o.optJSONArray("images").strings(),
        barcode = o.nullableString("barcode"),
        lowStockThreshold = o.optInt("lowStockThreshold", 3),
        active = o.optBoolean("active", true)
    )

    private fun discount(d: Discount): JSONObject {
        val o = JSONObject()
        o.put("id", d.id)
        o.put("title", d.title)
        o.put("percent", d.percent)
        o.put("scope", d.scope.name)
        o.put("targetIds", strings(d.targetIds))
        if (d.code != null) o.put("code", d.code)
        o.put("startsAt", d.startsAt)
        o.put("endsAt", d.endsAt)
        o.put("active", d.active)
        return o
    }

    private fun readDiscount(o: JSONObject) = Discount(
        id = o.getString("id"),
        title = o.optString("title", ""),
        percent = o.getInt("percent"),
        scope = enumOf(o.optString("scope", ""), DiscountScope.ALL),
        targetIds = o.optJSONArray("targetIds").strings().toSet(),
        code = o.nullableString("code"),
        startsAt = o.optLong("startsAt", 0L),
        endsAt = o.optLong("endsAt", Long.MAX_VALUE),
        active = o.optBoolean("active", true)
    )
}
