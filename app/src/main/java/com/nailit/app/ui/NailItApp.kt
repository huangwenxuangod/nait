package com.nailit.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.ui.screens.AdaptationScreen
import com.nailit.app.ui.screens.FinishCaptureScreen
import com.nailit.app.ui.screens.HomeScreen
import com.nailit.app.ui.screens.ConversationScreen
import com.nailit.app.ui.screens.SharePosterScreen

@Composable
fun NailItApp() {
    val navController = rememberNavController()
    var selectedTutorialId by remember { mutableStateOf<String?>("aurora-cat") }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                selectedId = selectedTutorialId,
                onSelectId = { selectedTutorialId = it },
                onOpenFlow = { navController.navigate("adaptation") },
                onOpenResult = { navController.navigate("adaptation") },
                onOpenChat = { navController.navigate("conversation") }
            )
        }
        composable("adaptation") {
            AdaptationScreen(
                selectedId = selectedTutorialId,
                sessionSnapshot = NailSessionRuntime.current,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate("conversation") }
            )
        }
        composable("conversation") {
            ConversationScreen(
                sessionSnapshot = NailSessionRuntime.current,
                onBack = { navController.popBackStack() },
                onOpenTutorial = { navController.navigate("home") },
                onOpenTryOn = { navController.navigate("adaptation") },
                onOpenSop = { navController.navigate("finish_capture") }
            )
        }
        composable("finish_capture") {
            FinishCaptureScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate("share_poster") }
            )
        }
        composable("share_poster") {
            SharePosterScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
