@file:OptIn(ExperimentalMaterial3Api::class)

package com.hamid.clothing.admin.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.hamid.clothing.admin.AdminStore
import com.hamid.clothing.admin.BackupFile
import com.hamid.clothing.admin.PreparedRestore
import com.hamid.clothing.core.WrongPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BackupScreen(store: AdminStore, padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var building by remember { mutableStateOf(false) }
    var made by remember { mutableStateOf<BackupFile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var restoring by remember { mutableStateOf(false) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var prepared by remember { mutableStateOf<PreparedRestore?>(null) }

    // a decrypted backup that was never applied is removed when the screen closes
    DisposableEffect(Unit) {
        onDispose { prepared?.let { store.cancelRestore(it) } }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedUri = uri
            restorePassword = ""
            restoreError = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScreenHeader("پشتیبان‌گیری و بازیابی", onBack = onBack)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminCard {
                Text("چرا پشتیبان؟", fontWeight = FontWeight.Bold, color = Gold)
                Text(
                    "همه‌ی اطلاعات (کالا، عکس، مشتری، سفارش، چک، حساب‌ها) فقط روی همین گوشی است. اگر گوشی گم شود، خراب شود یا برنامه پاک شود، بدون پشتیبان همه‌چیز از بین می‌رود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Text(
                    if (store.lastBackupAt > 0) "آخرین پشتیبان: ${faDate(store.lastBackupAt)}" else "هنوز پشتیبان‌گیری نکرده‌اید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (store.lastBackupAt > 0) Success else Danger,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            SectionLabel("ساخت پشتیبان")
            Text(
                "فایل پشتیبان با رمزی که شما می‌گذارید رمزنگاری می‌شود. فایل را در جای امن (مثلاً Saved Messages تلگرام، فضای ابری یا کامپیوتر) نگه دارید.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            PasswordField("رمز پشتیبان (حداقل ۶ نویسه)", password) { password = it; error = null }
            PasswordField("تکرار رمز", confirm) { confirm = it; error = null }
            AdminCard {
                Text(
                    "رمز را حتماً جایی یادداشت کنید. رمز فراموش‌شده قابل بازیابی نیست و فایل پشتیبان بدون آن باز نمی‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Danger
                )
            }
            if (error != null) ErrorBox(listOf(error ?: ""))
            val result = made
            if (result != null) {
                AdminCard {
                    Text("پشتیبان آماده شد", fontWeight = FontWeight.Bold, color = Success)
                    Text(
                        "${result.file.name} — ${faInt(result.images)} عکس، ${faInt((result.bytes / 1024).toInt())} کیلوبایت",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Button(
                onClick = {
                    when {
                        password.length < 6 -> error = "رمز باید حداقل ۶ نویسه باشد"
                        password != confirm -> error = "رمز و تکرار آن یکسان نیست"
                        else -> {
                            building = true
                            error = null
                            val chars = password.toCharArray()
                            scope.launch {
                                val built = withContext(Dispatchers.IO) {
                                    try {
                                        store.createBackup(chars)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                building = false
                                if (built == null) {
                                    error = "ساخت پشتیبان ناموفق بود"
                                } else {
                                    made = built
                                    shareFile(context, built.file, "application/octet-stream", "ذخیره‌ی پشتیبان")
                                }
                            }
                        }
                    }
                },
                enabled = !building,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (building) "در حال ساخت پشتیبان…" else "ساخت و ذخیره‌ی پشتیبان") }

            SectionLabel("بازیابی")
            Text(
                "با بازیابی، همه‌ی اطلاعات فعلی برنامه با اطلاعات فایل پشتیبان جایگزین می‌شود. از این گزینه برای گوشی جدید یا بعد از پاک‌شدن برنامه استفاده کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            OutlinedButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !restoring,
                modifier = Modifier.fillMaxWidth()
            ) { Text("انتخاب فایل پشتیبان (csbk.)") }
            Box(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    // step 1: ask for the password of the chosen file
    val uri = pickedUri
    if (uri != null && prepared == null) {
        AlertDialog(
            onDismissRequest = { if (!restoring) pickedUri = null },
            title = { Text("رمز فایل پشتیبان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PasswordField("رمز", restorePassword) { restorePassword = it; restoreError = null }
                    if (restoreError != null) Text(restoreError ?: "", color = Danger)
                    if (restoring) Text("در حال باز کردن فایل…", color = Muted)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !restoring && restorePassword.isNotEmpty(),
                    onClick = {
                        restoring = true
                        restoreError = null
                        val chars = restorePassword.toCharArray()
                        scope.launch {
                            val outcome = withContext(Dispatchers.IO) {
                                try {
                                    store.prepareRestore(uri, chars) to null
                                } catch (e: WrongPasswordException) {
                                    null to "رمز اشتباه است یا فایل آسیب دیده است"
                                } catch (e: java.io.IOException) {
                                    null to "این فایل، پشتیبان معتبر نیست"
                                } catch (e: Exception) {
                                    null to "بازیابی ممکن نشد: فایل خراب یا ناسازگار است"
                                }
                            }
                            restoring = false
                            if (outcome.first != null) prepared = outcome.first else restoreError = outcome.second
                        }
                    }
                ) { Text("باز کردن") }
            },
            dismissButton = { TextButton(enabled = !restoring, onClick = { pickedUri = null }) { Text("انصراف") } }
        )
    }

    // step 2: show what is inside and ask for the final confirmation
    val ready = prepared
    if (ready != null) {
        val s = ready.snapshot
        AlertDialog(
            onDismissRequest = {},
            title = { Text("جایگزینی اطلاعات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ready.createdAt > 0) Text("تاریخ پشتیبان: ${faDate(ready.createdAt)}")
                    Text("${faInt(s.products.size)} کالا · ${faInt(s.customers.size)} مشتری · ${faInt(s.orders.size)} سفارش")
                    Text("${faInt(s.suppliers.size)} تأمین‌کننده · ${faInt(s.cheques.size)} چک · ${faInt(s.expenses.size)} هزینه")
                    Text(
                        "همه‌ی اطلاعات فعلی این برنامه حذف و با این پشتیبان جایگزین می‌شود. ادامه می‌دهید؟",
                        color = Danger,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.applyRestore(ready)
                    prepared = null
                    pickedUri = null
                    toast(context, "اطلاعات بازیابی شد")
                }) { Text("جایگزین شود", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = {
                    store.cancelRestore(ready)
                    prepared = null
                    pickedUri = null
                }) { Text("انصراف") }
            }
        )
    }
}
