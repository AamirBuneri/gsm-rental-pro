package com.aamirbuneri.abgsmrental.ui.rentals

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.Rental
import com.aamirbuneri.abgsmrental.ui.DataVM
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.Routes
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
import com.aamirbuneri.abgsmrental.ui.components.rememberNow
import com.aamirbuneri.abgsmrental.ui.components.rentalLabel
import com.aamirbuneri.abgsmrental.ui.components.rentalTone
import com.aamirbuneri.abgsmrental.ui.goTab
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.copy
import com.aamirbuneri.abgsmrental.ui.util.dateTime
import com.aamirbuneri.abgsmrental.ui.util.duration
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.shortDate
import com.aamirbuneri.abgsmrental.ui.util.timeOnly
import com.aamirbuneri.abgsmrental.work.Reminders
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsScreen(nav: NavHostController) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: DataVM<List<Rental>> = viewModel(key = "rentals") { DataVM { api.rentals("all", 100) } }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val state = vm.state
    LaunchedEffect(Unit) {
        while (true) { delay(45_000); vm.refresh() }
    }
    LaunchedEffect(state) {
        if (state is Load.Ok) Reminders.sync(context, state.data.filter { it.running })
    }
    ScreenBackground {
        PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                        Text("My rentals", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(14.dp))
                        Segmented(listOf("Running", "History"), tab) { tab = it }
                    }
                }
                when (state) {
                    is Load.Loading -> item { LoadingCards(4, 100.dp) }
                    is Load.Err -> item { ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() } }
                    is Load.Ok -> {
                        if (vm.refreshError != null) item { InlineError(vm.refreshError, Modifier.padding(horizontal = 16.dp)) }
                        val running = state.data.filter { it.running }
                        val history = state.data.filter { !it.running }
                        if (tab == 0) {
                            if (running.isEmpty()) item {
                                EmptyState(Icons.Outlined.Timer, "Nothing running", "Rent a tool and it shows up here with a live timer.", action = "Rent a tool") { nav.goTab(Routes.RENT) }
                            }
                            items(running, key = { it.id }) { r -> RunningRental(r, Modifier.padding(horizontal = 16.dp)) { nav.navigate(Routes.rental(r.id)) } }
                        } else {
                            if (history.isEmpty()) item { EmptyState(Icons.Outlined.History, "No history yet", "Finished rentals are listed here.") }
                            items(history, key = { it.id }) { r -> HistoryRow(r, Modifier.padding(horizontal = 16.dp)) { nav.navigate(Routes.rental(r.id)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (on) Modifier.background(AB.brand.gradient) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RunningRental(r: Rental, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CountdownRing(r.endsAtMs, r.minutes, size = 84.dp, stroke = 7.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(r.tool, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${r.plan} · ${money(r.price, r.currency)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Ends ${timeOnly(r.endsAtMs)}", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Key, null, Modifier.height(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(6.dp))
                    Text("Show login", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(r: Rental, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemAvatar(r.tool, r.color, null, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.tool, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${r.plan} · ${shortDate(r.startedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(rentalLabel(r.status), rentalTone(r.status))
                Spacer(Modifier.height(4.dp))
                Text(money(r.price, r.currency), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalDetailScreen(nav: NavHostController, id: Int) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: DataVM<Rental> = viewModel(key = "rental-$id") { DataVM { api.rental(id) } }
    val state = vm.state
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text((state as? Load.Ok)?.data?.tool ?: "Rental") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                when (state) {
                    is Load.Loading -> LoadingCards(3, 140.dp)
                    is Load.Err -> ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() }
                    is Load.Ok -> RentalDetail(state.data, vm.refreshError) { nav.navigate(com.aamirbuneri.abgsmrental.ui.panel.PanelCtx.route("returnreq", id, "${state.data.tool} · ${state.data.number}")) }
                }
            }
        }
    }
}

@Composable
private fun RentalDetail(r: Rental, refreshError: String?, onProblem: () -> Unit = {}) {
    val context = LocalContext.current
    val now = rememberNow()
    val running = r.status == "active" && r.endsAtMs > now
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InlineError(refreshError)
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (running) {
                    CountdownRing(r.endsAtMs, r.minutes, size = 168.dp, stroke = 12.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("Ends at ${timeOnly(r.endsAtMs)}", style = MaterialTheme.typography.titleMedium)
                } else {
                    ItemAvatar(r.tool, r.color, null, 72.dp)
                    Spacer(Modifier.height(12.dp))
                    StatusPill(rentalLabel(r.status), rentalTone(r.status))
                    Spacer(Modifier.height(8.dp))
                    Text("This rental has ended — the login no longer works.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text("${r.tool} · ${r.plan}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val login = r.login
        if (running && login != null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tool login", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StatusPill("Active", com.aamirbuneri.abgsmrental.ui.components.Tone.Success)
                }
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (login.username.isNotBlank()) CredentialRow("Username", login.username)
                    if (login.email.isNotBlank() && login.email != login.username) CredentialRow("Email", login.email)
                    if (login.password.isNotBlank()) CredentialRow("Password", login.password, secret = true)
                }
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Copy all", {
                    val text = buildString {
                        if (login.username.isNotBlank()) appendLine("Username: ${login.username}")
                        if (login.email.isNotBlank() && login.email != login.username) appendLine("Email: ${login.email}")
                        if (login.password.isNotBlank()) append("Password: ${login.password}")
                    }.trim()
                    copy(context, "Login", text, sensitive = true)
                }, Modifier.fillMaxWidth(), icon = Icons.Outlined.ContentCopy)
                Spacer(Modifier.height(8.dp))
                Text("Please don’t change the account’s password or settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            InfoRow("Order", r.number)
            InfoRow("Plan", listOf(r.plan, duration(r.minutes)).filter { it.isNotBlank() }.distinctBy { it.lowercase() }.joinToString(" · "))
            InfoRow("Price", money(r.price, r.currency), valueColor = MaterialTheme.colorScheme.primary)
            InfoRow("Started", dateTime(r.startedAt))
            InfoRow("Ends", dateTime(r.expiresAt))
            InfoRow("Status", rentalLabel(if (running) "active" else r.status))
        }
        if (running) {
            SecondaryButton("Didn’t work — ask for a refund", onProblem, Modifier.fillMaxWidth().testTag("rental_problem"))
        }
    }
}
