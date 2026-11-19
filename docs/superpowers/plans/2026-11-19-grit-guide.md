# The Grit Guide (1.16) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A full-screen, 8-screen animated guide on both apps that opens on a new install, replays from Settings, and explains the plan, logging and the coach with a sample lifter.

**Architecture:** The guide is defined once in the shared domain: screens, copy built from the coach's own constants, a per-side rule table, and a sample lifter whose numbers are worked out by the real coach code. Each app draws it with its own real screen content components (`TodayContent`, `PlanContent`, `CoachContent`, `ProgressContent`), fed sample state and shown small inside a frame. Each picture is a short list of sample states stepped on a clock. Screen 2 draws from `ModeSwitchMotion.dropFrame` / `pumpFrame` in a small box.

**Tech Stack:** Kotlin Multiplatform domain (`@JsExport` through `GritDomain`), Android Compose, React and TypeScript, Robolectric, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-19-grit-guide-design.md`. Read it with this plan, including "Settled after the builder check" and "Amended while planning".

## Global Constraints

- **Version:** 1.16 on both apps, versionCode 26.
- **Nothing of the lifter's is written** by the guide, except: the side picked on screen 2 when the guide opened by first launch, and the "seen" flag on that same path. A replay from Settings writes nothing at all.
- **Coach text:** every number in the guide's copy comes from a named constant in the coach's code. No sources are named in the app.
- **No sound, no vibration** anywhere in the guide. It never calls `ModeSwitchController.start` or `modeSwitch.flip`.
- **Nothing Chromium-only** on the web: no View Transitions, no `pseudoElement` animations.
- **Not loaded until opened:** on the web the guide is a lazy chunk; on Android nothing of it is built or computed at app start beyond reading the "seen" flag.
- **Reduce motion:** every screen shows its last state, still.
- **No Room schema change** (DB stays v5) and no Dexie schema change (stays v5).
- **Each screen's text** is under about 40 words, in plain words.
- **Fixed rules:** new commits only; no model names in code or commits; stage by path; never commit `.claude/`, `CLAUDE.md`, `.superpowers/`; e2e with `--trace off`; never `playwright install`; every commit passes `npx tsc -b`; the commit trailer comes from the session's latest attribution reminder.
- **e2e runs with motion off.** Tests of the guide in motion use a held clock, as the 1.15 pins do.
- **Speed:** hard checks are structural (the lazy chunk isn't requested before the guide opens; no layout reads per frame). Frame timing only reports. The 300 ms first-screen check is modelled on the Exercises-tab speed test and is hard only if it reads under about 150 ms on the build machine; otherwise it reports.

## Review Focus

1. **The app is killed mid-guide after the side pick:** on the next start the guide opens again on screen 1 (not yet "seen"), the side picked stays, and nothing else was written.
2. **Back from the template list on a new install** lands on Plan, not back in the guide, and the guide doesn't reopen.
3. **Rotation or a resize** keeps the current screen (Android configuration change; web resize).
4. **Web, in a browser tab:** the guide shows first; the install card appears only after the guide is closed.
5. **Large text on screen 6** (the longest copy): at 200 % font the text wraps, nothing is cut, and Skip, Back and Next stay on screen.

Each has a test in the task that owns it (Tasks 3 and 4).

## File Structure

- **Domain**
  - Create `domain/src/commonMain/kotlin/com/powertracker/domain/guide/Guide.kt`: screens, copy, rule table.
  - Create `domain/src/commonMain/kotlin/com/powertracker/domain/guide/GuideSample.kt`: the sample lifter, computed with the real coach.
  - Create `domain/src/commonTest/kotlin/com/powertracker/domain/guide/GuideTest.kt`, `GuideSampleTest.kt`.
  - Modify `domain/src/jsMain/kotlin/com/powertracker/domain/web/GritDomain.kt` and add `JsGuideTypes.kt`; run `:domain:syncWebDomain` and commit `web/src/domain/generated`.
- **Android** (`app/src/main/kotlin/com/powertracker/`)
  - Create `ui/guide/GuideScreen.kt` (shell: pager, controls), `ui/guide/GuidePictures.kt` (one composable per screen), `ui/guide/GuideSampleState.kt` (domain sample → the content components' state).
  - Create `data/GuideSeen.kt` (the "seen" key and the new-user check).
  - Modify `ui/AppShell.kt` (route `guide`, first-launch gate), `ui/plan/SettingsScreen.kt` (the Guide row), `data/db/Daos.kt` (two EXISTS queries).
- **Web** (`web/src/`)
  - Create `screens/guide/GuideScreen.tsx`, `GuideScreen.css`, `GuidePictures.tsx`, `guideSampleState.ts`, `guideSeen.ts`.
  - Modify `router.tsx` (route `#/guide`), `App.tsx` (lazy route, first-launch gate, install card order), `screens/settings/SettingsScreen.tsx` (the Guide row).

