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

    fun suppliers(): List<Supplier> = listOf(
        Supplier("s-1", "تولیدی نگین", "09130000001", "تولیدکننده شومیز و مانتو"),
        Supplier("s-2", "پخش مروارید", "09130000002", "اکسسوری و لباس راحتی")
    )

    fun supplierLedger(now: Long): List<LedgerEntry> = listOf(
        LedgerEntry("l-s1", "s-1", EntryType.PURCHASE, 30_000_000L, now - 25 * DAY, note = "خرید کالا"),
        LedgerEntry("l-s2", "s-1", EntryType.PAYMENT, 10_000_000L, now - 12 * DAY, note = "پرداخت نقدی"),
        LedgerEntry("l-s3", "s-2", EntryType.PURCHASE, 6_000_000L, now - 8 * DAY, note = "خرید کالا")
    )

    fun cheques(now: Long): List<Cheque> = listOf(
        Cheque("ch-1", ChequeDirection.RECEIVED, "c-2", 5_000_000L, "1234567890123456", now + 5 * DAY, "ملی"),
        Cheque("ch-2", ChequeDirection.PAID, "s-1", 10_000_000L, "6543210987654321", now + 20 * DAY, "ملت")
    )

    fun expenses(now: Long): List<Expense> = listOf(
        Expense("e-1", ExpenseCategory.RENT, 12_000_000L, now - 10 * DAY, "اجاره فروشگاه"),
        Expense("e-2", ExpenseCategory.PACKAGING, 800_000L, now - 6 * DAY, "کارتن و پلاستیک"),
        Expense("e-3", ExpenseCategory.PERSONAL, 3_000_000L, now - 4 * DAY, "هزینه شخصی")
    )

    fun bankAccounts(): List<BankAccount> = listOf(
        BankAccount(
            "b-1", "بانک نمونه", "صاحب حساب (نمونه)", "6037991899999993", "IR050170000000123456789012",
            active = true, visibleToCustomers = true
        )
    )

    fun orders(now: Long): List<Order> = listOf(
        Order(
            id = "o-1", number = "ORD-1001", customerId = "c-1", channel = Channel.RETAIL,
            items = listOf(OrderItem("p-4", "شلوار کتان راسته", "بژ", "S", 1, 1_190_000L, 740_000L)),
            paymentMethod = PaymentMethod.CARD_TO_CARD, shippingMethod = ShippingMethod.COURIER,
            createdAt = now - 3 * DAY, status = OrderStatus.DELIVERED, paidAmount = 1_190_000L,
            trackingCode = "TRK-1001", stockCommitted = true,
            history = listOf(
                StatusChange(OrderStatus.NEW, OrderStatus.AWAITING_PAYMENT, now - 3 * DAY),
                StatusChange(OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID, now - 3 * DAY + 3_600_000L),
                StatusChange(OrderStatus.PAID, OrderStatus.PREPARING, now - 2 * DAY),
                StatusChange(OrderStatus.PREPARING, OrderStatus.SHIPPED, now - 2 * DAY + 3_600_000L),
                StatusChange(OrderStatus.SHIPPED, OrderStatus.DELIVERED, now - 1 * DAY)
            ),
            receiverName = "مریم احمدی", receiverPhone = "09120000001", address = "اصفهان"
        ),
        Order(
            id = "o-2", number = "ORD-1002", customerId = "c-3", channel = Channel.WHOLESALE,
            items = listOf(OrderItem("p-2", "شومیز یقه‌ربان‌دار", "شیری", "M", 4, 1_100_000L, 850_000L)),
            paymentMethod = PaymentMethod.ON_ACCOUNT, shippingMethod = ShippingMethod.POST,
            createdAt = now - 1 * DAY, status = OrderStatus.AWAITING_SELLER_APPROVAL,
            history = listOf(StatusChange(OrderStatus.NEW, OrderStatus.AWAITING_SELLER_APPROVAL, now - 1 * DAY)),
            receiverName = "پوشاک آرتا", receiverPhone = "09120000003", address = "نجف‌آباد"
        ),
        Order(
            id = "o-3", number = "ORD-1003", customerId = "c-4", channel = Channel.RETAIL,
            items = listOf(OrderItem("p-5", "دامن پلیسه بلند", "خاکی", "فری", 1, 972_000L, 690_000L, 10)),
            paymentMethod = PaymentMethod.CARD_TO_CARD, shippingMethod = ShippingMethod.COURIER,
            createdAt = now - 2 * 3_600_000L, status = OrderStatus.AWAITING_PAYMENT,
            history = listOf(StatusChange(OrderStatus.NEW, OrderStatus.AWAITING_PAYMENT, now - 2 * 3_600_000L)),
            receiverName = "سارا محمدی", receiverPhone = "09120000004", address = "شاهین‌شهر"
        )
    )

    fun snapshot(now: Long): StoreSnapshot = StoreSnapshot(
        sample = true,
        categories = categories(),
        products = products(),
        customers = customers(now),
        ledger = ledger(now) + supplierLedger(now),
        orders = orders(now),
        suppliers = suppliers(),
        cheques = cheques(now),
        expenses = expenses(now),
        bankAccounts = bankAccounts(),
        settings = StoreSettings(shopName = "فروشگاه پوشاک", phone = "", address = "")
    )
}
