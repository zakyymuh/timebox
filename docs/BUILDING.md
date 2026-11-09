# Building the apps

This repo builds three Android apps:

| App | Module | Package | What it does |
|---|---|---|---|
| **papacti** | `:app` | `dev.zakyymuh.timebox` | Plant schedule, calendar, encyclopedia, collection and wishlist, soil air-space tests, and the 18:00 watering reminder |
| **Daily** (Habits) | `:habits` | `dev.zakyymuh.habits` | Today's list, the home-screen widget, and the 06:30 morning reminder |
| **Motor** | `:motor` | `dev.zakyymuh.motor` | Your vehicles and their service logs, the dates your SIM and STNK run out, and the 09:00 expiry reminder |

There is no Android SDK on the machine this was written on, and the connection there
runs at roughly 150 KB/s. So the APKs are built in CI and downloaded, rather than built
locally. Nothing about the project requires that. If you have an SDK, see *Building
locally* at the bottom.

## The modules

| Module | What it is | Needs an Android SDK? |
|---|---|---|
| `:core` | Every decision the apps make: plant schedules, list rules, service-log sums, alarm timing, widget state. Pure Kotlin. | **no** |
| `:ui` | The Android library all three apps share: the Fresh theme, the Inter font, cards, fields, navigation guards and the daily alarm. | yes |
| `:app` | papacti, the plant app. | yes |
| `:habits` | The daily-list app. | yes |
| `:motor` | The service-log app. | yes |

`settings.gradle.kts` includes the Android modules only when it finds an SDK
(`ANDROID_HOME`, `ANDROID_SDK_ROOT`, or a `local.properties`). Without one it builds
`:core` alone and says so. This is deliberate: applying the Android Gradle plugin with no
SDK fails configuration for *every* project in the build, so a single missing SDK would
otherwise make even the pure rules untestable.

So on a machine with no SDK you can still run the part that matters most:

```bash
./gradlew :core:test
```

## Getting the APKs onto your phone

1. Push to GitHub. The `build` workflow runs on every push.
2. Open the run under the repo's **Actions** tab.
3. Download the artifacts you want. Each one is a zip with one `.apk` in it:
   - **plants-debug-apk**: papacti
   - **habits-debug-apk**: Daily
   - **motor-debug-apk**: Motor
4. Unzip them and move the `.apk` files to your phone however you like (USB, Drive, or anything else).
5. On the phone, tap each APK and allow installing from that source when asked.

All three are signed with the same committed debug key (`app/debug.keystore`), and CI
fails if they ever differ. That's what lets each new build install over the last one.
A different key would force an uninstall, and an uninstall deletes that app's data.

If the build fails, the run also uploads a **test-reports** artifact with the HTML
test reports, which is the only way to read a failure from here.

### First install after the split (November 2026)

Before the split, one app held all three features. The order matters:

1. **Install papacti over the old app first.** It keeps every plant, watering,
   photo, collection and wishlist entry, and soil test. It drops the old daily lists
   and service records. That data is not carried across, and was meant to be retyped.
2. **Install Daily and Motor.** Both start empty.
3. **Open Daily once** and allow notifications, so the 06:30 reminder is armed.
   On Xiaomi / HyperOS, also turn on **Settings → Apps → Daily → Autostart**.
   Sideloaded apps start with it off, and without it Android never tells Daily the
   phone rebooted, so the reminder stops until the app is next opened. papacti's old
   grant doesn't carry over, because Daily is a new app. Check papacti still has it
   too, for the 18:00 watering reminder.
4. **Open Motor once** and allow notifications, so the daily 09:00 expiry check is
   armed. On HyperOS, turn on **Settings → Apps → Motor → Autostart** too, for the
   same reason as Daily.
5. **Add the widget again.** It now belongs to Daily: long-press the home screen →
   Widgets → Daily. The old widget disappears when papacti is updated.

## Building locally

With an SDK installed, nothing special is needed:

```bash
./gradlew :core:test :app:testDebugUnitTest :habits:testDebugUnitTest :motor:testDebugUnitTest
./gradlew :app:assembleDebug :habits:assembleDebug :motor:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r habits/build/outputs/apk/debug/habits-debug.apk
adb install -r motor/build/outputs/apk/debug/motor-debug.apk
```

A build-only SDK is about 1.5 GB. You don't need Android Studio or the emulator:

```bash
sdkmanager "platform-tools" "build-tools;37.0.0" "platforms;android-37"
```
