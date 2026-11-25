# Workout history on Progress: design

Owner request, 2026-11-25: "lets also add a "History" button at the top right of the UI on the progress tab showing
all previous workouts, times, dates, sets, reps, weight etc". Design approved in the conversation the same day. On
times the owner chose **dates only** (option 1): Grit has never stored a workout's time of day or length, and
recording them would change the stored log and the backup file on both apps.

## Why

Progress answers "how is this lift going". The owner also wants to look back at a whole workout: what was done on a
given day, exercise by exercise, set by set. Today only one lift's or one exercise's history can be opened.

## What the lifter sees

A **History** button at the top right of Progress, on both sides, whether or not that side has anything logged. It
opens a screen:

```
←  History
   42 WORKOUTS

Wed 18 Nov 2026
BODYBUILDING · 2 EXERCISES · 5 SETS
DB press
  1   25 kg × 10  @ 8
  2   25 kg × 9   @ 8
Lat pulldown
  1   plate 7 × 12  @ 8
```

- Every workout with at least one set, newest first, from both sides in one list. Each names its side, how many
  exercises and sets it had, and the bodyweight when one was logged that day.
- Under each exercise, every set in the order logged: weight × reps, "@ RPE" when one was logged, "Missed" on a
  missed set, a plate number for a plate-counted set. The rows are the Today card's "Last time" rows.
- Read-only. Back returns to Progress.
- The first 20 workouts are drawn; **Show older workouts** draws 20 more each time.

## Goals, non-goals, anti-goals

- Goal: nothing new is stored and nothing stored changes: no database, Dexie or backup change.
- Goal: it fits a very narrow screen and a large text size, as the other history screens do.
- Non-goal: times of day and workout length (owner's choice). Editing or deleting from this screen. Search or
  filters. Marking records or top sets.
- Anti-goal: a slow or janky screen on a long log (hence the pages of 20).

## Approaches

1. **A new page beside the lift and exercise histories, built in the web app's own code** (picked): a `#/history`
   route, a builder beside `buildExerciseHistory`, and the "Last time" rows as one shared component. No shared-domain
   change, so the Android app is untouched until its port.
2. Put the grouping in the shared domain now. One rule for both apps, but the existing histories are each built in
   their app, and the grouping is a few lines; it would also move the generated web code for no gain yet.
3. A dialog or sheet over Progress. Less routing, but a long scrolling list in a sheet is awkward on a phone, and
   Back already works for pages.

## My calls

- Today's own session is in the list as soon as it has a set.
- A session with no sets is left out, and not counted.
- An archived exercise's sets still show, under its name.
- The date always carries the year, as the other history screens do.

## Order and release

Web first; the Android app once the owner has used it. Version 1.19, on top of 1.18 (complete at commit `929b437`).
