package com.devtoolkit.feature.color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devtoolkit.core.data.preferences.ToolStateDataStore
import com.devtoolkit.core.ui.ClipboardBar
import com.devtoolkit.core.ui.OutputPanel
import com.devtoolkit.core.ui.ToolScaffold
import com.devtoolkit.core.ui.ToolScreenColumn
import com.devtoolkit.core.ui.ToolSection
import com.devtoolkit.core.ui.rememberToolActions
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun ColorScreen(onBack: () -> Unit) {
    val actions = rememberToolActions("color")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toolStateStore = remember(context) { ToolStateDataStore(context.applicationContext) }
    var hue by rememberSaveable { mutableStateOf(210f) }
    var saturation by rememberSaveable { mutableStateOf(0.75f) }
    var value by rememberSaveable { mutableStateOf(0.85f) }
    var hexInput by rememberSaveable { mutableStateOf("#2A7FFF") }
    var contrastHex by rememberSaveable { mutableStateOf("#FFFFFF") }
    var paletteSearch by rememberSaveable { mutableStateOf("") }
    val savedSwatches by toolStateStore.colorSwatches.collectAsStateWithLifecycle(initialValue = emptyList())

    val currentColor = remember(hue, saturation, value) { hsvToComposeColor(hue, saturation, value) }
    val currentHex = remember(currentColor) { colorToHex(currentColor) }
    val paletteMatches = namedPalettes.filter { it.first.contains(paletteSearch, ignoreCase = true) }.take(6)
    val paletteVariants = remember(hue, saturation, value) {
        listOf(
            "Complementary" to colorToHex(hsvToComposeColor((hue + 180f) % 360f, saturation, value)),
            "Analogous -" to colorToHex(hsvToComposeColor((hue + 330f) % 360f, saturation, value)),
            "Analogous +" to colorToHex(hsvToComposeColor((hue + 30f) % 360f, saturation, value)),
            "Triadic +" to colorToHex(hsvToComposeColor((hue + 120f) % 360f, saturation, value)),
            "Triadic -" to colorToHex(hsvToComposeColor((hue + 240f) % 360f, saturation, value)),
        )
    }
    val contrastColor = remember(contrastHex) { parseHexColor(contrastHex) ?: Color.White }
    val contrastRatio = remember(currentColor, contrastColor) { contrastRatio(currentColor, contrastColor) }

    fun loadHex(hex: String) {
        parseHexColor(hex)?.let { parsed ->
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(parsed.toArgb(), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            hexInput = colorToHex(parsed)
        }
    }

    ToolScaffold(title = "Colour Picker", onBack = onBack) { padding ->
        ToolScreenColumn(padding = padding) {
            ToolSection(title = "Picker") {
                ClipboardBar(
                    onPaste = { loadHex(actions.readClipboard()) },
                    onCopy = { actions.copyText(currentHex) },
                    onClear = {
                        hue = 0f
                        saturation = 0f
                        value = 0f
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(currentColor),
                )
                Text("Hue ${hue.toInt()}")
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text("Saturation ${(saturation * 100).toInt()}%")
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
                Text("Value ${(value * 100).toInt()}%")
                Slider(value = value, onValueChange = { thisValue -> value = thisValue }, valueRange = 0f..1f)
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    label = { Text("HEX") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            loadHex(hexInput)
                            actions.saveHistory(currentHex, currentHex)
                        },
                        label = { Text("Load hex") },
                    )
                    AssistChip(
                        onClick = {
                            scope.launch { toolStateStore.saveColorSwatch(currentHex) }
                            actions.saveHistory(currentHex, currentHex)
                        },
                        label = { Text("Save swatch") },
                    )
                    if (currentHex in savedSwatches) {
                        AssistChip(
                            onClick = { scope.launch { toolStateStore.removeColorSwatch(currentHex) } },
                            label = { Text("Remove swatch") },
                        )
                    }
                }
            }

            ToolSection(title = "Conversions & Contrast") {
                OutlinedTextField(
                    value = contrastHex,
                    onValueChange = { contrastHex = it },
                    label = { Text("Contrast against") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutputPanel(
                    content = buildString {
                        val rgb = rgbValues(currentColor)
                        val hsl = hslValues(currentColor)
                        val cmyk = cmykValues(currentColor)
                        appendLine("HEX: $currentHex")
                        appendLine("RGB: ${rgb.first}, ${rgb.second}, ${rgb.third}")
                        appendLine("HSV: ${hue.toInt()}, ${(saturation * 100).toInt()}%, ${(value * 100).toInt()}%")
                        appendLine("HSL: ${hsl.first}, ${hsl.second}%, ${hsl.third}%")
                        appendLine("CMYK: ${cmyk.first}%, ${cmyk.second}%, ${cmyk.third}%, ${cmyk.fourth}%")
                        appendLine("Contrast ratio: ${"%.2f".format(contrastRatio)}")
                        appendLine(if (contrastRatio >= 7f) "WCAG AAA pass" else if (contrastRatio >= 4.5f) "WCAG AA pass" else "Fails AA for normal text")
                    }.trim(),
                )
            }

            ToolSection(title = "Palettes") {
                OutlinedTextField(
                    value = paletteSearch,
                    onValueChange = { paletteSearch = it },
                    label = { Text("Search named colours") },
                    modifier = Modifier.fillMaxWidth(),
                )
                paletteMatches.forEach { (name, hex) ->
                    AssistChip(
                        onClick = { loadHex(hex) },
                        label = { Text("$name $hex") },
                    )
                }
                Text(
                    text = "Generated palette",
                    style = MaterialTheme.typography.labelLarge,
                )
                paletteVariants.forEach { (name, hex) ->
                    AssistChip(
                        onClick = { loadHex(hex) },
                        label = { Text("$name $hex") },
                    )
                }
                if (savedSwatches.isNotEmpty()) {
                    Text(
                        text = "Saved swatches",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    savedSwatches.forEach { swatch ->
                        AssistChip(
                            onClick = { loadHex(swatch) },
                            label = { Text(swatch) },
                        )
                    }
                }
            }
        }
    }
}

private fun hsvToComposeColor(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

private fun parseHexColor(value: String): Color? = runCatching {
    Color(android.graphics.Color.parseColor(value.trim()))
}.getOrNull()

private fun colorToHex(color: Color): String =
    "#%02X%02X%02X".format(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
    )

private fun rgbValues(color: Color): Triple<Int, Int, Int> = Triple(
    (color.red * 255).toInt(),
    (color.green * 255).toInt(),
    (color.blue * 255).toInt(),
)

private fun hslValues(color: Color): Triple<Int, Int, Int> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min
    val lightness = (max + min) / 2f
    val saturation = if (delta == 0f) 0f else delta / (1f - abs(2f * lightness - 1f))
    val hue = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it.isNaN()) 0f else if (it < 0f) it + 360f else it }
    return Triple(hue.toInt(), (saturation * 100).toInt(), (lightness * 100).toInt())
}

private data class Cmyk(val first: Int, val second: Int, val third: Int, val fourth: Int)

private fun cmykValues(color: Color): Cmyk {
    val r = color.red
    val g = color.green
    val b = color.blue
    val k = 1f - max(r, max(g, b))
    if (k >= 1f) return Cmyk(0, 0, 0, 100)
    val c = ((1f - r - k) / (1f - k) * 100).toInt()
    val m = ((1f - g - k) / (1f - k) * 100).toInt()
    val y = ((1f - b - k) / (1f - k) * 100).toInt()
    return Cmyk(c, m, y, (k * 100).toInt())
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    fun luminance(color: Color): Float {
        fun channel(value: Float): Float =
            if (value <= 0.03928f) value / 12.92f else Math.pow(((value + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
    }
    val l1 = luminance(foreground) + 0.05f
    val l2 = luminance(background) + 0.05f
    return max(l1, l2) / min(l1, l2)
}

private val namedPalettes = listOf(
    "Material Blue 500" to "#2196F3",
    "Material Red 500" to "#F44336",
    "Material Green 500" to "#4CAF50",
    "Tailwind Slate 700" to "#334155",
    "Tailwind Amber 500" to "#F59E0B",
    "Tailwind Emerald 500" to "#10B981",
)
