> **PARKED — 2026-10-26.** This spec describes the app-blocking gate. It is deferred
> to v2 at the user's request: the timeboxing half comes first. The code it produced is
> committed and tested, not deleted — `GateDecider`, `ForegroundFilter`, `PassExpiryGuard`
> and `WidgetStateBuilder` in `:core` (38 passing tests), and the `AppWatcherService` /
> `GateOverlay` / `GateActivity` spike in `:app`. The current spec is
> `2026-10-26-timeboxing-design.md`.

# Timebox — Design Spec

**Date:** 2026-10-26
**Status:** agreed, ready to plan

## The problem

Every morning I know what the day is for. By 10am my thumb has already
opened Instagram three times without my deciding to. The gap is not
knowledge, it is the half-second between the urge and the app opening —
nothing stands in it.

Timebox stands in it. It asks, in that half-second, what I actually
wanted to do, and holds me to what I wrote this morning.

## What it does

1. **Morning note.** A notification each morning. I write one short note:
   what today is for. That note is the day's vision.
2. **Off-plan apps.** I pick which apps are off-plan (Instagram, X,
   TikTok, YouTube, whatever).
3. **The gate.** The instant an off-plan app comes to the foreground,
   Timebox covers it with a full-screen wall that shows what I wrote this
   morning and asks why I need this app right now. I either go back to
   work, or I type a real reason, wait out a countdown, and get a short
   timed pass.
4. **The pass expires.** When it does, the wall comes back — even if I
   never left the app.
5. **A home-screen widget.** Today's note, readable without unlocking
   anything, and the fastest way to set it: one tap on a recent intention
   writes the day's note with no typing at all.

## What it is not (v1)

- No time-blocked schedule. One intention per day, not named blocks.
  (Deliberate: the schedule is the next version, and the gate has to earn
  its keep first.)
- No stats, streaks, or evening review. (The widget's "4 stops today" is
  a counter, not a stats feature.)
- No AI. The gate is a fixed local script: no network, no API key, no
  cost, works on a plane.
- No device-owner provisioning. See *Enforcement honesty* below.

## Enforcement honesty

Android gives a normal app **friction, not a lock.** Timebox detects the
app in ~100ms and throws a wall over it, which kills the reflex. It
cannot stop someone who walks into Settings → Accessibility and switches
Timebox off. That is the accepted trade: the app installs normally, needs
no factory reset, and breaks nothing.

The real failure mode is not bypass, it is **silence** — the service gets
switched off or killed and the app sits there looking installed while
doing nothing. So Timebox must check its own permissions on every launch
and say so loudly when it is not actually watching.

A later version may add optional Device Owner mode
(`setPackagesSuspended`, uninstall protection) for when friction stops
working. Out of scope here.

## How it works

**Detection.** An `AccessibilityService` subscribed only to
`typeWindowStateChanged`, with `canRetrieveWindowContent="false"` — it
reads the foreground package name and nothing else. It never reads screen
content. This is the only API that gives real-time foreground detection;
`UsageStatsManager` lags and is unusable for blocking.

**The wall is an Activity, not an overlay window.** The gate needs text
input, so it needs the soft keyboard and focus, which a
`TYPE_APPLICATION_OVERLAY` window fights. `GateActivity` is launched from
the service with `FLAG_ACTIVITY_NEW_TASK`, in its own task
(`taskAffinity=""`, `excludeFromRecents`, `singleInstance`).

**Why the app still needs "Display over other apps."** Not to draw.
Android 10+ blocks background activity starts, and holding
`SYSTEM_ALERT_WINDOW` is the documented exemption that lets the service
launch the wall. Both permissions are required; onboarding must explain
each in one line.

**Back press on the wall goes home**, it does not finish the activity —
finishing would reveal the app underneath.

## Rules the gate follows

Given a package that just came to the foreground:

| Condition | Result |
|---|---|
| Not in the off-plan set | allow, silently |
| Has an unexpired pass | allow |
| No note written for today yet | **block** — "you haven't said what today is for" |
| Otherwise | **block** — show today's note |

A pass is granted only when all three hold: the reason reads like a real
sentence, the countdown has finished, and I tap grant. Defaults: 15
characters minimum, 3 words of 2+ characters, 6 distinct characters,
10-second wait, 5-minute pass.

## The widget

Android widgets cannot accept typed text — there is no text field in the
`RemoteViews`/Glance surface, and there is no way around that. So the
widget earns "easy daily input" a different way: by removing the typing.

It has two states.

**No note for today yet:**

```
┌────────────────────────────────┐
│ Timebox · no note for today    │
│                                │
│ [ Ship the auth refactor    ]  │  ← one tap = today's note
│ [ Thesis chapter 3          ]  │
│ [ Rest day, no screens      ]  │
│                                │
│ [ Write something else…     ]  │  ← opens the note screen
└────────────────────────────────┘
```

The three buttons are my most-used past intentions, most recent first.
Tapping one writes it as today's note and the widget flips to the state
below. This is the whole point: on a normal day setting up my timebox is
a single tap from the home screen.

**Note written:**

```
┌────────────────────────────────┐
│ Today                          │
│ Ship the auth refactor.        │
│ No social until 17:00.         │
│                                │
│ 4 stops today · watching ✓     │  ← or "NOT WATCHING" in red
└────────────────────────────────┘
```

Tapping the note opens the note screen to edit it.

The `watching` line matters more than it looks: it is the fix for the
silence failure mode. If the accessibility service is off, I see it on my
home screen instead of discovering it a week later.

Built with Jetpack Glance. Resizable, with the intention buttons dropping
from three to one at the smallest width. The widget updates when the note
changes, when a stop is recorded, and on tap — not on a timer.

## Non-obvious requirements

- **The wall must not gate itself.** Timebox's own package is ignored,
  and consecutive events for the same package are collapsed — otherwise
  one app launch fires several events and the wall relaunches in a loop.
- **A pass expiring must re-gate the app I am still sitting in.** No new
  foreground event fires when a pass runs out, so expiry has to be
  scheduled and checked against the current foreground package.
- **Passes are wall-clock.** A pass granted at 09:00 for 5 minutes is
  over at 09:05 whether or not the phone rebooted in between.
- **Midnight and the empty state are the same case.** Fresh install, or
  2am, means no note exists. Block anyway, and offer to write one.
- **The widget must flip at midnight.** At 00:00 yesterday's note stops
  being today's note, and the widget has to go back to the empty state on
  its own — not the next time something happens to refresh it.
- **A one-tap intention must not need the app to be open.** The widget's
  tap writes to the database from a background worker; it must work from
  a cold-started process.

## Platform

Kotlin, Jetpack Compose, Room. minSdk 29 (Android 10), targetSdk 36
(Android 16). Phone only, portrait, single user, no accounts, no network
permission at all.
