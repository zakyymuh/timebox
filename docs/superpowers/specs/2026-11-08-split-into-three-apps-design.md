# Split into three apps: Plants, Habits, Motor

Date: 2026-11-08 · Status: agreed with the owner in chat; names to come later

## Why

The app (installed as "Daily") has grown into three unrelated things on one dashboard:
the daily habit list, plant care, and the motorcycle service log. The owner wants each
to be its own app with its own icon. They accepted that habit and service entries
will be **re-entered by hand** in the new apps, so no data is carried across.

## Goals

- Three installable apps built from this one repo:
  - **Plants**: the existing install (`applicationId dev.zakyymuh.timebox`), updated
    in place. Every plant, watering, collection/wishlist entry, photo and soil test
    survives the update.
  - **Habits**: new install. The Today list (drag to reorder, motivation line),
    the home-screen widget, and the 06:30 morning reminder.
  - **Motor**: new install. The service log and its add/edit screen.
- All three look like one family: same theme, fonts, cards and buttons.
- CI tests all three and uploads three APKs.

## Non-goals

- Moving or exporting existing habit/service data (the owner will re-type it).
- Final app names and icons. Use the placeholder labels below; the owner picks names later.
- New features in any of the apps.
- Splitting `:core` into per-app rule modules.

## Anti-goals (a failure even if it builds)

- **Losing any plant data on update.** The Plants app's database must migrate forward,
  never `fallbackToDestructiveMigration`, and must keep the same `applicationId`
  and signing key (`app/debug.keystore`) so the update installs over the old app
  instead of forcing an uninstall.
- The new apps being signed with a different key per CI run. They use the same
  committed debug key, so later builds also install as upgrades.

## Module layout (builder's call where marked)

| Module | Kind | Holds |
|---|---|---|
| `:core` | Kotlin/JVM, unchanged | All rules, as now. Every app depends on it. |
| `:ui` *(name: builder's call)* | Android library | `ui/theme/*` (Fresh colours, `DailyTheme`, type), `ui/common/Fresh.kt` (SoftCard, TopBar, RoundButton…), the Inter font, the shared `DateField`/`softFieldColors` (moved out of `ui/plants` because Motor uses them too), and the `ifResumed`/`go` nav helpers if worth sharing. |
| `:app` | Android app, **Plants** | Plants, calendar, encyclopedia, collection/dex, soil air-space, `PlantReceiver`/`PlantActionReceiver`, 18:00 `EveningReminder`, the dashboard (trimmed). |
| `:habits` | Android app, **Habits** | `ui/today`, `widget/TodayWidget`, `TimeboxRepository`/`TimeboxDao`/`DayEntity`/`TodoItemEntity`, `MorningReminder`/`MorningReceiver`, `DailyAlarm`, its own `BootReceiver`. |
| `:motor` | Android app, **Motor** | `ui/motor`, `ServiceEntity`/`ServiceDao`/`ServiceRepository`. |

Note: the earlier chat said "morning and evening reminders" go to Habits. In fact only
the **06:30 morning** reminder is about habits; the **18:00 evening** reminder is the
plant-watering check and stays in Plants.

`DailyAlarm` is needed by both Plants and Habits. Put it in `:ui` (or a small shared
Android module) rather than copying it. *(My call.)*

Keep the existing `:app` directory name for Plants so git history, the keystore path
and CI paths keep working. *(My call.)*

### Identity of each app

| App | applicationId | Placeholder label | Opens on |
|---|---|---|---|
| Plants | `dev.zakyymuh.timebox` (unchanged, mandatory) | "papacti" (set by the owner in f2c06d2; keep it) | the trimmed dashboard |
| Habits | `dev.zakyymuh.habits` | "Daily" (takes over the old name) | Today screen directly |
| Motor | `dev.zakyymuh.motor` | "Motor" | service list directly |

The new applicationIds are my call. They can't be changed later without a reinstall,
so they're deliberately plain and don't depend on the final names. Labels live in each
module's `strings.xml` and can change at any time.

Kotlin package names: `dev.zakyymuh.timebox.*` stays for Plants. The new modules use
`dev.zakyymuh.habits.*` and `dev.zakyymuh.motor.*` as their `namespace`. Moved
classes may keep their old package if that's simpler. *(Builder's call.)*

