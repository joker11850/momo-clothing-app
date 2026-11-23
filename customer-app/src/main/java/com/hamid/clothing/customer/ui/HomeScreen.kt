@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.hamid.clothing.customer.ui

import com.hamid.clothing.customer.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.clickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import android.content.Context
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.semantics.Role
import com.hamid.clothing.customer.CustomerStore
import kotlinx.coroutines.delay

private data class Slide(val title: String, val subtitle: String, val cta: String, val target: Screen, val colors: List<Color>, val image: String)

@Composable
fun HomeScreen(store: CustomerStore, nav: Nav, padding: PaddingValues) {
    val context = LocalContext.current
    val importCatalog = rememberCatalogImporter(store)
    val specials = remember(store.products, store.customer, store.catalogDate) {
        store.products.filter { store.quoteFor(it).discountPercent > 0 }
    }
    val newest = remember(store.products) { store.products.asReversed().take(8) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { HomeHeader(store, nav, importCatalog) }

        // Search is intentionally placed directly below the header for quick access.
        item {
            SoftCard(color = Color(0xFFEFDFE0), onClick = { nav.tab(Screen.Shop()) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Muted)
                    Text("جست‌وجو در کالاها، دسته‌ها و کدها…", color = Muted)
                }
            }
        }


        if (store.isDemoCatalog) {
            item {
                SoftCard(color = Beige) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("کاتالوگ فعلی نمونه است", fontWeight = FontWeight.ExtraBold, color = GoldInk)
                        Text("فایل کاتالوگ فروشگاه را از فروشنده بگیرید و دریافت کنید تا کالاها و عکس‌های واقعی نمایش داده شود.", style = MaterialTheme.typography.bodySmall, color = GoldInk)
                        GoldButton("دریافت کاتالوگ فروشگاه", onClick = importCatalog, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }


        item { InstagramCover(context) }
        item { BaleCover(context) }

        item { HeroPager(nav) }

        item {
            // Right: search, left: support (the page is RTL, so the first child sits on the right)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                QuickImageCard(R.drawable.quick_search_momo, "جست‌وجوی کالا", Modifier.weight(1f)) { nav.tab(Screen.Shop()) }
                QuickImageCard(R.drawable.quick_support_momo, "پشتیبانی", Modifier.weight(1f)) { nav.tab(Screen.Contact) }
            }
        }

        item { SectionHeader("دسته‌بندی محصولات") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(store.categories, key = { it.id }, contentType = { "category" }) { c ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(92.dp)
                    ) {
                        Surface(
                            onClick = { nav.go(Screen.Shop(c.id)) },
                            shape = RoundedCornerShape(28.dp),
                            color = RoseLight,
                            modifier = Modifier
                                .size(92.dp)
                                .border(1.dp, Gold.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Checkroom, contentDescription = null, tint = Burgundy, modifier = Modifier.size(40.dp))
                            }
                        }
                        Text(
                            c.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        if (specials.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.dp, Gold.copy(alpha = 0.16f), RoundedCornerShape(32.dp))
                        .background(Rose)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeader("فروش ویژه هفته", "پیشنهادهای محدود", "مشاهده همه") { nav.go(Screen.Shop()) }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(specials, key = { it.id }, contentType = { "product-card" }) { p ->
                            ProductCard(store, p, Modifier.width(170.dp)) { nav.go(Screen.ProductDetail(p.id)) }
                        }
                    }
                }
            }
        }

        item { SectionHeader("کالکشن جدید", "تازه‌ترین مدل‌های فروشگاه", "مشاهده همه") { nav.go(Screen.Shop()) } }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(newest, key = { it.id }, contentType = { "product-card" }) { p ->
                    ProductCard(store, p, Modifier.width(170.dp)) { nav.go(Screen.ProductDetail(p.id)) }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(142.dp)) {
                TrustCard("ارسال مطمئن", "بسته‌بندی استاندارد", Icons.Default.LocalShipping, BurgundyStyle, Modifier.weight(1f))
                TrustCard("پشتیبانی سریع", "پاسخ در پیام‌رسان‌ها", Icons.Default.SupportAgent, EmeraldStyle, Modifier.weight(1f))
                TrustCard("پرداخت آسان", "کارت‌به‌کارت و دفتری", Icons.Default.Payments, ChampagneStyle, Modifier.weight(1f))
            }
        }

        item { WholesaleBanner(nav, store) }
        item {
            SoftCard(color = RoseLight, onClick = { nav.go(Screen.BrandStory) }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Burgundy, modifier = Modifier.size(30.dp))
                    Column(Modifier.weight(1f)) {
                        Text("داستان MOMO", fontWeight = FontWeight.ExtraBold, color = Burgundy)
                        Text("کیفیت، سلیقه و استایل ماندگار", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Text("معرفی برند", color = Burgundy, fontWeight = FontWeight.Bold)
                }
            }
        }

    }
}

