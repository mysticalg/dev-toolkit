package com.devtoolkit.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ClipboardBar(
    onPaste: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
    showPaste: Boolean = true,
    showCopy: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showPaste) {
            AssistChip(
                onClick = onPaste,
                label = { Text("Paste") },
                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = "Paste") },
            )
        }
        if (showCopy) {
            AssistChip(
                onClick = onCopy,
                label = { Text("Copy") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") },
            )
        }
        onShare?.let {
            AssistChip(
                onClick = it,
                label = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Share") },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Delete, contentDescription = "Clear")
        }
    }
}
