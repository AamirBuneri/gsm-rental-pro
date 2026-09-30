package com.aamirbuneri.abgsmrental.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.AdminSlot
import com.aamirbuneri.abgsmrental.data.AdminTool
import com.aamirbuneri.abgsmrental.data.ToolLogin
import com.aamirbuneri.abgsmrental.data.ToolSlots
import com.aamirbuneri.abgsmrental.ui.DataVM
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.components.CredentialRow
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.ItemAvatar
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.StatusPill
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.components.rememberNow
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.clock
import com.aamirbuneri.abgsmrental.ui.util.parseIso
import com.aamirbuneri.abgsmrental.ui.util.shortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminToolsScreen(nav: NavHostController) {
    val api = LocalContext.current.container.api
    val vm: DataVM<List<AdminTool>> = viewModel(key = "a_tools") { DataVM { api.adminTools() } }
    val state = vm.state
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Tools & slots") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is Load.Loading -> LoadingCards(4, 96.dp)
                    is Load.Err -> ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                    is Load.Ok -> if (state.data.isEmpty()) EmptyState(Icons.Outlined.Build, "No tools yet", "Add tools and slots on the website (Admin → Tools).")
                    else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (vm.refreshError != null) item { InlineError(vm.refreshError) }
                        items(state.data, key = { it.id }) { t -> ToolCard(t) { nav.navigate(AdminRoutes.slots(t.id)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(t: AdminTool, onClick: () -> Unit) {
    val b = AB.brand
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemAvatar(t.name, t.color, t.image, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${t.free} free · ${t.busy} busy" + (if (t.disabled > 0) " · ${t.disabled} off" else ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!t.active) StatusPill("Hidden", Tone.Neutral)
            else if (t.expiring > 0) StatusPill("${t.expiring} expiring", Tone.Danger)
            else if (t.free == 0 && t.total > 0) StatusPill("Full", Tone.Warning)
        }
        Spacer(Modifier.height(12.dp))
        // slot usage bar
        val total = t.total.coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.outlineVariant)) {
            if (t.busy > 0) Box(Modifier.weight(t.busy.toFloat() / total).fillMaxHeight().background(b.warning))
            if (t.free > 0) Box(Modifier.weight(t.free.toFloat() / total).fillMaxHeight().background(b.success))
            val rest = total - t.busy - t.free
            if (rest > 0) Box(Modifier.weight(rest.toFloat() / total).fillMaxHeight())
        }
    }
}

private sealed interface SlotDialog {
    data object None : SlotDialog
    data class Password(val slot: AdminSlot) : SlotDialog
    data class Toggle(val slot: AdminSlot) : SlotDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSlotsScreen(nav: NavHostController, toolId: Int) {
    val api = LocalContext.current.container.api
    val vm: DetailVM<ToolSlots> = viewModel(key = "a_slots-$toolId") { DetailVM { api.adminSlots(toolId) } }
    val toast = rememberToast()
    var dialog by remember { mutableStateOf<SlotDialog>(SlotDialog.None) }
    var shown by remember { mutableStateOf<Map<Int, ToolLogin>>(emptyMap()) }
    val state = vm.state
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text((state as? Load.Ok)?.data?.tool?.name ?: "Slots", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is Load.Loading -> LoadingCards(4, 120.dp)
                    is Load.Err -> ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                    is Load.Ok -> if (state.data.slots.isEmpty()) EmptyState(Icons.Outlined.Key, "No slots", "Add slots for this tool on the website.")
                    else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (vm.refreshError != null) item { InlineError(vm.refreshError) }
                        items(state.data.slots, key = { it.id }) { s ->
                            SlotCard(
                                s, shown[s.id], vm.busy,
                                onShow = {
                                    if (shown.containsKey(s.id)) shown = shown - s.id
                                    else vm.side(toast, { api.adminSlotSecret(s.id) }) { login -> shown = shown + (s.id to login) }
                                },
                                onPassword = { dialog = SlotDialog.Password(s) },
                                onToggle = { dialog = SlotDialog.Toggle(s) },
                                onRental = { s.rentalId?.let { nav.navigate(AdminRoutes.rental(it)) } },
                            )
                        }
                    }
                }
            }
        }
    }
    when (val d = dialog) {
        SlotDialog.None -> {}
        is SlotDialog.Password -> PasswordDialog(
            d.slot.name,
            if (d.slot.status == "busy") "In use by ${d.slot.renter ?: "a renter"} — they keep the old login until their rental ends." else null,
            onDismiss = { dialog = SlotDialog.None },
        ) { pw ->
            dialog = SlotDialog.None
            shown = shown - d.slot.id
            vm.act(toast, { null }) {
                val saved = api.adminSlotPassword(d.slot.id, pw)
                toast(saved.message)
                api.adminSlots(toolId)
            }
        }
        is SlotDialog.Toggle -> ConfirmDialog(
            if (d.slot.status == "disabled") "Enable ${d.slot.name}?" else "Disable ${d.slot.name}?",
            if (d.slot.status == "disabled") "It can be rented again." else "Nobody can rent it until you enable it again.",
            if (d.slot.status == "disabled") "Enable" else "Disable",
            danger = d.slot.status != "disabled",
            onDismiss = { dialog = SlotDialog.None },
        ) {
            dialog = SlotDialog.None
            vm.act(toast, { null }) {
                val saved = api.adminSlotToggle(d.slot.id)
                toast(saved.message)
                api.adminSlots(toolId)
            }
        }
    }
}

