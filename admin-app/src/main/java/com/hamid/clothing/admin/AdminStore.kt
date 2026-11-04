package com.hamid.clothing.admin

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hamid.clothing.core.Category
import com.hamid.clothing.core.Customer
import com.hamid.clothing.core.CustomerValidator
import com.hamid.clothing.core.DataCodec
import com.hamid.clothing.core.EntryType
import com.hamid.clothing.core.LedgerEntry
import com.hamid.clothing.core.LedgerMath
import com.hamid.clothing.core.Product
import com.hamid.clothing.core.ProductValidator
import com.hamid.clothing.core.SampleData
import com.hamid.clothing.core.StoreSnapshot
import java.io.File
import java.util.UUID

/**
 * Single source of truth for the admin app. Data is local and encrypted; the same API will later
 * be backed by a server. Every change is written to disk immediately.
 */
class AdminStore(context: Context) {
    private val storage = SecureFile(File(context.filesDir, "admin-data.bin"))

    var snapshot by mutableStateOf(StoreSnapshot())
        private set

    /** One-time message for the user (for example when stored data could not be read). */
    var notice by mutableStateOf<String?>(null)
        private set

    val categories: List<Category> get() = snapshot.categories
    val products: List<Product> get() = snapshot.products
    val customers: List<Customer> get() = snapshot.customers
    val ledger: List<LedgerEntry> get() = snapshot.ledger
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
        return emptyList()
    }

    fun deleteProduct(id: String) {
        update { it.copy(products = it.products.filter { p -> p.id != id }) }
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
        if (ledger.any { it.partyId == id }) {
            return "این مشتری گردش حساب دارد و حذف نمی‌شود. می‌توانید او را مسدود کنید."
        }
        update { it.copy(customers = it.customers.filter { c -> c.id != id }) }
        return null
    }

    // ---------- customer account ----------

    /** Returns an error message, or null when the entry was recorded. */
    fun addLedgerEntry(customerId: String, type: EntryType, amount: Long, note: String): String? {
        if (amount <= 0) return "مبلغ باید بیشتر از صفر باشد"
        if (customers.none { it.id == customerId }) return "مشتری پیدا نشد"
        val entry = LedgerEntry(newId(), customerId, type, amount, now(), null, note.trim())
        update { it.copy(ledger = it.ledger + entry) }
        return null
    }

    // ---------- bulk ----------

    /** Removes products, customers and accounts. Categories are kept because they are needed to add products. */
    fun clearAllData() {
        update { StoreSnapshot(sample = false, categories = it.categories) }
    }

    fun loadSampleData() {
        update { SampleData.snapshot(now()) }
    }
}
