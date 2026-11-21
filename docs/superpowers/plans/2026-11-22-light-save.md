# Light check save button Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A floating 💾 button on Light check that saves the current lux, its band and the time into the database.

**Architecture:** New `LightReadingEntity` + `LightReadingDao` + `LightRepository` in `app/.../data/LightData.kt` (same shape as `NoteData.kt`), DB 16 → 17 via `MIGRATION_16_17`. `LightScreen` gets the repository from `AppNav` and calls it from a FAB.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Robolectric + JUnit4 tests.

**Spec:** `docs/superpowers/specs/2026-11-22-light-save-design.md`

## Global Constraints

- Table name `light_readings`; columns `id` (autogenerate), `lux REAL NOT NULL`, `band TEXT NOT NULL` (the `LightBand` name, e.g. `"Bright"`), `takenAt INTEGER NOT NULL` (epoch millis, via the existing `Instant` converter).
- `TimeboxDatabase` version 17; migration only creates the table.
- No toast, snackbar, list or "Saved" text. Only feedback: icon 💾 → ✓ for ~1 s.
- FAB hidden when there is no light sensor; tap does nothing while `lux == null`.

## Review Focus

- Saving the same moment twice (double tap) → two rows; acceptable, no crash.
- Negative or NaN sensor value → saved lux is coerced to `>= 0`, NaN saved as 0, band `Dark`.
- Upgrading an existing v16 install keeps all other data (migration test).
- Leaving the screen right after tapping → the insert is short and launched from `rememberCoroutineScope`; losing only the ✓ animation is fine.
- Time is taken from the injected `Clock`, not the system, so tests are deterministic.

---

### Task 1: Light readings storage

**Files:**
- Create: `app/src/main/java/dev/zakyymuh/timebox/data/LightData.kt`
- Modify: `app/src/main/java/dev/zakyymuh/timebox/data/TimeboxDatabase.kt` (entity list, version 17, `abstract fun lightDao(): LightReadingDao`)
- Modify: `app/src/main/java/dev/zakyymuh/timebox/data/Migrations.kt` (`MIGRATION_16_17`, append to `ALL_MIGRATIONS`)
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ServiceLocator.kt` (`fun lightRepository(context: Context): LightRepository`, same pattern as `soilRepository`)
- Test: `app/src/test/java/dev/zakyymuh/timebox/data/LightRepositoryTest.kt`, `app/src/test/java/dev/zakyymuh/timebox/data/MigrationTest.kt`

**Interfaces:**
- Produces:
  - `@Entity(tableName = "light_readings") data class LightReadingEntity(id: Long = 0, lux: Float, band: String, takenAt: Instant)`
  - `@Dao interface LightReadingDao { @Insert suspend fun insert(r: LightReadingEntity): Long; @Query("SELECT * FROM light_readings ORDER BY takenAt DESC, id DESC") suspend fun all(): List<LightReadingEntity> }`
  - `class LightRepository(dao: LightReadingDao, clock: Clock) { suspend fun save(lux: Float): Long }`
  - `ServiceLocator.lightRepository(context): LightRepository`

- [ ] **Step 1: Write the failing tests** in `LightRepositoryTest` (Robolectric, in-memory DB, `TestClock(Instant.parse("2026-11-22T05:41:00Z"), ZoneId.of("Asia/Jakarta"))`, like `SoilRepositoryTest`):
  - `saving keeps the lux, its band and the clock's time`: `save(12_300f)` → one row, `lux == 12_300f`, `band == "Bright"`, `takenAt == Instant.parse("2026-11-22T05:41:00Z")`.
  - `each save is its own reading, newest first`: save 3_100f, move clock +1 h, save 25_000f → `all().map { it.band } == listOf("Sun", "Medium")`.
  - `a negative or NaN reading is saved as 0, too dark`: `save(-5f)` and `save(Float.NaN)` → both `lux == 0f`, `band == "Dark"`.
  - In `MigrationTest`: `the upgrade to 17 keeps the collection and adds an empty light table that works` — build a v16 file the way the v16 test does (`Callback(16)`, migrations `endVersion <= 16`), insert one dex row, `openCurrent()`, assert the dex row is intact, insert a `LightReadingEntity` and read it back via `all()`.

- [ ] **Step 2: Run to see them fail**
  Run: `./gradlew :app:testDebugUnitTest --tests '*LightRepositoryTest' --tests '*MigrationTest'`
  Expected: compile failure (`LightRepository` unresolved).

- [ ] **Step 3: Implement** the entity, DAO, repository (`clean = if (lux.isNaN()) 0f else lux.coerceAtLeast(0f)`; band = `LightLevel.band(clean).name`; time = `clock.instant()`), `MIGRATION_16_17`:
  ```sql
  CREATE TABLE IF NOT EXISTS `light_readings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lux` REAL NOT NULL, `band` TEXT NOT NULL, `takenAt` INTEGER NOT NULL)
  ```
  database version 17, and `ServiceLocator.lightRepository`.

- [ ] **Step 4: Run the tests**
  Run: `./gradlew :app:testDebugUnitTest`
  Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/dev/zakyymuh/timebox/data app/src/main/java/dev/zakyymuh/timebox/ServiceLocator.kt app/src/test/java/dev/zakyymuh/timebox/data docs/superpowers
  git commit -m "feat: Light check readings have a table to be saved in (DB v17)"
  ```

### Task 2: Floating save button

**Files:**
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/light/LightScreen.kt`
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/AppNav.kt:228-230`

**Interfaces:**
- Consumes: `LightRepository.save(lux: Float): Long`, `ServiceLocator.lightRepository(context)`
- Produces: `fun LightScreen(onBack: () -> Unit, onSave: suspend (Float) -> Unit)`

- [ ] **Step 1: Wire it** — `AppNav`: `LightScreen(onBack = …, onSave = { ServiceLocator.lightRepository(context).save(it) })`.
- [ ] **Step 2: Add the FAB** — `Scaffold(floatingActionButton = …)`, shown only when `sensor != null`. `FloatingActionButton` with `containerColor = Fresh.Green`, content text `"💾"` or `"✓"`, `Modifier.semantics { contentDescription = "Save this reading" }`. On click: if `lux != null`, `scope.launch { onSave(lux); saved = true; delay(1_000); saved = false }`. Raise the bottom `Spacer` from 32.dp to 96.dp so the FAB doesn't cover the last card. Update the KDoc: replace "Nothing is saved." with "The 💾 button saves the current reading with its time."
- [ ] **Step 3: Build and test**
  Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
  Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Check on a phone/emulator** — open Light check, tap 💾: icon shows ✓ then 💾 again; no other text appears.
- [ ] **Step 5: Commit**
  ```bash
  git add app/src/main/java/dev/zakyymuh/timebox/ui
  git commit -m "feat: a floating 💾 on Light check saves the current light with its date and time"
  ```
