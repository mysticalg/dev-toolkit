package com.devtoolkit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devtoolkit.core.domain.Tool
import com.devtoolkit.core.domain.ToolCategory
import com.devtoolkit.core.domain.ToolRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    var searchQuery by remember { mutableStateOf("") }
    val prefs by settingsViewModel.preferences.collectAsState()
    val orderedTools = remember(prefs.toolOrder) { ToolRegistry.ordered(prefs.toolOrder) }
    val groupedTools = remember(orderedTools) { ToolCategory.entries.map { cat -> cat to orderedTools.filter { it.category == cat } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DevToolkit") },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search tools...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groupedTools.forEach { (category, tools) ->
                    val filtered = if (searchQuery.isBlank()) tools
                    else tools.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                    }
                    if (filtered.isNotEmpty()) {
                        item(key = category.name) {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(filtered, key = { it.id }) { tool ->
                            ToolCard(tool = tool, onClick = { onToolClick(tool.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(tool: Tool, onClick: () -> Unit) {
    val icon = when (tool.id) {
        "json" -> Icons.Default.DataObject
        "regex" -> Icons.AutoMirrored.Filled.ManageSearch
        "diff" -> Icons.AutoMirrored.Filled.CompareArrows
        "texttransform" -> Icons.Default.AutoFixHigh
        "base64" -> Icons.Default.Code
        "url" -> Icons.Default.Link
        "hash" -> Icons.Default.Fingerprint
        "jwt" -> Icons.Default.Key
        "epoch" -> Icons.Default.Schedule
        "color" -> Icons.Default.Palette
        "uuid" -> Icons.Default.Casino
        "mockdata" -> Icons.Default.Storage
        else -> Icons.Default.Build
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (tool.isFree) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Free", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
