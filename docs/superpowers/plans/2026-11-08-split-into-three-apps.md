# Split into Plants, Habits and Motor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build three Android apps (Plants, Habits, Motor) from this repo. The existing install becomes Plants and keeps all of its plant data.

**Architecture:** `:core` stays as the shared rules. A new Android library `:ui` holds the theme, the shared Compose components and `DailyAlarm`. `:habits` and `:motor` are new app modules that get the habit and service code. `:app` stays as Plants, loses that code, and runs a Room migration 7→8 that drops the `day`, `todo_item` and `service` tables.

**Tech Stack:** Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21, Compose, Room 2.8.5 + KSP, Glance 1.2.0, Robolectric 4.17.

**Spec:** `docs/superpowers/specs/2026-11-08-split-into-three-apps-design.md`

## Global Constraints

- Plants keeps `applicationId = "dev.zakyymuh.timebox"` and is signed with `app/debug.keystore` (password `android`, alias `androiddebugkey`).
- New apps: `applicationId` `dev.zakyymuh.habits` (label "Daily") and `dev.zakyymuh.motor` (label "Motor"). Plants keeps the label "papacti", its bishop's-cap launcher icon and its launch screen (`res/drawable/*myriostigma*`, `ic_launcher*`, `values-v31/themes.xml`), all added by another session in `431af02`..`f2c06d2`. Don't rename it and don't copy the cactus icon or splash into the new apps. They use the default launcher icon until the owner picks names and icons.
- Every app is signed with the same committed `app/debug.keystore`.
- Never `fallbackToDestructiveMigration` in any app.
- `minSdk 26`, `compileSdk 37`, `targetSdk 37`, Java/JVM 17 bytecode, same as `:app` today.
- Every plugin is declared once in the root `build.gradle.kts` with `apply false`, and modules apply it by id with no version.
- Android modules join the build only under the existing `hasAndroidSdk` guard in `settings.gradle.kts`. `:core` must keep building without an SDK.
- **This machine has no Android SDK.** Android modules compile and test only in CI (`.github/workflows/build.yml`, runs on every push). Local verification is `./gradlew :core:test`. Android verification means pushing the `split-apps` branch and reading the run (`gh run watch`).
- Before every commit and push: run `ListAgents` and `git status`/`git log`. If another session has uncommitted or new work touching `app/`, stop and coordinate. Commit only files this plan touches.
- Work on branch `split-apps`. Never push to `main` without the owner's go-ahead.

## Review Focus

1. **Updating over the real v7 install.** A v7 DB holding plants, waterings, dex, air tests, days, items and services opens at v8 with every plant-side row intact. Test: Task 4 `a version 7 database upgrades to 8 with plants intact and habit and service tables gone`.
2. **Old migration tests reading removed DAOs.** The v1–v7 tests call `current.dao()` / `serviceDao()`, which no longer exist. They must assert only on plant-side data, and each must still migrate all the way to v8. Covered in Task 4 Step 3.
3. **Plants opened from a stale intent.** A leftover pinned shortcut or intent asking for `"today"` must open the dashboard, not crash. Test: Task 4 `DestinationsTest.today is no longer a destination`.
4. **Habits after a reboot.** The morning alarm has to be re-armed by Habits' own `BootReceiver`. Checked manually in Task 6 Step 3.
5. **Same signing key across all three APKs**, so later builds install as upgrades. CI step in Task 5 compares the certificate digests and fails if they differ.

---

### Task 1: Shared `:ui` library; Plants builds on it

