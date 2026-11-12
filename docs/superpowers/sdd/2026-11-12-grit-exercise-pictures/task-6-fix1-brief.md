# Task 6 fix round 1 (chest pictures)

Review: review-6-report.md (same folder) — read it. Verdict: Needs fixes, 0 Critical / 1 Important / 5 Minor. The controller's rulings below say what to do. HEAD is c88b4b3 (local, not pushed): amend it (one commit for the task). Never push, never switch branches. Do not change any of the 31 pre-chest figures' drawings (fingerprint with the chest map emptied must still be 31:29075f43d649812c).

## Fix 1 (Important, ruling R13): Archer push-up must read as a push-up seen head-on
As drawn, the legs hang below the torso, so END reads as a person kneeling/standing with arms braced, and START (the thumbnail) is a blob.
- Keep the front (head-on) view. Draw the body as it looks from in front of the head, camera at about head height: the hips and legs lie behind the shoulders and are hidden by them — nothing of the legs may show below or beside the torso. Say so in a comment. The torso should be what a head-on view shows of a body sloping away (short, behind/below the head), not a full-length block.
- END: the working arm (hand near the ribs) is straight and close to vertical under its shoulder; the other arm is straight and long, sloping out to the side to its hand on the floor; the shoulders tilt/shift toward the working arm if the guide's geometry gives that. Hands stay put between START and END.
- START: chest low, working arm bent with the elbow up/back, the other arm straight out along the floor, head turned toward the straight arm only if the kit can show it cheaply (optional).
- Pick the thumbnail pose that reads better at 56×40 and look at it at that size.
- Extend the test so it would fail if the legs showed (e.g. no leg shape extends outside the torso/head outline, or however the kit can express it) and bounds the working arm's angle from vertical at END.
- If after an honest attempt it still does not read, say so with crops — do not fall back to a side view on your own.

## Fix 2 (ruling R14): the three lying dumbbell flys read as presses — draw the dumbbells in profile
DB fly, Incline DB fly, Decline DB fly. In a fly the palms face each other, so each dumbbell's handle runs along the body: seen from the side, the dumbbell is in profile (handle with a head at each end), where a press (palms toward the feet) shows it end-on as a disc. Today both draw the disc, so the fly's bottom reads as the bottom of a press.
- Give the dumbbell a grip orientation (e.g. `Dumbbell(hand, neutral = true)` or a separate prop; default unchanged so no existing figure's drawing changes — One-arm DB row keeps its disc). In the side view a neutral-grip dumbbell draws as the profile shape the front view already uses (handle + two heads), along the body's line (rotate with the bench angle if cheap; level is acceptable for flat, but on incline/decline it should follow the torso line).
- Use it for the three lying DB flys, both poses. KitTest case for the new drawing. Check at full size and thumbnail that DB fly and DB press are now clearly different in both panels.

## Fix 3 (review Minor, taken): Lying cable fly and Incline bench cable fly show no cable
Draw the cable from the low pulley housing to the handle in both poses (side view), so it reads as a cable exercise. Keep the housing as is. Add a check or test that the cable ends at the hand.

## Accepted as they are (no change)
Low-to-high cable fly hands at face height (the guide says so); pec deck / seated cable fly stub thighs; cable crossover lean; push-up wrist angle; the looser checks for out-of-plane arms (add one sentence in the Body.kt comment saying what is no longer caught — wrong-way elbow and over-flexion — so later batches know); the laziness test's small gap.

## Commit message
Amend the message to end with exactly these two lines (the session's current attribution lines; this is the controller's instruction and not optional):

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01KboHw6FdBYrNDSMx1QWyzy

## Testing
Focused tests while fixing. Then once: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` (re-record Android screenshots only if they change) and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Update the golden. Then `./gradlew --stop`. No speed runs needed (the controller does them). Re-render `npm run figures:sheet -- --label chest chest` and single sheets for the six changed figures.

## Report
Append a "Fix round 1" section to task-6-report.md: what changed per fix, RED/GREEN, golden, test counts, sheet paths, anything you could not do. Reply ONLY (under 10 lines): Status, commit, golden, test summary, concerns.
