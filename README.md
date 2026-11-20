# Rono

Android running app (Kotlin, Jetpack Compose) built from the HTML mockups in `design/`.

| Mockup | Implementation |
| --- | --- |
| `Main.dc.html` | `ui/HomeScreen.kt` |
| `Run.dc.html`, `Paused.dc.html` | `ui/RunScreen.kt` |
| `update1/History.dc.html` | `ui/HistoryScreen.kt` |
| `update1/Summary.dc.html` (replaces `Summary.dc.html`) | `ui/SummaryScreen.kt` |
| `LockScreen.dc.html`, `AOD.dc.html` | ongoing notification: `run/RunNotification.kt` + `res/layout/notification_run_*.xml` |

Colours, type sizes, spacing and radii come from the mockups (`ui/theme/Theme.kt`). Space Grotesk is
used for numbers and Inter for text; both are bundled as static TTFs in `res/font`.

## Fonts and licences

The bundled fonts are licensed under the SIL Open Font License 1.1. The licence texts are in
`licenses/`:

| Font | Version | Source | Licence |
| --- | --- | --- | --- |
| Inter | 4.001 | [rsms/inter](https://github.com/rsms/inter) | [`licenses/Inter-OFL.txt`](licenses/Inter-OFL.txt) |
| Space Grotesk | 2.000 | [floriankarsten/space-grotesk](https://github.com/floriankarsten/space-grotesk) | [`licenses/SpaceGrotesk-OFL.txt`](licenses/SpaceGrotesk-OFL.txt) |

## Build

Requires JDK 17+ (Android Studio's bundled JBR works) and Android SDK 35.

```
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install on a phone

Needs Android 8.0 (API 26) or newer.

### Option A: USB cable (adb)

1. On the phone, turn on **Developer options**: Settings → About phone → tap **Build number**
   7 times.
2. Turn on **USB debugging**: Settings → System → Developer options → USB debugging.
3. Connect the phone by USB and accept the "Allow USB debugging?" prompt on the phone.
4. Check the phone is listed (adb ships with the SDK in `%LOCALAPPDATA%\Android\Sdk\platform-tools`):
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   ```
5. Build and install in one step:
   ```powershell
   ./gradlew installDebug
   ```
   or install an APK you already built:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
   ```

### Option B: Wi‑Fi (Android 11+)

1. Turn on Developer options (as above), then Developer options → **Wireless debugging** → on.
2. Tap **Pair device with pairing code**, then on the computer (same Wi‑Fi network):
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" pair <ip>:<pairing-port>
   ```
   and enter the 6-digit code.
3. Connect using the IP and port shown on the Wireless debugging screen (not the pairing port):
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" connect <ip>:<port>
   ```
4. Run `./gradlew installDebug` as in Option A.

### Option C: Copy the APK (no adb)

1. Copy `app-debug.apk` to the phone (USB file transfer, cloud drive, email to yourself…).
2. Open it in the phone's Files app. When asked, allow that app to **install unknown apps**, then
   tap Install.

### First launch

Tap **START**. Allow **location** ("While using the app" is enough: tracking runs in a foreground
service) and **notifications**, so live stats show on the lock screen. If notifications were
denied, tap "Live stats on lock screen · Off" on the home screen to open the notification settings.

To remove the app: `adb uninstall app.rono`, or long-press the icon → Uninstall.

## How it works

- `run/RunService` is a `location` foreground service. It reads GPS once a second and owns the live
  notification, so tracking continues with the screen off.
- `run/RunTracker` holds the run state: moving time (pauses excluded), distance with jitter and
  jump filtering, current pace over the last ~20 s, and route segments. On finish it computes
  100 m and km splits.
- `run/RunStore` keeps finished runs in `files/runs.json`.
- The notification is a colorized foreground-service notification (#1C1F23) with custom
  collapsed and expanded views, plus Pause/Resume. The system ignores app fonts in RemoteViews, so
  `run/NotificationText` draws each text slot as a bitmap in the app's fonts.

## Demo data (debug builds)

To bring up the exact run shown in the mockups (27:48, 5.24 km, same route and splits), still
moving at 5:06 /km:

```
adb shell am start -n app.rono/.MainActivity --ez demo true
```
