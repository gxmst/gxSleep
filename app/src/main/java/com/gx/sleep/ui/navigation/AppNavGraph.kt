package com.gx.sleep.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gx.sleep.ui.screens.batteryguide.BatteryGuideScreen
import com.gx.sleep.ui.screens.debug.DebugScreen
import com.gx.sleep.ui.screens.eventdetail.EventDetailScreen
import com.gx.sleep.ui.screens.home.HomeScreen
import com.gx.sleep.ui.screens.privacy.PrivacyScreen
import com.gx.sleep.ui.screens.recording.RecordingScreen
import com.gx.sleep.ui.screens.sessiondetail.SessionDetailScreen
import com.gx.sleep.ui.screens.sessionlist.SessionListScreen
import com.gx.sleep.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToRecording = {
                    navController.navigate(Screen.Recording.route)
                },
                onNavigateToSessions = {
                    navController.navigate(Screen.SessionList.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Recording.route) {
            RecordingScreen(
                onBack = { navController.popBackStack() },
                onStopAndShowReport = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.SessionList.route) {
            SessionListScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                }
            )
        }

        composable(
            route = Screen.SessionDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                }
            )
        }

        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: return@composable
            EventDetailScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onBatteryGuide = { navController.navigate(Screen.BatteryGuide.route) },
                onPrivacy = { navController.navigate(Screen.Privacy.route) },
                onDebug = { navController.navigate(Screen.Debug.route) }
            )
        }

        composable(Screen.BatteryGuide.route) {
            BatteryGuideScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Debug.route) {
            DebugScreen(onBack = { navController.popBackStack() })
        }
    }
}
