# Timebox — Timeboxing Design Spec

**Date:** 2026-10-26
**Status:** agreed, ready to plan
**Supersedes:** `2026-10-26-timebox-design.md` (app-blocking gate, parked to v2)

## The problem

Every morning I know roughly what the day is for, and by evening I have no idea
where it went. Not because I was stopped by Instagram — that is a separate
problem — but because the day never had a shape in the first place. Four hours
of "work on the thesis" becomes four hours of something.

Timebox gives the day a shape before it starts, and then shows me where I am
in it.

## What it does

1. **Each morning I write what today is for** — one short free-text note, the
   day's vision. Then I break it into blocks: a title and a length each.
2. **I start the day when I actually start** — not at a time the app decided.
   The first block begins when I tap it.
3. **One block runs at a time.** It shows elapsed and remaining. When I finish
   it, the next one begins.
4. **When a block uses up its planned time**, a notification tells me, and says
   what is next. I can start the next block from the notification.
5. **A home-screen widget** shows what I am supposed to be doing right now and
   how long is left, and sets up the day in one tap on a morning when my plan
   is the same as a recent one.

## Durations, not clock times

A block has a **length**, not a start and end time. The day runs back-to-back
from whenever I begin.

This is a deliberate choice against classic timeboxing, and the reason is
honesty about how my days actually go. A 09:00–11:00 block is a lie by 09:20 on
a day that started late, and an app full of overdue blocks before breakfast is
an app I will stop opening. Durations survive a slipped morning: if I start at
10:30, the plan is still exactly as valid, just later.

What this costs: the app cannot tell me "you are behind schedule" against a
wall clock, and it cannot nudge me at a fixed time of day. Accepted.

## The states a block moves through

```
PENDING  ──start──▶  RUNNING  ──finish──▶  DONE
   │                    │
   └──────skip──────────┴──────skip──────▶ SKIPPED
```

- Exactly **zero or one** block is RUNNING on a given day.
- Starting a block while another runs finishes the running one first. That is
  the common case — "done, next" is one action, not two.
- A block records its **actual** elapsed time when it finishes, so planned
  against actual is visible. This is the thing that teaches me my estimates are
  wrong.
- **Overrun is allowed, not punished.** A running block past its planned length
  keeps running and shows "over by 12m". It does not auto-advance: deciding to
  carry on is a legitimate choice and the app should not overrule it.
- SKIPPED exists because a plan I abandon should be distinguishable from one I
  completed, and from one I never got to.

## What it is not (this version)

- **No app blocking.** Parked to v2, spec preserved. Nothing in this version
  needs an accessibility service, an overlay permission, or any of the MIUI
  battery and autostart handling that goes with them — which is most of the risk
  in this project, deliberately deferred.
- No calendar integration, no sync, no accounts, no network permission at all.
- No analytics beyond planned-against-actual on the day itself. The weekly view
  comes later, once there is data worth looking at.

## Non-obvious requirements

- **Starting a second block must close the first.** Two RUNNING blocks is a
  corrupt day, and the UI makes it one tap, so the invariant has to live in the
  domain rather than in the screen.
- **A running block must survive the app dying.** Elapsed time is computed from
  a stored start instant, never from a timer held in memory. Reopening the app
  an hour later shows an hour elapsed.
- **Midnight does not stop a running block.** If I start a block at 23:40 and
  finish it at 00:10, it belongs to the day it started on. Days are not clipped
  at midnight; the block keeps its own day.
- **The elapsed notification fires once per block**, not every minute after.
- **An empty plan is the normal state**, every single morning. The first screen
  I see on a fresh day must be about making a plan, not an empty list.
- **A plan copied from a recent day must reset its blocks**, not carry over
  yesterday's started/finished timestamps.

## Platform

Kotlin, Jetpack Compose, Room, Jetpack Glance. compileSdk 37, minSdk 24 (the
target phone is a Redmi Note 8, which may still be on Android 9). Phone only,
portrait. No `INTERNET` permission. The only runtime permission is
`POST_NOTIFICATIONS` on API 33+.
