# Task 8 fix round 1 (shoulder pictures)

Review: review-8-report.md (same folder) — read it. Verdict: Needs fixes, 0 Critical / 2 Important / 9 Minor. The controller's rulings below say what to do. HEAD is 3df8d7e (local, not pushed): amend it (one commit for the task; keep your commit message and attribution lines). Never push, never switch branches. The 91 older figures must not change (golden with the new shoulder entries removed stays 91:b4bc0637353c0a91).

## Fix 1 (Important 1): Arnold press — draw it from the front
The side view with end-on dumbbells cannot show what the exercise is (palms facing you at the bottom, opening out as you press). Do what review-8 Important 1 suggests: front view; START forearms upright with the elbows in front of the chest (upper arms toward the camera, foreshortened), hands close together at about chin height, dumbbells in front of the chin; END arms straight up, hands about shoulder-width apart. START and END must be clearly different from DB shoulder press and One-arm DB press at thumbnail size. Captions from the guide's wording that these panels show (≤24 characters). Test: hands close together in front at START (bound the gap), elbows inside the shoulders at START, arms straight and apart at END.

## Fix 2 (Important 2): Seated rear delt raise — the bend must show
START reads as sitting upright, and END cannot be told from Seated lateral raise at thumbnail size. Make the bent-over torso visible head-on, as the standing Rear delt raise does (head sunk low between/below the shoulder line, shoulders low over the knees, torso a short block), within the hip limit. Change the START caption to guide wording the panel shows. Add a test bound that an upright pose fails (shoulder height above the seat, or the head relative to the shoulders). Compare its thumbnail next to Seated lateral raise: the difference must be plain.

## Fix 3 (review Minors taken)
- Minor 2: Chest-supported rear delt raise — a bench top wider than the torso so it reads as lying on a bench, not standing behind a stool.
- Minor 4: Cable reverse fly — drawing order: both arms in front of both columns in END; START as the review says.
- Minor 9: add the knee bound to the Cable rear delt row test, and the forearmOut bound (10..40) to the rear delt raises' tests.

## Accepted as they are (no change)
Standing rear delt raise START; Machine reverse fly START; Machine shoulder press lever behind the head; Smith shoulder press recline and rail; Seated lateral raise bench; the rotations (neutral end points, they read and differ).

## Testing
Focused tests while fixing. Then once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` (re-record Android screenshots only if they change) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Update the golden. Then `./gradlew --stop`. No speed runs (the controller does them). Re-render `npm run figures:sheet -- --label shoulders shoulders`.

## Report
Append a "Fix round 1" section to task-8-report.md: what changed per fix, RED/GREEN, golden, test counts, anything you could not do. Reply ONLY (under 10 lines): Status, commit, golden, test summary, concerns.