---

### Task 1: The guide's definition in the domain

**Files:** the Domain list above.

**Interfaces — Produces:**

```kotlin
enum class GuideScreenId { WELCOME, SIDE, PLAN, TODAY, TARGETS, HOLDS_BACK, MONTHS_LATER, YOUR_TURN }

data class GuideLine(val text: String, val constants: List<String>)   // constants: names of the code constants its numbers come from
data class GuideScreen(val id: GuideScreenId, val title: String, val lines: List<GuideLine>, val example: Boolean)  // example: shows sample numbers, so the picture is labelled "Example"

object Guide {
    val SCREENS: List<GuideScreenId>                       // the 8, in order
    fun screen(id: GuideScreenId, mode: TrainingMode): GuideScreen
    const val STEP_MS: Int = 900                           // one picture step
}

object GuideSample {
    fun plan(mode: TrainingMode): List<PlanItem>           // a 3-day template week from PlanTemplates
    fun targets(mode: TrainingMode): List<GuideTargetStep> // one lift, session by session: what was done, the coach's next target
    fun progress(mode: TrainingMode): GuideProgress        // about 6 months: chart points, bests, and on bodybuilding a check-in's lagging muscle
}
```

JS exports on `GritDomain`: `guideScreens()`, `guideScreen(id, mode)`, `guideStepMs()`, `guideSamplePlan(mode)`, `guideSampleTargets(mode)`, `guideSampleProgress(mode)`, with plain `Js*` mirrors in `JsGuideTypes.kt`.

- [ ] **Step 1: Write the per-side rule table** as a comment block at the top of `Guide.kt`, by reading the callers (`Coach.kt`, `BodybuildingCoach.kt`, `PhysiqueCoach.kt`, `SkipCoach.kt`, `FatigueRule.kt`, `StallRule.kt`, `WeakPointRule.kt`). Columns: rule, code path, which side's coach runs it, constant(s), the guide's sentence. Rules: progression (powerlifting `ProgressionRule`, +2.5 kg on target, capped by `MAX_INCREASE`; bodybuilding reps first then `BodybuildingCoach.INCREASE`), deload (`FatigueRule`, `AdvicePayload.Deload`, `Coach.DELOAD_COOLDOWN_DAYS`), easing back (`Coach.GAP_DAYS`, `ReEntry`), the pain hold (`SkipCoach.held`: until that exercise is next logged), swap suggestions (`PhysiqueCoach.STUCK_DAYS`, bodybuilding only), check-ins (`PhysiqueCoach.CHECK_IN_DAYS`, bodybuilding only), stall and weak point (powerlifting only). A rule a side's coach doesn't run gets no sentence on that side.
- [ ] **Step 2: Write the failing tests** in `GuideTest.kt`:
  - `everyScreenOnBothSides_hasATitleAndUnderFortyWords`
  - `everyNumberInTheCopy_comesFromItsDeclaredConstant`: for each `GuideLine`, every number in `text` equals the value of one of `constants`, resolved through a test map of name → the real constant (e.g. `"Coach.GAP_DAYS" to Coach.GAP_DAYS`). Changing a constant must change the copy or fail this test.
  - `bodybuildingOnlyRules_areAbsentOnPowerlifting_andTheReverse`: screen 6 and 7 on powerlifting mention no swap or check-in; on bodybuilding no stall or weak point.
  - `screen7_onPowerlifting_endsOnBestsAndTotal_notACheckIn`
  - `noLineNamesASource`: no line contains any `CoachSources` publisher name.
