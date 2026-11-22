@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.customer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hamid.clothing.customer.CustomerStore
import com.hamid.clothing.customer.R

sealed interface Screen {
    data object Home : Screen
    data class Shop(val categoryId: String? = null, val query: String = "") : Screen
    data class ProductDetail(val id: String) : Screen
    data object Cart : Screen
    data object Checkout : Screen
    data class OrderDone(val orderId: String) : Screen
    data object Account : Screen
    data object Profile : Screen
    data object Orders : Screen
    data class OrderDetail(val id: String) : Screen
    data object Wholesale : Screen
    data object Favorites : Screen
    data object BankInfo : Screen
    data object Contact : Screen
    data object Notifications : Screen
    data object BrandStory : Screen
    data object Privacy : Screen
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
    var splashVisible by remember { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        splashVisible = false
    }
    if (splashVisible) {
        Image(
            painter = painterResource(com.hamid.clothing.customer.R.drawable.momo_splash),
            contentDescription = "اسپلش پوشاک MOMO",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        return
    }
    val context = LocalContext.current
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
            Screen.Profile -> ProfileScreen(store, nav, padding)
            Screen.Orders -> OrdersScreen(store, nav, padding)
            is Screen.OrderDetail -> OrderDetailScreen(store, nav, padding, current.id)
            Screen.Wholesale -> WholesaleScreen(store, nav, padding)
            Screen.Favorites -> FavoritesScreen(store, nav, padding)
            Screen.BankInfo -> BankInfoScreen(store, nav, padding)
            Screen.Contact -> ContactScreen(store, nav, padding)
            Screen.Notifications -> NotificationInboxScreen(nav, padding)
            Screen.BrandStory -> BrandStoryScreen(nav, padding)
            Screen.Privacy -> PrivacyScreen(nav, padding)
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

/**
 * Bottom navigation built from the finished artwork (res/drawable-nodpi/nav_bar_momo.webp).
 * The artwork has no badge and no active-indicator baked in: both are drawn here.
 * Zones are laid out left-to-right in the artwork order: contact, account, cart, shop, home.
 */
private data class NavZone(val weight: Float, val label: String, val selected: Boolean, val cart: Boolean, val onClick: () -> Unit)

@Composable
private fun BottomBar(store: CustomerStore, nav: Nav, current: Screen) {
    val zones = listOf(
        NavZone(0.222f, "تماس با ما", current is Screen.Contact, false) { nav.tab(Screen.Contact) },
        NavZone(0.182f, "حساب من", current is Screen.Account, false) { nav.tab(Screen.Account) },
        NavZone(0.196f, "سبد خرید", current is Screen.Cart, true) { nav.tab(Screen.Cart) },
        NavZone(0.185f, "فروشگاه", current is Screen.Shop, false) { nav.tab(Screen.Shop()) },
        NavZone(0.215f, "خانه", current is Screen.Home, false) { nav.tab(Screen.Home) }
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .aspectRatio(6.0870f)
        ) {
            val w = maxWidth
            val h = maxHeight
            Image(
                painter = painterResource(R.drawable.nav_bar_momo),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )
            Row(modifier = Modifier.matchParentSize()) {
                zones.forEach { z ->
                    Box(
                        modifier = Modifier
                            .weight(z.weight)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClickLabel = z.label,
                                onClick = z.onClick
                            )
                    ) {
                        if (z.selected && !z.cart) {
                            // thin burgundy line under the selected label
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = h * 0.87f)
                                    .fillMaxWidth(0.4f)
                                    .height(h * 0.045f)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF7A1530))
                            )
                        }
                        if (z.selected && z.cart) {
                            // light ring around the round cart button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = h * 0.367f - w * 0.0765f)
                                    .size(w * 0.153f)
                                    .border(2.dp, Color(0xFFFFE9A8), CircleShape)
                            )
                        }
                        if (z.cart && store.cartCount > 0) {
                            Badge(
                                containerColor = Color(0xFFE53935),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(x = w * 0.047f, y = h * 0.081f - 8.dp)
                            ) { Text(faInt(store.cartCount)) }
                        }
                    }
                }
            }
        }
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
