# Light check: save the current reading — design

## Why

On the Light check page the lux reading is live and gone once you leave. The owner wants one tap to keep the current light status with its date and time.

## What gets built

- A floating save button (💾) at the bottom corner of `LightScreen`.
- Tap: saves the current (smoothed) lux, its `LightBand`, and the moment it was taken. Nothing else happens on screen except the icon turning into ✓ for about a second, then back to 💾.
- Hidden when the phone has no light sensor. Does nothing while the reading is still `null` (waiting for the sensor).
- Storage: a new Room table `light_readings` (`id`, `lux: Float`, `band: String`, `takenAt: Long` epoch millis) in `TimeboxDatabase`, version 16 → 17, with a migration that only creates the table.
- The screen's KDoc no longer says "Nothing is saved".

## Non-goals

- No list, history, delete or export of saved readings (can come later).
- No plant or spot attached to a reading.
- No toast, snackbar or "Saved" text.

## Approaches

- **Room table in the existing database (picked):** same place as every other piece of app data, survives with it, ready for a history screen later.
- Preferences / a file: kept apart from the rest of the data; rejected.

## Left to the builder

- How the screen reaches the DAO (follow the existing screens' pattern).
- Exact FAB styling, using the app's `Fresh` theme colours.
