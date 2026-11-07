# Fresh look: white and pastel green — design

Date: 2026-11-08 · Status: written, awaiting owner approval

## Why

The owner wants the app **minimal, but it currently looks boring** (dark, grey, mono
labels, flat outlined boxes). They shared a reference screenshot of a meditation app
and asked for that feel with **white and pastel green as the core colours**.

What makes the reference minimal *and* lively — and what we copy:
- a big friendly greeting at the top;
- big soft rounded cards with lots of space;
- **one dark card beside one light card** for contrast;
- **one pastel colour filling whole cards** for "this needs you", not tinting text;
- small pill tags (`5 min`, `Morning`);
- round action buttons (the reference's ▶).

## Owner's decisions

- Colours: **white + pastel green** at the core.
- **Always light** — white and pastel green even when the phone is in dark mode.
- Dashboard gets a **Today** list under the two big cards.

## Look *(values are my calls unless marked)*

| Token | Value | Use |
|---|---|---|
| Page | `#FFFFFF` | background everywhere |
| Card | `#F5F7F3` | light cards, fields, rows |
| Ink | `#1B1D1A` | text, the dark card, primary buttons |
| Ink soft | `#6B7068` | secondary text |
| Pastel green | `#CDEBD5` | fills "needs you now" cards, selected chips, today in the calendar |
| Green | `#5DB37E` | round action buttons, ticks, small accents |
| Green deep | `#1F4D33` | text on pastel green |

- **Font:** Inter only, bold for headings (greeting ~30sp). JetBrains Mono and the
  uppercase spaced-out labels are dropped from the UI — they read as "technical",
  which is part of what feels boring. *(my call)*
- **Shapes:** cards 28dp corners, rows 20dp, pills fully round, round buttons 48dp
  (56dp for the main watering button). No outlines; light cards sit on white by colour
  alone, plus a very soft shadow on the two big dashboard cards.
- **Plant colours** (the six dots) stay as they are — they identify plants. *(my call)*
- Dark mode: none. The app is light regardless of the phone setting (owner).

## Screens

### Dashboard

```
┌──────────────────────────────┐
│ Hello, Zaky 👋               │
│ Saturday 7 November          │
│                              │
│ ┌────────────┐ ┌────────────┐│
│ │█ Daily    █│ │ Plant      ││
│ │█ habit    █│ │ schedule   ││
│ │█ 1/4 done █│ │ 1 due      ││
│ │█       (→)█│ │       (→)  ││
│ └────────────┘ └────────────┘│
│ Today                        │
│ ╭──────────────────────────╮ │ pastel green
│ │ Water Basil         (💧) │ │
│ │ (every 3 days) (today)   │ │
│ ╰──────────────────────────╯ │
│ ╭──────────────────────────╮ │ card grey
│ │ cuci piring          (○) │ │
│ ╰──────────────────────────╯ │
└──────────────────────────────┘
```

- Greeting **"Hello, Zaky 👋"** — name taken from the owner's git name *(my call;
  hard-coded, no settings screen)*. Date underneath.
- Two cards side by side: **Daily habit** = dark (ink, white text), **Plant schedule**
  = light. Each shows its status line (existing copy) and a round → button. Tapping
  anywhere on a card opens its page, as today.
- **Today** (owner): everything that still needs the owner today, in this order:
  1. plants due or overdue — pastel green cards, pills for interval and when
     (`every 3 days`, `today` / `2 days late`), round 💧 button. Tapping 💧 waters it,
     with the existing Undo snackbar ("Watered Basil · Undo").
  2. today's unticked items — light cards, round tick button on the right. Tapping it
     ticks the item; it leaves the Today list.
  Ticked items and not-yet-due plants are not shown here. *(my call)*
- Today empty: one light card, "All done for today 🌿" *(my call)*. If nothing is
  written yet today, the habit card still says "Nothing written yet" and Today shows
  only plants.

**Settled after review** *(my calls)*:
- Ticking an item from Today shows an Undo snackbar too ("Ticked cuci piring · Undo"),
  same as watering — a mis-tap must be cheap to fix.
- Tapping the body of a Today card (not its round button) opens that thing's page:
  a plant → Plant schedule, an item → Daily habit.
- The whole dashboard scrolls; the Today list is not capped.
- The watering snackbar says "Watered Basil · Undo" everywhere (dashboard and Plant
  schedule page); the copy lives in `:core` with the other plant copy, tested.

### Daily habit (today's list)

Same features as now, restyled: big bold "Today" title with the date under it,
the "What is today for?" note in a rounded card, items as rounded light rows with a
round tick on the left (ticked = green filled with ✓, text struck through and soft),
`×` to remove, rounded add field. The ‹ › record stepping stays, as round buttons.
Remove the "Send the morning reminder now (test)" link — the morning notice has
been seen working. *(my call)*

### Plant schedule

- Due today: each due plant is a **pastel green card**; plant name bold, pills for
  interval and when, and a large round green **💧** button with "I watered it" under
  or beside it.
- Nothing due: a light **Next watering** card (plant, date, "tomorrow"), small 💧 for
  watering early; **Later** list as light rows.
- Top: round ‹ back, title "Plants" in bold, round 📅 and **+** buttons.

### Calendar

Rounded month grid on white; today = filled pastel green circle; selected day =
ink outline; dots unchanged (solid watered, hollow planned). Day detail as a light card.

### Add / edit plant

Rounded light text fields (no outlines), round colour swatches, **Repeats / One time**
as pills (selected = pastel green), Save as a full-width ink pill button, Delete as
plain red text.

## Goals

- Feels calm, friendly and alive while staying minimal.
- Everything that needs doing today is reachable from the first screen in one tap.

## Non-goals

- Search bar and notification bell from the reference.
- Home-screen widget restyle (stays as is for now).
- Settings (name, theme). Dark mode.
- Any change to scheduling, storage or reminders.

## Anti-goals

- Clutter: no more than the greeting, two cards and the Today list on the dashboard.
- Green everywhere: pastel green only means "needs you now" (and selection); the rest
  is white, light grey and ink.

## Constraints

- Existing stack and pinned toolchain; Compose + Material 3; no new dependencies.
- Behaviour already tested in `:core` and the repositories must not change.
- Dashboard Today list needs data from both repositories; its ordering and copy belong
  in `:core` with tests, like the rest.

## Left to the builder

- Exact sizes and spacing within the values above; shadow strength.
- How the Material 3 colour scheme maps onto the tokens.
- Snackbar styling.
