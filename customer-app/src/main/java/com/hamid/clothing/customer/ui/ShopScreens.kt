@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.hamid.clothing.customer.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hamid.clothing.core.CatalogSearch
import com.hamid.clothing.core.PriceSource
import com.hamid.clothing.customer.CustomerStore

@Composable
fun ShopScreen(store: CustomerStore, nav: Nav, padding: PaddingValues, categoryId: String?, query: String) {
    var text by remember(query) { mutableStateOf(query) }
    var category by remember(categoryId) { mutableStateOf(categoryId) }
    var sort by remember { mutableStateOf(0) }

    val visible = remember(store.products, text, category, sort, store.customer, store.catalogDate) {
        val found = CatalogSearch.products(store.products, text, category, false)
        when (sort) {
            1 -> found.sortedBy { store.quoteFor(it).unitPrice }
            2 -> found.sortedByDescending { store.quoteFor(it).unitPrice }
            else -> found.asReversed()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("فروشگاه")
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("جست‌وجوی نام یا کد کالا") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterChip(selected = category == null, onClick = { category = null }, label = { Text("همه") }) }
            items(store.categories, key = { it.id }, contentType = { "category-chip" }) { c ->
                FilterChip(
                    selected = category == c.id,
                    onClick = { category = if (category == c.id) null else c.id },
                    label = { Text(c.name) }
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterChip(selected = sort == 0, onClick = { sort = 0 }, label = { Text("جدیدترین") }) }
            item { FilterChip(selected = sort == 1, onClick = { sort = 1 }, label = { Text("ارزان‌ترین") }) }
            item { FilterChip(selected = sort == 2, onClick = { sort = 2 }, label = { Text("گران‌ترین") }) }
        }
        if (visible.isEmpty()) EmptyState("کالایی با این شرایط پیدا نشد.")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visible, key = { it.id }, contentType = { "product-card" }) { p ->
                ProductCard(store, p, Modifier.fillMaxWidth()) { nav.go(Screen.ProductDetail(p.id)) }
            }
        }
    }
}

@Composable
fun FavoritesScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val favorites = store.products.filter { store.isFavorite(it.id) }
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("علاقه‌مندی‌ها", onBack = { nav.back() })
        if (favorites.isEmpty()) EmptyState("هنوز کالایی را به علاقه‌مندی‌ها اضافه نکرده‌اید.")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites, key = { it.id }, contentType = { "product-card" }) { p ->
                ProductCard(store, p, Modifier.fillMaxWidth()) { nav.go(Screen.ProductDetail(p.id)) }
            }
        }
    }
}

@Composable
fun ProductScreen(store: CustomerStore, nav: Nav, padding: PaddingValues, productId: String) {
    val product = store.product(productId)
    if (product == null) {
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenTitle("کالا", onBack = { nav.back() })
            EmptyState("کالا پیدا نشد.")
        }
        return
    }
    val context = LocalContext.current
    val colors = product.variants.map { it.color }.distinct()

    var color by remember(productId) {
        mutableStateOf(
            colors.firstOrNull { c -> product.variants.any { it.color == c && store.availableFor(product.id, c, it.size) > 0 } }
                ?: colors.firstOrNull() ?: ""
        )
    }
    val sizes = product.variants.filter { it.color == color }.map { it.size }
    var size by remember(productId, color) {
        mutableStateOf(sizes.firstOrNull { store.availableFor(product.id, color, it) > 0 } ?: "")
    }
    var quantity by remember(productId, color, size) { mutableStateOf(1) }

    val available = if (size.isEmpty()) 0 else store.availableFor(product.id, color, size)
    val quote = store.quoteFor(product)
    val favorite = store.isFavorite(product.id)
    val categoryName = store.categories.firstOrNull { it.id == product.categoryId }?.name ?: ""

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
            ProductGallery(store, product)
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.TopStart).padding(14.dp).size(42.dp).clickable { nav.back() }
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
            }
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(42.dp).clickable { store.toggleFavorite(product.id) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "علاقه‌مندی",
                        tint = Burgundy
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("کد کالا: ${product.code}" + if (categoryName.isNotEmpty()) " · $categoryName" else "", style = MaterialTheme.typography.bodySmall, color = Muted)
            }

            SoftCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (quote.source == PriceSource.WHOLESALE) {
                        Text("قیمت عمده‌ی شما", style = MaterialTheme.typography.labelMedium, color = Brown)
                    }
                    if (quote.discountPercent > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(formatToman(quote.basePrice), color = Muted, textDecoration = TextDecoration.LineThrough)
                            Surface(shape = RoundedCornerShape(10.dp), color = BadgeRose) {
                                Text(
                                    "${faInt(quote.discountPercent)}٪ تخفیف",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(formatToman(quote.unitPrice), style = MaterialTheme.typography.headlineSmall, color = Burgundy, fontWeight = FontWeight.ExtraBold)
                    if (product.wholesalePrice > 0 && store.customer?.canSeeWholesale != true) {
                        Text(
                            "قیمت عمده فقط برای مشتریان عمده‌ی تأییدشده نمایش داده می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                        PillButton("درخواست عضویت عمده", {
                            if (store.isLoggedIn) nav.go(Screen.Wholesale) else nav.go(Screen.Login(Screen.Wholesale))
                        })
                    }
                }
            }

            if (product.description.isNotBlank()) {
                Text(product.description, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }

            Text("رنگ", fontWeight = FontWeight.Bold)
            ChoiceChips(
                options = colors.map { c -> Triple(c, c, true) },
                selected = color,
                onSelect = { color = it }
            )

            Text("سایز", fontWeight = FontWeight.Bold)
            ChoiceChips(
                options = sizes.map { s ->
                    val ok = store.availableFor(product.id, color, s) > 0
                    Triple(s, if (ok) s else "$s (ناموجود)", ok)
                },
                selected = size,
                onSelect = { size = it }
            )

            Text(
                when {
                    available <= 0 -> "ناموجود"
                    available <= product.lowStockThreshold -> "تنها ${faInt(available)} عدد باقی مانده است"
                    else -> "${faInt(available)} عدد موجود است"
                },
                color = if (available <= 0) Danger else if (available <= product.lowStockThreshold) Brown else MintDark,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuantityStepper(
                    quantity = quantity.coerceAtMost(available.coerceAtLeast(1)),
                    onChange = { quantity = it.coerceAtLeast(1) },
                    max = available.coerceAtLeast(1)
                )
                GoldButton(
                    "افزودن به سبد خرید",
                    onClick = {
                        val error = store.addToCart(product, color, size, quantity.coerceAtMost(available.coerceAtLeast(1)))
                        toast(context, error ?: "به سبد خرید اضافه شد")
                    },
                    enabled = available > 0,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProductGallery(store: CustomerStore, product: com.hamid.clothing.core.Product) {
    val files = store.imageFiles(product)
    if (files.size <= 1) {
        ProductArt(store, product, Modifier.fillMaxSize(), maxPx = 900)
        return
    }
    val pager = rememberPagerState(pageCount = { files.size })
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            LocalImage(files[page], Modifier.fillMaxSize(), maxPx = 900)
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in files.indices) {
                Box(
                    modifier = Modifier
                        .size(if (pager.currentPage == i) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (pager.currentPage == i) Gold else Color.White.copy(alpha = 0.7f))
                )
            }
        }
    }
}
