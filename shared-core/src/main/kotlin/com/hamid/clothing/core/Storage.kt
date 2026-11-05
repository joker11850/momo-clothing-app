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
    val settings: StoreSettings = StoreSettings()
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
            settings = root.optJSONObject("settings")?.let { readSettings(it) } ?: StoreSettings()
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
        address = j.optString("address", "")
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
        return o
    }

    private fun readSettings(o: JSONObject) = StoreSettings(
        shopName = o.optString("shopName", ""),
        phone = o.optString("phone", ""),
        address = o.optString("address", ""),
        retailHoldHours = o.optInt("retailHoldHours", 24),
        wholesaleHoldHours = o.optInt("wholesaleHoldHours", 0)
    )
}
