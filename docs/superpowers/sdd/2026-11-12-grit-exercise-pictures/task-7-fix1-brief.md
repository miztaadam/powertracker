# Task 7 fix round 1 (back pictures)

Review: review-7-report.md (same folder) — read it. Verdict: Needs fixes, 0 Critical / 1 Important / 8 Minor. The controller's rulings below say what to do. HEAD is f52f10f (local, not pushed): amend it (one commit for the task; keep your commit message and attribution lines). Never push, never switch branches. The 61 older figures must not change (golden with the new back entries removed stays 61:bf681f219217a906).

## Fix 1 (Important): Chest-supported row and Chest-supported T-bar row read as kneeling
Do what review-7 Important 1 says: the legs trail nearly straight behind (knee about 10–25°), toes on the floor (DB row) or feet on the platform (T-bar; move the platform back/down or lengthen the reach), the pad still clearly under the chest. Add a knee bound (about 0..30) to both tests. Check both thumbnails.

## Fix 2 (controller + review Minor 2): Inverted row START
The arms lean about 35° toward the feet with the bar over the belly, so it reads as reaching. Hanging under a bar, the shoulders are almost straight under the hands. Move the bar toward the head (and adjust its height as the guide says: "just high enough that your arms can fully straighten") so that at END the bar meets the chest ("Pull your chest up to the bar") and at START the arms are within about 15° of vertical, body straight and rigid, heels on the floor, the back just off the floor. Hands and heels stay put between the panels. Bound the START arm angle in the test (0..15° from vertical).

## Fix 3 (review Minor 1, part): Wide-grip cable row START caption
"Wider than shoulders" names something the side view does not show. Use guide wording the START picture does show (the stretch / straight arms), keep ≤24 characters. No pose change.

## Fix 4 (review Minor 4): Weighted pull-up / Weighted chin-up chain
Draw the belt's chain so it is seen from the hip to the plate (after the leg, or ending where it leaves the thigh), so the plate reads as hanging from the belt and not as a ring below the feet.

## Fix 5 (review Minor 7 and 8)
Add the one-line typical-size comments to Spotter, CableAttachment (units for STRAIGHT_HALF and HANDLE), CableHandle and PulloverDumbbell. Add the elbow bound to the One-arm pulldown test as the review suggests.

## Accepted as they are (no change)
Look-alike pairs the side view cannot separate (close-grip vs lat pulldown, weighted/assisted chin-up vs pull-up, neutral-grip vs pull-up, weighted vs plain back extension); the band crossing the body; the chin-up's legs; the shrug height; the DB pullover spotter (the guide asks for one).

## Testing
Focused tests while fixing. Then once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` (re-record Android screenshots only if they change) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Update the golden. Then `./gradlew --stop`. No speed runs (the controller does them). Re-render `npm run figures:sheet -- --label back back`.

## Report
Append a "Fix round 1" section to task-7-report.md: what changed per fix, RED/GREEN, golden, test counts, anything you could not do. Reply ONLY (under 10 lines): Status, commit, golden, test summary, concerns.
