package com.aamirbuneri.abgsmrental

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.data.ThemeMode
import com.aamirbuneri.abgsmrental.ui.AppRoot
import com.aamirbuneri.abgsmrental.ui.theme.ABTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleOpen(intent)
    }

    /** A tapped notification: its button link, or the notification itself. */
    private fun handleOpen(intent: android.content.Intent?) {
        intent ?: return
        val link = intent.getStringExtra(com.aamirbuneri.abgsmrental.work.Notifier.EXTRA_LINK)
        val notice = intent.getIntExtra(com.aamirbuneri.abgsmrental.work.Notifier.EXTRA_NOTICE, 0)
        when {
            !link.isNullOrBlank() -> container.openLink.value = link
            notice > 0 -> container.openLink.value = "notice:$notice"
        }
        intent.removeExtra(com.aamirbuneri.abgsmrental.work.Notifier.EXTRA_LINK)
        intent.removeExtra(com.aamirbuneri.abgsmrental.work.Notifier.EXTRA_NOTICE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the system launch screen up only until the saved settings are read (a few ms).
        val loaded = MutableStateFlow<Settings?>(null)
        splash.setKeepOnScreenCondition { loaded.value == null }
        lifecycleScope.launch { loaded.value = container.prefs.settings.first() }

        handleOpen(intent)
        enableEdgeToEdge()
        setContent {
            val first by loaded.collectAsState()
            val settings by container.prefs.settings.collectAsState(initial = first ?: Settings())
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // status / navigation bar icons follow the app theme, not only the phone's
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            ABTheme(dark = dark) {
                if (first != null) AppRoot(settings = settings, dark = dark)
            }
        }
    }
}
