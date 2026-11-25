# Last time on the Today card: design

Owner request, 2026-11-25, with a screenshot of the bodybuilding Today card: where the Stretch and Squeeze lines
are, show what was achieved on the exercise the previous week instead. Same on the powerlifting side. Approved in
the conversation the same day, with one change from the owner: every set from that session is shown.

## Why

While logging, the lifter wants to see what they did last time, set by set, so they know what to beat. The
Stretch and Squeeze lines took that place on the card and are not what they look at between sets.

## What the lifter sees

Between the sets logged today and the weight entry, on both sides:

```
LAST TIME · WED 18 NOV
1   25 kg × 10  @ 8
2   25 kg × 9   @ 8
3   25 kg × 8   @ 9
```

- One row for each set of that session, in the order logged: set number, weight × reps, and the RPE when one was
  logged. A missed set is marked "missed". A plate-counted set reads as a plate number, as today's rows do.
- "Previous week" means the latest earlier session on this side that has the exercise, whatever its date. The date
  is in the heading (weekday, day, month; the year is added when it is not this year).
- Each side reads its own sessions, as its coach does.
- An exercise never done on this side shows nothing and takes no room.
- The rows are read-only: quieter than today's rows, no tick, not tappable.

## Goals, non-goals, anti-goals

- Goal: the same block, the same sets and the same wording in both apps, from one shared function.
- Goal: it fits a very narrow screen and a large text size without cutting anything.
- Non-goal: the coach's box. Its "Last time: …" sentence is stored and matched by pattern; it is left alone, so on
  the bodybuilding side the numbers show twice while the box is open.
- Non-goal: the Stretch and Squeeze lines in the ⓘ guide. They stay there in both apps.
- Anti-goal: the block and the coach's sentence disagreeing about which session was "last time". (As built: they
  agree, except just after an exercise's plate setting changed. The coaches then read only the sets in the new unit
  and can name an earlier day; the block shows what was really done last, in the unit it was logged in.)
- Anti-goal: a slower Today screen. The history already loaded is reused; nothing more is read.

## Approaches

1. **One shared domain function, used by both apps and by the bodybuilding coach** (picked). The coach's private
   "last sets" lookup becomes a shared one that takes the side; Today asks it for each card. One rule, so the block
   and the coach cannot disagree.
2. Each app works it out from its own loaded history. Less plumbing, but two copies of the rule, and a third in the
   coach.
3. Reuse the coach's stored sentence. No new code, but it exists only on the bodybuilding side, only while a
   suggestion is open, and has no date or RPE.

## How

- `domain`: a history function `lastTime(exerciseId, mode, before)` returning the session's date and that
  exercise's sets, each in the unit it was logged in, as the coach's sentence has them. `BodybuildingCoach` uses it.
  Exposed to the web through `GritDomain`; `syncWebDomain` regenerates the JavaScript.
- Web: `TodayItem` gains `lastTime` (date and sets), worked out with the history Today already holds and kept per
  history object like `lastTopSet`. `ExerciseCard` draws a `LastTime` block in place of `Cues` on both sides.
- Android: the same field on `TodayItem` and the same block in `ExerciseCard.kt` in place of `MuscleCues`.
- Tests that assert the cues on the Today card are rewritten for the new block, including the narrow-width checks.

## Order and release

Web first; Android once the owner has used it (owner's rule of 2026-11-21). The shared domain change lands with
the web build. A version bump, and nothing is published until the owner says "publish".

## Left to the builder

The exact type styles and spacing, within `docs/design/DESIGN.md`.
