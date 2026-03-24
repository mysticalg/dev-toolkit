package com.devtoolkit.feature.jwt

import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.devtoolkit.core.domain.looksLikeJwt
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.CodeEditor
import com.devtoolkit.core.ui.OutputPanel
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

private data class JwtResult(
    val header: String,
    val payload: String,
    val claims: JsonObject,
    val validityText: String,
    val isExpired: Boolean,
)

@Composable
fun JwtScreen(
    sharedText: String? = null,
    onBack: () -> Unit,
) {
    val actions = rememberToolActions("jwt")
    var input by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    var headerOutput by rememberSaveable { mutableStateOf("") }
    var payloadOutput by rememberSaveable { mutableStateOf("") }
    var validity by rememberSaveable { mutableStateOf("Paste a JWT to decode its header and payload.") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var decodedClaims by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var expired by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(sharedText) {
        if (input.isBlank() && !sharedText.isNullOrBlank()) {
            input = sharedText
        }
    }

    LaunchedEffect(Unit) {
        val clipboard = actions.readClipboard()
        if (input.isBlank() && looksLikeJwt(clipboard)) {
            input = clipboard
        }
    }

    fun decode() {
        runCatching { decodeJwt(input) }
            .onSuccess { result ->
                headerOutput = result.header
                payloadOutput = result.payload
                validity = result.validityText
                expired = result.isExpired
                error = null
                decodedClaims = result.claims.entries.associate { (key, value) -> key to value.toString() }
                actions.saveHistory(input, "${result.header}\n\n${result.payload}")
            }
            .onFailure { throwable ->
                error = throwable.message ?: "Unable to decode JWT"
                headerOutput = ""
                payloadOutput = ""
                decodedClaims = emptyMap()
            }
    }

    ToolScaffold(title = "JWT Decoder", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Token") {
                ClipboardBar(
                    onPaste = { input = actions.readClipboard() },
                    onCopy = { actions.copyText(input) },
                    onClear = {
                        input = ""
                        headerOutput = ""
                        payloadOutput = ""
                    },
                )
                CodeEditor(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = "Paste a JWT here",
                )
                AssistChip(onClick = ::decode, label = { Text("Decode") })
                Text(
                    text = validity,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Signature verification is intentionally not performed offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            ToolSection(title = "Header") {
                ClipboardBar(
                    onPaste = {},
                    onCopy = { actions.copyText(headerOutput) },
                    onClear = { headerOutput = "" },
                    showPaste = false,
                )
                OutputPanel(content = headerOutput)
            }

            ToolSection(title = "Payload") {
                ClipboardBar(
                    onPaste = {},
                    onCopy = { actions.copyText(payloadOutput) },
                    onClear = { payloadOutput = "" },
                    onShare = { actions.shareText(payloadOutput) },
                    showPaste = false,
                )
                OutputPanel(content = payloadOutput)
                if (decodedClaims.isNotEmpty()) {
                    Text(
                        text = "Claim notes",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    decodedClaims.forEach { (key, value) ->
                        val explanation = claimNotes[key]
                        Text(
                            text = buildString {
                                append("$key = $value")
                                if (explanation != null) append("  $explanation")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun decodeJwt(token: String): JwtResult {
    require(looksLikeJwt(token.trim())) { "Input is not shaped like a JWT" }
    val parts = token.trim().split(".")
    val headerJson = decodeBase64Url(parts[0])
    val payloadJson = decodeBase64Url(parts[1])
    val headerElement = Json.parseToJsonElement(headerJson)
    val payloadElement = Json.parseToJsonElement(payloadJson)
    val claims = payloadElement.jsonObject
    val exp = claims["exp"]?.jsonPrimitive?.longOrNull
    val now = Instant.now()
    val validityText = if (exp != null) {
        val expiresAt = Instant.ofEpochSecond(exp)
        val remaining = Duration.between(now, expiresAt)
        val humanTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(expiresAt.atZone(ZoneId.systemDefault()))
        if (remaining.isNegative) {
            "Expired ${remaining.abs().toHours()}h ago at $humanTime"
        } else {
            "Valid for ${remaining.toHours()}h until $humanTime"
        }
    } else {
        "No exp claim present."
    }
    return JwtResult(
        header = Json { prettyPrint = true }.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), headerElement),
        payload = Json { prettyPrint = true }.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), payloadElement),
        claims = claims,
        validityText = validityText,
        isExpired = exp?.let { Instant.ofEpochSecond(it).isBefore(now) } ?: false,
    )
}

private fun decodeBase64Url(value: String): String {
    val padded = value + "=".repeat((4 - value.length % 4) % 4)
    return String(Base64.getUrlDecoder().decode(padded))
}

private val claimNotes = mapOf(
    "iat" to "Issued at time.",
    "exp" to "Expiry time.",
    "nbf" to "Not valid before this time.",
    "iss" to "Issuer identifier.",
    "sub" to "Subject identifier.",
    "aud" to "Audience this token targets.",
)
