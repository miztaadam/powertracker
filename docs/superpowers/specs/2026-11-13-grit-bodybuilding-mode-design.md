# Grit: bodybuilding mode

**Date:** 2026-11-13
**Status:** Agreed in brainstorming. Awaiting the owner's review of this spec.
**Applies to:** the shared Kotlin domain (`domain/`), Android (`app/`) and web (`web/`). Every stage ships to all three together.
**Delivery:** four stages, each planned, built, checked and merged on its own (§9). This document is the design for the whole mode; §4 to §7 are detailed enough to plan stage 1 from, and §8 fixes the scope of stages 2 to 4.

Lines marked **(owner)** are the owner's decisions. Lines marked **(call)** are the designer's, made where the owner left the how open; the owner can overrule any of them.

## 1. Purpose

The owner wants to train for a better physique as well as for strength, and the app is built only for strength: its coach, targets and Progress all hang off a heavier top set on four main lifts. Bodybuilding mode gives the app a second side for physique training, and lets the lifter move between the two without losing anything.

In the owner's words, a bodybuilding week is "3 or 4 days per week, 8-12 reps rpe 8, progressive overload but concentrating on the stretches and squeezes in the muscle as opposed to just trying to move the weight like a powerlifter".

## 2. Decisions made with the owner

- **One switch in Settings** turns bodybuilding mode on and off. Off is the app as it is today, called the powerlifting side. **(owner)**
- **Each side keeps its own plan.** Switching puts the current side away and brings the other back as it was left. **(owner)**
- **One training log.** Every logged set lives in one history whichever side it was logged on. **(owner)**
- **Welcome back.** When the lifter returns to a side after more than a month away, the coach says "Welcome back to the better side" and asks whether their goals have changed or they want to resume the same plan. **(owner)** Both sides say it. **(call)**
- **Training style on the bodybuilding side:** rep ranges, 8-12 at RPE 8 by default; progressive overload; attention on the stretch and the squeeze. **(owner)**
- **Progress is the lifter's own judgement:** the lifter tells the app which body parts are improving and which are progressing slower. **(owner)**
- **A lagging body part gets more sets and exercises the next week.** **(owner)**
- **The first bodybuilding plan is built from the lifter's answers:** how many days, which exercises they do now (or, if none, which they would like to do), and which body parts they most want to improve. It does not ask about equipment. **(owner)**
- **The look:** option C of the mockups. With the mode on, the accent turns blue, the background and cards take a blue tint, and a "Bodybuilding" label shows on every screen, in dark and light. **(owner)**
- **Staged delivery**, four stages (§9). **(owner)**

## 3. Goals, non-goals, anti-goals

**Goals**
- A lifter can train physique-style in Grit with the coach helping: rep ranges, reps-then-weight progression, body-part check-ins that steer the plan.
- Moving between the sides is one tap and loses nothing.
- At a glance it is obvious which side the app is on.

**Non-goals** (not in any stage)
- Nutrition, body measurements, progress photos. Bodyweight is already logged per session and stays.
- Supersets, drop sets, tempo timers, rest-pause.
- A third mode, or a different mode per day.
- A plan builder for the powerlifting side.
- An equipment question in the builder.

**Anti-goals** (these make the work a failure even if it runs)
- The powerlifting side changing in any way while the mode is off: its screens, wording, coach output and committed screenshots stay identical, and its existing tests pass unedited except where a signature gains a parameter. The one exception is Settings, which gains the new group and shows the new version number on both sides.
- Losing, splitting or duplicating the training log.
- A backup made by the newest app that the newest app on the other platform cannot restore.
- The coach changing a plan without the lifter tapping to apply it.
- A training number (sets per muscle, weekly ceiling, progression step) that is not traceable to a published source, recorded in the repo as the form guides' sources are. The owner does not judge training science; the sources do. Allowed sources are the same as for the guides: acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport. The owner's own numbers (8-12 reps, RPE 8, 3 or 4 days) need no source.

## 4. Approaches

