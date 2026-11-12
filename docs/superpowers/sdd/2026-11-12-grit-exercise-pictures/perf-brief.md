# Brief: Exercises tab opens too slowly with thumbnails (web)

Read first: /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/implementer-rules.md and constraints.md in the same folder (use the attribution lines in THIS brief, not constraints.md's). Spec: docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md (sections on the Exercises tab and the @speed check).

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n, BASE = HEAD = 285a275 (Task 5, not pushed). Never push, never switch branches, never amend 285a275: your work is ONE NEW commit on top of it.

## The problem
The e2e check `web/e2e/pictures.spec.ts` "Exercises tab speed" (tag @speed, project "speed" in web/playwright.config.ts) requires the tab to settle within 100 ms, thumbnails drawn, on open / every keystroke / every filter change. Measured by the controller on a quiet machine (Gradle daemons stopped), `cd web && npx playwright test --project=speed --no-deps --trace off e2e/pictures.spec.ts`:
- at c2ddaa6 (10 figures): open 115–121 ms, passed 1 of 3 runs;
- at 285a275 (31 figures): open 139–181 ms, failed 3 of 3.
The release will have 225 figures, so this must scale: the cost must not grow with the number of figures drawn on screen at open.

Likely cause (verify by profiling, don't assume): `web/src/screens/library/ExercisePicture.tsx` `ExerciseThumbnail` draws every tile within 600 px of the screen synchronously in a layout effect on first render, and `FigureSvg.tsx` `drawing()` calls the Kotlin/JS domain `grit.figureShapes` cold (~2 ms per figure — the pose solver) the first time each figure is drawn. Also measure what the tab costs with no thumbnails at all, so you know the budget left for them.

## What done means
- The @speed Exercises-tab check AND the @speed picker checks (web/e2e/library.spec.ts) pass in 3 consecutive quiet runs of `npx playwright test --project=speed --no-deps --trace off` — stop any Gradle/Kotlin daemons first (`cd /home/user/powertracker && ./gradlew --stop`) and don't run other heavy work at the same time. Report all measured times.
- Show it scales: measure the open time with thumbnails for many more figures than exist (e.g. a temporary local-only hack that maps every catalogue row to an existing figure — never committed), and report it. Target: open within 100 ms with 225 figures' worth of rows on screen-and-near.
- Every existing behaviour pinned by the tests still holds: a tile that starts on screen is never seen empty at first paint (if you change this, it must be because a test/spec allows it — read the spec first; if the spec forbids the approach you need, STOP and report NEEDS_CONTEXT with numbers rather than changing the rule), lazy drawing of far rows, theme following, the picker shows no thumbnails, accessibility (tiles hidden from screen readers).
- Do NOT change the @speed tests, their 100 ms threshold, or playwright.config.ts. Do not change domain (Kotlin) figure geometry or the fingerprint golden. Kotlin changes are allowed only if they are pure caching/performance with no output change, and then `:domain:allTests :domain:checkWebDomain` must pass.
- Approaches to consider (pick by evidence): warming the drawing cache in idle time after start-up (chunked, requestIdleCallback with fallback), drawing only the truly-on-screen tiles synchronously and the near-but-off-screen ones in idle time, caching serialised SVG markup, making `figureShapes` cheaper (memoising the solved pose in the domain bridge), or precomputing thumbnail shapes at build time. Prefer the simplest change that meets the target with headroom.
- Unit tests for any new behaviour (vitest, in the existing ExercisePicture.test.tsx style).
- Android: not in scope (it has no speed check); do not touch app/.

## Testing before the commit
Full suites once: `cd /home/user/powertracker && ./gradlew :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest` (Android SDK is installed; only needed if you touched Kotlin — otherwise say you skipped it) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off` (known flake: reload.spec toHaveValue timeout — re-run once). Never `playwright install`.

## Commit
One new commit, title `perf(web): exercises tab thumbnails stay within the speed budget`, short body, ending with exactly:

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01KboHw6FdBYrNDSMx1QWyzy

## Report
Full report to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/perf-report.md: the profile (where the time went, numbers), what you changed and why, before/after times (each quiet run), the 225-scale measurement and how you made it, tests with counts, concerns. Reply ONLY (under 12 lines): Status, commit, times before/after, 225-scale time, test summary, concerns, report path.
