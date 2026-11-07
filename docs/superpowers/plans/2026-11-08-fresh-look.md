# Fresh Look Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle every screen to white + pastel green (always light, Inter only, big rounded cards, pills, round buttons) and add a Today list to the dashboard that lets the owner water due plants and tick today's items in one tap, with Undo.

**Architecture:** What the Today list contains, its order and all new copy are pure functions in `:core` with JUnit tests. Theme tokens and a small set of shared composables (`common/Fresh.kt`) replace the paper theme; each screen is then rebuilt on them. Repositories, scheduling, storage and reminders are untouched.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose + Material 3, Robolectric 4.17 for ViewModel tests.

**Spec:** `docs/superpowers/specs/2026-11-08-fresh-look-design.md`

## Global Constraints

- Toolchain pinned; no new dependencies; `:core` stays Android-free.
- Tokens (exact): Page `#FFFFFF`, Card `#F5F7F3`, Ink `#1B1D1A`, Ink soft `#6B7068`, Pastel green `#CDEBD5`, Green `#5DB37E`, Green deep `#1F4D33`.
- Always light: ignore the system dark setting; status-bar icons dark.
- Inter only. JetBrains Mono and uppercase letter-spaced labels removed from the UI.
- Shapes: cards 28dp, rows 20dp, pills fully round, round buttons 48dp (56dp main watering button). No outlines.
- Pastel green only for "needs you now" and selection.
- Copy (exact): greeting `Hello, Zaky 👋`; empty Today `All done for today 🌿`; snackbars `Watered <name>` / `Ticked <text>` with action `Undo`.
- No change to repositories' behaviour, scheduling, storage, reminders, widget.
- Verification as before: `:core` locally (`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home ./gradlew :core:test -Pkotlin.incremental=false`), `:app` via CI on `timebox-v1`; device via adb (USB install now works; taps blocked unless "USB debugging (Security settings)").

## Review Focus

1. **Ticking from Today by mistake, then Undo** — the item is back, unticked, and back in Today. → VM test, Task 3.
2. **Nothing written today but a plant is due** — Today shows just the plant, no empty-state text. → core test, Task 1.
3. **Overdue plants** — listed before plants due today, with "2 days late". → core test, Task 1.
4. **Dashboard left open past midnight** — on return, Today shows the new day's items and due plants. → VM test, Task 3.
5. **Long item text or plant name** — wraps to at most 2 lines with ellipsis; the round button never gets pushed off. → `maxLines = 2, overflow = Ellipsis` on every card title (Tasks 3–6), checked on device in Task 7 with a 60-character item.

---

### Task 1: Today list and copy in `:core`

**Files:** Create `core/.../core/TodayAgenda.kt`; modify `core/.../core/plants/PlantsOverview.kt` (`PlantCopy`); test `core/src/test/kotlin/dev/zakyymuh/timebox/core/TodayAgendaTest.kt`, add cases to `PlantsOverviewTest.kt`.

**Interfaces — Produces:**
```kotlin
sealed interface TodayEntry {
    data class Water(val row: PlantRow) : TodayEntry
    data class Tick(val item: TodoItem) : TodayEntry
}
object TodayAgenda {
    /** Due/overdue plants in overview.due order, then today's unticked items by position. */
    fun of(day: DayList, overview: PlantsOverview): List<TodayEntry>
    const val ALL_DONE = "All done for today 🌿"
    fun ticked(text: String): String   // "Ticked cuci piring"
}
// PlantCopy gains:
fun watered(name: String): String     // "Watered Basil"
```

