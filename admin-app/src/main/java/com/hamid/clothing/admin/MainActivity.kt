package com.hamid.clothing.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hamid.clothing.admin.ui.AdminApp
import com.hamid.clothing.admin.ui.AdminTheme
import com.hamid.clothing.core.OrderExchange
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var store: AdminStore
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AdminStore(applicationContext)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AdminTheme(store.themeMode) {
                    AdminApp(store)
                }
            }
        }
        if (savedInstanceState == null) handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    /** An order file opened from a messenger, a file manager or sent directly by the customer app. */
    @Suppress("DEPRECATION")
    private fun handleIncoming(intent: Intent?) {
        val uri: Uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent?.data
            Intent.ACTION_SEND -> intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            else -> null
        } ?: return
        scope.launch {
            val text = withContext(Dispatchers.IO) { readSmallText(uri) }
            val error = if (text == null) "فایل خوانده نشد یا خیلی بزرگ است" else store.stageIncoming(text)
            if (error != null) Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
        }
    }

    /** Reads at most [OrderExchange.MAX_CHARS] bytes; bigger files are not order files. */
    private fun readSmallText(uri: Uri): String? = try {
        contentResolver.openInputStream(uri)?.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            var tooBig = false
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                total += n
                if (total > OrderExchange.MAX_CHARS) {
                    tooBig = true
                    break
                }
                out.write(buffer, 0, n)
            }
            if (tooBig) null else String(out.toByteArray(), Charsets.UTF_8)
        }
    } catch (e: Exception) {
        null
    }
}
