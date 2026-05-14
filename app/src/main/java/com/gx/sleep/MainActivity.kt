package com.gx.sleep

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gx.sleep.data.datastore.AppSettings
import com.gx.sleep.data.datastore.SettingsDataStore
import com.gx.sleep.system.PermissionManager
import com.gx.sleep.ui.screens.batteryguide.BatteryGuideScreen
import com.gx.sleep.ui.screens.debug.DebugScreen
import com.gx.sleep.ui.screens.eventdetail.EventDetailScreen
import com.gx.sleep.ui.screens.home.HomeScreen
import com.gx.sleep.ui.screens.privacy.PrivacyScreen
import com.gx.sleep.ui.screens.recording.RecordingScreen
import com.gx.sleep.ui.screens.sessiondetail.SessionDetailScreen
import com.gx.sleep.ui.screens.sessionlist.SessionListScreen
import com.gx.sleep.ui.screens.settings.SettingsScreen
import com.gx.sleep.ui.theme.GxSleepTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val settingsDataStore = remember { SettingsDataStore(context) }
            val settings by settingsDataStore.settings.collectAsState(initial = AppSettings())
            GxSleepTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = PermissionManager.getRequiredPermissions()
        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}

data class TabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val tabItems = listOf(
    TabItem("home", "首页", Icons.Filled.Home, Icons.Outlined.Home),
    TabItem("sessions", "历史", Icons.Filled.List, Icons.Outlined.List),
    TabItem("settings", "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show bottom bar only on tab screens
    val showBottomBar = currentDestination?.route in tabItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabItems.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(220)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(220)) }
    ) {
        // Tab screens
        composable("home") {
            HomeScreen(
                onNavigateToRecording = { navController.navigate("recording") },
                onNavigateToSessionDetail = { id -> navController.navigate("session/$id") }
            )
        }
        composable("sessions") {
            SessionListScreen(
                onSessionClick = { id -> navController.navigate("session/$id") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBatteryGuide = { navController.navigate("battery_guide") },
                onPrivacy = { navController.navigate("privacy") },
                onDebug = { navController.navigate("debug") }
            )
        }

        // Full-screen sub-pages
        composable("recording") {
            RecordingScreen(
                onBack = { navController.popBackStack() },
                onStopAndShowReport = { sessionId ->
                    navController.popBackStack()
                    if (sessionId > 0) {
                        navController.navigate("session/$sessionId")
                    }
                }
            )
        }
        composable(
            route = "session/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            SessionDetailScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onEventClick = { eventId -> navController.navigate("event/$eventId") }
            )
        }
        composable(
            route = "event/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: return@composable
            EventDetailScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("battery_guide") {
            BatteryGuideScreen(onBack = { navController.popBackStack() })
        }
        composable("privacy") {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable("debug") {
            DebugScreen(onBack = { navController.popBackStack() })
        }
    }
}
