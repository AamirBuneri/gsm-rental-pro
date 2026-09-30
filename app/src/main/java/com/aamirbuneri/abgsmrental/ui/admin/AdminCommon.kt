package com.aamirbuneri.abgsmrental.ui.admin

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.Page
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.money
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom

// ── view-models ────────────────────────────────────────────────────────────

/** A filtered, searchable, paged list from /api/v1/admin/…. */
class PagedVM<T>(
    initialFilter: String,
    private val idOf: (T) -> Int,
    private val fetch: suspend (filter: String, q: String, page: Int) -> Page<T>,
) : ViewModel() {
    var filter by mutableStateOf(initialFilter)
        private set
    var query by mutableStateOf("")
        private set
    var items by mutableStateOf<List<T>?>(null)
        private set
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var total by mutableStateOf(0)
        private set
    var hasMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiException?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    private var page = 1
    private var job: Job? = null

    init { load() }

    fun load(pull: Boolean = false, debounceMs: Long = 0) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            if (pull) refreshing = true
            try {
                val p = fetch(filter, query, 1)
                items = p.items; page = p.page; hasMore = p.page < p.pages; total = p.total
                if (p.counts.isNotEmpty()) counts = p.counts
                error = null
            } catch (e: ApiException) {
                error = e
            } finally {
                refreshing = false
            }
        }
    }

    fun more() {
        if (loadingMore || !hasMore) return
        viewModelScope.launch {
            loadingMore = true
            try {
                val p = fetch(filter, query, page + 1)
                val seen = items.orEmpty().map(idOf).toSet()
                items = items.orEmpty() + p.items.filter { idOf(it) !in seen }
                page = p.page; hasMore = p.page < p.pages
            } catch (e: ApiException) {
                error = e
            } finally {
                loadingMore = false
            }
        }
    }

    fun setFilter(f: String) {
        if (f == filter) return
        filter = f; items = null; load()
    }

    fun search(q: String) {
        query = q; load(debounceMs = 380)
    }

    /** Show the fresh copy an action returned without reloading the list. */
    fun replace(item: T) {
        items = items?.map { if (idOf(it) == idOf(item)) item else it }
    }
}

/** One record + actions on it (each action returns the updated record). */
class DetailVM<T>(private val fetch: suspend () -> T) : ViewModel() {
    var state: Load<T> by mutableStateOf<Load<T>>(Load.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var refreshError by mutableStateOf<String?>(null)
        private set

    init { refresh() }

    fun refresh(pull: Boolean = false) {
        viewModelScope.launch {
            val had = state is Load.Ok
            if (pull) refreshing = true
            try {
                state = Load.Ok(fetch()); refreshError = null
            } catch (e: ApiException) {
                if (had) refreshError = e.message else state = Load.Err(e)
            } finally {
                refreshing = false
            }
        }
    }

    /** Run an action: show the updated record and [message] of it, or the site's error. */
    fun act(toast: (String) -> Unit, message: (T) -> String?, block: suspend () -> T) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                val r = block()
                state = Load.Ok(r)
                message(r)?.takeIf { it.isNotBlank() }?.let(toast)
            } catch (e: ApiException) {
                toast(e.message ?: "Something went wrong.")
            } finally {
                busy = false
            }
        }
    }

    /** An action that returns something else (e.g. a login to show). */
    fun <R> side(toast: (String) -> Unit, block: suspend () -> R, done: (R) -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                done(block())
            } catch (e: ApiException) {
                toast(e.message ?: "Something went wrong.")
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun rememberToast(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { msg: String -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } }
}

// ── list pieces ────────────────────────────────────────────────────────────

@Composable
fun SearchBox(value: String, placeholder: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = if (value.isNotEmpty()) {
            { IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, "Clear") } }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = AB.brand.cardBorder,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

data class FilterOption(val key: String, val label: String, val count: Int? = null)

