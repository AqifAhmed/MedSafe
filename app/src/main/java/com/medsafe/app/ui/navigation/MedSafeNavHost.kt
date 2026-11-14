package com.medsafe.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.medsafe.app.ui.screens.addedit.AddEditMedScreen
import com.medsafe.app.ui.screens.history.HistoryScreen
import com.medsafe.app.ui.screens.meds.MyMedsScreen
import com.medsafe.app.ui.screens.onboarding.OnboardingScreen
import com.medsafe.app.ui.screens.profiles.ProfilesScreen
import com.medsafe.app.ui.screens.settings.SettingsScreen
import com.medsafe.app.ui.screens.today.TodayScreen

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.TODAY, "Today", Icons.Rounded.Today),
    BottomDestination(Routes.MEDS, "My Meds", Icons.Rounded.Medication),
    BottomDestination(Routes.HISTORY, "History", Icons.Rounded.CalendarMonth),
    BottomDestination(Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
)

@Composable
fun MedSafeApp(
    navController: NavHostController,
    startOnboarding: Boolean,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomBarRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.TODAY) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.TODAY,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    onAddMedication = { navController.navigate(Routes.addEditMed()) },
                    onOpenProfiles = { navController.navigate(Routes.PROFILES) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.MEDS) {
                MyMedsScreen(
                    onAddMedication = { navController.navigate(Routes.addEditMed()) },
                    onEditMedication = { medId -> navController.navigate(Routes.addEditMed(medId)) },
                )
            }
            composable(
                route = Routes.ADD_EDIT_MED,
                arguments = listOf(
                    navArgument(Routes.ARG_MED_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) {
                AddEditMedScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen()
            }
            composable(Routes.PROFILES) {
                ProfilesScreen(
                    onBack = { navController.popBackStack() },
                    onAddMedication = {
                        navController.navigate(Routes.addEditMed()) {
                            popUpTo(Routes.PROFILES) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenProfiles = { navController.navigate(Routes.PROFILES) },
                    onOpenOnboarding = { navController.navigate(Routes.ONBOARDING) },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Routes.TODAY) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
