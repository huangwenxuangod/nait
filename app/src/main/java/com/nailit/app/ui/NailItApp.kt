package com.nailit.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.ui.screens.AdaptationScreen
import com.nailit.app.ui.screens.HomeScreen
import com.nailit.app.ui.screens.TutorialScreen
import com.nailit.app.ui.screens.BomScreen
import com.nailit.app.ui.screens.SopScreen

@Composable
fun NailItApp() {
    val navController = rememberNavController()
    var selectedTutorialId by remember { mutableStateOf<String?>("aurora-cat") }
    val sessionSnapshot = NailSessionRuntime.current

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                selectedId = selectedTutorialId,
                onSelectId = { selectedTutorialId = it },
                onOpenFlow = { navController.navigate("tutorial") },
                onOpenResult = { /* No longer standalone */ }
            )
        }
        composable("tutorial") {
            TutorialScreen(
                selectedId = selectedTutorialId,
                sessionSnapshot = sessionSnapshot,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate("adaptation") }
            )
        }
        composable("adaptation") {
            AdaptationScreen(
                selectedId = selectedTutorialId,
                sessionSnapshot = sessionSnapshot,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate("bom") }
            )
        }
        composable("bom") {
            BomScreen(
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate("sop") }
            )
        }
        composable("sop") {
            SopScreen(
                onBack = { navController.popBackStack() },
                onFinish = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
