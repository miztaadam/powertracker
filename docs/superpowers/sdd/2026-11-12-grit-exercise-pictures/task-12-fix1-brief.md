# Task 12 fix round 1 (calf and core pictures)

Review: review-12-report.md (same folder) — read it. Verdict: Needs fixes, 0 Critical / 3 Important / 10 Minor. The controller's rulings below say what to do. HEAD is 1563e21 (local, not pushed): amend it (one commit for the task; keep your commit message and attribution lines). Never push, never switch branches. The 199 older figures must not change (golden with the new calf and core entries removed stays 199:f499503d0cebe8f9). Do not change guide text.

## Fix 1 (Important 1): Farmer's walk and Plate pinch carry look the same at thumbnail
Draw the Farmer's walk from the front, like the Suitcase carry: a dumbbell in each hand, shoulders level, upright (review option a). Then the four carries differ: farmer's (front, two loads), suitcase (front, one load), waiter's (side, overhead), plate pinch (side, plates). Also make the pinched plates read as plates at thumbnail size if that is cheap (a clearer, larger disc held by its top edge). Compare the four thumbnails side by side.

## Fix 2 (Important 2): Cable woodchop — a real diagonal, straight arms, and a test that catches a wrong-way elbow
The app's guide says arms straight, and its source (ACE standing rotational chop, already cited) sweeps the handle from above one shoulder down toward the opposite hip. As drawn the hands move only ±6 cm from the middle, one elbow bends 37°, and START reads as an overhead reach with the head hidden.
- START: both hands on the handle above and outside the near (pulley-side) shoulder, clear of the head; END: at the far hip, outside it. Arms as straight as two hands on one handle allow (aim ≤ 15° bend). The chop turns the shoulders: if the kit can cheaply draw a turned torso from the front (shoulders narrower across the camera by the turn's cosine, the far arm reaching across), do that and say so; if it cannot, widen the sweep as far as straight-ish arms reach and report the numbers.
- Test: bound the hands' sideways travel (well beyond today's ±6 cm), both elbows' bend, and add bounds that fail on a wrong-way elbow (the review's mutation — flipping the arm hint — must make the woodchop's own test fail, not only the golden).

## Fix 3 (Important 3): Cable crunch END — the head is inside the cable column
Move the stack out so the head and rope clear the column in both panels, and bound the clearance in the test.

## Fix 4 (Minors taken)
- Minor 1: Reverse crunch END — a bigger roll (pelvis about 13–15 cm off the floor; raise the bound) so "Roll your hips up" shows. Bicycle crunch caption: use wording the panel shows if the guide has it; otherwise leave it.
- Minor 2: Machine crunch — the chest pad looks unattached; show its lever (in front of the torso or beyond the shoulder).
- Minor 5: Smith calf raise — the safety stop the guide asks for, visible under the bar in both panels.
- Minor 7: remove the now-dead branch in `FiguresTest.hasFigure_resolvesNamesLikeFigure` (or keep it alive with a made-up non-catalogue name, as the review describes) so the test has no dead code.
- Minor 8: Side plank START — bound `out` and `forearmOut` so a wrong-way START arm fails.

## Accepted as they are (no change)
The crunches' rigid torso (floor, cable, machine); the Pallof press's column behind the body; the calf raises' small heel travel at thumbnail (sourced range); the Leg press calf raise's handles (as the approved Leg press); the four citations not in Sources.kt (the controller checked them).

## Testing
Focused tests while fixing. Then once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` (re-record Android screenshots only if they change) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Update the golden. Confirm again that `everyCatalogueExercise_hasFigure` passes with its @Ignore removed, then put the @Ignore back (Task 13 switches it on). Then `./gradlew --stop`. No speed runs (the controller does them). Re-render the calves and core sheets.

## Report
Append a "Fix round 1" section to task-12-report.md: what changed per fix, RED/GREEN, golden, test counts, anything you could not do. Reply ONLY (under 10 lines): Status, commit, golden, test summary, concerns.
