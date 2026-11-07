# Dashboard and Plant Schedule — design

Date: 2026-11-07 · Status: reviewed, awaiting owner approval

## Why

The owner waters two or three plants roughly every 7–8 days, but the timing drifts,
and they lose track of when the next watering is. A fixed calendar reminder doesn't
fit: once you water two days late, a rigid schedule nags against a plan you've
already left. What they want is a schedule that **follows what they actually did**.

They also want the app to open on a welcoming dashboard, so the existing timebox
(the daily list) and the new plant schedule sit side by side as two doors.

## What gets built

### 1. Dashboard (new start screen)

The app opens here. Two cards, stacked:

| Card | Name (owner's words) | Tapping it opens |
|---|---|---|
| 1 | **Daily habit** | the existing timebox / today page, unchanged |
| 2 | **Plant schedule** | the new plant schedule page |

Each card shows one line of live status under its name *(my call)*:
- Daily habit: today's progress, e.g. `2 / 5 done`, or `Nothing written yet` when empty.
- Plant schedule: `1 due today`, `Next: Monstera tomorrow`, or `No plants yet`.

Back from either page returns to the dashboard. Same paper look as the existing app
(Paper/Stone palette, Inter + JetBrains Mono).

```
┌──────────────────────────────┐
│ DAILY                        │
│ SAT 7 NOV                    │
│ ┌──────────────────────────┐ │
│ │ DAILY HABIT              │ │
│ │ 2 / 5 done            →  │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ PLANT SCHEDULE           │ │
│ │ 1 due today           →  │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

### 2. Plant schedule page

```
┌──────────────────────────────┐
│ ‹  PLANT SCHEDULE         +  │
│  ‹  NOVEMBER 2026  ›         │
│  Mo Tu We Th Fr Sa Su        │
│                       1      │
│   2  3  4  5  6 [7]  8       │
│                 🔵  🟢       │
│   9 10 11 12 13 14 15        │
│         🟠     🔵            │
│ ──────────────────────────── │
│  DUE TODAY                   │
│  🔵 Basil     every 3 days   │
│     [  I watered it  ]       │
│  UPCOMING                    │
│  🟢 Monstera     tomorrow    │
│  🟠 Snake plant  in 4 days   │
└──────────────────────────────┘
```

- **Top bar:** back `‹`, title, **`+`** at top right to add a plant (owner's ask).
- **Calendar:** a month grid like Google Calendar, one coloured dot per plant per
  date, today boxed. `‹ ›` steps months *(my call)*.
- **List below the calendar** *(my call, owner agreed):* `DUE TODAY` (includes
  overdue, shown as e.g. `2 days late`) then `UPCOMING`, soonest first. Each due
  plant has a big **I watered it** button.

### 3. A plant (an "agenda")

Fields: name, colour, and either
- **repeating every X days** (X from 1 to 90), or
- **one time** on a chosen date.

New repeating plant: asks "last watered" (default today) so the first due date is
right immediately *(my call)*. Colour picked from a fixed set of 6 that read on both
light and dark paper *(my call; owner said "colour like Google Calendar")*.

Tapping a plant row opens edit (name, colour, interval, delete) *(my call)*.

### 4. The schedule rule (the heart of it — owner's rule)

> Next due = the date you **actually tapped "I watered it"** + X days.

Water on Thursday when it was due Tuesday → next is Thursday + 7, not Tuesday + 7.
Watering early works the same way. Every tap moves all future dots for that plant.

- Due dates are **dates, not times**. "Due" means due date ≤ today.
- A one-time plant, once watered, is done: it leaves the lists and future calendar,
  but its watering stays in history.
- **Undo:** after tapping, a short snackbar "Undo" restores the previous state
  *(my call — big buttons get mis-tapped)*.

### 5. Calendar: past is fact, future is projection *(my call)*

- **Past days** show **solid** dots where watering actually happened (from the log).
- **Today and future** show **hollow** dots: projected due dates from the rule above,
  repeated every X days to fill whatever month is shown.
- An overdue plant shows its hollow dot on **today**, not back on the missed date.
- This is what "changes with my schedule" means: projections move on every tap;
  history never does.

### 6. 6pm reminder (owner's time)

- Every day at **18:00** local, if any plant is due or overdue, one notification:
  `Basil needs water` or `2 plants need water`. Tapping opens the plant schedule.
- If nothing is due, no notification. Overdue plants keep being included each
  evening until watered *(my call)*.
- Separate from the existing 06:30 timebox notice; its own channel ("Plants") so
  either can be muted in Android settings.
- Uses the same approach as the existing `MorningReminder` (AlarmManager, inexact
  fallback, re-armed on boot and after each fire).

### 7. Settled after review *(all my calls)*

- **Watering early:** every row (due or upcoming) has a watering-can icon on the
  right; due rows additionally show the big **I watered it** button. Tapping the
  plant's *name* opens edit.
- **Existing entry points keep going to Today:** the 06:30 notification and the
  home-screen widget open the Daily habit page directly; Back from there goes to
  the dashboard. The 6pm notice opens the plant schedule; Back goes to the dashboard.
- **"Last watered" on a new plant is an anchor, not history.** It's a field on the
  plant, never a log row, so it never draws a solid dot. Next due = (latest log
  entry, or the anchor if there is none, whichever is later) + X days.
- **Deleting a plant deletes its history too** (a plant that's gone shouldn't keep
  dotting the calendar). Edit asks for confirmation. "History never moves" is about
  re-planning, not deletion.
- **Edit can change everything:** name, colour, interval, switch repeating ↔ one
  time, and the one-time date.
- **One-time plant past its date and unwatered = overdue**, listed under DUE TODAY
  and included in the 6pm notice until watered.
- **Fixing a mistaken watering later:** tapping a past calendar day lists that
  day's waterings, each with a remove option. Undo snackbar covers the common case.

## Goals

- Never lose track of when each plant is next due.
- One tap to record a watering, and everything re-plans from it.
- Dashboard as the app's front door; existing timebox keeps working exactly as is.

## Non-goals

- General reminders (bills, filters). The data is named generically ("agenda") so
  this could grow later, but the UI says plants only.
- Plant widget — the existing home-screen widget stays timebox-only.
- Photos, species database, weather or soil-based watering, multiple reminders per day.
- Accounts, sync, internet. The app stays offline with no `INTERNET` permission.

## Anti-goals (would be a failure even if it "works")

- A rigid calendar that keeps the original dates after you water late.
- Rewriting history: past watering dots must never move.
- Nagging at 6pm when nothing is due.

## Constraints (from the existing project)

- Android, Kotlin, Jetpack Compose, Material 3; minSdk 26, compileSdk 37.
  Test device: Redmi Note 14, Android 16, HyperOS.
- Two modules: `:core` is pure Kotlin with JUnit tests — the schedule rule,
  projection, due/overdue logic and dashboard status text belong there.
  `:app` holds Room, UI, alarms.
- Room database already exists (`TimeboxDatabase`): adding tables needs a schema
  version bump with a migration — **existing daily lists must survive the upgrade.**
- Hand-rolled `ServiceLocator`, no DI framework. Navigation Compose is already a
  dependency.
- Toolchain is pinned (Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21); don't bump it.

## Left to the builder

- Table layout (suggested: `agenda` + `watering_log`, next-due derived, not stored).
- Exact visuals within the paper style; month-grid component.
- How to fit 3+ dots in one day cell.
- Notification ids, channel ids, request codes.
