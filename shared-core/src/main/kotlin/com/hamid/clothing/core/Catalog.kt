package com.hamid.clothing.core

object TextNorm {
    /** Lower-cases and unifies Arabic/Persian letter and digit variants so searches match. */
    fun normalize(input: String): String =
        Validators.toLatinDigits(input)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .trim()
            .lowercase()

    /** Makes Persian and Latin barcode digit forms compare equally. */
    fun normalizeBarcode(input: String): String = Validators.toLatinDigits(input).trim()
}

object ProductValidator {
    /** Returns Persian error messages; an empty list means the product is valid. [others] is the current catalogue. */
    fun validate(product: Product, others: List<Product>): List<String> {
        val errors = mutableListOf<String>()
        val rest = others.filter { it.id != product.id }

        if (product.name.isBlank()) errors.add("نام محصول را وارد کنید")
        if (product.code.isBlank()) {
            errors.add("کد کالا را وارد کنید")
        } else if (rest.any { TextNorm.normalize(it.code) == TextNorm.normalize(product.code) }) {
            errors.add("کد کالا تکراری است")
        }
        if (product.categoryId.isBlank()) errors.add("دسته‌بندی را انتخاب کنید")
        if (product.purchasePrice <= 0) errors.add("قیمت خرید باید بیشتر از صفر باشد")
        if (product.landedCost < product.purchasePrice) errors.add("قیمت تمام‌شده نمی‌تواند کمتر از قیمت خرید باشد")
        if (product.retailPrice <= 0) errors.add("قیمت فروش خرده باید بیشتر از صفر باشد")
        if (product.wholesalePrice > product.retailPrice && product.retailPrice > 0) {
            errors.add("قیمت عمده نباید بیشتر از قیمت خرده باشد")
        }
        if (product.compareAtPrice > 0 && product.compareAtPrice <= product.retailPrice) {
            errors.add("قیمت قبل از تخفیف باید بیشتر از قیمت فروش خرده باشد (یا خالی بماند)")
        }
        if (product.lowStockThreshold < 0) errors.add("حد هشدار موجودی نباید منفی باشد")

        if (product.variants.isEmpty()) {
            errors.add("حداقل یک ترکیب رنگ و سایز لازم است")
        } else {
            if (product.variants.any { it.color.isBlank() || it.size.isBlank() }) {
                errors.add("رنگ و سایز نباید خالی باشد")
            }
            val pairs = product.variants.map { TextNorm.normalize(it.color) + "|" + TextNorm.normalize(it.size) }
            if (pairs.size != pairs.toSet().size) errors.add("ترکیب رنگ و سایز تکراری است")
        }

        val barcode = product.barcode
        if (!barcode.isNullOrBlank() && rest.any {
                it.barcode?.let(TextNorm::normalizeBarcode) == TextNorm.normalizeBarcode(barcode)
            }) {
            errors.add("بارکد تکراری است")
        }
        return errors
    }
}

object CustomerValidator {
    /** [customer].phone must already be normalised (09xxxxxxxxx). */
    fun validate(customer: Customer, others: List<Customer>): List<String> {
        val errors = mutableListOf<String>()
        val rest = others.filter { it.id != customer.id }

        if (customer.fullName.isBlank()) errors.add("نام مشتری را وارد کنید")
        if (Validators.normalizeMobile(customer.phone) == null) {
            errors.add("شماره موبایل معتبر نیست")
        } else if (rest.any { it.phone == customer.phone }) {
            errors.add("این شماره قبلاً ثبت شده است")
        }
        if (customer.creditLimit < 0) errors.add("سقف اعتبار نباید منفی باشد")
        if (customer.settlementDays < 0) errors.add("مهلت تسویه نباید منفی باشد")
        if (customer.creditPolicy != CreditPolicy.NO_CREDIT && customer.type != CustomerType.WHOLESALE) {
            errors.add("خرید دفتری فقط برای مشتری عمده مجاز است")
        }
        if (customer.creditPolicy == CreditPolicy.WITHIN_LIMIT && customer.creditLimit <= 0) {
            errors.add("برای خرید دفتری تا سقف اعتبار، سقف را وارد کنید")
        }
        return errors
    }
}

object LedgerMath {
    /** partyId -> balance (positive = party owes the shop, for customers). */
    fun balances(entries: List<LedgerEntry>): Map<String, Long> =
        entries.groupBy { it.partyId }.mapValues { (_, list) -> list.sumOf { it.signed } }
}

enum class CustomerFilter { ALL, WHOLESALE, RETAIL, DEBTOR, BLOCKED, WHOLESALE_REQUEST }

object CatalogSearch {
    fun products(
        products: List<Product>,
        query: String,
        categoryId: String?,
        onlyLowStock: Boolean
    ): List<Product> {
        val q = TextNorm.normalize(query)
        return products.filter { p ->
            (categoryId == null || p.categoryId == categoryId) &&
                (!onlyLowStock || p.lowStockVariants.isNotEmpty()) &&
                (
                    q.isEmpty() ||
                        TextNorm.normalize(p.name).contains(q) ||
                        TextNorm.normalize(p.code).contains(q) ||
                        (p.barcode != null && TextNorm.normalizeBarcode(p.barcode).contains(TextNorm.normalizeBarcode(query)))
                    )
        }
    }

    fun customers(
        customers: List<Customer>,
        query: String,
        filter: CustomerFilter,
        balances: Map<String, Long>
    ): List<Customer> {
        val q = TextNorm.normalize(query)
        val digits = q.filter { it in '0'..'9' }
        return customers.filter { c ->
            val matchesFilter = when (filter) {
                CustomerFilter.ALL -> true
                CustomerFilter.WHOLESALE -> c.type == CustomerType.WHOLESALE
                CustomerFilter.RETAIL -> c.type == CustomerType.RETAIL
                CustomerFilter.DEBTOR -> (balances[c.id] ?: 0L) > 0L
                CustomerFilter.BLOCKED -> c.blocked
                CustomerFilter.WHOLESALE_REQUEST -> c.wholesaleStatus == WholesaleStatus.PENDING
            }
            val matchesQuery = q.isEmpty() ||
                TextNorm.normalize(c.fullName).contains(q) ||
                (digits.length >= 3 && c.phone.contains(digits)) ||
                c.groups.any { TextNorm.normalize(it).contains(q) }
            matchesFilter && matchesQuery
        }
    }
}

data class DashboardStats(
    val productCount: Int,
    val totalUnits: Int,
    val stockValueAtCost: Long,
    val lowStockVariantCount: Int,
    val customerCount: Int,
    val wholesaleCount: Int,
    val pendingWholesaleRequests: Int,
    val totalReceivable: Long,
    val debtorCount: Int
)

object DashboardCalculator {
    fun compute(products: List<Product>, customers: List<Customer>, balances: Map<String, Long>): DashboardStats {
        val customerBalances = customers.map { balances[it.id] ?: 0L }
        return DashboardStats(
            productCount = products.size,
            totalUnits = products.sumOf { it.totalStock },
            stockValueAtCost = products.sumOf { p -> p.variants.sumOf { v -> p.landedCost * v.stock } },
            lowStockVariantCount = products.sumOf { it.lowStockVariants.size },
            customerCount = customers.size,
            wholesaleCount = customers.count { it.type == CustomerType.WHOLESALE },
            pendingWholesaleRequests = customers.count { it.wholesaleStatus == WholesaleStatus.PENDING },
            totalReceivable = customerBalances.filter { it > 0 }.sum(),
            debtorCount = customerBalances.count { it > 0 }
        )
    }
}
