# Timebox Timeboxing v1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An Android app where each morning I write what the day is for, break it into blocks of a given length, then run them one at a time — seeing elapsed and remaining, nudged when a block's time is up, with a home-screen widget showing what I should be doing now.

**Architecture:** Unchanged from the parked plan: `:core` is a pure Kotlin JVM library holding every decision, `:app` is the Android shell. The whole block state machine — start, finish, advance, skip, overrun, elapsed — lives in `:core` and is tested in milliseconds with plain JUnit. `:app` adds Room, Compose and a Glance widget over it.

**Tech Stack:** Kotlin, Compose + Material 3, Room (KSP), Glance, WorkManager/AlarmManager for nudges. JUnit 4 for `:core`, Robolectric for `:app`.

**Spec:** `docs/superpowers/specs/2026-10-26-timeboxing-design.md`

**Parked:** `docs/superpowers/plans/2026-10-26-timebox-v1.md` — the app-blocking gate, deferred to v2. Its `:core` code (`GateDecider`, `ForegroundFilter`, `PassExpiryGuard`, `WidgetStateBuilder`) stays committed and passing; do not delete it, and do not wire it into anything here.

## Global Constraints

- Package root `dev.zakyymuh.timebox`; `:core` classes under `dev.zakyymuh.timebox.core`.
- `minSdk 24`, `compileSdk 37`, `targetSdk 37`, Java/Kotlin bytecode 17.
- **Toolchain, and it is load-bearing:** Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21. The previous pins (Gradle 9.5.1 / AGP 8.13.2) existed only because AGP 8 cannot run on Gradle 9.6+, and AGP 8 is now impossible because androidx requires compileSdk 37 and AGP 9.1+. AGP 9 previously appeared to be broken with Kotlin 2.4.21; that was a classloader-scope bug, fixed in `dd4c9ca`.
- **Every Gradle plugin is declared once in the root `build.gradle.kts` with `apply false`,** and applied in subprojects by id with no version. Not stylistic — a plugin requested in a subproject's own block lands in a child classloader scope the root-scoped Kotlin plugin cannot see, which cost three CI runs.
- **`:core` must not depend on Android.** `kotlin("jvm")` only.
- **The manifest declares no `INTERNET` permission.**
- All time handling goes through an injected `java.time.Clock`. No `Instant.now()` anywhere.
- **Elapsed time is always computed from a stored `Instant`,** never accumulated in a timer. A timer dies with the process; the day does not.
- Each task ends with a commit.

## Review Focus

1. **Two blocks running at once.** The UI makes "done, next" one tap, so the invariant cannot live in a screen. Starting a block must close any running one, in the domain. → Task 2.
2. **A running block must survive the app being killed.** Reopen an hour later and an hour must have elapsed. Any in-memory accumulation of elapsed time fails this. → Tasks 2 and 3.
3. **A block started at 23:40 and finished at 00:10 belongs to the 23:40 day.** Querying "today's plan" by a naive date range loses it, or worse, splits it. → Tasks 2 and 3.
4. **The "time is up" notification must fire once per block.** A nudge that repeats every minute while I deliberately overrun is an app I uninstall. → Task 6.
5. **A plan copied from a recent day must arrive empty of history.** Carrying yesterday's `startedAt`/`finishedAt` would show a day already finished before it began. → Task 2.

---

## File Structure

```
core/src/main/kotlin/dev/zakyymuh/timebox/core/
    Timebox.kt            Block, BlockStatus, DayTimebox
    Progress.kt           BlockProgress, Progress.of
    TimeboxEngine.kt      start / finishRunning / advance / skip / queries
    Blocks.kt             reorder, renumber, templateFrom
    DaySummary.kt         summarise
    (parked, untouched)   GateDecider, ForegroundFilter, PassExpiryGuard, WidgetStateBuilder
app/src/main/java/dev/zakyymuh/timebox/
    data/                 Entities, Converters, TimeboxDao, TimeboxDatabase, TimeboxRepository
    ui/today/             TodayScreen, TodayViewModel
    ui/plan/              PlanScreen, PlanViewModel
    nudge/                Nudges, BlockElapsedReceiver, MorningReceiver, BootReceiver
    widget/               TodayWidget, TodayWidgetReceiver, AdvanceAction
    (parked)              service/, ui/gate/ — left in place, not wired in
```

