package com.tachyon.transcripter.ui.navigation

// NavGraph.kt

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tachyon.transcripter.ui.dashboard.DashboardScreen
import com.tachyon.transcripter.ui.recording.RecordingScreen
import com.tachyon.transcripter.ui.summary.SummaryScreen

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")

    object Recording : Screen("recording/{sessionId}") {
        fun createRoute(sessionId: String) = "recording/$sessionId"

        const val ARG_SESSION_ID = "sessionId"
    }

    object Summary : Screen("summary/{sessionId}") {
        fun createRoute(sessionId: String) = "summary/$sessionId"

        const val ARG_SESSION_ID = "sessionId"
    }
}

/**
 * Main navigation graph for the app.
 */
@Composable
fun VoiceRecorderNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Dashboard screen - list of recordings
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRecording = { sessionId ->
                    navController.navigate(Screen.Recording.createRoute(sessionId))
                },
                onNavigateToSummary = { sessionId ->
                    navController.navigate(Screen.Summary.createRoute(sessionId))
                },
                onStartNewRecording = {
                    // Navigate to recording with new session
                    navController.navigate(Screen.Recording.createRoute("new"))
                }
            )
        }

        // Recording screen - active recording
        composable(
            route = Screen.Recording.route,
            arguments = listOf(
                navArgument(Screen.Recording.ARG_SESSION_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.Recording.ARG_SESSION_ID)

            RecordingScreen(
                sessionId = sessionId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSummary = { id ->
                    // Clear back stack and navigate to summary
                    navController.navigate(Screen.Summary.createRoute(id)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        // Summary screen - view summary of recording
        composable(
            route = Screen.Summary.route,
            arguments = listOf(
                navArgument(Screen.Summary.ARG_SESSION_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.Summary.ARG_SESSION_ID)
                ?: return@composable

            SummaryScreen(
                sessionId = sessionId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Extension function to safely navigate.
 */
fun NavHostController.navigateSafe(route: String) {
    try {
        navigate(route)
    } catch (e: Exception) {
        // Handle navigation error
    }
}

/**
 * Extension function to navigate and clear back stack.
 */
fun NavHostController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        popUpTo(0) {
            inclusive = true
        }
    }
}