package com.aamirbuneri.abgsmrental.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aamirbuneri.abgsmrental.ui.theme.AB

/** Message styles shared with the website: info, success ("good news"), promo ("offer"), warning, danger ("urgent"). */
data class NoticeStyle(val label: String, val icon: ImageVector, val color: Color)

@Composable
fun noticeStyle(type: String?): NoticeStyle {
    val b = AB.brand
    return when (type) {
        "success" -> NoticeStyle("Good news", Icons.Outlined.CheckCircle, b.success)
        "promo" -> NoticeStyle("Offer", Icons.Outlined.LocalOffer, Color(0xFFD946EF))
        "warning" -> NoticeStyle("Warning", Icons.Outlined.WarningAmber, b.warning)
        "danger" -> NoticeStyle("Urgent", Icons.Outlined.ErrorOutline, b.danger)
        "chat" -> NoticeStyle("Chat", Icons.Outlined.Campaign, b.info)
        else -> NoticeStyle("Info", Icons.Outlined.Info, b.info)
    }
}

private val TOKEN = Regex("""\*\*(.+?)\*\*|(?<![\w*])_(.+?)_(?!\w)|(https?://[^\s<>"')\]]+)""")

/** "**bold**", "_italic_" and web links, like the website shows them. */
fun formatted(text: String, link: Color): AnnotatedString = buildAnnotatedString {
    var at = 0
    for (m in TOKEN.findAll(text)) {
        append(text.substring(at, m.range.first))
        val (bold, italic, url) = m.destructured
        when {
            bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
            italic.isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
            else -> withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline)))) { append(url) }
        }
        at = m.range.last + 1
    }
    append(text.substring(at))
}

/**
 * A message as resellers see it: colored type badge, title, formatted text, optional picture
 * and one button. Used for the notification list and the composer's live preview.
 */
@Composable
fun RichNotice(
    title: String,
    text: String,
    type: String?,
    modifier: Modifier = Modifier,
    image: Any? = null,
    actionLabel: String? = null,
    time: String? = null,
    unread: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    onAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val st = noticeStyle(type)
    GlassCard(modifier.fillMaxWidth(), onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(4.dp).background(st.color))
            if (image != null) {
                AsyncImage(image, null, Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Crop)
            }
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(st.color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(st.icon, null, tint = st.color, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(st.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = st.color, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (time != null) Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (unread) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(title.ifBlank { "Title" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (text.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(formatted(text, MaterialTheme.colorScheme.secondary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
                }
                if (!actionLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onAction?.invoke() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = st.color, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(actionLabel, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
