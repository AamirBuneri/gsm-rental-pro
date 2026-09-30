package com.aamirbuneri.abgsmrental.ui.admin

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.Perm
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.ui.Shell
import com.aamirbuneri.abgsmrental.ui.account.AccountScreen
import com.aamirbuneri.abgsmrental.ui.goTab
import com.aamirbuneri.abgsmrental.ui.notifications.NotificationsScreen

object AdminRoutes {
    const val HOME = "a_home"
    const val RENTALS = "a_rentals"
    const val ORDERS = "a_orders"
    const val RESELLERS = "a_resellers"
    const val MORE = "a_more"
    const val TOOLS = "a_tools"
    const val NOTIFICATIONS = "a_notifications"
    const val RENTAL = "a_rental/{id}"
    const val ORDER = "a_order/{id}"
    const val RESELLER = "a_reseller/{id}"
    const val SLOTS = "a_slots/{id}"
    fun rental(id: Int) = "a_rental/$id"
    fun order(id: Int) = "a_order/$id"
    fun reseller(id: Int) = "a_reseller/$id"
    fun slots(toolId: Int) = "a_slots/$toolId"
}

/** Owner / staff state shared by the admin tabs. */
class AdminShellState {
    var openOrders by mutableIntStateOf(0)
    var pendingRegistrations by mutableIntStateOf(0)
    var servicesEnabled by mutableStateOf(true)
    /** Filter to open the Resellers / Orders / Rentals tab with (from the dashboard or a notification). */
    var resellerFilter by mutableStateOf<String?>(null)
    var orderFilter by mutableStateOf<String?>(null)
    var rentalFilter by mutableStateOf<String?>(null)
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector, val badge: () -> Int = { 0 })

@Composable
fun AdminShell(settings: Settings, shell: Shell) {
    val nav = rememberNavController()
    val admin = remember { AdminShellState() }
    val context = LocalContext.current
    LaunchedEffect(settings.site) {
        runCatching { context.container.api.appInfo(settings.site) }.onSuccess {
            shell.info = it
            admin.servicesEnabled = it.services
            if (it.name.isNotBlank()) context.container.prefs.setSiteName(it.name)
        }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = buildList {
        add(Tab(AdminRoutes.HOME, "Home", Icons.Outlined.Dashboard, Icons.Filled.Dashboard))
        if (settings.can(Perm.RENTALS)) add(Tab(AdminRoutes.RENTALS, "Rentals", Icons.Outlined.Timer, Icons.Filled.Timer))
        if (settings.can(Perm.SERVICES) && admin.servicesEnabled) add(Tab(AdminRoutes.ORDERS, "Orders", Icons.Outlined.SupportAgent, Icons.Filled.SupportAgent) { admin.openOrders })
        if (settings.can(Perm.RESELLERS)) add(Tab(AdminRoutes.RESELLERS, "Resellers", Icons.Outlined.Groups, Icons.Filled.Groups) { admin.pendingRegistrations })
        add(Tab(AdminRoutes.MORE, "More", Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz) { shell.unread })
    }
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        val badge = tab.badge()
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.goTab(tab.route) },
                            icon = {
                                if (badge > 0) {
                                    BadgedBox(badge = { Badge { Text(if (badge > 99) "99+" else "$badge") } }) {
                                        Icon(if (selected) tab.selectedIcon else tab.icon, tab.label)
                                    }
                                } else Icon(if (selected) tab.selectedIcon else tab.icon, tab.label)
                            },
                            label = { Text(tab.label, maxLines = 1) },
                            modifier = Modifier.testTag("tab_${tab.label.lowercase()}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(bottom = if (showBar) pad.calculateBottomPadding() else 0.dp)) {
            NavHost(
                navController = nav,
                startDestination = AdminRoutes.HOME,
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 12 } },
            ) {
                composable(AdminRoutes.HOME) { AdminHomeScreen(nav, shell, admin, settings) }
                composable(AdminRoutes.RENTALS) { AdminRentalsScreen(nav, admin) }
                composable(AdminRoutes.ORDERS) { AdminOrdersScreen(nav, admin) }
                composable(AdminRoutes.RESELLERS) { AdminResellersScreen(nav, admin) }
                composable(AdminRoutes.MORE) { AccountScreen(nav, shell, settings) }
                composable(AdminRoutes.TOOLS) { AdminToolsScreen(nav) }
                composable(AdminRoutes.NOTIFICATIONS) { NotificationsScreen(nav, shell, admin = true) }
                composable(AdminRoutes.RENTAL, arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                    AdminRentalScreen(nav, it.arguments?.getInt("id") ?: 0)
                }
                composable(AdminRoutes.ORDER, arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                    AdminOrderScreen(nav, it.arguments?.getInt("id") ?: 0)
                }
                composable(AdminRoutes.RESELLER, arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                    AdminResellerScreen(nav, settings, it.arguments?.getInt("id") ?: 0)
                }
                composable(AdminRoutes.SLOTS, arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                    AdminSlotsScreen(nav, it.arguments?.getInt("id") ?: 0)
                }
            }
        }
    }
}

/** Open a tab with a filter already picked (e.g. Resellers → Pending). */
fun NavHostController.goFiltered(admin: AdminShellState, route: String, filter: String) {
    when (route) {
        AdminRoutes.RESELLERS -> admin.resellerFilter = filter
        AdminRoutes.ORDERS -> admin.orderFilter = filter
        AdminRoutes.RENTALS -> admin.rentalFilter = filter
    }
    goTab(route)
}
