# Counting plates on stack machines: design

**Owner's request (2026-11-20):** "some machines don't have kg or lbs on them they only have plates and when using the machine you just set the amount of plates".

**The owner's choices:**
- **This design is approved,** with **half steps allowed** (some stacks have a small add-on weight).
- **Release 1.17,** after 1.16 (the guide) ships. 1.16's guide is not changed.

## Why
- On many pin-stack machines the plates are numbered, not weighed. The lifter knows "plate 7", not a weight.
- Today the lifter has to type a kg number they don't know. A guess gives the coach a wrong number, and the records a wrong best.
- With plates as their own unit, the lifter logs what the machine says, and the coach can still move them on: same plate and more reps, then one plate more.

## Goals
1. **A per-exercise choice, "Count plates on this machine",** in the exercise's menu on Today, on both apps.
   - With it: **Whole plates** or **Half steps too**.
2. **Logging in plates:** the weight field reads PLATE; − and + move one step (1, or ½ with half steps); sets read "plate 7" and "plate 7½".
3. **The coach coaches plates,** on both sides, in plate steps, with the same rules it uses for kg.
4. **History stays true:** each set records its own unit, so changing the setting later never rewrites a set.
5. **Both apps behave the same,** from one set of rules in the shared domain.

## Non-goals
- **Converting plates to kg,** or asking what a plate weighs.
- **Per-gym profiles** for the same exercise (two gyms, two stacks).
- **Plate-loaded machines** where the lifter adds kg plates. Those stay in kg. The app can't tell them apart (see "Which exercises get it"), so this rests on the lifter's choice.
- **A kg/lb setting.** The app has none today (see "What the code does today"); this design adds none.
- **A new chart.** See open question 2.

## Anti-goals (a failure even if it works)
- **A plate number shown or summed as kg.** Never "7 kg" for plate 7. A plate set never adds to a kg total, a volume, an estimated one-rep max (e1RM) or a record.
- **A plate set read as 0 kg.** Zero kg already means bodyweight in three places: `BodybuildingCoach.earnedIncrease` (no increase at 0 kg), `BodybuildingCoach.firstTimeWeight` (0 kg sets skipped) and `InputValidator.check` (no jump check from 0 kg). A plate set must never reach them as 0 kg.
- **Plates compared with kg.** The coach never compares a plate set with a kg set, in either direction.
- **History rewritten.** Changing the setting never changes a stored set, a stored target or a backup.
- **Squat, bench or deadlift in plates.** Never offered for a main lift.

## Constraints
- **No new sourced number.** Every factor the plate rules use already exists in the code: 0.85 (`AdvicePayload.ReEntry.loadFactor`), 0.9 (`AdvicePayload.Deload.loadFactor`), 0.95 and 0.975 (`ProgressionRule.FAILED_SCALE`, `HARD_SCALE`), 12 reps (`ProgressionRule.MAX_REPS`) and 1.30 (`InputValidator.MAX_JUMP_FACTOR`). The new rules only apply them to a plate number.
  - **`domain/src/commonMain/kotlin/com/powertracker/domain/coach/CoachSources.kt` gets one sentence,** not a new source: `BODYBUILDING_INCREASE` cites a 2–10% load increase. One plate is often more than 10% (plate 3 to 4 is a third more), because one plate is the smallest change the machine allows. The owner chose one step. The note says so; no number is added.