1. **Staged releases (chosen by the owner).** Four stages, each usable on its own. The lifter trains in the mode after stage 1; each later stage is small enough to check properly.
2. **One release.** Everything together. Rejected: it is the largest change the app has had, the lifter waits for all of it, and a problem in one part holds up the rest.
3. **One plan, re-prescribed.** The switch would only change how the existing plan is prescribed. Rejected: the owner wants each side to keep its own plan.

Technical shape **(call)**: the mode's rules live in the shared domain so both apps behave identically, as a second coach beside the existing one rather than branches inside the existing rules. That keeps the powerlifting side's code paths untouched, which is the first anti-goal.

## 5. Model and rules (shared domain) — stage 1

### 5.1 Mode
- `enum class TrainingMode { POWERLIFTING, BODYBUILDING }` in `domain/.../model/Models.kt`.
- `PlanItem` gains `mode: TrainingMode` and `repsHigh: Int?`. `reps` stays and is the low end; `repsHigh == null` means fixed reps (every powerlifting item). A bodybuilding item has `repsHigh >= reps`.
- `LoggedSession` gains `mode`: the side that was active when the session was started. There is still one session per date. **(call)**
- `Advice` gains `mode` for targets, stalls and weak points: each side has its own. Deload and easing-back-in advice belong to the lifter, not to a side (§5.2), and carry no mode.
- **Switching on a day already trained.** Neither app records that a session is finished, so the switch is never locked. Today follows the mode of today's session when there is one, and the active side otherwise. After switching on a day that already has a session, the look, Plan, Coach and Progress change at once, while Today keeps showing that day's session with its own plan and targets and one line: "Today's session is on the powerlifting side. Bodybuilding starts with your next session." (or the reverse). **(call)**
- The JS facade (`domain/src/jsMain/.../GritDomain.kt`, `JsTypes.kt`) carries the new fields; `:domain:syncWebDomain` then commits the regenerated bundle.

### 5.2 What each coach reads
- **Powerlifting coach** (`Coach`, `ProgressionRule`, `StallRule`, `WeakPointRule`): stall, weak-point and progression read only powerlifting sessions, so weeks of 3 × 10 benching are neither a stall nor a new top set. **(call)**
- **Fatigue** (`FatigueRule`): its top-set, failed-set, e1RM-trend and RPE-at-the-same-load signals read only powerlifting sessions, because a 3 × 10 after weeks of fives would look like a falling max and a rising RPE. Its readiness signal reads every session. On the bodybuilding side in stage 1 a deload can therefore come only from readiness; a sourced bodybuilding fatigue signal arrives with stage 3's ceilings. **(call)**
- **A deload or an easing-back-in belongs to the lifter, not to a side.** While one is open it clamps targets on whichever side is active and blocks a new one on both. Training is training. On a range target a deload means the same range at the existing deload reduction in load and one set fewer, as the powerlifting deload does today. **(call)**
- **Easing back in after a break** (the existing re-entry rule, a 14-day gap in the whole log) works as today on either side.
- **Returning to a side after a long time on the other** (the safety rule). For each exercise on the side returned to, look at the last 28 days of sets on either side. Powerlifting counts a main lift's whole family (the lift and its variations, by `parentLift`); bodybuilding counts the exercise itself.
  - *Trained in those 28 days, but not on this side:* the first target back is the old target capped by what those recent sets support at the planned reps and RPE, using the domain's existing e1RM estimate, rounded down.
  - *Not trained at all in those 28 days:* the first week back gets the existing re-entry reduction for that exercise, as if returning from a break. It never starts from the old top set plus an increase.
  **(call)**

