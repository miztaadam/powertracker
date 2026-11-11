# Powertracker — Phase 1 Design

**Date:** 2026-11-11
**Status:** Approved in brainstorming, awaiting spec review

## 1. Purpose

Powertracker is a private Android logbook for powerlifting. You log your own squat, bench, deadlift
and accessory sessions, and an offline coach reads your progression and advises you:

- what to lift next session,
- when a lift has stalled, and which accessories to add for the supporting muscles,
- when fatigue calls for a deload,
- which lift is lagging behind the others.

**Who it's for:** one lifter, on one phone. There is no sharing, no accounts and no server.

**Success:** the lifter opens it every gym session, logs their sets faster than on paper, and acts on
the coach's advice because it explains itself and is right often enough.

### Scope

| Phase 1 (this spec) | Phase 2 (separate spec, later) |
|---|---|
| Session logging, e1RM tracking, progress charts | Program builder: generate and adjust full training blocks from a goal and a schedule |
| Coach: next-session targets, stall detection with accessory prescriptions, deload calls, weak-point check | Optional "ask the coach" AI explanation on top of the rules |
| You write your own weekly plan | |

**Out of scope for phase 1:** pounds, meet-day tools, wearables, cloud sync, sharing.

## 2. Decisions made

- **Coach engine:** deterministic rules, fully offline. No AI in phase 1.
- **Units:** kg only. 20 kg bar, standard kg plates, 2.5 kg rounding by default, 1.25 kg if change
  plates are enabled.
- **Coach changes nothing without asking.** Targets start as the planned numbers, and every coach
  change is an Apply/Dismiss suggestion.
- **Defaults:** planned top-set RPE 8 for main lifts. Stall window 3 weeks (4 for deadlift).
- **Miss location** (Floor / Mid / Lockout) is an optional tap, offered only on failed reps or sets
  at RPE 9.5 or harder.

## 3. Architecture

A single-module Android app: Kotlin, Jetpack Compose, Material 3, Room and DataStore, following
GearCalc's conventions with nhlbetstracker-style layering.

```
app/src/main/java/.../powertracker/
  domain/     Pure Kotlin. No Android imports. Fully unit-tested.
    model/        Exercise, Session, SetEntry, Advice, Lift, MissPoint, ...
    e1rm/         E1rmCalculator (RPE table + Epley fallback)
    plates/       PlateLoader (kg plate breakdown, rounding)
    coach/
      ProgressionRule.kt     next-session targets
      StallRule.kt           stall detection
      AccessoryMap.kt        lift + miss point → muscles → accessories
      FatigueRule.kt         deload calls
      WeakPointRule.kt       lift ratios
      Coach.kt               runs the rules, resolves conflicts, applies the safety rails
  data/       Room database, DAOs, DataStore settings, repositories, backup
  ui/         Compose screens and ViewModels: today/, coach/, progress/, plan/, settings/
  platform/   Rest-timer foreground service, app lock (from GearCalc), file picker
```

**Dependency rule:** `ui → data → domain`. `domain` depends on nothing. The coach is a pure
function, `Coach.evaluate(history, plan, settings, today, priorAdvice): List<Advice>`, so every
rule can be tested on synthetic histories.

## 4. Data model (Room)

| Entity | Fields |
|---|---|
| `Exercise` | id, name, category (`SQUAT`, `BENCH`, `DEADLIFT`, `VARIATION`, `ACCESSORY`), parentLiftId (nullable, e.g. paused bench → bench), muscles (list), isBuiltIn |
| `Session` | id, date, bodyweightKg (nullable), readiness 1–5 (nullable), notes |
| `SetEntry` | id, sessionId, exerciseId, order, weightKg, reps, rpe (nullable, 6.0–10.0 in 0.5 steps), failed (bool), missPoint (`FLOOR`, `MID`, `LOCKOUT`, nullable), isTopSet |
| `PlanDay` / `PlanItem` | weekday; exerciseId, sets, reps, targetRpe, order |
| `Advice` | id, type (`TARGET`, `STALL`, `DELOAD`, `WEAK_POINT`), liftId (nullable), createdAt, payload (JSON: suggested load or accessories), reason text, status (`OPEN`, `APPLIED`, `DISMISSED`), severity |
| `Baseline` | exerciseId, known 1RM or recent best, date (to start a lift before it has history) |

Settings in DataStore: change plates on or off, available plates, training days, app lock, and the
default RPE, which can be overridden.

**e1RM is never stored.** It is calculated from the sets using an RPE-adjusted percentage table
(Tuchscherer), falling back to Epley when there is no RPE. Changing the formula therefore never
leaves stale data behind.

The exercise library is pre-loaded with the main lifts, common variations and every accessory
the `AccessoryMap` references.

## 5. Coach rules

Each rule takes the history and returns advice with a written reason.

### 5.1 Next-session targets (`ProgressionRule`)
- Input: last top set for the exercise, logged RPE compared with planned RPE (default 8).
- RPE ≥ 1 below plan → +5 kg (squat and deadlift) or +2.5 kg (bench and accessories).
- RPE within 0.5 of plan → +2.5 kg, or repeat if the last jump was within 2 weeks and RPE rose.
- RPE above plan, or a failed rep → repeat, or −2.5 to 5% if RPE ≥ plan + 1.5 or a rep failed.
- Rounded to plates that can be loaded. Shown as a suggestion chip, for example "Coach suggests
  152.5 × 5 @ 8, because last time you hit 150 × 5 @ 7".