**Files:**
- Modify: `gradle/libs.versions.toml` (add `android-library = { id = "com.android.library", version.ref = "agp" }` under `[plugins]`)
- Modify: `build.gradle.kts` (add `alias(libs.plugins.android.library) apply false`)
- Modify: `settings.gradle.kts` (include `:ui` under the same guard as `:app`, and require `ui/build.gradle.kts` to exist)
- Create: `ui/build.gradle.kts` (`com.android.library`, compose plugin, `namespace = "dev.zakyymuh.timebox.shared"`, compose and core-ktx deps exposed as `api`, `api(project(":core"))`)
- Create: `ui/src/main/AndroidManifest.xml` (empty `<manifest/>`)
- Move (git mv, **keep package names**): `app/.../ui/theme/{Color,Theme,Type}.kt` → `ui/src/main/java/dev/zakyymuh/timebox/ui/theme/`; `app/.../ui/common/Fresh.kt` → `ui/src/main/java/dev/zakyymuh/timebox/ui/common/`; `app/.../nudge/DailyAlarm.kt` → `ui/src/main/java/dev/zakyymuh/timebox/nudge/`; `app/src/main/res/font/inter_regular.ttf` → `ui/src/main/res/font/`
- Create: `ui/src/main/java/dev/zakyymuh/timebox/ui/common/Fields.kt`, holding `DateField` and `softFieldColors` cut from `app/.../ui/plants/EditPlantScreen.kt:196-270`, now `public`
- Create: `ui/src/main/java/dev/zakyymuh/timebox/ui/common/Nav.kt`, holding `NavBackStackEntry.ifResumed` and `NavHostController.go` cut from `AppNav.kt`, now `public` (`:ui` adds an `api` dependency on navigation-compose)
- Modify: `ui/.../theme/Type.kt`: `import dev.zakyymuh.timebox.shared.R`
- Modify: `app/build.gradle.kts` (`implementation(project(":ui"))`), plus every importer of `DateField`/`softFieldColors`/`ifResumed`/`go`

**Interfaces:**
- Produces (all public, in `:ui`): `DailyTheme {}`, `Fresh.*` colours, `SoftCard`, `TopBar`, `RoundButton`, `Label`/`Small` text styles, `DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit)`, `softFieldColors()`, `NavBackStackEntry.ifResumed(block: () -> Unit)`, `NavHostController.go(route: String)`, `DailyAlarm(context, requestCode: Int, receiver: Class<out BroadcastReceiver>, clock: Clock = Clock.systemDefaultZone())`.

- [ ] **Step 1:** `git switch -c split-apps`, then do the moves and edits above. No behaviour changes.
- [ ] **Step 2:** `./gradlew :core:test` → BUILD SUCCESSFUL.
- [ ] **Step 3:** Check other sessions, commit (`refactor: shared :ui module for theme, components and DailyAlarm`), push the branch. Run: `gh run watch`. Expected: `:app:testDebugUnitTest` and `:app:assembleDebug` pass, with the same test count as `main`.

### Task 2: `:motor` app

**Files:**
- Modify: `settings.gradle.kts` (include `:motor`)
- Create: `motor/build.gradle.kts`, modelled on `app/build.gradle.kts`: `namespace = "dev.zakyymuh.motor"`, `applicationId = "dev.zakyymuh.motor"`, `versionCode = 1`, `versionName = "1.0"`, `signingConfigs.debug.storeFile = file("../app/debug.keystore")`, deps `:ui`, room, ksp, test deps. No glance, no datastore.
- Create: `motor/src/main/AndroidManifest.xml` (one launcher `MainActivity`, portrait, theme `@style/Theme.Motor`, no permissions)
- Create: `motor/src/main/res/values/{strings.xml (app_name "Motor"), themes.xml}`
- Copy (from `:app`, package `dev.zakyymuh.motor.*`): `data/ServiceData.kt` → `motor/.../data/ServiceData.kt`; `ui/motor/ServiceScreens.kt` → `motor/.../ui/ServiceScreens.kt`
- Create: `motor/.../data/MotorDatabase.kt`: `@Database(entities = [ServiceEntity::class], version = 1, exportSchema = false)`, `abstract fun serviceDao(): ServiceDao`, file name `"motor.db"`. Add a `@TypeConverters` copy only if `ServiceEntity` needs one (it stores `LocalDate`/`Instant`, so copy the converters it uses from `app/.../data/Converters.kt`).
- Create: `motor/.../ServiceLocator.kt` (`serviceRepository(context): ServiceRepository`)
- Create: `motor/.../MainActivity.kt` (edge-to-edge light bars as in Plants, `DailyTheme { MotorNav() }`)
- Create: `motor/.../MotorNav.kt`: routes `"service"` (start) and `"service/edit/{id}"`. On the start screen, `onBack` calls `activity.finish()`.
- Copy test: `app/src/test/.../data/ServiceRepositoryTest.kt` → `motor/src/test/java/dev/zakyymuh/motor/data/ServiceRepositoryTest.kt` (built on `MotorDatabase`), plus a `TestClock` copy.

- [ ] **Step 1:** Copy the test, retargeted at `MotorDatabase`.
- [ ] **Step 2:** Add the module, database, locator, nav and activity.
- [ ] **Step 3:** Add `./gradlew :motor:testDebugUnitTest :motor:assembleDebug` to CI (the full CI rewrite is in Task 5; a temporary step is fine). Check other sessions, commit (`feat: Motor app with the service log`), push, `gh run watch`. Expected: `ServiceRepositoryTest` passes and the APK builds.

