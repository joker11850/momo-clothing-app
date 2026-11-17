@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.admin.R
import kotlinx.coroutines.delay
import com.hamid.clothing.core.ChequeDirection
import com.hamid.clothing.core.CustomerFilter

sealed interface Screen {
    data object Dashboard : Screen
    data class Orders(val filter: OrderFilter = OrderFilter.ALL) : Screen
    data object OrderForm : Screen
    data class OrderDetail(val id: String) : Screen
    data class Products(val lowStockOnly: Boolean = false) : Screen
    data class ProductForm(val productId: String?) : Screen
    data class Customers(val filter: CustomerFilter = CustomerFilter.ALL) : Screen
    data class CustomerForm(val customerId: String?) : Screen
    data class CustomerDetail(val customerId: String) : Screen
    data object Accounting : Screen
    data class Cheques(val direction: ChequeDirection) : Screen
    data class ChequeForm(val direction: ChequeDirection, val partyId: String? = null) : Screen
    data object Suppliers : Screen
    data class SupplierForm(val supplierId: String?) : Screen
    data class SupplierDetail(val supplierId: String) : Screen
    data class PurchaseForm(val supplierId: String) : Screen
    data object Expenses : Screen
    data object BankAccounts : Screen
    data class BankForm(val accountId: String?) : Screen
    data object Reports : Screen
    data object Settings : Screen
    data object Publish : Screen
    data object Notifications : Screen
    data object Backup : Screen
}

/** Tiny back stack: tabs replace the whole stack, other screens are pushed. */
class Nav(private val stack: SnapshotStateList<Screen>) {
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun go(screen: Screen) {
        stack.add(screen)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.size - 1)
    }

    fun replace(screen: Screen) {
        if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
        stack.add(screen)
    }

    fun tab(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }
}

