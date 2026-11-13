# Task 13b brief: finish — every exercise has a picture; pictures page

Read first, fully: implementer-rules.md and constraints.md in this folder (use the attribution lines your own harness gives you); the spec docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md (sections 5 Tests and screenshots, 6 Delivery); the plan's "Task 13: Finish" in docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md; task-3-report.md (how the tools `figures:sheet` and `figures:page` were built and how screenshots are recorded in each app).

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n, BASE = HEAD = edec171 (or the commit on top of it if the 13a review adds a fix commit — use the HEAD the controller names when it dispatches you). Never push, never switch branches. ONE new commit on top of BASE.

## What to do
1. Switch on `everyCatalogueExercise_hasFigure` (domain FiguresTest: remove the `@Ignore`) and run it: 225 of 225. If any name is missing, STOP and report which — do not draw new figures in this task.
2. Any other "until every exercise has a figure" leftovers: search the code and tests (domain, app, web, e2e) for comments or branches that exist only because some catalogue exercises had no figure ("not drawn yet", stand-ins, fixtures ordered around missing figures) and bring each up to date, keeping the behaviour for the lifter's own exercises (no figure, empty tile). List each in the report. In particular the web comment in e2e/library.spec.ts about the catalogue growing to about 250 and the stand-in exercises: leave the @speed tests themselves unchanged (no change to their steps, numbers or threshold); if a comment there is now untrue, say so in the report and leave it for the controller.
3. Re-record the spec section 5 screenshots in both apps, dark and light: the guide sheet with a picture for the bench press, the squat and the lat pulldown, and the Exercises tab with thumbnails (Android: `./gradlew :app:testDebugUnitTest -Pscreenshots.update=true`; web: as Task 3/5 did). Look at every screenshot that changed and say what changed in each. If the spec's three guide-sheet screenshots do not exist yet for an app, add them the way the existing screenshot tests are written.
4. Build the owner's pictures page: `cd web && npm run figures:page` → docs/library/pictures.html (check what the script writes and where; docs/library/pictures may already hold earlier output — replace it cleanly, no stale files left). Open it in Chromium (Playwright, headless; never `playwright install`) at phone and desktop widths, dark and light, and check: all 225 figures present, grouped by body area, each with its two panels, captions and thumbnail; no console errors; the page is self-contained (opens from the file with no network). Report its size.
5. Full suites once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off` (known flakes: reload.spec toHaveValue timeout; the JS coach golden's mocha timeout under load — re-run once). Then `./gradlew --stop`. No speed runs (the controller does them).

Do not change any figure's drawing (the fingerprint golden must not change), the @speed tests, or playwright.config.ts.

## Commit
One commit, title `feat: all exercise pictures; pictures page for the owner`, short body, ending with your harness's attribution lines. No model names elsewhere.

## Report
Full report to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/task-13b-report.md: each step's evidence (test names and counts, the list of screenshots changed and what changed, the page's checks and size, leftovers found and what you did), concerns. Reply ONLY (under 12 lines): Status, commit, golden (unchanged?), 225/225, screenshots changed, page path and size, test summary, concerns, report path.