- **Kg coaching is unchanged.** Every existing coach, progress and backup test passes unchanged. A lifter who never turns this on sees no difference. *(As built, 2026-11-21: every coach and progress test passes unchanged. The backup and database tests that pinned version 5 were edited to version 6, as the plan lists, and one fixture test passes the new plate option to the web's sample builder. Kg behaviour is unchanged.)*
- **The 1.16 guide is unchanged.** Its pictures draw the real Today and Progress screens and its sample lifter runs the real coach, all in kg. Its text, tests and screenshots stay as they are.
- **Both apps ship together.** As in every backup version so far, an app that reads only version 5 turns a version 6 file away.
- **Version:** 1.17 on both apps (versionCode 28; 1.16.1 took 27).
- **Fixed rules:** no model names in code or commits; new commits only; e2e with `--trace off`.

## Approaches
1. **Chosen: plates are their own unit.** A set is "plate 7 × 10", and the coach works in plate steps.
2. **Ask what a plate weighs, convert to kg.** Rejected: the lifter doesn't know, and a guess skews the records and every kg rule fed from them.
3. **Log kg and put the plate in a note.** Rejected: the coach can't read notes, so it stays blind on the exercise.

## What the code does today (read for this design)
- **A set's weight:**
  - **Domain:** `LoggedSet.weightKg: Double` in `domain/src/commonMain/kotlin/com/powertracker/domain/model/Models.kt`. There is no unit anywhere; every weight is kg.
  - **Android:** `SetEntity.weightKg` (table `logged_set`) in `app/src/main/kotlin/com/powertracker/data/db/Entities.kt`; Room version 5, migrations up to `MIGRATION_4_5` in `app/src/main/kotlin/com/powertracker/data/db/AppDatabase.kt`, exported schemas in `app/schemas/com.powertracker.data.db.AppDatabase/`. No query in `app/src/main/kotlin/com/powertracker/data/db/Daos.kt` reads the weight; the mapping to the domain is in `app/src/main/kotlin/com/powertracker/data/TrainingRepository.kt`.
  - **Web:** `SetRow.weightKg` in `web/src/db/schema.ts`; Dexie version 5 (`SCHEMA_VERSIONS`). Mapped to the domain in `web/src/db/mapping.ts`, through `JsSet` in `domain/src/jsMain/kotlin/com/powertracker/domain/web/JsTypes.kt`.
  - **Targets:** the coach's TARGET advice is stored, with `AdvicePayload.Target.weightKg` in its JSON payload (`domain/src/commonMain/kotlin/com/powertracker/domain/model/Advice.kt`). Today reads today's stored targets and fills the steppers from an applied one.
- **Backups:** `Backup.VERSION = 5` in `app/src/main/kotlin/com/powertracker/data/Backup.kt`; `BACKUP_VERSION = 5` in `web/src/backup/format.ts`, decoded in `web/src/backup/backup.ts`. Both decode strictly: an unknown key outside `settings` is a corrupt file, and a newer version is "too new". The web's safety copy (`web/src/db/safety.ts`) copies whole rows and needs no change. There is no other export (no CSV).
- **Kg or lb:** there is **no unit setting.** The app is kg-only (`AppSettings` in `app/src/main/kotlin/com/powertracker/data/SettingsRepository.kt`, `web/src/db/settings.ts`). The only weight setting is `changePlates` (0.5 and 0.25 kg plates), which sets the kg step. Plates ignore it.
- **Per-exercise settings:** **none exist.** An exercise row holds name, category, parent lift, muscles, built-in and archived. Rest times are per category (main lift or accessory), not per exercise.
- **Equipment** is only in the read-only catalogue: `LibraryExercise.equipment: List<Equipment>` ("the first is the primary") in `domain/src/commonMain/kotlin/com/powertracker/domain/library/LibraryModels.kt`. An exercise row is matched to it by name (`Library.find`). Custom exercises have no equipment; their dialog offers only Variation or Accessory.
- **Catalogue rows appear on first use.** Android's `app/src/main/kotlin/com/powertracker/data/CatalogueResolver.kt` and the web's `web/src/db/catalogue.ts` insert a catalogue exercise's row only with the plan item or set that needs it. On the web, a Today item can be a "stand-in" with no row yet (`standInEntry` in `web/src/state/useToday.ts`).
- **The exercise menu on Today (⋮):** it holds one item, **Skip today**, and shows only on a planned exercise with no sets yet (`canSkip`):
  - Android: `SkipMenuButton` in `app/src/main/kotlin/com/powertracker/ui/today/SkipToday.kt`, placed by `GuideAndMenu` in `app/src/main/kotlin/com/powertracker/ui/today/ExerciseCard.kt`, gated in `app/src/main/kotlin/com/powertracker/ui/today/TodayScreen.kt`.
  - Web: `SkipMenu` in `web/src/screens/today/SkipToday.tsx`, used by `web/src/screens/today/ExerciseCard.tsx` (open card and folded row).
- **The shared domain on the web** is the checked-in build in `web/src/domain/generated`, rebuilt with `./gradlew :domain:syncWebDomain`.

## Which exercises get it
- **Offered when either is true:**
  - a catalogue exercise (found by `Library.find` on its name) whose equipment list holds `MACHINE` or `CABLE` **anywhere in the list**, and whose category is `ACCESSORY`;
  - a custom exercise (`isBuiltIn` false) whose category is `ACCESSORY`.
- **Never for a main lift** (category SQUAT, BENCH, DEADLIFT or STRICT_PRESS). Every MACHINE or CABLE entry in the catalogue today is an accessory, so the category check only guards future entries.
- **Not for custom variations** (see open question 1), nor `SMITH` exercises (a bar loaded with plates in kg).
- **"Anywhere in the list"** *(my call)*: T-bar row is `MACHINE, BARBELL`, glute-ham raise and reverse hyper are `MACHINE, BODYWEIGHT`. Each is offered; the lifter decides.
- **The library can't tell a pin stack from a plate-loaded machine.** Leg press, single-leg leg press, hack squat, lever squat, T-bar rows, sled push and the calf machines are often plate-loaded. They are offered like the rest; non-goal 3 rests on the lifter not choosing plates there.
- **Assisted pull-up, assisted chin-up and assisted dip** (all `MACHINE`): left out unless the owner says otherwise; see open question 3.
- **One setting per exercise, on both sides.** Exercise rows are shared by the two sides, so the setting is too *(my call; per-gym and per-side profiles are non-goals)*.

## The design as decided

### The setting
- **Where:** the exercise's ⋮ menu on Today, on both apps.
- **When the menu shows:** whenever it has an item. Skip today keeps its own rule (a planned exercise with no sets). The plate item shows on every exercise it's offered for, planned or added, with or without sets. So an eligible exercise has a menu even after its first set.
- **What it opens:** a small sheet with three choices, the current one marked.

```
  ⋮  ─────────────────────────────┐
  │ ⏭  Skip today                 │
  │ ▦  Count plates on this machine│
  └────────────────────────────────┘

  Lat pulldown
  How does this machine show its weight?
  ( ) In kg
  (•) Whole plates          1, 2, 3 …
  ( ) Half steps too        1, 1½, 2 …
  The coach starts this exercise fresh in plates.
  Your kg sets stay as they are.               [Done]
```

- **The note line** shows only when the change starts a new unit and the exercise has sets in the old one. Moving between Whole plates and Half steps too is not a change of unit: no note, and the coach keeps its plate history.
- **On a web stand-in** (a catalogue exercise with no row yet), choosing plates creates its row, as logging its first set would, in the same write.

### Logging
- **The weight field reads PLATE** instead of kg; its accessible name is "Weight, plates".
- **− and +** move one step: 1 with Whole plates, ½ with Half steps too. **+ on an empty field gives plate 1** (kg gives the empty bar). **− never goes below plate 1.**
- **The lowest step is plate 1** *(my call)*. Plate ½ isn't offered: on a stack, the half step is an add-on on top of a plate.
- **Typing:** a whole number, or with half steps a whole number and a half ("7.5", "7,5"). Anything else is refused with a short message under the field, as kg is today: "Whole plates only" or "Whole or half plates". The upper bound is the builder's.
- **Shown as** "plate 7" and "plate 7½" everywhere: set rows, coach chip, reasons, history. Spoken as "plate 7 and a half".
- **The plate picture is hidden** for a plate set: no bar, no "No weight yet", no "Can't load …" note.
- **What fills the field** follows today's order, each **in the exercise's current unit only:** the last set logged today, then an applied target, then the last top set. With none, the field is empty.

```
 Lat pulldown                               ⓘ  ⋮
 3 × 8–12 @ 8
 ⚡ plate 7 × 8–12 @ 8
    Last time: plate 7 × 12, 12, 11. Beat it.
 ✓ 1   plate 7  × 12  @ 8
 ┌ PLATE ─────┐ ┌ REPS ──────┐ ┌ RPE ───────┐
 │ −   7   +  │ │ −  12   +  │ │ −   8   +  │
 └────────────┘ └────────────┘ └────────────┘
 [ Missed ]  [ ✓ Log set 2                   ]
```

### The coach on a plate-counted exercise
- **It reads only sets in the exercise's current unit.** After a change of unit the exercise is new to the coach: there are no sets to go on, so there is no target until the first set in the new unit is logged, as for any new exercise today.
- **Bodybuilding side (decided):** stay on the same plate and add reps; when every planned set reaches the top of the rep range at or under the target effort, go up **one step** and aim for the bottom of the range again. This is `BodybuildingCoach`'s double progression with one step in place of `INCREASE` (2%). Everything else in that rule is unchanged: missed sets, sets with no RPE, the pain hold.
- **Powerlifting side (my proposal, closest to today's kg rule):** machine accessories go through `ProgressionRule` today. There, a top set on target adds 2.5 kg (`STEP_KG`) capped at 5% of the last weight (`MAX_INCREASE`); when that cap stops the weight going up, it adds one rep instead, up to 12 (`MAX_REPS`). One plate is nearly always more than 5%, so in plates:
  - **On target or easier** (RPE no more than 0.5 over the target): the same plate and one more rep, up to 12. **At 12 reps** on target or easier: one step up, back to the planned reps. When the plan's reps are already 12 or more, each on-target top set earns the step.
  - **0.5 to 1.5 over the target,** or a recent step up with RPE rising (`recentJumpWithRisingRpe`): repeat.
  - **1.5 or more over the target** (`HARD_SCALE` 0.975) **or a missed top set** (`FAILED_SCALE` 0.95): that factor of the plate number, rounded down to the step, **at least one step down**, never below plate 1. Plate 7 missed: 7 × 0.95 = 6.65, so plate 6.
  - The squat and deadlift big step (`BIG_STEP_KG`) never applies: only accessories count plates.
- **Lighter weeks (decided):** after a 14-day break (re-entry, 0.85) and in a deload week (0.9), the factor applies to the plate number, rounds down to the step, goes **at least one step down**, and never below plate 1.
  - **The step down is measured from the same fixed top set** the kg clamp measures from (`capFor` in `Coach` and `BodybuildingCoach`: the top set from before the advice). So a lighter week of several sessions doesn't lose a plate each time.
  - **A deload and easing back in together** take the lower of the two caps, as kg does: not two steps.
  - **Examples:** plate 7 in a deload: 6.3, so plate 6. Plate 7 easing back in: 5.95, so plate 5 (5½ with half steps). Plate 2 in a deload: 1.8, so plate 1 (1½ with half steps). Plate 1: stays plate 1 (the deload still cuts the sets).
- **Pain skips, stalls, swaps, check-ins:** unchanged in logic. Any text that prints a weight prints plates.

## How each affected area behaves
Paths are relative to `domain/src/commonMain/kotlin/com/powertracker/domain/` unless they start with `app/` or `web/`.

- **`coach/ProgressionRule.kt`** (powerlifting targets):
  - `target`: in plates, the rule above. `lastTopSet` and `previousTopSet` are read in the current unit.
  - `held` (pain hold): compares plate with plate, as it compares kg with kg.
  - `isFreeLoad`, `round`, `roundDown`: not used for plates; a plate step function replaces them.
  - `describe`, `lastTime`, `fmt`: print "plate 7".
- **`coach/BodybuildingCoach.kt`:**
  - `earnedIncrease` and `heaviest`: over sets in the current unit. A plate set is never 0, so the bodyweight check never fires on it.
  - `increased`: one step for plates.
  - `firstTimeWeight` (first time on this side, from powerlifting sets through e1RM): for plates **(my proposal)** no e1RM and no rep conversion. Take the heaviest plate logged without a miss on the powerlifting side in the same 28-day window. When that is more than 28 days old, apply the re-entry step rule. With no plate sets, no target (baselines are main lifts only).
  - `clamp`, `roundDown`: the lighter-week step rule.
  - `lastTime`, `suggestion`: print plates. `withoutAddWeight` still drops "Time to add weight." when the final plate isn't above last time's.
  - `repsToBeat`: unchanged (reps only), reading sets in the current unit.
- **`coach/ReturnRule.kt`** (coming back from the other side):
  - "Trained on the other side": the kg rule caps the target by what those sets support at the target's reps, through e1RM. For plates **(my proposal)**: cap it at the heaviest plate logged without a miss on the other side in those 28 days. No rep conversion; the easing-back-in note shows when the cap lowers it.
  - "Not trained on either side": the re-entry step rule on the last plate top set.
  - `withSuggestion`: prints plates.
- **`coach/Coach.kt`:** `targetsFor` and `clamp` apply the step rule to plates. `evaluate` (stalls, weak points, fatigue, re-entry cards) reads main lifts and variations only, which never count plates.
- **`coach/FatigueRule.kt`:** reads main-lift sets only (`isMainLift`). Unchanged. Readiness is per session and unchanged.
- **`coach/StallRule.kt`, `coach/WeakPointRule.kt`, `coach/AccessoryMap.kt`:** read main-lift e1RM trends and miss points. Unchanged. Stall advice names accessories by name, never by weight.
- **`coach/PhysiqueCoach.kt`:**
  - `stuck` (swap advice) compares sessions with `beats` (heavier, or as heavy with more reps). It compares only sessions in the current unit, so a change of unit starts its 21 days afresh.
  - Weekly sets, more work and check-ins count sets, not weight: unchanged.
- **`coach/SkipCoach.kt`:** `held` looks for any set of the exercise, whatever its unit: unchanged. Its cards print no weight.
- **`model/Advice.kt`:** `AdvicePayload.Target` gains a unit, kg by default and left out of the JSON when kg (as `repsHigh` is), so every stored payload reads as before.
- **Stored targets on Today** (`app/src/main/kotlin/com/powertracker/ui/today/TodayViewModel.kt`, `web/src/state/useToday.ts`): today's targets are deduped by exercise. They're deduped by exercise and unit instead, and a stored target in the other unit is neither shown nor applied. Otherwise switching mid-day leaves a kg target standing for a plate exercise, and blocks the plate one.
- **`history/HistoryStats.kt`:**
  - `lastTopSet`, `previousTopSet`: in a given unit.
  - `weeklyTrend`, `currentE1rm`: e1RM of main lifts (and variations ×0.9). Plate sets are skipped there as well, as a guard (see the anti-goals).
- **`e1rm/E1rm.kt`:** takes kg only. No caller passes a plate set (test).
- **`progress/ProgressMath.kt`:** `prs`, `isNewPr`, `weeklyVolume`, `total` are main lifts only. Plate sets are skipped as a guard. No record celebration for a plate set.
- **`progress/BodybuildingProgress.kt`** (the bodybuilding Progress rows): the last four sessions **in the current unit**. Best set and "going up" compare plate with plate. Shown as "plate 9 × 10". `progress/MuscleProgress.kt` counts the going-up exercises from these rows and counts sets otherwise: unchanged.
- **`validation/InputValidator.kt`:** the big-jump check (1.30 ×) fires on an ordinary plate step (plate 3 to 4). For plates *(my call)*: one step up never warns; a bigger jump uses the same 1.30 factor on the plate number, against the last set in the same unit. The dialog reads "That is a big jump from plate 3." The high-reps check is unchanged.
- **`plates/PlateLoader.kt`:** kg only. Not called for plate sets.
- **Today, entry and steppers:** `app/src/main/kotlin/com/powertracker/ui/today/Entry.kt` and `web/src/screens/today/entry.ts` (`MAX_KG`, parsing), `stepWeight` and the plate strip in both `ExerciseCard` files, `doseText` and `targetHeadline`, `topSetIndex` (compares only sets in the current unit). Also the "Did you mean …" dialog in `app/src/main/kotlin/com/powertracker/ui/today/TodayDialogs.kt` and `web/src/screens/today/TodayDialogs.tsx`.
- **Set rows:** `app/src/main/kotlin/com/powertracker/ui/components/SetRow.kt` and `web/src/ui/components/SetRow.tsx` print the weight and, in the spoken label, "kg". They print plates for a plate set.
- **Formatting:** `formatKg` (`app/src/main/kotlin/com/powertracker/ui/components/Format.kt`, `web/src/ui/components/format.ts`) stays kg-only. One shared formatter in the domain prints a weight with its unit, so both apps and the coach's reasons say "plate 7½" the same way.
- **Progress charts and records** (`app/src/main/kotlin/com/powertracker/ui/progress/ProgressScreen.kt`, `web/src/screens/progress/ProgressSections.tsx`, `web/src/state/useProgress.ts`): e1RM trend, total, records and volume are main lifts only. Unchanged. The bodybuilding rows (`web/src/screens/progress/BodybuildingProgress.tsx` and the bodybuilding part of `ProgressScreen.kt`) print plates.
- **History:** lift history is main lifts only. Exercise history (`ExerciseHistoryViewModel` in `app/src/main/kotlin/com/powertracker/ui/progress/LiftHistoryViewModel.kt`, `web/src/screens/progress/ExerciseHistoryScreen.tsx`) lists every set from both sides, kg and plate sets each in their own words.
- **Coach screen cards** (`app/src/main/kotlin/com/powertracker/ui/coach/AdviceCard.kt`, `web/src/screens/coach/CoachAdviceCard.tsx`): show e1RM for main lifts only. Unchanged.
- **The web bridge** (`domain/src/jsMain/kotlin/com/powertracker/domain/web/JsTypes.kt`, `domain/src/jsMain/kotlin/com/powertracker/domain/web/GritDomain.kt`): `JsSet`, `JsExercise` and the target payload carry the new fields. `checkInput` takes the unit. New exports cover stepping, rounding and formatting plates. Then `./gradlew :domain:syncWebDomain`, with the regenerated `web/src/domain/generated` committed.
- **Not affected:** plan items and templates (no weights), the plan builder, rest timers, check-ins, the guide, app lock, bodyweight.

## Data and migration
- **Each set records its unit:** kg or plates, kg by default. The number stays in the set's one weight field and means "plate number" when the unit is plates.
  - **Invariant:** no code reads a set's number without its unit. Every e1RM, sum, volume, record and kg rule takes kg sets only, and no plate set ever reaches them as 0 kg or as kg.
  - **Naming is the builder's, with one rule for the tests.** The domain's `LoggedSet` gains the unit as a **last field with a kg default**, so the 78 test fixtures that build a `LoggedSet` (domain and Android tests) and the guide's sample (`domain/src/commonMain/kotlin/com/powertracker/domain/guide/GuideSample.kt`) compile and behave as they are *(my call)*. To find every use, the builder greps for `weightKg` (the list in "How each affected area behaves" is the starting point) rather than renaming the field. The web's test fixtures (`web/src/test/`) write rows without the field, which reads as kg.
- **Each exercise records its setting:** off (kg), whole plates, or half steps. Off by default.
- **Android, Room version 5 to 6:** `MIGRATION_5_6` only adds two columns, each with a default (a unit on `logged_set`, kg; the setting on `exercise`, off), as `MIGRATION_2_3` added `mode`. Nothing stored changes. Schema `6.json` is exported.
- **Web, Dexie version 5 to 6:** stamps every set as kg and every exercise as off, as version 3 stamped `mode`. A row without the field reads as kg or off. *(Changed while planning, 2026-11-20: version 6 is declared but stamps nothing, and a kg / off default is never written to a row or a backup; absent already reads as kg or off, and existing tests compare whole rows across an upgrade.)* No new index. `openGritDb` already writes the safety copy before an upgrade. *(Corrected 2026-11-21: declaring version 6 does not make an older web app refuse the database; Dexie opens a newer database whose stores match. The web is protected instead by its service worker reloading every open window when a new version takes over, and from 1.17 on by `openGritDb` refusing a database a newer Grit wrote, with an "Update Grit to carry on" screen.)*
- **Backup version 6, both apps:**
  - sets gain the unit; absent means kg;
  - exercises gain the setting; absent means off;
  - TARGET payloads may carry a unit; absent means kg. Both backups carry a payload as an opaque string, so neither decoder changes for it.
  - **Versions 1 to 5 still restore,** every set in kg and every exercise off.
  - **A version 6 file in an older app** (1.16 or before) is turned away as too new, with the message both apps already show; nothing is changed.
  - The doc comments listing versions (`Backup`, `format.ts`) gain a line for 6.

## Edge cases
- **Switching units mid-session:** allowed at any time.
  - Sets already logged keep their unit, and today's card shows each in its own words.
  - The weight field empties; the next set is in the new unit.
  - Today's stored target in the old unit stops showing; the coach makes no new one until a set exists in the new unit.
  - Top-set marking and "Last time" compare only sets in one unit. A session with both kinds of sets counts, for the coach, as a session with the sets in the current unit.
- **Half steps turned off after logging a half:**
  - The logged 7½ stays 7½ everywhere.
  - − and + from 7½ go to 7 and 8.
  - The coach's targets land on whole plates: a repeat or a lighter week rounds down (7), and an increase goes to the next whole plate above (8).
  - Typing 7.5 is refused while Whole plates is on.
- **A plate-counted exercise swapped for another** (a swap card or a skip's swap): the new exercise starts in kg, with its own setting off. The setting stays with the old exercise and its history, ready if the lifter swaps back. Swap texts name exercises, not weights.
- **Restoring a version 6 backup into an older app:** turned away as too new; the data on the phone is untouched.
- **First ever set, no history:** no target, as for any new exercise. The field is empty with a PLATE placeholder; + gives plate 1. On the bodybuilding side the reps aim is the bottom of the range.
- **Assisted machines** (assisted pull-up, chin-up, dip): more plates is easier, so "one step up" would make it easier. See open question 3.
- **Switching back to kg:** the coach picks the old kg sets up again, as if plates had been a break from the exercise. Kg targets come from the last kg top set, and the 14-day break and lighter-week rules apply to it as they would to any old top set. *(As built, 2026-11-21: the plates stretch is not treated as a break. The 14-day break rule looks at the gap in the whole log (`Coach.targetsFor` and `BodybuildingCoach.targetsFor` read `lastSessionDate` from the whole history), and the history filtered to the current unit keeps every session, so plate sessions of the same exercise count as training. An old kg top set is therefore not eased when the lifter kept training that exercise in plates, the same as a kg lifter who leaves one exercise out for months while training others. It is eased when there were no sessions at all for 14 days, and while a lighter week or easing back in is active. One case does ease it: `ReturnRule.ease` reads only current-unit sets, so with a session on the other side in the last 28 days and no kg set of the exercise on this side in those days, the side-return rule eases it as it would a kg exercise left out on this side. The same holds in reverse: old plate sets, recent kg sessions, plates turned back on.)*
- **The same exercise on both sides:** one setting for both.
- **Archived and restored exercises:** the setting is kept with the row.
- **Change plates (0.5 and 0.25 kg):** ignored by plate-counted exercises.

## Left to the builder
- **Names:** fields, enum and columns, and the domain field's rename if taken.
- **The sheet's exact layout** on each app, within the sketch above, and the upper bound for typed plates.
- **How the step functions are shaped** in the domain (one place, shared by both coaches, `ReturnRule` and the apps).
- **Tests on both apps:**
  - migration 5 to 6 on both databases, with existing sets read as kg;
  - backup versions 1 to 6 restore, and a version 6 file refused by a version 5 reader;
  - each coach rule in plates: increase, repeat, rep cap at 12, hard and missed top sets, deload, easing back in, both together, the floor at plate 1, half steps on and off;
  - the coach never mixes units: a kg history then plates gives no target;
  - no plate set reaches e1RM, totals, volume or records, and none reads as 0 kg;
  - the menu shows the item only on offered exercises, and never on a main lift or custom variation;
  - a web stand-in gets its row when plates are chosen;
  - display in set rows, the chip, reasons, history and bodybuilding Progress;
  - the big-jump check on plates;
  - every existing kg test and the 1.16 guide's tests unchanged.
- **Screenshots:** Today with a plate-counted exercise, and the sheet, dark and light, on both apps.

## Questions answered by the owner (2026-11-20)

The owner approved this design and accepted each recommendation below. They are decisions now.
1. **Custom variations.** A custom exercise can be an accessory or a variation of squat, bench, deadlift or strict press. Variation sets feed that lift's strength trend (at 90%), stalls and weak points. Should custom variations be offered plate counting?
   **Decided: no, accessories only.** A variation belongs to a main lift, and the main lifts are never in plates.
2. **The exercise chart.** Decision 6 says the exercise's chart plots the plate number, but neither app has a chart for a single machine or cable exercise. Progress charts are main lifts only, and the bodybuilding rows and exercise history are text. Should 1.17 add an exercise chart?
   **Decided: no chart in 1.17.** The rule stands for any chart added later.
3. **Assisted machines** (assisted pull-up, assisted chin-up, assisted dip). On these, more weight is easier, so "one step up" would make the exercise easier. Should they be offered plate counting?
   **Decided: not in 1.17; they stay in kg.** If there is no answer, the builder leaves them out, since a wrong-way target is worse than none. In kg the coach also adds weight on these as if it were harder. That matters only if lifters log the assistance as the weight; nothing in the code says which they log. That is a separate question for its own release.
4. **The powerlifting side's plate progression** (my proposal above): one more rep at the same plate, up to 12, then one step up and back to the planned reps; a step down after a missed or much-too-hard top set. Is that right?
   **Decided: yes.** It is what the kg rule already does when a machine's jump is too big, carried through to the step.
