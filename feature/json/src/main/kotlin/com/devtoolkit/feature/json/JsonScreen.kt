package com.devtoolkit.feature.json

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoolkit.core.domain.looksLikeJson
import com.devtoolkit.core.domain.looksLikeXml
import com.devtoolkit.core.domain.looksLikeYaml
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.CodeEditor
import com.devtoolkit.core.ui.OutputPanel
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.MarkedYAMLException
import org.xml.sax.SAXParseException
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private const val JsonPaginationThresholdBytes = 1_000_000
private const val JsonPageChars = 60_000
private const val JsonTreeMaxBytes = 300_000

private enum class JsonAction(val label: String) {
    PRETTY("Pretty"),
    MINIFY("Minify"),
    TO_JSON("To JSON"),
    TO_YAML("To YAML"),
    VALIDATE("Validate"),
}

private data class JsonResult(
    val output: String,
    val detectedFormat: String,
    val notice: String? = null,
    val treeJson: String? = null,
)

private data class ValidationIssue(
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
    val excerpt: String? = null,
)

@Composable
fun JsonScreen(
    sharedText: String? = null,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = rememberToolActions("json")

    var input by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var detectedFormat by rememberSaveable { mutableStateOf("Unknown") }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var treeJson by rememberSaveable { mutableStateOf<String?>(null) }
    var showTree by rememberSaveable { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var validationIssue by remember { mutableStateOf<ValidationIssue?>(null) }
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var exportFileName by rememberSaveable { mutableStateOf("devtoolkit-output.txt") }

    val pageCount = remember(output) { outputPageCount(output) }
    val pagedOutput = remember(output, currentPage) { outputPage(output, currentPage) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null && output.isNotBlank()) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        writeDocumentText(context, uri, output)
                    }
                }.onSuccess {
                    notice = "Exported output to ${uri.lastPathSegment ?: "a file"}."
                }.onFailure { throwable ->
                    error = throwable.message ?: "Unable to export file."
                }
            }
        }
    }

    LaunchedEffect(sharedText) {
        if (input.isBlank() && !sharedText.isNullOrBlank()) {
            input = sharedText
        }
    }

    LaunchedEffect(Unit) {
        val clipboard = actions.readClipboard()
        if (input.isBlank() && (looksLikeJson(clipboard) || looksLikeYaml(clipboard) || looksLikeXml(clipboard))) {
            input = clipboard
        }
    }

    fun runAction(action: JsonAction) {
        isProcessing = true
        error = null
        validationIssue = null
        notice = if (action == JsonAction.VALIDATE) "Validating..." else "Processing..."
        scope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    processContent(input, action)
                }
            }.onSuccess { result ->
                output = result.output
                detectedFormat = result.detectedFormat
                notice = result.notice
                treeJson = result.treeJson
                showTree = result.treeJson != null
                currentPage = 0
                validationIssue = null
                exportFileName = buildExportFileName(result.detectedFormat)
                error = null
                if (action != JsonAction.VALIDATE) {
                    actions.saveHistory(input, output)
                }
            }.onFailure { throwable ->
                error = throwable.message ?: "Unable to parse the input."
                output = ""
                notice = null
                treeJson = null
                showTree = false
                currentPage = 0
                validationIssue = extractValidationIssue(input, throwable)
            }
            isProcessing = false
        }
    }

    ToolScaffold(title = "JSON / YAML / XML", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Input") {
                ClipboardBar(
                    onPaste = { input = actions.readClipboard() },
                    onCopy = { actions.copyText(input) },
                    onClear = {
                        input = ""
                        output = ""
                        error = null
                        notice = null
                        treeJson = null
                        validationIssue = null
                        currentPage = 0
                    },
                )
                CodeEditor(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "Paste JSON, YAML, or XML here",
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    JsonAction.entries.forEach { action ->
                        AssistChip(
                            onClick = { runAction(action) },
                            enabled = !isProcessing,
                            label = { Text(if (isProcessing && action == JsonAction.VALIDATE) "Working..." else action.label) },
                        )
                    }
                }
                if (isProcessing) {
                    Text(
                        text = "Parsing and formatting in the background...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                validationIssue?.let { issue ->
                    ValidationIssuePanel(issue = issue)
                }
            }

            ToolSection(title = "Output") {
                Text(
                    text = "Detected format: $detectedFormat",
                    style = MaterialTheme.typography.labelLarge,
                )
                notice?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (treeJson != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !showTree,
                            onClick = { showTree = false },
                            label = { Text("Text") },
                        )
                        FilterChip(
                            selected = showTree,
                            onClick = { showTree = true },
                            label = { Text("Tree") },
                        )
                    }
                }
                if (!showTree && pageCount > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                            enabled = currentPage > 0,
                            label = { Text("Previous") },
                        )
                        AssistChip(
                            onClick = { currentPage = (currentPage + 1).coerceAtMost(pageCount - 1) },
                            enabled = currentPage < pageCount - 1,
                            label = { Text("Next") },
                        )
                        Text(
                            text = "Page ${currentPage + 1} of $pageCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                ClipboardBar(
                    onPaste = {},
                    onCopy = { actions.copyText(output) },
                    onClear = {
                        output = ""
                        treeJson = null
                        currentPage = 0
                    },
                    onShare = { actions.shareText(output) },
                    showPaste = false,
                    showCopy = output.isNotBlank(),
                )
                if (output.isNotBlank()) {
                    AssistChip(
                        onClick = { exportLauncher.launch(exportFileName) },
                        label = { Text("Export file") },
                    )
                }
                if (showTree && treeJson != null) {
                    JsonTreePanel(treeJson = treeJson!!)
                } else {
                    OutputPanel(
                        content = if (output.isBlank()) "Run a formatter action to see the result." else pagedOutput,
                        label = if (pageCount > 1) "Output Preview" else "Output",
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationIssuePanel(issue: ValidationIssue) {
    val location = buildString {
        if (issue.line != null) {
            append("Line ${issue.line}")
            if (issue.column != null) append(", column ${issue.column}")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (location.isBlank()) issue.message else "$location: ${issue.message}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        issue.excerpt?.let { excerpt ->
            OutputPanel(
                content = excerpt,
                label = "Validation Marker",
            )
        }
    }
}

private fun processContent(input: String, action: JsonAction): JsonResult {
    val trimmed = input.trim()
    require(trimmed.isNotBlank()) { "Input is empty." }
    return when {
        looksLikeJson(trimmed) -> processJson(trimmed, action)
        looksLikeYaml(trimmed) -> processYaml(trimmed, action)
        looksLikeXml(trimmed) -> processXml(trimmed, action)
        else -> throw IllegalArgumentException("Unable to auto-detect JSON, YAML, or XML.")
    }
}

private fun processJson(input: String, action: JsonAction): JsonResult {
    val element = Json { isLenient = true }.parseToJsonElement(sanitizeJsonLike(input))
    val treeJson = Json.encodeToString(JsonElement.serializer(), element)
    val output = when (action) {
        JsonAction.PRETTY, JsonAction.TO_JSON, JsonAction.VALIDATE ->
            Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), element)
        JsonAction.MINIFY ->
            Json.encodeToString(JsonElement.serializer(), element)
        JsonAction.TO_YAML ->
            Yaml().dump(jsonToAny(element))
    }
    return finalizeResult(output = output, detectedFormat = "JSON", treeJson = treeJson, action = action)
}