### 5.3 Bodybuilding targets: reps first, then weight **(call, from the owner's "progressive overload" and "8-12 reps rpe 8")**
A new `BodybuildingCoach` in `domain/.../coach/`, called for bodybuilding plan items where `Coach.targetsFor` is called today.
- **Looks at** the exercise's most recent bodybuilding session.
- **Every set reached `repsHigh` at or under the target RPE:** raise the weight and aim for the range again from the low end. The trigger is the owner's ("until every set reaches 12 reps at RPE 8"). The size of the increase comes from the allowed sources (a small percentage of the load is the usual published guidance), rounded to a weight that can be loaded: `PlateLoader.step` for barbell lifts, which respects the change-plates setting, and 0.5 kg otherwise; never less than one step. It is a suggestion: the lifter can type the nearest weight their gym has.
- **Otherwise:** same weight; the card shows last time's reps and says "Beat it."
- **No bodybuilding session yet for this exercise:** if it has powerlifting history, suggest a starting weight for the middle of the range at the target RPE from the e1RM estimate, rounded down; with no history at all there is no suggestion and the lifter enters a weight.
- **No lift is special** on this side: squat, bench, deadlift and strict press are ordinary exercises when planned here. Powerlifting records, baselines and the total are not touched by bodybuilding sessions. **(call)**
- The reason text must keep the Today card's existing "Coach suggests ... @ N." stripping working on both platforms (`SUGGESTION_SENTENCE`), or change both together.
- The size of the increase is cited to an allowed source in a code comment and in the sources file. If the sources also give a different trigger, the owner's trigger stays and the difference is noted there. If no allowed source supports a size, stop and ask.
- **Apply** puts the suggested weight in the weight field and the reps to beat in the reps field: last time's reps for that set plus one, capped at `repsHigh`; the low end of the range the first time and after an increase. **(call)**
- **Rest timer:** every exercise on this side uses the accessory rest. **(call)**

## 6. Storage, upgrade and backup — stage 1

- **Settings:** `trainingMode` (default POWERLIFTING) and, per side, the date it was last left (`powerliftingLeftOn`, `bodybuildingLeftOn`, absent until first left). Android: `SettingsRepository` (Preferences DataStore). Web: `web/src/db/settings.ts`.
- **Training days are per side**, because each side keeps its own set-up and the owner's bodybuilding week is 3 or 4 days. The existing `trainingDays` stays the powerlifting side's; a new `bodybuildingTrainingDays` starts as a copy of it the first time the mode is turned on. Settings shows and edits the active side's days. Every other setting is shared. **(call)**
- **Android database:** version 2 to 3, a real `Migration` (never a destructive fallback): `plan_item` gains `mode` (default `POWERLIFTING`) and nullable `repsHigh`; `session` and `advice` gain `mode` (default `POWERLIFTING`). Covered in `MigrationTest`.
- **Web database:** a version 3 entry in `SCHEMA_VERSIONS` whose upgrade stamps existing plan items, sessions and advice as powerlifting; `planItems` is indexed by mode. An upgrade test comes with it, as the file's rule requires.
- **Backup version 3** on both platforms (`app/.../data/Backup.kt`, `web/src/backup/format.ts`, `backup.ts`): the new settings, `mode` and `repsHigh` on plan items, `mode` on sessions and advice. Restoring a version 1 or 2 file treats everything as powerlifting. Parsing stays strict, so **an older app refuses a version 3 backup as too new, and both apps must be updated together**, as with strict press. A backup made on Android restores on web and the other way round, tested with a shared fixture.
- **App version:** 1.2 with stage 1, in both apps together (the versions-agree test enforces it); each later stage bumps it again.

## 7. User interface (both apps) — stage 1

Mockups: `.superpowers/brainstorm/bodybuilding/bodybuilding-look-{dark,light}.png` (working files, not committed; the builder commits real screenshots).

### 7.1 The switch
Settings gains a group **Training style**, first on the screen, with one switch **Bodybuilding mode**: "Rep ranges, a physique coach and a blue look. Your powerlifting plan is kept." Switching takes effect at once and is never locked (§5.1 says what Today shows on a day already trained).

### 7.2 The look (option C)
A second palette for each of dark and light, applied at the theme root when the mode is on. Android: `PowertrackerTheme` picks among four colour sets (`ui/theme/Color.kt`, `Theme.kt`). Web: `data-mode="bodybuilding"` on `<html>` with the variables overridden in `tokens.css`, and the `theme-color` meta follows (`ui/theme.ts`).

