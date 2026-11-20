package app.rono

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rono.run.Demo
import app.rono.run.Fmt
import app.rono.run.GpsFix
import app.rono.run.RunService
import app.rono.run.RunStore
import app.rono.run.RunSummary
import app.rono.run.RunTracker
import app.rono.ui.GpsChip
import app.rono.ui.HistoryScreen
import app.rono.ui.HomeScreen
import app.rono.ui.RunScreen
import app.rono.ui.SummaryScreen
import app.rono.ui.Tab
import app.rono.ui.theme.RonoColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        RunStore.load(this)
        if (BuildConfig.DEBUG && intent.getBooleanExtra("demoHistory", false)) RunStore.seedDemo(Demo.history())
        if (BuildConfig.DEBUG && intent.getBooleanExtra("demo", false) && !RunTracker.isActive) {
            RunService.start(this, demo = true)
        }
        setContent { RonoApp() }
    }
}

@Composable
private fun RonoApp() {
    val ctx = LocalContext.current
    val live by RunTracker.live.collectAsStateWithLifecycle()
    val gps by RunTracker.gps.collectAsStateWithLifecycle()
    val runs by RunStore.runs.collectAsStateWithLifecycle()
    var summaryId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Bottom-nav tab under the summary. Profile has no design yet, so it never becomes selected.
    var tab by rememberSaveable { mutableStateOf(Tab.Run) }
    // Hoisted so the History scroll position survives opening a run and coming back.
    val historyList = rememberLazyListState()
    val onTab: (Tab) -> Unit = { if (it != Tab.Profile) tab = it }

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    var hasLocation by remember { mutableStateOf(RunService.hasLocationPermission(ctx)) }
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }
    LifecycleResumeEffect(Unit) {
        hasLocation = RunService.hasLocationPermission(ctx)
        notificationsOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLocation = RunService.hasLocationPermission(ctx)
        notificationsOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        if (hasLocation) RunService.start(ctx)
        else Toast.makeText(ctx, "Rono needs location access to track your run", Toast.LENGTH_LONG).show()
    }

    // Re-evaluate GPS freshness every second.
    val now by produceState(SystemClock.elapsedRealtime()) {
        while (true) { delay(1000); value = SystemClock.elapsedRealtime() }
    }

    Box(Modifier.fillMaxSize().background(RonoColors.Background)) {
        val run = live
        val summary = summaryId?.let { id -> runs.firstOrNull { it.id == id } }
        when {
            run != null -> RunScreen(
                run = run,
                gpsLabel = gpsStrength(gps, now),
                onPause = RunTracker::pause,
                onResume = RunTracker::resume,
                onFinish = {
                    RunTracker.finish()?.let {
                        RunStore.add(ctx, it)
                        summaryId = it.id
                    }
                },
            )

            summary != null -> {
                BackHandler { summaryId = null }
                SummaryScreen(
                    summary,
                    backLabel = if (tab == Tab.History) "Back to history" else "Back",
                    onBack = { summaryId = null },
                    onShare = { share(ctx, summary) },
                )
            }

            tab == Tab.History -> {
                BackHandler { tab = Tab.Run }
                HistoryScreen(runs, historyList, onOpen = { summaryId = it.id }, onTab = onTab)
            }

            else -> {
                if (hasLocation) HomeGpsPreview(ctx)
                HomeScreen(
                    gps = homeGps(gps, now, hasLocation),
                    lockStatsOn = notificationsOn,
                    onStart = {
                        if (hasLocation) RunService.start(ctx) else launcher.launch(permissions)
                    },
                    onLockStatsClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName),
                        )
                    },
                    onSettings = { },
                    onRecenter = { },
                    onTab = onTab,
                )
            }
        }
    }
}

/** Listens to GPS while the home screen is visible, so "GPS ready · ±4 m" is real. */
@SuppressLint("MissingPermission")
@Composable
private fun HomeGpsPreview(ctx: Context) {
    DisposableEffect(Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { RunTracker.onLocation(it) }
        runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper()) }
        onDispose { lm.removeUpdates(listener) }
    }
}

private fun fresh(gps: GpsFix?, now: Long) = gps != null && now - gps.atElapsedMs < 10_000

private fun homeGps(gps: GpsFix?, now: Long, hasLocation: Boolean): GpsChip = when {
    !hasLocation -> GpsChip("Location off · tap Start", RonoColors.TextMuted)
    fresh(gps, now) && gps!!.accuracyM <= 25f -> GpsChip("GPS ready · ±${gps.accuracyM.roundToInt()} m", RonoColors.Accent)
    fresh(gps, now) -> GpsChip("Weak GPS · ±${gps!!.accuracyM.roundToInt()} m", RonoColors.Slow)
    else -> GpsChip("Finding GPS…", RonoColors.Slow)
}

private fun gpsStrength(gps: GpsFix?, now: Long): String = when {
    !fresh(gps, now) -> "Searching GPS"
    gps!!.accuracyM <= 10f -> "GPS strong"
    gps.accuracyM <= 25f -> "GPS good"
    else -> "GPS weak"
}

private fun share(ctx: Context, run: RunSummary) {
    val text = "${Fmt.runTitle(run.startedAt)}: ${Fmt.km(run.distanceM)} km in ${Fmt.duration(run.activeMs)} " +
        "(${Fmt.pace(run.avgPaceSecPerKm)} /km avg) with Rono"
    ctx.startActivity(
        Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null),
    )
}
