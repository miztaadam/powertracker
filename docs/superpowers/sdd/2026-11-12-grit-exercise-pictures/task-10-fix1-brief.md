# Task 10 fix round 1 (quad pictures)

Review: review-10-report.md (same folder) — read it. Verdict: Needs fixes, 0 Critical / 3 Important / 10 Minor. The controller's rulings below say what to do. HEAD is 44bdb8d (local, not pushed): amend it (one commit for the task; keep your commit message and attribution lines). Never push, never switch branches. The 145 older figures must not change (golden with the new quad entries removed stays 145:4ac19b1768653a16).

## Fix 1 (Important 1, ruling R16): the back leg of every split stance reads as kneeling
Lunge, Reverse lunge, Walking lunge, Barbell lunge, Barbell reverse lunge, Split squat, Smith split squat (and check the Bulgarian split squat is unaffected). The back shin lies along the floor. In a real lunge the back foot is on the ball of the foot with the heel well up, and the shin slopes up from the knee to the ankle. The figure has no toe joint, so do what review-10 Important 1 calls the cheapest honest fix: for the back foot on its ball, let the drawn foot stand steeper (about 70° from the shin: the ankle's plantar flexion plus the bend at the ball of the foot that the figure lacks), toes on the floor, so the ankle sits about 22–26 cm up and the shin slopes about 15–20°. Relax the ankle check for exactly this case (a named constant or a flag on the pose, with a comment saying why: the toes bend, not the ankle) — do not loosen the ankle limit for any other figure. ExRx's split squat page supports the raised heel ("heel of the rear foot ... elevated off the floor"). Add to `assertSplitBottom` a bound on the back shin's slope (about 12..25° up toward the heel) and tighten the heel bound to match. Look at every one of these at thumbnail size: it must read as a lunge, not as kneeling.

## Fix 2 (Important 2, ruling R17): Split squat and Smith split squat START is a half-lunge
Do what review-10 Important 2 says: give the two split squats their own, shorter stance (about 85–90 cm ankle to ankle) so START stands nearly tall on a split stance (front knee 5..25°), END unchanged in character; raise the test's START bounds as the review says (hips rise 35..50). The lunges keep their long step.

## Fix 3 (Important 3, ruling R18): Jump squat END reads as standing
Raise the jump to 35–40 cm off the floor (bounds 30..50, FeetOffFloor min 25), toes pointed, legs straight. Arms: the app's guide only says arms at your sides in the set-up. The catalogue entry cites ACE's squat jump pages (Sources.ACE_SQUAT_JUMPS, Sources.ACE_SQUAT_JUMP in Sources.kt): if one of those pages states the arm action in the jump (search it with WebSearch allowed_domains acefitness.org), draw that and cite it; if not, keep the arms at the sides. Check the thumbnail (END): the gap must be plain.

## Fix 4 (controller): Hack squat START arm
At START the near arm points forward in mid-air. Put the hands in the same place in both panels: on the handles beside the shoulder pads if the kit has or can cheaply get them (typical for the machine), otherwise resting along the body. Bound the hand in the test.

## Fix 5 (citations, comments only)
- Line ~290, jump squat "knees bend to 90 degrees": my allowed-domain search does not show that sentence on the cited page (ACE expert-articles/5869); it is on ACE's squat jump guidance. Cite the page that states it — preferably the URL already in Sources.kt for this exercise — or mark 90° neutral.
- Line ~952, Smith split squat: my search shows the ExRx page at https://exrx.net/WeightExercises/GluteusMaximus/SMSplitSquat; the code cites .../Quadriceps/SMSplitSquat. Use the URL in Sources.kt if that is what it has; otherwise the one the search shows.

## Minors taken
- Minor 3: bound the Smith squat START torso (80..90) if the pose can meet it without moving the bar off the rail; if it cannot, say so and leave it.

## Accepted as they are (no change)
Curtsy lunge colours; Sled push 55° (neutral); Single-leg leg press rail; Single-leg leg extension stub; Lever squat; Bodyweight squat START caption; the look-alike lunge thumbnails (Lunge, Reverse lunge, Walking lunge share a bottom).

## Testing
Focused tests while fixing. Then once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` (re-record Android screenshots only if they change) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Update the golden. Then `./gradlew --stop`. No speed runs (the controller does them). Re-render `npm run figures:sheet -- --label quads quads`.

## Report
Append a "Fix round 1" section to task-10-report.md: what changed per fix, RED/GREEN, golden, test counts, anything you could not do. Reply ONLY (under 10 lines): Status, commit, golden, test summary, concerns.
