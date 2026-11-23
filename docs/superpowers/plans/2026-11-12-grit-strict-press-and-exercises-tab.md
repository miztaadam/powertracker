# Strict press, RPE info box and Exercises tab: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:**
- Add strict press as a fourth main lift, with an owner switch that controls whether it shows on Progress and counts in the total.
- Add an RPE ⓘ.
- Add a fifth Exercises tab for browsing the library and adding exercises from it.

**Architecture:**
- **Shared domain:** new `MainLift.STRICT_PRESS` and `Category.STRICT_PRESS`, a strict press accessory map, a `total(includeStrictPress)` function, the catalogue rename, and RPE explainer content.
- **Data, in both apps:**
  - a one-time data upgrade (Room migration 1→2; a Dexie version bump with `upgrade`);
  - a `showStrictPress` setting;
  - backup format v2.
- **UI, in both apps:**
  - Progress respects the switch;
  - Today shows the new miss-point label;
  - the RPE ⓘ;
  - the Exercises tab, built from a browse component extracted from the existing picker.

**Tech Stack:**
- Kotlin Multiplatform (`:domain`).
- Android: Compose, Room, DataStore, with Robolectric tests.
- Web: React and TypeScript, Dexie, Vitest and Playwright.

**Specs:** `docs/superpowers/specs/2026-11-12-grit-strict-press-design.md` (S tasks) and `docs/superpowers/specs/2026-11-12-grit-exercises-tab-design.md` (E tasks).

## Global Constraints

**Enums and labels**
- Enum additions go last:
  - `MainLift { SQUAT, BENCH, DEADLIFT, STRICT_PRESS }`
  - `Category { SQUAT, BENCH, DEADLIFT, STRICT_PRESS, VARIATION, ACCESSORY }`
- Lift label: "Strict press". Total short label: "P".
- Strict press `FLOOR` miss-point label: "Off the shoulders". `MID` is "Mid" and `LOCKOUT` is "Lockout", as for the other lifts.

**Coach rules for strict press**
- +2.5 kg jumps (the bench rule).
- 3-week stall window.
- Included in fatigue and deload signals.
- Never in any weak-point ratio.

**Accessory map**, verbatim:

| Miss point | Muscles | Accessories |
|---|---|---|
| FLOOR | "Front delts", "Upper chest" | Seated barbell press, Incline bench press, Landmine press |
| MID | "Delts", "Triceps" | DB shoulder press, Arnold press, One-arm DB press |
| LOCKOUT | "Triceps", "Upper back" | Close-grip bench, JM press, DB overhead triceps extension |

- Every name must exist in the catalogue exactly as written.
- The fallback row is the same as for the other lifts.

**Total**
- `total(includeStrictPress)` sums S+B+D, plus P when true. It is null if any included lift has no data.
- The label is "Total (S+B+D)" or "Total (S+B+D+P)".

**Setting**
- `showStrictPress: Boolean = false`.
- Switch row title: "Strict press". Supporting text: "Show it on Progress and add it to your total."
- The switch affects Progress and the total only. Coaching is unaffected.

**Catalogue**
- The entry "Overhead press" is renamed "Strict press".
- "Overhead press" becomes an alias. "OHP" and "Military press" stay as aliases.
- category STRICT_PRESS, Kind COMPOUND, area MAIN_LIFTS.

**Upgrade rule**
- Find the non-archived rows whose name, ignoring case, is "Strict press" or "Overhead press", or any alias of the entry.
- The oldest such row becomes `name="Strict press"`, `category=STRICT_PRESS`, `isBuiltIn=true`.
- Every other matching row gets `category=STRICT_PRESS` and keeps its name.
- The upgrade is idempotent.
- The same rule applies when restoring a v1 backup.

**Backups**
- `Backup.VERSION = 2`. v1 files still restore.
- Older apps reject v2 with the existing "too new" message.
- New field: `settings.showStrictPress`. It is optional, defaulting to false.

**RPE sheet**
- Content uses reps in reserve: 10 = no more reps possible; 9 = one more; 8 = two; 7 = three; a half point means "maybe one more"; the app accepts 6–10 in 0.5 steps.
- Sourced, allowed sources only, recorded in the source registry.
- Button label: "What RPE means". No exercise safety line.

**Exercises tab**
- Tab order: Today, Coach, Progress, Exercises, Plan.
- Routes: `#/exercises` (web) and `exercises` (Android).
- Header "Exercises", with the eyebrow "<N> exercises".
- Five tabs must fit at 375 px and 360 dp, with targets ≥ 48 and no ellipsised labels.

