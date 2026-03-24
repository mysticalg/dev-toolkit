package com.devtoolkit.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.devtoolkit.core.domain.ThemeMode
import com.devtoolkit.core.domain.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val FONT_SIZE = intPreferencesKey("font_size")
        val MONO_FONT = stringPreferencesKey("mono_font")
        val TOOL_ORDER = stringPreferencesKey("tool_order")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            amoledBlack = prefs[Keys.AMOLED_BLACK] ?: false,
            fontSize = prefs[Keys.FONT_SIZE] ?: 14,
            monoFont = prefs[Keys.MONO_FONT] ?: "JetBrains Mono",
            toolOrder = prefs[Keys.TOOL_ORDER]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
        )
    }

    suspend fun updateTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AMOLED_BLACK] = enabled }
    }

    suspend fun updateFontSize(size: Int) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size }
    }

    suspend fun updateMonoFont(font: String) {
        context.dataStore.edit { it[Keys.MONO_FONT] = font }
    }

    suspend fun updateToolOrder(order: List<String>) {
        context.dataStore.edit { it[Keys.TOOL_ORDER] = order.joinToString(",") }
    }
}
