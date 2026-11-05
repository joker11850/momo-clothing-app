@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.customer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.core.Product
import com.hamid.clothing.core.PriceQuote
import com.hamid.clothing.core.Validators
import com.hamid.clothing.customer.CustomerStore
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

fun formatToman(amount: Long): String = PersianFormat.money(amount) + " تومان"
fun faInt(n: Int): String = PersianFormat.digits(n.toString())
fun faDigits(s: String): String = PersianFormat.digits(s)

fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        toast(context, "برنامه‌ی مناسب برای باز کردن این لینک پیدا نشد")
    }
}

fun shareText(context: Context, text: String) {
    try {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, "ارسال").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        toast(context, "اشتراک‌گذاری ممکن نشد")
    }
}

/** Looks up an optional image in res/drawable (for example banner_1 or p_sh_101). */
@Composable
fun drawableId(name: String): Int? {
    val ctx = LocalContext.current
    return remember(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName).takeIf { it != 0 } }
}

private val Palettes = listOf(
    listOf(Color(0xFFE9CBBE), Color(0xFFD7A7A5)),
    listOf(Color(0xFFD9C8DD), Color(0xFFB9A2C9)),
    listOf(Color(0xFFCFE0D4), Color(0xFF9DBEA8)),
    listOf(Color(0xFFF0DDB8), Color(0xFFD8B97E)),
    listOf(Color(0xFFD3DDEB), Color(0xFF9DB1CE))
)

/** Photo from the imported catalogue, then p_<code> in drawables, then a soft gradient with an icon. */
@Composable
fun ProductArt(store: CustomerStore, product: Product, modifier: Modifier = Modifier, maxPx: Int = 500) {
    val photo = store.coverFile(product)
    if (photo != null) {
        LocalImage(photo, modifier, maxPx) { ArtPlaceholder(product.code, product.id, Modifier.fillMaxSize()) }
    } else {
        ArtPlaceholder(product.code, product.id, modifier)
    }
}

@Composable
private fun ArtPlaceholder(code: String, seed: String, modifier: Modifier = Modifier) {
    val id = drawableId("p_" + code.lowercase().replace(Regex("[^a-z0-9]"), "_"))
    if (id != null) {
        Image(painter = painterResource(id), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        val palette = Palettes[kotlin.math.abs(seed.hashCode()) % Palettes.size]
        Box(modifier = modifier.background(Brush.linearGradient(palette)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Checkroom, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(56.dp))
        }
    }
}

/** Opens the file picker and imports the chosen catalog zip. Returns the function that starts it. */
@Composable
fun rememberCatalogImporter(store: CustomerStore): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = store.importCatalog(uri)
                if (!result.ok) toast(context, result.message)
            }
        }
    }
    return { picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = CardColor,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val colors = CardDefaults.cardColors(containerColor = color)
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation) {
            Column(content = content)
        }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation) {
            Column(content = content)
        }
    }
}

@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = GoldInk)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = RoseLight,
        modifier = modifier
    ) {
        Text(
            text,
            color = Burgundy,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, actionText: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        if (actionText != null) PillButton(actionText, onAction)
    }
}

@Composable
fun ScreenTitle(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
        } else {
            Box(modifier = Modifier.width(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Muted,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(32.dp)
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun ErrorList(errors: List<String>) {
    if (errors.isEmpty()) return
    SoftCard(color = Color(0xFFF9DEDC)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (e in errors) Text("• $e", color = Danger, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("متوجه شدم") } }
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = Danger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun <T> ChoiceChips(options: List<Triple<T, String, Boolean>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((value, label, enabled) in options) {
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, enabled = enabled, label = { Text(label) })
        }
    }
}

@Composable
fun QuantityStepper(quantity: Int, onChange: (Int) -> Unit, max: Int = 99) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(RoseLight)
    ) {
        IconButton(onClick = { onChange(quantity - 1) }) { Icon(Icons.Default.Remove, contentDescription = "کم") }
        Text(faInt(quantity), fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { if (quantity < max) onChange(quantity + 1) }, enabled = quantity < max) {
            Icon(Icons.Default.Add, contentDescription = "زیاد")
        }
    }
}

@Composable
fun ProductCard(
    store: CustomerStore,
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val quote: PriceQuote = store.quoteFor(product)
    val favorite = store.isFavorite(product.id)
    SoftCard(modifier = modifier, onClick = onClick) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            ProductArt(store, product, Modifier.fillMaxWidth().height(180.dp))
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(36.dp).clickable { store.toggleFavorite(product.id) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "علاقه‌مندی",
                        tint = Burgundy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (quote.discountPercent > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BadgeRose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                ) {
                    Text(
                        "${faInt(quote.discountPercent)}٪ تخفیف",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(product.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, minLines = 2)
            if (quote.discountPercent > 0) {
                Text(
                    formatToman(quote.basePrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    textDecoration = TextDecoration.LineThrough
                )
            } else {
                Text(" ", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                formatToman(quote.unitPrice),
                style = MaterialTheme.typography.titleSmall,
                color = Burgundy,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
