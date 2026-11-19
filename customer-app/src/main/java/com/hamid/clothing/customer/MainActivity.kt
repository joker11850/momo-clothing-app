package com.hamid.clothing.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hamid.clothing.customer.ui.CustomerApp
import com.hamid.clothing.customer.ui.CustomerTheme
import com.hamid.clothing.customer.ui.installImageMemoryCallbacks
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var store: CustomerStore
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installImageMemoryCallbacks(applicationContext)
        NotificationSetup.ensureChannel(applicationContext)
        store = CustomerStore(applicationContext)
        setContent {
            val preferences = getSharedPreferences("customer_preferences", MODE_PRIVATE)
            var darkMode by remember { mutableStateOf(preferences.getBoolean("dark_mode", false)) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CustomerTheme(darkMode = darkMode) {
                    CustomerApp(store, onToggleDarkMode = {
                        darkMode = !darkMode
                        preferences.edit().putBoolean("dark_mode", darkMode).apply()
                    })
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

    /** A catalog zip or an order-status file opened from a messenger, a file manager or the shop's app ("open with" or share to this app). */
    @Suppress("DEPRECATION")
    private fun handleIncoming(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent?.data
            Intent.ACTION_SEND -> intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri != null) scope.launch { store.importIncoming(uri) }
    }
}