private fun processYaml(input: String, action: JsonAction): JsonResult {
    val yamlValue = Yaml().load<Any?>(input)
    val element = anyToJsonElement(yamlValue)
    val treeJson = Json.encodeToString(JsonElement.serializer(), element)
    val output = when (action) {
        JsonAction.TO_YAML ->
            Yaml().dump(yamlValue)
        JsonAction.MINIFY ->
            Json.encodeToString(JsonElement.serializer(), element)
        JsonAction.PRETTY, JsonAction.TO_JSON, JsonAction.VALIDATE ->
            Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), element)
    }
    return finalizeResult(output = output, detectedFormat = "YAML", treeJson = treeJson, action = action)
}

private fun processXml(input: String, action: JsonAction): JsonResult {
    require(action != JsonAction.TO_JSON && action != JsonAction.TO_YAML) {
        "XML formatting is supported, but XML conversion is not included in this build."
    }
    val document = newSecureDocumentBuilderFactory().newDocumentBuilder().parse(input.byteInputStream())
    document.normalizeDocument()
    val transformer = TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.INDENT, if (action == JsonAction.MINIFY) "no" else "yes")
        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    }
    val writer = StringWriter()
    transformer.transform(DOMSource(document), StreamResult(writer))
    val output = if (action == JsonAction.MINIFY) {
        writer.toString().replace(Regex(">\\s+<"), "><").trim()
    } else {
        writer.toString().trim()
    }
    return finalizeResult(output = output, detectedFormat = "XML", treeJson = null, action = action)
}

