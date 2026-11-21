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
import com.hamid.clothing.core.Customer
import com.hamid.clothing.core.CustomerFilter
import com.hamid.clothing.core.CustomerType

private fun CustomerFilter.title(): String = when (this) {
    CustomerFilter.ALL -> "همه"
    CustomerFilter.WHOLESALE -> "عمده"
    CustomerFilter.RETAIL -> "خرده"
    CustomerFilter.DEBTOR -> "بدهکار"
    CustomerFilter.BLOCKED -> "مسدود"
    CustomerFilter.WHOLESALE_REQUEST -> "درخواست عمده"
}

@Composable
fun CustomersScreen(
    store: AdminStore,
    padding: PaddingValues,
    initialFilter: CustomerFilter,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember(initialFilter) { mutableStateOf(initialFilter) }
    val balances = store.balances
    val visible = CatalogSearch.customers(store.customers, query, filter, balances)

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader("مشتریان")
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("جست‌وجوی نام یا شماره موبایل") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CustomerFilter.values().toList(), key = { it.name }) { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.title()) })
                }
            }
            if (visible.isEmpty()) {
                EmptyState(if (store.customers.isEmpty()) "هنوز مشتری‌ای ثبت نشده است." else "مشتری‌ای با این شرایط پیدا نشد.")
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { c ->
                    CustomerRow(c, balances[c.id] ?: 0L) { onOpen(c.id) }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("مشتری جدید") },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )
    }
}

@Composable
private fun CustomerRow(c: Customer, balance: Long, onClick: () -> Unit) {
    AdminCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.fullName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                if (c.blocked) "مسدود" else c.type.label(),
                style = MaterialTheme.typography.labelMedium,
                color = if (c.blocked) Danger else if (c.type == CustomerType.WHOLESALE) Gold else Muted
            )
        }
        Text(com.hamid.clothing.core.PersianFormat.digits(c.phone), style = MaterialTheme.typography.bodySmall, color = Muted)
        when {
            balance > 0 -> Text(
                "بدهکار: ${formatToman(balance)}",
                style = MaterialTheme.typography.bodySmall,
                color = Danger,
                modifier = Modifier.padding(top = 4.dp)
            )
            balance < 0 -> Text(
                "بستانکار: ${formatToman(-balance)}",
                style = MaterialTheme.typography.bodySmall,
                color = Success,
                modifier = Modifier.padding(top = 4.dp)
            )
            else -> Text("تسویه", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
        if (c.wholesaleStatus == com.hamid.clothing.core.WholesaleStatus.PENDING) {
            Text("درخواست عضویت عمده در انتظار تأیید", style = MaterialTheme.typography.bodySmall, color = Gold)
        }
    }
}
