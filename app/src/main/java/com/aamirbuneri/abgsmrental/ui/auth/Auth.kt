package com.aamirbuneri.abgsmrental.ui.auth

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aamirbuneri.abgsmrental.BuildConfig
import com.aamirbuneri.abgsmrental.Container
import com.aamirbuneri.abgsmrental.R
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.Api
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.AppInfo
import com.aamirbuneri.abgsmrental.data.LoginResult
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.openUrl
import com.aamirbuneri.abgsmrental.ui.util.openWhatsApp
import com.aamirbuneri.abgsmrental.work.SyncWorker
import kotlinx.coroutines.launch

enum class AuthStep { SETUP, LOGIN, TWO_FACTOR }

class AuthViewModel(private val c: Container, settings: Settings) : ViewModel() {
    var step by mutableStateOf(if (settings.site.isBlank()) AuthStep.SETUP else AuthStep.LOGIN)
    var siteInput by mutableStateOf(settings.site.ifBlank { BuildConfig.DEFAULT_SITE })
    var site by mutableStateOf(settings.site)
    var info by mutableStateOf<AppInfo?>(null)
    var username by mutableStateOf(settings.username)
    var password by mutableStateOf("")
    var code by mutableStateOf("")
    var recovery by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    private var challenge: String? = null

    init {
        if (site.isNotBlank()) viewModelScope.launch { runCatching { info = c.api.appInfo(site) } }
    }

    fun connect() {
        val typed = siteInput.trim()
        if (typed.isEmpty()) { error = "Enter your website address."; return }
        viewModelScope.launch {
            busy = true; error = null
            val first = Api.normalizeSite(typed)
            val candidates = buildList {
                add(first)
                // typed without http(s)://: fall back to plain http for sites without SSL
                if (!typed.contains("://")) add(first.replaceFirst("https://", "http://"))
            }
            var lastError: ApiException? = null
            for (candidate in candidates) {
                try {
                    val i = c.api.appInfo(candidate)
                    if (i.app != "gsm-rental-pro") throw ApiException(400, "This address isn’t a GSM Rental Pro site.")
                    if (!i.appEnabled) throw ApiException(503, "The app is switched off on this site. Ask the admin, or use the website.")
                    c.prefs.setSite(candidate, i.name)
                    site = candidate; info = i; siteInput = candidate
                    step = AuthStep.LOGIN
                    busy = false
                    return@launch
                } catch (e: ApiException) {
                    lastError = e
                    if (!e.offline) break
                }
            }
            error = lastError?.message ?: "Couldn’t connect."
            busy = false
        }
    }