**Muscles drop-down (tab only)**
- A multi-select "Muscles" drop-down filter on the tab. The picker keeps its tile grid.
- It shows exercises that work any ticked group, combined with search and equipment.
- Ticked groups show as removable chips with "Clear".
- With nothing ticked, every exercise is listed, grouped by area.
- The ticked groups persist with the tab state.
- Domain: `Library.byMuscles(groups: Set<MuscleGroup>, equipment: Set<Equipment>)`, on the facade as `grit.exercisesByMuscles(groups, equipment)`.

**Tab actions**

| Action | When it shows | Behaviour | Snackbar |
|---|---|---|---|
| "Add to plan" (primary) | Always | Choose a day from the training days, or all seven if none are set. Uses the resolver and the defaults COMPOUND 3×5 / ACCESSORY 3×10 at the default RPE. | "Added to <Day>", action "Open Plan" |
| "Add to today" (secondary) | Only during an open, unfinished session | Uses Today's add path, which creates no row until the first set. | "Added to today", action "Open Today" |

- Duplicate messages:
  - Plan: "Already in your plan on <Day>."
  - Today: "Already in today's session."
- Both actions are guarded against double taps.
- On failure, show "Couldn't save — try again." and change nothing.

**Content rules**
- Every content line traces to an allowed source, found by WebSearch with `allowed_domains`: acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport.
- No /blog/ URLs, forums or sensation thresholds.
- Never fetch the sites directly or bypass bot checks.
- Owner direction: write for experienced lifters, with no beginner reminders.

**Hygiene and tests**
- Every promise that isn't awaited needs a `.catch`.
- Web tests unmount before closing the db.
- e2e data checks use `expect.poll`.
- The existing golden scenario and every existing coach test must pass unchanged.

**Commits** end with the session's attribution lines.

## Review Focus

1. **A lifter who never presses.** With strict press data absent, every coach output must be byte-identical to before: targets, stalls, deload, weak points, Progress and total. Pinned in S1 by `noStrictPressHistory_coachOutputUnchanged`, a golden-scenario comparison.
2. **The switch on but no strict press data yet.** The total shows the existing "needs all lifts" note naming strict press, and Progress doesn't crash on empty strict press series. Pinned in S5 and S6 by `switchOn_noPressData_totalExplains`.
3. **Upgrade on a database where the user created their own "Strict press" and the catalogue "Overhead press" both exist.** The oldest becomes the main lift, both are re-categorised and no history is lost. Pinned in S2 and S3 by `upgrade_twoMatchingRows_keepsAllHistory`.
4. **Restoring a v1 Android backup on the new web app,** and the reverse. The Overhead press row is converted and nothing else changes. Pinned in S2 and S3 with fixture tests.
5. **The Exercises tab's "Add to today" right as the session is finished in another view.** The button disappears or the add fails cleanly with the snackbar, and never adds to a finished session. Pinned in E1 and E2 by `addToToday_afterFinish_doesNotAdd`.

---

### Task S1: Domain — strict press lift, coach, total, catalogue

**Files:**
- Modify: `domain/src/commonMain/kotlin/com/powertracker/domain/model/Models.kt` (enums, mainLift mapping).
- Modify: `domain/.../coach/AccessoryMap.kt` (strict press rows), `ProgressionRule.kt` (jump size), `StallRule.kt` and `CoachSettings.kt` (window), `FatigueRule.kt` and `WeakPointRule.kt` (audit only).
- Modify: `domain/.../progress/ProgressMath.kt`: `total(stats, until, includeStrictPress: Boolean)`.
- Modify: `domain/.../library/areas/Shoulders.kt` → move the entry into `MainLifts.kt`, renamed, with alias. Modify `MuscleGroups.kt` to map "Delts" and "Upper chest", or reuse existing strings. Record the choice.
- Modify: `domain/.../seed/SeedLibrary.kt` only if needed. Seed ids and order must not change.
- Modify: `domain/src/jsMain/.../web/GritDomain.kt` / `JsTypes.kt`: total flag, miss-point label per lift, lift list.
- Regenerate with `./gradlew :domain:syncWebDomain`.
- Test: new `StrictPressTest.kt` in commonTest. Extend the golden scenario and the library tests.

**Interfaces:**
- Produces:
  - `MainLift.STRICT_PRESS` and `Category.STRICT_PRESS`.
  - `fun missPointLabel(lift: MainLift, point: MissPoint): String`.
  - `ProgressMath.total(stats, until, includeStrictPress: Boolean): Double?`.
  - JS: `grit.total(history, until, includeStrictPress)`, `grit.missPointLabel(lift, point)`.

**Steps:**
- [ ] **Step 1: Write the failing tests.** Add these tests:
  - targets on each RPE path with the +2.5 jump;
  - stall at 3 weeks with each accessory row;
  - press counts in the fatigue signals;
  - no ratio includes press;
  - `total` both ways, and null when an included lift is missing;
  - `missPointLabel`;
  - `Library.find("Overhead press").name == "Strict press"`;
  - every MuscleGroups mapping;
  - `noStrictPressHistory_coachOutputUnchanged`.
