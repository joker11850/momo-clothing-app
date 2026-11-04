package com.hamid.clothing.core

/** Built-in demo data so the apps are usable before any real data exists. */
object SampleData {
    private const val DAY = 86_400_000L

    private fun v(color: String, vararg sizes: Pair<String, Int>) = sizes.map { Variant(color, it.first, it.second) }

    fun categories(): List<Category> = listOf(
        Category("cat-1", "بلوز و شومیز"),
        Category("cat-2", "مانتو و کت"),
        Category("cat-3", "شلوار و دامن"),
        Category("cat-4", "لباس راحتی"),
        Category("cat-5", "اکسسوری")
    )

    fun products(): List<Product> = listOf(
        Product(
            "p-1", "SH-101", "شومیز زنانه گلدوزی‌شده", "cat-1", 900_000L, 960_000L, 1_530_000L, 1_250_000L,
            v("کرم", "M" to 6, "L" to 2, "XL" to 5) + v("سرمه‌ای", "M" to 4, "L" to 7),
            description = "پارچه نخی، مدل وینتیج", barcode = "6260000000011"
        ),
        Product(
            "p-2", "SH-102", "شومیز یقه‌ربان‌دار", "cat-1", 800_000L, 850_000L, 1_390_000L, 1_100_000L,
            v("شیری", "M" to 8, "L" to 8), barcode = "6260000000028"
        ),
        Product(
            "p-3", "MA-201", "مانتو کتی کرپ", "cat-2", 1_400_000L, 1_480_000L, 2_350_000L, 1_900_000L,
            v("مشکی", "38" to 5, "40" to 5, "42" to 3) + v("کله‌غازی", "38" to 4, "40" to 6)
        ),
        Product(
            "p-4", "PA-301", "شلوار کتان راسته", "cat-3", 700_000L, 740_000L, 1_190_000L, 960_000L,
            v("بژ", "S" to 9, "M" to 9, "L" to 6)
        ),
        Product(
            "p-5", "PA-302", "دامن پلیسه بلند", "cat-3", 650_000L, 690_000L, 1_080_000L, 880_000L,
            v("خاکی", "فری" to 12)
        ),
        Product(
            "p-6", "HM-401", "ست خواب دو تکه طرح خرس", "cat-4", 750_000L, 800_000L, 1_280_000L, 1_050_000L,
            v("نخی", "M" to 7, "L" to 4)
        ),
        Product(
            "p-7", "HM-402", "پیراهن راحتی نخی", "cat-4", 500_000L, 530_000L, 890_000L, 700_000L,
            v("صورتی", "فری" to 15)
        ),
        Product(
            "p-8", "AC-501", "شال نخی ساده", "cat-5", 250_000L, 270_000L, 460_000L, 360_000L,
            v("کرم", "فری" to 20) + v("قهوه‌ای", "فری" to 14)
        )
    )

    fun customers(now: Long): List<Customer> = listOf(
        Customer("c-1", "مریم احمدی", "09120000001", "اصفهان", joinedAt = now - 90 * DAY),
        Customer(
            "c-2", "فروشگاه گل‌نار", "09120000002", "اصفهان، خیابان چهارباغ",
            CustomerType.WHOLESALE, WholesaleStatus.APPROVED, now - 200 * DAY,
            CreditPolicy.WITHIN_LIMIT, 20_000_000L, 30, groups = setOf("خوش‌حساب")
        ),
        Customer(
            "c-3", "پوشاک آرتا", "09120000003", "نجف‌آباد",
            CustomerType.WHOLESALE, WholesaleStatus.APPROVED, now - 150 * DAY,
            CreditPolicy.APPROVAL_REQUIRED, 0L, 45
        ),
        Customer("c-4", "سارا محمدی", "09120000004", "شاهین‌شهر", joinedAt = now - 40 * DAY),
        Customer(
            "c-5", "نگین رضایی", "09120000005", "کاشان",
            CustomerType.WHOLESALE, WholesaleStatus.PENDING, now - 3 * DAY
        ),
        Customer(
            "c-6", "بوتیک نیلا", "09120000006", "یزد",
            CustomerType.WHOLESALE, WholesaleStatus.APPROVED, now - 60 * DAY
        )
    )

    fun ledger(now: Long): List<LedgerEntry> = listOf(
        LedgerEntry("l-1", "c-2", EntryType.PURCHASE, 12_000_000L, now - 20 * DAY, note = "خرید عمده"),
        LedgerEntry("l-2", "c-2", EntryType.PAYMENT, 5_000_000L, now - 10 * DAY, note = "کارت‌به‌کارت"),
        LedgerEntry("l-3", "c-3", EntryType.PURCHASE, 8_500_000L, now - 35 * DAY, note = "خرید عمده")
    )

    /** Demo discounts; "SALAM10" is a demo coupon code. */
    fun discounts(): List<Discount> = listOf(
        Discount("d-1", "فروش ویژه شومیز", 15, DiscountScope.PRODUCTS, setOf("p-1")),
        Discount("d-2", "فروش ویژه ست خواب", 14, DiscountScope.PRODUCTS, setOf("p-6")),
        Discount("d-3", "فروش ویژه دامن", 10, DiscountScope.PRODUCTS, setOf("p-5")),
        Discount("d-4", "کد خوش‌آمد", 10, code = "SALAM10")
    )

    fun snapshot(now: Long): StoreSnapshot =
        StoreSnapshot(true, categories(), products(), customers(now), ledger(now))
}
