package com.devtoolkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devtoolkit.core.domain.detectBestTool
import com.devtoolkit.feature.base64.Base64Screen
import com.devtoolkit.feature.color.ColorScreen
import com.devtoolkit.feature.diff.DiffScreen
import com.devtoolkit.feature.epoch.EpochScreen
import com.devtoolkit.feature.hash.HashScreen
import com.devtoolkit.feature.json.JsonScreen
import com.devtoolkit.feature.jwt.JwtScreen
import com.devtoolkit.feature.mockdata.MockDataScreen
import com.devtoolkit.feature.regex.RegexScreen
import com.devtoolkit.feature.texttransform.TextTransformScreen
import com.devtoolkit.feature.url.UrlScreen
import com.devtoolkit.feature.uuid.UuidScreen

@Composable
fun DevToolkitNavHost(
    sharedText: String? = null,
    requestedTool: String? = null,
) {
    val navController = rememberNavController()
    val suggestedRoute = requestedTool ?: detectBestTool(sharedText)

    LaunchedEffect(suggestedRoute, sharedText) {
        if (suggestedRoute != null) {
            navController.navigate(suggestedRoute)
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onToolClick = { toolId -> navController.navigate(toolId) },
                onSettingsClick = { navController.navigate("settings") },
                onHistoryClick = { navController.navigate("history") },
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPrivacyPolicyClick = { navController.navigate("privacy") },
            )
        }
        composable("privacy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable("history") {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable("json") {
            JsonScreen(sharedText = sharedText, onBack = { navController.popBackStack() })
        }
        composable("regex") {
            RegexScreen(onBack = { navController.popBackStack() })
        }
        composable("diff") {
            DiffScreen(onBack = { navController.popBackStack() })
        }
        composable("texttransform") {
            TextTransformScreen(sharedText = sharedText, onBack = { navController.popBackStack() })
        }
        composable("base64") {
            Base64Screen(sharedText = sharedText, onBack = { navController.popBackStack() })
        }
        composable("url") {
            UrlScreen(sharedText = sharedText, onBack = { navController.popBackStack() })
        }
        composable("hash") {
            HashScreen(sharedText = sharedText, onBack = { navController.popBackStack() })
        }
        composable("jwt") {
            JwtScreen(sharedText = sharedText, onBack = { navController.popBackStack() })
        }
        composable("epoch") {
            EpochScreen(sharedText = sharedText, onBack = { navController.popBackStack() })
        }
        composable("color") {
            ColorScreen(onBack = { navController.popBackStack() })
        }
        composable("uuid") {
            UuidScreen(sharedText = sharedText, onBack = { navController.popBackStack() })
        }
        composable("mockdata") {
            MockDataScreen(onBack = { navController.popBackStack() })
        }
    }
}
