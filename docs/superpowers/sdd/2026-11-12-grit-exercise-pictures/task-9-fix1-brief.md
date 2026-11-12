# Task 9 fix round 1 (arm pictures)

Review: review-9-report.md (same folder) — read it. Verdict: Needs fixes, 0 Critical / 2 Important / 12 Minor. The controller's rulings below say what to do. HEAD is c010525 (local, not pushed), on top of the docs commit e3c219f: amend c010525 only (one commit for the task; keep your commit message and attribution lines). Never push, never switch branches. The 114 older figures must not change (golden with the arm entries removed stays 114:817892063d845bbc).

## Fix 1 (Important 1): Concentration curl — the free arm goes through the body and bench
Do what review-9 Important 1 says: give the far arm a real pose (hand resting on the far thigh near the knee, as the kickback does, is preferred), in both panels, and add a test bound on the far wrist so an arm through the hip/bench fails. Then check every other arm figure for a free or far arm built with `HANG` on a leaning torso, and fix any that has the same fault.

## Fix 2 (Important 2): Triangle push-up — bound the END elbow
Add the bounds the review lists (elbow behind the shoulder, above the hand, hand under the chest, forearm's out-of-plane lean in a range). No picture change expected.

## Fix 3 (review Minors taken)
- Minor 2: the two wrist curls are identical — make their thumbnails differ: Wrist curl thumbnail END, Reverse wrist curl thumbnail START (or the other way round if that reads better); nothing else changes.
- Minor 5: DB lying triceps extension — tighten the test's upper-arm tip-back bound from 40° to about what is drawn plus a margin (≤ 20°).
- Minor 10: pushdown and cable overhead extension tests assert the attachment and that the cable runs from the pulley to the hand in both panels; knee bounds for the three lying figures (skull crusher, DB lying extension, JM press); the Seated DB curl gets the missing bounds (near elbow straight at START, torso upright, END bend); tighten the pad figures' knee bound (35..100) to what a seated lifter has.
- Minor 11: OverheadDumbbell gets its own size with units; correct the Dumbbell doc sentence about `squareToForearm`.

## Accepted as they are (no change)
Triceps dip depth (shoulder limit); Reverse curl = Curl and the pushdown/cable-curl look-alikes (side-view limit, captions differ); JM press path; curl tops; the small arm pads; heads hidden behind the arm in the two overhead extensions.

## Testing
Focused tests while fixing. Then once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` (re-record Android screenshots only if they change) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Update the golden. Then `./gradlew --stop`. No speed runs (the controller does them). Re-render `npm run figures:sheet -- --label arms arms`.

## Report
Append a "Fix round 1" section to task-9-report.md: what changed per fix, RED/GREEN, golden, test counts, anything you could not do. Reply ONLY (under 10 lines): Status, commit, golden, test summary, concerns.