private fun finalizeResult(
    output: String,
    detectedFormat: String,
    treeJson: String?,
    action: JsonAction,
): JsonResult {
    val notices = mutableListOf<String>()
    if (action == JsonAction.VALIDATE) {
        notices += "$detectedFormat validated successfully."
    }
    if (utf8Size(output) > JsonPaginationThresholdBytes) {
        notices += "Large payload detected. Text output is paginated to keep scrolling responsive."
    }
    val treeOutput = if (treeJson != null && utf8Size(treeJson) > JsonTreeMaxBytes) {
        notices += "Tree view is disabled for very large payloads."
        null
    } else {
        treeJson
    }
    return JsonResult(
        output = output,
        detectedFormat = detectedFormat,
        notice = notices.joinToString(" ").ifBlank { null },
        treeJson = treeOutput,
    )
}

private fun sanitizeJsonLike(input: String): String {
    return input
        .replace(Regex("(?s)/\\*.*?\\*/"), "")
        .replace(Regex("(?m)//.*$"), "")
        .replace(Regex(",(?=\\s*[}\\]])"), "")
}

private fun extractValidationIssue(input: String, throwable: Throwable): ValidationIssue? {
    val rootCause = generateSequence(throwable) { it.cause }.last()
    return when (rootCause) {
        is SAXParseException -> {
            buildValidationIssue(
                input = input,
                line = rootCause.lineNumber.takeIf { it > 0 },
                column = rootCause.columnNumber.takeIf { it > 0 },
                message = rootCause.message ?: "XML parsing error.",
            )
        }
        is MarkedYAMLException -> {
            val mark = rootCause.problemMark ?: rootCause.contextMark
            buildValidationIssue(
                input = input,
                line = mark?.line?.plus(1),
                column = mark?.column?.plus(1),
                message = rootCause.problem ?: rootCause.message ?: "YAML parsing error.",
            )
        }
        else -> {
            extractOffset(rootCause.message ?: throwable.message).let { offset ->
                val lineColumn = lineColumnForOffset(input, offset)
                buildValidationIssue(
                    input = input,
                    line = lineColumn?.first,
                    column = lineColumn?.second,
                    message = sanitizeValidationMessage(rootCause.message ?: throwable.message ?: "Parsing error."),
                )
            }
        }
    }
}

private fun buildValidationIssue(
    input: String,
    line: Int?,
    column: Int?,
    message: String,
): ValidationIssue {
    return ValidationIssue(
        message = sanitizeValidationMessage(message),
        line = line,
        column = column,
        excerpt = if (line != null) buildExcerpt(input, line, column) else null,
    )
}

private fun extractOffset(message: String?): Int? {
    if (message == null) return null
    return Regex("offset\\s+(\\d+)").find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

private fun lineColumnForOffset(input: String, offset: Int?): Pair<Int, Int>? {
    if (offset == null || offset < 0) return null
    var line = 1
    var column = 1
    input.take(offset.coerceAtMost(input.length)).forEach { character ->
        if (character == '\n') {
            line += 1
            column = 1
        } else {
            column += 1
        }
    }
    return line to column
}

private fun buildExcerpt(input: String, line: Int, column: Int?): String {
    val lines = input.lines()
    if (line !in 1..lines.size) return ""
    val startIndex = (line - 2).coerceAtLeast(0)
    val endIndex = (line).coerceAtMost(lines.lastIndex)
    val lineNumberWidth = (endIndex + 1).toString().length
    return buildString {
        for (index in startIndex..endIndex) {
            val lineNumber = index + 1
            append(lineNumber.toString().padStart(lineNumberWidth))
            append(" | ")
            appendLine(lines[index])
            if (lineNumber == line && column != null) {
                append(" ".repeat(lineNumberWidth))
                append(" | ")
                append(" ".repeat((column - 1).coerceAtLeast(0)))
                append('^')
                appendLine()
            }
        }
    }.trimEnd()
}

private fun sanitizeValidationMessage(message: String): String =
    message.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "Parsing error." }

