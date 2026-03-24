package com.devtoolkit.core.domain

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val fontSize: Int = 14,
    val monoFont: String = "JetBrains Mono",
    val toolOrder: List<String> = emptyList(),
)
