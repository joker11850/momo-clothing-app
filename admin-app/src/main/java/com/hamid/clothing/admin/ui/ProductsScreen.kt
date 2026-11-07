@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
    onEdit: (String) -> Unit,
    onPublish: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var lowOnly by remember(initialLowStock) { mutableStateOf(initialLowStock) }
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var bulkDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val visible = CatalogSearch.products(store.products, query, categoryId, lowOnly)

    fun stopSelecting() {
        selecting = false
        selected.clear()
    }

    BackHandler(enabled = selecting) { stopSelecting() }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                if (selecting) "${faInt(selected.size)} کالا انتخاب شد" else "کالاها",
                actions = {
                    if (selecting) {
                        TextButton(onClick = {
                            selected.clear()
                            selected.addAll(visible.map { it.id })
                        }) { Text("همه") }
                        TextButton(onClick = { stopSelecting() }) { Text("لغو") }
                    } else {
                        TextButton(onClick = { selecting = true }) { Text("انتخاب") }
                        TextButton(onClick = onPublish) { Text("انتشار") }
                    }
                }
            )
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { p ->
                    ProductRow(store, p, selecting, p.id in selected) {
                        if (selecting) {
                            if (p.id in selected) selected.remove(p.id) else selected.add(p.id)
                        } else {
                            onEdit(p.id)
                        }
                    }
                }
            }
        }

        if (selecting) {
            Surface(
                color = SurfaceHigh,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { bulkDialog = true },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("ویرایش گروهی") }
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("حذف", color = Danger) }
                }
            }
        } else {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("کالای جدید") },
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            )
        }
    }

    if (bulkDialog) {
        BulkEditDialog(
            store = store,
            ids = selected.toSet(),
            onDone = { message ->
                bulkDialog = false
                toast(context, message)
                stopSelecting()
            },
            onDismiss = { bulkDialog = false }
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "حذف کالاها",
            text = "${faInt(selected.size)} کالا حذف شود؟ کالاهایی که در سفارش‌ها استفاده شده‌اند حذف نمی‌شوند (آن‌ها را پنهان کنید).",
            confirmLabel = "حذف",
            onConfirm = {
                confirmDelete = false
                val (deleted, skipped) = store.bulkDelete(selected.toSet())
                toast(context, "${faInt(deleted)} کالا حذف شد" + if (skipped > 0) " و ${faInt(skipped)} کالا به‌خاطر سابقه‌ی سفارش نماند" else "")
                stopSelecting()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun ProductRow(store: AdminStore, p: Product, selecting: Boolean, checked: Boolean, onClick: () -> Unit) {
    val category = store.categories.firstOrNull { it.id == p.categoryId }?.name ?: "بدون دسته"
    val low = p.lowStockVariants
    val cover = p.imageUris.firstOrNull()?.let { store.images.file(it) }
    AdminCard(onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selecting) Checkbox(checked = checked, onCheckedChange = null)
            LocalImage(
                cover,
                Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceHigh),
                maxPx = 240
            ) {
                Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                    Text(if (p.imageUris.isEmpty()) "بدون عکس" else "…", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (!p.active) Text("پنهان", color = Muted, style = MaterialTheme.typography.labelSmall)
                    if (low.isNotEmpty()) {
                        Icon(Icons.Default.Warning, contentDescription = "کمبود موجودی", tint = Danger)
                    }
                }
                Text("${p.code} · $category", style = MaterialTheme.typography.bodySmall, color = Muted)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("خرده: ${formatToman(p.retailPrice)}", style = MaterialTheme.typography.bodySmall)
                    if (p.wholesalePrice > 0) {
                        Text("عمده: ${formatToman(p.wholesalePrice)}", style = MaterialTheme.typography.bodySmall, color = Gold)
                    }
                }
                Text(
                    "موجودی: ${faInt(p.totalStock)} عدد" + if (low.isNotEmpty()) " · ${faInt(low.size)} مورد کم‌موجودی" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (low.isNotEmpty()) Danger else Muted
                )
            }
        }
    }
}
