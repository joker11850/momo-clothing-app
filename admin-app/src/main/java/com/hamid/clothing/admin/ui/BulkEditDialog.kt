@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.core.BulkEdit
import com.hamid.clothing.core.BulkEditEngine

private enum class BulkKind { PRICE, CATEGORY, VISIBILITY, THRESHOLD }

/** One edit applied to every selected product. [onDone] receives a short result message. */
@Composable
fun BulkEditDialog(
    store: AdminStore,
    ids: Set<String>,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var kind by remember { mutableStateOf(BulkKind.PRICE) }
    var increase by remember { mutableStateOf(true) }
    var percentText by remember { mutableStateOf("") }
    var retail by remember { mutableStateOf(true) }
    var wholesale by remember { mutableStateOf(true) }
    var roundTo by remember { mutableStateOf(1000L) }
    var categoryId by remember { mutableStateOf(store.categories.firstOrNull()?.id ?: "") }
    var visible by remember { mutableStateOf(true) }
    var thresholdText by remember { mutableStateOf("3") }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }

    val percent = parseInt(percentText) * (if (increase) 1 else -1)
    val sample = store.products.firstOrNull { it.id in ids }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش گروهی (${faInt(ids.size)} کالا)") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChips(
                    options = listOf(
                        BulkKind.PRICE to "تغییر قیمت",
                        BulkKind.CATEGORY to "دسته‌بندی",
                        BulkKind.VISIBILITY to "نمایش/پنهان",
                        BulkKind.THRESHOLD to "حد هشدار"
                    ),
                    selected = kind,
                    onSelect = { kind = it; errors = emptyList() }
                )
                when (kind) {
                    BulkKind.PRICE -> {
                        ChoiceChips(
                            options = listOf(true to "افزایش", false to "کاهش"),
                            selected = increase,
                            onSelect = { increase = it }
                        )
                        IntField("درصد", percentText, { percentText = it }, Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("قیمت فروش خرده", modifier = Modifier.weight(1f))
                            Switch(checked = retail, onCheckedChange = { retail = it })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("قیمت فروش عمده", modifier = Modifier.weight(1f))
                            Switch(checked = wholesale, onCheckedChange = { wholesale = it })
                        }
                        Text("گرد کردن به:", style = MaterialTheme.typography.bodySmall, color = Muted)
                        ChoiceChips(
                            options = listOf(1L to "بدون گرد", 1000L to "هزار", 10_000L to "ده هزار", 100_000L to "صد هزار"),
                            selected = roundTo,
                            onSelect = { roundTo = it }
                        )
                        if (sample != null && percent != 0) {
                            Text(
                                "نمونه: «${sample.name}» خرده ${formatToman(sample.retailPrice)} ← ${formatToman(BulkEditEngine.adjustPrice(sample.retailPrice, percent, roundTo))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gold
                            )
                        }
                        Text(
                            "قیمت عمده‌ای که ثبت نشده (صفر) تغییر نمی‌کند و قیمت عمده هیچ‌وقت بیشتر از خرده نمی‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted
                        )
                    }
                    BulkKind.CATEGORY -> ChoiceChips(
                        options = store.categories.map { it.id to it.name },
                        selected = categoryId,
                        onSelect = { categoryId = it }
                    )
                    BulkKind.VISIBILITY -> ChoiceChips(
                        options = listOf(true to "نمایش در فروشگاه", false to "پنهان"),
                        selected = visible,
                        onSelect = { visible = it }
                    )
                    BulkKind.THRESHOLD -> IntField("هشدار وقتی موجودی کمتر یا مساوی این عدد شد", thresholdText, { thresholdText = it }, Modifier.fillMaxWidth())
                }
                if (errors.isNotEmpty()) ErrorBox(errors)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val edit = when (kind) {
                    BulkKind.PRICE -> BulkEdit.PriceChange(percent, retail, wholesale, roundTo)
                    BulkKind.CATEGORY -> BulkEdit.SetCategory(categoryId)
                    BulkKind.VISIBILITY -> BulkEdit.SetActive(visible)
                    BulkKind.THRESHOLD -> BulkEdit.SetLowStockThreshold(parseInt(thresholdText, -1))
                }
                val (problems, result) = store.bulkEdit(ids, edit)
                if (result == null) {
                    errors = problems
                } else {
                    onDone(
                        "${faInt(result.changed)} کالا تغییر کرد" +
                            if (result.clamped > 0) " و قیمت عمده‌ی ${faInt(result.clamped)} کالا با قیمت خرده برابر شد" else ""
                    )
                }
            }) { Text("اعمال") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
