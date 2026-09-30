package com.aamirbuneri.abgsmrental.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import com.aamirbuneri.abgsmrental.data.Rental
import com.aamirbuneri.abgsmrental.ui.util.timeOnly
import java.util.concurrent.TimeUnit

object Notifier {
    const val CH_UPDATES = "updates"
    const val CH_REMINDERS = "reminders"

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
    }

    fun show(context: Context, id: Int, title: String, text: String, channel: String = CH_UPDATES) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_ab)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(if (channel == CH_REMINDERS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    fun cancelAll(context: Context) = NotificationManagerCompat.from(context).cancelAll()
}

/** Every ~15 minutes: new notifications from the site → phone notifications. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.prefs.snapshot()
        if (!s.signedIn || !s.notifications) return Result.success()
        return try {
            val page = if (s.team) c.api.adminNotifications(limit = 15) else c.api.notifications(limit = 15)
            val last = c.prefs.lastNotice()
            val newest = page.items.maxOfOrNull { it.id } ?: last
            if (last > 0) {
                page.items.filter { !it.read && it.id > last }.take(5).reversed().forEach { n ->
                    Notifier.show(applicationContext, 10_000 + n.id, n.title, n.message.ifBlank { n.title })
                }
            }
            if (newest > last) c.prefs.setLastNotice(newest)
            Result.success()
        } catch (e: ApiException) {
            Result.success() // offline / signed out: try again next time
        }
    }

    companion object {
        private const val NAME = "sync"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
            WorkManager.getInstance(context).cancelAllWorkByTag(Reminders.TAG)
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