### Task 3: `:habits` app

**Files:**
- Modify: `settings.gradle.kts` (include `:habits`)
- Create: `habits/build.gradle.kts`, like Motor but `namespace`/`applicationId` = `dev.zakyymuh.habits`, plus glance deps
- Create: `habits/src/main/AndroidManifest.xml`: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `MainActivity` (launcher), `MorningReceiver`, `TodayWidgetReceiver` (with `APPWIDGET_UPDATE` + `today_widget_info`), `BootReceiver` (`BOOT_COMPLETED`). Copy the existing manifest comments that still apply.
- Create: `habits/src/main/res/values/strings.xml` (`app_name` "Daily", `widget_description`, `tagline`), `themes.xml`, `res/xml/today_widget_info.xml` (copy)
- Copy (package `dev.zakyymuh.habits.*`): `data/{Entities,TimeboxDao,TimeboxRepository}.kt` and the converters they need; `ui/today/{TodayScreen,TodayViewModel}.kt`; `widget/TodayWidget.kt`; `nudge/{MorningReminder,MorningReceiver}.kt`
- Create: `habits/.../data/HabitsDatabase.kt`: entities `[DayEntity, TodoItemEntity]`, version 1, file `"habits.db"`
- Create: `habits/.../ServiceLocator.kt` (`repository(context)`, with `onChanged` updating `TodayWidget`, as now)
- Create: `habits/.../MainActivity.kt`: arms `MorningReminder` (channel + schedule), asks for notifications, `DailyTheme { TodayRoute() }` (Today screen directly, no NavHost)
- Create: `habits/.../nudge/BootReceiver.kt`: re-arms only `MorningReminder`
- Modify (in the copies): the widget click and morning-notification tap use `Intent(context, MainActivity::class.java)` with `NEW_TASK or CLEAR_TASK`. There's no `Destinations`.
- Copy test: `TimeboxRepositoryTest.kt` → `habits/src/test/.../data/`, on `HabitsDatabase`, plus `TestClock`.

- [ ] **Step 1:** Copy the test, retargeted at `HabitsDatabase`.
- [ ] **Step 2:** Add the module and the files above.
- [ ] **Step 3:** Add a CI step for `:habits:testDebugUnitTest :habits:assembleDebug`. Check other sessions, commit (`feat: Habits app with today's list, widget and morning reminder`), push, `gh run watch`. Expected: green.

### Task 4: Trim Plants: migration 7→8, remove habit and service code

**Files:**
- Modify: `app/.../data/Migrations.kt`: add `MIGRATION_7_8` (`DROP TABLE IF EXISTS` for `todo_item`, `day` and `service`) and append it to `ALL_MIGRATIONS`
- Modify: `app/.../data/TimeboxDatabase.kt`: entities `[PlantEntity, WateringEntity, DexEntity, AirTestEntity]`, `version = 8`, remove `dao()` and `serviceDao()`
- Delete from `:app`: `data/{Entities,TimeboxDao,TimeboxRepository,ServiceData}.kt`, `ui/today/*`, `ui/motor/*`, `widget/TodayWidget.kt`, `nudge/{MorningReminder,MorningReceiver}.kt`, `res/xml/today_widget_info.xml`, tests `data/{TimeboxRepositoryTest,ServiceRepositoryTest}.kt`. Remove converters that are now unused.
- Modify: `ServiceLocator.kt` (drop `repository`, `serviceRepository`, `TodayWidget`), `AppNav.kt` (drop the TODAY/SERVICE/SERVICE_EDIT composables and dashboard callbacks), `Destinations.kt` (drop `TODAY`/`SERVICE`/`SERVICE_EDIT`/`serviceEdit`; `from` accepts only `PLANTS`), `MainActivity.kt` and `nudge/BootReceiver.kt` (evening reminder only), `AndroidManifest.xml` (remove `MorningReceiver` and `TodayWidgetReceiver`; update the comment at the top), `res/values/strings.xml` (keep `app_name` "papacti"; drop `widget_description`; move `tagline` to Habits only if Plants no longer uses it)
- Modify: `ui/dashboard/DashboardViewModel.kt`: constructor `(plants: PlantRepository, dex: DexRepository, clock: Clock, random: Random = Random.Default)`. `DashboardState` loses `habit`, `cheer` and `motor`. `todayDate` = `LocalDate.now(clock)`, and `refresh()` re-reads it.
- Modify: `ui/dashboard/DashboardScreen.kt`: remove `onOpenHabit`, `onOpenService`, `HabitProgress` and `MotorCard`. The Plants `DoorCard` goes full width.
- Modify: `app/build.gradle.kts`: `versionCode = 3`, `versionName = "0.4"`, remove the glance deps
- Test: `app/src/test/.../data/MigrationTest.kt`, `ui/dashboard/DashboardViewModelTest.kt`, new `app/src/test/.../ui/DestinationsTest.kt`

