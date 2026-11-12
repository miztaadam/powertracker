# Task 13a brief: thumbnails worked out at build time (web)

Read first, fully: implementer-rules.md and constraints.md in this folder (use the attribution lines your own harness gives you); the spec docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md sections 3.2 (Exercises tab thumbnail) and 3.4 (Speed); perf-brief.md, perf-report.md and review-perf-report.md in this folder (how the idle warm-up came about); and in progress.md the entries "Speed finding" and "T13 speed design input" (the measurements and Ruling R15 behind this task).

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n, BASE = HEAD = __BASE__ (all 225 figures drawn). Never push, never switch branches. ONE new commit on top of BASE.

## The problem
The Exercises tab shows a thumbnail on every row. Working a figure's pose out (the Kotlin/JS domain's solver, `grit.figureShapes`) costs about 3 ms each on this machine and several times that on a phone. Today `prepareThumbnails()` (web/src/screens/library/ExercisePicture.tsx, started from main.tsx) works every catalogue thumbnail out in idle time after every start-up. With 225 figures that is about 0.7 s of work here at each launch, more on a phone, and it competes with the first taps: the @speed picker check "a few hundred exercises" opens at 41–81 ms straight after load against 39–63 ms after the warm-up has finished, and fails the 100 ms gate about 1 run in 10 (102–113 ms). A tab opened before the warm-up reaches its rows builds them there and then (78–131 ms measured cold).

## The design (decided: Ruling R15)
Work the thumbnails out once, when the app is built, and ship them as data:
- A Vite plugin (in web/, used by `vite build`, `vite dev` and vitest alike) provides a virtual module — for example `virtual:figure-thumbnails` — that holds, for every catalogue exercise with a figure, its thumbnail pose's shapes and view box. The plugin builds them by loading the generated domain bundle the way web/scripts/figure-sheet.mjs `openFigures()` does (or more directly, if importing web/src/domain/generated in Node works without Vite) and calling `grit.figureInfo` / `grit.figureShapes` for each `grit.library()` entry. Nothing generated is committed: the data always matches the domain bundle in the tree.
- Keep the data compact: the controller measured 145 thumbnails as naive JSON at 309 KB raw / 69 KB gzip (4467 shapes: 3534 capsules, 421 circles, 163 rects, 349 polygons — the polygons, mostly plate rings, are most of the bytes). Aim well under that per figure: coordinates to 0.1, roles as small indexes, tuples not objects, and whatever else is cheap. Report raw and gzip size at 225.
- `ExerciseThumbnail` draws from that data: no pose solving for a thumbnail, at any time. `prepareThumbnails()` and its idle warm-up go away (and its call in main.tsx, its tests, and any helper only it used). The guide sheet's picture (`ExercisePicture`, two full poses of one exercise) still asks the domain on demand, as now.
- How the data loads is yours to choose by measurement: in the main bundle, or a separate chunk fetched at start-up. Either way a tile that starts on screen must never be seen empty at first paint when the tab opens (existing behaviour, pinned by tests — keep them passing; if a lazy chunk cannot guarantee it, put the data in the main bundle). Parse cost at start-up must be small: measure it (e.g. `JSON.parse` of a string literal is the fast way to ship large data) and report.
- Everything else stays: only tiles on or near the screen are mounted with their drawing (the IntersectionObserver logic) unless you can show with numbers that it is no longer needed and removing it is simpler and still within the gate; theme following (role classes, not baked colours); the picker shows no thumbnails; tiles hidden from screen readers; `grit.hasFigure` for the empty-tile decision (or the data's own key set — say which and why).
- The drawn thumbnail must be the same picture as before: add a test that, for every catalogue figure, the shapes decoded from the build-time data equal what `grit.figureShapes(name, thumbnailPose)` returns (to the rounding you chose; state the tolerance and why it cannot be seen at 56×40 or on the contact sheets).
- scripts/figure-sheet.mjs and scripts/pictures-page.mjs must keep working (they render through FigureSvg; make sure the virtual module resolves there too, or that they do not need it).
- Do NOT change the @speed tests, their 100 ms threshold or playwright.config.ts. Do not change Kotlin. Do not change app/.

## What done means
- All four @speed checks pass in 5 consecutive runs on a quiet machine: first `cd /home/user/powertracker && ./gradlew --stop; pkill -f kotlin-daemon`, wait about a minute, then `cd web && npx playwright test --project=speed --no-deps --trace off`, five times. Report each run.
- Report measured times before and after for: the picker "few hundred" opening straight after load (use a temporary, uncommitted spec as the controller did: copy `settle` and `editExercises` from e2e/library.spec.ts, open the picker right after load, log the ms, `--repeat-each 10`; delete it afterwards), and the Exercises tab opening straight after load.
- Report the build's size change (main bundle and total precache, raw and gzip) and the start-up parse cost.
- The web test `exercisesTab_coldList_buildsOnlyTheNearTilesFigures` (ExercisePicture.test.tsx; its fixture was reordered in Task 7 and it was flagged for a rethink) is rewritten or replaced to pin what is now true.
- Unit tests for the new behaviour in the existing style; no test left that pins the removed warm-up.

## Testing before the commit
`cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off` (known flake: reload.spec toHaveValue timeout — re-run once). Kotlin is untouched, so no Gradle run is needed; say you skipped it. Never `playwright install`.

## Commit
One commit, title `perf(web): thumbnails are worked out when the app is built`, short body, ending with your harness's attribution lines. No model names elsewhere.

## Report
Full report to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/task-13a-report.md: what you built and why each choice, the data format and sizes, before/after times (every run), tests with counts, RED/GREEN, concerns. If the design cannot meet the gate or the size is unreasonable, STOP and report NEEDS_CONTEXT with numbers — do not fall back to another design on your own. Reply ONLY (under 12 lines): Status, commit, sizes, times before/after, speed runs, test summary, concerns, report path.
