# papacti — next up

Pick these up once the current task is done. Each one ships on its own.

## 1. Random event check (like Daily)

Daily shows a random line about something you logged, like "Your last 'laundry' was 7 days ago", and tapping it shows another. Papacti should do the same with plant events.

- Reuse the pattern in `habits/.../ui/TodayViewModel.kt` (`factSeed`, `LastDone.funFact`) and `habits/.../ui/LastTime.kt` (`FunFactCard`).
- Events to draw from: waterings (`WateringEntity`), plants added, photos added to the collection.
- Sample lines: "You last watered Basil 4 days ago." "Your oldest plant is Monstera, 3 months with you."
- Done when: the dashboard shows one random plant event line, a tap shuffles it, and nothing shows until there is at least one event.

## 2. Delete button + Add plant

Today, "Delete plant" only lives at the bottom of `EditPlantScreen`, and adding a plant is the floating button on the dashboard plus the `+` on `PlantsScreen`.

- Put a delete button right next to Add plant so a plant can be removed without opening its edit screen.
- Keep the confirm dialog ("Delete Basil?") from `EditPlantScreen`.
- **Open question:** where exactly does the delete button go: on each plant row in `PlantsScreen`, or next to the floating Add plant button on the dashboard? Decide before starting.

## 3. A notification for every schedule

Right now there is one check at 18:00 (`nudge/EveningReminder.kt` → `PlantReceiver`) that posts only when something is due or overdue.

- Notify for each scheduled plant on its due day, not just one combined evening summary.
- Cover both kinds of schedule: repeating (`everyDays`) and one-off (`onceDate`). Respect `snoozedUntil`.
- Keep the alarm re-arming first and still surviving a reboot (`BootReceiver`).
- To decide: one notification per plant or a grouped stack, and whether the time stays 18:00 or can be set per plant.

## 4. Plant collection card → small square icon

`CollectionCard` in `ui/dashboard/DashboardScreen.kt` is a full-width rectangle with photos, a title, a count, and an arrow.

- Replace it with a small square tile showing only an icon (a photo or 🪴). Tap it to open the collection.
- Drop the "My plant collection" text, the count line, and the → button.
- Keep a content description ("Open collection") for accessibility.

## 5. Fun fact: simple and light

`FactCard` in `ui/dashboard/DashboardScreen.kt` currently has a "🌵 Did you know?" header, an "Encyclopedia" link, and an × close button.

- Remove all three. Show only the fact text, small and quiet.
- Remove `closeFact` from `DashboardViewModel` and the `onOpenEncyclopedia` / `onOpenEntry` wiring if nothing else uses them.
- Tapping the fact could shuffle to another one, like Daily does (ties in with #1).

## 6. Week strip day pill: green border instead of dots, and a date circle that fits the pill

In `DayPill` (`ui/dashboard/WeekCard.kt`, on the `week-strip` branch), each day shows up to 3 plant dots under the date circle, and the white date circle (`width - 10.dp`) sits in the middle of the pill.

- **Green border instead of the dots.** Remove the dot row. Show watering through the pill's border instead: green (`Fresh.Green`) when that day has a watering, and the normal white/ink border otherwise.
  - To decide: does a logged watering get a different green (solid vs. lighter) from one still to do, and how does a selected day look when its border is already green?
- **The date circle should match the pill's corner perfectly.** Move the circle to the bottom of the pill, with the same gap at the bottom as at the sides (5dp, so `padding(bottom = 5.dp)` instead of 10dp). Then the circle and the pill's rounded end share a centre and the gap stays even all the way round.
- Done when: no dots, the border carries the watering status, and the white circle sits concentric in the pill's rounded bottom with an even ring around it.

## 7. Event history: a square card on the dashboard, a list grouped by date, and search

Today (`week-strip`, commit cd5c814) the random plant event is a line in `EventCard`, next to `CollectionTile`. A tap shuffles it, but there's no way to see past events.

- **A new square card on the dashboard for the random event check.** Same size and style as `CollectionTile`, sitting beside it: an icon (e.g. 🕘) plus the current random event line, kept short. Tap it to open the history. (Decide whether shuffling stays on the card, e.g. on a long-press or a small ↻.)
- **History screen:** every event (waterings, plants added, photos added, and anything logged like Daily's "Last time"), newest first, **grouped by date** under headers like "Today", "Yesterday", "Mon 2 Nov", then "October 2026" for older months.
- **Search at the top of the section:** a search field above the list that filters by plant name or event name as you type ("basil", "repot"), keeps the date grouping, and shows "Nothing matches 'x'" when empty.
- Source: the events in `core/.../plants/PlantEvents.kt`, plus `LastDone` marks if logging gets added. Keep the grouping and filtering in `core` so they get unit tests.
- Done when: the dashboard shows two square tiles (collection + events), the events tile opens a date-grouped history, and search narrows it live.
