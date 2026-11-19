package com.hamid.clothing.customer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamid.clothing.core.FaLabels
import com.hamid.clothing.core.Order
import com.hamid.clothing.core.PersianFormat
import com.hamid.clothing.customer.R

@Composable
fun ReceiptWarrantyCard(order: Order, onShare: () -> Unit) {
    SoftCard(color = Beige) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldInk)
                Text("رسید دیجیتال و ضمانت MOMO", fontWeight = FontWeight.ExtraBold, color = GoldInk)
            }
            Text("شماره سفارش: ${order.number}", fontWeight = FontWeight.Bold)
            Text("وضعیت: ${FaLabels.status(order.status)}", color = Muted)
            Text("این رسید، ضمانت اصالت و سلامت ظاهری کالا هنگام تحویل است. برای پیگیری تعویض یا خدمات پس از فروش آن را نگه دارید.", style = MaterialTheme.typography.bodySmall, color = GoldInk)
            Button(onClick = onShare, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Burgundy)) {
                Text("اشتراک‌گذاری رسید")
            }
        }
    }
}

@Composable
fun BrandStoryScreen(nav: Nav, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        ScreenTitle("درباره MOMO", onBack = { nav.back() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.brand_logo),
                contentDescription = "لوگوی MOMO",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(28.dp))
            )
            SoftCard(color = RoseLight) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("استایل ماندگار، انتخاب MOMO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Burgundy)
                    Text("MOMO با نگاه به جزئیات، پوشاکی را انتخاب می‌کند که کیفیت، راحتی و زیبایی را کنار هم قرار می‌دهد. هدف ما این است که هر انتخاب، بخشی از استایل شخصی شما باشد.", color = Ink)
                }
            }
            Text("چرا MOMO؟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            BrandValue(Icons.Default.VerifiedUser, "اصالت و کیفیت", "انتخاب محصول با توجه به کیفیت دوخت، پارچه و ماندگاری")
            BrandValue(Icons.Default.CheckCircle, "انتخاب با سلیقه شما", "رنگ‌ها و مدل‌هایی برای استایل روزمره، رسمی و خاص")
            BrandValue(Icons.Default.LocalShipping, "تجربه خرید مطمئن", "بسته‌بندی دقیق و پیگیری سفارش تا رسیدن به دست شما")
            SoftCard(color = Beige) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("راهنمای نگهداری", fontWeight = FontWeight.ExtraBold, color = GoldInk)
                    Text("برای ماندگاری بیشتر لباس، دستور شست‌وشوی درج‌شده روی برچسب محصول را رعایت کنید و لباس را در دمای مناسب بشویید.", style = MaterialTheme.typography.bodySmall, color = GoldInk)
                }
            }
            GoldButton("تماس با MOMO", { nav.tab(Screen.Contact) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BrandValue(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    SoftCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Burgundy, modifier = Modifier.size(30.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(text, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}