| Token | Dark | Light |
|---|---|---|
| background | `#070B14` | `#EDF2FA` |
| surface | `#0D1422` | `#FFFFFF` |
| container | `#131C2E` | `#E2EAF6` |
| container high | `#1A2640` | `#D6E1F1` |
| container highest | `#22314F` | `#C9D7EC` |
| hairline | `#1E2A44` | `#D0DCEE` |
| accent | `#8DB8FF` | `#1F4FA8` |
| on accent (ink) | `#07162E` | `#F5F8FF` |
| accent text | `#8DB8FF` | `#1F4FA8` |
| accent container | `#14274A` | `#DCE8FB` |
| on accent container | `#D6E6FF` | `#173B7D` |

Text, success, warning, danger, chart and plate colours are unchanged. The contrast tests (`TokenContrastTest`, `tokens.test.ts`) are extended to both new palettes; if a pair fails, adjust the failing colour by the least that passes and keep the look. The exercise pictures draw their bar and plates in the accent, so they turn blue on this side; that is intended.

**The label:** a small pill reading **BODYBUILDING** under the date or overline in the header of every top-level screen (Today, Coach, Progress, Exercises, Plan, Settings). The powerlifting side shows no label, so it is unchanged.

### 7.3 Plan
Shows the active side's plan only. On the bodybuilding side an entry's reps are a range with two steppers, new entries default to 3 sets of 8-12 at RPE 8, and a row reads "3 × 8-12 @ 8" (en dash, as `AdviceCard`'s `dose()` already prints ranges). Until stage 2, an empty bodybuilding plan says so and invites the lifter to add exercises day by day.

### 7.4 Today
As the mockup: the prescription with its range; the coach card "26 kg × 8-12 @ 8" with "Last time: 26 × 11, 10, 9. Beat it."; logged sets; weight, reps and RPE entry as today. On this side the "Top set" chip and the "Where did it stall?" question are not shown; a set can still be marked Missed. The stretch and squeeze cues in the mockup arrive in stage 4; stage 1 leaves no gap where they will go.

### 7.5 Coach
Shows the active side's advice only. On the bodybuilding side in stage 1: targets, deload, easing back in, and the welcome-back card.

### 7.6 Progress
The powerlifting charts, records and total belong to the powerlifting side. On the bodybuilding side in stage 1: the plan's exercises, each with its best set from each of its last four bodybuilding sessions and whether it is going up; a row opens the existing exercise history, which shows sets from both sides. A session's best set is the one with the most reps at that session's heaviest weight. "Going up" means a heavier weight than the bodybuilding session before, or the same weight with more reps in total across the sets. **(call)** Stage 3 replaces this with the body-part view.

### 7.7 Welcome back
On switching to a side that has a plan and was left more than 30 days ago, a card shows once, on Today and at the top of Coach: **"Welcome back to the better side."** with **Resume my plan** and **My goals have changed**. Resume dismisses it; the first week back is eased in by §5.2. Goals changed opens that side's Plan for editing (from stage 2, on the bodybuilding side, it opens the builder). A side entered for the first time shows no card.

## 8. Later stages: agreed scope

Each gets its own plan when its turn comes; these are the decisions that plan must keep.

### Stage 2: the plan builder **(owner: the questions; call: the rules)**
- Asks: how many days (3 or 4) and which weekdays; **which exercises do you do now** (pre-ticked from the current plans and the last month's log; untick or add from the library); if none, **which would you like to do** (same picker, empty; if still none, a standard set of common gym exercises); **which body parts you most want to improve**.
- Builds: 3 days as three full-body sessions, 4 days as upper/lower twice, every muscle trained twice a week. It keeps the lifter's picks, fills muscles they do not cover with catalogue exercises using equipment like the picks', puts compound movements first, gives priority body parts more sets and an earlier slot, and prescribes 8-12 at RPE 8. If picks overfill a session it says which it left out and why.
- The lifter sees the draft before it is saved, can swap any exercise for an alternative for the same muscle or ask for another draft, then accepts.
- Fixed rules in the shared domain: the same answers give the same plan, offline. Sets per muscle, frequency and ordering are sourced.
- "My goals have changed" on the bodybuilding side runs it again.

### Stage 3: check-ins, the lagging-muscle coach, Progress
- **Check-in** every four weeks, and any time from Progress: every muscle starts "on track"; the lifter marks the ones that stand out "growing well" or "lagging" on the muscle grid the Exercises tab already has (the 18 `MuscleGroup`s). **(owner: the lifter judges; call: the mechanics)**
- **Lagging:** the coach suggests more work for it next week: first a set added to exercises already planned for it, then a new exercise from the library that works it differently, on equipment already in the plan. One tap applies it to the plan; it can be dismissed. It stops at a sourced weekly ceiling per muscle and overall. **(owner: more sets and exercises; call: the order and the ceiling)**
- **Growing well:** the coach leaves that muscle's work alone. **(call)**
- **A stuck exercise** (no gain in reps or weight for three weeks): the coach suggests swapping it for another for the same muscle. **(call)**
- **Progress on the bodybuilding side:** per muscle, the rating at each check-in, the weekly sets actually done, and whether its exercises are moving up. **(call)**
- Check-ins are stored, and go in the backup (a further version).

### Stage 4: stretch and squeeze cues
- Two short lines for each of the 225 catalogue exercises: where the stretch is and where the squeeze is. Shown on the Today card on the bodybuilding side (as in the mockup) and in the form guide on both sides. **(owner: the focus; call: where they show)**
- Written from, and checked against, the allowed sources with an independent review pass, exactly as the form guides were; sources recorded per exercise. An exercise with no meaningful stretch or squeeze (a carry, a hold) says so in one line rather than inventing one.
- Content only: it can be written while stages 2 and 3 are built.

## 9. Delivery

1. **Stage 1, the mode itself:** §5 to §7.
2. **Stage 2, the plan builder.**
3. **Stage 3, check-ins and the lagging-muscle coach.**
4. **Stage 4, stretch and squeeze cues** (alongside 2 and 3).

Each stage: its own plan in `docs/superpowers/plans/`, built on the working branch, the focused tests while building, the full Android, domain, web and end-to-end suites once before its commit and once before main, an independent check against this spec, and the owner's yes before it is merged. `docs/next-update.md` is updated as each lands.

## 10. Tests (stage 1)

- **Domain:** bodybuilding targets (every branch of §5.3, including the first session and the weight step); mode filtering of the powerlifting rules (a log with bodybuilding sessions gives the same powerlifting advice as the log without them, except the return rule); the return rule; the existing coach golden scenarios unchanged.
- **Storage:** Android migration 2 to 3 and web upgrade 2 to 3 on real old data; settings round trip.
- **Backup:** version 3 round trip on each platform; a version 2 file restores as all powerlifting; a shared fixture restores on both.
- **Look:** contrast tests cover the two new palettes; new committed screenshots of Today, Plan, Coach, Progress and Settings with the mode on, dark and light, on both apps; every existing screenshot unchanged.
- **Behaviour (Robolectric and Playwright):** switching swaps the plan, the training days and the look, and shows the label; a session logged on one side shows in the exercise history on the other; switching on a day already trained leaves Today on that day's session with the one-line note; the welcome-back card after 31 days away, once, with both buttons; no card on first entry.
- **Domain, added:** an open deload or easing-back-in clamps targets on both sides and blocks a second one; the fatigue rule's powerlifting signals ignore bodybuilding sessions; both branches of the return rule, including a lift untrained for six weeks never starting from its old top set.
- The picker and Exercises tab speed checks still pass with the mode on.

## 11. Left to the builder

- Names and placement of new files, and how the theme root receives the mode on each platform.
- The exact wording of supporting lines and empty states, in the app's existing voice.
- Layout details the mockup does not settle (the range steppers, the label's exact position per screen), matching the design system; the builder looks at its own screenshots in dark and light before reporting.
- Anything this spec does not cover that would change what the lifter sees or what the coach says is not the builder's to settle: stop and ask.
