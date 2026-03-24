package com.devtoolkit.core.domain

data class HistoryEntry(
    val id: Long = 0,
    val toolId: String,
    val input: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false,
)