- [ ] **Step 2:** Run `./gradlew :domain:allTests`. Expected: FAIL.
- [ ] **Step 3: Implement.** Audit every `MainLift.entries` and `Category` `when` across `domain/` and list each one in the report with its rule from spec §2.2.
- [ ] **Step 4:** Run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` and `cd web && npm test && npm run typecheck`. Expected: PASS. App and web compile errors from the new enum values are fixed minimally here, with exhaustive `when` branches. UI behaviour comes later.
- [ ] **Step 5: Commit:** `feat(domain): strict press as a fourth main lift`.

### Task S2: Android data — upgrade, setting, backup v2

**Files:**
- Modify: `app/src/main/kotlin/com/powertracker/data/db/AppDatabase.kt`: version 2, plus `MIGRATION_1_2`, a data-only migration implementing the upgrade rule.
- Modify: `data/SettingsRepository.kt` to add `showStrictPress`.
- Modify: `data/Backup.kt`: VERSION 2, the new field, the v1 conversion on restore.
- Modify: `app/src/test/.../data/__fixtures__` or the existing fixture paths. Add `android-v2.json` and keep the v1 fixtures.
- Test: a migration test with Room `MigrationTestHelper`, or the repo's existing migration-test approach. Extend the backup tests.

**Interfaces:**
- Consumes: S1's enums.
- Produces:
  - `SettingsRepository.showStrictPress: Flow<Boolean>` and `setShowStrictPress(Boolean)`.
  - Backup v2 JSON (the shape follows the existing format).

**Steps:**
- [ ] **Step 1: Write the failing tests:**
  - `upgrade_overheadPressRow_becomesStrictPress_keepsSets`;
  - `upgrade_twoMatchingRows_keepsAllHistory`;
  - `upgrade_noMatch_noChange`;
  - `upgrade_twice_isIdempotent`;
  - `v1Restore_convertsOverheadPress`;
  - `v2RoundTrip_keepsShowStrictPress`;
  - `v2File_onVersion1Decoder_isTooNew`;
  - `webV2Fixture_restoresOnAndroid`.
- [ ] **Step 2:** Run the tests. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4:** Run `./gradlew :domain:allTests :app:testDebugUnitTest :app:assembleDebug`. Expected: PASS.
- [ ] **Step 5: Commit:** `feat(android): strict press upgrade, setting and backup v2`.

### Task S3: Web data — upgrade, setting, backup v2

**Files:**
- Modify: `web/src/db/schema.ts` to append a new `SCHEMA_VERSIONS` entry with an `upgrade` that implements the rule. The existing safety copy runs first.
- Modify: `web/src/db/settings*` to add `showStrictPress`.
- Modify: `web/src/backup/format.ts` and `backup.ts`: VERSION 2, the v1 conversion.
- Add the fixture `web/src/backup/__fixtures__/web-v2.json`. Add `android-v2.json`, copied from S2's fixture.
- Test: db upgrade tests in the existing style (`openGritDb` with injected `schemaVersions`), plus backup tests.

**Interfaces:**
- Consumes: S1's facade.
- Produces: the settings field `showStrictPress`, and backup v2.

**Steps:**
- [ ] **Step 1: Write the failing tests:** the same names as S2, plus `androidV2Fixture_restoresOnWeb` and `upgradeFailure_stillOpensRecovery`.
- [ ] **Step 2:** Run the tests. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4:** Run `cd web && npm test && npm run typecheck && npm run build`. Expected: PASS.
- [ ] **Step 5: Commit:** `feat(web): strict press upgrade, setting and backup v2`.

### Task S4: RPE explainer content

**Files:**
- Create: `domain/.../library/RpeGuide.kt`. It holds `object RpeGuide { val title: String; val lines: List<String>; val sources: List<SourceId> }`.
- Add the sources to `Sources.kt`.
- Expose it on the facade as `grit.rpeGuide()`.
- Regenerate `docs/library/sources.md`.
- Test: an `RpeGuideTest` that checks the content is complete, every value from 6 to 10 is explained, and the sources are allowed and known.

**Steps:**
- [ ] **Step 1:** Research the reps-in-reserve RPE scale with WebSearch on the allowed domains. Write the RED test.
- [ ] **Step 2:** Implement and run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain`. Expected: PASS.
- [ ] **Step 3: Commit:** `feat(domain): RPE explainer content`.

### Task S5: Android UI — Progress switch, labels, Settings switch, RPE ⓘ