    fun signIn(context: Context) {
        if (username.isBlank() || password.isEmpty()) { error = "Enter your username and password."; return }
        viewModelScope.launch {
            busy = true; error = null
            try {
                handle(context, c.api.login(site, username, password))
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    fun verify(context: Context) {
        val ch = challenge ?: run { step = AuthStep.LOGIN; return }
        if (code.isBlank()) { error = "Enter the code."; return }
        viewModelScope.launch {
            busy = true; error = null
            try {
                handle(context, c.api.verify2fa(site, ch, code))
            } catch (e: ApiException) {
                error = e.message
                code = ""
                if (e.code == 429 || e.message?.contains("Sign in again") == true) { step = AuthStep.LOGIN; challenge = null }
            } finally {
                busy = false
            }
        }
    }

    private suspend fun handle(context: Context, r: LoginResult) {
        if (r.need2fa && r.challenge != null) {
            challenge = r.challenge; code = ""; recovery = false
            step = AuthStep.TWO_FACTOR
            return
        }
        val token = r.token ?: throw ApiException(500, "The site didn’t return a sign-in token.")
        c.prefs.signIn(token, r.user?.username ?: username.trim())
        password = ""
        SyncWorker.schedule(context)
    }

    fun changeSite() {
        error = null; step = AuthStep.SETUP
    }

    fun backToLogin() {
        error = null; challenge = null; code = ""; step = AuthStep.LOGIN
    }
}

@Composable
fun AuthFlow(settings: Settings) {
    val context = LocalContext.current
    val c = context.container
    val vm: AuthViewModel = viewModel { AuthViewModel(c, settings) }
    ScreenBackground {
        AnimatedContent(
            targetState = vm.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
            },
            label = "auth",
        ) { step ->
            when (step) {
                AuthStep.SETUP -> SetupScreen(vm)
                AuthStep.LOGIN -> LoginScreen(vm, settings)
                AuthStep.TWO_FACTOR -> TwoFactorScreen(vm)
            }
        }
    }
}

@Composable
private fun AuthColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun BrandHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(28.dp))
    Image(painterResource(R.drawable.brand_mark), "AB Gsm Rental", Modifier.width(170.dp))
    Spacer(Modifier.height(10.dp))
    Row {
        Text("AB ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = AB.brand.gradientColors.first())
        Text("Gsm Rental", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
    Spacer(Modifier.height(28.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(26.dp))
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboard: KeyboardOptions,
    actions: KeyboardActions = KeyboardActions(),
    password: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = if (password) {
            { IconButton(onClick = { shown = !shown }) { Icon(if (shown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (shown) "Hide password" else "Show password") } }
        } else null,
        visualTransformation = if (password && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        keyboardOptions = keyboard,
        keyboardActions = actions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SetupScreen(vm: AuthViewModel) {
    val focus = LocalFocusManager.current
    AuthColumn {
        BrandHeader("Connect to your panel", "Enter the website address of your rental panel. You only do this once.")
        Field(
            value = vm.siteInput,
            onChange = { vm.siteInput = it; vm.error = null },
            label = "Website address",
            icon = Icons.Outlined.Language,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false),
            actions = KeyboardActions(onGo = { focus.clearFocus(); vm.connect() }),
        )
        Spacer(Modifier.height(12.dp))
        InlineError(vm.error)
        Spacer(Modifier.height(12.dp))
        GradientButton("Continue", { focus.clearFocus(); vm.connect() }, Modifier.fillMaxWidth(), loading = vm.busy)
        Spacer(Modifier.height(16.dp))
        Text(
            "Example: aamirbuneri.com",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(36.dp))
        Footer()
    }
}

@Composable
private fun LoginScreen(vm: AuthViewModel, settings: Settings) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val passFocus = remember { FocusRequester() }
    val info = vm.info
    val siteName = info?.name?.takeIf { it.isNotBlank() } ?: settings.siteName.ifBlank { vm.site.removePrefix("https://").removePrefix("http://") }
    AuthColumn {
        BrandHeader("Welcome back", "Sign in with your reseller account.")
        // which site we're signing in to
        Row(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, AB.brand.cardBorder, RoundedCornerShape(16.dp))
                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(AB.brand.success, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(siteName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(vm.site.removePrefix("https://").removePrefix("http://"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            TextButton(onClick = { vm.changeSite() }) { Text("Change") }
        }
        Spacer(Modifier.height(18.dp))
        Field(
            value = vm.username,
            onChange = { vm.username = it; vm.error = null },
            label = "Username or email",
            icon = Icons.Outlined.Person,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            actions = KeyboardActions(onNext = { passFocus.requestFocus() }),
        )
        Spacer(Modifier.height(12.dp))
        Field(
            value = vm.password,
            onChange = { vm.password = it; vm.error = null },
            label = "Password",
            icon = Icons.Outlined.Lock,
            password = true,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            actions = KeyboardActions(onDone = { focus.clearFocus(); vm.signIn(context) }),
            modifier = Modifier.focusRequester(passFocus),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            val forgot = info?.forgotUrl.orEmpty()
            if (forgot.isNotBlank()) TextButton(onClick = { openUrl(context, forgot) }) { Text("Forgot password?") }
        }
        InlineError(vm.error)
        Spacer(Modifier.height(12.dp))
        GradientButton("Sign in", { focus.clearFocus(); vm.signIn(context) }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Outlined.Login, loading = vm.busy)
        Spacer(Modifier.height(18.dp))
        if (info?.registration == true && info.registerUrl.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New reseller?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { openUrl(context, info.registerUrl) }) { Text("Create an account") }
            }
        }
        val wa = info?.support?.whatsapp.orEmpty()
        if (wa.isNotBlank()) {
            TextButton(onClick = { openWhatsApp(context, wa, "Hi, I need help signing in to the AB Gsm Rental app.") }) { Text("Need help? Chat on WhatsApp") }
        }
        Spacer(Modifier.height(20.dp))
        Footer()
    }
}

@Composable
private fun TwoFactorScreen(vm: AuthViewModel) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(vm.recovery) { runCatching { focus.requestFocus() } }
    AuthColumn {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = { vm.backToLogin() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        }
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(84.dp).background(AB.brand.gradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Shield, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(22.dp))
        Text("Two-step sign-in", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            if (vm.recovery) "Enter one of your recovery codes." else "Enter the 6-digit code from your authenticator app.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (vm.recovery) {
            Field(
                value = vm.code, onChange = { vm.code = it; vm.error = null }, label = "Recovery code", icon = Icons.Outlined.Lock,
                keyboard = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
                actions = KeyboardActions(onDone = { vm.verify(context) }),
                modifier = Modifier.focusRequester(focus),
            )
        } else {
            OtpBoxes(vm.code, { v ->
                vm.code = v; vm.error = null
                if (v.length == 6 && !vm.busy) vm.verify(context)
            }, Modifier.focusRequester(focus))
        }
        Spacer(Modifier.height(16.dp))
        InlineError(vm.error)
        Spacer(Modifier.height(16.dp))
        GradientButton("Verify", { vm.verify(context) }, Modifier.fillMaxWidth(), loading = vm.busy)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { vm.recovery = !vm.recovery; vm.code = ""; vm.error = null }) {
            Text(if (vm.recovery) "Use the authenticator code" else "Lost your phone? Use a recovery code")
        }
    }
}

@Composable
private fun OtpBoxes(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(6)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        singleLine = true,
        modifier = modifier,
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) { i ->
                    val ch = value.getOrNull(i)
                    val active = i == value.length
                    Box(
                        Modifier
                            .size(width = 46.dp, height = 58.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
                            .border(
                                if (active) 2.dp else 1.dp,
                                if (active) MaterialTheme.colorScheme.secondary else AB.brand.cardBorder,
                                RoundedCornerShape(14.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ch?.toString() ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    )
}

@Composable
private fun Footer() {
    Text(
        "By Aamir-Buneri  ·  v${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}
