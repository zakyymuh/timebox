# Plant care, done properly — evaluation and design

Date: 2026-11-08 · Status: decided by Claude on the owner's delegation
("suggest and add features without my permission… take evaluation first").

## What a plant professional would change

The single most important rule of watering houseplants: **water when the soil is
dry, not when the calendar says so.** More houseplants die from overwatering than
from underwatering. The owner already lives this ("every 7 and 8 days, it's
tentative"), and the app already counts from the real watering. What it lacks is a way
to say *"I checked — it's still moist"* without lying that it was watered, and a way
to learn the real rhythm instead of guessing it.

The owner's only plant today, *Greenovia* (a succulent rosette), is set to every 4
days. Succulents usually want far less. The features below let the soil and the
owner's own history correct that, instead of the app hard-coding advice.

## Evaluation

Scored on: does it help the owner remember and care better (value), does it fit a
calm, minimal, offline app (fit), and what it costs to build and maintain (cost).

| Idea | Value | Fit | Cost | Decision |
|---|---|---|---|---|
| **"Soil still moist" — check again tomorrow** | High: the core professional habit; prevents overwatering | High: one tap, no new screen | Small schema change | **Build** |
| **"Last watered N days ago" on every plant** | High: the fact you need when deciding | High: one line of text | Tiny | **Build** |
| **Learn the interval from history** ("You usually water every 8 days — use 8?") | High: turns "7 or 8, tentative" into a real number | High: a suggestion, never automatic | Small, pure logic | **Build** |
| **Act from the 6pm notification** ("Watered" / "Still moist" buttons) | High: done without opening the app | High | Medium: one receiver | **Build** |
| Fertilising / repotting schedules | Medium | Medium: doubles every plant screen | Medium–high | Later, once watering is proven |
| Species presets ("succulent: 14 days") | Medium | Low: offline advice can be wrong and the owner's history is better | Medium | Reject |
| Seasonal adjustment (water less in winter) | Low: Jakarta has no winter | — | — | Reject |
| Photos per plant | Low for remembering | Low: camera + storage | High | Reject |
| Rooms / locations | Low with 2–3 plants | — | Small | Reject |
| Plant widget | Medium | Dashboard Today already covers it | Medium | Reject for now |
| "Are you sure? watered 2 days ago" dialog | Low: nagging | Low | Small | Reject — the "last watered" line does this calmly |
| Streaks / badges | Low | Anti-goal: calm, not gamified | Small | Reject |

## What gets built

### 1. Soil still moist
- On a due plant (plant page card and the 6pm notification): **Still moist** next to
  **I watered it**. It means "I checked; not yet". The plant moves to **tomorrow**.
- It is not a watering: no solid dot, and the interval still counts from the last real
  watering. Pressing it again tomorrow moves it one more day.
- Watering clears it. Undo available like watering ("Checked Basil · Undo").
- Stored as `snoozedUntil` on the plant (schema 2 → 3, nullable column; existing data
  untouched). Rule: next due = the later of (last watering + interval) and
  `snoozedUntil`. Calendar projections follow automatically.

### 2. Last watered
- Every plant line and card shows `Watered 3 days ago` / `Watered today` /
  `Watered yesterday` / `Not watered yet`. Copy in `:core`, tested.

### 3. Learn the interval
- After at least **4 waterings** (3 gaps), take the **median** of the last 5 gaps.
  If it differs from the set interval by **1 day or more**, the plant page shows a
  pastel tip: `You usually water every 8 days` with **Use 8**. Tapping it saves the
  new interval; nothing changes on its own. Repeating plants only.
- Snoozes don't count as waterings, so a plant that is often "still moist" naturally
  produces longer gaps — which is the point.

### 4. Notification actions
- The 6pm notification for **one** due plant gets two buttons: **Watered** and
  **Still moist**. Each acts immediately in the background and clears the
  notification. With several plants due, the notification opens the app (no bulk
  actions: too easy to water the wrong thing).

## Non-goals
Everything rejected above. No change to the daily list, the dashboard layout, or the
look.

## Constraints
Same stack, pinned toolchain, no new dependencies, no new permissions. Room 2 → 3
migration must keep all data (tested like 1 → 2).
