package com.devtoolkit.feature.epoch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.devtoolkit.core.domain.looksLikeEpoch
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.OutputPanel
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun EpochScreen(
    sharedText: String? = null,
    onBack: () -> Unit,
) {
    val actions = rememberToolActions("epoch")
    var epochInput by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    var dateInput by rememberSaveable { mutableStateOf("") }
    var zoneSearch by rememberSaveable { mutableStateOf("") }
    var selectedZone by rememberSaveable { mutableStateOf(ZoneId.systemDefault().id) }
    var customFormat by rememberSaveable { mutableStateOf("yyyy-MM-dd HH:mm:ss") }
    var output by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("Convert Unix timestamps and human-readable dates.") }

    val liveEpoch by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    val filteredZones = ZoneId.getAvailableZoneIds()
        .sorted()
        .filter { it.contains(zoneSearch, ignoreCase = true) }
        .take(8)

    LaunchedEffect(sharedText) {
        if (epochInput.isBlank() && !sharedText.isNullOrBlank() && looksLikeEpoch(sharedText)) {
            epochInput = sharedText
        }
    }

    fun convertEpoch() {
        runCatching {
            val raw = epochInput.trim().toLong()
            val instant = if (epochInput.trim().length > 10) Instant.ofEpochMilli(raw) else Instant.ofEpochSecond(raw)
            val zoned = instant.atZone(ZoneId.of(selectedZone))
            dateInput = zoned.toLocalDateTime().toString()
            val custom = runCatching { zoned.format(DateTimeFormatter.ofPattern(customFormat)) }.getOrDefault("Invalid custom format")
            output = buildString {
                appendLine("Seconds: ${instant.epochSecond}")
                appendLine("Milliseconds: ${instant.toEpochMilli()}")
                appendLine("ISO 8601: ${DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zoned)}")
                appendLine("RFC 2822: ${DateTimeFormatter.RFC_1123_DATE_TIME.format(zoned)}")
                appendLine("Custom: $custom")
                appendLine("Relative: ${relativeTime(instant)}")
                appendLine("Zone: $selectedZone")
            }.trim()
            status = "Converted epoch to date/time."
            actions.saveHistory(epochInput, output)
        }.onFailure {
            status = it.message ?: "Unable to parse epoch value"
            output = ""
        }
    }

    fun convertDate() {
        try {
            val dateTime = LocalDateTime.parse(dateInput)
            val zoned = dateTime.atZone(ZoneId.of(selectedZone))
            epochInput = zoned.toEpochSecond().toString()
            output = buildString {
                appendLine("Seconds: ${zoned.toEpochSecond()}")
                appendLine("Milliseconds: ${zoned.toInstant().toEpochMilli()}")
                appendLine("Relative: ${relativeTime(zoned.toInstant())}")
            }.trim()
            status = "Converted date/time to epoch."
            actions.saveHistory(dateInput, output)
        } catch (_: DateTimeParseException) {
            status = "Use ISO local date-time format, e.g. 2026-03-24T18:45:00"
        }
    }

    ToolScaffold(title = "Epoch Converter", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Timestamp") {
                ClipboardBar(
                    onPaste = { epochInput = actions.readClipboard() },
                    onCopy = { actions.copyText(epochInput) },
                    onClear = { epochInput = "" },
                )
                OutlinedTextField(
                    value = epochInput,
                    onValueChange = { epochInput = it },
                    label = { Text("Unix timestamp") },
                    modifier = Modifier.fillMaxWidth(),
                )
                AssistChip(
                    onClick = { epochInput = liveEpoch.toString() },
                    label = { Text("Now: $liveEpoch") },
                )
                AssistChip(onClick = ::convertEpoch, label = { Text("Convert timestamp") })
            }

            ToolSection(title = "Date & Time") {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Date/time (ISO local)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = customFormat,
                    onValueChange = { customFormat = it },
                    label = { Text("Custom format") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = zoneSearch,
                    onValueChange = { zoneSearch = it },
                    label = { Text("Search timezone") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Selected zone: $selectedZone",
                    style = MaterialTheme.typography.labelLarge,
                )
                filteredZones.forEach { zone ->
                    AssistChip(
                        onClick = { selectedZone = zone },
                        label = { Text(zone) },
                    )
                }
                AssistChip(onClick = ::convertDate, label = { Text("Convert date") })
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

private fun relativeTime(instant: Instant): String {
    val duration = Duration.between(instant, Instant.now())
    val hours = kotlin.math.abs(duration.toHours())
    return if (duration.isNegative) {
        "in ${hours}h"
    } else {
        "${hours}h ago"
    }
}
