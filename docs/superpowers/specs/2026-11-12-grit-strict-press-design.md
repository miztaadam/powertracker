# Grit: strict press as a fourth main lift, and an RPE info box

**Date:** 2026-11-12
**Status:** Approved in brainstorming. Awaiting the owner's review of this spec.
**Applies to:** the shared Kotlin domain (`domain/`), Android (`app/`) and web (`web/`). All three ship together.
**Part of:** the next update (`docs/next-update.md`). This is the last item on that list. The Exercises tab is specced separately.

## 1. Purpose

The owner trains the strict press and wants it treated like squat, bench and deadlift. That means the coach gives it targets, detects stalls and prescribes accessories, and it has its own Progress charts and records. The owner also wants a switch that controls whether it shows on Progress and counts in the total. Separately, some lifters don't know what RPE means, so the app gets an ⓘ that explains it.

**Decisions made with the owner:**

- The library's existing **"Overhead press"** entry (standing barbell overhead press) **becomes the Strict press main lift**. It is not a separate exercise.
- One **"Strict press" switch** in Settings, **off by default**. On: strict press shows on Progress and counts in the total. Off: it is hidden from Progress and left out of the total. Its data is kept either way.
- **No ratio check against bench**, and no other weak-point ratio either.
- The stall accessory map is as approved (§3.2).
- An **RPE ⓘ** appears wherever RPE is entered.

## 2. Model changes (shared domain)

### 2.1 Enums

- **`MainLift`:** add `STRICT_PRESS`, appended last. It becomes `SQUAT, BENCH, DEADLIFT, STRICT_PRESS`.
- **`Category`:** add `STRICT_PRESS`, appended after `DEADLIFT`. Main-lift categories map to `MainLift` one-to-one, as they already do.
- **Display:**
  - The label is "Strict press". The short label in totals is "P".
  - `MissPoint` labels are per lift. For strict press, `FLOOR` reads **"Off the shoulders"**. `MID` and `LOCKOUT` keep their labels. The stored enum values don't change.

### 2.2 Every use of `MainLift.entries` is audited

Adding an enum value changes every place that loops over all main lifts. Each one is classified and gets a test:

| Use | Rule after this change |
|---|---|
| Coach progression targets, stall rule, accessory map | Strict press included, as for any main lift. |
| Fatigue / deload signals ("2 or more lifts") | Strict press counts as a lift. A lifter who never presses gets exactly today's behaviour. |
| Weak-point ratios | Unchanged: only squat ÷ deadlift and bench ÷ squat. Strict press is never part of a ratio. |
| `ProgressMath.total` | Takes a parameter `includeStrictPress: Boolean`. The lifts summed are S+B+D, plus P when true. It returns null if any included lift has no data, as today. |
| Progress screens: charts, records, volume, lift history | Strict press is shown only when the switch is on. |
| Backup, seed and AccessoryMap integrity checks | Updated to know the new values. |

The existing golden scenario and every existing coach test must pass **unchanged** for histories with no strict press.

### 2.3 Progression and stall settings

- **Load jump:** like bench and accessories, +2.5 kg on the "RPE ≥ 1 below plan" path. Every other path follows the existing rule.
- **Stall window:** 3 weeks, like squat and bench.
- **Safety rails:** the existing rules apply as written. These are the deload priority, the 5% cap, one suggestion per lift per week, the minimum history, the re-entry week after a 14-day gap, and variations at 0.9.

### 2.4 Catalogue entry

- The catalogue entry's **name changes from "Overhead press" to "Strict press"**. The old name, "Overhead press", becomes an alias, so it stays findable. "OHP" and "Military press" stay aliases.
  - This is the one allowed rename. It follows the catalogue rule: the old name is kept as an alias.
  - **`Library.find("Overhead press")`** resolves to Strict press.
- **Classification:** category `STRICT_PRESS`, Kind `COMPOUND`, area `MAIN_LIFTS`, parentLift null. Muscles stay as they are.
- **Guide:** the existing reviewed guide is kept. The purpose line is reworded to name the lift. The rack safety line stays.
- **Variations:** the existing shoulder presses stay ACCESSORY entries. No press variations are added in this project.

## 3. Coach

### 3.1 Targets

Strict press gets next-session targets exactly as the other main lifts do, with the bench-style +2.5 kg jump (§2.3).

### 3.2 Stall accessory map (approved)

| Lift | Miss point | Muscles | Accessories (catalogue names) |
|---|---|---|---|
| Strict press | Off the shoulders (`FLOOR`) | Front delts, Upper chest | Seated barbell press, Incline bench press, Landmine press |
| Strict press | Mid | Delts, Triceps | DB shoulder press, Arnold press, One-arm DB press |
| Strict press | Lockout | Triceps, Upper back | Close-grip bench, JM press, DB overhead triceps extension |
| Strict press | None (fallback) | — | The same fallback as the other lifts |

- **Names:** every name must exist in the catalogue exactly as written. The existing `everyAccessoryMapName_isInTheCatalogue` test covers this.
- **Muscle strings:** they must map to a muscle group (`MuscleGroups`). "Delts" and "Upper chest" need a mapping or an existing equivalent, recorded in the plan.
- **Heavy flag:** follows the existing convention. Close-grip bench stays "heavy" (a variation of bench). The rest are accessory doses.

### 3.3 Weak points

`WeakPointRule` doesn't change, so strict press is never flagged as lagging against another lift.

## 4. Data, upgrade and backups

### 4.1 One-time upgrade in both apps

When the app upgrades:

1. **Matching rows:** find non-archived exercise rows whose name, ignoring case, is "Overhead press" or any alias of the Strict press catalogue entry, or "Strict press" itself.
2. **Convert:** change the oldest of those rows to `name = "Strict press"`, `category = STRICT_PRESS`, `isBuiltIn = true`. Its sets, plan items and history stay attached through its id.
3. **Other matching rows:** any others keep their rows and become `category = STRICT_PRESS` too, so their history also counts towards the strict press trend. Their names are left as they are.
4. **Mechanism:**
   - **Android:** a Room migration (schema version +1, no table change, data only), with a migration test.
   - **Web:** a Dexie version bump with an `upgrade` function, preceded by the existing safety copy. A failure follows the existing `UpgradeFailedError` → recovery path.
5. **Repeatable:** running the upgrade twice changes nothing more.

### 4.2 Settings

- **Setting:** add `showStrictPress: Boolean = false`, in Android DataStore and the web `settings` table.
- **Settings screen:** a switch row titled "Strict press", with the supporting text "Show it on Progress and add it to your total." It goes in the coach or training section.

### 4.3 Backup format version 2

- **Version:** `Backup.VERSION` becomes **2**. Exports write version 2.
- **New content:**
  - exercise rows may now carry category `STRICT_PRESS`;
  - settings gain `showStrictPress`;
  - the web keeps its existing `webRestAlert` field.
- **Restoring:**
  - **Version 1 backups** still restore. A v1 file has no strict press category and no `showStrictPress`, which defaults to false. A v1 file with an "Overhead press" row is converted on restore by the same rule as §4.1.
  - **A version 2 backup on an older app** gets the existing "too new" message. That's why both apps update together.
- **Fixtures:**
  - the Android and web v2 fixtures are added, and the v1 fixtures are kept;
  - two-way tests cover v1→v2 and v2 round-trips on both apps.

## 5. User interface (both apps)

### 5.1 Today

- Strict press behaves like the other main lifts on Today:
  - the plate strip;
  - the main-lift rest timer, which uses the main-lift default duration;
  - miss-point chips reading "Off the shoulders / Mid / Lockout".
- The rest timer and plate rules for "main lift or variation" include strict press.

### 5.2 Progress

- **Switch on:** strict press appears as a fourth lift, with an e1RM chart, a stat tile, records, a weekly volume row and its own lift-history page. These follow the same layout as the other three lifts.
- **Switch off:** none of that shows. The Progress screen is exactly as it is today.
- **Total:**
  - The total always carries its label: "Total (S+B+D)" or "Total (S+B+D+P)".
  - With the switch on, the total is null when strict press has no data. This follows the existing rule that every included lift needs data. In that case the screen shows the same "needs all lifts" note it uses today, naming strict press.

### 5.3 Coach

- Strict press advice cards (targets, stall prescriptions, re-entry) appear whatever the switch says. The switch controls Progress and the total, not the coaching.

### 5.4 RPE ⓘ

- **Placement:** an ⓘ sits next to the RPE field label on the Today exercise card, next to "Target RPE" in the Plan item dialog, and next to the default-RPE setting in Settings.
- **The sheet:** it uses the existing guide-sheet style. It explains RPE as effort measured by **reps in reserve**:
  - 10 = no more reps possible;
  - 9 = one more rep was possible;
  - 8 = two more;
  - 7 = three more;
  - half points, such as 8.5, mean "maybe one more";
  - the app accepts 6–10 in 0.5 steps.
- **Sources:** the text is sourced like the guides, from allowed sources, for example the PMC papers that define the RIR-based RPE scale. The sources are recorded in the catalogue source registry, and the sheet ends with no safety line, because it isn't an exercise.
- **Accessibility:** the button label is "What RPE means". The touch target is at least 48.

## 6. Tests

- **Domain:**
  - Strict press targets on each RPE path.
  - Stall detection at 3 weeks, and the accessory map per miss point.
  - Strict press counts in deload signals.
  - No weak-point ratio includes strict press.
  - `total(includeStrictPress)` both ways.
  - Every existing test and the golden scenario unchanged.
  - The catalogue rename and the alias lookup.
  - `MuscleGroups` covers the new muscle strings.
- **Upgrade:**
  - Android Room migration test.
  - Web Dexie upgrade test.
  - One "Overhead press" row with sets → converted, with history kept.
  - Two matching rows → the oldest is renamed and both are re-categorised.
  - No matching row → no change.
  - Running the upgrade twice → no change.
- **Backups:**
  - v1 restore, including the "Overhead press" conversion.
  - v2 round-trip.
  - Android↔web v2 fixtures.
  - An older version that reads v2 shows "too new". This is tested at the `decodeBackup` level with VERSION = 1.
- **UI (both apps):**
  - Progress with the switch on and off.
  - The total label both ways.
  - The Today miss-point label for strict press.
  - The RPE ⓘ opens from all three places.
  - No overflow at 375 px or 360 dp, and touch targets at least 48.
- **Screenshots:** Progress with the switch on, the Settings switch and the RPE sheet, dark and light, in both apps.

## 7. Delivery

- **Branch:** the existing working branch. Merge to `main` only with the owner's go-ahead.
- **Build order:**
  1. domain (enums, coach, total, catalogue, MuscleGroups) with tests;
  2. Android data (migration, setting, backup v2);
  3. web data (Dexie upgrade, setting, backup v2);
  4. Android UI;
  5. web UI;
  6. RPE sheet content (sourced) and both UIs.
- **Review:** each task gets its own review, and the whole branch gets a final review.
- **Who builds:** Fable builds the UI tasks and Opus builds the domain, data and sourced content. Sonnet reviews each task, and Opus does the final review.
