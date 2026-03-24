package com.devtoolkit.feature.uuid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
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
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.OutputPanel
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import java.security.SecureRandom
import java.util.UUID

private enum class GeneratorType(val label: String) {
    UUID_V4("UUID v4"),
    UUID_V7("UUID v7"),
    ULID("ULID"),
    NANO_ID("Nano ID"),
}

private val secureRandom = SecureRandom()
private const val crockford = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

@Composable
fun UuidScreen(
    sharedText: String? = null,
    onBack: () -> Unit,
) {
    val actions = rememberToolActions("uuid")
    var selectedType by rememberSaveable { mutableStateOf(GeneratorType.UUID_V4) }
    var countInput by rememberSaveable { mutableStateOf("1") }
    var nanoLength by rememberSaveable { mutableStateOf("21") }
    var nanoAlphabet by rememberSaveable { mutableStateOf("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_") }
    var validatorInput by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    var output by rememberSaveable { mutableStateOf("") }
    var validationStatus by rememberSaveable { mutableStateOf("Generate IDs or paste one to validate.") }

    LaunchedEffect(sharedText) {
        if (validatorInput.isBlank() && !sharedText.isNullOrBlank()) {
            validatorInput = sharedText
        }
    }

    fun generate() {
        val count = countInput.toIntOrNull()?.coerceIn(1, 100) ?: 1
        output = (1..count).joinToString("\n") {
            when (selectedType) {
                GeneratorType.UUID_V4 -> UUID.randomUUID().toString()
                GeneratorType.UUID_V7 -> generateUuidV7()
                GeneratorType.ULID -> generateUlid()
                GeneratorType.NANO_ID -> generateNanoId(nanoLength.toIntOrNull()?.coerceAtLeast(1) ?: 21, nanoAlphabet)
            }
        }
        actions.saveHistory(selectedType.label, output)
    }

    fun validate() {
        validationStatus = describeIdentifier(validatorInput)
    }

    ToolScaffold(title = "UUID Generator", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Generator") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GeneratorType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = countInput,
                    onValueChange = { countInput = it.filter(Char::isDigit) },
                    label = { Text("Batch count") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (selectedType == GeneratorType.NANO_ID) {
                    OutlinedTextField(
                        value = nanoLength,
                        onValueChange = { nanoLength = it.filter(Char::isDigit) },
                        label = { Text("Nano ID length") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = nanoAlphabet,
                        onValueChange = { nanoAlphabet = it },
                        label = { Text("Nano ID alphabet") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AssistChip(onClick = ::generate, label = { Text("Generate") })
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

            ToolSection(title = "Validator") {
                ClipboardBar(
                    onPaste = { validatorInput = actions.readClipboard() },
                    onCopy = { actions.copyText(validatorInput) },
                    onClear = { validatorInput = "" },
                )
                OutlinedTextField(
                    value = validatorInput,
                    onValueChange = { validatorInput = it },
                    label = { Text("Validate / detect version") },
                    modifier = Modifier.fillMaxWidth(),
                )
                AssistChip(onClick = ::validate, label = { Text("Validate") })
                Text(
                    text = validationStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun generateUuidV7(): String {
    val bytes = ByteArray(16)
    secureRandom.nextBytes(bytes)
    val timestamp = System.currentTimeMillis()
    bytes[0] = (timestamp shr 40).toByte()
    bytes[1] = (timestamp shr 32).toByte()
    bytes[2] = (timestamp shr 24).toByte()
    bytes[3] = (timestamp shr 16).toByte()
    bytes[4] = (timestamp shr 8).toByte()
    bytes[5] = timestamp.toByte()
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
    val msb = bytes.copyOfRange(0, 8).fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
    val lsb = bytes.copyOfRange(8, 16).fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
    return UUID(msb, lsb).toString()
}

private fun generateUlid(): String {
    val timestamp = System.currentTimeMillis()
    val randomBytes = ByteArray(10).also(secureRandom::nextBytes)
    val chars = CharArray(26)
    var time = timestamp
    for (index in 9 downTo 0) {
        chars[index] = crockford[(time and 31).toInt()]
        time = time shr 5
    }
    var buffer = 0
    var bits = 0
    var outputIndex = 10
    for (byte in randomBytes) {
        buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
        bits += 8
        while (bits >= 5 && outputIndex < chars.size) {
            bits -= 5
            chars[outputIndex++] = crockford[(buffer shr bits) and 31]
        }
    }
    while (outputIndex < chars.size) {
        chars[outputIndex++] = crockford[secureRandom.nextInt(crockford.length)]
    }
    return chars.concatToString()
}

private fun generateNanoId(length: Int, alphabet: String): String {
    val safeAlphabet = alphabet.ifBlank { "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_" }
    return buildString {
        repeat(length) {
            append(safeAlphabet[secureRandom.nextInt(safeAlphabet.length)])
        }
    }
}

private fun describeIdentifier(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return "No identifier provided."
    return runCatching {
        val uuid = UUID.fromString(trimmed)
        "Valid UUID version ${uuid.version()}"
    }.getOrElse {
        when {
            trimmed.length == 26 && trimmed.all { it in crockford } -> "Looks like a ULID"
            trimmed.length >= 6 -> "Looks like a Nano ID or custom identifier"
            else -> "Identifier format not recognized"
        }
    }
}