**Files:**
- Modify the following:
  - `app/src/main/kotlin/com/powertracker/ui/progress/*`: strict press series, tile, records, volume and lift history when the switch is on, and the total label.
  - `ui/today/*`: the miss-point label through `missPointLabel`, and the rest timer and plate rules for strict press.
  - `ui/plan/SettingsScreen.kt`: the switch row and the RPE ⓘ next to the default RPE.
  - Plan item dialog: the RPE ⓘ.
  - Today card: the RPE ⓘ.
- Create: `ui/library/RpeSheet.kt`, using the guide sheet style.
- Test: extend `ProgressScreenTest` / `ProgressViewModelTest`, `TodayScreenTest` and the Settings tests. Add screenshots.

**Steps:**
- [ ] **Step 1: Write the failing tests:**
  - `switchOff_progressUnchanged`;
  - `switchOn_showsStrictPressEverywhere`;
  - `totalLabel_bothWays`;
  - `switchOn_noPressData_totalExplains`;
  - `today_strictPressMissLabel`;
  - `rpeInfo_opensFromTodayPlanAndSettings`;
  - 360 dp fit.
- [ ] **Step 2:** Run the tests. Expected: FAIL.
- [ ] **Step 3: Implement.** Re-render the screenshots with `-Pscreenshots.update=true`.
- [ ] **Step 4:** Run `./gradlew :domain:allTests :app:testDebugUnitTest :app:assembleDebug`. Expected: PASS.
- [ ] **Step 5: Commit:** `feat(android): strict press on Progress, settings switch and RPE info`.

### Task S6: Web UI — the same as S5

**Files:** the web counterparts, under `web/src/screens/progress/*`, `screens/today/*`, `screens/settings/*` and `screens/plan/*`. Create `screens/library/RpeSheet.tsx`. Add tests and e2e checks, and the screenshots.

**Steps:**
- [ ] **Step 1: Write the failing tests:** the same names as S5, plus the 375 px fit.
- [ ] **Step 2:** Run the tests. Expected: FAIL.
- [ ] **Step 3: Implement.** Re-render the screenshots with `UPDATE_SCREENSHOTS=1`.
- [ ] **Step 4:** Run `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: PASS.
- [ ] **Step 5: Commit:** `feat(web): strict press on Progress, settings switch and RPE info`.

### Task E1: Android — shared browse component and Exercises tab

**Files:**
- Modify: `ui/library/ExercisePickerScreen.kt` to extract `ExerciseBrowser(...)`, covering search, chips, the muscle grid, muscle mode and rows. The picker keeps its pick behaviour.
- Create: `ui/library/ExercisesScreen.kt` and the sheet actions.
- Modify: `ui/AppShell.kt` to add the fifth tab.
- Modify: the Plan and Today add paths so they're callable from the tab. Reuse the existing view-model actions and the resolver.
- Test: `app/src/test/.../ui/library/ExercisesScreenTest.kt` with all the test names from spec §6 (including the three `musclesDropdown_*` tests), plus `addToToday_afterFinish_doesNotAdd`. Add `Library.byMuscles` to the domain with a test, `byMuscles_anyOf_alphabetical_filtersEquipment`, in E1. Extend the tab bar tests. Add screenshots.

**Interfaces:**
- Produces: `@Composable fun ExerciseBrowser(state, onRow: (entry) -> Unit, onInfo: (entry) -> Unit)`. Both the picker and the tab use it.

**Steps:**
- [ ] **Step 1: Write the failing tests.** Run them. Expected: FAIL.
- [ ] **Step 2: Implement.** The existing picker tests must stay green, and the browse behaviour must not be copied.
- [ ] **Step 3:** Run `./gradlew :domain:allTests :app:testDebugUnitTest :app:assembleDebug`. Expected: PASS. Re-render the screenshots.
- [ ] **Step 4: Commit:** `feat(android): Exercises tab`.

### Task E2: Web — shared browse component and Exercises tab

**Files:**
- Extract the browse component from `web/src/screens/library/ExercisePicker.tsx`, using `web/src/state/exercisePicker.ts`.
- Create: `web/src/screens/library/ExercisesScreen.tsx`.
- Modify: `web/src/router.tsx` (the `Tab` type, routes and order), `App.tsx` and `ui/components/TabBar.tsx`.
- Test: unit tests with the same names as E1. Add e2e checks for the five-tab fit at 375 px and the add-to-plan flow. Add screenshots.

**Steps:**
- [ ] **Step 1: Write the failing tests.** Run them. Expected: FAIL.
- [ ] **Step 2: Implement.** The existing picker tests must stay green.
- [ ] **Step 3:** Run `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: PASS. Re-render the screenshots.
- [ ] **Step 4: Commit:** `feat(web): Exercises tab`.
