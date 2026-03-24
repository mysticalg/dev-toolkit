package com.devtoolkit.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devtoolkit.core.data.repository.HistoryRepository
import com.devtoolkit.core.domain.HistoryEntry
import com.devtoolkit.core.domain.ToolRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedToolId = MutableStateFlow<String?>(null)
    private val starredOnly = MutableStateFlow(false)

    val filters = combine(searchQuery, selectedToolId, starredOnly) { query, toolId, starred ->
        HistoryFilters(query = query, selectedToolId = toolId, starredOnly = starred)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HistoryFilters(),
    )

    val entries = filters.flatMapLatest { filter ->
        repository.filterEntries(
            query = filter.query,
            toolId = filter.selectedToolId,
            starredOnly = filter.starredOnly,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun setSelectedTool(toolId: String?) {
        selectedToolId.value = toolId
    }

    fun setStarredOnly(enabled: Boolean) {
        starredOnly.value = enabled
    }

    fun toggleStar(entry: HistoryEntry) = viewModelScope.launch { repository.toggleStar(entry) }

    fun delete(entry: HistoryEntry) = viewModelScope.launch { repository.deleteEntry(entry) }
}

data class HistoryFilters(
    val query: String = "",
    val selectedToolId: String? = null,
    val starredOnly: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val dateFormat = androidx.compose.runtime.remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = androidx.compose.ui.Modifier.padding(padding)) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setSearch,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search history...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            Row(
                modifier = androidx.compose.ui.Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !filters.starredOnly,
                    onClick = { viewModel.setStarredOnly(false) },
                    label = { Text("All entries") },
                )
                FilterChip(
                    selected = filters.starredOnly,
                    onClick = { viewModel.setStarredOnly(true) },
                    label = { Text("Pinned only") },
                )
            }

            Row(
                modifier = androidx.compose.ui.Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filters.selectedToolId == null,
                    onClick = { viewModel.setSelectedTool(null) },
                    label = { Text("All tools") },
                )
                ToolRegistry.tools.forEach { tool ->
                    FilterChip(
                        selected = filters.selectedToolId == tool.id,
                        onClick = { viewModel.setSelectedTool(tool.id) },
                        label = { Text(tool.name) },
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        text = "No history matches the current filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        HistoryEntryCard(
                            entry = entry,
                            dateFormat = dateFormat,
                            onToggleStar = { viewModel.toggleStar(entry) },
                            onDelete = { viewModel.delete(entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: HistoryEntry,
    dateFormat: SimpleDateFormat,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
) {
    val toolName = ToolRegistry.tools.find { it.id == entry.toolId }?.name ?: entry.toolId
    Card(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Row(
            modifier = androidx.compose.ui.Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.Top,
        ) {
            Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        text = toolName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    if (entry.isStarred) {
                        Text(
                            text = "Pinned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    }
                    Text(
                        text = dateFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                Text(
                    text = entry.input.take(140).ifBlank { "[No input captured]" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                )
                if (entry.output.isNotBlank()) {
                    Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                    Text(
                        text = entry.output.take(160),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                    )
                }
            }
            IconButton(onClick = onToggleStar) {
                Icon(
                    if (entry.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Pin",
                    tint = if (entry.isStarred) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
