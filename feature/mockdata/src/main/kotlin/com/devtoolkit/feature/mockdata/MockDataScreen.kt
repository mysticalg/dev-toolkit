package com.devtoolkit.feature.mockdata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private enum class MockMode(val label: String) {
    LOREM("Lorem"),
    PEOPLE("People"),
    JSON("JSON"),
}

private val random = SecureRandom()

@Composable
fun MockDataScreen(onBack: () -> Unit) {
    val actions = rememberToolActions("mockdata")
    var mode by rememberSaveable { mutableStateOf(MockMode.LOREM) }
    var locale by rememberSaveable { mutableStateOf("en_GB") }
    var countInput by rememberSaveable { mutableStateOf("3") }
    var schema by rememberSaveable { mutableStateOf("id:uuid,name:name,email:email") }
    var csvOutput by rememberSaveable { mutableStateOf(false) }
    var output by rememberSaveable { mutableStateOf("") }

    fun generate() {
        val count = countInput.toIntOrNull()?.coerceIn(1, 1000) ?: 1
        output = when (mode) {
            MockMode.LOREM -> generateLorem(count)
            MockMode.PEOPLE -> generatePeople(count, locale, csvOutput)
            MockMode.JSON -> generateJsonSchema(count, schema)
        }
        actions.saveHistory("${mode.label} x$count", output)
    }

    ToolScaffold(title = "Mock Data", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Generator") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MockMode.entries.forEach { item ->
                        FilterChip(
                            selected = mode == item,
                            onClick = { mode = item },
                            label = { Text(item.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = locale == "en_GB",
                        onClick = { locale = "en_GB" },
                        label = { Text("en_GB") },
                    )
                    FilterChip(
                        selected = locale == "en_US",
                        onClick = { locale = "en_US" },
                        label = { Text("en_US") },
                    )
                    FilterChip(
                        selected = csvOutput,
                        onClick = { csvOutput = !csvOutput },
                        label = { Text("CSV output") },
                    )
                }
                OutlinedTextField(
                    value = countInput,
                    onValueChange = { countInput = it.filter(Char::isDigit) },
                    label = { Text("Count") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (mode == MockMode.JSON) {
                    OutlinedTextField(
                        value = schema,
                        onValueChange = { schema = it },
                        label = { Text("Schema (field:type,...)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AssistChip(onClick = ::generate, label = { Text("Generate") })
                Text(
                    text = when (mode) {
                        MockMode.LOREM -> "Generates lorem ipsum paragraphs."
                        MockMode.PEOPLE -> "Generates names, emails, addresses, and phone numbers."
                        MockMode.JSON -> "Generates mock JSON arrays from a lightweight schema."
                    },
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

private fun generateLorem(count: Int): String {
    return (1..count).joinToString("\n\n") {
        (1..4).joinToString(" ") {
            loremSentences[random.nextInt(loremSentences.size)]
        }
    }
}

private fun generatePeople(count: Int, locale: String, csvOutput: Boolean): String {
    val data = (1..count).map {
        val firstName = pick(firstNames)
        val lastName = pick(lastNames)
        val city = if (locale == "en_GB") pick(ukCities) else pick(usCities)
        val phonePrefix = if (locale == "en_GB") "+44" else "+1"
        val name = "$firstName $lastName"
        mapOf(
            "name" to name,
            "email" to "${firstName.lowercase()}.${lastName.lowercase()}@example.dev",
            "address" to "${randomInt(1, 220)} ${pick(streetNames)} St, $city",
            "phone" to "$phonePrefix ${randomInt(100_000, 999_999)}",
        )
    }
    return if (csvOutput) {
        buildString {
            appendLine("name,email,address,phone")
            data.forEach { row ->
                appendLine(row.values.joinToString(",") { value -> "\"$value\"" })
            }
        }.trim()
    } else {
        data.joinToString("\n") { row ->
            row.entries.joinToString(" | ") { (key, value) -> "$key: $value" }
        }
    }
}

private fun generateJsonSchema(count: Int, schema: String): String {
    val fields = schema.split(",").mapNotNull { descriptor ->
        val name = descriptor.substringBefore(":").trim()
        val type = descriptor.substringAfter(":", "").trim()
        if (name.isBlank() || type.isBlank()) null else name to type
    }
    return buildString {
        appendLine("[")
        repeat(count) { index ->
            append("  {")
            append(
                fields.joinToString(", ") { (name, type) ->
                    "\"$name\": ${jsonValue(type)}"
                }
            )
            append("}")
            if (index < count - 1) append(',')
            appendLine()
        }
        append("]")
    }
}

private fun jsonValue(type: String): String = when (type.lowercase()) {
    "uuid" -> "\"${UUID.randomUUID()}\""
    "name" -> "\"${pick(firstNames)} ${pick(lastNames)}\""
    "email" -> "\"user${random.nextInt(10_000)}@example.dev\""
    "address" -> "\"${randomInt(1, 220)} ${pick(streetNames)} Street\""
    "phone" -> "\"+1 ${randomInt(100_000, 999_999)}\""
    "int", "number" -> randomInt(18, 90).toString()
    "bool", "boolean" -> (random.nextInt(2) == 0).toString()
    "lorem", "string" -> "\"${pick(loremSentences)}\""
    else -> "\"$type\""
}

private fun <T> pick(items: List<T>): T = items[random.nextInt(items.size)]

private fun randomInt(startInclusive: Int, endExclusive: Int): Int {
    require(endExclusive > startInclusive) { "endExclusive must be greater than startInclusive" }
    return startInclusive + random.nextInt(endExclusive - startInclusive)
}

private val firstNames = listOf("Alex", "Jordan", "Riley", "Morgan", "Taylor", "Casey")
private val lastNames = listOf("Patel", "Nguyen", "Smith", "Jones", "Brown", "Williams")
private val streetNames = listOf("King", "Station", "Bridge", "High", "Market", "Oak")
private val ukCities = listOf("London", "Manchester", "Bristol", "Leeds")
private val usCities = listOf("New York", "Austin", "Seattle", "Denver")
private val loremSentences = listOf(
    "Lorem ipsum dolor sit amet consectetur adipiscing elit.",
    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    "Ut enim ad minim veniam quis nostrud exercitation ullamco laboris.",
    "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore.",
)