/**
 * Home header: finished artwork (res/drawable-nodpi/home_header_momo.webp).
 * Heart circle (right) opens favorites, bag circle (left) opens the cart; the cart count badge is drawn here.
 */
@Composable
private fun HomeHeader(store: CustomerStore, nav: Nav, onImport: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(5.3640f)) {
        val w = maxWidth
        val h = maxHeight
        Image(
            painter = painterResource(R.drawable.home_header_momo),
            contentDescription = "پوشاک MOMO، فروشگاه آنلاین پوشاک",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        val hit = w * 0.14f
        // heart (right)
        Box(
            modifier = Modifier
                .absoluteOffset(x = w * 0.901f - hit / 2, y = h * 0.463f - hit / 2)
                .size(hit)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = "علاقه‌مندی‌ها") { nav.go(Screen.Favorites) }
        )
        // bag (left)
        Box(
            modifier = Modifier
                .absoluteOffset(x = w * 0.097f - hit / 2, y = h * 0.463f - hit / 2)
                .size(hit)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = "سبد خرید") { nav.tab(Screen.Cart) }
        )
        if (store.cartCount > 0) {
            Badge(
                containerColor = Color(0xFFE53935),
                contentColor = Color.White,
                modifier = Modifier.absoluteOffset(x = w * 0.139f - 8.dp, y = h * 0.238f - 8.dp)
            ) { Text(faInt(store.cartCount)) }
        }
    }
}

