package com.hamid.clothing.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogTest {
    private val now = Fixtures.NOW

    private fun validProduct(id: String = "n1", code: String = "NEW-1") = Product(
        id, code, "محصول جدید", "cat-1", 100_000L, 110_000L, 200_000L, 150_000L,
        listOf(Variant("قرمز", "M", 5))
    )

    @Test fun sampleDataIsValid() {
        val products = SampleData.products()
        for (p in products) assertEquals(emptyList(), ProductValidator.validate(p, products), p.code)
        val customers = SampleData.customers(now)
        for (c in customers) assertEquals(emptyList(), CustomerValidator.validate(c, customers), c.fullName)
        val categoryIds = SampleData.categories().map { it.id }.toSet()
        assertTrue(products.all { it.categoryId in categoryIds })
        val customerIds = customers.map { it.id }.toSet()
        assertTrue(SampleData.ledger(now).all { it.partyId in customerIds })
    }

    @Test fun validProductPasses() {
        assertEquals(emptyList(), ProductValidator.validate(validProduct(), SampleData.products()))
    }

    @Test fun productRulesReportErrors() {
        val existing = SampleData.products()
        val bad = validProduct().copy(
            name = " ", code = "sh-101", categoryId = "", purchasePrice = 0L, landedCost = 0L,
            retailPrice = 0L, variants = emptyList()
        )
        val errors = ProductValidator.validate(bad, existing)
        assertTrue("نام محصول را وارد کنید" in errors)
        assertTrue("کد کالا تکراری است" in errors) // case-insensitive duplicate of SH-101
        assertTrue("دسته‌بندی را انتخاب کنید" in errors)
        assertTrue("قیمت خرید باید بیشتر از صفر باشد" in errors)
        assertTrue("قیمت فروش خرده باید بیشتر از صفر باشد" in errors)
        assertTrue("حداقل یک ترکیب رنگ و سایز لازم است" in errors)
    }

    @Test fun priceRules() {
        val errors = ProductValidator.validate(
            validProduct().copy(purchasePrice = 120_000L, landedCost = 110_000L, wholesalePrice = 250_000L), emptyList()
        )
        assertTrue("قیمت تمام‌شده نمی‌تواند کمتر از قیمت خرید باشد" in errors)
        assertTrue("قیمت عمده نباید بیشتر از قیمت خرده باشد" in errors)
    }

    @Test fun variantAndBarcodeRules() {
        val dup = validProduct().copy(variants = listOf(Variant("قرمز", "M", 1), Variant(" قرمز", "m", 2)))
        assertTrue("ترکیب رنگ و سایز تکراری است" in ProductValidator.validate(dup, emptyList()))
        val blank = validProduct().copy(variants = listOf(Variant("", "M", 1)))
        assertTrue("رنگ و سایز نباید خالی باشد" in ProductValidator.validate(blank, emptyList()))
        val sameBarcode = validProduct().copy(barcode = "6260000000011")
        assertTrue("بارکد تکراری است" in ProductValidator.validate(sameBarcode, SampleData.products()))
    }

    @Test fun editingSameProductIsNotADuplicate() {
        val products = SampleData.products()
        val edited = products[0].copy(name = "نام جدید")
        assertEquals(emptyList(), ProductValidator.validate(edited, products))
    }

    @Test fun customerRules() {
        val customers = SampleData.customers(now)
        val dupPhone = Customer("new", "کسی", "09120000002")
        assertTrue("این شماره قبلاً ثبت شده است" in CustomerValidator.validate(dupPhone, customers))
        val badPhone = Customer("new", "کسی", "12345")
        assertTrue("شماره موبایل معتبر نیست" in CustomerValidator.validate(badPhone, customers))
        val noName = Customer("new", " ", "09129998888")
        assertTrue("نام مشتری را وارد کنید" in CustomerValidator.validate(noName, customers))
        val retailCredit = Customer("new", "کسی", "09129998888", creditPolicy = CreditPolicy.APPROVAL_REQUIRED)
        assertTrue("خرید دفتری فقط برای مشتری عمده مجاز است" in CustomerValidator.validate(retailCredit, customers))
        val noLimit = Customer(
            "new", "کسی", "09129998888", type = CustomerType.WHOLESALE,
            wholesaleStatus = WholesaleStatus.APPROVED, creditPolicy = CreditPolicy.WITHIN_LIMIT
        )
        assertTrue("برای خرید دفتری تا سقف اعتبار، سقف را وارد کنید" in CustomerValidator.validate(noLimit, customers))
    }

    @Test fun productSearch() {
        val all = SampleData.products()
        assertEquals(listOf("p-1"), CatalogSearch.products(all, "گلدوزی", null, false).map { it.id })
        assertEquals(listOf("p-1"), CatalogSearch.products(all, "sh-101", null, false).map { it.id })
        assertEquals(listOf("p-2"), CatalogSearch.products(all, "۶۲۶۰۰۰۰۰۰۰۰۲۸", null, false).map { it.id })
        assertEquals(listOf("p-4", "p-5"), CatalogSearch.products(all, "", "cat-3", false).map { it.id })
        assertEquals(all.size, CatalogSearch.products(all, "  ", null, false).size)
    }

    @Test fun arabicLettersMatchPersian() {
        val all = SampleData.products()
        // "ك" and "ي" (Arabic) must find "ک" and "ی"
        assertEquals(listOf("p-6"), CatalogSearch.products(all, "ست خواب", null, false).map { it.id })
        assertEquals(listOf("p-4"), CatalogSearch.products(all, "شلوار كتان", null, false).map { it.id })
    }

    @Test fun lowStockFilter() {
        val all = listOf(Fixtures.product("a", stock = 10), Fixtures.product("b", stock = 2))
        assertEquals(listOf("b"), CatalogSearch.products(all, "", null, true).map { it.id })
        assertEquals(2, all[1].lowStockVariants.size)
        assertEquals(0, all[0].lowStockVariants.size)
        assertEquals(2, CatalogSearch.products(all, "", null, false).size)
    }

    @Test fun customerSearchAndFilters() {
        val customers = SampleData.customers(now)
        val balances = LedgerMath.balances(SampleData.ledger(now))
        assertEquals(listOf("c-2"), CatalogSearch.customers(customers, "گل‌نار", CustomerFilter.ALL, balances).map { it.id })
        assertEquals(listOf("c-4"), CatalogSearch.customers(customers, "0912000000۴", CustomerFilter.ALL, balances).map { it.id })
        assertEquals(listOf("c-2", "c-3", "c-5", "c-6"), CatalogSearch.customers(customers, "", CustomerFilter.WHOLESALE, balances).map { it.id })
        assertEquals(listOf("c-1", "c-4"), CatalogSearch.customers(customers, "", CustomerFilter.RETAIL, balances).map { it.id })
        assertEquals(listOf("c-2", "c-3"), CatalogSearch.customers(customers, "", CustomerFilter.DEBTOR, balances).map { it.id })
        assertEquals(listOf("c-5"), CatalogSearch.customers(customers, "", CustomerFilter.WHOLESALE_REQUEST, balances).map { it.id })
        assertEquals(emptyList(), CatalogSearch.customers(customers, "", CustomerFilter.BLOCKED, balances).map { it.id })
        assertEquals(listOf("c-2"), CatalogSearch.customers(customers, "خوش‌حساب", CustomerFilter.ALL, balances).map { it.id })
    }

    @Test fun ledgerBalancesPerParty() {
        val balances = LedgerMath.balances(SampleData.ledger(now))
        assertEquals(7_000_000L, balances["c-2"])
        assertEquals(8_500_000L, balances["c-3"])
        assertEquals(null, balances["c-1"])
    }

    @Test fun dashboardNumbers() {
        val products = listOf(
            Fixtures.product("a", cost = 100_000L, stock = 4), // 2 variants x 4 = 8 units, low (<=3)? no
            Fixtures.product("b", cost = 200_000L, stock = 2)  // 2 variants x 2 = 4 units, both low
        )
        val customers = SampleData.customers(now)
        val stats = DashboardCalculator.compute(products, customers, LedgerMath.balances(SampleData.ledger(now)))
        assertEquals(2, stats.productCount)
        assertEquals(12, stats.totalUnits)
        assertEquals(8 * 100_000L + 4 * 200_000L, stats.stockValueAtCost)
        assertEquals(2, stats.lowStockVariantCount)
        assertEquals(6, stats.customerCount)
        assertEquals(4, stats.wholesaleCount)
        assertEquals(1, stats.pendingWholesaleRequests)
        assertEquals(15_500_000L, stats.totalReceivable)
        assertEquals(2, stats.debtorCount)
    }
}
