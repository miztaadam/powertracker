# Task 8 report: shoulder pictures (23 new figures)

**Status:** DONE_WITH_CONCERNS. BASE b35cabb, one commit on top: `feat(domain): shoulder pictures` (local, not pushed).

**Count:** the catalogue has 24 `BodyArea.SHOULDERS` entries (library/areas/Shoulders.kt). DB shoulder press was drawn (unchanged); 23 are new. `BatchShouldersTest.shoulders_everyExerciseHasAFigure_thatPassesItsChecks` asserts all 24 pass FigureChecks and each one's camera.

**Golden:** `114:f101883ab2331f52`. Proof nothing existing moved: with the new entries filtered out of `shoulderFigures` (after every kit change), FiguresTest passed against the old golden `91:b4bc0637353c0a91`.

**Attribution:** the commit ends with my harness's lines (Co-Authored-By: Claude Opus 5.5 + the session line), as the brief allows when the harness gives different lines.

## Where the code is
- All figures in `figures/areas/Shoulders.kt`, under "Task 8: the rest of the Shoulders area" (camera choices explained in its header). New figures are `private val x by lazy`, map `shoulderBatchFigures` of `lazy { }` merged into `shoulderFigures`; AreaMapsLazyTest passes.
- New kit file `figures/KitShoulders.kt`.

