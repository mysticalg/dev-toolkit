package com.devtoolkit.core.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devtoolkit.core.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolActions(
    val readClipboard: () -> String,
    val copyText: (String) -> Unit,
    val shareText: (String) -> Unit,
    val saveHistory: (String, String) -> Unit,
)

@Composable
fun rememberToolActions(toolId: String): ToolActions {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val historyViewModel: ToolHistoryViewModel = hiltViewModel()

    return remember(toolId, clipboardManager, context, historyViewModel) {
        ToolActions(
            readClipboard = { clipboardManager.getText()?.text.orEmpty() },
            copyText = { text ->
                clipboardManager.setText(AnnotatedString(text))
                if (text.isNotBlank()) {
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            },
            shareText = { text ->
                if (text.isNotBlank()) {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            },
                            "Share via",
                        )
                    )
                }
            },
            saveHistory = { input, output ->
                historyViewModel.save(toolId = toolId, input = input, output = output)
            },
        )
    }
}

@HiltViewModel
class ToolHistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    fun save(toolId: String, input: String, output: String) {
        if (input.isBlank() && output.isBlank()) return
        viewModelScope.launch {
            historyRepository.addEntry(toolId = toolId, input = input, output = output)
        }
    }
}
