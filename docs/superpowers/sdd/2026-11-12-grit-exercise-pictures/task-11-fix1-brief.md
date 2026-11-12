# Task 11 fix round 1 (hamstring and glute pictures)

Review: review-11-report.md (same folder) — read it. Verdict: Needs fixes, 0 Critical / 3 Important / 8 Minor. The controller's rulings below say what to do. HEAD is 2091c06 (local, not pushed), on top of the docs commit b0a8382: amend 2091c06 only (one commit for the task; keep your commit message and attribution lines). Never push, never switch branches. The 172 older figures must not change (golden with the new entries removed stays 172:84a1fde1ff223652). Do not change any guide text (the catalogue is out of scope).

## Fix 1 (Important 1): Single-leg RDL — draw what its guide says
The guide's step is "Lower until the weight reaches the floor"; END shows the dumbbell about 14 cm in the air. Draw the dumbbell at the floor (touching, within about 2 cm). Get there the way ExRx describes (its single-leg stiff-leg deadlift page: the weights go toward the floor, and a lifter bends the supporting knee more to bring them closer): bend the standing knee more (still a soft knee, not a squat) and, only if needed, let the torso go a few degrees below level; back flat, free leg straight and in line with the torso. Update the test bounds (dumbbell height 0..3 cm, standing knee range, torso range). Minor 2: START caption "Knees slightly bent" (the guide's wording).

## Fix 2 (Important 2): Quadruped hip extension — the back slopes too much
22° up toward the head reads as sagging hips. With straight arms the least the proportions allow is about 15–17°, so: a soft (slightly bent) elbow is allowed; aim for a back slope of 10–15° and tighten the test bound from 5..30 to about 5..16. Hands under the shoulders, knees under the hips, as the guide says.

## Fix 3 (Important 3): Clamshell START caption
"Hips stacked, knees bent" names a bend the front view does not show. Use guide wording the panel shows (the review suggests "Feet together"; check it is the guide's wording, ≤24 characters).

## Fix 4 (controller): Good morning — step back from the rack
At END the head sits at the tip of the rack's safety arm, so it looks as if the head rests on it. Stand the lifter (or place the rack) about 15–20 cm further apart so the head clears the arm and the upright in both panels; the bar still above the safety arms as the other rack figures. No pose change needed otherwise (the review accepted the bar path).

## Fix 5 (Minors taken)
- Minor 3: Machine hip abduction, Machine hip adduction and Reverse hyper — the guides say to grip the handles: put the hands on drawn handles (or the bench's edge/handles for the reverse hyper), and bound the hands in the tests.
- Minor 4: the thrusts' and the Barbell glute bridge's `handsOn` arms — add a bound that would fail on a wrong-way elbow or an elbow through the bench/floor.
- Citation: the clamshell's "hips at 45° and knees at 90°" — my allowed-domain search confirms PMC3418110 is the three side-lying exercises study but its snippet does not show those angles (PMC9993003 and PMC4595911 state 45° hips). Cite the page whose text you saw the numbers on, or mark what is not stated as neutral.

## Accepted as they are (no change)
Good morning bar path and knees; Lateral band walk step width; Hip thrust START torso; the low-value captions in Minor 8; the hip machines' seat overlap; the Reverse hyper without a pendulum.

## Testing
Focused tests while fixing. Then once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` (re-record Android screenshots only if they change) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Update the golden. Then `./gradlew --stop`. No speed runs (the controller does them). Re-render `npm run figures:sheet -- --label hamglutes hamstrings_glutes`.

## Report
Append a "Fix round 1" section to task-11-report.md: what changed per fix, RED/GREEN, golden, test counts, anything you could not do. Reply ONLY (under 10 lines): Status, commit, golden, test summary, concerns.
