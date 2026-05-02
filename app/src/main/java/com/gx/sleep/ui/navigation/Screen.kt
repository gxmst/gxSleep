package com.gx.sleep.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Recording : Screen("recording")
    data object SessionList : Screen("sessions")
    data object SessionDetail : Screen("session/{sessionId}") {
        fun createRoute(sessionId: Long) = "session/$sessionId"
    }
    data object EventDetail : Screen("event/{eventId}") {
        fun createRoute(eventId: Long) = "event/$eventId"
    }
    data object Settings : Screen("settings")
    data object BatteryGuide : Screen("battery_guide")
    data object Privacy : Screen("privacy")
    data object Debug : Screen("debug")
}
