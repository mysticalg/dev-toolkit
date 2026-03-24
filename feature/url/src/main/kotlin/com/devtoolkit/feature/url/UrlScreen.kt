package com.devtoolkit.feature.url

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoolkit.core.domain.decodeUrlComponent
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.CodeEditor
import com.devtoolkit.core.ui.OutputPanel
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun UrlScreen(
    sharedText: String? = null,
    onBack: () -> Unit,
) {
    val actions = rememberToolActions("url")
    var input by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    var output by rememberSaveable { mutableStateOf("") }
    var scheme by rememberSaveable { mutableStateOf("") }
    var authority by rememberSaveable { mutableStateOf("") }
    var path by rememberSaveable { mutableStateOf("") }
    var fragment by rememberSaveable { mutableStateOf("") }
    var queryPairs by rememberSaveable { mutableStateOf(listOf<String>()) }
    var status by rememberSaveable { mutableStateOf("Paste a URL, query string, or Android intent URI.") }

    LaunchedEffect(sharedText) {
        if (input.isBlank() && !sharedText.isNullOrBlank()) {
            input = sharedText
        }
    }

    fun encodeUrl() {
        output = URLEncoder.encode(input, StandardCharsets.UTF_8.name())
        status = "Encoded input using UTF-8."
        actions.saveHistory(input, output)
    }

    fun decodeUrl() {
        output = decodeUrlComponent(input)
        status = "Decoded input using UTF-8."
        actions.saveHistory(input, output)
    }

    fun parseUrl() {
        if (input.startsWith("intent:", ignoreCase = true)) {
            val intent = Intent.parseUri(input, Intent.URI_INTENT_SCHEME)
            output = buildString {
                appendLine("Intent URI")
                appendLine("Scheme: ${intent.scheme.orEmpty()}")
                appendLine("Package: ${intent.`package`.orEmpty()}")
                appendLine("Action: ${intent.action.orEmpty()}")
                appendLine("Data: ${intent.dataString.orEmpty()}")
            }.trim()
            status = "Parsed Android intent URI."
            actions.saveHistory(input, output)
            return
        }

        val uri = Uri.parse(input)
        scheme = uri.scheme.orEmpty()
        authority = uri.encodedAuthority.orEmpty()
        path = uri.path.orEmpty()
        fragment = uri.fragment.orEmpty()
        queryPairs = uri.queryParameterNames.flatMap { name ->
            val values = uri.getQueryParameters(name)
            if (values.isEmpty()) listOf("$name=") else values.map { value -> "$name=$value" }
        }
        output = buildString {
            appendLine("Scheme: $scheme")
            appendLine("Authority: $authority")
            appendLine("Path: $path")
            appendLine("Fragment: $fragment")
            if (queryPairs.isNotEmpty()) {
                appendLine("Query params:")
                queryPairs.forEach { appendLine(" - $it") }
            }
        }.trim()
        status = "Parsed URL components."
        actions.saveHistory(input, output)
    }

    fun rebuildUrl() {
        val builder = Uri.Builder()
        if (scheme.isNotBlank()) builder.scheme(scheme)
        if (authority.isNotBlank()) builder.encodedAuthority(authority)
        path.trim('/').split('/').filter { it.isNotBlank() }.forEach(builder::appendPath)
        queryPairs.forEach { pair ->
            val key = pair.substringBefore("=")
            val value = pair.substringAfter("=", "")
            if (key.isNotBlank()) builder.appendQueryParameter(key, value)
        }
        if (fragment.isNotBlank()) builder.fragment(fragment)
        output = builder.build().toString()
        status = "Rebuilt URL from edited components."
        actions.saveHistory(input, output)
    }

    ToolScaffold(title = "URL Encoder", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Input") {
                ClipboardBar(
                    onPaste = { input = actions.readClipboard() },
                    onCopy = { actions.copyText(input) },
                    onClear = {
                        input = ""
                        output = ""
                    },
                )
                CodeEditor(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "Paste a full URL, query string, or intent URI",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = ::encodeUrl, label = { Text("Encode") })
                    AssistChip(onClick = ::decodeUrl, label = { Text("Decode") })
                    AssistChip(onClick = ::parseUrl, label = { Text("Parse") })
                    AssistChip(onClick = ::rebuildUrl, label = { Text("Rebuild") })
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ToolSection(title = "Components") {
                OutlinedTextField(
                    value = scheme,
                    onValueChange = { scheme = it },
                    label = { Text("Scheme") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = authority,
                    onValueChange = { authority = it },
                    label = { Text("Authority") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Path") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fragment,
                    onValueChange = { fragment = it },
                    label = { Text("Fragment") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (queryPairs.isNotEmpty()) {
                    Text(
                        text = "Query Params",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    queryPairs.forEachIndexed { index, pair ->
                        OutlinedTextField(
                            value = pair,
                            onValueChange = { value ->
                                queryPairs = queryPairs.toMutableList().apply { set(index, value) }
                            },
                            label = { Text("Param ${index + 1}") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            ToolSection(title = "Output") {
                ClipboardBar(
                    onPaste = {},
                    onCopy = { actions.copyText(output) },
                    onClear = { output = "" },
                    onShare = { actions.shareText(output) },
                    showPaste = false,
                )
                OutputPanel(content = output)
            }
        }
    }
}
