# Light check: saved list, ☀️ button, ? card — design

Builds on `2026-11-22-light-save-design.md`. The owner couldn't see what 💾 saved, so:

- **☀️ floating button, centre bottom**, replaces 💾. A tap saves the current lux, band and time (as before). No ✓; the new line at the top of the list is the feedback.
- **The page shows only the saved list**, newest first. Each line: band swatch, lux, band title (e.g. "12,300 lux · Bright light"). Date/time are stored but not shown. Empty list: nothing below the title.
- **? round button, top right**, opens a card (AlertDialog) holding everything the page used to show: "Right now" reading, brightest since opening with ↺, the band scale, how to measure, or the "No light sensor" card.
- The sensor keeps listening while the card is closed, so ☀️ always saves the live value.

Non-goals: deleting readings, showing dates, attaching readings to plants.
