package com.devtoolkit.feature.texttransform

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devtoolkit.core.data.preferences.ToolStateDataStore
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.CodeEditor
import com.devtoolkit.core.ui.OutputPanel
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import kotlinx.coroutines.launch

private enum class TransformOp(val label: String) {
    CAMEL("camelCase"),
    SNAKE("snake_case"),
    KEBAB("kebab-case"),
    PASCAL("PascalCase"),
    UPPER("UPPER"),
    LOWER("lower"),
    TITLE("Title Case"),
    TRIM("Trim"),
    SORT("Sort"),
    DEDUPE("Deduplicate"),
    REVERSE("Reverse"),
    NUMBER("Number lines"),
    WRAP("Wrap 80"),
    UNWRAP("Unwrap"),
    REPLACE("Find & replace"),
    EXTRACT("Extract regex"),
}

private data class SavedPipeline(
    val name: String,
    val selectedOps: List<TransformOp>,
    val findPattern: String,
    val replacement: String,
    val extractPattern: String,
)

@Composable
fun TextTransformScreen(
    sharedText: String? = null,
    onBack: () -> Unit,
) {
    val actions = rememberToolActions("texttransform")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toolStateStore = remember(context) { ToolStateDataStore(context.applicationContext) }
    var input by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    var selectedOps by rememberSaveable { mutableStateOf(listOf<TransformOp>()) }
    var output by rememberSaveable { mutableStateOf("") }
    var findPattern by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var extractPattern by rememberSaveable { mutableStateOf("") }
    var pipelineName by rememberSaveable { mutableStateOf("") }
    val savedPipelines by toolStateStore.textPipelines.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(sharedText) {
        if (input.isBlank() && !sharedText.isNullOrBlank()) {
            input = sharedText
        }
    }

    fun applyPipeline() {
        output = selectedOps.fold(input) { current, op ->
            applyOperation(
                value = current,
                operation = op,
                findPattern = findPattern,
                replacement = replacement,
                extractPattern = extractPattern,
            )
        }
        actions.saveHistory(input, output)
    }

    ToolScaffold(title = "Text Transform", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Input") {
                ClipboardBar(
                    onPaste = { input = actions.readClipboard() },
                    onCopy = { actions.copyText(input) },
                    onClear = { input = "" },
                )
                CodeEditor(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "Paste text to transform",
                )
            }

            ToolSection(title = "Pipeline") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TransformOp.entries.forEach { op ->
                        FilterChip(
                            selected = op in selectedOps,
                            onClick = {
                                selectedOps = if (op in selectedOps) selectedOps - op else selectedOps + op
                            },
                            label = { Text(op.label) },
                        )
                    }
                }
                if (TransformOp.REPLACE in selectedOps) {
                    OutlinedTextField(
                        value = findPattern,
                        onValueChange = { findPattern = it },
                        label = { Text("Find regex") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = replacement,
                        onValueChange = { replacement = it },
                        label = { Text("Replace with") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (TransformOp.EXTRACT in selectedOps) {
                    OutlinedTextField(
                        value = extractPattern,
                        onValueChange = { extractPattern = it },
                        label = { Text("Extract regex") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = ::applyPipeline, label = { Text("Apply") })
                    AssistChip(
                        onClick = {
                            if (pipelineName.isNotBlank() && selectedOps.isNotEmpty()) {
                                val encoded = encodePipeline(
                                    name = pipelineName,
                                    selectedOps = selectedOps,
                                    findPattern = findPattern,
                                    replacement = replacement,
                                    extractPattern = extractPattern,
                                )
                                scope.launch { toolStateStore.saveTextPipeline(encoded) }
                                pipelineName = ""
                            }
                        },
                        label = { Text("Save pipeline") },
                    )
                }
                OutlinedTextField(
                    value = pipelineName,
                    onValueChange = { pipelineName = it },
                    label = { Text("Pipeline name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (savedPipelines.isNotEmpty()) {
                    Text(
                        text = "Saved pipelines",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    savedPipelines.forEach { encoded ->
                        val pipeline = decodePipeline(encoded)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {
                                    selectedOps = pipeline.selectedOps
                                    findPattern = pipeline.findPattern
                                    replacement = pipeline.replacement
                                    extractPattern = pipeline.extractPattern
                                },
                                label = { Text(pipeline.name) },
                            )
                            AssistChip(
                                onClick = { scope.launch { toolStateStore.removeTextPipeline(encoded) } },
                                label = { Text("Delete") },
                            )
                        }
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
                OutputPanel(
                    content = if (output.isBlank()) "Choose one or more transforms, then tap Apply." else output,
                )
            }
        }
    }
}

private fun applyOperation(
    value: String,
    operation: TransformOp,
    findPattern: String,
    replacement: String,
    extractPattern: String,
): String = when (operation) {
    TransformOp.CAMEL -> splitWords(value).joinToString("") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }.replaceFirstChar { it.lowercase() }
    TransformOp.SNAKE -> splitWords(value).joinToString("_") { it.lowercase() }
    TransformOp.KEBAB -> splitWords(value).joinToString("-") { it.lowercase() }
    TransformOp.PASCAL -> splitWords(value).joinToString("") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    TransformOp.UPPER -> value.uppercase()
    TransformOp.LOWER -> value.lowercase()
    TransformOp.TITLE -> splitWords(value).joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    TransformOp.TRIM -> value.lineSequence().joinToString("\n") { it.trim() }.trim()
    TransformOp.SORT -> value.lines().sorted().joinToString("\n")
    TransformOp.DEDUPE -> value.lines().distinct().joinToString("\n")
    TransformOp.REVERSE -> value.lines().reversed().joinToString("\n")
    TransformOp.NUMBER -> value.lines().mapIndexed { index, line -> "${index + 1}. $line" }.joinToString("\n")
    TransformOp.WRAP -> wrapLines(value, 80)
    TransformOp.UNWRAP -> value.replace(Regex("\\s*\\n\\s*"), " ").trim()
    TransformOp.REPLACE -> runCatching { Regex(findPattern).replace(value, replacement) }.getOrDefault(value)
    TransformOp.EXTRACT -> runCatching { Regex(extractPattern).findAll(value).joinToString("\n") { it.value } }.getOrDefault(value)
}

private fun splitWords(value: String): List<String> {
    return value
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotBlank() }
}

private fun wrapLines(value: String, width: Int): String {
    if (value.isBlank()) return value
    return value.lines().joinToString("\n") { line ->
        line.chunked(width).joinToString("\n")
    }
}

private fun encodePipeline(
    name: String,
    selectedOps: List<TransformOp>,
    findPattern: String,
    replacement: String,
    extractPattern: String,
): String = listOf(
    name,
    selectedOps.joinToString(",") { it.name },
    findPattern.replace("|", "\\|"),
    replacement.replace("|", "\\|"),
    extractPattern.replace("|", "\\|"),
).joinToString("|")

private fun decodePipeline(encoded: String): SavedPipeline {
    val parts = encoded.split("|")
    return SavedPipeline(
        name = parts.getOrElse(0) { "Pipeline" },
        selectedOps = parts.getOrElse(1) { "" }
            .split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { value -> TransformOp.entries.find { it.name == value } },
        findPattern = parts.getOrElse(2) { "" }.replace("\\|", "|"),
        replacement = parts.getOrElse(3) { "" }.replace("\\|", "|"),
        extractPattern = parts.getOrElse(4) { "" }.replace("\\|", "|"),
    )
}