- [ ] **Step 3: Write the failing tests** in `GuideSampleTest.kt`:
  - `targets_areWhatTheRealCoachGives`: replay `GuideSample.targets(mode)` through `Coach` / `BodybuildingCoach` and assert each step's target equals the coach's output for the history before it.
  - `plan_isAPlanTemplatesDraft` for each side.
  - `progress_bestsNeverFall_andStayWithinTheProgressionCap`.
- [ ] **Step 4: Run** `./gradlew :domain:allTests` — the new tests fail (unresolved references).
- [ ] **Step 5: Implement** `Guide` and `GuideSample`. Copy is built with string templates from the constants, never typed numbers. Sample numbers are produced by calling the real coach in a loop from fixed starting lifts (literals: the starting weights and the sample's rep/RPE results only).
- [ ] **Step 6: Add the JS exports,** run `./gradlew :domain:syncWebDomain :domain:allTests :domain:checkWebDomain` — all pass.
- [ ] **Step 7: Commit** `feat(domain): the guide's screens, copy and sample lifter`.

### Task 2: The guide's shell and one picture on both apps, then the owner's look

**Files:** Android `ui/guide/GuideScreen.kt`, `GuidePictures.kt`, `GuideSampleState.kt`, route in `AppShell.kt`; web `screens/guide/*`, `router.tsx`, `App.tsx` (lazy route only). Tests: `app/src/test/kotlin/com/powertracker/ui/guide/GuideScreenTest.kt`, `web/src/screens/guide/GuideScreen.test.tsx`, `web/e2e/guide.spec.ts`.

**Interfaces — Consumes:** Task 1's `Guide`, `GuideSample`. **Produces:**

```kotlin
@Composable fun GuideScreen(mode: TrainingMode, firstLaunch: Boolean, hasPlan: Boolean, motionOn: Boolean,
    onPickSide: (TrainingMode) -> Unit, onClose: (GuideExit) -> Unit, clock: GuideClock = rememberGuideClock())
enum class GuideExit { SKIPPED, DONE, CHOOSE_TEMPLATE, BUILD_PLAN, SEE_PLAN }
```

```ts
export function GuideScreen(props: { mode: TrainingMode; firstLaunch: boolean; hasPlan: boolean; motionOn: boolean;
  onPickSide: (mode: TrainingMode) => void; onClose: (exit: GuideExit) => void; clock?: GuideClock }): JSX.Element
export type GuideExit = 'skipped' | 'done' | 'choose-template' | 'build-plan' | 'see-plan'
```

`GuideClock` gives the current step for a picture and can be held by tests. Test tags / test ids, the same on both apps: `guide`, `guide-skip`, `guide-back`, `guide-next`, `guide-dots`, `guide-title`, `guide-picture`, `guide-example`.

- [ ] **Step 1: Write failing tests** (both apps, same names where the framework allows):
  - `guide_showsEightDots_andMovesWithNextBackAndSwipe`
  - `guide_skipIsOnEveryScreen_andClosesWithSkipped`
  - `guide_systemBack_closesWithSkipped` (Android back; web browser back / Escape)
  - `guide_screen4_stepsThroughLoggingASet_onAHeldClock`: at step 0 the set row is empty; by the last step the weight, reps and RPE are filled and the tick is on; then Skip today is shown.
  - `guide_reduceMotion_showsTheLastStepStill`
  - `guide_picturesShowingNumbers_areLabelledExample`
  - `guide_pictureIsNotInteractive`: taps inside `guide-picture` do nothing and it is hidden from the accessibility tree except for one description.
- [ ] **Step 2: Run them — they fail.**
- [ ] **Step 3: Implement the shell:** a horizontal pager of `Guide.SCREENS`, the dots, Skip top right, Back and Next. Screens other than TODAY show the title and lines with an empty picture frame for now. The page index survives rotation (`rememberSaveable`) and web resize.
- [ ] **Step 4: Implement screen 4's picture** with the real `TodayContent` fed `GuideSampleState` states, scaled to fit the frame, with no-op actions. One state per step; the clock advances every `Guide.STEP_MS`. If `TodayContent` can't be driven this way cleanly on either app, stop and report before drawing a mock-up: the plan's architecture depends on it.
- [ ] **Step 5: Web: make the guide a lazy chunk** (`React.lazy`), route `#/guide`. e2e `guide.spec.ts`: `theGuidesChunk_isNotRequestedBeforeItOpens` (record requests on load of `#/`, then open `#/guide`).
- [ ] **Step 6: Screenshots:** screen 4 at its last step, dark and light, both sides, both apps (`guide-today-*`), through the existing `ScreenshotTest.render` and the web `shoot` helper.
- [ ] **Step 7: Run** the Android unit tests, `npm test`, `npx tsc -b`, `npx playwright test e2e/guide.spec.ts --trace off`. Commit `feat: the guide's shell and its Today picture (both apps)`.
- [ ] **Step 8: Stop. The owner sees the screen-4 shots** (and a short screen recording or frame strip of the steps) before the other pictures are built.

### Task 3: Opening it: first launch, "seen", the side pick, screen 8, the Settings row

**Files:** Android `data/GuideSeen.kt`, `data/db/Daos.kt`, `ui/AppShell.kt`, `ui/plan/SettingsScreen.kt`; web `screens/guide/guideSeen.ts`, `App.tsx`, `screens/settings/SettingsScreen.tsx`, `screens/install/InstallCard.tsx` (order only). Tests beside each, plus `web/e2e/guide.spec.ts`.

**Interfaces — Produces:**

```kotlin
class GuideSeen(context: Context) {            // key "guide_seen" in the "settings" DataStore, outside AppSettings
    val seen: Flow<Boolean>
    suspend fun markSeen()
}
suspend fun isNewUser(sessions: SessionDao, plans: PlanDao): Boolean   // no sessions and no plan items on either side
// Daos: @Query("SELECT EXISTS(SELECT 1 FROM sessions)") suspend fun any(): Boolean  (and the same for plan items; use the tables' real names)
```

```ts
export const GUIDE_SEEN_KEY = 'guide:seen'                       // in `meta`
export function guideSeen(db: GritDb): Promise<boolean>
export function markGuideSeen(db: GritDb): Promise<void>
export function isNewUser(db: GritDb): Promise<boolean>          // sessions.count() === 0 && planItems.count() === 0
```

**The two conditions** (keep them separate):
- `firstLaunch` = the guide was opened by the first-launch gate (`!seen && isNewUser`). Only then does screen 2 show the two side buttons, and only then is "seen" written, when the guide closes by any exit.
- `hasPlan` = the side now on has plan items. Screen 8 shows **Choose a template** (and on bodybuilding **Build my plan** below it) when `!hasPlan`; otherwise **Done** and **See my plan**.

- [ ] **Step 1: Write failing tests** (both apps):
  - `firstLaunch_withNoData_opensTheGuideOnce`: open, close, restart: it doesn't open again.
  - `firstLaunch_withASessionOrAPlanItemOnEitherSide_neverOpensIt` (four cases).
  - `seenIsReadFirst_soASeenDeviceRunsNoDataQuery` (Android: the DAO is not called when seen).
  - `screen2_onFirstLaunch_setsTheRealSide_withNoAnimationSoundOrVibration`: `setTrainingMode` is called with the pick; the mode-switch controller's `start` / `flip` is never called.
  - `screen2_pickingTheSideAlreadyOn_writesNothing`.
  - `screen2_onAReplay_showsNoButtons`.
  - `killedAfterTheSidePick_reopensOnScreen1_withTheSideKept` (Review Focus 1).
  - `screen8_withNoPlan_opensThatSidesTemplateList`; on bodybuilding `buildMyPlan_opensTheBuilder`.
  - `backFromTheTemplateList_landsOnPlan_andTheGuideStaysClosed` (Review Focus 2).
  - `screen8_withAPlan_offersDoneAndSeeMyPlan_andReplacesNothing`.
  - `settings_hasAGuideRow_thatOpensTheGuideAsAReplay`: row "Guide", supporting text "See how Grit works".
  - `replay_writesNothing`: snapshot every table and every settings/meta key before and after a full replay ending in Done; they are equal (web: all Dexie stores; Android: the Room tables and the whole DataStore).
  - `aBackupMadeAfterTheGuide_hasNoGuideKey`, and `restore_keepsSeen` on both apps.
  - Web e2e: `inABrowserTab_theGuideComesFirst_thenTheInstallCard` (Review Focus 4).
- [ ] **Step 2: Run them — they fail.**
- [ ] **Step 3: Implement.** Android: gate in `AppShell` before the NavHost's first frame settles (read `seen`, then `isNewUser`), route `guide`, exits mapped to `Routes.templates(mode)`, `PLAN_BUILDER`, Plan, or back. Web: the same in `App.tsx`; the install card renders only when the guide is not open. If the web export writes `meta` whole, exclude `guide:seen` from it.
- [ ] **Step 4: Run** the Android unit tests, `npm test`, `npx tsc -b`, `npx playwright test e2e/guide.spec.ts --trace off` — pass. Commit `feat: the guide opens on a new install and from Settings (both apps)`.

### Task 4: The other seven pictures

**Files:** `GuidePictures.kt`, `GuideSampleState.kt`; `GuidePictures.tsx`, `guideSampleState.ts`; tests and shots as in Task 2.

**Interfaces — Consumes:** Task 1's sample data, Task 2's `GuideClock` and frame.

Pictures, each a short list of steps on the clock:
1. **WELCOME:** the app's mark and the two sides' colours; no numbers.
2. **SIDE:** a small box drawing `ModeSwitchMotion.dropFrame` and `pumpFrame` in a loop (cut down: the hammer's fall and impact; the pump's three beats), side by side, silent. On first launch the two buttons sit under it.
3. **PLAN:** `PlanContent` filling a sample week from `GuideSample.plan`: empty, the template list's days-a-week choice, the week filled, one Swap.
5. **TARGETS:** one lift's target card stepping through `GuideSample.targets` (on bodybuilding: reps climb, then the weight).
6. **HOLDS_BACK:** `CoachContent` advice cards for that side's rules, one per step, from the rule table.
7. **MONTHS_LATER:** `ProgressContent` with `GuideSample.progress`: the chart drawing in, the bests counting up; on bodybuilding a check-in naming the lagging muscle, on powerlifting the total.
8. **YOUR_TURN:** the Plan tab's empty state or the lifter's side colours, with the buttons.

- [ ] **Step 1: Write failing tests** (both apps): per screen, `guide_<screen>_firstAndLastStep` on a held clock for each side; `guide_screen2_callsNoControllerSoundOrVibration`; `guide_screen6_atTwiceTheFontSize_cutsNothing_andKeepsTheControls` (Review Focus 5); `guide_rotationOrResize_keepsTheScreen` (Review Focus 3); web held-clock pin `guide_noLayoutReadsPerStep` (count `getBoundingClientRect` and `offset*` reads across steps: zero after the first frame).
- [ ] **Step 2: Run them — they fail. Step 3: Implement. Step 4: Run — pass.**
- [ ] **Step 5: Screenshots:** every screen's last step, dark and light, both sides, both apps (`guide-<screen>-<side>-<scheme>`).
- [ ] **Step 6: Speed (web, `@speed`, reporting):** time from opening `#/guide` to the first screen's title painted; report it, and assert only if under 150 ms on this machine (limit 300 ms).
- [ ] **Step 7: Commit** `feat: the guide's pictures (both apps)`.

### Task 5: Version 1.16, notes, the owner's look, full run

- [ ] Version 1.16 on both apps (versionCode 26); reshoot the Settings shots (they gain the Guide row).
- [ ] `docs/next-update.md`: "Done: 1.16, the guide (both apps)": the owner's decisions, my calls, Android/web differences, the rule table's findings, what only reports (frame timing), untested browsers.
- [ ] README phone checks, each app: a new install opens the guide once; the side pick; Skip; replay from Settings changes nothing; reduce motion; large text; **iPhone Safari and Home Screen** each show it once.
- [ ] Follow-up notes: `vibrateOnTaps` resets on an Android restore (found while planning; not part of this work).
- [ ] Full run, one after another: Gradle (`:domain:allTests --rerun-tasks :domain:checkWebDomain :app:testDebugUnitTest --rerun-tasks`), `npm test`, `npx tsc -b`, `npm run build`, `npx playwright test --trace off`, the speed project 3 times.
- [ ] Commit `chore: version 1.16, notes`.
- [ ] **Stop before the merge.** The owner sees every screen's shots, both sides, light and dark.
