# Building Timebox

There is no Android SDK on the machine this was written on, and the connection there
runs at roughly 150 KB/s. So the APK is built in CI and downloaded, rather than built
locally. Nothing about the project requires that — if you have an SDK, see *Building
locally* at the bottom.

## The two modules

| Module | What it is | Needs an Android SDK? |
|---|---|---|
| `:core` | Every decision Timebox makes: the gate's rules, reason validation, the foreground event filter, pass expiry, alarm timing, widget state. Pure Kotlin. | **no** |
| `:app` | The Android shell: accessibility service, Room database, Compose screens, Glance widget. | yes |

`settings.gradle.kts` only includes `:app` when it finds an SDK (`ANDROID_HOME`,
`ANDROID_SDK_ROOT`, or a `local.properties`). Without one it builds `:core` alone and
says so. This is deliberate: applying the Android Gradle plugin with no SDK fails
configuration for *every* project in the build, so a single missing SDK would otherwise
make even the pure rules untestable.

So on a machine with no SDK you can still run the part that matters most:

```bash
./gradlew :core:test
```

## Getting an APK onto your phone

1. Push to GitHub. The `build` workflow runs on every push.
2. Open the run under the repo's **Actions** tab.
3. Download the **timebox-debug-apk** artifact (a zip, ~10-15 MB).
4. Unzip it and move the `.apk` to your phone — USB, Drive, or whatever you use.
5. On the phone, tap the APK and allow installing from that source when asked.

If the build fails, the run also uploads a **test-reports** artifact with the HTML
test reports, which is the only way to read a failure from here.

### After installing, two permissions

Neither is optional, and the app will tell you so on its home screen if either is missing.

- **Accessibility** — Settings → Accessibility → Timebox. This is how Timebox sees which
  app came to the foreground. It is configured with `canRetrieveWindowContent="false"`,
  so it reads package names and never screen content.
- **Display over other apps** — this is not about drawing. Android 10 and later block
  apps from starting an activity from the background, and holding this permission is the
  documented exemption that lets the gate appear over the app you just opened.

On a phone where the APK was sideloaded, Android 13+ may hide the accessibility toggle
behind **Restricted settings**. If the toggle is greyed out: Settings → Apps → Timebox →
⋮ → *Allow restricted settings*, then go back and enable it.

## Building locally

With an SDK installed, nothing special is needed:

```bash
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

A build-only SDK is about 1.5 GB — you do not need Android Studio or the emulator,
since this app has to be tested against your own apps on your own phone anyway:

```bash
sdkmanager "platform-tools" "build-tools;36.0.0" "platforms;android-36"
```
