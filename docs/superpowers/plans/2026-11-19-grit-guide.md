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
- **The existing suite stays green with no test edited,** except through the shared fixtures named in Task 3. Existing tests start from empty storage, which is exactly "new user, not seen", so the first-launch gate has a seam (Task 3, Step 1).
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
  - Modify `domain/src/commonMain/kotlin/com/powertracker/domain/coach/ProgressionRule.kt`: name the literals in `change()` (no behaviour change).
  - Modify `domain/src/jsMain/kotlin/com/powertracker/domain/web/GritDomain.kt` and add `JsGuideTypes.kt`; run `:domain:syncWebDomain` and commit `web/src/domain/generated`.
- **Android** (`app/src/main/kotlin/com/powertracker/`)
  - Create `ui/guide/GuideScreen.kt` (shell: pager, controls), `ui/guide/GuidePictures.kt` (one composable per screen), `ui/guide/GuideSampleState.kt` (domain sample → the content components' state).
  - Create `ui/motion/ModeSwitchPaint.kt`: the hammer and pump draw functions, moved out of `ModeSwitchOverlay.kt` unchanged, so screen 2 can draw them.
  - Modify `data/SettingsRepository.kt` (the "seen" key and its accessors; the settings DataStore allows one instance per file, so the key can't live in a second delegate), `ui/AppShell.kt` (route `guide`, first-launch gate), `MainActivity.kt` (turns the gate on), `ui/plan/SettingsScreen.kt` (the Guide row), `data/db/Daos.kt` (two EXISTS queries).
- **Web** (`web/src/`)
  - Create `screens/guide/GuideScreen.tsx`, `GuideScreen.css`, `GuidePictures.tsx`, `guideSampleState.ts`, `guideSeen.ts`.
  - Create `ui/motion/modeSwitchPaint.ts`: the hammer and pump painting, moved out of `ModeSwitchOverlay.tsx` unchanged.
  - Modify `playwright.config.ts` (the shared fixture, Task 3), `router.tsx` (route `#/guide`), `App.tsx` (lazy route, first-launch gate, install card order), `screens/settings/SettingsScreen.tsx` (the Guide row).

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

data class GuideTargetStep(val date: LocalDate, val exerciseId: String, val doneWeightKg: Double, val doneReps: Int, val doneRpe: Double,
    val nextWeightKg: Double, val nextRepsLow: Int, val nextRepsHigh: Int, val note: String?)
data class GuideProgress(val points: List<Pair<LocalDate, Double>>,        // the chart, oldest first
    val bests: List<Pair<String, Double>>,                                 // exercise id to best, kg
    val totalKg: Double?,                                                  // powerlifting only
    val laggingMuscle: String?)                                            // bodybuilding only, from a check-in

object GuideSample {
    val TODAY: LocalDate                                    // a fixed date, so every run draws the same
    fun template(mode: TrainingMode): PlanTemplate          // the 3-day template screen 3 picks
    fun plan(mode: TrainingMode): List<PlanItem>            // that template's draft, saved
    fun today(mode: TrainingMode): GuideTargetStep          // the set screen 4 logs: equal to targets(mode).first()
    fun targets(mode: TrainingMode): List<GuideTargetStep>  // one lift, 5 sessions: what was done, the coach's next target
    fun advice(mode: TrainingMode): List<Advice>            // screen 6's cards, in the rule table's order, from the real rules
    fun progress(mode: TrainingMode): GuideProgress         // about 6 months
}

// The timelines: what each picture shows at each step. The apps draw these; they don't invent steps.
enum class GuideStep {                                      // in order within each screen
    WELCOME_MARK, WELCOME_SIDES,
    SIDE_LOOP,
    PLAN_EMPTY, PLAN_TEMPLATES, PLAN_DRAFT, PLAN_SWAP, PLAN_FILLED,
    TODAY_EMPTY, TODAY_WEIGHT, TODAY_REPS, TODAY_RPE, TODAY_TICKED, TODAY_SKIP,
    TARGETS_SESSION,                                        // repeated once per targets(mode) entry
    HOLDS_CARD,                                             // repeated once per advice(mode) entry
    MONTHS_CHART, MONTHS_BESTS, MONTHS_ENDING,
    TURN_BUTTONS,
}
// in Guide:  fun steps(id: GuideScreenId, mode: TrainingMode): List<GuideStep>
```

JS exports on `GritDomain`: `guideScreens()`, `guideScreen(id, mode)`, `guideSteps(id, mode)`, `guideStepMs()`, `guideSampleToday()`, `guideSampleTemplate(mode)`, `guideSamplePlan(mode)`, `guideSampleTodaySet(mode)`, `guideSampleTargets(mode)`, `guideSampleAdvice(mode)`, `guideSampleProgress(mode)`, with plain `Js*` mirrors in `JsGuideTypes.kt`.

- [ ] **Step 1: Write the per-side rule table** as a comment block at the top of `Guide.kt`, by reading the callers (`Coach.kt`, `BodybuildingCoach.kt`, `PhysiqueCoach.kt`, `SkipCoach.kt`, `FatigueRule.kt`, `StallRule.kt`, `WeakPointRule.kt`). Columns: rule, code path, which side's coach runs it, constant(s), the guide's sentence. Rules: progression (powerlifting `ProgressionRule`, +2.5 kg on target, capped by `MAX_INCREASE`; bodybuilding reps first then `BodybuildingCoach.INCREASE`), deload (`FatigueRule`, `AdvicePayload.Deload`, `Coach.DELOAD_COOLDOWN_DAYS`), easing back (`Coach.GAP_DAYS`, `ReEntry`), the pain hold (`SkipCoach.held`: until that exercise is next logged), swap suggestions (`PhysiqueCoach.STUCK_DAYS`, bodybuilding only), check-ins (`PhysiqueCoach.CHECK_IN_DAYS`, bodybuilding only), stall and weak point (powerlifting only). A rule a side's coach doesn't run gets no sentence on that side.
- [ ] **Step 1b: Name `ProgressionRule.change()`'s literals** as `internal const val`s, with no behaviour change: `FAILED_SCALE = 0.95`, `HARD_SCALE = 0.975`, `STEP_KG = 2.5`, `BIG_STEP_KG = 5.0`; make `MAX_INCREASE` `internal`. The existing `ProgressionRule` tests pass unchanged.
- [ ] **Step 2: Write the failing tests** in `GuideTest.kt`:
  - `everyScreenOnBothSides_hasATitleAndUnderFortyWords`
  - `everyNumberInTheCopy_comesFromItsDeclaredConstant`: for each `GuideLine`, every number in `text` equals the value of one of `constants`, resolved through a test map of name → the real constant (e.g. `"Coach.GAP_DAYS" to Coach.GAP_DAYS`). Changing a constant must change the copy or fail this test. The only conversions allowed between a constant and its number in the text: a ratio to a whole percent (`1.05` → "5%", `0.02` → "2%", `0.6` → "60%"), days as written, kilograms as written (`2.5`), and counts as written. The test's expected numbers are written by hand per line, not derived with the copy's own formula.
  - `bodybuildingOnlyRules_areAbsentOnPowerlifting_andTheReverse`: screen 6 and 7 on powerlifting mention no swap or check-in; on bodybuilding no stall or weak point.
  - `screen7_onPowerlifting_endsOnBestsAndTotal_notACheckIn`
  - `noLineNamesASource`: no line contains any `CoachSources` publisher name.
- [ ] **Step 3: Write the failing tests** in `GuideSampleTest.kt`:
  - `targets_areWhatTheRealCoachGives`: replay `GuideSample.targets(mode)` through `Coach` / `BodybuildingCoach` and assert each step's target equals the coach's output for the history before it.
  - `plan_isAPlanTemplatesDraft` for each side.
  - `today_isTheFirstTargetStep`, and `advice_isWhatTheRealRulesGive`: each card in `advice(mode)` is produced by running that rule on a small history built for it, and only that side's rules appear.
  - `steps_coverEveryScreen_andRepeatOncePerSampleEntry` for each side.
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
- [ ] **Step 4: Implement screen 4's picture** with the real `TodayContent` fed `GuideSampleState` states, scaled to fit the frame, with no-op actions. One state per `Guide.steps(TODAY, mode)` entry, built from `GuideSample.today(mode)`; the clock advances every `Guide.STEP_MS`. If `TodayContent` can't be driven this way cleanly on either app, stop and report before drawing a mock-up: the plan's architecture depends on it.
- [ ] **Step 5: Web: make the guide a lazy chunk** (`React.lazy`), route `#/guide`. e2e `guide.spec.ts`: `theGuidesChunk_isNotRequestedBeforeItOpens` (record requests on load of `#/`, then open `#/guide`). In this task nothing opens the guide by itself yet, so no fixture is needed; Task 3 adds the gate and its seam.
- [ ] **Step 6: Screenshots:** screen 4 at its last step, dark and light, both sides, both apps (`guide-today-*`), through the existing `ScreenshotTest.render` and the web `shoot` helper.
- [ ] **Step 7: Run** the Android unit tests, `npm test`, `npx tsc -b`, `npx playwright test e2e/guide.spec.ts --trace off`. Commit `feat: the guide's shell and its Today picture (both apps)`.
- [ ] **Step 8: Stop. The owner sees the screen-4 shots** (and a short screen recording or frame strip of the steps) before the other pictures are built.

### Task 2b: The Today picture moves, then the owner's clip

The owner asked for smoother motion than a slideshow (2026-11-19). An inventory of the real screens found:
- **Android already animates from state** (`LayoutMotion.kt`, `Charts.kt`): a logged set's block slides open, a skipped exercise fades out and the rest close up, the current card advances with `SlidingSwap`, coach cards fade and slide, charts draw in. Numbers, the tick and the "Skipped today" line are instant.
- **The web animates only from actions** (view transitions started by its hooks), so a picture fed props shows no motion at all.
- Neither app can be changed inside its real screens (spec non-goal), and the guide may not use View Transitions.

**So the guide's motion has three parts, the same on every picture:**
1. **Inherited (Android):** the picture keeps one mounted instance, so the real screen's own motion plays. Nothing to build, but nothing may break it.
2. **Fed (both apps):** during a step's move, the guide feeds in-between sample values the app could really show: the weight in loadable steps, whole reps, RPE in halves, chart points one at a time.
3. **The guide's own layout motion (web):** a small FLIP layer in the guide. Before a step change it records the boxes of the picture's rows; after the render it moves them from the old box to the new with the Web Animations API (transform and opacity only), and fades in what is new. One layout read per step change, none per frame. This gives the web the slides Android inherits.

**Files:** domain `guide/Guide.kt`, `GuideSample.kt` (+ tests, JS bridge, `syncWebDomain`); Android `ui/guide/GuideScreen.kt`, `GuidePictures.kt`; web `screens/guide/GuideScreen.tsx`, `GuidePictures.tsx`, new `screens/guide/guideFlip.ts`.

**Interfaces — Produces:**

```kotlin
// in Guide: one definition of time, as ModeSwitchMotion.dropFrame is for the switch
const val MOVE_MS: Int = 320            // a step's move; then it holds for STEP_MS
data class GuideFrame(val stepIndex: Int, val step: GuideStep, val progress: Double, val done: Boolean)  // progress 0..1 eased within the move; 1 while holding
fun frame(id: GuideScreenId, mode: TrainingMode, tMs: Int): GuideFrame   // each step lasts MOVE_MS + STEP_MS; the last step holds for good (done)
// in GuideSample: what the Today editor shows part-way through a move
data class GuideEditor(val weightKg: Double, val reps: Int, val rpe: Double)
fun todayEditor(mode: TrainingMode, step: GuideStep, progress: Double): GuideEditor  // snaps to values the app allows; equals the step's end value at progress 1
```

`GuideClock` on both apps now gives elapsed milliseconds (frame-driven while a move runs, idle while a step holds); the apps call `Guide.frame`. They invent no durations: web FLIP moves take `MOVE_MS`; Android keeps its own springs for inherited motion.

- [ ] **Step 1: Failing domain tests:** `frame_stepsAndProgress_overTime` (boundaries, the last step holds, `done`); `todayEditor_onlyShowsValuesTheAppAllows` (weight on the 2.5 kg grid, whole reps, RPE in halves, monotonic from the step's start to its end); `todayEditor_atProgressOne_equalsTheStepsEndState`.
- [ ] **Step 2: Failing app tests** (both apps, held clock):
  - `guide_today_numbersRollThroughInBetweenValues`: mid-move the editor shows a value strictly between the step's start and end.
  - `guide_whileAStepHolds_nothingRenders`: no recomposition / no React render of the picture between the end of a move and the next step.
  - `guide_replayOrPageReturn_restartsWithoutPlayingBackwards`: returning to the page shows step 1 at once, with no reverse animation (Android: key the picture on a run counter).
  - `guide_clockStops_whenHiddenOrUnmounted` (Android lifecycle `STOPPED`; web `visibilitychange`), and no timers after unmount.
  - `guide_reduceMotion_showsTheLastStepStill` stays as it is.
  - Web: `guideFlip_movesARowFromItsOldBoxToItsNew` (unit, with stubbed boxes); `guideFlip_readsLayoutOncePerStepChange_neverPerFrame` (held-clock e2e counting `getBoundingClientRect`); `guideFlip_usesOnlyTransformAndOpacity`; e2e `guide_today_skippedRow_slidesAndTheRestCloseUp` asserting a running animation on the rows after the skip step (via `getAnimations()`).
  - Android: `guide_today_inheritsTheScreensOwnMotion`: with motion on, after the log step the set block is mid-slide on the next frame (the real screen's `SlidingBlock`), proving the instance wasn't rebuilt.
- [ ] **Step 3: Implement,** domain first (`syncWebDomain`), then the apps.
- [ ] **Step 4: Shots stay at settled states.** Record for the owner: a web video of screen 4 on each side (Playwright `recordVideo`, motion on, real time), and an Android frame strip (every 80 ms across the log and skip steps) from the held clock.
- [ ] **Step 5: Run** the domain, app and web suites and the full e2e. Commit `feat: the guide's Today picture moves (both apps)`.
- [ ] **Step 6: Stop. The owner sees the clip** before the other seven pictures are built.

**Not built without the owner's say** (each needs a real screen changed, so it would also change the real app): the tick drawing in; the "Skipped today" line sliding in on Android; a tap ring showing where a finger would press (Android gives the guide no way to find a control inside the picture).

### Task 2c: The three extras, then the owner's clip

The owner approved all three from a looping sketch (spec, "Amended while planning" point 8). This task runs before Task 3. Its new clip, not Task 2b's, is what the owner approves before Task 4.

**Files:**
- Modify (Android, real screen, these effects only): the Today set row's tick and the "Skipped today" block under `app/src/main/kotlin/com/powertracker/ui/today/`; `ui/guide/GuidePictures.kt`, `GuideScreen.kt`.
- Modify (web, guide only): `web/src/screens/guide/guideFlip.ts`, `GuidePictures.tsx`, `GuideScreen.css`. No file under `web/src/screens/today/` changes.
- Modify (domain): `Guide.kt` only if a step needs a "tap" target; `JsGuideTypes.kt` to match.
- Test: beside each file changed, plus `web/e2e/guide.spec.ts`.

**Values (from the sketch the owner saw; keep them):**
- **Tick:** the circle scales 0.4 → 1.12 → 1 while fading in, and the check's line is drawn start to end. Whole effect within `MOVE_MS`.
- **Ring:** one ring, 2 px line, centred on the button, growing about 4× while fading from 0.9 to 0, over `FADE_MS`…`MOVE_MS`, ending as the result appears. On screen 4 it is on "Log set" only (what the owner saw). Skip gets no ring unless the builder can anchor it on the row's menu button the same way; say which in the report.
- **Skipped line:** rises 14 px while fading in.

**Rules:**
- **On the event only.** On the real Android screen the tick draws only for a set logged now. It does not replay for sets already logged when the screen opens, on restore from the saved state, when a row scrolls back into view, or on a mode switch. The skipped line does not move when the screen opens with skips already there. One test per case.
- **Motion off.** The app's motion setting and the system's reduced motion turn all three off, on the real screen and in the guide. Tested on both.
- **The guide's clock.** In the guide each effect is driven by the guide's time, so a held clock holds it and a replay replays it. On Android, if the real screen's tick runs on its own animation clock, the frame strip must still show it mid-draw.
- **Web's paint rule, amended for the tick only:** the check's line may animate `stroke-dashoffset`; everything else stays transform and opacity. The ring and the rise are transform and opacity.
- **Ring anchor.** First try to find the button without touching the real screen (web: a query inside the phone frame at the step change, one read; Android: an existing test tag or semantics). Only if that fails, add a marker to the real screen that draws nothing.
- **At rest nothing changes.** The committed real-Today shots (Android and web) must be byte-for-byte unchanged, or the report says why not.

- [ ] **Step 1:** Failing tests for the rules above (event-only cases, motion off, held clock, at-rest shots).
- [ ] **Step 2:** Android: the tick and the skipped line on the real screen; the ring in the guide.
- [ ] **Step 3:** Web: the tick, the ring and the rise in the guide's own layer.
- [ ] **Step 4:** Full suites, not only the guide's (real screens changed): Gradle `:domain:allTests :domain:checkWebDomain :app:testDebugUnitTest`; web `npm test`, `npx tsc -b`, `npm run build`, `npx playwright test --trace off`. Watch the known `SkipTodayScreenTest` flake: rerun it alone before calling it one.
- [ ] **Step 5:** Reshoot the `guide-today-*` shots; record the two web videos and the Android frame strip again into `scratchpad/guide-t2d/`. Commit, push if green.
- [ ] **Step 6:** Stop. The owner approves this clip before Task 4. Task 4's pictures reuse the ring wherever a step stands for a tap.

Task 5's notes gain a line: the real Today screen on Android now draws its tick and slides its "Skipped today" line.

### Task 3: Opening it: first launch, "seen", the side pick, screen 8, the Settings row

**Files:** Android `data/GuideSeen.kt`, `data/db/Daos.kt`, `ui/AppShell.kt`, `ui/plan/SettingsScreen.kt`; web `screens/guide/guideSeen.ts`, `App.tsx`, `screens/settings/SettingsScreen.tsx`, `screens/install/InstallCard.tsx` (order only). Tests beside each, plus `web/e2e/guide.spec.ts`.

**Interfaces — Produces:**

```kotlin
// in SettingsRepository (key "guide_seen" in the same store, not a field of AppSettings, not in backups):
val guideSeen: Flow<Boolean>
suspend fun markGuideSeen()

// the seam: the gate is off unless asked for. MainActivity passes true; every existing test keeps the default.
@Composable fun AppShell(container: AppContainer, guideOnFirstLaunch: Boolean = false)

suspend fun isNewUser(sessions: SessionDao, plans: PlanDao): Boolean   // no sessions and no plan items on either side
// Daos: @Query("SELECT EXISTS(SELECT 1 FROM sessions)") suspend fun any(): Boolean  (and the same for plan items; use the tables' real names)
```

```ts
export const GUIDE_SEEN_KEY = 'guide:seen'                       // in `meta`
export function guideSeen(db: GritDb): Promise<boolean>
export function markGuideSeen(db: GritDb): Promise<void>
export function isNewUser(db: GritDb): Promise<boolean>          // sessions.count() === 0 && planItems.count() === 0
export const GUIDE_KEY = 'grit:guide'                            // localStorage; 'off' keeps the first-launch gate shut (the seam, like MOTION_KEY)
```

**The web seam:** `playwright.config.ts`'s shared `storageState` gains `{ name: 'grit:guide', value: 'off' }` beside `grit:motion`. The guide's own first-launch tests override it with `test.use`. Vitest tests that render `App` set the same key in the shared setup file. No existing test file is edited.

**The two conditions** (keep them separate):
- `firstLaunch` = the guide was opened by the first-launch gate (`!seen && isNewUser`). Only then does screen 2 show the two side buttons, and only then is "seen" written, when the guide closes by any exit.
- `hasPlan` = the side now on has plan items. Screen 8 shows **Choose a template** (and on bodybuilding **Build my plan** below it) when `!hasPlan`; otherwise **Done** and **See my plan**.

- [ ] **Step 1: Add the seam first** (the default-off `guideOnFirstLaunch` parameter; the `grit:guide` key in the shared Playwright state and the vitest setup), with the gate itself still unbuilt. Run the whole existing suite: green, with no existing test edited.
- [ ] **Step 1b: Write failing tests** (both apps):
  - `firstLaunch_withNoData_opensTheGuideOnce`: open, close, restart: it doesn't open again.
  - `firstLaunch_withASessionOrAPlanItemOnEitherSide_neverOpensIt` (four cases).
  - `seenIsReadFirst_soASeenDeviceRunsNoDataQuery` (Android: the DAO is not called when seen).
  - `screen2_onFirstLaunch_setsTheRealSide_withNoAnimationSoundOrVibration`: `setTrainingMode` is called with the pick; the mode-switch controller's `start` / `flip` is never called.
  - `screen2_pickingTheSideAlreadyOn_writesNothing`.
  - `pickingBodybuildingOnFirstLaunch_thenFirstVisitingPowerlifting_showsNoWelcomeBackOrEasing`: the pick stamps `powerliftingLeftOn`, as the Settings switch does; a side with no sessions must not greet the lifter as returning. If it does, stop and report: that would be an existing behaviour of the Settings switch too.
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
- [ ] **Step 3: Implement.** Android: gate in `AppShell` before the NavHost's first frame settles (read `seen`, then `isNewUser`), route `guide`, exits mapped to `Routes.templates(mode)`, `PLAN_BUILDER`, Plan, or back. Web: the same in `App.tsx`; the install card renders only when the guide is not open. (The web export doesn't write `meta`, and the Android backup holds only `BackupSettings`; the two backup tests pin that.)
- [ ] **Step 4: Run** the Android unit tests, `npm test`, `npx tsc -b`, `npx playwright test e2e/guide.spec.ts --trace off` — pass. Commit `feat: the guide opens on a new install and from Settings (both apps)`.

### Task 4: The other seven pictures

**Files:** `GuidePictures.kt`, `GuideSampleState.kt`; `GuidePictures.tsx`, `guideSampleState.ts`; tests and shots as in Task 2.

**Interfaces — Consumes:** Task 1's sample data, Task 2's `GuideClock` and frame.

Every picture moves as Task 2b defines: one mounted instance (Android inherits the real screen's motion), fed in-between values, and the web's FLIP layer. Per picture: screen 3's rows slide in as the week fills; screen 5's weight and reps roll between sessions; screen 6's cards fade and slide from one to the next; screen 7's chart draws in (remount it under a new key so the real draw-in plays) and its bests count up through fed values. Add `GuideSample` in-between functions as needed, with the same "only values the app allows" test.

Pictures, each a short list of steps on the clock:
1. **WELCOME:** the app's mark and the two sides' colours; no numbers.
2. **SIDE:** a small box drawing `ModeSwitchMotion.dropFrame` and `pumpFrame` in a loop (cut down: the hammer's fall and impact; the pump's three beats), side by side, silent. It uses the paint functions moved into `ModeSwitchPaint.kt` / `modeSwitchPaint.ts`. That move changes no behaviour: the 1.15 tests, held-clock pins and mode-switch shots pass unchanged. On first launch the two buttons sit under it.
3. **PLAN:** the empty `PlanContent`; then the real `TemplatesContent(state, actions)` showing the list with its days-a-week lines, the chosen template's draft, and one Swap; then `PlanContent` with `GuideSample.plan`.
5. **TARGETS:** one lift's target card stepping through `GuideSample.targets` (on bodybuilding: reps climb, then the weight).
6. **HOLDS_BACK:** `CoachContent` advice cards for that side's rules, one per step, from the rule table.
7. **MONTHS_LATER:** `ProgressContent` with `GuideSample.progress`: the chart drawing in, the bests counting up; on bodybuilding a check-in naming the lagging muscle, on powerlifting the total.
8. **YOUR_TURN:** the Plan tab's empty state or the lifter's side colours, with the buttons.

- [ ] **Step 1: Write failing tests** (both apps): per screen, `guide_<screen>_firstAndLastStep` on a held clock for each side; `guide_screen2_callsNoControllerSoundOrVibration`; `guide_screen6_atTwiceTheFontSize_cutsNothing_andKeepsTheControls` (Review Focus 5); `guide_rotationOrResize_keepsTheScreen` (Review Focus 3); web held-clock pin `guide_noLayoutReadsPerStep` (count `getBoundingClientRect` and `offset*` reads across steps: zero after the first frame).
- [ ] **Step 2: Run them — they fail. Step 3: Implement. Step 4: Run — pass.** If any content component can't be driven with sample state cleanly, or the paint move can't be done without changing the overlay's behaviour, stop and report before drawing a mock-up.
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
