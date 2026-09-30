package com.aamirbuneri.abgsmrental.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aamirbuneri.abgsmrental.ui.theme.AB
import com.aamirbuneri.abgsmrental.ui.util.clock
import com.aamirbuneri.abgsmrental.ui.util.copy
import com.aamirbuneri.abgsmrental.ui.util.initials
import com.aamirbuneri.abgsmrental.ui.util.parseColor
import kotlinx.coroutines.delay

/** Current time, ticking every second while on screen. */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L - System.currentTimeMillis() % 1000L)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** Soft green + blue glows behind a screen, like the splash art. */
fun Modifier.brandGlow(green: Color, blue: Color): Modifier = drawBehind {
    drawCircle(Brush.radialGradient(listOf(green, Color.Transparent), center = Offset(size.width * 0.1f, size.height * 0.05f), radius = size.width * 0.9f))
    drawCircle(Brush.radialGradient(listOf(blue, Color.Transparent), center = Offset(size.width * 0.95f, size.height * 0.35f), radius = size.width * 0.8f))
}

@Composable
fun ScreenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val b = AB.brand
    // screens outside a Surface would otherwise draw text in the default (black) colour in dark mode
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .brandGlow(b.glowGreen, b.glowBlue),
            content = content,
        )
    }
}

/** Card with a hairline border — the main building block. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, AB.brand.cardBorder),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** Green → blue gradient button (the app's main action). */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 54.dp,
) {
    val b = AB.brand
    val active = enabled && !loading
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(height)
            .clip(shape)
            .background(b.gradient)
            .alpha(if (active) 1f else 0.55f)
            .clickable(enabled = active, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

enum class Tone { Success, Warning, Danger, Info, Neutral, Brand }

@Composable
fun toneColor(tone: Tone): Color {
    val b = AB.brand
    return when (tone) {
        Tone.Success -> b.success
        Tone.Warning -> b.warning
        Tone.Danger -> b.danger
        Tone.Info -> b.info
        Tone.Brand -> MaterialTheme.colorScheme.primary
        Tone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier, dot: Boolean = true) {
    val c = toneColor(tone)
    Row(
        modifier
            .clip(CircleShape)
            .background(c.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(c))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = c, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

fun orderTone(status: String): Tone = when (status) {
    "completed" -> Tone.Success
    "in_progress" -> Tone.Info
    "pending" -> Tone.Warning
    "quoted" -> Tone.Brand
    "failed" -> Tone.Danger
    else -> Tone.Neutral
}

fun rentalTone(status: String): Tone = when (status) {
    "active" -> Tone.Success
    "expired" -> Tone.Neutral
    "returned" -> Tone.Info
    "closed" -> Tone.Danger
    else -> Tone.Neutral
}

fun rentalLabel(status: String): String = when (status) {
    "active" -> "Running"
    "expired" -> "Ended"
    "returned" -> "Returned"
    "closed" -> "Closed"
    else -> status.replaceFirstChar { it.uppercase() }
}

/** Tool / service picture, or its initials on its colour. */
@Composable
fun ItemAvatar(name: String, color: String?, image: String?, size: Dp = 48.dp) {
    val c = parseColor(color, MaterialTheme.colorScheme.secondary)
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.copy(alpha = 0.28f), c.copy(alpha = 0.10f))))
            .border(1.dp, c.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!image.isNullOrBlank()) {
            AsyncImage(model = image, contentDescription = name, modifier = Modifier.fillMaxSize().clip(shape))
        } else {
            Text(initials(name), color = c, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.34f).sp)
        }
    }
}

/** Ring that empties as the rental runs out, with the time left in the middle. */
@Composable
fun CountdownRing(endsAtMs: Long, totalMinutes: Int, size: Dp = 76.dp, stroke: Dp = 7.dp, showText: Boolean = true) {
    val now = rememberNow()
    val left = ((endsAtMs - now) / 1000L).coerceAtLeast(0)
    val total = (totalMinutes * 60L).coerceAtLeast(1)
    val target = (left.toFloat() / total).coerceIn(0f, 1f)
    val frac by animateFloatAsState(target, tween(600), label = "ring")
    val b = AB.brand
    val track = MaterialTheme.colorScheme.outlineVariant
    val urgent = left in 1..600
    val colors = if (urgent) listOf(b.warning, b.danger) else b.gradientColors
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(sw))
            drawArc(
                Brush.sweepGradient(colors + colors.first()),
                -90f, 360f * frac, false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        if (showText) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (left > 0) clock(left) else "Ended",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value / 6.2f).sp,
                    color = if (urgent) b.danger else MaterialTheme.colorScheme.onSurface,
                )
                if (left > 0 && size >= 90.dp) Text("left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** One line of a tool login: masked until revealed, one-tap copy. */
@Composable
fun CredentialRow(label: String, value: String, secret: Boolean = false, initiallyShown: Boolean = !secret) {
    val context = LocalContext.current
    var shown by rememberSaveable(label, value) { mutableStateOf(initiallyShown) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (shown) value else "••••••••••",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (secret) {
            IconButton(onClick = { shown = !shown }) {
                Icon(if (shown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (shown) "Hide" else "Show")
            }
        }
        IconButton(onClick = { copy(context, label, value, sensitive = secret) }) {
            Icon(Icons.Outlined.ContentCopy, "Copy $label", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
    }
}

@Composable
fun Shimmer(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x")
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hi = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(base, hi, base), start = Offset(size.width * x, 0f), end = Offset(size.width * (x + 0.7f), size.height)))
            }
    )
}

@Composable
fun LoadingCards(count: Int = 4, height: Dp = 86.dp) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { Shimmer(Modifier.fillMaxWidth().height(height)) }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            GradientButton(action, onAction, Modifier.fillMaxWidth(0.7f), height = 48.dp)
        }
    }
}

@Composable
fun ErrorState(message: String, offline: Boolean = false, onRetry: () -> Unit) {
    EmptyState(
        icon = if (offline) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline,
        title = if (offline) "You’re offline" else "Couldn’t load this",
        text = message,
        action = "Try again",
        onAction = onRetry,
    )
}

/** Small banner for a problem while old data is still shown. */
@Composable
fun InlineError(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(message != null, enter = fadeIn(), exit = fadeOut()) {
        Row(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(message.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Round icon badge used in lists / tiles. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 42.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}