private fun newSecureDocumentBuilderFactory(): DocumentBuilderFactory {
    return DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        setXIncludeAware(false)
        isExpandEntityReferences = false
    }
}

private fun outputPageCount(output: String): Int {
    if (output.isBlank() || utf8Size(output) <= JsonPaginationThresholdBytes) return 1
    return ((output.length - 1) / JsonPageChars) + 1
}

private fun outputPage(output: String, pageIndex: Int): String {
    if (output.isBlank()) return output
    if (utf8Size(output) <= JsonPaginationThresholdBytes) return output
    val start = (pageIndex.coerceAtLeast(0) * JsonPageChars).coerceAtMost(output.length)
    val end = (start + JsonPageChars).coerceAtMost(output.length)
    return output.substring(start, end)
}

private fun buildExportFileName(detectedFormat: String): String = when (detectedFormat) {
    "JSON" -> "devtoolkit-output.json"
    "YAML" -> "devtoolkit-output.yaml"
    "XML" -> "devtoolkit-output.xml"
    else -> "devtoolkit-output.txt"
}

private fun writeDocumentText(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(text.toByteArray())
        output.flush()
    } ?: error("Unable to open destination file.")
}

private fun utf8Size(text: String): Int = text.toByteArray(Charsets.UTF_8).size

private fun jsonToAny(element: JsonElement): Any? = when (element) {
    is JsonObject -> element.mapValues { (_, value) -> jsonToAny(value) }
    is JsonArray -> element.map(::jsonToAny)
    is JsonNull -> null
    is JsonPrimitive -> when {
        element.isString -> element.content
        element.booleanOrNull != null -> element.content.toBooleanStrict()
        element.longOrNull != null -> element.content.toLong()
        element.doubleOrNull != null -> element.content.toDouble()
        else -> element.content
    }
}

private fun anyToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is Map<*, *> -> buildJsonObject {
        value.forEach { (key, item) ->
            if (key != null) put(key.toString(), anyToJsonElement(item))
        }
    }
    is Iterable<*> -> buildJsonArray {
        value.forEach { add(anyToJsonElement(it)) }
    }
    is Number -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    else -> JsonPrimitive(value.toString())
}

@Composable
private fun JsonTreePanel(treeJson: String) {
    val element = remember(treeJson) {
        runCatching { Json.parseToJsonElement(treeJson) }.getOrNull()
    }
    if (element == null) {
        Text(
            text = "Tree view is unavailable for this payload.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        JsonTreeNode(
            label = null,
            element = element,
            path = "$",
            depth = 0,
        )
    }
}

@Composable
private fun JsonTreeNode(
    label: String?,
    element: JsonElement,
    path: String,
    depth: Int,
) {
    when (element) {
        is JsonObject -> {
            val itemLabel = label?.let { "\"$it\": " }.orEmpty() + "{${element.size} keys}"
            ExpandableJsonNode(
                label = itemLabel,
                path = path,
                depth = depth,
            ) {
                element.forEach { (key, value) ->
                    JsonTreeNode(
                        label = key,
                        element = value,
                        path = "$path.$key",
                        depth = depth + 1,
                    )
                }
            }
        }
        is JsonArray -> {
            val itemLabel = label?.let { "\"$it\": " }.orEmpty() + "[${element.size} items]"
            ExpandableJsonNode(
                label = itemLabel,
                path = path,
                depth = depth,
            ) {
                element.forEachIndexed { index, value ->
                    JsonTreeNode(
                        label = "[$index]",
                        element = value,
                        path = "$path[$index]",
                        depth = depth + 1,
                    )
                }
            }
        }
        else -> {
            Text(
                text = buildString {
                    if (label != null) append("\"$label\": ")
                    append(element.toString())
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = (depth * 14).dp, top = 2.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun ExpandableJsonNode(
    label: String,
    path: String,
    depth: Int,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(path) { mutableStateOf(depth < 1) }
    Text(
        text = "${if (expanded) "\u25BC" else "\u25B6"} $label",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp, top = 2.dp, bottom = 2.dp),
    )
    AssistChip(
        onClick = { expanded = !expanded },
        label = { Text(if (expanded) "Collapse" else "Expand") },
        modifier = Modifier.padding(start = (depth * 14).dp),
    )
    if (expanded) {
        content()
    }
}
