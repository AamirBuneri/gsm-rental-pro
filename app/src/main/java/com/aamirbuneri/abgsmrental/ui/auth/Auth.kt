package com.aamirbuneri.abgsmrental.ui.auth

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.aamirbuneri.abgsmrental.data.CurrencyOption
import com.aamirbuneri.abgsmrental.data.LoginResult
import com.aamirbuneri.abgsmrental.data.Settings
import com.aamirbuneri.abgsmrental.data.UserBrief
import com.aamirbuneri.abgsmrental.ui.components.GradientButton
import com.aamirbuneri.abgsmrental.ui.components.InlineError
import com.aamirbuneri.abgsmrental.ui.components.ScreenBackground
import com.aamirbuneri.abgsmrental.ui.components.SecondaryButton
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.openUrl
import com.aamirbuneri.abgsmrental.ui.util.openWhatsApp
import com.aamirbuneri.abgsmrental.work.SyncWorker
import kotlinx.coroutines.launch

enum class AuthStep { SETUP, LOGIN, REGISTER, FORGOT, TWO_FACTOR, STATUS }

/** Account made / signed in, but the site wants something first: confirm the email, or wait for approval. */
enum class Waiting { EMAIL, APPROVAL }

private val FALLBACK_CURRENCIES = listOf(
    CurrencyOption("PKR", "Pakistani Rupee", "Rs"), CurrencyOption("USD", "US Dollar", "$"), CurrencyOption("EUR", "Euro", "€"),
    CurrencyOption("GBP", "British Pound", "£"), CurrencyOption("AED", "UAE Dirham", "AED"),
)

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
    var errorState by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)
    var waiting by mutableStateOf(Waiting.APPROVAL)
    var waitingText by mutableStateOf("")
    private var challenge: String? = null

    // sign-up form
    var fullName by mutableStateOf("")
    var regUsername by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var company by mutableStateOf("")
    var currency by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regPassword2 by mutableStateOf("")
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())

    // forgot password
    var forgotEmail by mutableStateOf("")
    var forgotSent by mutableStateOf<String?>(null)

    init {
        if (site.isNotBlank()) viewModelScope.launch { runCatching { info = c.api.appInfo(site) } }
    }

    val currencies: List<CurrencyOption> get() = info?.currencies?.takeIf { it.isNotEmpty() } ?: FALLBACK_CURRENCIES

    private fun clearMessages() { error = null; errorState = null; notice = null }

    fun connect() {
        val typed = siteInput.trim()
        if (typed.isEmpty()) { error = "Enter your website address."; return }
        viewModelScope.launch {
            busy = true; clearMessages()
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
            busy = true; clearMessages()
            try {
                handle(context, c.api.login(site, username, password))
            } catch (e: ApiException) {
                if (!showWaiting(e)) { error = e.message; errorState = e.state }
            } finally {
                busy = false
            }
        }
    }

    fun verify(context: Context) {
        val ch = challenge ?: run { step = AuthStep.LOGIN; return }
        if (code.isBlank()) { error = "Enter the code."; return }
        viewModelScope.launch {
            busy = true; clearMessages()
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
        finish(context, token, r.user ?: UserBrief(username = username.trim()))
    }

    private suspend fun finish(context: Context, token: String, user: UserBrief) {
        c.prefs.signIn(token, user.copy(username = user.username.ifBlank { username.trim() }))
        password = ""; regPassword = ""; regPassword2 = ""
        SyncWorker.schedule(context)
    }

    /** Confirm-email / approval answers open the waiting screen instead of a red error. */
    private fun showWaiting(e: ApiException): Boolean {
        val w = when (e.state) {
            "verify_email" -> Waiting.EMAIL
            "pending" -> Waiting.APPROVAL
            else -> return false
        }
        waiting = w; waitingText = e.message.orEmpty()
        step = AuthStep.STATUS
        return true
    }

    fun openRegister() {
        clearMessages(); fieldErrors = emptyMap()
        if (currency.isBlank()) currency = info?.currency?.ifBlank { null } ?: "PKR"
        step = AuthStep.REGISTER
    }

    fun register(context: Context) {
        val errs = linkedMapOf<String, String>()
        if (fullName.isBlank()) errs["full_name"] = "Enter your name."
        if (!Regex("^[A-Za-z0-9_.-]{3,40}$").matches(regUsername.trim())) errs["username"] = "3–40 letters, numbers, dot, dash or underscore."
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())) errs["email"] = "Enter a valid email address."
        if (regPassword.length < 8) errs["password"] = "At least 8 characters."
        if (regPassword2 != regPassword) errs["password2"] = "Passwords don’t match."
        fieldErrors = errs
        if (errs.isNotEmpty()) { error = "Check the highlighted fields."; return }
        viewModelScope.launch {
            busy = true; clearMessages()
            try {
                val r = c.api.register(site, mapOf(
                    "full_name" to fullName, "username" to regUsername, "email" to email, "phone" to phone,
                    "company" to company, "currency" to currency.ifBlank { "PKR" }, "password" to regPassword,
                ))
                // keep what's needed to sign in by itself once the account is ready
                username = regUsername.trim(); password = regPassword
                when {
                    r.state == "ready" && r.token != null -> finish(context, r.token, r.user ?: UserBrief(username = regUsername.trim()))
                    r.state == "verify_email" -> { waiting = Waiting.EMAIL; waitingText = r.message; step = AuthStep.STATUS }
                    else -> { waiting = Waiting.APPROVAL; waitingText = r.message; step = AuthStep.STATUS }
                }
            } catch (e: ApiException) {
                error = e.message
                // point at the field the site complained about
                val m = e.message.orEmpty().lowercase()
                fieldErrors = when {
                    "username" in m -> mapOf("username" to e.message.orEmpty())
                    "email" in m -> mapOf("email" to e.message.orEmpty())
                    "password" in m -> mapOf("password" to e.message.orEmpty())
                    "name" in m -> mapOf("full_name" to e.message.orEmpty())
                    else -> emptyMap()
                }
            } finally {
                busy = false
            }
        }
    }

    /** "I've confirmed" / "Check again": sign in with what the user just typed. */
    fun checkAgain(context: Context) {
        if (username.isBlank() || password.isEmpty()) { step = AuthStep.LOGIN; return }
        viewModelScope.launch {
            busy = true; clearMessages()
            try {
                handle(context, c.api.login(site, username, password))
            } catch (e: ApiException) {
                if (e.state == "verify_email" || e.state == "pending") {
                    val w = if (e.state == "verify_email") Waiting.EMAIL else Waiting.APPROVAL
                    notice = if (w == waiting) (if (w == Waiting.EMAIL) "Not confirmed yet — open the link in the email first." else "Not approved yet. We’ll keep your details — check again later.") else null
                    waiting = w; waitingText = e.message.orEmpty()
                } else {
                    error = e.message
                }
            } finally {
                busy = false
            }
        }
    }

    fun resend() {
        viewModelScope.launch {
            busy = true; clearMessages()
            try {
                notice = c.api.resendVerification(site, username, password).message
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    fun openForgot() {
        clearMessages(); forgotSent = null
        if (forgotEmail.isBlank() && username.contains('@')) forgotEmail = username.trim()
        step = AuthStep.FORGOT
    }

    fun sendReset() {
        if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(forgotEmail.trim())) { error = "Enter the email address of your account."; return }
        viewModelScope.launch {
            busy = true; clearMessages()
            try {
                forgotSent = c.api.forgotPassword(site, forgotEmail).message
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    fun changeSite() {
        clearMessages(); step = AuthStep.SETUP
    }

    fun backToLogin() {
        clearMessages(); challenge = null; code = ""; step = AuthStep.LOGIN
    }
}

@Composable
fun AuthFlow(settings: Settings) {
    val context = LocalContext.current
    val c = context.container
    val vm: AuthViewModel = viewModel { AuthViewModel(c, settings) }
    // system back from sign-up / forgot / 2FA / waiting → the sign-in screen (not out of the app)
    BackHandler(enabled = vm.step != AuthStep.LOGIN && vm.step != AuthStep.SETUP) { vm.backToLogin() }
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
                AuthStep.REGISTER -> RegisterScreen(vm)
                AuthStep.FORGOT -> ForgotScreen(vm)
                AuthStep.TWO_FACTOR -> TwoFactorScreen(vm)
                AuthStep.STATUS -> WaitingScreen(vm)
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
private fun BrandHeader(title: String, subtitle: String, compact: Boolean = false) {
    Spacer(Modifier.height(if (compact) 8.dp else 28.dp))
    Image(painterResource(R.drawable.brand_mark), "AB Gsm Rental", Modifier.width(if (compact) 110.dp else 170.dp))
    Spacer(Modifier.height(10.dp))
    Row {
        Text("AB ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = AB.brand.gradientColors.first())
        Text("Gsm Rental", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
    Spacer(Modifier.height(if (compact) 18.dp else 28.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(if (compact) 20.dp else 26.dp))
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
    }
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
    error: String? = null,
    hint: String? = null,
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
        isError = error != null,
        supportingText = if (error != null || hint != null) {
            { Text(error ?: hint.orEmpty()) }
        } else null,
        keyboardOptions = keyboard,
        keyboardActions = actions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Green "done" banner (email sent, link sent again …). */
@Composable
private fun Notice(text: String?) {
    AnimatedVisibility(text != null, enter = fadeIn(), exit = fadeOut()) {
        val c = AB.brand.success
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.12f)).border(1.dp, c.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CheckCircle, null, tint = c, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
    }
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
            modifier = Modifier.testTag("site"),
        )
        Spacer(Modifier.height(12.dp))
        InlineError(vm.error)
        Spacer(Modifier.height(12.dp))
        GradientButton("Continue", { focus.clearFocus(); vm.connect() }, Modifier.fillMaxWidth(), loading = vm.busy)
        Spacer(Modifier.height(16.dp))
        Text("Example: aamirbuneri.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))
        Footer()
    }
}

@Composable
private fun SiteChip(vm: AuthViewModel, settings: Settings?) {
    val info = vm.info
    val siteName = info?.name?.takeIf { it.isNotBlank() } ?: settings?.siteName?.ifBlank { null } ?: vm.site.removePrefix("https://").removePrefix("http://")
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
}

@Composable
private fun LoginScreen(vm: AuthViewModel, settings: Settings) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val passFocus = remember { FocusRequester() }
    val info = vm.info
    AuthColumn {
        BrandHeader("Welcome back", "Sign in with your account — resellers, staff and admins.")
        SiteChip(vm, settings)
        Spacer(Modifier.height(18.dp))
        Field(
            value = vm.username,
            onChange = { vm.username = it; vm.error = null },
            label = "Username or email",
            icon = Icons.Outlined.Person,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            actions = KeyboardActions(onNext = { passFocus.requestFocus() }),
            modifier = Modifier.testTag("username"),
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
            modifier = Modifier.focusRequester(passFocus).testTag("password"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            when {
                info?.passwordReset == true -> TextButton(onClick = { vm.openForgot() }, modifier = Modifier.testTag("forgot")) { Text("Forgot password?") }
                // older site: its own page
                info?.passwordReset == null && info?.forgotUrl?.isNotBlank() == true -> TextButton(onClick = { openUrl(context, info.forgotUrl) }) { Text("Forgot password?") }
            }
        }
        InlineError(vm.error)
        if (vm.errorState == "setup_2fa" && info?.website?.isNotBlank() == true) {
            TextButton(onClick = { openUrl(context, info.website) }) { Text("Open the website") }
        }
        Spacer(Modifier.height(12.dp))
        GradientButton("Sign in", { focus.clearFocus(); vm.signIn(context) }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Outlined.Login, loading = vm.busy)
        Spacer(Modifier.height(18.dp))
        if (info?.registration == true) {
            val native = info.appSignup == true
            val web = info.appSignup == null && info.registerUrl.isNotBlank() // site older than 3.4
            if (native || web) {
                GlassRow(
                    icon = Icons.Outlined.PersonAdd,
                    title = "New reseller?",
                    text = "Create your account here in a minute.",
                    action = "Create account",
                    modifier = Modifier.testTag("register"),
                ) { if (native) vm.openRegister() else openUrl(context, info.registerUrl) }
                Spacer(Modifier.height(8.dp))
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
private fun GlassRow(icon: ImageVector, title: String, text: String, action: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, AB.brand.cardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(AB.brand.gradient), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegisterScreen(vm: AuthViewModel) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val e = vm.fieldErrors
    fun edit(key: String) { vm.error = null; if (key in vm.fieldErrors) vm.fieldErrors = vm.fieldErrors - key }
    AuthColumn {
        BackRow { vm.backToLogin() }
        BrandHeader("Create your account", "Rent tools and order services from ${vm.info?.name?.ifBlank { null } ?: "your panel"} — right from your phone.", compact = true)
        val next = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(vm.fullName, { vm.fullName = it; edit("full_name") }, "Your name", Icons.Outlined.Badge,
                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next), next, error = e["full_name"], modifier = Modifier.testTag("reg_name"))
            Field(vm.regUsername, { vm.regUsername = it.replace(" ", ""); edit("username") }, "Username", Icons.Outlined.AlternateEmail,
                KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next, autoCorrectEnabled = false), next,
                error = e["username"], hint = "You sign in with this", modifier = Modifier.testTag("reg_username"))
            Field(vm.email, { vm.email = it.trim(); edit("email") }, "Email", Icons.Outlined.Email,
                KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false), next, error = e["email"], modifier = Modifier.testTag("reg_email"))
            Field(vm.phone, { vm.phone = it; edit("phone") }, "WhatsApp number (optional)", Icons.Outlined.Phone,
                KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next), next, hint = "For rental and wallet messages, e.g. 923001234567", modifier = Modifier.testTag("reg_phone"))
            Field(vm.company, { vm.company = it }, "Shop / company (optional)", Icons.Outlined.Storefront,
                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next), next, modifier = Modifier.testTag("reg_company"))
        }
        Spacer(Modifier.height(14.dp))
        Text("Billing currency", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.currencies.forEach { cur ->
                val on = vm.currency.equals(cur.code, true)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (on) Modifier.background(AB.brand.gradient) else Modifier.background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
                        .clickable { vm.currency = cur.code }
                        .testTag("cur_${cur.code}")
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(cur.code, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else MaterialTheme.colorScheme.onSurface)
                    if (cur.symbol.isNotBlank() && cur.symbol != cur.code) {
                        Spacer(Modifier.width(6.dp))
                        Text(cur.symbol, style = MaterialTheme.typography.labelMedium, color = if (on) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("Prices and your wallet use this currency. Only the admin can change it later.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Field(vm.regPassword, { vm.regPassword = it; edit("password") }, "Password", Icons.Outlined.Lock,
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next, autoCorrectEnabled = false), next,
                password = true, error = e["password"], hint = "At least 8 characters", modifier = Modifier.testTag("reg_password"))
            Field(vm.regPassword2, { vm.regPassword2 = it; edit("password2") }, "Confirm password", Icons.Outlined.Lock,
                KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                KeyboardActions(onDone = { focus.clearFocus(); vm.register(context) }),
                password = true, error = e["password2"], modifier = Modifier.testTag("reg_password2"))
        }
        Spacer(Modifier.height(12.dp))
        InlineError(vm.error)
        Spacer(Modifier.height(12.dp))
        GradientButton("Create account", { focus.clearFocus(); vm.register(context) }, Modifier.fillMaxWidth().testTag("reg_submit"), icon = Icons.Outlined.PersonAdd, loading = vm.busy)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { vm.backToLogin() }) { Text("Sign in") }
        }
        Spacer(Modifier.height(12.dp))
        Footer()
    }
}

@Composable
private fun ForgotScreen(vm: AuthViewModel) {
    val focus = LocalFocusManager.current
    AuthColumn {
        BackRow { vm.backToLogin() }
        Spacer(Modifier.height(16.dp))
        RoundIcon(Icons.Outlined.LockReset)
        Spacer(Modifier.height(20.dp))
        Text("Forgot your password?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Enter the email of your account. We’ll send a link to choose a new password.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        val sent = vm.forgotSent
        if (sent == null) {
            Field(
                vm.forgotEmail, { vm.forgotEmail = it.trim(); vm.error = null }, "Email", Icons.Outlined.Email,
                KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send, autoCorrectEnabled = false),
                KeyboardActions(onSend = { focus.clearFocus(); vm.sendReset() }), modifier = Modifier.testTag("forgot_email"),
            )
            Spacer(Modifier.height(12.dp))
            InlineError(vm.error)
            Spacer(Modifier.height(12.dp))
            GradientButton("Send reset link", { focus.clearFocus(); vm.sendReset() }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Outlined.Send, loading = vm.busy)
        } else {
            Notice(sent)
            Spacer(Modifier.height(18.dp))
            GradientButton("Back to sign in", { vm.backToLogin() }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Outlined.Login)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.forgotSent = null }) { Text("Use a different email") }
        }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, size: androidx.compose.ui.unit.Dp = 84.dp) {
    Box(Modifier.size(size).background(AB.brand.gradient, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
private fun WaitingScreen(vm: AuthViewModel) {
    val context = LocalContext.current
    val email = vm.waiting == Waiting.EMAIL
    val wa = vm.info?.support?.whatsapp.orEmpty()
    AuthColumn {
        BackRow { vm.backToLogin() }
        Spacer(Modifier.height(28.dp))
        RoundIcon(if (email) Icons.Outlined.MarkEmailUnread else Icons.Outlined.HourglassTop, 96.dp)
        Spacer(Modifier.height(22.dp))
        Text(if (email) "Confirm your email" else "Waiting for approval", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.testTag("waiting_title"))
        Spacer(Modifier.height(8.dp))
        Text(
            vm.waitingText.ifBlank {
                if (email) "We sent a link to your email. Open it to confirm your address." else "The admin checks every new account. You can sign in as soon as yours is approved."
            },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (email) "Can’t find it? Check the spam folder." else "We’ll sign you in right here — no need to type your password again.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Steps(email)
        Spacer(Modifier.height(20.dp))
        Notice(vm.notice)
        InlineError(vm.error)
        Spacer(Modifier.height(14.dp))
        GradientButton(if (email) "I’ve confirmed — continue" else "Check again", { vm.checkAgain(context) }, Modifier.fillMaxWidth().testTag("check_again"), icon = Icons.Outlined.Refresh, loading = vm.busy)
        Spacer(Modifier.height(10.dp))
        if (email) {
            SecondaryButton("Send the link again", { vm.resend() }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Outlined.Send, enabled = !vm.busy)
            Spacer(Modifier.height(6.dp))
        }
        if (wa.isNotBlank()) {
            TextButton(onClick = {
                openWhatsApp(context, wa, if (email) "Hi, I made an account (${vm.username}) but didn’t get the confirmation email." else "Hi, I just made an account (${vm.username}). Please approve it.")
            }) {
                Icon(Icons.AutoMirrored.Outlined.Chat, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (email) "No email? Ask on WhatsApp" else "Ask the admin on WhatsApp")
            }
        }
    }
}

/** Where the new account is: made → email → approval → ready. */
@Composable
private fun Steps(email: Boolean) {
    val b = AB.brand
    val labels = listOf("Account made", "Email confirmed", "Approved")
    val done = if (email) 1 else 2
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        labels.forEachIndexed { i, label ->
            val isDone = i < done
            val current = i == done
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .then(if (isDone) Modifier.background(b.gradient) else Modifier.background(MaterialTheme.colorScheme.surfaceContainer).border(if (current) 2.dp else 1.dp, if (current) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) Icon(Icons.Outlined.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    else Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = if (current) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = if (isDone || current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TwoFactorScreen(vm: AuthViewModel) {
    val context = LocalContext.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(vm.recovery) { runCatching { focus.requestFocus() } }
    AuthColumn {
        BackRow { vm.backToLogin() }
        Spacer(Modifier.height(24.dp))
        RoundIcon(Icons.Outlined.Shield)
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
