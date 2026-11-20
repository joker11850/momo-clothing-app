package com.hamid.clothing.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyScreen(nav: Nav, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("سیاست حفظ حریم خصوصی", onBack = { nav.back() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SoftCard(color = RoseLight) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("سیاست حفظ حریم خصوصی MOMO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Burgundy)
                    Text("آخرین به‌روزرسانی: ۳۰ آبان ۱۴۰۵", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            PrivacySection("۱. اطلاعاتی که ذخیره می‌شود", "اطلاعات حساب مانند نام و شماره موبایل، اطلاعات گیرنده و نشانی سفارش، سبد خرید، علاقه‌مندی‌ها و سفارش‌های ثبت‌شده ممکن است برای ارائه خدمات در برنامه ذخیره شود.")
            PrivacySection("۲. ذخیره‌سازی فعلی", "در نسخه فعلی، اطلاعات اصلی برنامه به‌صورت آفلاین روی همین دستگاه نگهداری می‌شود. با حذف برنامه یا پاک‌کردن داده‌های آن، ممکن است اطلاعات محلی از بین برود.")
            PrivacySection("۳. استفاده از اطلاعات", "اطلاعات فقط برای نمایش کاتالوگ، مدیریت سبد خرید، ثبت و پیگیری سفارش، پاسخ‌گویی و بهبود تجربه خرید استفاده می‌شود. اطلاعات کاربران فروخته نمی‌شود.")
            PrivacySection("۴. اشتراک‌گذاری اطلاعات", "در نسخه فعلی، اطلاعات به‌صورت خودکار با شخص ثالث به اشتراک گذاشته نمی‌شود. هنگام ارسال رسید یا محصول در پیام‌رسان‌ها، اطلاعات فقط با برنامه‌ای که خودتان انتخاب می‌کنید ارسال خواهد شد. شناسه‌های ارتباطی فروشگاه در کارت‌های تماس نمایش داده نمی‌شوند و فقط با لمس کارت، لینک مربوط باز می‌شود.")
            PrivacySection("۵. اعلان‌ها و سرور آینده", "اعلان‌های داخل برنامه فعال هستند. پس از راه‌اندازی سرور و سرویس اعلان، نحوه ثبت دستگاه و ارسال اعلان طبق همین سیاست و با حداقل اطلاعات لازم انجام خواهد شد.")
            PrivacySection("۶. امنیت", "از اطلاعات برنامه در حد امکانات ذخیره‌سازی محلی محافظت می‌شود. کلیدهای امضا، رمزها و اطلاعات محرمانه در کد برنامه یا فایل APK قرار نمی‌گیرند.")
            PrivacySection("۷. حقوق کاربر", "کاربر می‌تواند برای اصلاح اطلاعات حساب یا درخواست راهنمایی درباره سفارش با MOMO تماس بگیرد. برای حذف کامل اطلاعات محلی، می‌توانید از تنظیمات گوشی داده‌های برنامه را پاک کنید.")
            SoftCard(color = Beige) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("تماس با ما", fontWeight = FontWeight.ExtraBold, color = GoldInk)
                    Text("برای پرسش‌های مربوط به حریم خصوصی با شماره 09132275597 تماس بگیرید.", style = MaterialTheme.typography.bodySmall, color = GoldInk)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    SoftCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, color = Burgundy)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Ink)
        }
    }
}
