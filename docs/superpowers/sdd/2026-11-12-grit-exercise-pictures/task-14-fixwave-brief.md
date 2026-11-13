# Final fix wave brief

Read first: implementer-rules.md and constraints.md in this folder (use the attribution lines your own harness gives you); review-final-report.md (Minor 1 and Minor 3); task-12-report.md (how the calf raises were built); task-10-report.md "Fix round 1" (the split squats' shorter stance); task-13a-report.md (the thumbnails plugin).

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n, BASE = HEAD = 186b7ae (a docs-only commit on top of 32ea347). Everything is pushed: make NEW commits, never amend. Never push, never switch branches. If you are cut off and later resumed, check `git status` first and carry on from the working tree.

## Fix A (the owner's own finding): Smith calf raise — the upper body reads as facing left while the feet point right
The owner looked at the Smith calf raise and said the man's feet and body are the wrong way round, and confirmed the reading: his feet point right, but the bar and the raised arm sit on his left, so the upper body looks as if it faces left (as if he held the bar in front of his chest). Render it and see: `cd web && node scripts/figure-sheet.mjs --label fw --columns 2 "Smith calf raise" "Barbell calf raise"`.
What is drawn today (figures/areas/Calves.kt, `smithCalfRaise` and `barbellCalfRaise`, both via `barArm` = `armTo(shoulderOf(pelvis, 90.0), barOnBack(pelvis, 90.0), 90.0, turnedOut = true)`): the torso is exactly upright, the bar's disc sits behind the body with its centre behind the back's edge, the elbow is in FRONT of the torso and the forearm slants up and BACK across the body to the bar, and the head sits straight above the torso. Nothing says which way he faces except the feet.
Make both calf raises read, at full size and at thumbnail size, as a lifter facing the way his feet point with the bar across his upper back:
- the bar rests ON the upper back / top of the shoulders (the disc's centre at the back of the neck and shoulders, not floating behind the back);
- the hands hold the bar just outside the shoulders with the elbows pointing DOWN and slightly BACK, under or just behind the bar — the forearms run up to the bar from below/behind, never across the front of the chest;
- the head is clearly in front of the bar (face forward, toward the toes' side), and a small natural forward lean of the torso is fine if the guide allows it (check the guide text; keep "stand tall" true);
- the Smith rail still runs through the bar in both panels (BarOnRails), the stops under the bar, feet and block as they are.
Add test bounds that pin the facing: elbow x at or behind the shoulder's x (toward the heels), grip above the elbow, bar centre within a few cm of the upper-back point, head centre in front of the bar centre. They must fail on today's pose (RED) and pass after.
Do NOT change any other figure's drawing in this fix. But the same arm is used by other figures where a lifter stands upright with a bar on the back (for example the START of Smith squat, Barbell lunge, Barbell reverse lunge, Good morning, Squat and its variants): list every figure/panel that uses an upright (torso within 10° of vertical) bar-on-back hold, render them side by side at thumbnail size in one sheet (`--label fwback`), and say in the report which of them have the same facing problem. The controller will take that list to the owner; do not change them.

## Fix B (review-final Minor 1): Split squat and Smith split squat take 140–240 ms to build
Every other figure builds in about 1 ms on the JVM; these two take 140–190 ms on the JVM and about 240 ms in Node, which is felt when their guide opens (web) or their row scrolls in (Android, main thread). Find what is slow (the stance/pose search added in Task 10's fix round is the likely place) and make them build in a few ms without changing what is drawn: the fingerprint golden must not change because of this fix (if it must change by rounding, stop and report). Add a test that every catalogue figure builds within a generous bound (for example 50 ms each on the JVM after a warm-up of ten others, written so it cannot flake on a loaded machine: take the best of three builds per figure) so a slow figure cannot slip in again.

## Fix C (review-13a Minor, review-final Minor 3): dev server and the thumbnail data
While `vite dev` runs, a re-synced domain bundle (`:domain:syncWebDomain`) must give new thumbnail data without restarting the server: invalidate the virtual module when the bundle file changes (the plugin already watches the file). A small unit test of the plugin itself (it resolves and loads the virtual module, the data has an entry for a known catalogue name and none for a made-up name; and reloads after the bundle's mtime changes, if that can be tested cheaply).

## Fix D: one stale comment (comment text only)
web/e2e/library.spec.ts, the comment above the "a few hundred exercises" test says the catalogue grows to about 250 and the lifter's own exercises stand in until it has. Replace the comment text only with what is true now: the catalogue has 225 exercises, and 250 of the lifter's own on top make a list of 475 rows. Do not change the test's steps, numbers or threshold.

## Testing
Focused tests while fixing. Then once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` (re-record Android screenshots only if they change — the calf raises are probably in none) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off` (known flakes: reload.spec toHaveValue; the JS coach golden mocha timeout — re-run once). Update the fingerprint golden for the two calf raises only (prove the other 223 are unchanged: the golden with those two entries removed, before and after). Regenerate docs/library/pictures.html (`cd web && npm run figures:page`) since two pictures change. Then `./gradlew --stop`. No speed runs (the controller does them).

## Commits
Two new commits on top of BASE, in this order, each ending with your harness's attribution lines; no model names elsewhere:
1. `fix(domain): calf raises face the way the feet point; split squats build quickly` (Fixes A and B, the regenerated web domain bundle and pictures page).
2. `fix(web): dev server refreshes thumbnail data; picker speed comment` (Fixes C and D).

## Report
Full report to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/task-14-fixwave-report.md: per fix what changed and why, RED/GREEN, the list and sheet of other upright bar-on-back figures with your judgement of each, build times before/after for the two split squats and the slowest five figures now, golden before/after and the 223-unchanged proof, test counts, concerns. Reply ONLY (under 12 lines): Status, commits, golden, the list of other figures with the same facing problem (names only), split squat build times before/after, test summary, concerns, report path.
