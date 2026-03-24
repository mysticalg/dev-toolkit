package com.devtoolkit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devtoolkit.core.data.preferences.UserPreferencesDataStore
import com.devtoolkit.core.domain.ThemeMode
import com.devtoolkit.core.domain.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsStore: UserPreferencesDataStore,
) : ViewModel() {
    val preferences = prefsStore.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferences(),
    )

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefsStore.updateTheme(mode) }
    fun setAmoledBlack(enabled: Boolean) = viewModelScope.launch { prefsStore.updateAmoledBlack(enabled) }
    fun setFontSize(size: Int) = viewModelScope.launch { prefsStore.updateFontSize(size) }
    fun setMonoFont(font: String) = viewModelScope.launch { prefsStore.updateMonoFont(font) }
    fun setToolOrder(order: List<String>) = viewModelScope.launch { prefsStore.updateToolOrder(order) }
}