- [ ] **Step 1: Write the failing tests**

In `MigrationTest`, add `a version 7 database upgrades to 8 with plants intact and habit and service tables gone`. Build a v7 file the same way the existing tests do (v1 SQL, then MIGRATION 1→7 SQL, or the v1 helper plus opening with migrations only up to 7). Insert one day, three items, one service (`AHASS`), the plant `Greenovia` with one watering, the dex entry `Astrophytum`, and one air test. Then open the current DB and assert:
```kotlin
assertEquals("Greenovia", current.plantDao().plants().single().name)
assertEquals(1, current.plantDao().waterings().size)
assertEquals("Astrophytum", current.dexDao().entries().single().name)
assertEquals(1, current.airTestDao().testsFlow().first().size)
val tables = current.openHelper.readableDatabase
    .query("SELECT name FROM sqlite_master WHERE type='table'").use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
assertFalse("day" in tables); assertFalse("todo_item" in tables); assertFalse("service" in tables)
```
`DestinationsTest` (Robolectric): `today is no longer a destination`, where `Destinations.from(Intent().putExtra(Destinations.EXTRA, "today"))` is `null`, and `plants still is`, where it returns `Destinations.PLANTS`.

- [ ] **Step 2:** Push and confirm in CI that both tests fail (before the code changes: v8 doesn't exist and `"today"` is still accepted).
- [ ] **Step 3:** Make the code changes above. Rewrite the existing v1–v7 migration tests so that each one still builds its old version, opens at v8, and asserts only on plant, dex and air-test data. Drop the `dao()`/`serviceDao()` assertions. The v6 test becomes "an upgrade from 6 keeps everything plant-side". Rewrite `DashboardViewModelTest` against the new constructor: delete the habit-card and motorcycle-card tests, and keep the collection, fun-fact, close-fact and after-midnight tests (midnight now drives `clock`).
- [ ] **Step 4:** Check other sessions, commit (`feat: Plants keeps only plant features; drop habit and service tables in v8`), push, `gh run watch`. Expected: all `:app` tests pass, including the two new ones.

### Task 5: CI and docs

**Files:**
- Modify: `.github/workflows/build.yml`: one test step `./gradlew :app:testDebugUnitTest :habits:testDebugUnitTest :motor:testDebugUnitTest`, one build step `./gradlew :app:assembleDebug :habits:assembleDebug :motor:assembleDebug`, and three uploads named `plants-debug-apk`, `habits-debug-apk` and `motor-debug-apk`. Replace the temporary steps from Tasks 2–3, and add `habits/build/reports/tests/` and `motor/build/reports/tests/` to the failure upload. Add a signing check: `apksigner verify --print-certs` (from `$ANDROID_HOME/build-tools/*/`) on each APK; fail if the three SHA-256 digests differ.
- Modify: `docs/BUILDING.md`: module table (`:core`, `:ui`, `:app` = Plants, `:habits`, `:motor`), the three artifacts, and the install order: update Plants over the old app first, then install Habits and Motor, re-add the widget, and allow notifications in Habits.

- [ ] **Step 1:** Edit both files.
- [ ] **Step 2:** Check other sessions, commit (`ci: test and build all three apps`), push, `gh run watch`. Expected: green, three APK artifacts, and the signing check passes.

### Task 6: Hand-off and phone check

- [ ] **Step 1:** Ask the owner before merging `split-apps` into `main` and pushing.
- [ ] **Step 2:** The owner installs the Plants APK over the old app. Expected: plants, photos, collection and soil tests are all there, and the dashboard shows no habit or motor cards.
- [ ] **Step 3:** The owner installs Habits and Motor, adds the widget, and reboots the phone. Expected: the next morning the 06:30 reminder arrives from Habits. If it doesn't, check `BootReceiver` and `MainActivity` scheduling.
