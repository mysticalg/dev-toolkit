package com.devtoolkit

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.devtoolkit.core.ui.ProvideEditorSettings
import com.devtoolkit.core.ui.DevToolkitTheme
import com.devtoolkit.core.domain.ThemeMode
import com.devtoolkit.ui.DevToolkitNavHost
import com.devtoolkit.ui.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

const val EXTRA_USE_CLIPBOARD = "com.devtoolkit.extra.USE_CLIPBOARD"
const val EXTRA_TARGET_TOOL = "com.devtoolkit.extra.TARGET_TOOL"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val clipboardText = if (intent?.getBooleanExtra(EXTRA_USE_CLIPBOARD, false) == true) {
            getSystemService(ClipboardManager::class.java)
                ?.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(this)
                ?.toString()
        } else null
        val sharedText = when {
            intent?.action == Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            !clipboardText.isNullOrBlank() -> clipboardText
            else -> null
        }
        val requestedTool = intent?.getStringExtra(EXTRA_TARGET_TOOL)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val prefs by settingsViewModel.preferences.collectAsState()

            val isDark = when (prefs.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> null
            }

            DevToolkitTheme(
                darkTheme = isDark ?: androidx.compose.foundation.isSystemInDarkTheme(),
                amoledBlack = prefs.amoledBlack,
            ) {
                ProvideEditorSettings(
                    fontSizeSp = prefs.fontSize,
                    monoFont = prefs.monoFont,
                ) {
                    DevToolkitNavHost(
                        sharedText = sharedText,
                        requestedTool = requestedTool,
                    )
                }
            }
        }
    }
}