## Changes to shared code (no existing figure's drawing changed; proven by the golden)
- **Kit.kt:** `CableStack.crossed` (front two-column: each pulley cabled to the other side's hand); `cableInFront` now also works in a front view's single column (`halfSpan == 0`); wiring for `Kettlebell` and `LateralRaiseMachine`; `PecDeck.drawFront`.
- **KitChest.kt:** `PressMachine.handleEndOn` (palms-forward handle drawn end-on, a disc); `PecDeck.facingPad` (reverse fly: no back pad, a chest pad drawn in front of the torso, handles level). Defaults are the old drawing.
- **FigureChecks.kt:** bar radius 0 for `Kettlebell` (exhaustive `when`).
- **Back.kt:** `rowSeatShoulder`, `rowPose`, `rowStation`, `SEATED_ROW_CONTACTS` made `internal` (unchanged) for the cable rear delt row.

## Kit (KitShoulders.kt; each with a typical-size comment and a KitTest case, +5 KitTest)
| Piece | What |
|---|---|
| `Kettlebell(hand, hold)` | 16 kg bell, 21 cm across, 3 cm handle; RACK: bell just in front of the upright forearm by the wrist; OVERHEAD: behind the forearm. Centre for checks = the grip. |
| `LateralRaiseMachine` | Front view: seat, back pad, levers pivoting at the shoulder joints, 20 cm × 9 cm arm pads on the outside of the upper arms by the elbows, drawn in front with gap outlines. |
| PecDeck `facingPad` | 28 × 36 cm chest pad over the torso. |
| CableStack `crossed` / front single-column `cableInFront` | as above. |
| PressMachine `handleEndOn` | end-on handle disc. |

## Per figure (camera; key positions; sources; judgement calls)
Sources: Sources.kt's ExRx/ACE pages for each exercise (URL in each comment) plus ExRx DBOneArmLateralRaise (not in Sources.kt; its 10–30° fixed elbow angle, seen in the search result) and DBLyingRearLateralRaise (10–30° elbow, bench close to horizontal). Everything else is marked neutral in its comment.

**Presses (side)**
- Seated barbell press: upright bench in a rack (posts 60 cm in front, beyond the feet); START bar 8 cm above the shoulder joints (chin ≈ 9), 12.5 cm in front (clear of the face), elbows forward and down (3D arm, out ≈ 28°); END lockout over shoulders. Pins 10 cm under the start bar (RACK_SAFETY). Grip 30 cm out neutral.
- Smith shoulder press: bench back 80° (neutral) so the head rests back on the pad and the rail (8.5 cm in front of the shoulder joint) passes 0.3–3 cm in front of the face in both panels; stops just under the start bar; bar on rails both panels; straight arms at END (no LockoutOverShoulders: a vertical rail clear of the face cannot be within 5 cm of the shoulder line).
- Machine shoulder press: PressMachine, back 80°, handles 8 cm above the shoulders (palms forward, end-on), END bend 10° ("nearly straight"), lever pivot behind the pad, same length both panels. Caption "Level with shoulders".
- One-arm DB press: standing; START = the approved DB shoulder press's start (elbow under wrist, upper arm 50° out); free arm hangs; END lockout.
- Arnold press: START elbows in front of the chest (upper arm 42° forward, in plane), forearms upright, dumbbells just clear of the face; END lockout. **Judgement:** palms facing you and palms forward both put the handle across the body, so the side view shows end-on dumbbells in both panels; the brief's expected end-on vs profile change does not happen physically. The visible difference from the DB shoulder press is the elbows in front (drawn full length) vs out to the sides.
- Kettlebell press: feet 12 cm staggered, KB side foot forward; START rack (upper arm 15° forward, elbow 145°, grip 18 cm in front at shoulder height, elbow by the ribs); END straight overhead, bell behind the forearm.
- Landmine press: TBarLever reused (pivot on the floor in front, plates 20 cm before the hand); split stance (back foot on the ball, heel 25° up; back knee ≈31°, front ≈24°), torso 85°; START end at the shoulder; END up the arc (bar length kept) to a straight arm.

**Front raises (side):** Front raise (DBs end-on against the thighs → arms level, straight); Barbell front raise (bar just in front of thighs → level, elbow 5°); Cable front raise (low pulley behind, cable drawn in front both panels, arm 12° back → level).

**Lateral raises (front):** fixed 18° bend, elbows back (forearm 18° forward), END elbows exactly at shoulder height with hands level (never elbows below wrists). Lateral raise (DBs end-on, clear of thighs); Seated lateral raise (bench end-on = `Seat` 30 cm wide, legs together); Cable lateral raise (single low pulley on the free arm's side, cable across in front); Machine lateral raise (elbow 90°, forearm forward; pivots at the shoulder joints; pads outside the arm, on top at level). Caption "Out to shoulder height" (the guide's "elbows are at shoulder height" is 25 chars).

**Rear delts (front, head-on torso):** Rear delt raise (lean 70 = torso 20° above level as the Row, thighs 25° forward, head sunk in line, neckOut 75; legs drawn below because they stand under the hips); Seated rear delt raise (lean 58); Chest-supported (bench close to flat → lean 80, legs hidden per the archer rule, bench end-on under the chest, top ≈68 cm so DBs clear the floor). DBs end-on (palms facing each other); arms hang → straight out level. Machine reverse fly (PecDeck facingPad; START arms forward to handles at shoulder height, END elbows 12° behind the body plane, level); Cable reverse fly (seen from behind; columns ±30, pulleys at shoulder height, crossed cables, hands crossed at START, level out at END; Arm.out still measured towards the chest — documented in the header).

**Cable rear delt row (side):** Back.kt's row station/body; END bar touching the upper chest, elbows level with the shoulders, 12 cm behind the joint (2 cm behind the back), out ≈68°; with fixed lengths the hands end ≈30 cm out (elbows 48): "about elbow-width" drawn on the narrow side.

**Rotations:** camera front, chosen because the forearm's swing across/out shows as the hand moving across the picture (forearm foreshortened where it points forward). Elbow at the side (upper arm straight down along the torso, its inner edge on the torso's side), bend 90° (true 3D) both panels. Across the belly = 35° forward of straight across (passes in front of the belly); turned out = 45° past forward (neutral, conservative — no allowed page I found states the arm-at-side ER range). Cable ER: pulley on the far side at hand height (waist); cable in front. Cable IR: pulley on the near side at elbow height. Side-lying ER: front view of a body lying along the floor (torso 174°), forearm from hanging (in front of the belly, toward the floor) to pointing up; bottom arm along the floor beyond the head; top leg slightly forward; DB in profile (handle along the body).

## Tests
| Suite | Result |
|---|---|
| domain jvmTest | 418 passed, 1 skipped |
| domain jsNodeTest | 433 passed, 1 skipped (first run: the known GoldenScenarioTest 2 s Mocha timeout; rerun green) |
| checkWebDomain | ok (bridge regenerated) |
| app testDebugUnitTest | 301 passed |
| web unit | 657 passed; typecheck clean; build ok |
| e2e `--trace off` | 90 passed |

