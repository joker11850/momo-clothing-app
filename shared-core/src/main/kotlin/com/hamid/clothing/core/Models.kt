package com.hamid.clothing.core

enum class Channel { RETAIL, WHOLESALE }
enum class CustomerType { RETAIL, WHOLESALE }
enum class WholesaleStatus { NONE, PENDING, APPROVED, REJECTED }
enum class CreditPolicy { NO_CREDIT, WITHIN_LIMIT, APPROVAL_REQUIRED }

data class Customer(
    val id: String,
    val fullName: String,
    val phone: String,
    val address: String = "",
    val type: CustomerType = CustomerType.RETAIL,
    val wholesaleStatus: WholesaleStatus = WholesaleStatus.NONE,
    val joinedAt: Long = 0L,
    val creditPolicy: CreditPolicy = CreditPolicy.NO_CREDIT,
    val creditLimit: Long = 0L,
    val settlementDays: Int = 0,
    val blocked: Boolean = false,
    val groups: Set<String> = emptySet(),
    val adminNote: String = ""
) {
    /** Wholesale prices are visible only to approved, non-blocked wholesale customers. */
    val canSeeWholesale: Boolean
        get() = !blocked && type == CustomerType.WHOLESALE && wholesaleStatus == WholesaleStatus.APPROVED
}

data class Category(val id: String, val name: String, val parentId: String? = null)

data class Variant(val color: String, val size: String, val stock: Int) {
    init { require(stock >= 0) { "stock must not be negative" } }
}

data class Product(
    val id: String,
    val code: String,
    val name: String,
    val categoryId: String,
    val purchasePrice: Long,
    val landedCost: Long,
    val retailPrice: Long,
    val wholesalePrice: Long,
    val variants: List<Variant>,
    val description: String = "",
    val imageUris: List<String> = emptyList(),
    val barcode: String? = null,
    val lowStockThreshold: Int = 3,
    val active: Boolean = true
) {
    init {
        require(purchasePrice >= 0 && landedCost >= 0 && retailPrice >= 0 && wholesalePrice >= 0) {
            "prices must not be negative"
        }
    }

    val totalStock: Int get() = variants.sumOf { it.stock }
}

data class Supplier(val id: String, val name: String, val phone: String = "", val note: String = "")

data class BankAccount(
    val id: String,
    val bankName: String,
    val ownerName: String,
    val cardNumber: String = "",
    val sheba: String = "",
    val accountNumber: String = "",
    val active: Boolean = true,
    val visibleToCustomers: Boolean = false
)
