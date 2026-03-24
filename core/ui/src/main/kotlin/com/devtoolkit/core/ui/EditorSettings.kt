package com.devtoolkit.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Immutable
data class EditorSettings(
    val fontSize: TextUnit = 14.sp,
    val fontFamily: FontFamily = FontFamily.Monospace,
)

val LocalEditorSettings = staticCompositionLocalOf { EditorSettings() }

@Composable
fun ProvideEditorSettings(
    fontSizeSp: Int,
    monoFont: String,
    content: @Composable () -> Unit,
) {
    val fontFamily = when (monoFont) {
        "System Monospace" -> FontFamily.Monospace
        else -> FontFamily.Monospace
    }
    CompositionLocalProvider(
        LocalEditorSettings provides EditorSettings(
            fontSize = fontSizeSp.sp,
            fontFamily = fontFamily,
        ),
        content = content,
    )
}
