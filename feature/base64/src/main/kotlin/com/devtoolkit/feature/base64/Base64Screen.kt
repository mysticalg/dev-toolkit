package com.devtoolkit.feature.base64

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devtoolkit.core.domain.looksLikeBase64
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
import java.util.Base64

private const val Base64FileMaxBytes = 2 * 1024 * 1024L
private const val Base64PreviewChars = 200_000

private data class DocumentInfo(
    val name: String,
    val sizeBytes: Long?,
    val mimeType: String?,
)

@Composable
fun Base64Screen(
    sharedText: String? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = rememberToolActions("base64")

    var input by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    var fullOutput by rememberSaveable { mutableStateOf("") }
    var outputPreview by rememberSaveable { mutableStateOf("") }
    var outputNotice by rememberSaveable { mutableStateOf<String?>(null) }
    var urlSafe by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("Paste text or pick a small file to get started.") }
    var decodedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileInfo by remember { mutableStateOf<DocumentInfo?>(null) }
    var pendingSaveName by remember { mutableStateOf("decoded.bin") }

    fun setOutput(value: String, notice: String? = null) {
        fullOutput = value
        if (value.length > Base64PreviewChars) {
            outputPreview = value.take(Base64PreviewChars)
            outputNotice = notice ?: "Preview truncated to $Base64PreviewChars characters."
        } else {
            outputPreview = value
            outputNotice = notice
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileInfo = queryDocumentInfo(context, uri)
            status = buildString {
                append("Selected ${selectedFileInfo?.name ?: "file"}")
                selectedFileInfo?.sizeBytes?.let { append(" (${formatBytes(it)})") }
            }
        }
    }
    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val bytes = decodedBytes
        if (uri != null && bytes != null) {
            scope.launch {
                runCatching { writeDocumentBytes(context, uri, bytes) }
                    .onSuccess { status = "Saved decoded bytes to a file." }
                    .onFailure { status = it.message ?: "Unable to save decoded file." }
            }
        }
    }

    LaunchedEffect(sharedText) {
        if (input.isBlank() && !sharedText.isNullOrBlank()) {
            input = sharedText
        }
    }

    LaunchedEffect(input, urlSafe) {
        if (input.isNotBlank() && looksLikeBase64(input.filterNot(Char::isWhitespace))) {
            status = "Input looks like Base64."
        }
    }

    fun encodeText() {
        if (input.isBlank()) {
            setOutput("")
            status = "Paste text to encode."
            return
        }

        val encoder = if (urlSafe) Base64.getUrlEncoder() else Base64.getEncoder()
        val encoded = encoder.encodeToString(input.toByteArray())
        setOutput(encoded)
        decodedBytes = null
        status = "Encoded ${input.length} characters."
        actions.saveHistory(input, outputPreview)
    }

    fun decodeText() {
        val compact = input.filterNot(Char::isWhitespace)
        if (compact.isBlank()) {
            setOutput("")
            decodedBytes = null
            status = "Paste Base64 text to decode."
            return
        }

        runCatching {
            val decoder = if (urlSafe) Base64.getUrlDecoder() else Base64.getDecoder()
            decoder.decode(compact)
        }.onSuccess { bytes ->
            decodedBytes = bytes
            pendingSaveName = guessFileName(bytes)
            setOutput(bytes.toString(Charsets.UTF_8))
            status = "Decoded ${formatBytes(bytes.size.toLong())}."
            actions.saveHistory(input, outputPreview)
        }.onFailure { error ->
            decodedBytes = null
            setOutput("")
            status = error.message ?: "Unable to decode Base64."
        }
    }

    fun encodeSelectedFile() {
        val uri = selectedFileUri ?: run {
            status = "Pick a file to encode."
            return
        }
        val info = selectedFileInfo
        scope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) { readDocumentBytes(context, uri, Base64FileMaxBytes) }
                val encoder = if (urlSafe) Base64.getUrlEncoder() else Base64.getEncoder()
                info to encoder.encodeToString(bytes)
            }.onSuccess { (fileInfo, encoded) ->
                decodedBytes = null
                setOutput(encoded)
                status = "Encoded ${fileInfo?.name ?: "file"} to Base64."
                actions.saveHistory("FILE:${fileInfo?.name ?: uri}", outputPreview)
            }.onFailure { error ->
                setOutput("")
                status = error.message ?: "Unable to encode file."
            }
        }
    }

    val previewBitmap = remember(decodedBytes) {
        decodedBytes?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
    }

    ToolScaffold(title = "Base64", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Input") {
                ClipboardBar(
                    onPaste = { input = actions.readClipboard() },
                    onCopy = { actions.copyText(input) },
                    onClear = {
                        input = ""
                        setOutput("")
                        decodedBytes = null
                        selectedFileUri = null
                        selectedFileInfo = null
                        status = "Paste text or pick a small file to get started."
                    },
                )
                CodeEditor(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "Paste plain text or Base64 here",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = urlSafe,
                        onClick = { urlSafe = !urlSafe },
                        label = { Text("URL-safe mode") },
                    )
                    AssistChip(onClick = ::encodeText, label = { Text("Encode text") })
                    AssistChip(onClick = ::decodeText, label = { Text("Decode text") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { pickFileLauncher.launch(arrayOf("*/*")) },
                        label = { Text("Pick file") },
                    )
                    AssistChip(
                        onClick = ::encodeSelectedFile,
                        enabled = selectedFileUri != null,
                        label = { Text("Encode file") },
                    )
                    if (decodedBytes != null) {
                        AssistChip(
                            onClick = { saveFileLauncher.launch(pendingSaveName) },
                            label = { Text("Save decoded file") },
                        )
                    }
                }
                selectedFileInfo?.let { info ->
                    Text(
                        text = formatDocumentInfo(info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ToolSection(title = "Output") {
                outputNotice?.let { notice ->
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ClipboardBar(
                    onPaste = {},
                    onCopy = { actions.copyText(fullOutput) },
                    onClear = { setOutput("") },
                    onShare = { actions.shareText(fullOutput) },
                    showPaste = false,
                )
                OutputPanel(content = outputPreview)
                if (previewBitmap != null) {
                    Text(
                        text = "Decoded image preview",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Decoded Base64 image preview",
                    )
                }
            }
        }
    }
}

private fun formatDocumentInfo(info: DocumentInfo): String {
    return buildString {
        append("Selected file: ${info.name}")
        info.sizeBytes?.let { append(" | ${formatBytes(it)}") }
        info.mimeType?.let { append(" | $it") }
    }
}

private fun queryDocumentInfo(context: Context, uri: Uri): DocumentInfo {
    var name = uri.lastPathSegment ?: "selected-file"
    var size: Long? = null
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    return DocumentInfo(
        name = name,
        sizeBytes = size,
        mimeType = context.contentResolver.getType(uri),
    )
}

private fun readDocumentBytes(context: Context, uri: Uri, maxBytes: Long): ByteArray {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(16 * 1024)
        val output = java.io.ByteArrayOutputStream()
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            require(total <= maxBytes) {
                "File is too large for Base64 mode. Limit is ${formatBytes(maxBytes)}."
            }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    } ?: error("Unable to open selected file.")
}

private fun writeDocumentBytes(context: Context, uri: Uri, bytes: ByteArray) {
    context.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(bytes)
        output.flush()
    } ?: error("Unable to open destination file.")
}

private fun guessFileName(bytes: ByteArray): String = when {
    bytes.startsWith(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) -> "decoded.png"
    bytes.startsWith(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) -> "decoded.jpg"
    bytes.startsWith("GIF8".encodeToByteArray()) -> "decoded.gif"
    bytes.startsWith("%PDF".encodeToByteArray()) -> "decoded.pdf"
    else -> "decoded.bin"
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    return prefix.indices.all { index -> this[index] == prefix[index] }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "${"%.1f".format(bytes / 1024f / 1024f)} MB"
    bytes >= 1024 -> "${"%.1f".format(bytes / 1024f)} KB"
    else -> "$bytes B"
}
