package com.aamirbuneri.abgsmrental.ui.panel

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.aamirbuneri.abgsmrental.data.Api
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.BridgeResult
import com.aamirbuneri.abgsmrental.data.FilePart
import com.aamirbuneri.abgsmrental.data.obj
import com.aamirbuneri.abgsmrental.data.int
import com.aamirbuneri.abgsmrental.ui.Load
import com.aamirbuneri.abgsmrental.ui.components.ErrorState
import com.aamirbuneri.abgsmrental.ui.components.GlassCard
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.LoadingCards
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.StatusPill
import com.aamirbuneri.abgsmrental.ui.components.Tone
import com.aamirbuneri.abgsmrental.ui.components.toneColor
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.parseColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ── view-model for one page of the website's panel ─────────────────────────

/**
 * One website page opened through the bridge ([Api.page]) plus its forms ([Api.submit]).
 * [query] holds the page's filters (status, search, page number …).
 */
class PageVM(private val api: Api, val path: String, initial: Map<String, String> = emptyMap()) : ViewModel() {
    var query by mutableStateOf(initial)
        private set
    var state: Load<BridgeResult> by mutableStateOf<Load<BridgeResult>>(Load.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var refreshError by mutableStateOf<String?>(null)
        private set
    private var job: Job? = null

    val data: JsonElement? get() = (state as? Load.Ok)?.data?.let { it.data ?: it.json }

    init { load() }

    fun load(pull: Boolean = false, debounceMs: Long = 0) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val had = state is Load.Ok
            if (pull) refreshing = true
            try {
                val r = api.page(path, query)
                // the website sent us elsewhere (e.g. "turn on Remote services first")
                if (r.kind == "redirect") throw ApiException(409, r.flash.firstOrNull()?.message ?: "This page isn’t available right now.")
                state = Load.Ok(r); refreshError = null
            } catch (e: ApiException) {
                if (had) refreshError = e.message else state = Load.Err(e)
            } finally {
                refreshing = false
            }
        }
    }

    /** Change one filter (blank removes it); a new filter starts again at page 1. */
    fun set(key: String, value: String?) {
        var q = if (value.isNullOrBlank()) query - key else query + (key to value)
        if (key != "page") q = q - "page"
        query = q
        load(debounceMs = if (key == "q") 380 else 0)
    }

    fun submit(path: String, data: JsonObject, toast: (String) -> Unit, reload: Boolean = true, onDone: (BridgeResult) -> Unit = {}) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                val r = api.submit(path, data)
                r.message?.let(toast)
                if (reload) load()
                onDone(r)
            } catch (e: ApiException) {
                toast(e.message ?: "Something went wrong.")
            } finally {
                busy = false
            }
        }
    }

    fun submitFiles(path: String, fields: List<Pair<String, String>>, files: List<FilePart>, toast: (String) -> Unit, reload: Boolean = true, onDone: (BridgeResult) -> Unit = {}) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try {
                val r = api.submitFiles(path, fields, files)
                r.message?.let(toast)
                if (reload) load()
                onDone(r)
            } catch (e: ApiException) {
                toast(e.message ?: "Something went wrong.")
            } finally {
                busy = false
            }
        }
    }

    /** Anything else (e.g. a small JSON page like a slot's login). */
    fun <R> fetch(toast: (String) -> Unit, block: suspend (Api) -> R, done: (R) -> Unit) {
        viewModelScope.launch {
            try { done(block(api)) } catch (e: ApiException) { toast(e.message ?: "Something went wrong.") }
        }
    }
}

// ── page frame ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Panel(
    title: String,
    nav: NavHostController,
    vm: PageVM,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable () -> Unit)? = null,
    content: LazyListScope.(JsonElement?) -> Unit,
) {
    val state = vm.state
    ScreenBackground {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            PullToRefreshBox(isRefreshing = vm.refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state) {
                    is Load.Loading -> LoadingCards(4, 90.dp)
                    is Load.Err -> ErrorState(state.error.message ?: "", state.error.offline) { vm.load() }
                    is Load.Ok -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (vm.refreshError != null) item { InlineError(vm.refreshError) }
                        content(state.data.data ?: state.data.json)
                    }
                }
            }
            if (bottom != null && state is Load.Ok) {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) { bottom() }
            }
        }
    }
}

