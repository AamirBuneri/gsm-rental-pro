package com.aamirbuneri.abgsmrental.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.BuildConfig
import com.aamirbuneri.abgsmrental.R
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.data.ThemeMode
import com.aamirbuneri.abgsmrental.ui.Routes
import com.aamirbuneri.abgsmrental.ui.admin.AdminRoutes
import com.aamirbuneri.abgsmrental.data.Perm
import com.aamirbuneri.abgsmrental.ui.Shell
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.IconBadge
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.goTab
import com.aamirbuneri.abgsmrental.ui.rentals.Segmented
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.dial
import com.aamirbuneri.abgsmrental.ui.util.email
import com.aamirbuneri.abgsmrental.ui.util.initials
import com.aamirbuneri.abgsmrental.ui.util.openUrl
import com.aamirbuneri.abgsmrental.ui.util.openWhatsApp
import com.aamirbuneri.abgsmrental.work.SyncWorker
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(nav: NavHostController, shell: Shell, settings: Settings) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    var confirmSignOut by remember { mutableStateOf(false) }
    val info = shell.info
    val b = AB.brand

    ScreenBackground {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Account", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 4.dp, top = 16.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(b.gradient), contentAlignment = Alignment.Center) {
                        Text(initials(settings.username), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("@" + settings.username, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            (if (settings.team) (if (settings.owner) "Owner · " else "Staff · ") else "") +
                                (info?.name?.ifBlank { null } ?: settings.siteName.ifBlank { settings.site }),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        )
                    }
                }
            }

            if (settings.team) {
                Group("Admin") {
                    Item(Icons.Outlined.Notifications, "Notifications", b.info, badge = shell.unread) { nav.navigate(AdminRoutes.NOTIFICATIONS) }
                    if (settings.can(Perm.TOOLS)) {
                        Divider()
                        Item(Icons.Outlined.Inventory2, "Tools & slots", b.warning) { nav.navigate(AdminRoutes.TOOLS) }
                    }
                    Divider()
                    Item(Icons.Outlined.AdminPanelSettings, "Full admin panel (website)", MaterialTheme.colorScheme.tertiary) {
                        openUrl(context, settings.site.trimEnd('/') + "/index.php?r=%2Fadmin")
                    }
                }
                if (!settings.owner) {
                    Text(
                        "You can use: " + settings.perms.joinToString(", ") { permLabel(it) }.ifBlank { "the dashboard" } + ". The owner changes this in Admin → Staff.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            } else {
                Group("My account") {
                    Item(Icons.Outlined.AccountBalanceWallet, "Wallet & history", b.success) { nav.navigate(Routes.WALLET) }
                    Divider()
                    Item(Icons.Outlined.Notifications, "Notifications", b.info, badge = shell.unread) { nav.navigate(Routes.NOTIFICATIONS) }
                    if (shell.services) {
                        Divider()
                        Item(Icons.Outlined.SupportAgent, "My service orders", b.warning) { nav.goTab(Routes.SERVICES) }
                    }
                }
            }

            Group("Appearance") {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.DarkMode, MaterialTheme.colorScheme.tertiary, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Theme", style = MaterialTheme.typography.titleSmall)
                }
                Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                    Segmented(listOf("System", "Light", "Dark"), modes.indexOf(settings.themeMode)) { i ->
                        scope.launch { c.prefs.setTheme(modes[i]) }
                    }
                }
                Divider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.NotificationsActive, b.warning, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Alerts on this phone", style = MaterialTheme.typography.titleSmall)
                        Text(if (settings.team) "New sign-ups, orders and alerts" else "Rental ending, order updates, wallet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.notifications,
                        onCheckedChange = { on ->
                            scope.launch {
                                c.prefs.setNotifications(on)
                                if (on) SyncWorker.schedule(context) else SyncWorker.cancel(context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }

            val support = info?.support?.takeIf { !settings.team } // the team is the support
            if (support != null && (support.whatsapp.isNotBlank() || support.phone.isNotBlank() || support.email.isNotBlank()) || info?.website?.isNotBlank() == true) {
                Group("Help") {
                    var first = true
                    if (support != null && support.whatsapp.isNotBlank()) {
                        Item(Icons.AutoMirrored.Outlined.Chat, "Chat on WhatsApp", b.success) { openWhatsApp(context, support.whatsapp, "Hi, I need help. Username: ${settings.username}") }
                        first = false
                    }
                    if (support != null && support.phone.isNotBlank()) {
                        if (!first) Divider()
                        Item(Icons.Outlined.Call, "Call ${support.phone}", b.info) { dial(context, support.phone) }
                        first = false
                    }
                    if (support != null && support.email.isNotBlank()) {
                        if (!first) Divider()
                        Item(Icons.Outlined.Email, support.email, b.warning) { email(context, support.email) }
                        first = false
                    }
                    val web = info?.website.orEmpty()
                    if (web.isNotBlank()) {
                        if (!first) Divider()
                        Item(Icons.Outlined.Language, "Open the website", MaterialTheme.colorScheme.tertiary) { openUrl(context, web) }
                    }
                }
            }

            GlassCard(Modifier.fillMaxWidth(), onClick = { confirmSignOut = true }, padding = PaddingValues(0.dp)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.AutoMirrored.Outlined.Logout, b.danger, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Sign out", style = MaterialTheme.typography.titleSmall, color = b.danger, modifier = Modifier.weight(1f))
                }
            }

            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.brand_mark), null, Modifier.width(72.dp))
                Spacer(Modifier.height(6.dp))
                Text("AB Gsm Rental · v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("By Aamir-Buneri", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text(if (settings.team) "You’ll need your password to sign in again. Alerts on this phone stop." else "You’ll need your password to sign in again. Rental reminders on this phone stop.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    scope.launch { c.signOut() }
                }) { Text("Sign out", color = b.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        GlassCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun Item(icon: ImageVector, label: String, tint: Color, badge: Int = 0, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint, 38.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (badge > 0) {
            Badge { Text(if (badge > 99) "99+" else "$badge") }
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(start = 66.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

private fun permLabel(p: String): String = when (p) {
    "rentals" -> "Rentals"; "tools" -> "Tools & slots"; "services" -> "Service orders"; "catalog" -> "Service catalog"
    "resellers" -> "Resellers"; "money" -> "Money"; "chat" -> "Chat"; "website" -> "Website"
    else -> p
}
