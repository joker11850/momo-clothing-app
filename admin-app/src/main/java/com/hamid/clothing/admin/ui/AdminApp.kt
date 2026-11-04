@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.CustomerFilter

sealed interface Screen {
    data object Dashboard : Screen
    data object Products : Screen
    data class ProductForm(val productId: String?) : Screen
    data object Customers : Screen
    data class CustomerForm(val customerId: String?) : Screen
    data class CustomerDetail(val customerId: String) : Screen
}

private fun backTarget(screen: Screen): Screen = when (screen) {
    is Screen.ProductForm -> Screen.Products
    is Screen.CustomerForm -> if (screen.customerId != null) Screen.CustomerDetail(screen.customerId) else Screen.Customers
    is Screen.CustomerDetail -> Screen.Customers
    Screen.Products, Screen.Customers -> Screen.Dashboard
    Screen.Dashboard -> Screen.Dashboard
}

@Composable
fun AdminApp(store: AdminStore) {
    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var lowStockOnly by remember { mutableStateOf(false) }
    var customerFilter by remember { mutableStateOf(CustomerFilter.ALL) }

    BackHandler(enabled = screen != Screen.Dashboard) { screen = backTarget(screen) }

    val showBar = screen is Screen.Dashboard || screen is Screen.Products || screen is Screen.Customers

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = SurfaceDark) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Gold,
                        indicatorColor = Gold,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                    NavigationBarItem(
                        selected = screen is Screen.Dashboard,
                        onClick = { screen = Screen.Dashboard },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text("داشبورد") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = screen is Screen.Products,
                        onClick = { lowStockOnly = false; screen = Screen.Products },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        label = { Text("کالاها") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = screen is Screen.Customers,
                        onClick = { customerFilter = CustomerFilter.ALL; screen = Screen.Customers },
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("مشتریان") },
                        colors = itemColors
                    )
                }
            }
        }
    ) { padding: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val current = screen) {
                Screen.Dashboard -> DashboardScreen(
                    store = store,
                    padding = padding,
                    onOpenLowStock = { lowStockOnly = true; screen = Screen.Products },
                    onOpenWholesaleRequests = {
                        customerFilter = CustomerFilter.WHOLESALE_REQUEST
                        screen = Screen.Customers
                    }
                )
                Screen.Products -> ProductsScreen(
                    store = store,
                    padding = padding,
                    initialLowStock = lowStockOnly,
                    onAdd = { screen = Screen.ProductForm(null) },
                    onEdit = { id -> screen = Screen.ProductForm(id) }
                )
                is Screen.ProductForm -> ProductFormScreen(
                    store = store,
                    padding = padding,
                    productId = current.productId,
                    onDone = { screen = Screen.Products },
                    onBack = { screen = Screen.Products }
                )
                Screen.Customers -> CustomersScreen(
                    store = store,
                    padding = padding,
                    initialFilter = customerFilter,
                    onAdd = { screen = Screen.CustomerForm(null) },
                    onOpen = { id -> screen = Screen.CustomerDetail(id) }
                )
                is Screen.CustomerForm -> CustomerFormScreen(
                    store = store,
                    padding = padding,
                    customerId = current.customerId,
                    onDone = { id -> screen = Screen.CustomerDetail(id) },
                    onBack = { screen = backTarget(current) }
                )
                is Screen.CustomerDetail -> CustomerDetailScreen(
                    store = store,
                    padding = padding,
                    customerId = current.customerId,
                    onEdit = { screen = Screen.CustomerForm(current.customerId) },
                    onBack = { screen = Screen.Customers },
                    onDeleted = { screen = Screen.Customers }
                )
            }
        }
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