@Composable
private fun SlotCard(s: AdminSlot, login: ToolLogin?, busy: Boolean, onShow: () -> Unit, onPassword: () -> Unit, onToggle: () -> Unit, onRental: () -> Unit) {
    val b = AB.brand
    val now = rememberNow()
    val (label, tone) = when (s.status) {
        "available" -> "Free" to Tone.Success
        "busy" -> "In use" to Tone.Warning
        "disabled" -> "Off" to Tone.Neutral
        "expired" -> "Account expired" to Tone.Danger
        else -> s.status to Tone.Neutral
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.username.ifBlank { s.email }.ifBlank { "No username saved" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusPill(label, tone)
        }
        if (s.status == "busy" && s.renter != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(b.warning.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Timer, null, tint = b.warning, modifier = Modifier.padding(end = 8.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.renter, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.rentalNumber.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(clock(((s.endsAtMs - now) / 1000).coerceAtLeast(0)), style = MaterialTheme.typography.titleSmall, color = b.warning)
                TextButton(onClick = onRental) { Text("Open") }
            }
        }
        s.accountExpiresAt?.let { exp ->
            val soon = (parseIso(exp)?.time ?: Long.MAX_VALUE) - now < 7L * 86_400_000L
            Spacer(Modifier.height(8.dp))
            Text("Tool account valid until ${shortDate(exp)}", style = MaterialTheme.typography.labelMedium, color = if (soon) b.danger else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (login != null) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (login.username.isNotBlank()) CredentialRow("Username", login.username)
                if (login.email.isNotBlank() && login.email != login.username) CredentialRow("Email", login.email)
                CredentialRow("Password", login.password.ifBlank { "—" }, secret = true, initiallyShown = true)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(if (login != null) "Hide login" else "Show login", Icons.Outlined.Key, b.info, Modifier.weight(1f), enabled = !busy, onClick = onShow)
            ActionTile("New password", Icons.Outlined.Password, b.warning, Modifier.weight(1f), enabled = !busy, onClick = onPassword)
            ActionTile(if (s.status == "disabled") "Enable" else "Disable", Icons.Outlined.PowerSettingsNew, if (s.status == "disabled") b.success else b.danger, Modifier.weight(1f), enabled = !busy && s.status != "busy", onClick = onToggle)
        }
    }
}
