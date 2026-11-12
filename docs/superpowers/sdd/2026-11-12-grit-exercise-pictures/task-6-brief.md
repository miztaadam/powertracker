# Task 6 brief: chest pictures (about 30 figures)

Read first, fully:
- Rules: /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/implementer-rules.md
- Constraints: /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/constraints.md (use the attribution lines in THIS brief, not constraints.md's)
- Spec (authority): docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md
- Plan, "Tasks 5–12" and "Task 4": docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md
- The pattern to follow, and its lessons: .superpowers/sdd/2026-11-12-grit-exercise-pictures/task-5-report.md and review-5-report.md (also review-4-report.md). Lessons: a pose must read correctly to an experienced lifter, not only pass the checks; where the guide describes a visible difference from a parent exercise (angle, stop point, gap, stance), it must be visible at app scale (check the thumbnail and a 3× crop); choose the camera (side or FRONT) that shows what the guide is about.

BASE: e6cdd73. Branch ccr-2f9842b5-owq43n. Never push. Never switch branches.

## What to build
A figure for every CHEST catalogue exercise (catalogue: domain/src/commonMain/kotlin/com/powertracker/domain/library/areas/Chest.kt; figures go in domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/areas/Chest.kt). Get the exact list from the catalogue (every BodyArea.CHEST entry by its canonical name, not aliases); report the count. The plan estimates 30.

- Each pose from that exercise's own guide (setup + steps); captions ≤24 characters from the guide's wording. Positions the guide doesn't fix: sourced from the allowed domains only (WebSearch with allowed_domains), cited in a code comment (URL + what it supports), conservative where sources disagree.
- Reuse kit and approved poses where they fit (Bench, DB shoulder press's `Arm.out`, the adjustable bench from Task 4). Do not change the look of any existing figure. Kit/renderer changes that alter an existing figure's drawing only if needed, listed in the report with the reason; the fingerprint of the existing 31 figures must stay the same unless you list why.
- Kit pieces you will likely need (manufacturer-neutral typical size in a comment, each with a KitTest case): incline and decline bench (with leg brace), Smith machine, cable stack with handles and adjustable pulleys (a CableStack exists — extend), chest press / incline / decline press machines, pec deck, dip bars, and whatever else the list needs.
- Laziness rule (Figures.kt comment): an area's map must not build its figures — `internal val chestFigures: Map<String, Lazy<ExerciseFigure>>`. Also add the test the last review asked for: a test that building the real area maps (all areas, via Figures.all's keys) solves no pose — e.g. check every area map's `Lazy.isInitialized()` is false before any lookup, in a fresh test that does not share state (if test-order state makes that impossible on one JVM, find another clean way, e.g. counting pose solves through a hook; explain).
- Pins/spotter only where the guide's safety line asks (constraints.md).
- Key checks per entry: presses → the guide's stop point at START/END (BarTouches or the dumbbell/handle position), lockout over the shoulders where the guide says so, contacts with bench/pad/seat; flys → the arc's end points (level with shoulders/chest at the bottom, elbows' fixed bend), contacts; dips/push-ups → the guide's depth and lockout; machines → seated/back on pad, handles where the guide says. Each entry gets a focused test in a new `BatchChestTest.kt` with real bounds.

## Testing
- Focused while building: `./gradlew :domain:jvmTest --tests '*BatchChestTest*'` etc.
- Before the commit, the full suites once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off` (known flake: reload.spec toHaveValue timeout — re-run once). The Android SDK is installed and works. Update the fingerprint golden; re-record only screenshots that change (Android: `./gradlew :app:testDebugUnitTest -Pscreenshots.update=true`; web: as Task 5 did), and look at each one that changed.
- Speed: after the full suites, stop Gradle (`./gradlew --stop`) and run `cd web && npx playwright test --project=speed --no-deps --trace off` 3 times on a quiet machine; report each run. Do NOT change the @speed tests, their threshold, playwright.config.ts or the warm-up code; if they fail, report the numbers — don't fix.
- Never run `playwright install`.

## Sheets
`cd web && npm run figures:sheet -- --label chest chest` (check the area argument name). Check every figure at full size and thumbnail size, dark and light (render one at a time with `--columns 1 "<name>"` for detail), and fix what reads wrong before you hand back.

## Commit
One commit on top of BASE, title `feat(domain): chest pictures`, a short body, ending with exactly:

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01KboHw6FdBYrNDSMx1QWyzy

No model names elsewhere.

## Report
Full report to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/task-6-report.md (per implementer-rules.md: per figure its sources, camera and judgement calls; tests with counts; RED/GREEN evidence; kit changes; any change to existing figures; screenshots changed; speed times; sheet paths; concerns). Reply ONLY (under 12 lines): Status, commit, golden, count of figures, one-line test summary, speed times, concerns, report path.
