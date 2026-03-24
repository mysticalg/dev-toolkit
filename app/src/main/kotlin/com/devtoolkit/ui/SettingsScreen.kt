package com.devtoolkit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devtoolkit.core.domain.ToolRegistry
import com.devtoolkit.core.domain.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            var themeExpanded by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(prefs.themeMode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.clickable { themeExpanded = true },
            )
            DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            viewModel.setTheme(mode)
                            themeExpanded = false
                        },
                    )
                }
            }

            ListItem(
                headlineContent = { Text("AMOLED Black") },
                supportingContent = { Text("Pure black background for OLED screens") },
                trailingContent = {
                    Switch(
                        checked = prefs.amoledBlack,
                        onCheckedChange = { viewModel.setAmoledBlack(it) },
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Editor",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            ListItem(
                headlineContent = { Text("Font Size: ${prefs.fontSize}sp") },
                supportingContent = {
                    Slider(
                        value = prefs.fontSize.toFloat(),
                        onValueChange = { viewModel.setFontSize(it.toInt()) },
                        valueRange = 10f..24f,
                        steps = 13,
                    )
                },
            )

            var fontExpanded by remember { mutableStateOf(false) }
            val fonts = listOf("JetBrains Mono", "Fira Code", "Source Code Pro", "System Monospace")
            ListItem(
                headlineContent = { Text("Monospace Font") },
                supportingContent = { Text(prefs.monoFont) },
                modifier = Modifier.clickable { fontExpanded = true },
            )
            DropdownMenu(expanded = fontExpanded, onDismissRequest = { fontExpanded = false }) {
                fonts.forEach { font ->
                    DropdownMenuItem(
                        text = { Text(font) },
                        onClick = {
                            viewModel.setMonoFont(font)
                            fontExpanded = false
                        },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Tool Order",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            var orderedTools by remember(prefs.toolOrder) {
                mutableStateOf(
                    ToolRegistry.ordered(prefs.toolOrder).map { it.id }
                )
            }
            ToolRegistry.ordered(orderedTools).forEachIndexed { index, tool ->
                ListItem(
                    headlineContent = { Text(tool.name) },
                    supportingContent = { Text(tool.category.displayName) },
                    trailingContent = {
                        Row {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        orderedTools = orderedTools.toMutableList().apply {
                                            add(index - 1, removeAt(index))
                                        }
                                        viewModel.setToolOrder(orderedTools)
                                    }
                                },
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                            }
                            IconButton(
                                onClick = {
                                    if (index < orderedTools.lastIndex) {
                                        orderedTools = orderedTools.toMutableList().apply {
                                            add(index + 1, removeAt(index))
                                        }
                                        viewModel.setToolOrder(orderedTools)
                                    }
                                },
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                            }
                        }
                    },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            ListItem(
                headlineContent = { Text("DevToolkit") },
                supportingContent = { Text("Version 1.0\nThe Developer's Pocket Utility Belt") },
            )
            ListItem(
                headlineContent = { Text("Privacy Policy") },
                supportingContent = { Text("Read how DevToolkit handles local data and clipboard content") },
                modifier = Modifier.clickable(onClick = onPrivacyPolicyClick),
            )
        }
    }
}
