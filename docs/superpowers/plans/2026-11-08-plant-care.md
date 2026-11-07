# Plant Care Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** "Still moist" check, "last watered" line, interval learned from history, and Watered/Still-moist buttons on the 6pm notification.

**Architecture:** All rules in `:core` (snooze-aware next due, last-watered copy, median-gap advice). One nullable column via Room 2→3 migration. Notification actions go through a pure `PlantActions.handle` tested with Robolectric; the receiver is a thin shell.

**Tech Stack:** as before. **Spec:** `docs/superpowers/specs/2026-11-08-plant-care-design.md`

## Global Constraints
- Pinned toolchain, no new dependencies or permissions; `:core` Android-free.
- Copy (exact): `Still moist`, `Checked <name>` (+ `Undo`), `Watered today` / `Watered yesterday` / `Watered N days ago` / `Not watered yet`, `You usually water every N days`, `Use N`, notification buttons `Watered` / `Still moist`.
- Snooze = tomorrow (today + 1). Advice: ≥ 4 waterings, median of the last 5 gaps (rounded half up), shown when |median − interval| ≥ 1, repeating plants only.
- Migration 2→3 keeps every row; never destructive.

## Review Focus
1. **Snoozed, then watered** — the snooze is gone; next due = today + interval. → repository test, Task 2.
2. **Snoozed two days in a row** — moves to tomorrow each time, never further. → core test, Task 1.
3. **Upgrading a phone on schema 2 with a plant** — plant and waterings intact. → migration test, Task 2.
4. **Irregular history (one huge gap from a holiday)** — median ignores it; no silly suggestion. → core test, Task 1.
5. **Notification "Watered" pressed for a plant deleted meanwhile** — nothing crashes, nothing inserted. → `PlantActions` test, Task 4.

---

### Task 1: Rules in `:core`
**Files:** modify `core/.../plants/Plant.kt` (`Plant.snoozedUntil: LocalDate? = null`), `PlantSchedule.kt`, `PlantsOverview.kt` (`PlantRow.lastWatered: LocalDate?`, `PlantRow.suggestedInterval: Int?`; `PlantCopy.lastWatered(date: LocalDate?, today: LocalDate)`, `PlantCopy.checked(name)`, `PlantCopy.usually(days)`, `PlantCopy.use(days)`); create `core/.../plants/IntervalAdvice.kt` (`fun suggest(plant: Plant, waterings: List<Watering>): Int?`). Tests: `PlantScheduleTest`, `PlantsOverviewTest`, new `IntervalAdviceTest`.
- [ ] RED: `snooze moves next due to tomorrow when later than the schedule`; `snooze earlier than the schedule changes nothing`; `snoozing again next day moves one more day` (Focus 2); `one-time plant respects snooze`; `last watered copy` (today / yesterday / 3 days ago / never); `advice needs four waterings`; `advice is the median of the last five gaps` (gaps 7,8,8,9 with interval 4 → 8); `advice ignores one holiday gap` (gaps 7,7,30,7 → 7, Focus 4); `no advice when within a day`; `no advice for one-time plants`; `PlantRow carries lastWatered and suggestedInterval`.
- [ ] Run → FAIL; implement; run → PASS; full `:core:test` green; commit `feat(core): soil-moist snooze, last watered, interval advice`.

### Task 2: Storage
**Files:** `app/.../data/PlantEntities.kt` (`snoozedUntil: LocalDate? = null`, mapped both ways), `TimeboxDatabase.kt` (version 3), `Migrations.kt` (`MIGRATION_2_3`: `ALTER TABLE plant ADD COLUMN snoozedUntil TEXT`), `ServiceLocator.kt` (add migration), `PlantRepository.kt`:
```kotlin
suspend fun snooze(plantId: Long): LocalDate?          // sets today+1, returns previous value for Undo
suspend fun restoreSnooze(plantId: Long, previous: LocalDate?)
suspend fun setInterval(plantId: Long, days: Int)      // used by "Use N"; anchor unchanged
// water() now also clears snoozedUntil
```
Tests: `MigrationTest` gains `a version 2 database with a plant upgrades intact` (Focus 3; v2 schema = v1 + MIGRATION_1_2 SQL); `PlantRepositoryTest`: `still moist moves a due plant to tomorrow`, `watering clears the snooze` (Focus 1), `undoing a snooze restores the previous date`, `use suggested interval re-plans`.
- [ ] RED via CI (with Task 3/4 tests), implement, GREEN; commit `feat(data): snoozedUntil with a 2→3 migration`.

### Task 3: Plant page
**Files:** `ui/plants/PlantsViewModel.kt` (`snooze(plantId)`, `useInterval(plantId, days)`, undo events become `sealed interface PlantUndo { Watered(id, name); Checked(plantId, name, previous) }`), `PlantsScreen.kt`.
Due card: below the big button row, a second row `RoundButton("🤚")`-free text button **Still moist** (pill, `Fresh.Page` on pastel). Every card/line shows `PlantCopy.lastWatered` in `Label`. When `row.suggestedInterval != null`: pastel tip under that plant — `You usually water every N days` + pill button `Use N`.
Tests in `PlantViewModelsTest`: `still moist emits a Checked undo and moves the plant to tomorrow`.
- [ ] RED/GREEN via CI; commit `feat: still moist, last watered and interval tip on the plant page`.

### Task 4: Notification actions
**Files:** create `app/.../nudge/PlantActions.kt` (`object PlantActions { const val ACTION_WATER = "dev.zakyymuh.timebox.WATER"; const val ACTION_CHECK = "dev.zakyymuh.timebox.CHECK"; const val EXTRA_PLANT = "plant"; suspend fun handle(repo: PlantRepository, action: String?, plantId: Long): Boolean }`), `PlantActionReceiver.kt` (goAsync → handle → cancel notification `EveningReminder.NOTIFICATION_ID`), manifest (`exported=false`), `PlantReceiver.kt` (when `overview.due.size == 1`, add two actions with `PendingIntent.getBroadcast` request codes 201/202, `FLAG_IMMUTABLE`).
Tests `app/src/test/.../nudge/PlantActionsTest.kt`: `water action records a watering`; `check action snoozes`; `unknown plant does nothing and returns false` (Focus 5); `unknown action does nothing`.
- [ ] RED/GREEN via CI; commit `feat: Watered and Still moist buttons on the 6pm notification`.

### Task 5: On the phone
Install (upgrade in place; migration 2→3 runs on the owner's real data with Greenovia), confirm no crash and the plant still listed (`run-as` DB read: `snoozedUntil` column exists, Greenovia intact). Owner checks the buttons.