---

### Task 1: Toolchain move and a Compose skeleton

The riskiest task, so it goes first and alone. Nothing but an empty Compose screen, proving AGP 9 + compileSdk 37 + Compose + Room + Glance resolve and build on CI.

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `.github/workflows/build.yml`
- Modify: `app/src/main/java/dev/zakyymuh/timebox/ui/MainActivity.kt` (replace the spike's view code with a Compose `setContent`)

**Interfaces:**
- Produces: a buildable `:app` with Compose, Room+KSP and Glance on the classpath, and an APK artifact from CI.

- [ ] **Step 1: Resolve every version from the registries**

Do not pin from memory; the prior for Kotlin was two minors stale. Read `maven-metadata.xml` for each of: AGP, Compose BOM, activity-compose, lifecycle-viewmodel-compose, navigation-compose, room, KSP, glance-appwidget, datastore-preferences, kotlinx-coroutines, robolectric. Record the resolved values in the catalog with a comment saying they were resolved, not recalled.

- [ ] **Step 2: Move the toolchain**

Gradle wrapper to 9.8.2. AGP to 9.4.1. `compileSdk`/`targetSdk` 37. Keep `minSdk 24`. Keep the root-only plugin declarations. Add the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`, version = Kotlin version) and KSP, both declared at the root with `apply false`.

- [ ] **Step 3: Make CI able to build against SDK 37**

GitHub runners may not ship `platforms;android-37`. Add a step before the Gradle steps:

```yaml
- name: Ensure SDK 37
  run: yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-37" || true
```

Expected: the step succeeds whether or not the platform was already present.

- [ ] **Step 4: Replace MainActivity's body with Compose**

A `ComponentActivity` with `setContent { }` showing a Material 3 `Scaffold` and the text "Timebox". Delete nothing from `service/` or `ui/gate/` — they stay compiled and unused.

- [ ] **Step 5: Verify locally as far as possible**

Run: `./gradlew :core:test`
Expected: PASS, 38/38 — the parked gate tests must still pass after the toolchain move.

- [ ] **Step 6: Push and read CI**

Expected: `Test :core`, `Test :app`, `Build debug APK` and `Upload APK` all green. If AGP 9 fails, read the message before changing versions: a `NoClassDefFoundError` naming a class that exists in the artifact is a classloader-scope problem, not a missing dependency.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "build: move to AGP 9.4.1, compileSdk 37, Compose, Room and Glance"
```

---

### Task 2: The block state machine

All of it pure, all of it tested on the JVM. This is the task that decides whether the app is correct.

**Files:**
- Create: `core/src/main/kotlin/dev/zakyymuh/timebox/core/Timebox.kt`, `Progress.kt`, `TimeboxEngine.kt`, `Blocks.kt`, `DaySummary.kt`
- Test: `core/src/test/kotlin/dev/zakyymuh/timebox/core/TimeboxEngineTest.kt`, `ProgressTest.kt`, `BlocksTest.kt`, `DaySummaryTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class BlockStatus { PENDING, RUNNING, DONE, SKIPPED }

  data class Block(
      val id: Long,
      val position: Int,
      val title: String,
      val planned: Duration,
      val startedAt: Instant? = null,
      val finishedAt: Instant? = null,
      val skipped: Boolean = false,
  ) { val status: BlockStatus }

  data class DayTimebox(
      val date: LocalDate,
      val intention: String?,
      val blocks: List<Block>,      // ordered by position
  )

  data class BlockProgress(
      val elapsed: Duration,
      val remaining: Duration,      // floored at ZERO
      val overrunBy: Duration,      // ZERO unless past planned
  ) { val isOverrun: Boolean }

  object Progress { fun of(block: Block, now: Instant): BlockProgress? }

  class TimeboxEngine {
      fun start(day: DayTimebox, blockId: Long, now: Instant): DayTimebox
      fun finishRunning(day: DayTimebox, now: Instant): DayTimebox
      fun advance(day: DayTimebox, now: Instant): DayTimebox
      fun skip(day: DayTimebox, blockId: Long, now: Instant): DayTimebox
      fun running(day: DayTimebox): Block?
      fun nextPending(day: DayTimebox, afterPosition: Int? = null): Block?
  }

  object Blocks {
      fun reorder(blocks: List<Block>, fromIndex: Int, toIndex: Int): List<Block>
      fun renumber(blocks: List<Block>): List<Block>
      fun templateFrom(day: DayTimebox, newDate: LocalDate): DayTimebox
  }

  data class DaySummary(
      val plannedTotal: Duration, val actualTotal: Duration,
      val done: Int, val skipped: Int, val total: Int,
  )
  object DaySummaries { fun summarise(day: DayTimebox, now: Instant): DaySummary }
  ```

- [ ] **Step 1: Write the failing `Block.status` and `Progress` tests**

```kotlin
@Test fun `a block with no timestamps is PENDING`()
@Test fun `a started but unfinished block is RUNNING`()
@Test fun `a finished block is DONE`()
@Test fun `a skipped block is SKIPPED even when it was started`()
@Test fun `progress is null for a block that never started`()
@Test fun `elapsed counts from startedAt to now while running`()
@Test fun `elapsed counts from startedAt to finishedAt once done`()   // Review Focus 2
@Test fun `remaining is floored at zero rather than going negative`()
@Test fun `overrunBy is zero before the planned length is reached`()
@Test fun `overrunBy is the excess once past the planned length`()
@Test fun `a block running across midnight reports the full elapsed time`()  // Review Focus 3
```

- [ ] **Step 2: Run to verify they fail** — `./gradlew :core:test --tests '*ProgressTest*'`, FAIL unresolved.

- [ ] **Step 3: Implement `Timebox.kt` and `Progress.kt`**

`status`: skipped → SKIPPED, else finishedAt != null → DONE, else startedAt != null → RUNNING, else PENDING. `Progress.of` returns null when `startedAt` is null; `elapsed = Duration.between(startedAt, finishedAt ?: now)`.

- [ ] **Step 4: Run to verify they pass.**

- [ ] **Step 5: Write the failing `TimeboxEngine` tests** — Review Focus 1

```kotlin
@Test fun `starting a pending block makes it running`()
@Test fun `starting a block finishes whichever block was running`()          // Review Focus 1
@Test fun `at most one block is ever running`()                              // Review Focus 1
@Test fun `starting the already-running block changes nothing`()
@Test fun `the finished block keeps the instant it was finished at`()
@Test fun `finishRunning on a day with nothing running changes nothing`()
@Test fun `advance finishes the running block and starts the next pending one`()
@Test fun `advance skips over blocks that are already done`()
@Test fun `advance on the last block finishes it and starts nothing`()
@Test fun `advance with nothing running starts the first pending block`()
@Test fun `skip marks a pending block skipped without starting it`()
@Test fun `skip on the running block finishes and marks it skipped`()
@Test fun `advance does not start a skipped block`()
@Test fun `restarting a done block clears its finish time`()
@Test fun `running returns null when every block is done`()
@Test fun `nextPending ignores done and skipped blocks`()
```

- [ ] **Step 6: Run to verify they fail.**

- [ ] **Step 7: Implement `TimeboxEngine`**

Every method returns a new `DayTimebox`; nothing mutates. `start` finishes any running block at `now` first, then sets `startedAt = now`, `finishedAt = null`, `skipped = false` on the target. `advance` = `finishRunning` then start the next PENDING block **strictly after** the position of the block that was running — "next" means forward, so finishing the middle block moves on rather than back to an earlier untouched one, and reaching the end starts nothing. With nothing running, it starts the earliest PENDING block. (Corrected during Task 2: this originally read "lowest-position PENDING block", which two tests caught as wrong.)

- [ ] **Step 8: Run to verify they pass.**

- [ ] **Step 9: Write the failing `Blocks` and `DaySummary` tests** — Review Focus 5

```kotlin
@Test fun `reorder moves a block and renumbers positions contiguously from zero`()
@Test fun `reorder to the same index changes nothing`()
@Test fun `renumber closes gaps left by a deletion`()
@Test fun `templateFrom clears every startedAt, finishedAt and skipped flag`()  // Review Focus 5
@Test fun `templateFrom keeps titles, planned lengths and order`()
@Test fun `templateFrom sets the new date and drops the old intention`()
@Test fun `summarise totals the planned durations of every block`()
@Test fun `summarise totals actual time from done blocks only`()
@Test fun `summarise counts done, skipped and total`()
@Test fun `summarise includes the running block's elapsed time in actualTotal`()
```

- [ ] **Step 10: Run to verify they fail, implement, run to verify they pass.**

- [ ] **Step 11: Commit**

```bash
git add core
git commit -m "feat: the block state machine, pure and tested"
```

---

### Task 3: Persistence

**Files:**
- Create: `app/src/main/java/dev/zakyymuh/timebox/data/Entities.kt`, `Converters.kt`, `TimeboxDao.kt`, `TimeboxDatabase.kt`, `TimeboxRepository.kt`, `ServiceLocator.kt`
- Test: `app/src/test/java/dev/zakyymuh/timebox/data/TimeboxRepositoryTest.kt` (Robolectric, in-memory Room)

**Interfaces:**
- Consumes: everything from Task 2.
- Produces:
  ```kotlin
  class TimeboxRepository(private val dao: TimeboxDao, private val clock: Clock) {
      fun today(): Flow<DayTimebox?>
      suspend fun todayNow(): DayTimebox?
      suspend fun saveIntention(text: String)
      suspend fun replaceBlocks(date: LocalDate, blocks: List<Block>)
      suspend fun apply(day: DayTimebox)          // persists a DayTimebox the engine returned
      fun recentPlans(limit: Int): Flow<List<DayTimebox>>
      suspend fun recentPlansNow(limit: Int): List<DayTimebox>
      suspend fun dayOf(date: LocalDate): DayTimebox?
  }
  ```
  Two tables: `day` keyed by `date`, and `block` with a `date` foreign key, `position`, `title`, `plannedSeconds`, `startedAtMillis`, `finishedAtMillis`, `skipped`.

- [ ] **Step 1: Write the failing tests**

Build the repository with `Clock.fixed` in `ZoneId.of("Asia/Jakarta")`.

```kotlin
@Test fun `saveIntention then todayNow returns it`()
@Test fun `todayNow is null when the only plan is yesterday's`()
@Test fun `replaceBlocks stores blocks in position order`()
@Test fun `replaceBlocks replaces rather than appends`()
@Test fun `apply persists a started block and its start instant`()
@Test fun `a running block reloaded from the database still reports its elapsed time`()  // Review Focus 2
@Test fun `a block started before midnight stays on the day it started`()                // Review Focus 3
@Test fun `recentPlansNow returns previous days, most recent first, excluding today`()
@Test fun `deleting a day's blocks leaves other days untouched`()
```

- [ ] **Step 2: Run to verify they fail.**

- [ ] **Step 3: Implement the entities, DAO, converters and database**

`Instant` as epoch millis, `LocalDate` as ISO text, `Duration` as seconds. Index `block.date`. A block belongs to the `day` row it was created under, which is what keeps a block started at 23:40 on its own day regardless of when it finished.

- [ ] **Step 4: Implement `TimeboxRepository` and `ServiceLocator`, run to verify they pass.**

- [ ] **Step 5: Commit**

---

### Task 4: Today

The screen I look at all day.

**Files:**
- Create: `ui/today/TodayScreen.kt`, `ui/today/TodayViewModel.kt`
- Modify: `ui/MainActivity.kt`, add `ui/TimeboxNavHost.kt`
- Test: `app/src/test/java/dev/zakyymuh/timebox/ui/TodayViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object Routes { const val TODAY = "today"; const val PLAN = "plan" }
  data class TodayUiState(
      val intention: String?,
      val running: Block?,
      val progress: BlockProgress?,
      val blocks: List<Block>,
      val summary: DaySummary,
      val hasPlan: Boolean,
  )
  class TodayViewModel(repo: TimeboxRepository, engine: TimeboxEngine, clock: Clock) {
      val state: StateFlow<TodayUiState>
      fun start(blockId: Long)
      fun advance()
      fun skip(blockId: Long)
  }
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `hasPlan is false when no plan exists for today`()           // Review Focus: empty state
@Test fun `state exposes the running block and its progress`()
@Test fun `progress is null when nothing is running`()
@Test fun `advance finishes the running block and starts the next`()
@Test fun `start on a second block leaves only one running`()
@Test fun `skip removes a block from the pending queue`()
@Test fun `the ticking clock updates elapsed without a database write`()
```

- [ ] **Step 2: Run to verify they fail.**

- [ ] **Step 3: Implement `TodayViewModel`**

A one-second ticker in `viewModelScope` drives `progress` recomputation from the stored instants. It must not write to the database — only `start`, `advance` and `skip` persist.

- [ ] **Step 4: Implement `TodayScreen`**

The running block large at the top: its title, elapsed, and remaining — or "over by 12m" in the warning colour when `isOverrun`. One primary button, "Done, next". Below it the rest of the day as a list, each row showing status, title and planned length, done ones de-emphasised. When `hasPlan` is false the whole screen is a single call to action to plan the day — that is the state every morning starts in, so it gets designed, not left as an empty list.

- [ ] **Step 5: Run to verify they pass. Commit.**

---

### Task 5: Planning the day

**Files:**
- Create: `ui/plan/PlanScreen.kt`, `ui/plan/PlanViewModel.kt`
- Test: `app/src/test/java/dev/zakyymuh/timebox/ui/PlanViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class DraftBlock(val title: String, val minutes: Int)
  data class PlanUiState(
      val intention: String,
      val drafts: List<DraftBlock>,
      val templates: List<DayTimebox>,
      val plannedTotal: Duration,
      val canSave: Boolean,
  )
  class PlanViewModel(repo: TimeboxRepository, clock: Clock) {
      val state: StateFlow<PlanUiState>
      fun onIntentionChanged(text: String)
      fun addBlock(title: String, minutes: Int)
      fun editBlock(index: Int, title: String, minutes: Int)
      fun removeBlock(index: Int)
      fun move(fromIndex: Int, toIndex: Int)
      fun useTemplate(day: DayTimebox)
      fun save()
  }
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `canSave is false with no blocks`()
@Test fun `canSave is true once one block has a title and a positive length`()
@Test fun `addBlock rejects a blank title`()
@Test fun `addBlock rejects a zero or negative length`()
@Test fun `plannedTotal sums the drafts`()
@Test fun `move reorders the drafts`()
@Test fun `removeBlock closes the gap`()
@Test fun `useTemplate fills the drafts from a previous day with no history`()  // Review Focus 5
@Test fun `save writes the intention and the blocks for today`()
@Test fun `save on a day that already has a plan replaces it`()
```

- [ ] **Step 2: Run to verify they fail, implement, verify they pass.**

- [ ] **Step 3: Implement `PlanScreen`**

A free-text field for "What is today for?", then the block list: each row a title field and a minutes field, with drag-to-reorder and swipe-to-delete. A row of chips at the top offering recent days as templates, which is the one-tap morning path. The total planned time is always visible — seeing "11h 30m" is how you learn the plan is fiction.

- [ ] **Step 4: Commit.**

---

### Task 6: Nudges — Review Focus 4

**Files:**
- Create: `nudge/Nudges.kt`, `nudge/BlockElapsedReceiver.kt`, `nudge/MorningReceiver.kt`, `nudge/BootReceiver.kt`
- Modify: `AndroidManifest.xml` (`POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, receivers), `ui/today/TodayViewModel.kt`
- Test: `core/src/test/kotlin/.../NextFireTest.kt` already exists and is reused; add `app/src/test/java/.../NudgesTest.kt`

