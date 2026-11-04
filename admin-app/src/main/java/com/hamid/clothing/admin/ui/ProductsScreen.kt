@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.CatalogSearch
import com.hamid.clothing.core.Product

@Composable
fun ProductsScreen(
    store: AdminStore,
    padding: PaddingValues,
    initialLowStock: Boolean,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var lowOnly by remember(initialLowStock) { mutableStateOf(initialLowStock) }
    val visible = CatalogSearch.products(store.products, query, categoryId, lowOnly)

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader("کالاها")
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("جست‌وجوی نام، کد یا بارکد") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = categoryId == null && !lowOnly,
                        onClick = { categoryId = null; lowOnly = false },
                        label = { Text("همه") }
                    )
                }
                item {
                    FilterChip(
                        selected = lowOnly,
                        onClick = { lowOnly = !lowOnly },
                        label = { Text("کمبود موجودی") }
                    )
                }
                items(store.categories, key = { it.id }) { c ->
                    FilterChip(
                        selected = categoryId == c.id,
                        onClick = { categoryId = if (categoryId == c.id) null else c.id },
                        label = { Text(c.name) }
                    )
                }
            }
            if (visible.isEmpty()) {
                EmptyState(if (store.products.isEmpty()) "هنوز کالایی ثبت نشده است. با دکمه‌ی «کالای جدید» شروع کنید." else "کالایی با این شرایط پیدا نشد.")
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { p ->
                    ProductRow(store, p) { onEdit(p.id) }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("کالای جدید") },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }
}

@Composable
private fun ProductRow(store: AdminStore, p: Product, onClick: () -> Unit) {
    val category = store.categories.firstOrNull { it.id == p.categoryId }?.name ?: "بدون دسته"
    val low = p.lowStockVariants
    AdminCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (low.isNotEmpty()) {
                Icon(Icons.Default.Warning, contentDescription = "کمبود موجودی", tint = Danger)
            }
        }
        Text("${p.code} · $category", style = MaterialTheme.typography.bodySmall, color = Muted)
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("خرده: ${formatToman(p.retailPrice)}", style = MaterialTheme.typography.bodySmall)
            if (p.wholesalePrice > 0) {
                Text("عمده: ${formatToman(p.wholesalePrice)}", style = MaterialTheme.typography.bodySmall, color = Gold)
            }
        }
        Text(
            "موجودی: ${faInt(p.totalStock)} عدد در ${faInt(p.variants.size)} رنگ/سایز" +
                if (low.isNotEmpty()) " · ${faInt(low.size)} مورد کم‌موجودی" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (low.isNotEmpty()) Danger else Muted,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
