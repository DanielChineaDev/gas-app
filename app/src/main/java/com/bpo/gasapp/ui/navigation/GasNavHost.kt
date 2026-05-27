package com.bpo.gasapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bpo.gasapp.ui.account.AccountScreen
import com.bpo.gasapp.ui.comparator.TankComparatorScreen
import com.bpo.gasapp.ui.detail.StationDetailRoute
import com.bpo.gasapp.ui.detail.StationDetailScreen
import com.bpo.gasapp.ui.favorites.FavoritesScreen
import com.bpo.gasapp.ui.map.MapScreen
import com.bpo.gasapp.ui.planner.RoutePlannerScreen
import com.bpo.gasapp.ui.profile.ProfileScreen
import com.bpo.gasapp.ui.refuel.RefuelLogRoute
import com.bpo.gasapp.ui.refuel.RefuelLogScreen
import com.bpo.gasapp.ui.saving.FuelSavingScreen
import com.bpo.gasapp.ui.settings.SettingsScreen
import com.bpo.gasapp.ui.stats.StatsScreen
import com.bpo.gasapp.ui.stations.StationListScreen

object Routes {
    const val LIST = "stations"
    const val MAP = "map"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val ACCOUNT = "account"
    const val SETTINGS = "settings"
    const val COMPARATOR = "comparator"
    const val PLANNER = "planner"
    const val SAVING = "saving"
    const val STATS = "stats"
}

private enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    LIST(Routes.LIST, "Inicio", Icons.AutoMirrored.Filled.List),
    MAP(Routes.MAP, "Mapa", Icons.Default.Map),
    FAVORITES(Routes.FAVORITES, "Favoritas", Icons.Default.Favorite),
    PROFILE(Routes.PROFILE, "Perfil", Icons.Default.Person)
}

@Composable
fun GasNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevel.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevel.entries.forEach { item ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LIST) {
                StationListScreen(
                    onStationClick = { id -> navController.navigate(StationDetailRoute.build(id)) },
                    onLogRefuel = { id, name, fuel ->
                        navController.navigate(RefuelLogRoute.build(id, name, fuel))
                    }
                )
            }
            composable(Routes.MAP) {
                MapScreen(
                    onStationClick = { id -> navController.navigate(StationDetailRoute.build(id)) }
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onStationClick = { id -> navController.navigate(StationDetailRoute.build(id)) },
                    onCompareClick = { navController.navigate(Routes.COMPARATOR) }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogin = { navController.navigate(Routes.ACCOUNT) },
                    onStats = { navController.navigate(Routes.STATS) },
                    onPlanner = { navController.navigate(Routes.PLANNER) },
                    onSaving = { navController.navigate(Routes.SAVING) },
                    onSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onBack = { navController.popBackStack() },
                    onLoggedIn = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.COMPARATOR) {
                TankComparatorScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PLANNER) {
                RoutePlannerScreen(
                    onStationClick = { id -> navController.navigate(StationDetailRoute.build(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SAVING) {
                FuelSavingScreen(
                    onStationClick = { id -> navController.navigate(StationDetailRoute.build(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    onBack = { navController.popBackStack() },
                    onAddRefuel = { navController.navigate(RefuelLogRoute.build()) }
                )
            }
            composable(
                route = RefuelLogRoute.PATTERN,
                arguments = listOf(
                    navArgument(RefuelLogRoute.ARG_STATION_ID) { type = NavType.StringType; defaultValue = "" },
                    navArgument(RefuelLogRoute.ARG_STATION_NAME) { type = NavType.StringType; defaultValue = "" },
                    navArgument(RefuelLogRoute.ARG_FUEL) { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                RefuelLogScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = StationDetailRoute.PATTERN,
                arguments = listOf(navArgument(StationDetailRoute.ARG_ID) { type = NavType.StringType })
            ) {
                StationDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