@Composable
private fun HeroPager(nav: Nav) {
    val slides = listOf(
        Slide("کالکشن‌های ویژه", "انتخاب‌های تازه برای استایل شما", "مشاهده محصولات", Screen.Shop(), listOf(Color(0xFF3B2A2A), Color(0xFF8A5A4A)), "banner_1"),
        Slide("فروش ویژه هفته", "پیشنهادهای محدود با تخفیف", "دیدن تخفیف‌ها", Screen.Shop(), listOf(Color(0xFF6E1F3A), Color(0xFFC2566F)), "banner_2"),
        Slide("خرید عمده", "قیمت ویژه برای همکاران و فروشگاه‌ها", "عضویت عمده", Screen.Wholesale, listOf(Color(0xFF7A5A1E), Color(0xFFD1A64A)), "banner_3")
    )
    val pager = rememberPagerState(pageCount = { slides.size })
    LaunchedEffect(pager) {
        while (true) {
            delay(4500)
            pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(210.dp), pageSpacing = 12.dp) { page ->
            val s = slides[page]
            val imageId = drawableId(s.image)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, Gold.copy(alpha = 0.28f), RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(s.colors))
            ) {
                if (imageId != null) {
                    Image(painter = painterResource(imageId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF241614).copy(alpha = 0.66f), Color.Transparent)
                            )
                        )
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(s.title, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                    Text(s.subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    GoldButton(s.cta, onClick = { nav.go(s.target) })
                }
            }
        }
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in slides.indices) {
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (pager.currentPage == i) 28.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (pager.currentPage == i) Gold else Gold.copy(alpha = 0.26f))
                )
            }
        }
    }
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    SoftCard(modifier = modifier.height(132.dp), color = color, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = Color(0xFFFFFFFF), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                title,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private class TrustStyle(
    val colors: List<Color>,
    val title: Color,
    val subtitle: Color,
    val icon: Color,
    val border: Color
)

private val BurgundyStyle = TrustStyle(
    listOf(Color(0xFF8A2547), Color(0xFF4A1226)), Color.White, Color(0xFFF1DCE2), Color(0xFFE8C772), Color(0x66E8C772)
)
private val EmeraldStyle = TrustStyle(
    listOf(Color(0xFF23705B), Color(0xFF0F3A2F)), Color.White, Color(0xFFD7EBE2), Color(0xFFE8C772), Color(0x66E8C772)
)
private val ChampagneStyle = TrustStyle(
    listOf(Color(0xFFF0D898), Color(0xFFC79B45)), Color(0xFF3A2A10), Color(0xFF5A4318), Color(0xFF6B4A12), Color(0x66FFFFFF)
)

/** Luxury info tile: gradient background, gold icon, text sized to stay readable. */
@Composable
private fun TrustCard(title: String, subtitle: String, icon: ImageVector, style: TrustStyle, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(Brush.linearGradient(style.colors))
            .border(1.dp, style.border, shape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = style.icon, modifier = Modifier.size(24.dp))
                }
            }
            Text(
                title,
                fontWeight = FontWeight.ExtraBold,
                color = style.title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = style.subtitle,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun WholesaleBanner(nav: Nav, store: CustomerStore) {
    val imageId = drawableId("banner_wholesale")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Gold.copy(alpha = 0.28f), RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF1DDB2), Color(0xFFD9B36A))))
    ) {
        if (imageId != null) {
            Image(painter = painterResource(imageId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("خرید آسان، قیمت ویژه، سود بیشتر", style = MaterialTheme.typography.labelMedium, color = GoldInk)
            Text("خرید عمده", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(
                "بهترین انتخاب برای فروشگاه‌داران و همکاران عزیز. قیمت عمده بعد از تأیید عضویت نمایش داده می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = GoldInk
            )
            GoldButton(
                if (store.customer?.canSeeWholesale == true) "شما عضو عمده هستید" else "درخواست عضویت عمده",
                onClick = { nav.go(Screen.Wholesale) }
            )
        }
    }
}

@Composable
private fun CompactSocialCard(
    tag: String,
    title: String,
    handle: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SoftCard(modifier = modifier.height(112.dp), color = Color.Transparent, onClick = onClick) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors)).padding(12.dp)) {
            BrandIcon(tag, Modifier.align(Alignment.TopEnd), size = 30.dp)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.BottomStart).padding(end = 38.dp)
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(handle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PromoBanner(tag: String, title: String, subtitle: String, handle: String, colors: List<Color>, onClick: () -> Unit) {
    SoftCard(color = Color.Transparent, onClick = onClick) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(colors)).padding(22.dp)) {
            BrandIcon(tag, Modifier.align(Alignment.TopEnd), size = 44.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(end = 56.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.18f), modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        handle,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}


private const val MOMO_INSTAGRAM = "poshake_momo"

/** Opens the Instagram app on the page directly; falls back to the browser if the app is not installed. */
private fun openInstagramPage(context: Context) {
    try {
        val app = Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/_u/$MOMO_INSTAGRAM"))
            .setPackage("com.instagram.android")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(app)
    } catch (e: Exception) {
        openUrl(context, "https://instagram.com/$MOMO_INSTAGRAM")
    }
}

/** Instagram cover: the finished artwork (res/drawable-nodpi/banner_instagram_momo.jpg); tapping opens the page. */
@Composable
private fun InstagramCover(context: Context) {
    val image = drawableId("banner_instagram_momo") ?: return
    val shape = RoundedCornerShape(28.dp)
    Image(
        painter = painterResource(image),
        contentDescription = "پیج اینستاگرام momo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1400f / 617f)
            .shadow(4.dp, shape)
            .clip(shape)
            .clickable { openInstagramPage(context) }
    )
}


/** Bale cover: finished artwork (res/drawable-nodpi/banner_bale_momo.jpg); tapping opens the Bale channel. */
@Composable
private fun BaleCover(context: Context) {
    val image = drawableId("banner_bale_momo") ?: return
    val shape = RoundedCornerShape(28.dp)
    Image(
        painter = painterResource(image),
        contentDescription = "کانال بله پوشاک MOMO",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1400f / 700)
            .shadow(4.dp, shape)
            .clip(shape)
            .clickable { openUrl(context, "https://ble.ir/poshak_momo") }
    )
}


/** Quick-access card made of finished artwork (title, subtitle and chevron are part of the picture). */
@Composable
private fun QuickImageCard(image: Int, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Image(
        painter = painterResource(image),
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .aspectRatio(1.8274f)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
    )
}
