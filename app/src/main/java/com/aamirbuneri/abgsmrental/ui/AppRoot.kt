package com.aamirbuneri.abgsmrental.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.AppInfo
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.ui.account.AccountScreen
import com.aamirbuneri.abgsmrental.ui.admin.AdminShell
import com.aamirbuneri.abgsmrental.ui.auth.AuthFlow
import com.aamirbuneri.abgsmrental.ui.home.HomeScreen
import com.aamirbuneri.abgsmrental.ui.notifications.NotificationsScreen
import com.aamirbuneri.abgsmrental.ui.rent.RentScreen
import com.aamirbuneri.abgsmrental.ui.rentals.RentalDetailScreen
import com.aamirbuneri.abgsmrental.ui.rentals.RentalsScreen
import com.aamirbuneri.abgsmrental.ui.services.OrderDetailScreen
import com.aamirbuneri.abgsmrental.ui.services.OrderFormScreen
import com.aamirbuneri.abgsmrental.ui.services.ServicesScreen
import com.aamirbuneri.abgsmrental.ui.splash.BrandSplash
import com.aamirbuneri.abgsmrental.ui.wallet.WalletScreen

/** Small app-wide state the tabs share. */
class Shell {
    var unread by mutableIntStateOf(0)
    var services by mutableStateOf(true)
    var info by mutableStateOf<AppInfo?>(null)
}

private enum class Stage { SPLASH, AUTH, MAIN }

@Composable
fun AppRoot(settings: Settings, dark: Boolean) {
    val context = LocalContext.current
    var splashDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        context.container.signedOut.collect {
            Toast.makeText(context, "You were signed out. Please sign in again.", Toast.LENGTH_LONG).show()
        }
    }
    val stage = when {
        !splashDone -> Stage.SPLASH
        settings.site.isBlank() || !settings.signedIn -> Stage.AUTH
        else -> Stage.MAIN
    }
    AnimatedContent(
        targetState = stage,
        transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(320)) },
        label = "stage",
    ) { s ->
        when (s) {
            Stage.SPLASH -> BrandSplash(dark) { splashDone = true }
            Stage.AUTH -> AuthFlow(settings)
            Stage.MAIN -> SignedIn(settings)
        }
    }
}

object Routes {
    const val HOME = "home"
    const val RENT = "rent"
    const val RENTALS = "rentals"
    const val SERVICES = "services"
    const val ACCOUNT = "account"
    const val WALLET = "wallet"
    const val NOTIFICATIONS = "notifications"
    const val RENTAL = "rental/{id}"
    const val ORDER = "order/{id}"
    const val ORDER_NEW = "order-new/{serviceId}"
    fun rental(id: Int) = "rental/$id"
    fun order(id: Int) = "order/$id"
    fun orderNew(serviceId: Int) = "order-new/$serviceId"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

/** Resellers get the shop screens; the owner and staff get the admin screens. */
@Composable
private fun SignedIn(settings: Settings) {
    val context = LocalContext.current
    // the role / staff permissions can change on the site (e.g. the owner ticks a new area) — check once per start
    LaunchedEffect(Unit) {
        runCatching { context.container.api.me() }.onSuccess { me ->
            if (me.role.isNotBlank() && (me.role != settings.role || me.perms.toSet() != settings.perms.toSet())) context.container.prefs.setRole(me)
        }
    }
    val shell = remember(settings.team) { Shell() }
    if (settings.team) AdminShell(settings, shell) else MainShell(settings, shell)
}

@Composable
private fun MainShell(settings: Settings, shell: Shell) {
    val nav = rememberNavController()
    val context = LocalContext.current
    LaunchedEffect(settings.site) {
        runCatching { context.container.api.appInfo(settings.site) }.onSuccess {
            shell.info = it
            shell.services = it.services
            if (it.name.isNotBlank()) context.container.prefs.setSiteName(it.name)
        }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tabs = buildList {
        add(Tab(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home))
        add(Tab(Routes.RENT, "Rent", Icons.Outlined.Build, Icons.Filled.Build))
        add(Tab(Routes.RENTALS, "Rentals", Icons.Outlined.Timer, Icons.Filled.Timer))
        if (shell.services) add(Tab(Routes.SERVICES, "Services", Icons.Outlined.SupportAgent, Icons.Filled.SupportAgent))
        add(Tab(Routes.ACCOUNT, "Account", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle))
    }
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.goTab(tab.route) },
                            icon = {
                                if (tab.route == Routes.ACCOUNT && shell.unread > 0) {
                                    BadgedBox(badge = { Badge { Text(if (shell.unread > 99) "99+" else "${shell.unread}") } }) {
                                        Icon(if (selected) tab.selectedIcon else tab.icon, tab.label)
                                    }
                                } else Icon(if (selected) tab.selectedIcon else tab.icon, tab.label)
                            },
                            label = { Text(tab.label, maxLines = 1) },
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
                startDestination = Routes.HOME,
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 12 } },
            ) {
                composable(Routes.HOME) { HomeScreen(nav, shell, settings) }
                composable(Routes.RENT) { RentScreen(nav) }
                composable(Routes.RENTALS) { RentalsScreen(nav) }
                composable(Routes.SERVICES) { ServicesScreen(nav) }
                composable(Routes.ACCOUNT) { AccountScreen(nav, shell, settings) }
                composable(Routes.WALLET) { WalletScreen(nav) }
                composable(Routes.NOTIFICATIONS) { NotificationsScreen(nav, shell) }
                composable(Routes.RENTAL, arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                    RentalDetailScreen(nav, it.arguments?.getInt("id") ?: 0)
                }
                composable(Routes.ORDER, arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                    OrderDetailScreen(nav, it.arguments?.getInt("id") ?: 0)
                }
                composable(Routes.ORDER_NEW, arguments = listOf(navArgument("serviceId") { type = NavType.IntType })) {
                    OrderFormScreen(nav, it.arguments?.getInt("serviceId") ?: 0)
                }
            }
        }
    }
}

/** Switch bottom tab without stacking copies; re-tapping a tab keeps its state. */
fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

