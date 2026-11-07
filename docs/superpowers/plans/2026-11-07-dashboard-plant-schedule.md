# Dashboard and Plant Schedule Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Open the app on a two-card dashboard (Daily habit → existing today page, Plant schedule → new page), and add plants whose next watering is counted from the day you actually watered, with a calendar behind an icon and a 6pm reminder.

**Architecture:** Every schedule decision (next due, overdue, calendar dots, status copy, form validation) is a pure function in `:core` with JUnit tests. `:app` adds two Room tables behind a hand-written 1→2 migration, a `PlantRepository`, Navigation Compose screens, and a second daily alarm that shares code with the morning one.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose + Material 3, Navigation Compose, Room 2.8.5 (KSP), AlarmManager, JUnit 4, Robolectric 4.17 (`@Config(sdk = [33])`).

**Spec:** `docs/superpowers/specs/2026-11-07-dashboard-plant-schedule-design.md`

## Global Constraints

- Toolchain pinned: Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21, compileSdk 37, minSdk 26, Java 17 bytecode. Do not bump anything in `gradle/libs.versions.toml`; do not apply `org.jetbrains.kotlin.android`.
- `:core` stays pure Kotlin: no dependency with `android` in its coordinates.
- No new permissions. No `INTERNET`. (Exact alarms are an open question for the owner — out of this plan.)
- Visual style: existing `DailyTheme` — `Paper`/`Stone` colours, `Inter` + `Mono` families, `MonoLabel` for uppercase labels. Dynamic colour stays off.
- Owner's words, verbatim: card names **Daily habit** and **Plant schedule**; button **I watered it**; reminder at **18:00**.
- Due dates are `LocalDate`s from the repository's `Clock`, never times.
- **Verification:** `:core` runs locally: `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home ./gradlew :core:test`. `:app` cannot build locally (Android SDK licences not accepted by the owner), so `:app` steps are verified by pushing to `timebox-v1` and reading CI (`build.yml`: `:core:test`, `:app:testDebugUnitTest`, `:app:assembleDebug`). Batch `:app` tasks per push where noted; read the failure log on red before changing anything.

## Review Focus

1. **Upgrading a phone that already has daily lists** — the owner's three items must survive v1→v2. → migration test in Task 5.
2. **Double-tapping "I watered it", then Undo once** — still counts as watered today; next due unchanged. → repository test in Task 6.
3. **Changing a plant's interval after it has been watered** — next due = latest watering + *new* interval, immediately. → core test in Task 2.
4. **Blank name, interval 0 or 91, "last watered" in the future** — Save is disabled; nothing invalid reaches the database. → core test in Task 3.
5. **App left open across midnight** — on returning to the app, "due today" reflects the new day without a restart. → `refresh()` on resume in Tasks 7–8, checked on device in Task 11.

---

## File map

