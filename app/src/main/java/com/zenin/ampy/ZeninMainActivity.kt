package com.zenin.ampy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.zenin.ampy.ui.theme.AmpyTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import com.zenin.ampy.ui.nav.NavRoute
import com.zenin.ampy.ui.screen.About
import com.zenin.ampy.ui.screen.BatteryMonitor
import com.zenin.ampy.ui.screen.BatteryMonitorSettings
import com.zenin.ampy.ui.screen.Dashboard
import com.zenin.ampy.ui.screen.History
import com.zenin.ampy.ui.screen.Settings
import com.zenin.ampy.ui.screen.FloatingWindowSettings
import com.zenin.ampy.ui.screen.UniversalSupportLogcat
import com.zenin.ampy.vm.BatteryInfoViewModel
import com.zenin.ampy.vm.BatteryLogViewModel
import com.zenin.ampy.vm.BatteryMonitorSettingsViewModel
import com.zenin.ampy.vm.FloatingWindowSettingsViewModel
import com.zenin.ampy.vm.HistoryInfoViewModel
import com.zenin.ampy.vm.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ZeninMainActivity : ComponentActivity() {

    // This list contains the top-level routes that will show the bottom navigation bar
    private val topLevelRoutes = listOf(
        "dashboard", "battery_monitor", "history", "settings"
    )
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val battMonViewModel: BatteryMonitorSettingsViewModel by viewModels()
    private val floatingWindowSettingsViewModel: FloatingWindowSettingsViewModel by viewModels()
    private val batteryInfoViewModel: BatteryInfoViewModel by viewModels()
    private val historyInfoViewModel: HistoryInfoViewModel by viewModels()
    private val batteryLogViewModel: BatteryLogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val useDarkTheme = (application as MainApplication).useDarkTheme
            AmpyTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    // Only show the top bar when on a top-level screen
                    bottomBar = {
                        if (currentRoute in topLevelRoutes) {
                            BottomNavigationBar(navController)
                        }
                    }
                )
                { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = paddingValues.calculateBottomPadding())
                    ) {
                        composable("dashboard") {
                            Dashboard(
                                stringResource(R.string.app_name),
                                batteryInfoViewModel,
                                settingsViewModel,
                                batteryLogViewModel,
                                onOpenLogcatDetails = {
                                    navController.navigate("battery_logcat_experiment")
                                }
                            )
                        }
                        composable("history") {
                            History(
                                historyInfoViewModel,
                                stringResource(R.string.history)
                            )
                        }
                        composable("settings")  {
                            Settings(
                                currentTitle = stringResource(R.string.settings),
                                navController = navController,
                                settingsVM = settingsViewModel
                            )
                        }
                        composable("about")     { About(stringResource(R.string.about), navController) }
                        composable("batt_mon_settings") {
                            BatteryMonitorSettings(
                                navController,
                                battMonViewModel,
                                stringResource(R.string.battery_monitor_entry_settings)
                            )
                        }
                        composable("floating_window_settings") {
                            FloatingWindowSettings(
                                stringResource(R.string.floating_window_settings),
                                navController,
                                floatingWindowSettingsViewModel
                            )
                        }
                        composable("battery_monitor") {
                            BatteryMonitor(
                                stringResource(R.string.battery_monitor),
                                navController,
                                battMonViewModel
                            )
                        }
                        composable("battery_logcat_experiment") {
                            UniversalSupportLogcat(
                                navController,
                                batteryLogViewModel,
                                stringResource(R.string.get_from_logcat_title)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    // Define the list of navigation routes using the data class
    val navRoutes = listOf(
        NavRoute(
            "dashboard",
            selectedIcon = ImageVector.vectorResource(id = R.drawable.home_24dp_1f1f1f_fill1_wght400_grad0_opsz24),
            unselectedIcon = ImageVector.vectorResource(id = R.drawable.home_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
            label = stringResource(R.string.nav_dashboard)
        ),
        NavRoute(
            "battery_monitor",
            selectedIcon = ImageVector.vectorResource(id = R.drawable.speed_24dp_1f1f1f_fill1_wght400_grad0_opsz24),
            unselectedIcon = ImageVector.vectorResource(id = R.drawable.speed_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
            label = stringResource(R.string.monitor)
        ),
        NavRoute(
            "history",
            selectedIcon = ImageVector.vectorResource(id = R.drawable.library_books_24dp_1f1f1f_fill1_wght400_grad0_opsz24),
            unselectedIcon = ImageVector.vectorResource(id = R.drawable.library_books_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
            label = stringResource(R.string.nav_history)
        ),
        NavRoute(
            "settings",
            selectedIcon = ImageVector.vectorResource(id = R.drawable.settings_24dp_1f1f1f_fill1_wght400_grad0_opsz24),
            unselectedIcon = ImageVector.vectorResource(id = R.drawable.settings_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
            label = stringResource(R.string.settings)
        ),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
    {
        navRoutes.forEach { navRoute ->
            val selected = currentDestination?.route == navRoute.route
            NavigationBarItem(
                alwaysShowLabel = false,
                icon = {
                    Icon(
                        imageVector = if (selected) navRoute.selectedIcon else navRoute.unselectedIcon,
                        contentDescription = navRoute.label
                    )
                },
                label = { Text(navRoute.label) },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = {
                    navController.navigate(navRoute.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
