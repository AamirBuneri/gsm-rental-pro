package com.aamirbuneri.abgsmrental.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAlarm
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.AdminRental
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.components.CountdownRing
import com.aamirbuneri.abgsmrental.ui.components.CredentialRow
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.InfoRow
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.ItemAvatar
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.SecondaryButton
import com.aamirbuneri.abgsmrental.ui.components.StatusPill
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.components.rememberNow
import com.aamirbuneri.abgsmrental.ui.components.rentalLabel
import com.aamirbuneri.abgsmrental.ui.components.rentalTone
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.ago
import com.aamirbuneri.abgsmrental.ui.util.copy
import com.aamirbuneri.abgsmrental.ui.util.dateTime
import com.aamirbuneri.abgsmrental.ui.util.duration
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.shortDate
import com.aamirbuneri.abgsmrental.ui.util.timeOnly
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRentalsScreen(nav: NavHostController, admin: AdminShellState) {
    val api = LocalContext.current.container.api
    val vm: PagedVM<AdminRental> = viewModel(key = "a_rentals") {
        PagedVM("active", { it.id }) { f, q, p -> api.adminRentals(f, q, p) }
    }
    LaunchedEffect(admin.rentalFilter) {
        admin.rentalFilter?.let { vm.pick(it); admin.rentalFilter = null }
    }
    LaunchedEffect(Unit) { while (true) { delay(45_000); vm.load() } }
    val c = vm.counts
    ScreenBackground {
        PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ScreenTitle("Rentals", if (vm.items != null) "${vm.total} ${if (vm.filter == "active") "running" else "shown"}" else null)
                        SearchBox(vm.query, "Rental number, tool, slot or renter", { vm.search(it) }, Modifier.padding(horizontal = 16.dp))
                        FilterRow(
                            listOf(
                                FilterOption("active", "Running", c["active"]),
                                FilterOption("expired", "Ended", c["expired"]),
                                FilterOption("closed", "Closed", c["closed"]),
                                FilterOption("returned", "Returned", c["returned"]),
                                FilterOption("all", "All", c["all"]),
                            ),
                            vm.filter, { vm.pick(it) },
                        )
                    }
                }
                val list = vm.items
                val err = vm.error
                when {
                    list == null && err != null -> item { ErrorState(err.message ?: "", err.offline) { vm.load() } }
                    list == null -> item { LoadingCards(4, 96.dp) }
                    list.isEmpty() -> item {
                        EmptyState(Icons.Outlined.Timer, if (vm.filter == "active") "Nothing running" else "No rentals here", if (vm.query.isNotBlank()) "Nothing matches “${vm.query}”." else "Rentals show up here as resellers rent tools.")
                    }
                    else -> {
                        if (err != null) item { InlineError(err.message, Modifier.padding(horizontal = 16.dp)) }
                        items(list, key = { it.id }) { r -> RentalRow(r, Modifier.padding(horizontal = 16.dp)) { nav.navigate(AdminRoutes.rental(r.id)) } }
                        if (vm.hasMore) item { LoadMore(vm.loadingMore) { vm.more() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun RentalRow(r: AdminRental, modifier: Modifier, onClick: () -> Unit) {
    val now = rememberNow()
    val running = r.status == "active" && r.endsAtMs > now
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (running) CountdownRing(r.endsAtMs, r.minutes, size = 60.dp, stroke = 5.dp)
            else ItemAvatar(r.tool, r.color, r.image, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.tool, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${r.renter.ifBlank { "—" }} · ${r.slot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (running) "Ends ${timeOnly(r.endsAtMs)} · ${r.plan}" else "${r.plan} · ${shortDate(r.startedAt)}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(rentalLabel(if (running) "active" else if (r.status == "active") "expired" else r.status), rentalTone(if (running) "active" else r.status))
                Spacer(Modifier.height(4.dp))
                Text(money(r.price, r.currency), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRentalScreen(nav: NavHostController, id: Int) {
    val api = LocalContext.current.container.api
    val vm: DetailVM<AdminRental> = viewModel(key = "a_rental-$id") { DetailVM { api.adminRental(id) } }
    val toast = rememberToast()
    var extend by remember { mutableStateOf(false) }
    var close by remember { mutableStateOf(false) }
    val state = vm.state
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text((state as? Load.Ok)?.data?.let { it.tool } ?: "Rental", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is Load.Loading -> LoadingCards(3, 140.dp)
                    is Load.Err -> ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                    is Load.Ok -> Detail(nav, state.data, vm.refreshError, vm.busy, { extend = true }, { close = true })
                }
            }
        }
    }
    if (extend) ExtendDialog(onDismiss = { extend = false }) { m ->
        extend = false
        vm.act(toast, { it.message }) { api.adminExtend(id, m) }
    }
    if (close) ConfirmDialog(
        title = "Close this rental now?",
        text = "The renter loses access and the slot is freed. No refund is made here — for a refund use a return on the website. Change the slot password afterwards.",
        confirm = "Close rental", danger = true, reasonLabel = "Reason (optional)",
        onDismiss = { close = false },
    ) { reason ->
        close = false
        vm.act(toast, { it.message }) { api.adminCloseRental(id, reason.ifBlank { "Closed in the app" }) }
    }
}

@Composable
private fun Detail(nav: NavHostController, r: AdminRental, refreshError: String?, busy: Boolean, onExtend: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val now = rememberNow()
    val running = r.status == "active" && r.endsAtMs > now
    val b = AB.brand
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InlineError(refreshError)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (running) {
                    CountdownRing(r.endsAtMs, r.minutes, size = 150.dp, stroke = 11.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Ends at ${timeOnly(r.endsAtMs)}", style = MaterialTheme.typography.titleMedium)
                } else {
                    ItemAvatar(r.tool, r.color, r.image, 72.dp)
                    Spacer(Modifier.height(12.dp))
                    StatusPill(rentalLabel(if (r.status == "active") "expired" else r.status), rentalTone(r.status))
                }
                Spacer(Modifier.height(6.dp))
                Text("${r.tool} · ${r.slot} · ${r.plan}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile(if (running) "Extend" else "Reopen", Icons.Outlined.AddAlarm, b.success, Modifier.weight(1f), enabled = !busy && r.status != "returned", onClick = onExtend)
            ActionTile("Close now", Icons.Outlined.Block, b.danger, Modifier.weight(1f), enabled = !busy && running, onClick = onClose)
            ActionTile("Renter", Icons.Outlined.Person, b.info, Modifier.weight(1f), enabled = r.resellerId != null) { r.resellerId?.let { nav.navigate(AdminRoutes.reseller(it)) } }
        }
        val login = r.login
        if (running && login != null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Renter’s login", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StatusPill("In use", Tone.Success)
                }
                Spacer(Modifier.height(4.dp))
                Text("The login this renter got. A new slot password saved later stays hidden from them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (login.username.isNotBlank()) CredentialRow("Username", login.username)
                    if (login.email.isNotBlank() && login.email != login.username) CredentialRow("Email", login.email)
                    if (login.password.isNotBlank()) CredentialRow("Password", login.password, secret = true)
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Copy all", {
                    copy(context, "Login", listOfNotNull(
                        login.username.takeIf { it.isNotBlank() }?.let { "Username: $it" },
                        login.email.takeIf { it.isNotBlank() && it != login.username }?.let { "Email: $it" },
                        login.password.takeIf { it.isNotBlank() }?.let { "Password: $it" },
                    ).joinToString("\n"), sensitive = true)
                }, Modifier.fillMaxWidth(), icon = Icons.Outlined.ContentCopy)
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            InfoRow("Rental", r.number)
            InfoRow("Renter", listOf(r.renter, if (r.reseller.isNotBlank()) "@${r.reseller}" else if (r.walkIn) "walk-in" else "").filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" })
            InfoRow("Tool / slot", "${r.tool} · ${r.slot}")
            InfoRow("Plan", listOf(r.plan, duration(r.minutes)).filter { it.isNotBlank() }.distinctBy { it.lowercase() }.joinToString(" · "))
            InfoRow("Price", money(r.price, r.currency), valueColor = MaterialTheme.colorScheme.primary)
            InfoRow("Started", dateTime(r.startedAt))
            InfoRow("Ends", dateTime(r.expiresAt))
            if (r.closeReason.isNotBlank()) InfoRow("Closed because", r.closeReason)
            if (r.notes.isNotBlank()) InfoRow("Note", r.notes)
        }
        if (r.history.isNotEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Notes", style = MaterialTheme.typography.titleMedium)
                r.history.forEach { h ->
                    Spacer(Modifier.height(10.dp))
                    Text(h.body, style = MaterialTheme.typography.bodyMedium)
                    Text("${h.by} · ${ago(h.at)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