- New `BatchShouldersTest`: 24 tests (area + one per new figure) with real bounds: bar/handle heights, face clearance, elbows forward/below/out (explicit bound for every out-of-plane arm: presses' `out` ranges and elbow-below/in-front, lateral raises `out == 0` and forearmOut 10..30, reverse flys' out ranges, rear delt row out 55..85 and elbow behind the back), level elbows, lever/bar lengths kept, contacts, knees (kettlebell 0..20, landmine front 15..60 / back 0..40), pulley side and height, rotation elbow at the side and 90°.
- KitTest +5.
- RED: with the batch filtered out, BatchShouldersTest ran 24 tests, 24 failed ("no figure"); FiguresTest still matched the old golden. The first GREEN attempts failed on real problems, fixed: leg out of reach (landmine), elbows past 150° (machine press, landmine), ankle past 35° and hip extension past 20° (landmine back leg → on the ball of the foot), hip flexion past 140° (seated rear delt), head through the Smith pad, the rear delt row bar 5 cm off the chest (I had used the wrong forearm-to-grip length), side-lying limbs through the floor, two captions over 24, then three test bounds (kettlebell stance, reverse-fly hands not crossed, one-arm DB height bound widened to the approved press's start).
- KitTest cases were written alongside the kit, not before it (no separate RED run for them).

## Screenshots
Android re-recorded with `-Pscreenshots.update=true --rerun`: no file changed. Web: no committed screenshot changed (e2e passed).

## Speed (after `./gradlew --stop`, `npx playwright test --project=speed --no-deps --trace off`)
- Run 1: 4/4 passed, 14.1 s (Exercises tab 1.8 s; picker 1.3/1.1/2.0 s).
- Run 2: **1 failed**: picker "a few hundred exercises" (1.3 s), so the serial picker browse test did not run; Exercises tab passed (2.0 s). The test prints no reading; not changed.
- Run 3: 4/4 passed, 15.5 s (tab 1.9 s).
- Two extra runs: 4/4 passed both (14.1–15.9 s, tab 1.8–1.9 s).

## Sheets
- `/home/user/powertracker/web/.figure-sheets/shoulders-dark.png`, `shoulders-light.png` (all 24, 3 columns); detail `/home/user/powertracker/web/.figure-sheets/s8d-{dark,light}.png`.
- Checked all at sheet size and thumbnail, dark and light. Fixed after looking: rear delt raise head sunk (reads bent over head-on), kettlebell racked bell moved in front of the forearm (was lost in the body), machine press pivot moved back so the lever shows.

## Concerns
1. Rear delt raise / seated / chest-supported START head-on views are abstract: the foreshortened torso reads as a short trunk; the END "T" reads well (thumbnails are END).
2. Seated rear delt raise: the 140° hip-flexion limit stops the chest about 5 cm above the thighs (invisible from the front).
3. Arnold press: dumbbells end-on in both panels (see above); the rotation itself is not visible.
4. Smith press bench reclined 10° to show "head back"; no LockoutOverShoulders (bar 8.5 cm in front of the shoulder line at lockout).
5. Cable reverse fly seen from behind; the columns are mostly hidden behind the body and the thin crossed cables are faint at thumbnail size.
6. Rotation end points (35° across, 45° out) and many lengths are neutral; no allowed page found stating arm-at-side ER range.
7. Speed run 2 had one picker-test failure (unrelated area, no thumbnails), other 4 runs passed.
8. Out-of-plane arms are bounded per figure in the tests (Body.kt's looser checks).

## Fix round 1

Brief: task-8-fix1-brief.md, from review-8-report.md. I amended the Task 8 commit and kept its message and attribution lines.

### Fix 1: Arnold press drawn from the front
- The camera is now the front view. Captions: START "Elbows under your wrists", END "Arms straight overhead". The thumbnail is START.
- START:
  - The forearms are upright.
  - The elbows are in front of the chest, inside the shoulders. The upper arms point forward and in: `out` is about 32°, drawn short.
  - The hands are 24 cm apart, 7 cm above the shoulder joints. That is in front of the shoulders, below the chin, so the top of the face shows.
  - The dumbbells are in profile (the handles run across the body, as the palms-facing-you grip gives).
- END:
  - The arms are straight overhead, about 59 cm apart.
  - They stay 10° out from upright, because the front view's shoulder limit is 170°.
  - The dumbbells are still in profile (palms forward, the handles still across).
- LockoutOverShoulders is replaced by ElbowBend(0..1, END). In the front view that check measures the far dumbbell's position, not the midpoint between the hands.
- Seen next to DB shoulder press and One-arm DB press at full size and as thumbnails, both panels now look different from those two.
- Test bounds:
  - START: hands 15..30 cm apart; 3..15 cm above the shoulder joints; elbow at least 3 cm inside the shoulder and under the wrist; upper arm forward (`out` 25..60).
  - END: straight; hands 45..70 cm apart; above the head; dumbbells in profile.

### Fix 2: Seated rear delt raise bent over
- The torso leans 67.5° and the head is sunk in line with it (neckOut 75°): the head centre is 0..10 cm above the shoulders' line.
- To stay inside the 140° hip limit, the thighs slope down 18° (out 72°) and the shins tuck back 15°. The bench end is therefore about 52 cm high (neutral).
- The shoulders are now about 27 cm above the seat. The upright Seated lateral raise has them about 60 cm above.
- START caption: "Bend over".
- Next to Seated lateral raise, the thumbnails are plainly different: a low, hunched T against a tall one.
- New test bounds:
  - the shoulders 15..35 cm above the seat;
  - the head 0..10 cm above the shoulders' line;
  - the shoulders at least 20 cm lower above their seat than in Seated lateral raise (an upright pose fails).

### Fix 3: the review's Minors
- **Chest-supported rear delt raise:**
  - The bench end is now 46 cm wide and the arms hang outside it (hanging 14° out).
  - The 46 cm width is drawn wider than a real 30 cm pad so the bench reads as a bench. This is said in a code comment and is the one size not at real scale.
  - New bounds: the bench top is wider than the shoulders, and the elbows are outside it.
- **Cable reverse fly:**
  - New `CableStack.beyond` (default false). FigureRenderer draws such a station first, before the far limbs, so both arms are in front of both columns.
  - No existing figure uses it (proven by the golden).
  - START now reaches forward with the hands just outside the shoulders, 0..12 cm out (bounded), so nothing crosses over the back.
  - New test: in both panels, the far arm is drawn after the last column.
- **Two test bounds:** Cable rear delt row knees 0..40 in both panels. The rear delt raises' forearmOut 10..40.

### RED/GREEN
- **RED:** with the new tests on the old figure code (the `beyond` assertion commented out, since that property did not exist yet), BatchShouldersTest ran 24 tests and 5 failed:
  - the area test: Arnold press was SIDE, not FRONT;
  - Arnold press: the hands were 0 cm apart in the side view;
  - Seated rear delt raise: the lean was 58;
  - Chest-supported rear delt raise: the bench was not wider than the shoulders;
  - Cable reverse fly: the hands were 24 cm inside the shoulders.
- The knee and forearmOut bounds passed already, as expected.
- **GREEN** problems on the way, each fixed:
  - the reverse-fly hands were too far out (18.8 cm);
  - the Arnold elbow was at 157° when I lowered the hands to 4 cm above the shoulders, so I used 7 cm.

### Golden and tests
- **Golden:** `114:817892063d845bbc`.
- With the new shoulder entries removed (after the renderer and `beyond` change), FiguresTest still passed against `91:b4bc0637353c0a91`.

| Suite | Result |
|---|---|
| domain jvmTest | 418 passed, 1 skipped |
| domain jsNodeTest | 433 passed, 1 skipped (the known GoldenScenarioTest timeout once; rerun green) |
| checkWebDomain | ok |
| app testDebugUnitTest | 301 passed; screenshots re-recorded, none changed |
| web unit | 657 passed |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

- `./gradlew --stop` done. No speed runs, as the brief says.
- The regenerated bridge also reorders interface lists in two Kotlin runtime files (kotlin-kotlin-stdlib.mjs and kotlinx-serialization-kotlinx-serialization-json.mjs). This is the compiler's output; checkWebDomain passes.

### Sheets
- `/home/user/powertracker/web/.figure-sheets/shoulders-{dark,light}.png` (all 24, 6 columns).
- `/home/user/powertracker/web/.figure-sheets/s8f-{dark,light}.png` (the changed figures beside their look-alikes).
- `/home/user/powertracker/web/.figure-sheets/s8a-{dark,light}.png` (Arnold press beside One-arm DB press).

### Concerns
- At START of the Arnold press the dumbbells cover the lower face.
- The chest-supported bench end is drawn wider than real scale.
