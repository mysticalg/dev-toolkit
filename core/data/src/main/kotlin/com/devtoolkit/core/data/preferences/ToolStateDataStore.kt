package com.devtoolkit.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.toolStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "tool_state")

class ToolStateDataStore(
    private val context: Context,
) {
    private object Keys {
        val REGEX_HISTORY = stringPreferencesKey("regex_history")
        val COLOR_SWATCHES = stringPreferencesKey("color_swatches")
        val TEXT_PIPELINES = stringPreferencesKey("text_pipelines")
    }

    val regexHistory: Flow<List<String>> = context.toolStateDataStore.data.map { prefs ->
        decodeList(prefs[Keys.REGEX_HISTORY])
    }

    val colorSwatches: Flow<List<String>> = context.toolStateDataStore.data.map { prefs ->
        decodeList(prefs[Keys.COLOR_SWATCHES])
    }

    val textPipelines: Flow<List<String>> = context.toolStateDataStore.data.map { prefs ->
        decodeList(prefs[Keys.TEXT_PIPELINES])
    }

    suspend fun rememberRegexPattern(pattern: String) {
        updateList(Keys.REGEX_HISTORY, pattern.trim(), maxItems = 20)
    }

    suspend fun saveColorSwatch(hex: String) {
        updateList(Keys.COLOR_SWATCHES, hex.trim().uppercase(), maxItems = 48)
    }

    suspend fun removeColorSwatch(hex: String) {
        removeItem(Keys.COLOR_SWATCHES, hex.trim().uppercase())
    }

    suspend fun saveTextPipeline(encodedPipeline: String) {
        updateList(Keys.TEXT_PIPELINES, encodedPipeline, maxItems = 20)
    }

    suspend fun removeTextPipeline(encodedPipeline: String) {
        removeItem(Keys.TEXT_PIPELINES, encodedPipeline)
    }

    private suspend fun updateList(
        key: Preferences.Key<String>,
        value: String,
        maxItems: Int,
    ) {
        if (value.isBlank()) return
        context.toolStateDataStore.edit { prefs ->
            val updated = listOf(value) + decodeList(prefs[key]).filterNot { it == value }
            prefs[key] = Json.encodeToString(updated.take(maxItems))
        }
    }

    private suspend fun removeItem(
        key: Preferences.Key<String>,
        value: String,
    ) {
        context.toolStateDataStore.edit { prefs ->
            prefs[key] = Json.encodeToString(decodeList(prefs[key]).filterNot { it == value })
        }
    }

    private fun decodeList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }
}
