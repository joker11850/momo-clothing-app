package com.hamid.clothing.customer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hamid.clothing.customer.R

private val PrivBurgundy = Color(0xFF8A1038)
private val PrivBody = Color(0xFF4A1A2E)
private val PrivCardBg = Brush.verticalGradient(listOf(Color(0xFFFFFCF9), Color(0xFFFFF3EE)))
private val PrivBorder = Brush.horizontalGradient(listOf(Color(0xFFE2B66A), Color(0xFFF3DDB0), Color(0xFFE2B66A)))
private val PrivNumberBg = Brush.verticalGradient(listOf(Color(0xFFFFE3E8), Color(0xFFF9C9D3)))

private const val PRIVACY_PHONE = "09132275597"
private const val PRIVACY_DATE = "آخرین به‌روزرسانی: ۱ آذر ۱۴۰۵"

private val PrivacySections = listOf(
    "ثبت‌نام و حساب کاربری" to "برای استفاده از برنامه نیازی به ثبت‌نام، ساخت حساب یا ورود با شماره موبایل نیست. برنامه شماره موبایل شما را تأیید نمی‌کند و پیامک تأیید نمی‌فرستد.",
    "اطلاعاتی که ذخیره می‌شود" to "فقط اطلاعاتی که خودتان هنگام خرید وارد می‌کنید ذخیره می‌شود: نام گیرنده، شماره تماس برای هماهنگی سفارش، نشانی تحویل و توضیحات سفارش. سبد خرید، علاقه‌مندی‌ها و سفارش‌های ثبت‌شده هم برای نمایش و پیگیری نگه داشته می‌شوند.",
    "محل نگهداری اطلاعات" to "در نسخه فعلی، همه این اطلاعات فقط روی گوشی خودتان نگهداری می‌شود و برنامه آن‌ها را به سرور یا سرویس دیگری نمی‌فرستد. با حذف برنامه یا پاک‌کردن داده‌های آن از تنظیمات گوشی، اطلاعات از بین می‌رود.",
    "استفاده از اطلاعات" to "اطلاعات فقط برای نمایش کاتالوگ، مدیریت سبد خرید، ثبت و پیگیری سفارش و پاسخ‌گویی به شما استفاده می‌شود. اطلاعات شما فروخته نمی‌شود و برای تبلیغات به کسی داده نمی‌شود.",
    "اشتراک‌گذاری اطلاعات" to "اطلاعات به‌صورت خودکار با هیچ شخص ثالثی به اشتراک گذاشته نمی‌شود. هر وقت سفارش، رسید یا محصولی را از طریق پیام‌رسان می‌فرستید، اطلاعات همان پیام فقط برای برنامه و مخاطبی که خودتان انتخاب کرده‌اید ارسال می‌شود. دکمه‌های تماس، واتساپ، تلگرام، اینستاگرام، روبیکا، ایتا و بله فقط با لمس شما باز می‌شوند و بعد از آن، اطلاعات شما تابع قوانین همان برنامه است.",
    "مجوزهای برنامه" to "برنامه به مخاطبین، موقعیت مکانی، دوربین، میکروفون و پیامک گوشی دسترسی نمی‌گیرد. تنها مجوز آن نمایش اعلان‌های فروشگاه است.",
    "اعلان‌ها" to "اعلان‌ها و پیام‌های فروشگاه داخل برنامه نمایش داده می‌شوند. اگر بعداً سرویس اعلان بیرونی اضافه شود، همین سیاست به‌روز می‌شود و فقط حداقل اطلاعات لازم استفاده خواهد شد.",
    "امنیت" to "از اطلاعات برنامه در حد امکانات ذخیره‌سازی گوشی محافظت می‌شود. برای امنیت بیشتر، قفل صفحه گوشی خود را فعال نگه دارید.",
    "حقوق شما" to "هر زمان می‌توانید اطلاعات ذخیره‌شده را با پاک‌کردن داده‌های برنامه از تنظیمات گوشی حذف کنید. برای اصلاح اطلاعات یک سفارش یا هر پرسشی درباره حریم خصوصی، با MOMO تماس بگیرید.",
    "تغییر این سیاست" to "اگر برنامه امکان جدیدی بگیرد که روی اطلاعات شما اثر بگذارد، این متن هم به‌روز می‌شود و تاریخ بالای آن تغییر می‌کند."
)

@Composable
fun PrivacyScreen(nav: Nav, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("سیاست حفظ حریم خصوصی", onBack = { nav.back() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrivacyHeader()
            PrivacySections.forEachIndexed { index, (title, body) ->
                PrivacySection(index + 1, title, body)
            }
            PrivacyContactCard()
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Header banner (res/drawable-nodpi/privacy_header_momo.webp) with the update date under it. */
@Composable
private fun PrivacyHeader() {
    val shape = RoundedCornerShape(20.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(
            painter = painterResource(R.drawable.privacy_header_momo),
            contentDescription = "سیاست حفظ حریم خصوصی MOMO",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.8455f)
                .shadow(4.dp, shape)
                .clip(shape)
        )
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = PrivBody, modifier = Modifier.size(18.dp))
            Text(PRIVACY_DATE, color = PrivBody, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Collapsible section card: numbered circle on the right, title, body (open by default). */
@Composable
private fun PrivacySection(number: Int, title: String, body: String) {
    var expanded by remember { mutableStateOf(true) }
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clip(shape)
            .background(PrivCardBg)
            .border(1.dp, PrivBorder, shape)
            .clickable(role = Role.Button, onClickLabel = title) { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrivNumberBg)
                    .border(1.dp, Color(0xFFF1D0D6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(faInt(number), color = PrivBurgundy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
            Text(title, color = PrivBurgundy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = PrivBody,
                modifier = Modifier.size(26.dp).rotate(if (expanded) 0f else -90f)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(body, color = PrivBody, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PrivacyContactCard() {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF1EC), Color(0xFFFBDDE4))))
            .border(1.dp, PrivBorder, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("تماس با ما", color = PrivBurgundy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Text("برای پرسش‌های مربوط به حریم خصوصی با شماره ${faDigits(PRIVACY_PHONE)} تماس بگیرید.", color = PrivBody, style = MaterialTheme.typography.bodyMedium)
    }
}
