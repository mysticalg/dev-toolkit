package com.devtoolkit.core.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun CodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    placeholder: String = "Paste or type here...",
    singleLine: Boolean = false,
) {
    val editorSettings = LocalEditorSettings.current
    val textStyle = TextStyle(
        fontFamily = editorSettings.fontFamily,
        fontSize = editorSettings.fontSize,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp,
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
                textStyle = textStyle,
                readOnly = readOnly,
                singleLine = singleLine,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
