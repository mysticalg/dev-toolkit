package com.devtoolkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.devtoolkit.R
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val policyText = remember {
        context.resources.openRawResource(R.raw.privacy_policy)
            .bufferedReader()
            .use { it.readText() }
    }

    ToolScaffold(title = "Privacy Policy", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "DevToolkit") {
                Text(
                    text = policyText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ToolSection(title = "Summary") {
                Text(
                    text = "DevToolkit stores data locally, does not use analytics, and does not send tool content off-device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "For privacy questions, contact dhookster@gmail.com.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