## Data

- **Plants DB** (`timebox.db`): add `MIGRATION_7_8`, which drops the `day`,
  `todo_item` and `service` tables. Remove `DayEntity`, `TodoItemEntity` and
  `ServiceEntity` from `TimeboxDatabase` and bump to version 8. Extend
  `MigrationTest`: a v7 database holding plants, waterings, dex entries, air tests,
  days, items and services migrates to v8 with all the plant-side rows intact and the
  three tables gone. Renaming the database class/file is out of scope.
- **Habits DB**: new Room database (own file name), version 1, entities `day` and
  `todo_item`, schema identical to today's. No migrations.
- **Motor DB**: new Room database, version 1, entity `service`. No migrations.
- Each app gets its own small `ServiceLocator` holding only what it uses.
- Tests move with their code: `TimeboxRepositoryTest` → `:habits`,
  `ServiceRepositoryTest` → `:motor`. `DashboardViewModelTest` is updated for the
  trimmed dashboard.

## Plants app after the split

- **Dashboard:** remove the Habit door card, the habit progress, the habit cheer and
  the Motor card. Keep the Plants card, the Collection card and the fun fact. With one
  door card left, the builder lays it out so it doesn't look half-empty (a full-width
  card is fine).
- `DashboardViewModel` drops `TimeboxRepository` and `ServiceRepository`.
  `DashboardState` drops `habit`, `cheer` and `motor`. The date comes from the clock
  (or `PlantRepository`) instead of `timebox.todayDate()`.
- `Destinations` drops `TODAY`, `SERVICE` and `SERVICE_EDIT`. `Destinations.from`
  accepts only `PLANTS`.
- Manifest: remove `MorningReceiver` and `TodayWidgetReceiver`, and
  `today_widget_info.xml`. `BootReceiver` and `MainActivity` re-arm only the evening
  reminder.
- Leave the old notification channel `timebox_morning` alone. Deleting it is
  optional, and harmless either way.
- Bump `versionCode` (2 → 3) and `versionName`.

## Habits app

- Single-activity Compose app: `MainActivity` → `TodayScreen`, with no dashboard and no
  back target.
- Widget: `TodayWidget` + receiver + `today_widget_info.xml`. Tapping it opens the app.
- Morning reminder: 06:30, as now. Its notification tap opens the app (no destination
  routing needed with one screen).
- Asks for `POST_NOTIFICATIONS` on first launch; `RECEIVE_BOOT_COMPLETED` for its
  boot receiver.
- After installing, the owner has to add the widget to the home screen again. The old
  widget disappears when Plants is updated.

## Motor app

- Single activity: service list as the start screen → add/edit screen. The list's
  back button is hidden or closes the app.
- No notifications, no permissions.

## Build & CI

- Root `build.gradle.kts`: declare `com.android.library` with `apply false`,
  following the existing comment about declaring every plugin once at the root.
  Add it to `libs.versions.toml` at the same AGP version.
- `settings.gradle.kts`: include `:ui`, `:habits` and `:motor` under the same
  Android-SDK guard as `:app`. Keep `:core` building on SDK-less machines.
- Each app module points at the shared `app/debug.keystore` (relative path such as
  `../app/debug.keystore`, or move it to the root and update `:app`). *(Builder's call.)*
- `.github/workflows/build.yml`: run unit tests for `:app`, `:habits` and `:motor`
  (and `:ui` if it gets tests), assemble all three debug APKs, and upload them as three
  separately named artifacts.
- Update `docs/BUILDING.md` with the new modules and install order.

## Rollout order for the owner

1. Install the new Plants build over the old app. Plants data is kept; habits and
   service data are gone from it.
2. Install Habits and Motor, re-enter entries, and add the widget again.

## Left to the builder

The shared module's name, where `DailyAlarm` and the nav helpers live, how moved code
is packaged, the trimmed dashboard's exact layout, and the plan's task order. The plan
should leave the Plants app building and passing tests after every step.
