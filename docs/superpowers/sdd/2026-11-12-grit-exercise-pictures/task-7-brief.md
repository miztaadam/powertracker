# Task 7 brief: back pictures (about 28 new figures)

Read first, fully:
- Rules: /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/implementer-rules.md
- Constraints: /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/constraints.md (use the attribution lines in THIS brief, not constraints.md's)
- Spec (authority): docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md
- Plan, "Tasks 5–12" and "Task 4": docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md
- The pattern to follow, and its lessons: .superpowers/sdd/2026-11-12-grit-exercise-pictures/task-6-report.md, review-6-report.md and task-6-fix1-brief.md (also task-5-report.md, review-5-report.md). Lessons: a pose must read correctly to an experienced lifter, not only pass the checks; where the guide describes a visible difference from a parent exercise (angle, stop point, gap, stance), it must be visible at app scale (check the thumbnail and a 3× crop); choose the camera (side or FRONT) that shows what the guide is about.
- More lessons from the chest batch: (1) in a head-on front view of a body lying or planking away from the camera, the hips and legs are hidden behind the shoulders — never draw them hanging below the torso; (2) a dumbbell held with palms facing in (neutral grip: rows, hammer-style pulls) shows in profile from the side, one held palms-toward-feet shows end-on as a disc — use the orientation the guide's grip gives, for NEW figures only; (3) a cable exercise draws its cable from the pulley to the hand/attachment in both poses; (4) citations: cite the page whose text you actually saw the number on in the search result; where a source is already in domain/.../library/Sources.kt use that exact URL; if no allowed-domain page states the number, write "neutral: <value> drawn" and cite nothing — a wrong citation is worse than none (the chest batch had three).

BASE: 13f8a93 (the reviewed Task 6 commit). Branch ccr-2f9842b5-owq43n. Never push. Never switch branches.

## What to build
A figure for every BACK catalogue exercise that has none yet (catalogue: domain/src/commonMain/kotlin/com/powertracker/domain/library/areas/Back.kt; figures go in domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/areas/Back.kt, which already has Pull-up, Lat pulldown and One-arm DB row — approved, do not change them). Get the exact list from the catalogue (every BodyArea.BACK entry by its canonical name, not aliases); report the count. The plan estimates 31 in the area, so about 28 new.

- Each pose from that exercise's own guide (setup + steps); captions ≤24 characters from the guide's wording. Positions the guide doesn't fix: sourced from the allowed domains only (WebSearch with allowed_domains), cited in a code comment (URL + what it supports), conservative where sources disagree.
- Reuse kit and approved poses where they fit (Pull-up, Lat pulldown, One-arm DB row, Deadlift/RDL hinge poses, the cable station, press-machine seat, benches, Smith machine). Do not change the look of any existing figure. Kit/renderer changes that alter an existing figure's drawing only if needed, listed in the report with the reason; the fingerprint of the existing 61 figures must stay the same unless you list why (prove it as Task 6 did: the golden with the new entries emptied).
- Kit pieces you will likely need (manufacturer-neutral typical size in a comment, each with a KitTest case): seated cable row station with foot plate and attachments (V-handle, straight bar, rope), T-bar / landmine row, chest-supported row bench and machine row with chest pad, pulldown attachments (V-handle, wide/close bars), assisted pull-up machine (reuse the assisted dip one if it fits), back-extension (45° hyperextension) bench, shrug set-ups, and whatever else the list needs.
- Laziness rule (Figures.kt comment): an area's map must not build its figures (map of `Lazy<ExerciseFigure>`); AreaMapsLazyTest must keep passing.
- Pins/spotter only where the guide's safety line asks (constraints.md).
- Key checks per entry: rows → torso angle the guide gives (bent-over vs upright vs chest-supported), where the bar/handle ends (lower chest, ribs, waist — the guide's words), arms straight at the stretch, back flat, contacts (chest on pad, feet on plate, knee/hand on bench); pulldowns and pull-up variants → grip (width and direction: wide/close/neutral/underhand must look different where the camera can show it — consider FRONT for width), full hang at the stretch, chin over bar / bar to upper chest at the end (ChinOverBar exists); pullovers, straight-arm pulldowns → the arc's end points and the fixed elbow bend; shrugs → shoulders up, arms straight; back extensions → hip on pad, body line at the top (no over-arch). Each entry gets a focused test in a new `BatchBackTest.kt` with real bounds. The arm checks for out-of-plane arms are looser since Task 6 (see the Body.kt comment): for any arm with `out != 0`, add an explicit bound in the figure's own test that would catch a wrong-way elbow.

## Testing
- Focused while building: `./gradlew :domain:jvmTest --tests '*BatchBackTest*'` etc.
- Before the commit, the full suites once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off` (known flake: reload.spec toHaveValue timeout — re-run once). The Android SDK is installed and works. Update the fingerprint golden; re-record only screenshots that change (Android: `./gradlew :app:testDebugUnitTest -Pscreenshots.update=true`; web: as Task 5 did), and look at each one that changed.
- Speed: after the full suites, stop Gradle (`./gradlew --stop`) and run `cd web && npx playwright test --project=speed --no-deps --trace off` 3 times on a quiet machine; report each run. Do NOT change the @speed tests, their threshold, playwright.config.ts or the warm-up code; if they fail, report the numbers — don't fix.
- Never run `playwright install`.

## Sheets
`cd web && npm run figures:sheet -- --label back back` (check the area argument name). Check every figure at full size and thumbnail size, dark and light (render one at a time with `--columns 1 "<name>"` for detail), and fix what reads wrong before you hand back.

## Commit
One commit on top of BASE, title `feat(domain): back pictures`, a short body, ending with exactly:

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01KboHw6FdBYrNDSMx1QWyzy

If your own harness gives you different attribution lines for commits you create, use those. No model names elsewhere.

## Report
Full report to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/task-7-report.md (per implementer-rules.md: per figure its sources, camera and judgement calls; tests with counts; RED/GREEN evidence; kit changes; any change to existing figures; screenshots changed; speed times; sheet paths; concerns). Reply ONLY (under 12 lines): Status, commit, golden, count of figures, one-line test summary, speed times, concerns, report path.
