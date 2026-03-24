package com.devtoolkit.feature.diff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.CodeEditor
import com.devtoolkit.core.ui.OutputPanel
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import kotlin.math.max
import kotlin.math.min

private enum class DiffKind { SAME, ADDED, REMOVED }

private data class DiffRow(
    val kind: DiffKind,
    val leftLine: String?,
    val rightLine: String?,
)

private data class DiffResult(
    val unifiedText: String,
    val splitLeft: String,
    val splitRight: String,
    val added: Int,
    val removed: Int,
    val changed: Int,
)

@Composable
fun DiffScreen(onBack: () -> Unit) {
    val actions = rememberToolActions("diff")
    var leftInput by rememberSaveable { mutableStateOf("") }
    var rightInput by rememberSaveable { mutableStateOf("") }
    var ignoreWhitespace by rememberSaveable { mutableStateOf(false) }
    var splitView by rememberSaveable { mutableStateOf(false) }
    var diffResult by rememberSaveable { mutableStateOf<DiffResult?>(null) }

    fun compare() {
        val result = computeDiff(leftInput, rightInput, ignoreWhitespace)
        diffResult = result
        actions.saveHistory("$leftInput\n---\n$rightInput", result.unifiedText)
    }

    ToolScaffold(title = "Diff / Compare", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Inputs") {
                ClipboardBar(
                    onPaste = { leftInput = actions.readClipboard() },
                    onCopy = { actions.copyText(leftInput) },
                    onClear = { leftInput = "" },
                )
                CodeEditor(
                    value = leftInput,
                    onValueChange = { leftInput = it },
                    placeholder = "Left text",
                )
                ClipboardBar(
                    onPaste = { rightInput = actions.readClipboard() },
                    onCopy = { actions.copyText(rightInput) },
                    onClear = { rightInput = "" },
                )
                CodeEditor(
                    value = rightInput,
                    onValueChange = { rightInput = it },
                    placeholder = "Right text",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = ignoreWhitespace,
                        onClick = { ignoreWhitespace = !ignoreWhitespace },
                        label = { Text("Ignore whitespace") },
                    )
                    FilterChip(
                        selected = splitView,
                        onClick = { splitView = !splitView },
                        label = { Text(if (splitView) "Split view" else "Unified view") },
                    )
                }
                AssistChip(onClick = ::compare, label = { Text("Compare") })
            }

            ToolSection(title = "Diff") {
                val result = diffResult
                if (result == null) {
                    Text(
                        text = "Run compare to see line-level differences.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = "Added ${result.added}  Removed ${result.removed}  Changed ${result.changed}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    ClipboardBar(
                        onPaste = {},
                        onCopy = { actions.copyText(result.unifiedText) },
                        onClear = { diffResult = null },
                        onShare = { actions.shareText(result.unifiedText) },
                        showPaste = false,
                    )
                    if (splitView) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutputPanel(
                                content = result.splitLeft,
                                label = "Left",
                                modifier = Modifier.weight(1f),
                            )
                            OutputPanel(
                                content = result.splitRight,
                                label = "Right",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        OutputPanel(content = result.unifiedText, label = "Unified Diff")
                    }
                }
            }
        }
    }
}

private fun computeDiff(leftInput: String, rightInput: String, ignoreWhitespace: Boolean): DiffResult {
    val leftLines = leftInput.lines()
    val rightLines = rightInput.lines()
    val normalizedLeft = leftLines.map { if (ignoreWhitespace) it.trim() else it }
    val normalizedRight = rightLines.map { if (ignoreWhitespace) it.trim() else it }
    val dp = Array(leftLines.size + 1) { IntArray(rightLines.size + 1) }
    for (i in leftLines.size - 1 downTo 0) {
        for (j in rightLines.size - 1 downTo 0) {
            dp[i][j] = if (normalizedLeft[i] == normalizedRight[j]) {
                dp[i + 1][j + 1] + 1
            } else {
                max(dp[i + 1][j], dp[i][j + 1])
            }
        }
    }

    var i = 0
    var j = 0
    val rows = mutableListOf<DiffRow>()
    while (i < leftLines.size && j < rightLines.size) {
        when {
            normalizedLeft[i] == normalizedRight[j] -> {
                rows += DiffRow(DiffKind.SAME, leftLines[i], rightLines[j])
                i++
                j++
            }
            dp[i + 1][j] >= dp[i][j + 1] -> {
                rows += DiffRow(DiffKind.REMOVED, leftLines[i], null)
                i++
            }
            else -> {
                rows += DiffRow(DiffKind.ADDED, null, rightLines[j])
                j++
            }
        }
    }
    while (i < leftLines.size) {
        rows += DiffRow(DiffKind.REMOVED, leftLines[i], null)
        i++
    }
    while (j < rightLines.size) {
        rows += DiffRow(DiffKind.ADDED, null, rightLines[j])
        j++
    }

    val added = rows.count { it.kind == DiffKind.ADDED }
    val removed = rows.count { it.kind == DiffKind.REMOVED }
    val unifiedText = rows.joinToString("\n") { row ->
        val prefix = when (row.kind) {
            DiffKind.SAME -> " "
            DiffKind.ADDED -> "+"
            DiffKind.REMOVED -> "-"
        }
        prefix + (row.rightLine ?: row.leftLine).orEmpty()
    }
    val splitLeft = rows.joinToString("\n") { row ->
        when (row.kind) {
            DiffKind.ADDED -> "  "
            DiffKind.REMOVED -> "- ${row.leftLine.orEmpty()}"
            DiffKind.SAME -> "  ${row.leftLine.orEmpty()}"
        }
    }
    val splitRight = rows.joinToString("\n") { row ->
        when (row.kind) {
            DiffKind.REMOVED -> "  "
            DiffKind.ADDED -> "+ ${row.rightLine.orEmpty()}"
            DiffKind.SAME -> "  ${row.rightLine.orEmpty()}"
        }
    }
    return DiffResult(
        unifiedText = unifiedText,
        splitLeft = splitLeft,
        splitRight = splitRight,
        added = added,
        removed = removed,
        changed = min(added, removed),
    )
}
