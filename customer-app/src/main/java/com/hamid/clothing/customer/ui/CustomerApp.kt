@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.customer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.hamid.clothing.customer.CustomerStore
import com.hamid.clothing.customer.R
import kotlinx.coroutines.delay

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
    data object Notifications : Screen
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
fun CustomerApp(store: CustomerStore, onToggleDarkMode: () -> Unit = {}) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(4000)
        showSplash = false
    }
    if (showSplash) {
        MomoSplash()
        return
    }
    val startScreen = remember {
        when (context.getSharedPreferences("customer_preferences", android.content.Context.MODE_PRIVATE).getString("last_screen", "home")) {
            "shop" -> Screen.Shop()
            "cart" -> Screen.Cart
            "account" -> Screen.Account
            "contact" -> Screen.Contact
            else -> Screen.Home
        }
    }
    val stack = remember { mutableStateListOf<Screen>(startScreen) }
    val nav = remember { Nav(stack) }
    androidx.compose.runtime.LaunchedEffect(nav.current) {
        val key = when (nav.current) {
            is Screen.Shop -> "shop"
            Screen.Cart -> "cart"
            Screen.Account -> "account"
            Screen.Contact -> "contact"
            else -> "home"
        }
        context.getSharedPreferences("customer_preferences", android.content.Context.MODE_PRIVATE).edit().putString("last_screen", key).apply()
    }
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
            Screen.Account -> AccountScreen(store, nav, padding, onToggleDarkMode)
            is Screen.Login -> LoginScreen(store, nav, padding, current.next)
            Screen.Profile -> ProfileScreen(store, nav, padding)
            Screen.Orders -> OrdersScreen(store, nav, padding)
            is Screen.OrderDetail -> OrderDetailScreen(store, nav, padding, current.id)
            Screen.Wholesale -> WholesaleScreen(store, nav, padding)
            Screen.Favorites -> FavoritesScreen(store, nav, padding)
            Screen.BankInfo -> BankInfoScreen(store, nav, padding)
            Screen.Contact -> ContactScreen(store, nav, padding)
            Screen.Notifications -> NotificationInboxScreen(nav, padding)
        }
    }

    if (store.importing) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("دریافت کاتالوگ") },
            text = { Text("در حال دریافت کالاها و عکس‌ها…") },
            confirmButton = {}
        )
    }
    val message = store.notice
    if (message != null) InfoDialog("توجه", message) { store.dismissNotice() }
}

@Composable
private fun BottomBar(store: CustomerStore, nav: Nav, current: Screen) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.White,
        selectedTextColor = Burgundy,
        indicatorColor = Color.Transparent,
        unselectedIconColor = Muted,
        unselectedTextColor = Muted
    )
    val homeColor = Color(0xFFFF4652)
    val shopColor = Color(0xFF7B3FF2)
    val cartColor = Color(0xFFFF8A3D)
    val accountColor = Color(0xFF3B82F6)
    val contactColor = Color(0xFF00A896)
    NavigationBar(containerColor = CardColor) {
        NavigationBarItem(
            selected = current is Screen.Home,
            onClick = { nav.tab(Screen.Home) },
            icon = { ColorfulNavIcon(Icons.Default.Home, current is Screen.Home, homeColor, "خانه") },
            label = { Text("خانه") },
            colors = colors
        )
        NavigationBarItem(
            selected = current is Screen.Shop,
            onClick = { nav.tab(Screen.Shop()) },
            icon = { ColorfulNavIcon(Icons.Default.ShoppingBag, current is Screen.Shop, shopColor, "فروشگاه") },
            label = { Text("فروشگاه") },
            colors = colors
        )
        NavigationBarItem(
            selected = current is Screen.Cart,
            onClick = { nav.tab(Screen.Cart) },
            icon = {
                ColorfulNavIcon(
                    icon = Icons.Default.ShoppingCart,
                    selected = current is Screen.Cart,
                    color = cartColor,
                    description = "سبد خرید",
                    badge = store.cartCount.takeIf { it > 0 }
                )
            },
            label = { Text("سبد خرید") },
            colors = colors
        )
        NavigationBarItem(
            selected = current is Screen.Account,
            onClick = { nav.tab(Screen.Account) },
            icon = { ColorfulNavIcon(Icons.Default.Person, current is Screen.Account, accountColor, "حساب من") },
            label = { Text("حساب من") },
            colors = colors
        )
        NavigationBarItem(
            selected = current is Screen.Contact,
            onClick = { nav.tab(Screen.Contact) },
            icon = { ColorfulNavIcon(Icons.Default.Phone, current is Screen.Contact, contactColor, "تماس") },
            label = { Text("تماس") },
            colors = colors
        )
    }
}

@Composable
private fun ColorfulNavIcon(
    icon: ImageVector,
    selected: Boolean,
    color: Color,
    description: String,
    badge: Int? = null
) {
    Surface(
        shape = CircleShape,
        color = if (selected) color else color.copy(alpha = 0.13f),
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            BadgedBox(badge = { if (badge != null) Badge { Text(faInt(badge)) } }) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = if (selected) Color.White else color,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
