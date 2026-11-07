# Plant encyclopedia and daily fun fact — design

Date: 2026-11-08 · Status: owner asked for it; product calls are Claude's (owner delegated plant decisions)

## Ask (owner's words)
"An encyclopedia about this plant… Greenovia, Astrophytum (and variegata type), myrios (also
its var type), how to water, sun time and a short description. Share it on the dashboard with a
little fun fact, but also have the encyclopedia."

*My reading:* "myrios" = **Astrophytum myriostigma** (bishop's cap); "Astrophytum" =
**Astrophytum asterias** (sea urchin / Kabuto), the other Astrophytum commonly kept with it.
Each gets its variegated (*f. variegata*) and named forms inside its entry.

## Decisions
- **Offline, bundled content.** The app has no internet permission; entries are written into
  the app (in `:core`, as data), with the sources named at the bottom of each entry.
- **Entries:** Greenovia (Mountain Rose), Astrophytum asterias, Astrophytum myriostigma, plus a
  short **Watering basics** entry (soak and dry) because "how to water" applies to all of them.
- **Each entry:** name + other names, origin, short description, **Sun**, **Water**, care tips,
  forms/varieties (incl. variegata notes), 4–5 fun facts, sources.
- **Honest about disagreement:** where sources conflict (mainly sun), give the cautious middle
  ("bright light, shade from harsh midday sun") and say it depends; watering is always "when the
  soil is completely dry", with rough intervals as a guide, plus a note for Jakarta's year-round heat.
- **Dashboard:** a **Did you know?** card under the two big cards: one fact per day, the same
  all day, rotating daily. Facts about plants the owner actually has come first (matched by name,
  e.g. a plant called "Greenovia"). Tapping it opens that entry; a small "Encyclopedia" link opens
  the list.
- **Encyclopedia screens:** a list (cards: name, other name, one-line description) and an entry
  page. Reachable from the dashboard card and a 📖 button on the Plants page.
- Look: the existing fresh style (white, pastel green for the fact card, soft cards).

## Non-goals
Search, images, user-editable entries, more species (easy to add later as data), linking
an entry to a plant's watering interval automatically.
