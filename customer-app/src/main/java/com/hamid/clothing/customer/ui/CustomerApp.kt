@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.customer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.hamid.clothing.customer.CustomerStore

sealed interface Screen {
    data object Home : Screen
    data class Shop(val categoryId: String? = null, val query: String = "") : Screen
    data class ProductDetail(val id: String) : Screen
    data object Cart : Screen
    data object Checkout : Screen
    data class OrderDone(val orderId: String) : Screen
    data object Account : Screen
    data class Login(val next: Screen? = null) : Screen
    data object Profile : Screen
    data object Orders : Screen
    data class OrderDetail(val id: String) : Screen
    data object Wholesale : Screen
    data object Favorites : Screen
    data object BankInfo : Screen
    data object Contact : Screen
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
fun CustomerApp(store: CustomerStore) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val nav = remember { Nav(stack) }
    BackHandler(enabled = nav.canGoBack) { nav.back() }

    val current = nav.current
    val topLevel = current is Screen.Home || current is Screen.Shop || current is Screen.Cart ||
        current is Screen.Account || current is Screen.Contact

    Scaffold(
        containerColor = Cream,
        bottomBar = { if (topLevel) BottomBar(store, nav, current) }
    ) { padding ->
        when (current) {
            Screen.Home -> HomeScreen(store, nav, padding)
            is Screen.Shop -> ShopScreen(store, nav, padding, current.categoryId, current.query)
            is Screen.ProductDetail -> ProductScreen(store, nav, padding, current.id)
            Screen.Cart -> CartScreen(store, nav, padding)
            Screen.Checkout -> CheckoutScreen(store, nav, padding)
            is Screen.OrderDone -> OrderDoneScreen(store, nav, padding, current.orderId)
            Screen.Account -> AccountScreen(store, nav, padding)
            is Screen.Login -> LoginScreen(store, nav, padding, current.next)
            Screen.Profile -> ProfileScreen(store, nav, padding)
            Screen.Orders -> OrdersScreen(store, nav, padding)
            is Screen.OrderDetail -> OrderDetailScreen(store, nav, padding, current.id)
            Screen.Wholesale -> WholesaleScreen(store, nav, padding)
            Screen.Favorites -> FavoritesScreen(store, nav, padding)
            Screen.BankInfo -> BankInfoScreen(nav, padding)
            Screen.Contact -> ContactScreen(nav, padding)
        }
    }

    val message = store.notice
    if (message != null) InfoDialog("توجه", message) { store.dismissNotice() }
}

@Composable
private fun BottomBar(store: CustomerStore, nav: Nav, current: Screen) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Burgundy,
        selectedTextColor = Burgundy,
        indicatorColor = RoseLight,
        unselectedIconColor = Muted,
        unselectedTextColor = Muted
    )
    NavigationBar(containerColor = CardColor) {
        NavigationBarItem(
            selected = current is Screen.Home,
            onClick = { nav.tab(Screen.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("خانه") },
            colors = colors
        )
        NavigationBarItem(
            selected = current is Screen.Shop,
            onClick = { nav.tab(Screen.Shop()) },
            icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
            label = { Text("فروشگاه") },
            colors = colors
        )
        NavigationBarItem(
            selected = current is Screen.Cart,
            onClick = { nav.tab(Screen.Cart) },
            icon = {
                BadgedBox(badge = { if (store.cartCount > 0) Badge { Text(faInt(store.cartCount)) } }) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                }
            },
            label = { Text("سبد خرید") },
            colors = colors
        )
        NavigationBarItem(
            selected = current is Screen.Account,
            onClick = { nav.tab(Screen.Account) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("حساب من") },
            colors = colors
        )
        NavigationBarItem(
            selected = current is Screen.Contact,
            onClick = { nav.tab(Screen.Contact) },
            icon = { Icon(Icons.Default.Phone, contentDescription = null) },
            label = { Text("تماس") },
            colors = colors
        )
    }
}
