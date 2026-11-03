package com.hamid.clothing.customer

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cream = Color(0xFFF7F5F0)
private val Ink = Color(0xFF242321)
private val Accent = Color(0xFF9B7952)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CustomerHome()
            }
        }
    }
}

@Composable
private fun CustomerHome() {
    MaterialTheme(colorScheme = lightColorScheme(
        background = Cream,
        surface = Color.White,
        primary = Accent
    )) {
        Scaffold(
            containerColor = Cream,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("خانه") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Category, null) },
                        label = { Text("دسته‌بندی") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.ShoppingCart, null) },
                        label = { Text("سبد خرید") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("حساب من") }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Cream)
                    .padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("فروشگاه پوشاک", color = Accent, fontSize = 13.sp)
                            Text(
                                "انتخابی برای استایل تو",
                                color = Ink,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.Search, null, tint = Ink)
                    }
                }

                item {
                    Surface(
                        color = Color(0xFFE9E0D3),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            Text("مجموعه جدید", color = Accent)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "استایل تازه،\nانتخاب تازه",
                                color = Ink,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("محصولات جدید فروشگاه را ببینید", color = Ink, fontSize = 13.sp)
                        }
                    }
                }

                item {
                    Text(
                        "دسته‌بندی محصولات",
                        color = Ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("مردانه", "زنانه", "بچگانه", "جدیدترین‌ها").forEach {
                            Surface(color = Color.White, shape = RoundedCornerShape(50)) {
                                Text(it, modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp), color = Ink, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Text(
                        "محصولات منتخب",
                        color = Ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item { ProductCard("پیراهن روزمره", "۴۵۰,۰۰۰ تومان", "موجود در چند رنگ") }
                item { ProductCard("شلوار کتان", "۶۸۰,۰۰۰ تومان", "سایزبندی کامل") }
                item { ProductCard("تیشرت ساده", "۳۲۰,۰۰۰ تومان", "مجموعه جدید") }
            }
        }
    }
}

@Composable
private fun ProductCard(name: String, price: String, detail: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFFEDE8E0),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(
                    Icons.Default.Checkroom,
                    null,
                    tint = Accent,
                    modifier = Modifier.size(66.dp).padding(17.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = Ink, fontWeight = FontWeight.Bold)
                Text(detail, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text(price, color = Accent, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.AddShoppingCart, null, tint = Accent)
        }
    }
}
