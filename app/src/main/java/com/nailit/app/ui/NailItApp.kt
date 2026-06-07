package com.nailit.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nailit.app.core.catalog.TemplateCatalog
import com.nailit.app.core.preview.NailSessionRuntime
import com.nailit.app.ui.screens.AdaptationScreen
import com.nailit.app.ui.screens.BomScreen
import com.nailit.app.ui.screens.FinishCaptureScreen
import com.nailit.app.ui.screens.HomeScreen
import com.nailit.app.ui.screens.ConversationScreen
import com.nailit.app.ui.screens.InspirationScreen
import com.nailit.app.ui.screens.ProfileScreen
import com.nailit.app.ui.screens.SharePosterScreen

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun NailItApp() {
    val navController = rememberNavController()
    var selectedTutorialId by remember { mutableStateOf<String?>(TemplateCatalog.items.firstOrNull()?.id) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelDestinations = listOf(
        TopLevelDestination(
            route = "home",
            label = "首页",
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
        ),
        TopLevelDestination(
            route = "inspiration",
            label = "灵感",
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
        ),
        TopLevelDestination(
            route = "profile",
            label = "我的",
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
        ),
    )
    val showBottomBar = currentRoute in topLevelDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == destination.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = destination.icon,
                            label = { androidx.compose.material3.Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    selectedId = selectedTutorialId,
                    onSelectId = { selectedTutorialId = it },
                    onOpenFlow = { navController.navigate("adaptation") },
                    onOpenResult = { navController.navigate("adaptation") },
                    onOpenChat = { navController.navigate("conversation") },
                    onOpenInspiration = { navController.navigate("inspiration") },
                )
            }
            composable("inspiration") {
                InspirationScreen(
                    selectedId = selectedTutorialId,
                    onSelectId = { templateId ->
                        selectedTutorialId = templateId
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable("profile") {
                ProfileScreen(
                    sessionSnapshot = NailSessionRuntime.current,
                    onResume = {
                        val session = NailSessionRuntime.current
                        when {
                            session?.executionSteps?.isNotEmpty() == true -> navController.navigate("conversation")
                            !session?.targetImagePath.isNullOrBlank() -> navController.navigate("prepare")
                            session != null -> navController.navigate("adaptation")
                            else -> navController.navigate("home")
                        }
                    },
                )
            }
            composable("adaptation") {
                AdaptationScreen(
                    selectedId = selectedTutorialId,
                    sessionSnapshot = NailSessionRuntime.current,
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate("prepare") }
                )
            }
            composable("prepare") {
                BomScreen(
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
}
