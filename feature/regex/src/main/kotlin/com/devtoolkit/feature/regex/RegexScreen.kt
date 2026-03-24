package com.devtoolkit.feature.regex

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import java.util.regex.Pattern

private data class RegexMatchInfo(
    val value: String,
    val range: IntRange,
    val groups: List<Pair<String, String>>,
)

private data class RegexAnalysis(
    val highlightedText: String = "",
    val replacementPreview: String = "",
    val matchCount: Int = 0,
    val matches: List<RegexMatchInfo> = emptyList(),
    val error: String? = null,
)

@Composable
fun RegexScreen(onBack: () -> Unit) {
    val actions = rememberToolActions("regex")
    val context = LocalContext.current
    val toolStateStore = remember(context) { ToolStateDataStore(context.applicationContext) }
    var pattern by rememberSaveable { mutableStateOf("") }
    var flags by rememberSaveable { mutableStateOf("i") }
    var input by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var replaceMode by rememberSaveable { mutableStateOf(false) }
    var showCheatSheet by rememberSaveable { mutableStateOf(false) }
    val recentPatterns by toolStateStore.regexHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    val analysis = remember(pattern, flags, input, replacement, replaceMode) {
        analyzeRegex(pattern, flags, input, replacement, replaceMode)
    }

    LaunchedEffect(pattern, analysis.error) {
        if (pattern.isNotBlank() && analysis.error == null) {
            delay(400)
            toolStateStore.rememberRegexPattern(pattern)
        }
    }

    ToolScaffold(title = "Regex Tester", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Pattern") {
                ClipboardBar(
                    onPaste = { pattern = actions.readClipboard() },
                    onCopy = { actions.copyText(pattern) },
                    onClear = {
                        pattern = ""
                        flags = "i"
                    },
                )
                CodeEditor(
                    value = pattern,
                    onValueChange = { pattern = it },
                    placeholder = "Enter a Java regular expression",
                    singleLine = true,
                )
                CodeEditor(
                    value = flags,
                    onValueChange = { flags = it.filter { flag -> flag.lowercaseChar() in "imsuxd" } },
                    placeholder = "Flags: i m s u x d",
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    commonPatterns.forEach { (label, value) ->
                        AssistChip(
                            onClick = { pattern = value },
                            label = { Text(label) },
                        )
                    }
                }
                if (recentPatterns.isNotEmpty()) {
                    Text(
                        text = "Recent patterns",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recentPatterns.forEach { savedPattern ->
                            AssistChip(
                                onClick = { pattern = savedPattern },
                                label = { Text(savedPattern.take(24)) },
                            )
                        }
                    }
                }
                FilterChip(
                    selected = replaceMode,
                    onClick = { replaceMode = !replaceMode },
                    label = { Text("Replace mode") },
                )
                if (replaceMode) {
                    CodeEditor(
                        value = replacement,
                        onValueChange = { replacement = it },
                        placeholder = "Replacement pattern, e.g. \$1 or \${name}",
                        singleLine = true,
                    )
                }
            }

            ToolSection(title = "Test String") {
                ClipboardBar(
                    onPaste = { input = actions.readClipboard() },
                    onCopy = { actions.copyText(input) },
                    onClear = { input = "" },
                )
                CodeEditor(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "Paste text to test against the pattern",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            val summary = buildString {
                                append("Matches: ${analysis.matchCount}\n")
                                append(if (replaceMode) analysis.replacementPreview else analysis.highlightedText)
                            }
                            actions.saveHistory("$pattern\n/$flags/\n$input", summary)
                        },
                        label = { Text("Save snapshot") },
                    )
                    AssistChip(
                        onClick = { showCheatSheet = !showCheatSheet },
                        label = { Text(if (showCheatSheet) "Hide cheat sheet" else "Show cheat sheet") },
                    )
                }
                if (showCheatSheet) {
                    Text(
                        text = cheatSheetText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Engine: java.util.regex. Some PCRE-only features may differ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ToolSection(title = "Results") {
                analysis.error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = "Matches found: ${analysis.matchCount}",
                    style = MaterialTheme.typography.labelLarge,
                )
                ClipboardBar(
                    onPaste = {},
                    onCopy = {
                        actions.copyText(if (replaceMode) analysis.replacementPreview else analysis.highlightedText)
                    },
                    onClear = {},
                    onShare = {
                        actions.shareText(if (replaceMode) analysis.replacementPreview else analysis.highlightedText)
                    },
                    showPaste = false,
                    showCopy = analysis.highlightedText.isNotBlank() || analysis.replacementPreview.isNotBlank(),
                )
                OutputPanel(
                    label = if (replaceMode) "Replacement Preview" else "Highlighted Matches",
                    content = if (replaceMode) analysis.replacementPreview else analysis.highlightedText,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    analysis.matches.forEachIndexed { index, match ->
                        Text(
                            text = "Match ${index + 1}: ${match.value} [${match.range.first}..${match.range.last}]",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (match.groups.isEmpty()) {
                            Text(
                                text = "No capture groups",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            match.groups.forEach { (name, value) ->
                                Text(
                                    text = "$name = $value",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun analyzeRegex(
    pattern: String,
    flags: String,
    input: String,
    replacement: String,
    replaceMode: Boolean,
): RegexAnalysis {
    if (pattern.isBlank() || input.isBlank()) return RegexAnalysis()
    return runCatching {
        val compiled = Pattern.compile(pattern, parseFlags(flags))
        val matcher = compiled.matcher(input)
        val matches = mutableListOf<RegexMatchInfo>()
        val groupNames = extractNamedGroups(pattern)
        while (matcher.find()) {
            val groups = buildList {
                for (index in 1..matcher.groupCount()) {
                    add("Group $index" to matcher.group(index).orEmpty())
                }
                groupNames.forEach { (index, name) ->
                    if (index <= matcher.groupCount()) {
                        add(name to matcher.group(index).orEmpty())
                    }
                }
            }
            matches += RegexMatchInfo(
                value = matcher.group(),
                range = matcher.start() until matcher.end(),
                groups = groups.distinctBy { it.first },
            )
        }
        RegexAnalysis(
            highlightedText = highlightMatches(input, matches.map { it.range }),
            replacementPreview = if (replaceMode) compiled.matcher(input).replaceAll(replacement) else "",
            matchCount = matches.size,
            matches = matches,
        )
    }.getOrElse { throwable ->
        RegexAnalysis(error = throwable.message ?: "Invalid regular expression")
    }
}

private fun parseFlags(flags: String): Int {
    var result = 0
    flags.lowercase().forEach { flag ->
        result = result or when (flag) {
            'i' -> Pattern.CASE_INSENSITIVE
            'm' -> Pattern.MULTILINE
            's' -> Pattern.DOTALL
            'u' -> Pattern.UNICODE_CASE
            'x' -> Pattern.COMMENTS
            'd' -> Pattern.UNIX_LINES
            else -> 0
        }
    }
    return result
}

private fun extractNamedGroups(pattern: String): List<Pair<Int, String>> {
    return Regex("\\(\\?<([A-Za-z][A-Za-z0-9_]*)>").findAll(pattern)
        .mapIndexed { index, matchResult -> index + 1 to matchResult.groupValues[1] }
        .toList()
}

private fun highlightMatches(input: String, ranges: List<IntRange>): String {
    if (ranges.isEmpty()) return input
    val builder = StringBuilder()
    var cursor = 0
    ranges.sortedBy { it.first }.forEach { range ->
        if (cursor < range.first) {
            builder.append(input.substring(cursor, range.first))
        }
        builder.append('[')
        builder.append(input.substring(range.first, range.last + 1))
        builder.append(']')
        cursor = range.last + 1
    }
    if (cursor < input.length) {
        builder.append(input.substring(cursor))
    }
    return builder.toString()
}

private val commonPatterns = listOf(
    "Email" to "[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+",
    "URL" to "https?://[^\\s]+",
    "IPv4" to "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b",
    "Date" to "\\b\\d{4}-\\d{2}-\\d{2}\\b",
    "UUID" to "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\\b",
)

private val cheatSheetText = """
    . any char
    \d digit
    \w word char
    * zero or more
    + one or more
    ? optional / lazy modifier
    () capture group
    (?<name>...) named capture group
    ^ start of line
    $ end of line
""".trimIndent()
