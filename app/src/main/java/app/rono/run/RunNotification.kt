package app.rono.run

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.rono.MainActivity
import app.rono.R
import app.rono.run.NotificationText.Face as F

/**
 * The ongoing "live run" notification. design/LockScreen.dc.html and design/AOD.dc.html show
 * the same data on the lock screen and always-on display: time, distance, pace, avg pace and a
 * Pause action. It's a colorized foreground-service notification (card colour #1C1F23) with a
 * custom body, plus plain-text fallbacks for surfaces that can't show custom views.
 */
object RunNotification {

    const val CHANNEL_ID = "live_run"
    const val NOTIFICATION_ID = 1

    private const val CARD = 0xFF1C1F23.toInt()

    fun createChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        // DEFAULT (not LOW) so it stays in the alerting section of the lock screen, but silent.
        val ch = NotificationChannel(CHANNEL_ID, ctx.getString(R.string.channel_run_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = ctx.getString(R.string.channel_run_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)
    }

    fun build(ctx: Context, run: LiveRun) = run.let { r ->
        val paused = r.phase == RunPhase.Paused
        val time = Fmt.duration(r.activeMs)
        val pace = Fmt.pace(r.currentPaceSecPerKm)
        val avg = Fmt.pace(r.avgPaceSecPerKm)
        val dist = "${Fmt.km(r.distanceM)} km"
        val accent = ContextCompat.getColor(ctx, R.color.accent)
        val muted = ContextCompat.getColor(ctx, R.color.muted)
        val slow = ContextCompat.getColor(ctx, R.color.slow)
        val toggle = toggleIntent(ctx, paused)

        val fg = ContextCompat.getColor(ctx, R.color.text)
        val dark = ContextCompat.getColor(ctx, R.color.bg)
        val paceColor = if (r.currentPaceSecPerKm == null) muted else accent
        val t = NotificationText(ctx)

        val collapsed = RemoteViews(ctx.packageName, R.layout.notification_run_collapsed).apply {
            with(t) {
                // One shared line box so all values sit on the time's baseline.
                val row = box(F.GroteskSemiBold, 28f)
                text(R.id.time, time, F.GroteskSemiBold, 28f, fg, row, -0.02f, "Time $time")
                text(R.id.distance, dist, F.GroteskSemiBold, 18f, fg, row)
                text(R.id.pace, pace, F.GroteskSemiBold, 18f, paceColor, row, description = "Pace $pace per km")
                text(R.id.pace_unit, " /km", F.GroteskMedium, 13f, muted, row)
            }
            setImageViewResource(R.id.toggle, if (paused) R.drawable.ic_play else R.drawable.ic_pause)
            setContentDescription(R.id.toggle, if (paused) "Resume run" else "Pause run")
            setOnClickPendingIntent(R.id.toggle, toggle)
        }

        // Sizes, weights and line-heights from design/LockScreen.dc.html.
        val expanded = RemoteViews(ctx.packageName, R.layout.notification_run_expanded).apply {
            with(t) {
                text(R.id.label_time, "Time", F.InterRegular, 12f, muted)
                text(R.id.time, time, F.GroteskSemiBold, 52f, fg, box(F.GroteskSemiBold, 52f, 1f), -0.02f, "Time $time")
                text(R.id.status, if (paused) "Paused" else "Live", F.InterSemiBold, 12f, if (paused) slow else accent)
                text(R.id.label_pace, "Pace", F.InterRegular, 12f, muted)
                val paceBox = box(F.GroteskSemiBold, 36f, 1f)
                text(R.id.pace, pace, F.GroteskSemiBold, 36f, paceColor, paceBox, description = "Pace $pace per km")
                text(R.id.pace_unit, " /km", F.GroteskMedium, 14f, muted, paceBox)
                text(R.id.label_distance, "Distance", F.InterRegular, 12f, muted)
                text(R.id.distance, dist, F.GroteskSemiBold, 20f, fg)
                text(R.id.label_avg, "Avg pace", F.InterRegular, 12f, muted)
                text(R.id.avg_pace, avg, F.GroteskSemiBold, 20f, fg, description = "Average pace $avg per km")
                text(R.id.km_left, "${r.metersToNextKm} m to km ${r.nextKm}", F.InterRegular, 12f, muted)
                text(R.id.toggle_label, if (paused) "Resume" else "Pause", F.InterBold, 14f, dark)
            }
            setProgressBar(R.id.km_progress, 1000, (r.kmProgress * 1000).toInt(), false)
            setImageViewResource(R.id.status_dot, if (paused) R.drawable.notif_dot_slow else R.drawable.notif_dot_accent)
            setImageViewResource(R.id.toggle_icon, if (paused) R.drawable.ic_play else R.drawable.ic_pause)
            setOnClickPendingIntent(R.id.toggle, toggle)
        }

        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val action = NotificationCompat.Action.Builder(
            if (paused) R.drawable.ic_play else R.drawable.ic_pause,
            if (paused) "Resume" else "Pause",
            toggle,
        ).build()

        NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_runner)
            .setColor(CARD)
            .setColorized(true)
            .setSubText("Free run")
            // Text fallbacks: accessibility, wearables, and AODs that only render plain text.
            .setContentTitle(if (paused) "Paused · $time" else "$time · $dist")
            .setContentText("Pace $pace /km · Avg $avg /km")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            // The in-card button handles phones; watches get a regular action.
            .extend(NotificationCompat.WearableExtender().addAction(action))
            .build()
    }

    private fun toggleIntent(ctx: Context, paused: Boolean): PendingIntent = PendingIntent.getService(
        ctx,
        if (paused) 2 else 1,
        Intent(ctx, RunService::class.java).setAction(if (paused) RunService.ACTION_RESUME else RunService.ACTION_PAUSE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
