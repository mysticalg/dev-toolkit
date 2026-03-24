package com.devtoolkit.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.material3.ColorProviders
import androidx.compose.ui.unit.dp
import com.devtoolkit.EXTRA_TARGET_TOOL
import com.devtoolkit.EXTRA_USE_CLIPBOARD
import com.devtoolkit.MainActivity
import java.util.UUID

private object WidgetStateKeys {
    val LAST_UUID = stringPreferencesKey("last_uuid")
    val LAST_EPOCH = longPreferencesKey("last_epoch")
}

private object WidgetGlanceColorScheme {
    val colors = ColorProviders(
        light = lightColorScheme(
            primary = Color(0xFF2563EB),
            onPrimary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF374151),
            onSecondaryContainer = Color(0xFFFFFFFF),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFFFFFFF),
            background = Color(0xFF111827),
            onBackground = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFFCBD5E1),
        ),
        dark = darkColorScheme(
            primary = Color(0xFF2563EB),
            onPrimary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF374151),
            onSecondaryContainer = Color(0xFFFFFFFF),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFFFFFFF),
            background = Color(0xFF111827),
            onBackground = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFFCBD5E1),
        ),
    )
}

class UuidWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UuidWidget()
}

class EpochWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EpochWidget()
}

class QuickPasteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickPasteWidget()
}

private class UuidWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val lastUuid = prefs[WidgetStateKeys.LAST_UUID] ?: "Tap generate to copy a UUID"
            WidgetShell(
                title = "Generate UUID",
                primary = lastUuid,
                secondary = "One tap generates and copies a UUID v4.",
                primaryActionLabel = "Generate",
                primaryAction = actionRunCallback<GenerateUuidAction>(),
                secondaryActionLabel = "Open Tool",
                secondaryAction = actionRunCallback<OpenUuidToolAction>(),
            )
        }
    }
}

private class EpochWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val epoch = prefs[WidgetStateKeys.LAST_EPOCH] ?: System.currentTimeMillis()
            WidgetShell(
                title = "Current Epoch",
                primary = "ms: $epoch",
                secondary = "sec: ${epoch / 1000}",
                primaryActionLabel = "Refresh & Copy",
                primaryAction = actionRunCallback<RefreshEpochAction>(),
                secondaryActionLabel = "Open Tool",
                secondaryAction = actionRunCallback<OpenEpochToolAction>(),
            )
        }
    }
}

private class QuickPasteWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        provideContent {
            WidgetShell(
                title = "Quick Paste",
                primary = "Use your clipboard",
                secondary = "Open the best tool for whatever you copied.",
                primaryActionLabel = "Paste Clipboard",
                primaryAction = actionRunCallback<OpenQuickPasteAction>(),
                secondaryActionLabel = "Open App",
                secondaryAction = actionRunCallback<OpenHomeAction>(),
            )
        }
    }
}

class GenerateUuidAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters,
    ) {
        val uuid = UUID.randomUUID().toString()
        copyToClipboard(context, "UUID", uuid)
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetStateKeys.LAST_UUID] = uuid
        }
        UuidWidget().update(context, glanceId)
    }
}

class RefreshEpochAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: androidx.glance.GlanceId,
        parameters: ActionParameters,
    ) {
        val epoch = System.currentTimeMillis()
        copyToClipboard(context, "Epoch", epoch.toString())
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetStateKeys.LAST_EPOCH] = epoch
        }
        EpochWidget().update(context, glanceId)
    }
}

class OpenUuidToolAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: androidx.glance.GlanceId, parameters: ActionParameters) {
        context.startActivity(toolIntent(context, "uuid"))
    }
}

class OpenEpochToolAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: androidx.glance.GlanceId, parameters: ActionParameters) {
        context.startActivity(toolIntent(context, "epoch"))
    }
}

class OpenQuickPasteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: androidx.glance.GlanceId, parameters: ActionParameters) {
        context.startActivity(clipboardIntent(context))
    }
}

class OpenHomeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: androidx.glance.GlanceId, parameters: ActionParameters) {
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
    }
}

@androidx.compose.runtime.Composable
private fun WidgetShell(
    title: String,
    primary: String,
    secondary: String,
    primaryActionLabel: String,
    primaryAction: Action,
    secondaryActionLabel: String,
    secondaryAction: Action,
) {
    GlanceTheme(colors = WidgetGlanceColorScheme.colors) {
        val colors = GlanceTheme.colors
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.widgetBackground)
                .padding(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.height(10.dp))
            Text(
                text = primary,
                style = TextStyle(color = colors.onSurface),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 3,
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = secondary,
                style = TextStyle(color = colors.onSurfaceVariant),
                modifier = GlanceModifier.fillMaxWidth(),
                maxLines = 2,
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            Button(
                text = primaryActionLabel,
                onClick = primaryAction,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Button(
                text = secondaryActionLabel,
                onClick = secondaryAction,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                ),
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun toolIntent(context: Context, toolId: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_TARGET_TOOL, toolId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

private fun clipboardIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_USE_CLIPBOARD, true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
