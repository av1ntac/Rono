# CLAUDE.md

Rono is an Android running app (Kotlin, Jetpack Compose, single module `:app`, package `app.rono`).
The HTML mockups in `design/` (`*.dc.html` plus PNG renders) are the source of truth for the UI.

## Build, install, run

- JDK: use Android Studio's bundled JBR. Java isn't on PATH:
  `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (bash) or
  `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` (PowerShell).
- Build: `./gradlew assembleDebug`. Install to a connected phone or emulator: `./gradlew installDebug`.
  The phone setup steps are in README.md under "Install on a phone".
- Versions are pinned to what's in the local Gradle cache: Gradle 8.9, AGP 8.6.1, Kotlin 2.0.21,
  Compose BOM 2024.11.00, compileSdk/targetSdk 35, build-tools 35.0.0, minSdk 26. Don't bump them
  casually.
- There are no unit tests yet. Verify UI changes on the emulator (AVD `Pixel_8a`, 1080×2400 @ 420 dpi,
  about 411 dp wide, close to the mockups' 412 px) by comparing screenshots with the PNGs in `design/`.

### adb on this machine

- Run adb from **PowerShell**, not Git Bash. Git Bash rewrites device paths such as `/sdcard/x.png`
  into `C:/Program Files/Git/...`, which makes `adb push`, `screencap` and similar commands fail.
- `adb install` sometimes fails with an empty error on the emulator. Use
  `adb push <apk> /data/local/tmp/rono.apk` then `adb shell pm install -r /data/local/tmp/rono.apk`.
- Don't use `adb emu ...`; it hangs. For mock GPS use
  `adb shell cmd location providers add-test-provider gps` and
  `... set-test-provider-location gps --location <lat>,<lon> --accuracy 4`. Remove it afterwards.
- Demo run (debug builds only), reproducing the mockup data: `adb shell am start -n app.rono/.MainActivity --ez demo true`.
  Demo history (the seven runs from `update1/History.dc.html`, in memory only, never saved):
  `... --ez demoHistory true`.
- Element bounds for layout checks: `adb shell uiautomator dump /sdcard/ui.xml`, then read the file.

## Layout

- `ui/theme/Theme.kt`: colour tokens copied verbatim from the mockups, plus the `grotesk()` / `inter()`
  text styles. `ui/theme/RonoIcons.kt`: the mockups' inline SVG icons as ImageVectors.
- `ui/HomeScreen.kt`, `ui/RunScreen.kt` (running and paused), `ui/HistoryScreen.kt`, `ui/SummaryScreen.kt`:
  one file per mockup. `design/update1/` supersedes the older `Summary.dc.html`.
- `ui/MapArt.kt`: the mockups' stylised SVG maps drawn on Canvas with the same viewBox and
  preserveAspectRatio, with the real GPS route projected into the area the mockup route occupies.
- `run/RunTracker.kt`: the single source of truth for the run in progress (StateFlow). It's fed by
  `run/RunService.kt`, a `location` foreground service that owns GPS and a 1 s ticker.
- `run/RunNotification.kt` + `run/NotificationText.kt` + `res/layout/notification_run_*.xml`: the
  live notification that implements `LockScreen.dc.html` / `AOD.dc.html`.
- `run/RunStore.kt`: finished runs as JSON in `filesDir/runs.json`. `run/Demo.kt`: debug demo data.
- Navigation is plain state in `MainActivity.RonoApp()`; there's no navigation library.

## Rules and gotchas

- **Match the mockups exactly.** CSS px map 1:1 to dp, and font px to sp. Space Grotesk is for numbers
  and display text, Inter for running text. All text uses tabular figures (`tnum`).
- **Use `Txt(...)` from `ui/Components.kt`, not `BasicText` directly**, when a style sets
  `lineHeight` (the `grotesk(..., lineHeight = 1f)` helper). Compose won't shrink a single line
  below the font's ascent and descent, so `Txt` reproduces CSS `line-height` with a fixed-height
  box and the glyphs centred in it. Without it, the big numbers sit roughly 20–30 dp too low.
- **The lock-screen and AOD designs are a notification, not an app screen.** Keep it that way.
- **Notification text must be rendered to bitmaps** (`NotificationText`). The system inflates
  RemoteViews in its own process and ignores app `@font` resources, so TextViews fall back to Roboto.
  Bitmaps are ALPHA_8, tinted with `setColorFilter`, to keep the notification parcel well under the
  1 MB binder limit. Every text slot needs a content description.
- The notification is colorized (#1C1F23), which is allowed because it belongs to a foreground
  service. The colours in the layouts assume that dark background.
- Lock screens show the **collapsed** view (about 48 dp tall), so the Pause/Resume button must stay
  reachable there. The distance slot uses `scaleType="fitEnd"` with weight so it shrinks first.
- Notification channel importance is DEFAULT with no sound. LOW would move it into the "Silent"
  section, which some lock screens hide.
- Moving time excludes pauses, and pausing breaks the route into a new segment, so the gap never
  counts as distance.
- The Settings, Recenter and Profile buttons have no design yet and do nothing.