@Composable
fun AdminApp(store: AdminStore) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1100)
        showSplash = false
    }
    if (showSplash) {
        MomoSplash("مدیریت پوشاک MOMO", "مدیریت فروشگاه")
        return
    }
    val stack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }
    val nav = remember { Nav(stack) }
    BackHandler(enabled = nav.canGoBack) { nav.back() }

    val current = nav.current
    val showBar = current is Screen.Dashboard || current is Screen.Orders || current is Screen.Products ||
        current is Screen.Customers || current is Screen.Accounting

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (showBar) AdminBottomBar(nav, current) }
    ) { padding ->
        when (current) {
            Screen.Dashboard -> DashboardScreen(
                store = store,
                padding = padding,
                onOpenLowStock = { nav.tab(Screen.Products(true)) },
                onOpenWholesaleRequests = { nav.tab(Screen.Customers(CustomerFilter.WHOLESALE_REQUEST)) },
                onOpenOrders = { filter -> nav.tab(Screen.Orders(filter)) },
                onOpenCheques = { nav.go(Screen.Cheques(ChequeDirection.RECEIVED)) },
                onOpenReports = { nav.go(Screen.Reports) },
                onOpenSettings = { nav.go(Screen.Settings) },
                onOpenPublish = { nav.go(Screen.Publish) },
                onOpenNotifications = { nav.go(Screen.Notifications) },
                onOpenBackup = { nav.go(Screen.Backup) }
            )
            is Screen.Orders -> OrdersScreen(
                store = store,
                padding = padding,
                initialFilter = current.filter,
                onAdd = { nav.go(Screen.OrderForm) },
                onOpen = { id -> nav.go(Screen.OrderDetail(id)) }
            )
            Screen.OrderForm -> OrderFormScreen(
                store = store,
                padding = padding,
                onDone = { id -> nav.replace(Screen.OrderDetail(id)) },
                onBack = { nav.back() }
            )
            is Screen.OrderDetail -> OrderDetailScreen(store, padding, current.id) { nav.back() }
            is Screen.Products -> ProductsScreen(
                store = store,
                padding = padding,
                initialLowStock = current.lowStockOnly,
                onAdd = { nav.go(Screen.ProductForm(null)) },
                onEdit = { id -> nav.go(Screen.ProductForm(id)) },
                onPublish = { nav.go(Screen.Publish) }
            )
            is Screen.ProductForm -> ProductFormScreen(
                store = store,
                padding = padding,
                productId = current.productId,
                onDone = { nav.back() },
                onBack = { nav.back() }
            )
            is Screen.Customers -> CustomersScreen(
                store = store,
                padding = padding,
                initialFilter = current.filter,
                onAdd = { nav.go(Screen.CustomerForm(null)) },
                onOpen = { id -> nav.go(Screen.CustomerDetail(id)) }
            )
            is Screen.CustomerForm -> CustomerFormScreen(
                store = store,
                padding = padding,
                customerId = current.customerId,
                onDone = { id ->
                    if (current.customerId == null) nav.replace(Screen.CustomerDetail(id)) else nav.back()
                },
                onBack = { nav.back() }
            )
            is Screen.CustomerDetail -> CustomerDetailScreen(
                store = store,
                padding = padding,
                customerId = current.customerId,
                onEdit = { nav.go(Screen.CustomerForm(current.customerId)) },
                onBack = { nav.back() },
                onDeleted = { nav.back() }
            )
            Screen.Accounting -> AccountingHubScreen(store, padding) { target -> nav.go(target) }
            is Screen.Cheques -> ChequesScreen(
                store = store,
                padding = padding,
                initialDirection = current.direction,
                onAdd = { direction -> nav.go(Screen.ChequeForm(direction)) },
                onBack = { nav.back() }
            )
            is Screen.ChequeForm -> ChequeFormScreen(
                store = store,
                padding = padding,
                direction = current.direction,
                partyId = current.partyId,
                onDone = { nav.back() },
                onBack = { nav.back() }
            )
            Screen.Suppliers -> SuppliersScreen(
                store = store,
                padding = padding,
                onAdd = { nav.go(Screen.SupplierForm(null)) },
                onOpen = { id -> nav.go(Screen.SupplierDetail(id)) },
                onBack = { nav.back() }
            )
            is Screen.SupplierForm -> SupplierFormScreen(
                store = store,
                padding = padding,
                supplierId = current.supplierId,
                onDone = { id ->
                    if (current.supplierId == null) nav.replace(Screen.SupplierDetail(id)) else nav.back()
                },
                onBack = { nav.back() }
            )
            is Screen.SupplierDetail -> SupplierDetailScreen(
                store = store,
                padding = padding,
                supplierId = current.supplierId,
                onBack = { nav.back() },
                onEdit = { nav.go(Screen.SupplierForm(current.supplierId)) },
                onPurchase = { nav.go(Screen.PurchaseForm(current.supplierId)) },
                onCheque = { nav.go(Screen.ChequeForm(ChequeDirection.PAID, current.supplierId)) }
            )
            is Screen.PurchaseForm -> PurchaseFormScreen(
                store = store,
                padding = padding,
                supplierId = current.supplierId,
                onDone = { nav.back() },
                onBack = { nav.back() }
            )
            Screen.Expenses -> ExpensesScreen(store, padding) { nav.back() }
            Screen.BankAccounts -> BankAccountsScreen(
                store = store,
                padding = padding,
                onAdd = { nav.go(Screen.BankForm(null)) },
                onEdit = { id -> nav.go(Screen.BankForm(id)) },
                onBack = { nav.back() }
            )
            is Screen.BankForm -> BankFormScreen(
                store = store,
                padding = padding,
                accountId = current.accountId,
                onDone = { nav.back() },
                onBack = { nav.back() }
            )
            Screen.Reports -> ReportsScreen(store, padding) { nav.back() }
            Screen.Settings -> SettingsScreen(store, padding) { nav.back() }
            Screen.Publish -> PublishScreen(store, padding) { nav.back() }
            Screen.Notifications -> NotificationsScreen(store, padding) { nav.back() }
            Screen.Backup -> BackupScreen(store, padding) { nav.back() }
        }
    }

    IncomingOrderDialog(store) { id ->
        nav.go(Screen.OrderDetail(id))
    }

    val message = store.notice
    if (message != null) {
        AlertDialog(
            onDismissRequest = { store.dismissNotice() },
            title = { Text("توجه") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { store.dismissNotice() }) { Text("متوجه شدم") } }
        )
    }
}

@Composable
private fun AdminBottomBar(nav: Nav, current: Screen) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.Black,
        selectedTextColor = Gold,
        indicatorColor = Gold,
        unselectedIconColor = Muted,
        unselectedTextColor = Muted
    )
    NavigationBar(containerColor = SurfaceDark) {
        NavigationBarItem(
            selected = current is Screen.Dashboard,
            onClick = { nav.tab(Screen.Dashboard) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("داشبورد") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = current is Screen.Orders,
            onClick = { nav.tab(Screen.Orders()) },
            icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
            label = { Text("سفارش‌ها") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = current is Screen.Products,
            onClick = { nav.tab(Screen.Products()) },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
            label = { Text("کالاها") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = current is Screen.Customers,
            onClick = { nav.tab(Screen.Customers()) },
            icon = { Icon(Icons.Default.People, contentDescription = null) },
            label = { Text("مشتریان") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = current is Screen.Accounting,
            onClick = { nav.tab(Screen.Accounting) },
            icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
            label = { Text("حسابداری") },
            colors = itemColors
        )
    }
}