/** Scrollable filter pills with counts. */
@Composable
fun FilterRow(options: List<FilterOption>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { o ->
            val on = o.key == selected
            Row(
                Modifier
                    .clip(CircleShape)
                    .then(
                        if (on) Modifier.background(AB.brand.gradient)
                        else Modifier.background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, AB.brand.cardBorder, CircleShape)
                    )
                    .clickable { onSelect(o.key) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("filter_${o.key}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(o.label, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else MaterialTheme.colorScheme.onSurface)
                if (o.count != null && o.count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.clip(CircleShape).background(if (on) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)).padding(horizontal = 7.dp, vertical = 1.dp),
                    ) {
                        Text(if (o.count > 999) "999+" else "${o.count}", style = MaterialTheme.typography.labelSmall, color = if (on) Color.White else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadMore(loading: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = !loading, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(if (loading) "Loading…" else "Load more")
    }
}

@Composable
fun ScreenTitle(title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (!sub.isNullOrBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── cards ──────────────────────────────────────────────────────────────────

@Composable
fun MiniStat(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, hint: String? = null, onClick: (() -> Unit)? = null) {
    GlassCard(modifier, onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            if (hint != null) Text(hint, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 7-day sales bars (today highlighted). */
@Composable
fun WeekBars(values: List<Pair<String, Double>>, currency: String, modifier: Modifier = Modifier, onBrand: Boolean = false) {
    val max = (values.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    val grow by animateFloatAsState(1f, tween(700), label = "bars")
    val track = if (onBrand) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.outlineVariant
    val barColors = if (onBrand) listOf(Color.White, Color.White.copy(alpha = 0.75f)) else AB.brand.gradientColors
    val labelColor = if (onBrand) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(86.dp)) {
            val n = values.size.coerceAtLeast(1)
            val gap = size.width * 0.035f
            val w = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, (_, v) ->
                val x = i * (w + gap)
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 3))
                val h = (size.height * (v / max).toFloat() * grow).coerceAtLeast(if (v > 0) 6f else 0f)
                if (h > 0f) drawRoundRect(
                    Brush.verticalGradient(barColors, startY = size.height - h, endY = size.height),
                    Offset(x, size.height - h), Size(w, h), CornerRadius(w / 3),
                    alpha = if (i == values.lastIndex) 1f else 0.72f,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            values.forEach { (label, _) ->
                Text(label.substringBefore(' '), style = MaterialTheme.typography.labelSmall, color = labelColor, textAlign = TextAlign.Center, modifier = Modifier.weight(1f), maxLines = 1)
            }
        }
        if (values.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text("Best day ${money(values.maxOf { it.second }, currency)}", style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

/** Small "owner" / "staff" / permission badge. */
@Composable
fun RoleBadge(text: String, tint: Color = MaterialTheme.colorScheme.secondary) {
    Box(Modifier.clip(CircleShape).background(tint.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
fun ActionTile(label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    GlassCard(modifier.then(if (enabled) Modifier else Modifier.padding(0.dp)), onClick = if (enabled) onClick else null, padding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = if (enabled) 0.15f else 0.06f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── dialogs ────────────────────────────────────────────────────────────────

@Composable
fun DialogField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    number: Boolean = false,
    lines: Int = 1,
    secret: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(!secret) }
    val eye: (@Composable () -> Unit)? = if (secret) {
        { IconButton(onClick = { shown = !shown }) { Icon(if (shown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null) } }
    } else null
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = lines == 1,
        minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else if (secret) KeyboardType.Password else KeyboardType.Text, autoCorrectEnabled = !secret),
        visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = if (secret) MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge,
        trailingIcon = trailing ?: eye,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Confirm with an optional reason. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    danger: Boolean = false,
    reasonLabel: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text)
                if (reasonLabel != null) DialogField(reason, { reason = it }, reasonLabel, lines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason.trim()) }) { Text(confirm, color = if (danger) AB.brand.danger else MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Pick how long to add to a rental. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtendDialog(onDismiss: () -> Unit, onExtend: (Int) -> Unit) {
    val presets = listOf(30 to "+30 min", 60 to "+1 hour", 180 to "+3 hours", 360 to "+6 hours", 1440 to "+1 day", 10080 to "+7 days")
    var picked by remember { mutableStateOf(60) }
    var custom by remember { mutableStateOf("") }
    val minutes = custom.toIntOrNull()?.takeIf { it > 0 } ?: picked
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extend rental") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (m, label) ->
                        val on = custom.isBlank() && picked == m
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp))
                                .then(if (on) Modifier.background(AB.brand.gradient) else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
                                .clickable { picked = m; custom = "" }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else MaterialTheme.colorScheme.onSurface) }
                    }
                }
                DialogField(custom, { v -> custom = v.filter(Char::isDigit).take(6) }, "Or minutes", number = true)
                Text("Free for the renter — no wallet charge. The renter gets a notification.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onExtend(minutes) }) { Text("Extend") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Amount + note (price quote, wallet top-up / deduction). */
@Composable
fun AmountDialog(
    title: String,
    currency: String,
    confirm: String,
    note: String = "Note (optional)",
    initial: String = "",
    extra: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit,
) {
    var amount by remember { mutableStateOf(initial) }
    var text by remember { mutableStateOf("") }
    val value = amount.replace(",", "").toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                extra?.invoke()
                DialogField(amount, { v -> amount = v.filter { it.isDigit() || it == '.' }.take(12) }, "Amount ($currency)", number = true, modifier = Modifier.testTag("amount"))
                DialogField(text, { text = it }, note, lines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { if (value != null && value > 0) onConfirm(value, text.trim()) }, enabled = value != null && value > 0) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Free text (the result sent to the reseller when an order is done). */
@Composable
fun TextDialog(title: String, label: String, confirm: String, hint: String? = null, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DialogField(text, { text = it }, label, lines = 3)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** New slot password, with a one-tap strong password. */
@Composable
fun PasswordDialog(slot: String, busyNote: String?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New password · $slot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Change it on the tool’s website first, then save the same password here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DialogField(pw, { pw = it }, "New password", secret = true, trailing = {
                    IconButton(onClick = { pw = strongPassword() }) { Icon(Icons.Outlined.AutoAwesome, "Make a strong password", tint = MaterialTheme.colorScheme.secondary) }
                })
                if (busyNote != null) Text(busyNote, style = MaterialTheme.typography.bodySmall, color = AB.brand.warning)
            }
        },
        confirmButton = { TextButton(onClick = { if (pw.isNotBlank()) onSave(pw) }, enabled = pw.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun strongPassword(length: Int = 12): String {
    val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"; val lower = "abcdefghijkmnpqrstuvwxyz"; val digits = "23456789"; val sym = "@#%*!?"
    val all = upper + lower + digits + sym
    val r = SecureRandom()
    val chars = mutableListOf(upper[r.nextInt(upper.length)], lower[r.nextInt(lower.length)], digits[r.nextInt(digits.length)], sym[r.nextInt(sym.length)])
    while (chars.size < length) chars += all[r.nextInt(all.length)]
    chars.shuffle(r)
    return chars.joinToString("")
}

/** Two-way switch used inside dialogs (Add / Deduct, Cancelled / Failed). */
@Composable
fun TwoWay(a: String, b: String, first: Boolean, onChange: (Boolean) -> Unit, tintB: Color = AB.brand.danger) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)).padding(4.dp)) {
        listOf(a to true, b to false).forEach { (label, isFirst) ->
            val on = first == isFirst
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .then(if (on) Modifier.background(if (isFirst) AB.brand.gradient else Brush.linearGradient(listOf(tintB, tintB))) else Modifier)
                    .clickable { onChange(isFirst) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Initials avatar for people. */
@Composable
fun PersonAvatar(name: String, size: Dp = 44.dp, online: Boolean = false) {
    Box {
        Box(Modifier.size(size).clip(CircleShape).background(AB.brand.gradient), contentAlignment = Alignment.Center) {
            Text(com.aamirbuneri.abgsmrental.ui.util.initials(name), color = Color.White, fontWeight = FontWeight.Bold, style = if (size > 50.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall)
        }
        if (online) Box(
            Modifier.align(Alignment.BottomEnd).size(size * 0.28f).clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(2.dp).clip(CircleShape).background(AB.brand.success),
        )
    }
}