/** Full-screen editor shown over the page (no dialog window). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(
    visible: Boolean,
    title: String,
    onClose: () -> Unit,
    action: String? = null,
    busy: Boolean = false,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(visible, enter = slideInVertically { it / 3 } + fadeIn(), exit = slideOutVertically { it / 3 } + fadeOut()) {
        BackHandler(visible) { onClose() }
        ScreenBackground {
            Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                TopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close") } },
                    actions = {
                        if (action != null && onAction != null) {
                            TextButton(onClick = onAction, enabled = !busy) { Text(if (busy) "Saving…" else action, fontWeight = FontWeight.Bold) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

// ── rows and bits ──────────────────────────────────────────────────────────

/** A list row: optional leading badge, title + subtitle, value / pill on the right, tap and ⋮ menu. */
@Composable
fun Line(
    title: String,
    sub: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    pill: Pair<String, Tone>? = null,
    menu: List<Pair<String, () -> Unit>> = emptyList(),
    onClick: (() -> Unit)? = null,
) {
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(start = 14.dp, top = 12.dp, bottom = 12.dp, end = if (menu.isEmpty()) 14.dp else 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!sub.isNullOrBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (pill != null || value != null) {
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (pill != null) StatusPill(pill.first, pill.second)
                    if (value != null) {
                        if (pill != null) Spacer(Modifier.height(4.dp))
                        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor, maxLines = 1)
                    }
                }
            }
            if (menu.isNotEmpty()) MoreMenu(menu) else if (onClick != null && pill == null && value == null) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MoreMenu(items: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, "More") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { open = false; action() })
            }
        }
    }
}

/** Colored initials / dot for a tool or service. */
@Composable
fun Dot(name: String, color: String?, size: Dp = 40.dp) {
    val c = parseColor(color, MaterialTheme.colorScheme.secondary)
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(c.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Text(com.aamirbuneri.abgsmrental.ui.util.initials(name), color = c, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Section(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp))
    }
}

