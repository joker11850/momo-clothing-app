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
    val ledger: List<LedgerEntry> = emptyList()
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
            ledger = root.optJSONArray("ledger").objects { readEntry(it) }
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
}
