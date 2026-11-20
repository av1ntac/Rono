package app.rono.run

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Keeps GPS running while the screen is off and owns the ongoing live-run notification. */
class RunService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var demoJob: Job? = null
    private val listener = LocationListener { RunTracker.onLocation(it) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin(demo = intent.getBooleanExtra(EXTRA_DEMO, false))
            ACTION_PAUSE -> RunTracker.pause()
            ACTION_RESUME -> RunTracker.resume()
        }
        // A pause/resume tap that arrives after the process died has nothing to act on.
        if (!started) stopSelf()
        return START_NOT_STICKY
    }

    private fun begin(demo: Boolean) {
        if (started) return
        if (demo) Demo.seed() else RunTracker.start()
        val run = RunTracker.live.value ?: return
        RunNotification.createChannel(this)
        ServiceCompat.startForeground(
            this, RunNotification.NOTIFICATION_ID, RunNotification.build(this, run),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        started = true
        if (demo) startDemoFeed() else requestLocation()

        // 1 s ticker, aligned to the second, drives the clock in the app and the notification.
        scope.launch {
            while (isActive) {
                delay(1000 - SystemClock.elapsedRealtime() % 1000)
                RunTracker.tick()
                RunTracker.live.value?.let(::notify)
            }
        }
        // Pause/resume should show at once; the run ending stops the service.
        scope.launch {
            RunTracker.live.map { it?.phase }.distinctUntilChanged().collect { phase ->
                val run = RunTracker.live.value
                if (phase == null || run == null) {
                    ServiceCompat.stopForeground(this@RunService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    notify(run)
                }
            }
        }
    }

    private fun notify(run: LiveRun) {
        getSystemService(NotificationManager::class.java)
            .notify(RunNotification.NOTIFICATION_ID, RunNotification.build(this, run))
    }

    @SuppressLint("MissingPermission") // checked by the UI before the service is started
    private fun requestLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!hasLocationPermission(this)) return
        runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
        }
    }

    private fun startDemoFeed() {
        demoJob = scope.launch {
            while (isActive) {
                delay(1000)
                RunTracker.onLocation(Demo.nextLocation())
            }
        }
    }

    override fun onDestroy() {
        runCatching { (getSystemService(Context.LOCATION_SERVICE) as LocationManager).removeUpdates(listener) }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "app.rono.action.START"
        const val ACTION_PAUSE = "app.rono.action.PAUSE"
        const val ACTION_RESUME = "app.rono.action.RESUME"
        const val EXTRA_DEMO = "demo"

        fun hasLocationPermission(ctx: Context) =
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

        fun start(ctx: Context, demo: Boolean = false) {
            ContextCompat.startForegroundService(
                ctx,
                Intent(ctx, RunService::class.java).setAction(ACTION_START).putExtra(EXTRA_DEMO, demo),
            )
        }
    }
}