/** A big number tile. */
@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary, sub: String? = null) {
    GlassCard(modifier, padding = PaddingValues(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

/** "‹ Page 2 of 5 ›" for pages with a "pg" block. */
fun LazyListScope.pager(data: JsonElement?, vm: PageVM) {
    val pg = data.obj("pg") ?: return
    val page = pg.int("page", 1)
    val pages = pg.int("pages", 1)
    if (pages <= 1) return
    item {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { vm.set("page", "${page - 1}") }, enabled = page > 1) { Text("‹ Newer") }
            Text("Page $page of $pages", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { vm.set("page", "${page + 1}") }, enabled = page < pages) { Text("Older ›") }
        }
    }
}

// ── forms ──────────────────────────────────────────────────────────────────

/** Values of a form (all text, like the website's form fields; switches are "1" / "0"). */
class FormState(initial: Map<String, String> = emptyMap()) {
    val values = mutableStateMapOf<String, String>().apply { putAll(initial) }
    operator fun get(key: String): String = values[key] ?: ""
    operator fun set(key: String, value: String) { values[key] = value }
    fun on(key: String): Boolean = values[key] == "1"

    /** As JSON form data; [lists] are sent as arrays (e.g. plans). */
    fun json(lists: Map<String, List<String>> = emptyMap(), extra: Map<String, String> = emptyMap()): JsonObject = JsonObject(
        values.mapValues { JsonPrimitive(it.value) } + extra.mapValues { JsonPrimitive(it.value) } +
            lists.mapValues { (_, v) -> JsonArray(v.map { JsonPrimitive(it) }) }
    )

    fun pairs(): List<Pair<String, String>> = values.map { it.key to it.value }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.secondary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
)

@Composable
fun TextIn(
    f: FormState,
    key: String,
    label: String,
    hint: String? = null,
    lines: Int = 1,
    number: Boolean = false,
    password: Boolean = false,
    mono: Boolean = false,
    keyboard: KeyboardType? = null,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(!password) }
    val eye: (@Composable () -> Unit)? = if (password) {
        { IconButton(onClick = { shown = !shown }) { Icon(if (shown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null) } }
    } else null
    val hintText: (@Composable () -> Unit)? = if (hint != null) { { Text(hint) } } else null
    OutlinedTextField(
        value = f[key],
        onValueChange = { f[key] = it },
        label = { Text(label) },
        supportingText = hintText,
        singleLine = lines == 1,
        minLines = lines,
        trailingIcon = eye,
        visualTransformation = if (password && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard ?: if (number) KeyboardType.Decimal else if (password) KeyboardType.Password else KeyboardType.Text),
        textStyle = if (mono) MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun SwitchIn(f: FormState, key: String, label: String, sub: String? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, AB.brand.cardBorder, RoundedCornerShape(14.dp)).clickable { f[key] = if (f.on(key)) "0" else "1" }.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = f.on(key), onCheckedChange = { f[key] = if (it) "1" else "0" }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}

/** Pick one of a few options (chips). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceIn(f: FormState, key: String, label: String?, options: List<Pair<String, String>>, onPick: ((String) -> Unit)? = null) {
    Column(Modifier.fillMaxWidth()) {
        if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                val on = f[key] == value
                Box(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .then(if (on) Modifier.background(AB.brand.gradient) else Modifier.background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
                        .clickable { f[key] = value; onPick?.invoke(value) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) { Text(text, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else MaterialTheme.colorScheme.onSurface) }
            }
        }
    }
}

/** Pick one from a long list (people, tools …) with search. */
@Composable
fun PickIn(f: FormState, key: String, label: String, options: List<Pair<String, String>>, onPick: ((String) -> Unit)? = null) {
    var open by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    val current = options.firstOrNull { it.first == f[key] }?.second
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, if (open) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .clickable { open = !open }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(current ?: "Choose…", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(open) {
            GlassCard(Modifier.fillMaxWidth().padding(top = 6.dp), padding = PaddingValues(6.dp)) {
                if (options.size > 8) {
                    OutlinedTextField(q, { q = it }, placeholder = { Text("Search") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(4.dp))
                }
                options.filter { q.isBlank() || it.second.contains(q, true) }.take(60).forEach { (value, text) ->
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (value == f[key]) FontWeight.Bold else FontWeight.Normal,
                        color = if (value == f[key]) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { f[key] = value; open = false; onPick?.invoke(value) }.padding(horizontal = 10.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/** Date (yyyy-MM-dd, the website's format) with a calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateIn(f: FormState, key: String, label: String, hint: String? = null) {
    var open by remember { mutableStateOf(false) }
    val hintText: (@Composable () -> Unit)? = if (hint != null) { { Text(hint) } } else null
    OutlinedTextField(
        value = f[key], onValueChange = { f[key] = it.take(10) }, label = { Text(label) }, singleLine = true, supportingText = hintText,
        placeholder = { Text("YYYY-MM-DD") },
        trailingIcon = { IconButton(onClick = { open = true }) { Icon(Icons.Outlined.CalendarMonth, "Pick a date") } },
        shape = RoundedCornerShape(14.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
    )
    if (open) {
        val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") } }
        val initial = runCatching { fmt.parse(f[key].take(10))?.time }.getOrNull()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { f[key] = fmt.format(Date(it)) + f[key].drop(10) }; open = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { f[key] = ""; open = false }) { Text("Clear") } },
        ) { DatePicker(state) }
    }
}

/** Website color presets as swatches. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorIn(f: FormState, key: String, palette: List<String>) {
    Column {
        Text("Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            palette.forEach { hex ->
                val c = parseColor(hex, MaterialTheme.colorScheme.primary)
                val on = f[key].equals(hex, true)
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(c)
                        .border(if (on) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { f[key] = hex },
                )
            }
        }
    }
}

/** Pick a picture from the phone; shows it (or the current one from the site). */
@Composable
fun ImageIn(label: String, picked: FilePart?, currentUrl: String?, onPicked: (FilePart?) -> Unit) {
    val context = LocalContext.current
    var preview by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            preview = uri
            onPicked(readPicture(context, uri))
        }
    }
    GlassCard(Modifier.fillMaxWidth(), onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val model: Any? = preview ?: currentUrl
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (model != null) AsyncImage(model, null, Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                else Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(if (picked != null) "New picture chosen" else if (currentUrl != null) "Tap to change" else "Tap to choose (optional)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (picked != null) IconButton(onClick = { preview = null; onPicked(null) }) { Icon(Icons.Outlined.Close, "Remove") }
        }
    }
}

fun readPicture(context: Context, uri: Uri, field: String = "image"): FilePart? = runCatching {
    val cr = context.contentResolver
    val mime = cr.getType(uri) ?: "image/jpeg"
    val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.size > 5 * 1024 * 1024) return null
    val ext = when { mime.contains("png") -> "png"; mime.contains("webp") -> "webp"; mime.contains("gif") -> "gif"; else -> "jpg" }
    FilePart(field, "picture.$ext", mime, bytes)
}.getOrNull()

/** One small titled card for a group of fields. */
@Composable
fun Group(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

/** Tone from a site color word (green / amber / red …) or a status. */
fun tone(word: String?): Tone = when (word?.lowercase()) {
    "green", "success", "paid", "approved", "active", "completed", "ok", "available" -> Tone.Success
    "amber", "warning", "partial", "pending", "soon", "quoted", "busy" -> Tone.Warning
    "red", "danger", "unpaid", "rejected", "expired", "failed", "disabled" -> Tone.Danger
    "blue", "info", "in_progress" -> Tone.Info
    else -> Tone.Neutral
}

@Composable
fun ToneDot(t: Tone) = Box(Modifier.size(10.dp).clip(CircleShape).background(toneColor(t)))

@Composable
fun IconLine(icon: ImageVector, title: String, sub: String? = null, tint: Color = MaterialTheme.colorScheme.secondary, onClick: () -> Unit) {
    Line(title, sub, leading = { com.aamirbuneri.abgsmrental.ui.components.IconBadge(icon, tint, 40.dp) }, onClick = onClick)
}
