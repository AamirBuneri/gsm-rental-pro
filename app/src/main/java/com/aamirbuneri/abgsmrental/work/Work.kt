package com.aamirbuneri.abgsmrental.work

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aamirbuneri.abgsmrental.MainActivity
import com.aamirbuneri.abgsmrental.R
import com.aamirbuneri.abgsmrental.container
import com.aamirbuneri.abgsmrental.data.ApiException
import com.aamirbuneri.abgsmrental.data.Notice
import com.aamirbuneri.abgsmrental.data.Rental
import com.aamirbuneri.abgsmrental.ui.util.timeOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object Notifier {
    const val CH_UPDATES = "updates"
    const val CH_REMINDERS = "reminders"
    const val CH_MESSAGES = "messages"
    const val CH_LIVE = "live"

    /** Extras on the intent that opens the app from a notification. */
    const val EXTRA_LINK = "open_link"
    const val EXTRA_NOTICE = "open_notice"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_UPDATES, context.getString(R.string.channel_updates), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_updates_desc) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.channel_reminders_desc) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_MESSAGES, "Messages & offers", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Messages from the shop: news, offers and announcements" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_LIVE, "Live alerts (background)", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Keeps the app listening for new alerts when it is closed. You can hide this one."; setShowBadge(false) }
        )
    }

    private fun canPost(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openIntent(context: Context, requestCode: Int, link: String? = null, noticeId: Int? = null): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply {
                    if (link != null) putExtra(EXTRA_LINK, link)
                    if (noticeId != null) putExtra(EXTRA_NOTICE, noticeId)
                },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun show(context: Context, id: Int, title: String, text: String, channel: String = CH_UPDATES) {
        if (!canPost(context)) return
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_ab)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openIntent(context, id))
            .setPriority(if (channel == CH_REMINDERS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    /** A site notification on the phone: picture (big), formatted text, and its button. */
    suspend fun showNotice(context: Context, n: Notice) {
        if (!canPost(context)) return
        val plain = n.message.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1").replace(Regex("(?<![\\w])_(.+?)_(?![\\w])"), "$1").ifBlank { n.title }
        val picture = n.image?.let { loadBitmap(it) }
        val id = 10_000 + n.id
        val b = NotificationCompat.Builder(context, if (n.broadcast) CH_MESSAGES else CH_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_ab)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(n.title)
            .setContentText(plain)
            .setAutoCancel(true)
            .setContentIntent(openIntent(context, id, noticeId = n.id))
            .setPriority(if (n.broadcast || n.type == "danger") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (n.broadcast) NotificationCompat.CATEGORY_PROMO else NotificationCompat.CATEGORY_STATUS)
        if (picture != null) {
            b.setLargeIcon(picture)
            b.setStyle(NotificationCompat.BigPictureStyle().bigPicture(picture).bigLargeIcon(null as Bitmap?).setSummaryText(plain))
        } else {
            b.setStyle(NotificationCompat.BigTextStyle().bigText(plain))
        }
        val url = n.actionUrl
        if (!url.isNullOrBlank()) {
            b.addAction(0, n.actionLabel ?: "Open", openIntent(context, id + 500_000, link = url, noticeId = n.id))
        }
        runCatching { NotificationManagerCompat.from(context).notify(id, b.build()) }
    }

    private val imageHttp by lazy { OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build() }

    private suspend fun loadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            imageHttp.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) return@use null
                val bytes = r.body?.bytes() ?: return@use null
                if (bytes.size > 6_000_000) return@use null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                var sample = 1
                while (opts.outWidth / sample > 1400 || opts.outHeight / sample > 1400) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }.getOrNull()
    }

    fun cancelAll(context: Context) = NotificationManagerCompat.from(context).cancelAll()
}

/** One check for new notifications, shared by the live service, the wake-up alarm and the 15-minute worker. */
object AlertCheck {
    private val lock = Mutex()

