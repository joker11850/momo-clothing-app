@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.Product
import com.hamid.clothing.core.Variant

private data class VariantDraft(val color: String, val size: String, val stock: String)

@Composable
fun ProductFormScreen(
    store: AdminStore,
    padding: PaddingValues,
    productId: String?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val existing = remember(productId) { store.products.firstOrNull { it.id == productId } }

    var name by remember(productId) { mutableStateOf(existing?.name ?: "") }
    var code by remember(productId) { mutableStateOf(existing?.code ?: "") }
    var categoryId by remember(productId) { mutableStateOf(existing?.categoryId ?: "") }
    var description by remember(productId) { mutableStateOf(existing?.description ?: "") }
    var barcode by remember(productId) { mutableStateOf(existing?.barcode ?: "") }
    var purchaseText by remember(productId) { mutableStateOf(existing?.purchasePrice?.toString() ?: "") }
    var landedText by remember(productId) { mutableStateOf(existing?.landedCost?.toString() ?: "") }
    var retailText by remember(productId) { mutableStateOf(existing?.retailPrice?.toString() ?: "") }
    var wholesaleText by remember(productId) { mutableStateOf(existing?.wholesalePrice?.toString() ?: "") }
    var thresholdText by remember(productId) { mutableStateOf((existing?.lowStockThreshold ?: 3).toString()) }
    var active by remember(productId) { mutableStateOf(existing?.active ?: true) }
    val variants = remember(productId) {
        val list = mutableStateListOf<VariantDraft>()
        existing?.variants?.forEach { list.add(VariantDraft(it.color, it.size, it.stock.toString())) }
        if (list.isEmpty()) list.add(VariantDraft("", "", "0"))
        list
    }
    var errors by remember(productId) { mutableStateOf<List<String>>(emptyList()) }
    var newCategoryDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save() {
        val purchase = parseMoney(purchaseText)
        val landed = if (landedText.isBlank()) purchase else parseMoney(landedText)
        val product = Product(
            id = existing?.id ?: store.newId(),
            code = code.trim(),
            name = name.trim(),
            categoryId = categoryId,
            purchasePrice = purchase,
            landedCost = landed,
            retailPrice = parseMoney(retailText),
            wholesalePrice = parseMoney(wholesaleText),
            variants = variants.map { Variant(it.color.trim(), it.size.trim(), parseInt(it.stock)) },
            description = description.trim(),
            imageUris = existing?.imageUris ?: emptyList(),
            barcode = barcode.trim().ifBlank { null },
            lowStockThreshold = parseInt(thresholdText, 3),
            active = active
        )
        val result = store.saveProduct(product)
        if (result.isEmpty()) onDone() else errors = result
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader(
            title = if (existing == null) "کالای جدید" else "ویرایش کالا",
            onBack = onBack,
            actions = { TextButton(onClick = { save() }) { Text("ذخیره") } }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ErrorBox(errors)

            SectionLabel("مشخصات")
            AdminTextField("نام کالا", name, { name = it })
            AdminTextField("کد اختصاصی کالا", code, { code = it })
            AdminTextField("بارکد (اختیاری)", barcode, { barcode = it }, keyboardType = KeyboardType.Number)
            AdminTextField("توضیحات", description, { description = it }, singleLine = false)

            SectionLabel("دسته‌بندی")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ChoiceChips(
                        options = store.categories.map { it.id to it.name },
                        selected = categoryId,
                        onSelect = { categoryId = it }
                    )
                }
                OutlinedButton(onClick = { newCategoryDialog = true }) { Text("+ دسته") }
            }

            SectionLabel("قیمت‌ها (تومان)")
            MoneyField("قیمت خرید", purchaseText, { purchaseText = it })
            MoneyField("قیمت تمام‌شده (خالی = برابر قیمت خرید)", landedText, { landedText = it })
            MoneyField("قیمت فروش خرده", retailText, { retailText = it })
            MoneyField("قیمت فروش عمده", wholesaleText, { wholesaleText = it })

            SectionLabel("رنگ، سایز و موجودی")
            for (index in variants.indices) {
                val v = variants[index]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminTextField("رنگ", v.color, { variants[index] = v.copy(color = it) }, Modifier.weight(1.3f))
                    AdminTextField("سایز", v.size, { variants[index] = v.copy(size = it) }, Modifier.weight(1f))
                    IntField("تعداد", v.stock, { variants[index] = v.copy(stock = it) }, Modifier.weight(1f))
                    IconButton(
                        onClick = { if (variants.size > 1) variants.removeAt(index) },
                        enabled = variants.size > 1
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "حذف ردیف")
                    }
                }
            }
            OutlinedButton(onClick = {
                val last = variants.lastOrNull()
                variants.add(VariantDraft(last?.color ?: "", "", "0"))
            }) { Text("+ رنگ/سایز جدید") }

            IntField("هشدار کمبود: موجودی کمتر یا مساوی", thresholdText, { thresholdText = it }, Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("نمایش در فروشگاه (فعال)", modifier = Modifier.weight(1f))
                Switch(checked = active, onCheckedChange = { active = it })
            }

            Text(
                "ثبت تصویر کالا و اسکن بارکد با دوربین در مرحله‌ی بعد اضافه می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )

            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره کالا") }
            if (existing != null) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("حذف کالا", color = Danger)
                }
            }
            Column(modifier = Modifier.padding(bottom = 24.dp)) {}
        }
    }

    if (newCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newCategoryDialog = false },
            title = { Text("دسته‌بندی جدید") },
            text = { AdminTextField("نام دسته", categoryName, { categoryName = it }) },
            confirmButton = {
                TextButton(onClick = {
                    val created = store.addCategory(categoryName)
                    if (created != null) categoryId = created.id
                    newCategoryDialog = false
                }) { Text("افزودن") }
            },
            dismissButton = { TextButton(onClick = { newCategoryDialog = false }) { Text("انصراف") } }
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "حذف کالا",
            text = "«${existing.name}» حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = { store.deleteProduct(existing.id); confirmDelete = false; onDone() },
            onDismiss = { confirmDelete = false }
        )
    }
}
