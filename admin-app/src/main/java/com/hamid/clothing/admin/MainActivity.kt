package com.hamid.clothing.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101010)
private val SurfaceDark = Color(0xFF202020)
private val Gold = Color(0xFFD6B46A)
private val Muted = Color(0xFFAAAAAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AdminDashboard()
            }
        }
    }
}

@Composable
private fun AdminDashboard() {
    MaterialTheme(colorScheme = darkColorScheme(
        background = Background,
        surface = SurfaceDark,
        primary = Gold
    )) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Icon(Icons.Default.Dashboard, null) },
                        label = { Text("خانه") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Inventory2, null) },
                        label = { Text("کالاها") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.People, null) },
                        label = { Text("مشتریان") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("تنظیمات") }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("پنل مدیریت", color = Gold, fontSize = 13.sp)
                            Text(
                                "خوش آمدید",
                                color = Color.White,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceDark
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "اعلان‌ها",
                                tint = Gold,
                                modifier = Modifier.padding(13.dp)
                            )
                        }
                    }
                }

                item {
                    Surface(
                        color = Color(0xFF29251D),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("نمای کلی فروشگاه", color = Gold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "امروز چه خبر؟",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "آمارها در نسخه آزمایشی هستند",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("فروش امروز", "۱۲,۵۰۰,۰۰۰", "تومان", Icons.Default.TrendingUp, Modifier.weight(1f))
                        MetricCard("سفارش جدید", "۱۲", "سفارش", Icons.Default.ShoppingBag, Modifier.weight(1f))
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("موجودی کالا", "۳۸۵", "عدد", Icons.Default.Inventory2, Modifier.weight(1f))
                        MetricCard("بدهی مشتریان", "۲۸,۰۰۰,۰۰۰", "تومان", Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
                    }
                }

                item {
                    Text(
                        "دسترسی سریع",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction("ثبت محصول جدید", Icons.Default.AddBox)
                        QuickAction("مشاهده محصولات و انبار", Icons.Default.Inventory)
                        QuickAction("مدیریت مشتریان", Icons.Default.People)
                        QuickAction("سفارش‌ها و فروش", Icons.Default.ShoppingCart)
                        QuickAction("حسابداری و چک‌ها", Icons.Default.AccountBalance)
                        QuickAction("گزارش‌های فروش", Icons.Default.BarChart)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceDark,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = Gold)
            Spacer(Modifier.height(12.dp))
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(unit, color = Gold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun QuickAction(title: String, icon: ImageVector) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Gold)
            Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}