    /** @return false when the phone is signed out (stop checking) */
    suspend fun run(context: Context): Boolean = lock.withLock {
        val c = context.applicationContext.container
        val s = c.prefs.snapshot()
        if (!s.signedIn) return@withLock false
        try {
            val last = c.prefs.lastNotice()
            val page = withTimeoutOrNull(25_000) { c.api.ping(last) } ?: return@withLock true
            c.unread.value = page.unread
            if (last > 0 && s.notifications) {
                page.items.filter { !it.read && it.id > last }.takeLast(5).forEach { Notifier.showNotice(context, it) }
            }
            if (page.latestId > last || last == 0) c.prefs.setLastNotice(maxOf(page.latestId, last))
            true
        } catch (e: ApiException) {
            !e.signedOut
        }
    }
}

/** Every ~15 minutes, even if the live service was stopped by the system. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AlertCheck.run(applicationContext)
        LiveAlerts.ensure(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "sync"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
            val app = context.applicationContext
            app.container.scope.launch { LiveAlerts.ensure(app) }
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
            WorkManager.getInstance(context).cancelAllWorkByTag(Reminders.TAG)
            LiveAlerts.stop(context)
        }
    }
}

/** "UnlockTool ends in 10 minutes" — scheduled on the phone, works offline. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val s = applicationContext.container.prefs.snapshot()
        if (!s.signedIn || !s.notifications) return Result.success()
        val endsAt = inputData.getLong("ends_at", 0)
        if (endsAt < System.currentTimeMillis()) return Result.success()
        val tool = inputData.getString("tool").orEmpty()
        val mins = ((endsAt - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1)
        Notifier.show(
            applicationContext,
            20_000 + inputData.getInt("rental_id", 0),
            "$tool ends in $mins min",
            "Your ${inputData.getString("plan").orEmpty()} rental ends at ${timeOnly(endsAt)}. Finish your work or rent again.",
            Notifier.CH_REMINDERS,
        )
        return Result.success()
    }
}

object Reminders {
    const val TAG = "rental-reminder"
    private const val BEFORE_MS = 10 * 60_000L

    /** Plan a reminder 10 minutes before each running rental ends (once per rental and end time). */
    fun sync(context: Context, rentals: List<Rental>) {
        val wm = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()
        rentals.filter { it.status == "active" }.forEach { r ->
            val at = r.endsAtMs - BEFORE_MS
            if (at <= now + 30_000L) return@forEach
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(at - now, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("rental_id" to r.id, "tool" to r.tool, "plan" to r.plan, "ends_at" to r.endsAtMs))
                .addTag(TAG)
                .build()
            // same rental + same end time → one reminder; an extended rental gets a new one
            val end = r.expiresAt.ifBlank { (r.endsAtMs / 60_000L).toString() }.filter { it.isLetterOrDigit() }
            wm.enqueueUniqueWork("rental-${r.id}-$end", ExistingWorkPolicy.KEEP, req)
        }
    }
}

/** Starts / stops the background listener and its wake-up alarm. */
object LiveAlerts {
    private const val ALARM_MS = 60_000L

    /** Start (or keep) the live service when the user wants alerts; otherwise stop it. Safe to call often. */
    suspend fun ensure(context: Context) {
        val app = context.applicationContext
        val s = app.container.prefs.snapshot()
        if (s.signedIn && s.notifications && s.liveAlerts) start(app) else stop(app)
    }

    fun start(context: Context) {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, LiveService::class.java))
        }
        scheduleAlarm(context)
    }

    fun stop(context: Context) {
        runCatching { context.stopService(Intent(context, LiveService::class.java)) }
        alarmManager(context)?.cancel(alarmIntent(context))
    }

    fun scheduleAlarm(context: Context, delayMs: Long = ALARM_MS) {
        val am = alarmManager(context) ?: return
        val at = SystemClock.elapsedRealtime() + delayMs
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, alarmIntent(context))
            } else {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, alarmIntent(context))
            }
        }
    }

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 7101, Intent(context, AlertAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

}
