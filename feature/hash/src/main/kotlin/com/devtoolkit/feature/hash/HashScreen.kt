package com.devtoolkit.feature.hash

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
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
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.CodeEditor
import com.devtoolkit.core.ui.LocalEditorSettings
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HashFileMaxBytes = 50L * 1024L * 1024L

private val DigestAlgorithms = listOf("MD5", "SHA-1", "SHA-256", "SHA-512")
private val HmacAlgorithms = listOf(
    "HMAC-MD5" to "HmacMD5",
    "HMAC-SHA1" to "HmacSHA1",
    "HMAC-SHA256" to "HmacSHA256",
    "HMAC-SHA512" to "HmacSHA512",
)

private data class HashResult(
    val algorithm: String,
    val value: String,
)

private data class DocumentInfo(
    val name: String,
    val sizeBytes: Long?,
    val mimeType: String?,
)

private enum class HashInputMode {
    Text,
    File,
}

private enum class StatusLevel {
    Neutral,
    Success,
    Error,
}

@Composable
fun HashScreen(
    sharedText: String? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = rememberToolActions("hash")
    val editorSettings = LocalEditorSettings.current

    var inputModeName by rememberSaveable { mutableStateOf(HashInputMode.Text.name) }
    val inputMode = HashInputMode.valueOf(inputModeName)
    var input by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    var useHmac by rememberSaveable { mutableStateOf(false) }
    var secretKey by rememberSaveable { mutableStateOf("") }
    var expectedHash by rememberSaveable { mutableStateOf("") }
    var statusMessage by rememberSaveable { mutableStateOf("Generate hashes to compare values.") }
    var statusLevelName by rememberSaveable { mutableStateOf(StatusLevel.Neutral.name) }
    var isWorking by rememberSaveable { mutableStateOf(false) }
    var results by remember { mutableStateOf(emptyList<HashResult>()) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileInfo by remember { mutableStateOf<DocumentInfo?>(null) }

    fun setStatus(message: String, level: StatusLevel = StatusLevel.Neutral) {
        statusMessage = message
        statusLevelName = level.name
    }

    fun applyResults(sourceLabel: String, computedResults: List<HashResult>) {
        results = computedResults
        val normalizedExpected = expectedHash.trim().lowercase()
        if (normalizedExpected.isBlank()) {
            setStatus("Generated ${computedResults.size} hashes.", StatusLevel.Success)
        } else {
            val match = computedResults.firstOrNull { result ->
                result.value.equals(normalizedExpected, ignoreCase = true)
            }
            if (match != null) {
                setStatus("Match: ${match.algorithm}", StatusLevel.Success)
            } else {
                setStatus("No generated hash matched the expected value.", StatusLevel.Error)
            }
        }
        actions.saveHistory(sourceLabel, formatResults(computedResults))
    }

    fun generateTextResults() {
        if (input.isBlank()) {
            results = emptyList()
            setStatus("Paste text to hash.", StatusLevel.Error)
            return
        }

        runCatching {
            generateTextHashes(
                value = input,
                useHmac = useHmac,
                secretKey = secretKey,
            )
        }.onSuccess { computedResults ->
            applyResults(sourceLabel = input, computedResults = computedResults)
        }.onFailure { error ->
            results = emptyList()
            setStatus(error.message ?: "Unable to generate hashes.", StatusLevel.Error)
        }
    }

    fun generateFileResults() {
        val uri = selectedFileUri
        val info = selectedFileInfo
        if (uri == null) {
            results = emptyList()
            setStatus("Pick a file to hash.", StatusLevel.Error)
            return
        }

        isWorking = true
        setStatus("Hashing ${info?.name ?: "selected file"}...", StatusLevel.Neutral)
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    generateFileHashes(
                        context = context,
                        uri = uri,
                        useHmac = useHmac,
                        secretKey = secretKey,
                        maxBytes = HashFileMaxBytes,
                    )
                }
            }.onSuccess { computedResults ->
                applyResults(
                    sourceLabel = "FILE:${info?.name ?: uri}",
                    computedResults = computedResults,
                )
            }.onFailure { error ->
                results = emptyList()
                setStatus(error.message ?: "Unable to hash file.", StatusLevel.Error)
            }
            isWorking = false
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            inputModeName = HashInputMode.File.name
            selectedFileUri = uri
            selectedFileInfo = queryDocumentInfo(context, uri)
            results = emptyList()
            setStatus(
                buildString {
                    append("Selected ${selectedFileInfo?.name ?: "file"}")
                    selectedFileInfo?.sizeBytes?.let { append(" (${formatBytes(it)})") }
                },
            )
        }
    }

    LaunchedEffect(sharedText) {
        if (input.isBlank() && !sharedText.isNullOrBlank()) {
            input = sharedText
        }
    }

    val statusLevel = StatusLevel.valueOf(statusLevelName)
    val statusColor = when {
        isWorking -> MaterialTheme.colorScheme.primary
        statusLevel == StatusLevel.Success -> MaterialTheme.colorScheme.primary
        statusLevel == StatusLevel.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val output = remember(results) { formatResults(results) }

    ToolScaffold(title = "Hash Generator", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Input") {
                ClipboardBar(
                    onPaste = {
                        inputModeName = HashInputMode.Text.name
                        input = actions.readClipboard()
                    },
                    onCopy = { actions.copyText(input) },
                    onClear = {
                        input = ""
                        results = emptyList()
                        selectedFileUri = null
                        selectedFileInfo = null
                        setStatus("Generate hashes to compare values.")
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = inputMode == HashInputMode.Text,
                        onClick = { inputModeName = HashInputMode.Text.name },
                        label = { Text("Text") },
                    )
                    FilterChip(
                        selected = inputMode == HashInputMode.File,
                        onClick = { inputModeName = HashInputMode.File.name },
                        label = { Text("File") },
                    )
                }
                if (inputMode == HashInputMode.Text) {
                    CodeEditor(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "Paste text to hash",
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { pickFileLauncher.launch(arrayOf("*/*")) },
                            label = { Text("Pick file") },
                        )
                        AssistChip(
                            onClick = ::generateFileResults,
                            enabled = selectedFileUri != null && !isWorking,
                            label = { Text(if (isWorking) "Hashing..." else "Hash file") },
                        )
                    }
                    selectedFileInfo?.let { info ->
                        Text(
                            text = buildString {
                                append("Selected file: ${info.name}")
                                info.sizeBytes?.let { append(" | ${formatBytes(it)}") }
                                info.mimeType?.let { append(" | $it") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = useHmac,
                        onClick = { useHmac = !useHmac },
                        label = { Text("HMAC mode") },
                    )
                    AssistChip(
                        onClick = {
                            if (inputMode == HashInputMode.Text) {
                                generateTextResults()
                            } else {
                                generateFileResults()
                            }
                        },
                        enabled = !isWorking,
                        label = { Text(if (isWorking) "Working..." else "Generate") },
                    )
                }
                if (useHmac) {
                    OutlinedTextField(
                        value = secretKey,
                        onValueChange = { secretKey = it },
                        label = { Text("Secret key") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = expectedHash,
                    onValueChange = { expectedHash = it },
                    label = { Text("Expected hash (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }

            ToolSection(title = "Hashes") {
                ClipboardBar(
                    onPaste = {},
                    onCopy = { actions.copyText(output) },
                    onClear = { results = emptyList() },
                    onShare = { actions.shareText(output) },
                    showPaste = false,
                )
                if (results.isEmpty()) {
                    Text(
                        text = "No hashes generated yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        results.forEach { result ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = result.algorithm,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        AssistChip(
                                            onClick = { actions.copyText(result.value) },
                                            label = { Text("Copy") },
                                        )
                                    }
                                    Text(
                                        text = result.value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = editorSettings.fontFamily,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateTextHashes(
    value: String,
    useHmac: Boolean,
    secretKey: String,
): List<HashResult> {
    validateSecretKey(useHmac, secretKey)
    return buildList {
        DigestAlgorithms.forEach { algorithm ->
            add(HashResult(algorithm = algorithm, value = digest(algorithm, value.toByteArray())))
        }
        if (useHmac) {
            HmacAlgorithms.forEach { (label, algorithm) ->
                add(HashResult(algorithm = label, value = hmac(algorithm, value.toByteArray(), secretKey)))
            }
        }
    }
}

private fun generateFileHashes(
    context: Context,
    uri: Uri,
    useHmac: Boolean,
    secretKey: String,
    maxBytes: Long,
): List<HashResult> {
    validateSecretKey(useHmac, secretKey)

    val digests = DigestAlgorithms.associateWith { algorithm -> MessageDigest.getInstance(algorithm) }
    val macs = if (useHmac) {
        HmacAlgorithms.associate { (label, algorithm) ->
            label to Mac.getInstance(algorithm).apply {
                init(SecretKeySpec(secretKey.toByteArray(), algorithm))
            }
        }
    } else {
        emptyMap()
    }

    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = ByteArray(64 * 1024)
        var totalRead = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            totalRead += read
            require(totalRead <= maxBytes) {
                "File is too large for hashing. Limit is ${formatBytes(maxBytes)}."
            }
            digests.values.forEach { digest -> digest.update(buffer, 0, read) }
            macs.values.forEach { mac -> mac.update(buffer, 0, read) }
        }
    } ?: error("Unable to open selected file.")

    return buildList {
        DigestAlgorithms.forEach { algorithm ->
            add(HashResult(algorithm = algorithm, value = digests.getValue(algorithm).digest().toHexString()))
        }
        if (useHmac) {
            HmacAlgorithms.forEach { (label, _) ->
                add(HashResult(algorithm = label, value = macs.getValue(label).doFinal().toHexString()))
            }
        }
    }
}

private fun validateSecretKey(useHmac: Boolean, secretKey: String) {
    if (useHmac) {
        require(secretKey.isNotBlank()) { "Enter a secret key for HMAC mode." }
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

private fun formatResults(results: List<HashResult>): String {
    return results.joinToString(separator = "\n") { result -> "${result.algorithm}: ${result.value}" }
}

private fun digest(algorithm: String, value: ByteArray): String {
    return MessageDigest.getInstance(algorithm).digest(value).toHexString()
}

private fun hmac(algorithm: String, value: ByteArray, secret: String): String {
    val mac = Mac.getInstance(algorithm)
    mac.init(SecretKeySpec(secret.toByteArray(), algorithm))
    return mac.doFinal(value).toHexString()
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "${"%.1f".format(bytes / 1024f / 1024f)} MB"
    bytes >= 1024 -> "${"%.1f".format(bytes / 1024f)} KB"
    else -> "$bytes B"
}