**Interfaces:**
- Consumes: `NextFire` from the parked work — it is general daily-time logic and is reused as-is.
- Produces:
  ```kotlin
  class Nudges(context: Context, alarmManager: AlarmManager, clock: Clock) {
      fun scheduleMorning(at: LocalTime)
      fun scheduleBlockElapsed(blockId: Long, at: Instant)
      fun cancelBlockElapsed()
      companion object { const val CHANNEL_ID = "timebox" }
  }
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `scheduling a block's elapsed nudge cancels any earlier one`()   // Review Focus 4
@Test fun `a block already past its planned time schedules nothing`()
@Test fun `the morning nudge is skipped when today already has a plan`()
@Test fun `the elapsed nudge names the next block when there is one`()
@Test fun `the elapsed nudge is not re-posted for a block that already fired`()  // Review Focus 4
```

- [ ] **Step 2: Run to verify they fail.**

- [ ] **Step 3: Implement**

One pending intent for the block-elapsed nudge, keyed by a single fixed request code, so scheduling a new one replaces the old — there is only ever one running block, so there is only ever one pending nudge. Fire it at `startedAt + planned`. Its notification says what is up and what is next, with a "Start next" action that advances without opening the app. `TodayViewModel.start`/`advance` reschedule; `BootReceiver` restores the morning alarm and re-arms the elapsed nudge from the stored start instant.

- [ ] **Step 4: Verify, commit.**

---

### Task 7: The widget

**Files:**
- Create: `widget/TodayWidget.kt`, `widget/TodayWidgetReceiver.kt`, `widget/AdvanceAction.kt`, `res/xml/today_widget_info.xml`
- Create: `core/src/main/kotlin/dev/zakyymuh/timebox/core/WidgetView.kt`
- Test: `core/src/test/kotlin/dev/zakyymuh/timebox/core/WidgetViewTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  sealed interface WidgetView {
      data class NeedsPlan(val templates: List<String>) : WidgetView
      data class Running(val title: String, val remaining: Duration,
                         val isOverrun: Boolean, val doneCount: Int, val total: Int) : WidgetView
      data class Idle(val nextTitle: String?, val doneCount: Int, val total: Int) : WidgetView
      data object DayComplete : WidgetView
  }
  object WidgetViews { fun of(day: DayTimebox?, now: Instant, templates: List<String>): WidgetView }
  ```
  Named `WidgetView` rather than reusing the parked `WidgetState`, which belongs to the gate.

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test fun `no plan for today gives NeedsPlan with the template names`()
@Test fun `a running block gives Running with its remaining time`()
@Test fun `an overrun running block sets isOverrun`()
@Test fun `a plan with nothing running gives Idle naming the next block`()
@Test fun `every block done or skipped gives DayComplete`()
@Test fun `Running counts done blocks out of the total`()
```

- [ ] **Step 2: Run to verify they fail, implement, verify.**

- [ ] **Step 3: Implement the Glance widget**

`Running` shows the title, the remaining time big, and a "Done, next" button running `AdvanceAction`. `Idle` shows a "Start" button. `NeedsPlan` shows template buttons and a "Plan today" button opening `Routes.PLAN`. `AdvanceAction` reaches the repository through `ServiceLocator` and must work from a cold-started process.

- [ ] **Step 4: Keep it current**

Update the widget after every `start`, `advance` and `skip`, and from the block-elapsed receiver. A once-a-minute refresh while a block runs is acceptable; a timer that ticks all day is not.

- [ ] **Step 5: Commit.**

---

## Done when

On the phone, with notifications allowed:

1. A reminder arrives in the morning; I plan the day, or fill it from a recent one in a single tap.
2. I tap the first block when I actually start, and it runs.
3. Today shows what I am doing, how long it has taken, and how long is left.
4. "Done, next" finishes a block and starts the following one, from the screen, the notification, or the widget.
5. When a block's time is up I get told once, and overrunning on purpose is not nagged.
6. Killing the app and reopening it an hour later shows an hour elapsed, not zero.
