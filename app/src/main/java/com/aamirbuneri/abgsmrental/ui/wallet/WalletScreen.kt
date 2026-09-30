package com.aamirbuneri.abgsmrental.ui.wallet

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ReceiptLong
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.Api
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.LedgerEntry
import com.aamirbuneri.abgsmrental.data.WalletPage
import com.aamirbuneri.abgsmrental.ui.components.EmptyState
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.IconBadge
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.money
import com.aamirbuneri.abgsmrental.ui.util.openWhatsApp
import com.aamirbuneri.abgsmrental.ui.util.shortDate
import kotlinx.coroutines.launch

class WalletVM(private val api: Api) : ViewModel() {
    var page by mutableStateOf<WalletPage?>(null)
        private set
    var items by mutableStateOf<List<LedgerEntry>>(emptyList())
        private set
    var error by mutableStateOf<ApiException?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var end by mutableStateOf(false)
        private set

    init { refresh() }

    fun refresh(pull: Boolean = false) {
        viewModelScope.launch {
            if (pull) refreshing = true
            try {
                val p = api.wallet(limit = 30)
                page = p; items = p.items; end = p.items.size < 30; error = null
            } catch (e: ApiException) {
                error = e
            } finally {
                refreshing = false
            }
        }
    }

    fun more() {
        if (loadingMore || end) return
        val last = items.lastOrNull()?.id ?: return
        viewModelScope.launch {
            loadingMore = true
            try {
                val p = api.wallet(beforeId = last, limit = 30)
                items = items + p.items; end = p.items.size < 30
            } catch (e: ApiException) {
                error = e
            } finally {
                loadingMore = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(nav: NavHostController) {
    val context = LocalContext.current
    val api = context.container.api
    val vm: WalletVM = viewModel(key = "wallet") { WalletVM(api) }
    val prefs = context.container.prefs
    val username by androidx.compose.runtime.produceState("", prefs) { value = prefs.snapshot().username }
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Wallet") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.refresh(pull = true) }, modifier = Modifier.fillMaxSize()) {
                val page = vm.page
                val err = vm.error
                when {
                    page == null && err != null -> ErrorState(err.message ?: "", err.offline) { vm.refresh() }
                    page == null -> LoadingCards(5, 70.dp)
                    else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(AB.brand.gradient).padding(22.dp),
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                                        Text("Balance", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(money(page.balance, page.currency), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                                    val sub = when {
                                        page.canSpend == null -> "Unlimited credit"
                                        page.creditAllowed && (page.creditLimit ?: 0.0) > 0 -> "Credit limit ${money(page.creditLimit ?: 0.0, page.currency)} · can spend ${money(page.canSpend ?: 0.0, page.currency)}"
                                        else -> "Top up to rent tools and order services"
                                    }
                                    Text(sub, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (page.topupWhatsapp.isNotBlank()) item {
                            GradientButton("Request top-up on WhatsApp", {
                                openWhatsApp(context, page.topupWhatsapp, "Hi, I want to top up my wallet. Username: $username")
                            }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Add)
                        }
                        if (err != null) item { InlineError(err.message) }
                        item { Text("History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
                        if (vm.items.isEmpty()) item { EmptyState(Icons.Outlined.ReceiptLong, "No transactions", "Top-ups, rentals and refunds appear here.") }
                        items(vm.items, key = { it.id }) { e -> LedgerRow(e) }
                        if (!vm.end && vm.items.isNotEmpty()) item {
                            TextButton(onClick = { vm.more() }, enabled = !vm.loadingMore, modifier = Modifier.fillMaxWidth()) {
                                Text(if (vm.loadingMore) "Loading…" else "Load more")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(e: LedgerEntry) {
    val b = AB.brand
    val credit = e.isCredit && e.amount >= 0
    GlassCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (credit) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward, if (credit) b.success else b.danger, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(e.description.ifBlank { if (credit) "Credit" else "Debit" }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(shortDate(e.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text((if (credit) "+" else "−") + money(kotlin.math.abs(e.amount), e.currency), style = MaterialTheme.typography.titleSmall, color = if (credit) b.success else b.danger, fontWeight = FontWeight.Bold)
                Text("Bal. " + money(e.balanceAfter, e.currency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
