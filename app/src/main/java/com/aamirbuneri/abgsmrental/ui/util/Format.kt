package com.aamirbuneri.abgsmrental.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val symbols = mapOf("PKR" to "Rs ", "USD" to "$", "EUR" to "€", "GBP" to "£", "AED" to "AED ", "INR" to "₹", "SAR" to "SAR ")

fun money(amount: Double, currency: String): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        val whole = abs(amount - Math.round(amount)) < 0.005
        minimumFractionDigits = if (whole) 0 else 2
        maximumFractionDigits = if (whole) 0 else 2
    }
    val sign = if (amount < 0) "−" else ""
    return sign + (symbols[currency.uppercase()] ?: "$currency ") + nf.format(abs(amount))
}

private val isoFormats = listOf("yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss")

/** The site's UTC "2026-09-30T10:00:00Z" → Date (null when empty / unknown). */
fun parseIso(s: String?): Date? {
    if (s.isNullOrBlank()) return null
    for (f in isoFormats) {
        runCatching {
            return SimpleDateFormat(f, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(s)
        }
    }
    return null
}

fun dateTime(s: String?): String = parseIso(s)?.let { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(it) } ?: "—"

fun shortDate(s: String?): String = parseIso(s)?.let { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(it) } ?: "—"

fun timeOnly(ms: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

/** "just now", "5 min ago", "3 h ago", "yesterday", "12 Sep". */
fun ago(s: String?): String {
    val d = parseIso(s) ?: return ""
    val sec = (System.currentTimeMillis() - d.time) / 1000
    return when {
        sec < 45 -> "just now"
        sec < 3600 -> "${sec / 60} min ago"
        sec < 86400 -> "${sec / 3600} h ago"
        sec < 172800 -> "yesterday"
        sec < 7 * 86400 -> "${sec / 86400} days ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(d)
    }
}

/** 90 → "1 h 30 min", 360 → "6 hours", 1440 → "1 day". */
fun duration(minutes: Int): String = when {
    minutes <= 0 -> ""
    minutes % 1440 == 0 -> (minutes / 1440).let { if (it == 1) "1 day" else "$it days" }
    minutes % 60 == 0 -> (minutes / 60).let { if (it == 1) "1 hour" else "$it hours" }
    minutes > 60 -> "${minutes / 60} h ${minutes % 60} min"
    else -> "$minutes min"
}

/** Seconds → "05:42:09" or "12:09". */
fun clock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, sec) else String.format(Locale.US, "%02d:%02d", m, sec)
}

fun parseColor(hex: String?, fallback: Color): Color = runCatching {
    val h = hex?.trim()?.removePrefix("#") ?: return fallback
    when (h.length) {
        6 -> Color(("FF$h").toLong(16))
        8 -> Color(h.toLong(16))
        3 -> Color(("FF" + h.map { "$it$it" }.joinToString("")).toLong(16))
        else -> fallback
    }
}.getOrDefault(fallback)

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

fun copy(context: Context, label: String, value: String, sensitive: Boolean = false) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, value)
    if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    }
    cm.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Toast.makeText(context, "No app to open this link", Toast.LENGTH_SHORT).show() }
}

fun openWhatsApp(context: Context, number: String, text: String) {
    val digits = number.filter { it.isDigit() }
    if (digits.isEmpty()) return
    openUrl(context, "https://wa.me/$digits?text=" + Uri.encode(text))
}

fun dial(context: Context, phone: String) = openUrl(context, "tel:" + phone.filter { it.isDigit() || it == '+' })

fun email(context: Context, address: String) = openUrl(context, "mailto:$address")