- [ ] **Step 1: Failing tests** — `plants come first, most overdue first, then unticked items by position`; `ticked items and upcoming plants are left out`; `nothing written but a plant due gives just the plant` (Focus 2); `overdue plant keeps its days-late status` (Focus 3: entry's `row.status == Due(2)`); `everything done gives an empty list`; `ticked copy` → `"Ticked cuci piring"`; `watered copy` → `"Watered Basil"`.
- [ ] **Step 2:** `./gradlew :core:test --tests '*TodayAgendaTest' --tests '*PlantsOverviewTest' -Pkotlin.incremental=false` → FAIL (unresolved).
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Same → PASS; full `:core:test` green.
- [ ] **Step 5:** Commit `feat(core): the Today list and its copy`.

---

### Task 2: Tokens, always-light theme, Inter-only type, shared components

**Files:** Modify `app/.../ui/theme/Color.kt` (replace `Paper`/`Stone`/`Emerald` with `object Fresh { Page, Card, Ink, InkSoft, Pastel, Green, GreenDeep }`), `Theme.kt` (`DailyTheme` takes no dark flag; one `lightColorScheme`: background/surface = Page, surfaceVariant/surfaceContainer = Card, onBackground/onSurface = Ink, onSurfaceVariant = InkSoft, primary = Ink, onPrimary = Page, secondary = Green, onSecondary = Page, secondaryContainer = Pastel, onSecondaryContainer = GreenDeep, tertiary = Green, outline = InkSoft, error `#C0453A`), `Type.kt` (drop `Mono`; `headlineLarge` Inter Bold 30sp; `headlineMedium` Inter Bold 24sp; `titleMedium` Inter SemiBold 16sp; replace `MonoLabel`/`MonoSmall` with `Label` (Inter Medium 13sp) and `Small` (Inter Normal 12sp) and update every usage), `MainActivity.kt` (`enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(Page, Page), navigationBarStyle = SystemBarStyle.light(Page, Page))` before `setContent`); delete `app/src/main/res/font/jetbrains_mono_regular.ttf`; create `app/.../ui/common/Fresh.kt`, delete `ui/common/Paper.kt` and move its users over.

**Interfaces — Produces** (`ui/common/Fresh.kt`):
```kotlin
@Composable fun RoundButton(glyph: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    container: Color = Fresh.Card, content: Color = Fresh.Ink, size: Dp = 48.dp, description: String? = null)
@Composable fun SoftCard(modifier: Modifier = Modifier, color: Color = Fresh.Card, corner: Dp = 28.dp,
    onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)   // padding 20dp
@Composable fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Fresh.Page, textColor: Color = Fresh.Ink)
@Composable fun TopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {})  // round ‹, bold title
@Composable fun SectionTitle(text: String)   // Inter SemiBold 20sp, sentence case
```
- [ ] **Step 1:** Implement (styling; covered by Task 7's device check). Every screen must still compile: swap `GlyphButton`→`RoundButton`, `PaperCard`→`SoftCard`, `PaperTopBar`→`TopBar`, `SectionLabel`→`SectionTitle`, `MonoLabel`→`Label`, `MonoSmall`→`Small`.
- [ ] **Step 2:** Push with Task 3 → CI green.
- [ ] **Step 3:** Commit `feat: white and pastel green, always light, Inter only`.

---

### Task 3: Dashboard with greeting, two cards and Today

**Files:** Modify `ui/dashboard/DashboardViewModel.kt`, `DashboardScreen.kt`, `ui/AppNav.kt`; test `app/src/test/java/dev/zakyymuh/timebox/ui/dashboard/DashboardViewModelTest.kt` (Robolectric + in-memory DB + `TestClock` + `Dispatchers.setMain(UnconfinedTestDispatcher())`, as `PlantViewModelsTest`; move `TestClock` to `app/src/test/.../TestClock.kt` and share it).

**Interfaces — Consumes:** Task 1, `TimeboxRepository.toggle(itemId)`, `PlantRepository.water/removeWatering`. **Produces:**
```kotlin
data class DashboardState(val date: LocalDate?, val habit: String, val plants: String, val today: List<TodayEntry>?)
sealed interface DashboardUndo {
    data class Watered(val wateringId: Long, val name: String) : DashboardUndo
    data class Ticked(val itemId: Long, val text: String) : DashboardUndo
}
class DashboardViewModel(timebox: TimeboxRepository, plants: PlantRepository) {
    val state: StateFlow<DashboardState>; val undo: SharedFlow<DashboardUndo>
    fun water(plantId: Long); fun tick(itemId: Long); fun undo(event: DashboardUndo); fun refresh()
}
```
Undo of `Ticked` = `toggle(itemId)` again; of `Watered` = `removeWatering`.

Layout per spec: `Hello, Zaky 👋` (headlineLarge) + date; Row of two equal cards (`height(180.dp)`): Daily habit `SoftCard(color = Fresh.Ink)` with white text, Plant schedule `SoftCard(color = Fresh.Card)` + soft shadow (`shadow(8.dp, RoundedCornerShape(28.dp), ambientColor/spotColor = Ink.copy(alpha = .08f))`), each status line + `RoundButton("→")`; `SectionTitle("Today")`; Water entries: `SoftCard(color = Fresh.Pastel, corner = 20.dp)`, name, pills `interval` + `whenLabel`, `RoundButton("💧", container = Fresh.Green, content = White)`; Tick entries: `SoftCard(corner = 20.dp)`, text, `RoundButton("✓"…)` outline-free on Page colour; card body → plant page / habit page; empty → `ALL_DONE` card. Whole column `verticalScroll`. Snackbar host with `Watered …`/`Ticked …` + `Undo`. `AppNav` passes `onOpenToday` and `onOpenPlants` for card bodies.

- [ ] **Step 1: Failing tests** — `Today lists due plants then unticked items`; `ticking from Today then Undo brings the item back unticked` (Focus 1: after undo, `state.today` contains `Tick(item)` and item `!isDone`); `watering from Today removes it and Undo restores it`; `after midnight refresh shows the new day` (Focus 4: items written on the 7th are gone from Today on the 8th, a plant due on the 8th appears).
- [ ] **Step 2:** Push tests (RED: unresolved `today`/`tick`/`undo`).
- [ ] **Step 3:** Implement VM, screen, nav.
- [ ] **Step 4:** Push with Task 2 → CI green, the four tests PASSED in the log.
- [ ] **Step 5:** Commit `feat: dashboard with greeting, two cards and Today`.

---

### Task 4: Plant schedule page

**Files:** Modify `ui/plants/PlantsScreen.kt`, `PlantsViewModel.kt` (`undo: SharedFlow<Pair<Long, String>>` = watering id + plant name; snackbar text `PlantCopy.watered(name)`).
Due plant: `SoftCard(color = Fresh.Pastel)`, bold name, pills, `RoundButton("💧", container = Fresh.Green, content = White, size = 56.dp)` with "I watered it" label beside it. Nothing due: `SoftCard` "Next watering" + date + pill, small `RoundButton("💧")`; `SectionTitle("Later")` rows as `SoftCard(corner = 20.dp)`. TopBar title `Plants`, actions `RoundButton("📅")`, `RoundButton("+")`. Titles `maxLines = 2, Ellipsis`.
- [ ] **Step 1:** Update `PlantViewModelsTest` with `watering emits the plant's name for the snackbar` (RED first in the same push as Task 3's tests).
- [ ] **Step 2:** Implement; push → CI green.
- [ ] **Step 3:** Commit `feat: plant page in the fresh look`.

---

### Task 5: Calendar and add/edit

**Files:** Modify `ui/plants/CalendarScreen.kt`, `EditPlantScreen.kt`.
Calendar: today = filled `Fresh.Pastel` circle with GreenDeep number; selected = 1.5dp Ink ring (the one outline allowed — it marks selection); weekday row in `Small`/InkSoft; day detail in `SoftCard`. Edit: `TextField` with `Fresh.Card` container, no indicator line, 20dp corners; colour swatches 40dp circles, selected with a Green ring; Repeats / One time as `Pill`s (selected `Fresh.Pastel` + GreenDeep text); Save = full-width 56dp Ink pill with white text; Delete = plain error-red text.
- [ ] **Step 1:** Implement (styling only; behaviour tests from the plant-schedule plan still cover it).
- [ ] **Step 2:** Push → CI green. Commit `feat: calendar and plant form in the fresh look`.

---

### Task 6: Daily habit page

**Files:** Modify `ui/today/TodayScreen.kt`, `TodayViewModel.kt` (remove `sendTestReminder`/`testReminder`), `ui/AppNav.kt` + `MainActivity.kt` (drop the `sendTestReminder` plumbing; receivers keep `EXTRA_FORCE`).
Big bold `Today`/`Record` + date; ‹ › as `RoundButton`s; note in `SoftCard(corner = 20.dp)`; rows as `SoftCard(corner = 20.dp)` with a 28dp round tick (done = Green fill + white ✓, text InkSoft + strikethrough), `×` at the right; add field rounded `Fresh.Card`; record footer in `Small`. Remove the masthead ("ONE DAY AT A TIME · OFFLINE") — the dashboard greeting replaces it.
- [ ] **Step 1:** Implement; existing `TimeboxRepositoryTest` stays green.
- [ ] **Step 2:** Push → CI green. Commit `feat: daily list in the fresh look`.

---

### Task 7: On the phone

- [ ] **Step 1:** Download the CI APK; `adb install -r` (same key — upgrades in place, data kept).
- [ ] **Step 2:** Screenshots: dashboard (launch), daily list (`am start … --es destination today`), plant page (`--es destination plants`). Check against spec: white page, pastel green only on due plants, Inter everywhere, dark status-bar icons, no outlines.
- [ ] **Step 3:** No crash in `adb logcat -b crash`; data intact (dashboard count matches the backup's items).
- [ ] **Step 4:** Calendar, edit, Today tap + Undo, and a 60-character item (Focus 5) need taps: owner does them, or turns on "USB debugging (Security settings)" and I drive them.
