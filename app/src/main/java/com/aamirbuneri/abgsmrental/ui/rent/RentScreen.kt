package com.aamirbuneri.abgsmrental.ui.rent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.Api
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.Plan
import com.aamirbuneri.abgsmrental.data.Rental
import com.aamirbuneri.abgsmrental.data.Tool
import com.aamirbuneri.abgsmrental.ui.DataVM
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.Routes
import com.aamirbuneri.abgsmrental.ui.components.CredentialRow
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.ItemAvatar
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.StatusPill
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.duration
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.shortDate
import com.aamirbuneri.abgsmrental.ui.util.timeOnly
import com.aamirbuneri.abgsmrental.work.Reminders
import kotlinx.coroutines.launch

class RentAction(private val api: Api) : ViewModel() {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var result by mutableStateOf<Rental?>(null)

    fun rent(tool: Tool, plan: Plan, onDone: (Rental) -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true; error = null
            try {
                val r = api.rent(tool.id, plan.id)
                result = r
                onDone(r)
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    fun reset() {
        error = null; result = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentScreen(nav: NavHostController) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: DataVM<List<Tool>> = viewModel(key = "tools") { DataVM { api.tools() } }
    val action: RentAction = viewModel(key = "rent-action") { RentAction(api) }
    var query by rememberSaveable { mutableStateOf("") }
    var openToolId by rememberSaveable { mutableStateOf<Int?>(null) }
    val state = vm.state

    ScreenBackground {
        PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                        Text("Rent a tool", style = MaterialTheme.typography.headlineMedium)
                        Text("Pick a tool and a plan — paid from your wallet, login ready instantly.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                when (state) {
                    is Load.Loading -> item { LoadingCards(5, 92.dp) }
                    is Load.Err -> item { ErrorState(state.error.message ?: "", state.error.offline) { vm.refresh() } }
                    is Load.Ok -> {
                        val tools = state.data
                        if (tools.size > 4) item {
                            OutlinedTextField(
                                value = query, onValueChange = { query = it },
                                placeholder = { Text("Search tools") },
                                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear") } },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedBorderColor = AB.brand.cardBorder,
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                            )
                        }
                        val shown = tools.filter { query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true) }
                        if (tools.isEmpty()) item { EmptyState(Icons.Outlined.Build, "No tools yet", "The admin hasn’t added any tools for rent.") }
                        else if (shown.isEmpty()) item { EmptyState(Icons.Outlined.Search, "Nothing found", "No tool matches “$query”.") }
                        items(shown, key = { it.id }) { t ->
                            ToolCard(t, Modifier.padding(horizontal = 16.dp)) { action.reset(); openToolId = t.id }
                        }
                    }
                }
            }
        }
    }

    val tool = (state as? Load.Ok)?.data?.firstOrNull { it.id == openToolId }
    if (tool != null) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { openToolId = null; if (action.result != null) vm.refresh(); action.reset() },
            sheetState = sheet,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            RentSheet(
                tool = tool,
                action = action,
                onRent = { plan -> action.rent(tool, plan) { r -> Reminders.sync(context, listOf(r)) } },
                onOpenRental = { id ->
                    scope.launch { sheet.hide() }.invokeOnCompletion {
                        openToolId = null; action.reset(); vm.refresh()
                        nav.navigate(Routes.rental(id))
                    }
                },
                onClose = {
                    scope.launch { sheet.hide() }.invokeOnCompletion { openToolId = null; if (action.result != null) vm.refresh(); action.reset() }
                },
            )
        }
    }
}

@Composable
private fun ToolCard(t: Tool, modifier: Modifier, onClick: () -> Unit) {
    val from = t.plans.minByOrNull { it.price }
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemAvatar(t.name, t.color, t.image, 54.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                if (t.description.isNotBlank()) {
                    Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (t.available) StatusPill(if (t.freeSlots != null) "${t.freeSlots} free" else "Available", Tone.Success)
                    else StatusPill(t.nextFreeAt?.let { "Busy · free ~${shortDate(it)}" } ?: "Busy", Tone.Warning)
                    Spacer(Modifier.width(8.dp))
                    if (from != null) Text("from ${money(from.price, from.currency)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RentSheet(tool: Tool, action: RentAction, onRent: (Plan) -> Unit, onOpenRental: (Int) -> Unit, onClose: () -> Unit) {
    var planId by rememberSaveable(tool.id) { mutableStateOf(tool.plans.firstOrNull()?.id) }
    val plan = tool.plans.firstOrNull { it.id == planId }
    AnimatedContent(
        targetState = action.result,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
        label = "rent",
    ) { result ->
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            if (result == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ItemAvatar(tool.name, tool.color, tool.image, 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tool.name, style = MaterialTheme.typography.titleLarge)
                        Text(if (tool.available) "Ready to rent" else "All accounts are busy right now", style = MaterialTheme.typography.bodySmall, color = if (tool.available) AB.brand.success else AB.brand.warning)
                    }
                }
                if (tool.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(tool.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(20.dp))
                Text("Choose a plan", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                if (tool.plans.isEmpty()) {
                    Text("No plans in your currency yet. Ask the admin.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                tool.plans.forEach { p ->
                    PlanOption(p, selected = p.id == planId) { planId = p.id }
                    Spacer(Modifier.height(10.dp))
                }
                InlineError(action.error)
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    text = if (plan != null) "Rent now · ${money(plan.price, plan.currency)}" else "Rent now",
                    onClick = { if (plan != null) onRent(plan) },
                    modifier = Modifier.fillMaxWidth(),
                    loading = action.busy,
                    enabled = plan != null && tool.available,
                )
                Spacer(Modifier.height(8.dp))
                Text("Paid from your wallet. The login appears here right away.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                RentSuccess(result, onOpenRental, onClose)
            }
        }
    }
}

@Composable
private fun PlanOption(p: Plan, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else AB.brand.cardBorder
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainer)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).border(2.dp, border, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.label, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(duration(p.minutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(money(p.price, p.currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RentSuccess(r: Rental, onOpenRental: (Int) -> Unit, onClose: () -> Unit) {
    val pop = remember { androidx.compose.animation.core.Animatable(0.4f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(78.dp).scale(pop.value).clip(CircleShape).background(AB.brand.gradient),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(42.dp)) }
        Spacer(Modifier.height(14.dp))
        Text("${r.tool} is ready", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text("${r.plan} · ends at ${timeOnly(r.endsAtMs)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (r.balance != null) {
            Spacer(Modifier.height(4.dp))
            Text("New balance ${money(r.balance, r.currency)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(Modifier.height(20.dp))
    val login = r.login
    if (login != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (login.username.isNotBlank()) CredentialRow("Username", login.username)
            if (login.email.isNotBlank() && login.email != login.username) CredentialRow("Email", login.email)
            if (login.password.isNotBlank()) CredentialRow("Password", login.password, secret = true, initiallyShown = true)
        }
        Spacer(Modifier.height(10.dp))
        Text("Don’t change the account’s password or settings. It stops working when the rental ends.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(20.dp))
    GradientButton("Open rental", { onOpenRental(r.id) }, Modifier.fillMaxWidth())
    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}