### 5.2 Stall detection and accessories (`StallRule` + `AccessoryMap`)
- Stall = best weekly e1RM for a main lift is flat or down (within +1%) for 3 weeks, 4 for
  deadlift.
- The rule finds the dominant miss point over that window. If there's no miss data, it uses the
  lift's general fallback.
- `AccessoryMap` (static data):

| Lift | Miss point | Muscles | Accessories |
|---|---|---|---|
| Squat | Floor (out of the hole) | Quads, glutes | Pause squat, front squat, Bulgarian split squat |
| Squat | Mid | Upper back, trunk | Pin squat, safety-bar squat, front squat |
| Squat | Lockout | Glutes, hips | Hip thrust, box squat |
| Bench | Floor (off the chest) | Pecs, front delts | Spoto press, long-pause bench, DB press |
| Bench | Mid | Pecs, triceps | Pin press, larsen press |
| Bench | Lockout | Triceps | Close-grip bench, JM press, board press |
| Deadlift | Floor | Quads, lats, position | Deficit deadlift, paused deadlift |
| Deadlift | Mid (knee) | Hamstrings, lats | RDL, paused deadlift at the knee |
| Deadlift | Lockout | Glutes, upper back | Block pull, hip thrust, RDL |
| Any | None (fallback) | — | Add one set of volume, change rep range (e.g. 5s → 3s or 8s), or swap to a variation |

- The rule suggests 2–3 accessories the lifter isn't already doing, with starting sets and reps
  (3×8–10 for hypertrophy accessories, 3×3–5 for heavy variations). Apply adds them to the plan.

### 5.3 Fatigue and deload (`FatigueRule`)
- Signals, each checked over the last 2 weeks:
  1. RPE rising ≥ 1 at the same or lower load on 2 or more lifts,
  2. 2 or more failed reps across main lifts,
  3. readiness averaging ≤ 2 or falling by ≥ 1.5,
  4. e1RM down on 2 or more main lifts at once.
- Two or more signals → a deload suggestion: one week at about 60% of the volume and about 90% of
  the load, with the signals listed as the reason.
- A stall on a single lift is a weakness (5.2). A drop across several lifts is fatigue (5.3).

### 5.4 Weak-point check (`WeakPointRule`)
- Typical ratios: squat at 80–90% of deadlift, bench at 60–70% of squat.
- A ratio below its range for 4 weeks, or a consistent miss point, flags the lagging lift with a
  prioritisation suggestion, even before it stalls.

### 5.5 Conflict resolution and safety rails (`Coach`)
- An open deload suggestion takes priority: no progression targets above the current load while it
  is open.
- No suggested load more than 5% above the last completed top set.
- At most one new suggestion per lift per week, and at most one of each type at a time.
- Dismissed advice is remembered and not shown again unless its trigger gets worse (stall longer,
  more fatigue signals).
- No advice for a lift with fewer than 3 sessions of history unless a `Baseline` exists.
- After a gap of 14 days or more: suggest a lighter re-entry week and pause stall detection for 2
  weeks.
- Variations count towards the parent lift's trends with a 0.9 discount, and never set the main
  lift's e1RM.

## 6. Screens

**Today (opens here)**
- Today's plan items with planned targets and any coach suggestion chips (Apply/Dismiss).
- Tap a set to log it as prescribed, or use steppers to adjust the weight, reps or RPE.
- A miss-point row (Floor / Mid / Lockout) appears on failed reps or RPE ≥ 9.5.
- Plate loader strip showing each side, for example "20 + 15 + 2.5".
- Rest timer starts automatically after a set and runs as a foreground-service notification.
- Start a quick session with optional bodyweight and readiness, or add an exercise ad hoc.
- Finishing a session runs the coach.

**Coach:** a feed of open advice cards, each showing what to do, why, a mini chart, and Apply/Dismiss.

**Progress:** e1RM charts for squat, bench, deadlift and the total, plus bodyweight; PR history;
weekly volume per lift; lift ratios against their typical ranges; tap a lift for its full history.

**Plan:** weekly template (days, exercises, sets × reps @ RPE). Settings are reached from here.

**Input validation:** reps > 12 or a weight jump > 30% from the last set of that exercise prompts
"Did you mean…?" before saving.

## 7. Data safety

- Offline only. The app requests no internet permission.
- Optional app lock (biometric/PIN), carried over from GearCalc.
- Backup/restore: a single JSON file with a version number, through the share sheet or a file
  picker. Restore validates the version and offers to replace or cancel.
- Room migrations are written and tested for every schema change, never destructive.

## 8. Testing

- **Domain unit tests** (JUnit, no Android): each rule on synthetic histories, for example:
  - flat bench for 4 weeks with lockout misses → triceps accessories,
  - rising RPE across all three lifts plus low readiness → deload, and no progression targets,
  - 3 sessions at RPE 7 → +2.5/5 kg targets, never above +5%,
  - a 20-day gap → re-entry week, no stall.
- E1rmCalculator and PlateLoader tests against known values.
- Room DAO and migration tests (instrumented).
- Compose UI test: log a set end to end, apply a coach suggestion.

## 9. Delivery

- GitHub repo `miztaadam/powertracker`.
- minSdk 26, targetSdk latest stable. Release builds use R8 for the whole app (learned from
  GearCalc's startup crash).
- Phase 2 (program builder) gets its own spec after phase 1 ships.