| File | Responsibility |
|---|---|
| `core/.../core/plants/Plant.kt` | `Plant`, `Recurrence`, `Watering`, `PlantColour` |
| `core/.../core/plants/PlantSchedule.kt` | next due + status (the owner's rule) |
| `core/.../core/plants/PlantsOverview.kt` | due/upcoming grouping, all user-facing copy |
| `core/.../core/plants/PlantForm.kt` | add/edit validation |
| `core/.../core/plants/CalendarMarks.kt` | solid/hollow dots for a month |
| `app/.../data/PlantEntities.kt`, `PlantDao.kt`, `Migrations.kt`, `PlantRepository.kt` | storage |
| `app/.../ui/Destinations.kt`, `ui/AppNav.kt` | routes, NavHost, entry-point routing |
| `app/.../ui/dashboard/` | dashboard screen + VM |
| `app/.../ui/plants/` | plant page, edit, calendar screens + VMs, `PlantColours.kt` |
| `app/.../nudge/DailyAlarm.kt`, `EveningReminder.kt`, `PlantReceiver.kt` | 18:00 reminder |

`core/...` = `core/src/main/kotlin/dev/zakyymuh/timebox`, tests mirror under `core/src/test/kotlin/...`. `app/...` = `app/src/main/java/dev/zakyymuh/timebox`, tests under `app/src/test/java/...`.

---

### Task 1: Stable debug signing, so installs upgrade instead of wiping data

**Files:**
- Create: `app/debug.keystore` (committed; debug keys are not secret)
- Modify: `app/build.gradle.kts` (`signingConfigs`, `versionCode = 2`, `versionName = "0.3"`; also fix the stale "Redmi Note 8 on Android 9+" comment → "Redmi Note 14 on Android 16")

- [ ] **Step 1:** Generate the keystore with the standard debug values:
  `keytool -genkeypair -v -keystore app/debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"`
- [ ] **Step 2:** In `android {}` add `signingConfigs { getByName("debug") { storeFile = file("debug.keystore"); storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android" } }` and `buildTypes { debug { signingConfig = signingConfigs.getByName("debug") } }`.
- [ ] **Step 3:** Commit `chore: commit a debug key so every build upgrades in place`. (Verified with Task 5's push: Task 11 checks two CI APKs print the same SHA-256 via `keytool -printcert -jarfile`.)

---

### Task 2: The schedule rule in `:core`

**Files:**
- Create: `core/.../core/plants/Plant.kt`, `core/.../core/plants/PlantSchedule.kt`
- Test: `core/src/test/kotlin/dev/zakyymuh/timebox/core/plants/PlantScheduleTest.kt`

**Interfaces — Produces:**
```kotlin
enum class PlantColour { MOSS, OCHRE, DUSK, CLAY, PLUM, SLATE }   // stored by name

sealed interface Recurrence {
    data class EveryDays(val days: Int) : Recurrence
    data class Once(val on: LocalDate) : Recurrence
}

data class Plant(
    val id: Long, val name: String, val colour: PlantColour,
    val recurrence: Recurrence,
    /** "Last watered" given at creation. Not history: never drawn as a solid dot. */
    val anchor: LocalDate,
)

data class Watering(val id: Long, val plantId: Long, val date: LocalDate)

sealed interface PlantStatus {
    data class Due(val daysLate: Int) : PlantStatus      // 0 = due today
    data class Upcoming(val inDays: Int) : PlantStatus   // >= 1
    data object Done : PlantStatus                       // one-time plant, watered
}

object PlantSchedule {
    /** Next due date, or null for a watered one-time plant. [waterings] may include other plants'. */
    fun nextDue(plant: Plant, waterings: List<Watering>): LocalDate?
    fun status(plant: Plant, waterings: List<Watering>, today: LocalDate): PlantStatus
}
```
Rule: repeating → `max(latest watering date for this plant, anchor) + days`. One-time → `on` if it has no waterings, else null (watering early or late both finish it).

- [ ] **Step 1: Write the failing tests** (today = `2026-11-07`, Monstera every 7 days, anchor `2026-11-01`):
  - `next due counts from the anchor when never watered` → `2026-11-08`
  - `next due counts from the day actually watered, not the old due date` → waterings on `2026-11-05` (owner's Thursday case: late vs the 3rd) → `2026-11-12`
  - `watering early also restarts the clock` → anchor `11-01`, watered `11-03` → `11-10`
  - `a watering older than the anchor is ignored` → watered `10-20`, anchor `11-01` → `11-08`
  - `other plants' waterings do not count` → watering with `plantId = 99` → `11-08`
  - `changing the interval re-plans from the latest watering` → same plant with `EveryDays(3)`, watered `11-05` → `11-08`
  - `status is Due(0) on the due day, Due(2) two days later, Upcoming(1) the day before`
  - `one-time plant is Due(n) after its date until watered, then Done` → `Once(11-04)`, today `11-07` → `Due(3)`; with a watering → `Done`, `nextDue == null`
  - `one-time plant in the future is Upcoming` → `Once(11-10)` → `Upcoming(3)`
- [ ] **Step 2:** Run `./gradlew :core:test --tests '*PlantScheduleTest'` → FAIL (unresolved references).
- [ ] **Step 3:** Implement `Plant.kt` and `PlantSchedule.kt` as above (use `ChronoUnit.DAYS.between`).
- [ ] **Step 4:** Run the same command → PASS; then `./gradlew :core:test` → all green.
- [ ] **Step 5:** Commit `feat(core): next watering counts from the day you actually watered`.

---

### Task 3: Overview, copy and form validation in `:core`

**Files:**
- Create: `core/.../core/plants/PlantsOverview.kt`, `core/.../core/plants/PlantForm.kt`
- Test: `PlantsOverviewTest.kt`, `PlantFormTest.kt` (same test package)

**Interfaces — Consumes:** Task 2. **Produces:**
```kotlin
data class PlantRow(val plant: Plant, val status: PlantStatus, val nextDue: LocalDate?)

data class PlantsOverview(val due: List<PlantRow>, val upcoming: List<PlantRow>) {
    /** Shown as the NEXT WATERING card when nothing is due. */
    val next: PlantRow? get() = if (due.isEmpty()) upcoming.firstOrNull() else null
    val isEmpty: Boolean   // no plants that are due or upcoming
    companion object { fun of(plants: List<Plant>, waterings: List<Watering>, today: LocalDate): PlantsOverview }
}

object PlantCopy {
    fun whenLabel(status: PlantStatus): String        // "today", "1 day late", "3 days late", "tomorrow", "in 4 days", "done"
    fun interval(recurrence: Recurrence): String      // "every 3 days", "every day", "once"
    fun dashboard(overview: PlantsOverview): String   // see tests
    fun notification(overview: PlantsOverview): String?  // null when nothing due
    fun habit(summary: DaySummary): String            // "2 / 5 done", "Nothing written yet"
}

data class PlantForm(
    val name: String, val colour: PlantColour,
    val repeats: Boolean, val everyDays: Int, val onceDate: LocalDate, val lastWatered: LocalDate,
) {
    fun errors(today: LocalDate): Set<FormError>
    fun isValid(today: LocalDate): Boolean = errors(today).isEmpty()
}
enum class FormError { NAME_BLANK, NAME_TOO_LONG, INTERVAL_OUT_OF_RANGE, LAST_WATERED_IN_FUTURE }
```
Ordering: `due` most-late first, then by name; `upcoming` soonest first, then by name. `Done` plants are in neither list.

- [ ] **Step 1: Write the failing tests:**
  - `due sorts most overdue first and upcoming soonest first`
  - `next is the soonest upcoming plant only when nothing is due`
  - `a watered one-time plant appears in neither list`
  - `dashboard copy`: no plants → `"No plants yet"`; one due → `"1 due today"`; two due → `"2 due today"`; none due, Monstera tomorrow → `"Next: Monstera tomorrow"`; in 4 days → `"Next: Monstera in 4 days"`
  - `notification copy`: none → `null`; Basil due → `"Basil needs water"`; two due → `"2 plants need water"`
  - `whenLabel and interval copy` → exact strings listed in the interface
  - `habit copy`: `DaySummary(2,5)` → `"2 / 5 done"`; empty → `"Nothing written yet"`
  - `form rejects blank or whitespace name, name over 40 chars, interval 0 and 91, last watered tomorrow` (Review Focus 4); `form accepts interval 1 and 90`; `interval is not checked for one-time plants`
- [ ] **Step 2:** `./gradlew :core:test --tests '*PlantsOverviewTest' --tests '*PlantFormTest'` → FAIL.
- [ ] **Step 3:** Implement. Name is trimmed before checks.
- [ ] **Step 4:** Same command → PASS; full `:core:test` green.
- [ ] **Step 5:** Commit `feat(core): plant overview, copy and form rules`.

---

### Task 4: Calendar dots in `:core`

**Files:**
- Create: `core/.../core/plants/CalendarMarks.kt`
- Test: `CalendarMarksTest.kt`

**Interfaces — Consumes:** Tasks 2. **Produces:**
```kotlin
enum class MarkKind { WATERED, PROJECTED }
data class Mark(val plantId: Long, val colour: PlantColour, val kind: MarkKind)

object CalendarMarks {
    fun forMonth(month: YearMonth, plants: List<Plant>, waterings: List<Watering>, today: LocalDate): Map<LocalDate, List<Mark>>
}
```
Rules (spec §5): every watering in the month → `WATERED` on its date (any date ≤ today). Projections only on dates ≥ today: if the plant is `Due`, a `PROJECTED` mark on **today** (not the missed date); then for repeating plants, `nextDue + k·days` (for overdue plants, start from `today + days`) for every date inside the month. One-time: one projection on `max(on, today)` if not done. Marks within a day ordered by plant id.

- [ ] **Step 1: Write the failing tests** (today `2026-11-07`):
  - `past waterings are solid and never projected` → watering `11-01` gives one WATERED on `11-01`, nothing else before today
  - `projections repeat every interval to the end of the month` → every 7, next due `11-08` → PROJECTED on 8, 15, 22, 29
  - `an overdue plant is projected on today, then every interval from today` → due `11-04` every 3 → PROJECTED on 7, 10, 13 …
  - `watering today replaces today's projection with a solid mark` → watered `11-07`, every 3 → WATERED on 7, PROJECTED on 10
  - `a month entirely in the past has only solid marks` → `YearMonth.of(2026, 10)`
  - `a future month continues the projection chain` → December for the every-7 plant → 6, 13, 20, 27
  - `a done one-time plant shows only its watering`
- [ ] **Step 2:** `./gradlew :core:test --tests '*CalendarMarksTest'` → FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** → PASS; full `:core:test` green.
- [ ] **Step 5:** Commit `feat(core): calendar marks — past is fact, future is projection`.

---

### Task 5: Room tables and the 1→2 migration

**Files:**
- Create: `app/.../data/PlantEntities.kt`, `app/.../data/PlantDao.kt`, `app/.../data/Migrations.kt`
- Modify: `app/.../data/TimeboxDatabase.kt` (entities, `version = 2`, `abstract fun plantDao(): PlantDao`), `app/.../ServiceLocator.kt` (`.addMigrations(MIGRATION_1_2)`; never `fallbackToDestructiveMigration`)
- Test: `app/src/test/java/dev/zakyymuh/timebox/data/MigrationTest.kt`

**Interfaces — Produces:**
```kotlin
@Entity(tableName = "plant")
data class PlantEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
    val colour: String, val everyDays: Int?, val onceDate: LocalDate?, val anchor: LocalDate)
// exactly one of everyDays / onceDate is non-null; toModel()/fromModel() live beside it

@Entity(tableName = "watering", indices = [Index("plantId")],
    foreignKeys = [ForeignKey(PlantEntity::class, ["id"], ["plantId"], onDelete = ForeignKey.CASCADE)])
data class WateringEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long, val date: LocalDate, val wateredAt: Instant)

@Dao interface PlantDao {
    @Query("SELECT * FROM plant ORDER BY name") fun plantsFlow(): Flow<List<PlantEntity>>
    @Query("SELECT * FROM plant ORDER BY name") suspend fun plants(): List<PlantEntity>
    @Query("SELECT * FROM plant WHERE id = :id") suspend fun plant(id: Long): PlantEntity?
    @Query("SELECT * FROM watering") fun wateringsFlow(): Flow<List<WateringEntity>>
    @Query("SELECT * FROM watering") suspend fun waterings(): List<WateringEntity>
    @Upsert suspend fun upsertPlant(plant: PlantEntity): Long
    @Query("DELETE FROM plant WHERE id = :id") suspend fun deletePlant(id: Long)
    @Insert suspend fun insertWatering(w: WateringEntity): Long
    @Query("DELETE FROM watering WHERE id = :id") suspend fun deleteWatering(id: Long)
}
val MIGRATION_1_2: Migration
```
Migration SQL (must match what Room generates for the entities above, exactly):
```sql
CREATE TABLE IF NOT EXISTS `plant` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colour` TEXT NOT NULL, `everyDays` INTEGER, `onceDate` TEXT, `anchor` TEXT NOT NULL)
CREATE TABLE IF NOT EXISTS `watering` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plantId` INTEGER NOT NULL, `date` TEXT NOT NULL, `wateredAt` INTEGER NOT NULL, FOREIGN KEY(`plantId`) REFERENCES `plant`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
CREATE INDEX IF NOT EXISTS `index_watering_plantId` ON `watering` (`plantId`)
```

- [ ] **Step 1: Write the failing test** `a version 1 database with lists upgrades with every item intact` (Review Focus 1): with `SQLiteDatabase.openOrCreateDatabase(file, null)` create the v1 schema —
  ```sql
  CREATE TABLE IF NOT EXISTS `day` (`date` TEXT NOT NULL, `note` TEXT, PRIMARY KEY(`date`))
  CREATE TABLE IF NOT EXISTS `todo_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `position` INTEGER NOT NULL, `text` TEXT NOT NULL, `doneAt` INTEGER)
  CREATE INDEX IF NOT EXISTS `index_todo_item_date` ON `todo_item` (`date`)
  ```
  — insert day `2026-11-07` and items `cuci piring`, `repotting`, `beberes`, set `version = 1`, close; open with `Room.databaseBuilder(..., file.name).addMigrations(MIGRATION_1_2).allowMainThreadQueries()`; assert `dao().items(2026-11-07).map { it.text } == listOf("cuci piring","repotting","beberes")` and `plantDao().plants().isEmpty()`. (Room throws on open if the migrated schema differs from the entities — that is the schema check.)
  Second test: `deleting a plant deletes its waterings`.
- [ ] **Step 2:** (CI) — batch with Task 6; expected red until Step 3.
- [ ] **Step 3:** Implement entities, DAO, migration, database and locator changes.
- [ ] **Step 4:** Push together with Tasks 1 and 6; CI `Test :app` green.
- [ ] **Step 5:** Commit `feat(data): plant and watering tables, with a migration that keeps your lists`.

---

### Task 6: `PlantRepository`

**Files:**
- Create: `app/.../data/PlantRepository.kt`
- Modify: `app/.../ServiceLocator.kt` (`fun plantRepository(context: Context): PlantRepository`, same lazy pattern)
- Test: `app/src/test/java/dev/zakyymuh/timebox/data/PlantRepositoryTest.kt` (same Robolectric/in-memory setup and fixed Jakarta clock as `TimeboxRepositoryTest`, now = `2026-11-07T11:00:00Z`)

**Interfaces — Consumes:** Tasks 2, 3, 5. **Produces:**
```kotlin
class PlantRepository(private val dao: PlantDao, private val clock: Clock) {
    fun todayDate(): LocalDate
    fun plantsFlow(): Flow<List<Plant>>
    fun wateringsFlow(): Flow<List<Watering>>
    suspend fun overviewNow(): PlantsOverview          // for the 18:00 receiver
    suspend fun plant(id: Long): Plant?
    suspend fun save(id: Long, form: PlantForm): Long  // id 0 = new; returns id; requires form.isValid
    suspend fun delete(id: Long)
    suspend fun water(plantId: Long): Long             // date = today, wateredAt = clock.instant(); returns watering id for Undo
    suspend fun removeWatering(wateringId: Long)
}
```
`save` maps `repeats=false` → `onceDate`, `everyDays = null`; anchor = `form.lastWatered` (for one-time plants, today). Name stored trimmed.

- [ ] **Step 1: Write the failing tests:**
  - `a new plant is due 7 days after the last watered date given` → save Monstera every 7, lastWatered `11-01` → `overviewNow().upcoming.single().nextDue == 2026-11-08`
  - `watering makes it due interval days from today` → water → nextDue `11-14`
  - `watering twice then undoing once still counts as watered today` (Review Focus 2) → water, water, removeWatering(second) → nextDue `11-14`
  - `undoing the only watering restores the previous due date` → `11-08`
  - `editing the interval keeps history and re-plans` → water, then save same id with every 3 → nextDue `11-10`, waterings count 1
  - `save rejects an invalid form` → `assertThrows<IllegalArgumentException>`
- [ ] **Step 2–4:** Implement; push with Task 5 → CI `Test :app` green.
- [ ] **Step 5:** Commit `feat(data): plant repository`.

---

### Task 7: Navigation, dashboard, and entry-point routing

**Files:**
- Create: `app/.../ui/Destinations.kt`, `app/.../ui/AppNav.kt`, `app/.../ui/dashboard/DashboardScreen.kt`, `app/.../ui/dashboard/DashboardViewModel.kt`
- Modify: `app/.../ui/MainActivity.kt` (build VMs via factories as today; `setContent { DailyTheme { AppNav(start = Destinations.from(intent)) } }`), `app/.../nudge/MorningReceiver.kt` and `app/.../widget/TodayWidget.kt` (open with `Destinations.intent(context, Destinations.TODAY)`)

**Interfaces — Produces:**
```kotlin
object Destinations {
    const val DASHBOARD = "dashboard"; const val TODAY = "today"; const val PLANTS = "plants"
    const val CALENDAR = "plants/calendar"; const val EDIT = "plants/edit/{id}"   // id 0 = new
    fun edit(id: Long): String
    const val EXTRA = "destination"
    /** NEW_TASK | CLEAR_TASK so onCreate always runs and back always lands on the dashboard. */
    fun intent(context: Context, destination: String): Intent
    fun from(intent: Intent?): String?   // TODAY or PLANTS, else null
}
class DashboardViewModel(timebox: TimeboxRepository, plants: PlantRepository) : ViewModel() {
    val state: StateFlow<DashboardState>   // DashboardState(date: LocalDate?, habit: String, plants: String)
    fun refresh()                           // re-reads today; call on resume
}
```
NavHost always starts at `DASHBOARD`; when `from(intent)` is non-null and `savedInstanceState == null`, navigate to it once, so Back returns to the dashboard. Dashboard copy comes from `PlantCopy.habit` / `PlantCopy.dashboard`. Layout per spec §1: `MonoLabel` "DAILY", date, two full-width cards with name, status line, `→`.

- [ ] **Step 1:** Implement (UI has no unit tests in this project; the copy is covered by Task 3).
- [ ] **Step 2:** Push with Tasks 8–9 → CI green.
- [ ] **Step 3:** Commit `feat: dashboard as the front door`.

---

### Task 8: Plant schedule page and edit screen

**Files:**
- Create: `app/.../ui/plants/PlantColours.kt`, `PlantsScreen.kt`, `PlantsViewModel.kt`, `EditPlantScreen.kt`, `EditPlantViewModel.kt`

**Interfaces — Consumes:** Tasks 3, 6, 7. **Produces:**
```kotlin
@Composable fun PlantColour.toColor(): Color   // light/dark pairs below
class PlantsViewModel(repo: PlantRepository) : ViewModel() {
    val overview: StateFlow<PlantsOverview?>
    val undo: SharedFlow<Long>       // emits watering id after water(); screen shows "Watered · Undo"
    fun water(plantId: Long); fun undo(wateringId: Long); fun refresh()
}
class EditPlantViewModel(repo: PlantRepository, id: Long) : ViewModel() {
    val form: StateFlow<PlantForm>; val canSave: StateFlow<Boolean>
    fun update(form: PlantForm); fun save(onDone: () -> Unit); fun delete(onDone: () -> Unit)
}
```
Colours (light / dark): MOSS `0xFF5E8C61`/`0xFF8FBF8F`, OCHRE `0xFFB7833E`/`0xFFE0B26A`, DUSK `0xFF5B7FA6`/`0xFF8EB1D9`, CLAY `0xFFB5654A`/`0xFFE0937A`, PLUM `0xFF8A5A8C`/`0xFFC08FC2`, SLATE `0xFF5F6B73`/`0xFF9AA6AE`.

Page per spec §2: top bar `‹`, "PLANT SCHEDULE", calendar icon → `CALENDAR`, `+` → `edit(0)`. If `due` non-empty: a card per due plant with colour dot, name, `PlantCopy.interval`, `whenLabel`, and a large **I watered it** button; then `NEXT` list. Else if `next != null`: **NEXT WATERING** card (dot, name, `EEEE d MMM · whenLabel`) and a `LATER` list. Not-due rows and the NEXT WATERING card have a small water-drop icon button ("watered early"). Tapping a name → `edit(id)`. Empty → "No plants yet" + hint to tap `+`. Undo via `SnackbarHost`, ~5 s. `refresh()` from `LifecycleResumeEffect` (Review Focus 5).

Edit per spec §3/§7: name field, six colour swatches, Repeats / One time switch; repeats → "Every [N] days" number field and "Last watered" date (new plants only, default today); one time → date. Material 3 `DatePickerDialog`. Save disabled unless `canSave`. Existing plant: Delete with an `AlertDialog` confirm ("Delete Basil and its watering history?").

- [ ] **Step 1:** Implement.
- [ ] **Step 2:** Push with Tasks 7, 9 → CI green.
- [ ] **Step 3:** Commit `feat: plant schedule page and editing`.

---

### Task 9: Calendar screen

**Files:**
- Create: `app/.../ui/plants/CalendarScreen.kt`, `app/.../ui/plants/CalendarViewModel.kt`

**Interfaces — Consumes:** Tasks 4, 6. **Produces:**
```kotlin
class CalendarViewModel(repo: PlantRepository) : ViewModel() {
    val state: StateFlow<CalendarState>  // month: YearMonth, today, marks: Map<LocalDate, List<Mark>>, selected: LocalDate?, selectedWaterings: List<Pair<Plant, Watering>>
    fun previousMonth(); fun nextMonth(); fun select(date: LocalDate?); fun removeWatering(id: Long)
}
```
Monday-first month grid, today boxed, dots below the number: WATERED filled, PROJECTED outlined, max 3 dots then `+n`. `‹ ›` step months. Tapping a past/today day with waterings opens a bottom sheet listing them, each with Remove (spec §7). Legend under the grid: "● watered  ○ planned".

- [ ] **Step 1:** Implement.
- [ ] **Step 2:** Push Tasks 7–9 → CI `:app:testDebugUnitTest` and `:app:assembleDebug` green.
- [ ] **Step 3:** Commit `feat: plant calendar`.

---

### Task 10: The 18:00 reminder

**Files:**
- Create: `app/.../nudge/DailyAlarm.kt`, `app/.../nudge/EveningReminder.kt`, `app/.../nudge/PlantReceiver.kt`
- Modify: `MorningReminder.kt` (delegate scheduling to `DailyAlarm`; behaviour unchanged), `BootReceiver.kt` (also `EveningReminder(...).schedule()`), `MainActivity.kt` (ensure channel + schedule, as for morning), `AndroidManifest.xml` (`PlantReceiver`, `exported=false`)

**Interfaces — Produces:**
```kotlin
class DailyAlarm(context: Context, requestCode: Int, receiver: Class<out BroadcastReceiver>, clock: Clock = Clock.systemDefaultZone()) {
    fun schedule(at: LocalTime); fun cancel()     // body moved verbatim from MorningReminder.schedule
}
class EveningReminder(context: Context) {
    fun schedule(); fun ensureChannel()
    companion object { const val CHANNEL_ID = "timebox_plants"; const val NOTIFICATION_ID = 2
        const val REQUEST_CODE = 200; val DEFAULT_TIME: LocalTime = LocalTime.of(18, 0) }
}
```
Channel name "Plants", description "When a plant needs water." `PlantReceiver` mirrors `MorningReceiver`: re-arm first, `goAsync()`, `ServiceLocator.plantRepository(...).overviewNow()`, post `PlantCopy.notification(...)` if non-null (or forced with `EXTRA_FORCE`), title = that text, body "Tap to open your plant schedule.", content intent `Destinations.intent(ctx, Destinations.PLANTS)`.

- [ ] **Step 1:** Implement.
- [ ] **Step 2:** Push → CI green.
- [ ] **Step 3:** Commit `feat: 6pm reminder when a plant needs water`.

---

### Task 11: On the phone

Uses `adb` at `/private/tmp/claude-501/-Users-zakyymuh-IdeaProjects-timebox/6534ff1c-35ee-4389-b28a-821fe409b1a9/scratchpad/tools/platform-tools/adb` (re-download platform-tools if the scratchpad was cleared).

- [ ] **Step 1: Save the owner's data** before the one-time key change: `adb exec-out run-as dev.zakyymuh.timebox tar c databases > timebox-db-backup.tar` (scratchpad). Confirm it contains `timebox.db`.
- [ ] **Step 2:** Download the CI APK; check its signer: `keytool -printcert -jarfile app-debug.apk` matches `keytool -list -v -keystore app/debug.keystore -storepass android`.
- [ ] **Step 3:** Uninstall the old build (different key), install the new one (owner taps `Daily.apk` if "Install via USB" is still off), then restore: `adb shell run-as dev.zakyymuh.timebox tar x < backup` **before first launch**, so Room runs the 1→2 migration on the real data.
- [ ] **Step 4:** Launch; screenshot the dashboard: "Daily habit · 0 / 3 done" (or the current count) and "Plant schedule · No plants yet". Open Daily habit: the three items are there. No crash in `adb logcat -b crash`.
- [ ] **Step 5:** With the owner, add Basil (every 3 days, last watered 4 days ago) and Monstera (every 7, last watered today). Screenshot: Basil card with **I watered it**, Monstera under NEXT. Tap it → Undo appears → page switches to NEXT WATERING. Open the calendar → solid dot today, hollow dots ahead.
- [ ] **Step 6:** Force the evening notification: `adb shell am broadcast -n dev.zakyymuh.timebox/.nudge.PlantReceiver --ez force true`; tapping it opens the plant page; Back → dashboard. `adb shell dumpsys alarm | grep -A2 dev.zakyymuh.timebox` shows two alarms, 06:30 and 18:00.
- [ ] **Step 7:** Report to the owner with screenshots; any defect goes back through superpowers:systematic-debugging, not a guess.
