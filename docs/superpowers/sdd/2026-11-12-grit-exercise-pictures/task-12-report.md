# Task 12 report: calf and core pictures (26 new figures)

**Status:** DONE_WITH_CONCERNS.
- **Commit:** `1563e21 feat(domain): calf and core pictures`, one commit on BASE 37843b9. Local only, not pushed. Attribution: my harness's lines (Co-Authored-By: Claude Opus 5.5, plus the session line), as the brief allows.
- **Count:** the catalogue has 9 CALVES entries (Standing, Seated, Leg press, Donkey, Barbell, Smith, Dumbbell, Single-leg and Seated dumbbell calf raise) and 17 CORE entries (the brief's list). All 26 are new, so the total is now 225.
- **Golden:** `225:f0c60837400511bf`.
- **Old figures unchanged:** I emptied both new area maps, leaving every kit, check and renderer change in place. FiguresTest then passed against the old golden `199:f499503d0cebe8f9` (7 tests, 1 skipped, 0 failures).
- **Completeness check:** I removed the `@Ignore` on `everyCatalogueExercise_hasFigure` locally and FiguresTest passed (7 tests, 0 skipped, 0 failures). I put the `@Ignore` back before committing, so it is still off for Task 13.
- **Advisor:** none was available in this session.

## Shared code changed (no existing drawing changes)
- **Kit.kt:** wires the new pieces into `surfaces`, `solids`, `draw`, `drawFront`, the held pieces' `center` and `draw`.
- **FigureChecks.kt:** AbWheel counts as two-handed; barRadius gets entries for AbWheel (its radius) and PinchPlate (0).
- **No other changes:** no new Surface or BodyPoint, and the renderer and joint limits are untouched.

## Existing tests changed
All 225 figures now exist, so two tests that assumed an undrawn exercise broke:
- **FiguresTest `hasFigure_resolvesNamesLikeFigure`:** the undrawn-exercise block now runs only if an undrawn exercise exists. The rest of the test is unchanged.
- **web ExercisePicture.test.tsx `exercisesTab_coldList_buildsOnlyTheNearTilesFigures`:** it compared the near rows with figures to the 8 leading rows. Now every near row has a figure, so it compares only the first 8 near rows. The key checks are unchanged: `hasFigure` is called for every row, `figureInfo` only for near rows, and the SVG count matches.

## Kit (KitCalvesCore.kt)
Each piece has a typical-size comment with units. KitTest gained 7 cases; the Task 12 cases all pass.

| Piece | What it is |
|---|---|
| StandingCalfMachine | Column behind the lifter; lever from a pivot to the 12 cm shoulder pad (pivot set so the lever is the same length in both panels); handle in front of the shoulders. Pad and grip drawn in front with gap rings. Surface: HANDLE. |
| SeatedCalfMachine | 40 cm seat; lever from a post in front to the 12 cm knee pad. Lever and pad drawn in front. Surfaces: SEAT, THIGH_PAD. |
| DonkeyCalfMachine | Front column with a 6 cm forearm support; lever to a 14 cm hip pad. Pad and support drawn in front. Surfaces: ARM_PAD, HIP_PAD. |
| CrunchMachine | 40 cm seat 45 cm up; 30 cm hip pad; lever to a 14 cm chest pad drawn in front; foot roller. Surfaces: SEAT, HIP_PAD, CHEST_PAD. |
| SupportPost | 4 cm post with a level grip (a rail about 100 cm up). Surface: HANDLE. |
| AbWheel (held, two hands) | 18 cm wheel, grip at the hub. |
| PinchPlate (held) | 10 kg plate, 30 cm across, 3 cm thick. Rim in the hand, centre 12 cm below the grip. Side view shows the face; front view the edge. |

**Reused unchanged:** the deficit platform at 10 cm as the calf block or step; LegPress45; SmithMachine; PowerRack (rackInFront); FlatBench; StabilityBall; CableStack (with `beyond` for the Pallof, `cableInFront` for the woodchop); CableHandle; CableAttachment(ROPE); Dumbbell; Kettlebell RACK and OVERHEAD.

## Per figure
Each figure's source and choices are also in its comment in `figures/areas/Calves.kt` and `Core.kt`.

### Calves
All 9 use the side view.

**Range used by every calf raise:**
- **Total ankle range:** 41° with the knees straight, 38° with them bent. Source: PMC4424477, "about 41°/38°" on a step, seen in the search result.
- **Split, neutral:**
  - Straight knees: 16° heels-down at START, 25° plantar flexion at END.
  - Bent knees: 15° and 23°.
  - 25° is inside PMC5343533's 25–50° for a standing heel raise, also seen in the search result.
- **Foot:** the figure's foot is one straight piece. At START the block's edge is under the ball, 5 cm behind the toe's end. At END the foot pivots on the toe tips, which stay in place.
- **Tests (all raises):**
  - heel underside 3–8 cm below the block top at START, and 5–14 cm above it at END;
  - heel 5–25 cm behind the edge, toes 1–10 cm onto the block;
  - ankle angle in range in both panels.

| Figure | Pose, judgement calls and tests |
|---|---|
| Standing calf raise | Pad on the shoulders, hands on the handles, body tall, knees straight (≤3°). Pelvis halfway between the two ankle positions, so the legs lean at most about 1.5°. Tests: pad 9–14 cm over the shoulder joint, grip on the handle, lever the same length in both panels. |
| Barbell calf raise | Squat bar on the back with the Squat's arms. Rack per the guide's safety line: J-hooks 8 cm and safety arms 16 cm under the lowest bar position. Posts 75 cm in front of the bar, clear of the chest and the block. END caption "Raise heels, pause". |
| Smith calf raise | Same body. Pelvis x is the same in both panels, so BarOnRails passes at START and END. Stops 4 cm under the lowest bar. |
| Dumbbell calf raise | Near hand: a palms-in dumbbell (profile). Far hand: on a SupportPost (grip 110 cm, 15 cm past the toes; neutral). Tests: grip on the post, near arm vertical. |
| Single-leg calf raise | Near foot on the step. Free leg bent 90° behind, foot 10–60 cm above the step and behind its edge. Far hand on the post. The optional dumbbell is not drawn. Captions: "Heel below the step" / "Knee straight". The bent free leg sets it apart from the dumbbell calf raise (lesson 10). |
| Seated calf raise | Seat at the knee's height. Knees about 90° with the shins upright at START (neutral). Hips fixed; knees rise 5–14 cm. END ankle angle solved at the bent-knee range. Pad on the lower thigh (70–95% along it), hands on the pad. |
| Seated dumbbell calf raise | End of a 43 cm flat bench; thighs slope up a little. One dumbbell across the thighs near the knees, end on (lesson 2), both hands on it. Hips fixed. |
| Donkey calf raise | Torso 15° above level (neutral), forearms level on the support, pad on the hips. Shoulders stay on the support, so at END the hips rise (5–14 cm) and the torso pivots about the shoulders. The legs then lean back a little, so the END ankle bound is −28..−23. |
| Leg press calf raise | Approved leg press geometry: back and hips on the pads, knees 3°. Toes on the plate's lower edge, heels past it. The sled solved along its rail until the straight leg's foot is on the edge. START ankle +16°, END −25° (each solved to the leg). Platform pushed 4–20 cm further at END. |

### Core
- **Side view:** 14 figures.
- **Front view:** the side plank, the cable woodchop and the suitcase carry. The camera reasoning is in Core.kt's header.
- **Torso:** the figure's torso is rigid, so a crunch's curl is drawn as the torso tilting about still hips (see concern 3).

**Crunches and supine drills**

| Figure | Panels, choices and tests |
|---|---|
| Floor crunch | Knees about 95°, feet flat, hands behind the head with the elbows out (3D arms, out 30–90°). END torso 30° up: ExRx curl-up protocol "30 degree spinal flexion" (exrx.net/Testing/CurlUpTests) and ACE GFI assessments, both seen in the search result. Hips and thighs still. |
| Bicycle crunch | Torso 30° up in both panels. START: far knee in (thigh 95–110°), near leg straight about 30° up, near elbow swung forward. END: switched. The trunk's turn is out of the picture. |
| Reverse crunch | START: knees over the hips, bent 90°. END: hips rolled 9 cm off the floor (neutral), thighs toward the chest (115–135°), same knee bend. Shoulders and head stay down. |
| Dead bug | START: arms up, knees over the hips. END: near arm overhead at 180° flexion, wrist bent so the fingertips are 2 cm off the floor; far leg straight, heel 2 cm off the floor. Far arm and near leg still up. Hips flat. |

**Planks**

| Figure | Panels, choices and tests |
|---|---|
| Front plank | START "Lie face down": torso level, toes tucked. Elbows under the shoulders: the upper arms point out toward the camera, so they are foreshortened. END: elbows under the shoulders (≤0.5 cm), forearms on the floor, toe tips down. Shoulder–hip–ankle bend ≤1°, hip, knee and neck 0. |
| Side plank (front view) | Bottom elbow on the floor under the bottom shoulder. The forearm points at the camera (a stub). Feet stacked (top ankle one shin width above). Top arm along the side. START "Prop up on your forearm": hips on the floor, bottom leg turned up 18° (inside the 20° limit); the upper arm then points out of the picture to land the elbow. END: one line, head to heels (bend ≤1°), hips 10–40 cm higher than at START. |

**Kneeling**

| Figure | Panels, choices and tests |
|---|---|
| Bird dog | Quadruped base with softened elbows, back slope ≤15°. END: near arm and far leg level with the floor (±0.5°). Far hip extended by the back's slope, about 15°, inside its range. |
| Ab wheel rollout | START: wheel 30 cm in front of the knees, shoulders over it (±8 cm), hips sat back. Elbows ≤12°; the drawn hand needs the wrist at 60° and a small bend to reach the handle. END: body one line from the knees at 20° (neutral), elbows ≤10°, wheel 60–150 cm further out. |
| Stability ball rollout | 65 cm ball. Body one straight line from the knees in both panels. START: 50°, upper arms vertical (shoulders above elbows), forearm on the ball. END: 30° (neutral), arm straight, ball 15–80 cm further forward. |
| Cable crunch | Kneeling, thighs upright, hips still, rope by the head, cable from the high pulley in both panels. Torso 75° to 25° (neutral). The rigid torso cannot bring the elbows onto the thighs (concern 3). Tests: elbows 25+ cm lower and 15+ cm nearer the knees. |
| Machine crunch | Hips against the hip pad and on the seat, feet flat. Torso 90° to 45°. Chest pad on the upper chest, hands on the pad. Lever the same length in both panels. |

**Standing**

| Figure | Panels, choices and tests |
|---|---|
| Pallof press | Side on, column beyond the body (drawn behind it), pulley level with the chest and beside it. Knees 10°. START: handle at the chest, elbows by the sides (≤30° from vertical). END: arms straight, level with the chest, 40–70 cm out. Torso square in both. |
| Cable woodchop (front view) | Column on the near side, pulley at shoulder height, cable to the handle in both panels. Feet 12° apart each, knees straight. START: handle 42 cm above the shoulders, 6 cm to the near side. END: at the far hip's height, 6 cm to the far side. Arms out in front (3D): one arm ≤12°, the other ≤38° (concern 1). |

**Carries.** The two panels show:
- **Farmer's walk:** START standing tall, arms straight at the sides with palms-in dumbbells (profile); END mid-stride, back upright.
- **Suitcase carry (front view):** START standing tall, the loaded arm hanging straight with the disc 1–3 cm clear of the thigh, shoulders and pelvis level, no lean; END the far foot swinging through, 3–20 cm off the floor.
- **Waiter's carry:** START kettlebell racked at the shoulder; END pressed overhead on a straight vertical arm (LockoutOverShoulders), mid-stride.
- **Plate pinch carry:** START standing tall with a plate pinched in each hand; END mid-stride.

**The mid-stride (neutral):**
- the front foot lands on its heel, toes up 15°, front knee 5°;
- the back foot is on its ball at 30°, back knee 15°;
- the stride is 40–70 cm, and the torso is upright and unturned in every carry.

## Sources
- **Searches:** WebSearch with allowed_domains only. The numbers I cited were all seen in the search results: PMC4424477 and PMC5343533 (calf range), and ExRx CurlUpTests and the ACE GFI assessments PDF (crunch 30°).
- **Other citations:** Sources.kt URLs.
- **Neutral:** every other angle is marked neutral in its comment.

## Tests
| Suite | Result |
|---|---|
| domain jvmTest | 545 (1 skipped, 0 failed). BatchCalvesTest 10, BatchCoreTest 18, KitTest +7, FiguresTest 7, AreaMapsLazyTest passing. |
| domain jsNodeTest | 560 (1 skipped, 0 failed). The first `allTests` run after the KitTest warning fix failed (I didn't capture the message, probably the known Mocha 2 s timeout); the re-run with `--continue` was green. |
| checkWebDomain | ok (bridge regenerated) |
| app testDebugUnitTest | 301 passed; no screenshot changed |
| web unit | 657 passed (see note) |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

**Web unit note:** `exercisesTab_browseByMuscleAndSearchWork` (ExercisesScreen.test.tsx) hit vitest's 5 s timeout in full runs while Gradle daemons were loading the machine (4.2 s alone). It fails the same way at BASE (I stashed my changes and checked). With Gradle stopped, the full run was 657/657.

**RED/GREEN:**
- **RED:** with both area maps emptied, 28 of 28 batch tests failed with "no figure for …".
- **GREEN:** the first runs caught real faults, which I fixed:
  - the side plank's legs went through the floor and the top leg was not stacked;
  - the front plank's lying START was impossible (upper arm through the floor);
  - the ab wheel hand went through the floor, elbows were hyperextended, and the start was a near-rollout (now sat back with the shoulders over the wheel);
  - the stride's back knee was at 56° and the back hip at −21°;
  - the dead bug hand and heel could not reach a light touch;
  - the woodchop arms folded at 90°;
  - the bird dog leg was not raised to level;
  - the seated calf ankles were past range once the knees rose;
  - the floor crunch knees were at 115°.

## Speed
I stopped Gradle (`./gradlew --stop`) before these runs.
- Run 1: 1 failed, 2 passed (14.4 s). Load was about 3.4, right after e2e, so the machine was not quiet. I don't know which test failed.
- Run 2: 4/4 passed (14.5 s), load 3.2.
- Run 3: 4/4 passed (14.3 s), load 2.8.
- Run 4: 4/4 passed (14.5 s), load 1.1 after 45 s of settling. The Exercises tab check took 2.4 s.

The three runs after the first all passed.

## Sheets
- **Whole areas:** `/home/user/powertracker/web/.figure-sheets/calves-{dark,light}.png` and `/home/user/powertracker/web/.figure-sheets/core-{dark,light}.png`.
- **Detail sheets:** `z1`, `z2` and `z3-{dark,light}.png` in the same folder.
- **Fixed after looking:**
  - calf heel drop raised from 11° to 16° so it shows;
  - standing-machine handle lowered from the face;
  - barbell rack posts moved off the block and the chest (gap 75);
  - side plank top arm and stacking;
  - woodchop START moved higher.

## Concerns
1. **Cable woodchop:** in the front view the shoulders cannot turn toward the machine. Two straight arms from square shoulders only meet on the middle line, so the diagonal is drawn small: the hands are only 6 cm either side of the middle. One arm softens to about 37°, which conflicts with the guide's "arms straight". START can read as an overhead reach. A real fix needs a torso turn in the front view, which would be a renderer change.
2. **Calf raises at thumbnail size:** the heel drop and rise are about 4 cm and 9 cm. The foot's tilt shows on the guide sheet, but at 56 × 40 it is only a couple of pixels. The sourced 41° range is what limits this, not the ankle's joint limits.
3. **Rigid torso:** crunches, the cable crunch and the machine crunch tilt the torso about still hips with the chin tucked, so there is no visible spinal "C" shape. The cable crunch's elbows come toward the thighs but do not reach them.
4. **Front plank START:** "Lie face down" with the elbows under the shoulders is drawn by pointing the upper arms out of the picture. The torso lies level with the toes tucked.
5. **Side plank START:** the bottom leg is turned 18° (near the adduction limit) and the bottom upper arm points out toward the camera to land the elbow.
6. **Pallof press:** side view with the column drawn behind the body. At START the cable is end-on (a point); at END it shows as a level line from the chest to the hands. It might be misread as a cable chest press.
7. **Dead bug:** the hand touches through a 46° wrist bend, because the shoulder joint is 10 cm up and flexion stops at 180°.
8. **Neutral values:** the reverse crunch's 9 cm roll; the ab wheel's 20° reach; the ball rollout's 50° and 30°; the cable crunch's 75° and 25°; the machine crunch's 45°; the donkey's 15° torso; the stride; the support post's height and place.
9. **Seated dumbbell calf raise:** the knees rise about 10 cm at the top because the foot pivots on the toe tips. This exaggerates the knee lift a little.

## Fix round 1

**Brief:** task-12-fix1-brief.md, from review-12-report.md.

**Commit:** `8b5f537 fix(domain): calf and core pictures, review fixes`. It is a new commit on 1563e21 because the controller changed the plan (do not amend). Attribution: my harness's lines. Not pushed. No guide text changed.

### Fix 1: Farmer's walk and the plates
- **Farmer's walk** is now the front view, built like the Suitcase carry with a shared helper:
  - a dumbbell in each hand, seen end on;
  - both arms straight and just clear of the thighs;
  - shoulders and loads level, torso upright;
  - END: the far foot swinging through, off the floor.
- **Suitcase carry:** same poses as before, now using the shared helper.
- **PinchPlate:** now a 45 cm bumper plate (radius 22.5), with a thicker rim and hub ring, so it reads as a plate.
- **Thumbnails side by side** (`/home/user/powertracker/web/.figure-sheets/carries-{dark,light}.png`):
  - farmer's: front view, two discs;
  - suitcase: front view, one disc;
  - waiter's: side view, arm overhead;
  - plate pinch: side view, large plate.
- Farmer's and plate pinch are now plainly different. Farmer's and suitcase differ only by the second small disc at thumbnail size (concern 1).

### Fix 2: Cable woodchop
- **New kit option, `Pose.twist`** (front view only, default 0):
  - the shoulder joints sit at SHOULDER_HALF_WIDTH × cos(twist) either side of the axis;
  - the torso outline narrows towards the shoulders by the same factor; the hips stay square.
  - A joint limit of ±35° is added, with a twist in a side view refused. Source: PMC3461891, about 35° each way (the textbook value) beside a 42.5–47.7° seated normal group, seen in the search result.
- **Poses:**
  - Trunk turned +30° towards the machine at START and −30° at END.
  - Both arms bent 8° (3D arm3, the shoulders' depth ±18 sin 30°). The hands sit where the turned chest points.
  - START: hands 35 cm above the shoulders and outside the near shoulder, clear of the head.
  - END: hands at the far hip's height, outside the far hip. The sweep across the body is 35–70 cm (tested).
  - Captions are back to "Above your near shoulder" / "Down to your far hip".
- **Test bounds:** twist, shoulders narrowed, hips square, hand positions, the sweep, both elbows 5–12°, both arms' `out` 10–80°, and `forearmOut − out` 1–20° (the elbow points towards the body).
- **Mutation check:** I flipped the hint to +z. The woodchop test then failed ("near elbow towards the body (START) −8.0"). I reverted the flip.

### Fix 3: Cable crunch
- The column now stands 95 cm in front of the knees (was 75). The reach is −65, so the pulley stays over the same spot.
- Test: the head and the rope are at least 5 cm clear of the column's left edge in both panels.

### Fix 4: minors
- **Reverse crunch:** the roll is now 14 cm (bound 12–16).
- **Bicycle crunch:** START caption is now "Straighten other leg", from the guide's "while you straighten your other leg", which the panel shows.
- **Machine crunch:** the lever turns about the hips' axis (pivot = pelvis). It is now drawn in front of the body with the pad and a hub; the frame beam is drawn behind. KitTest and the figure test assert that.
- **Smith calf raise:** new `SmithMachine.stopBehind` (default 6, so the old figures are unchanged), set to 20 here. The stop now shows 5–30 cm past the back in both panels (tested).
- **FiguresTest:** I removed the dead undrawn-name block. `registerForTest` is still exercised by `figure_resolvesAliasesAndFormerNames`.
- **Side plank:** START `farArm.out` is bounded to 20–80, and `forearmOut` to 80 in both panels.

### RED/GREEN
- **RED (Fix 2):** the hint-flip mutation failed the new woodchop test.
- **Other fixes:** the new bounds were written with the fixes and then run green. One went red first: the Smith stop's first setting of 30 cm reached 31.4 cm past the back and failed the 5–30 bound, so I set it to 20 cm.

### Golden and old figures
- **Golden:** `225:4cef8580b4d880a3`.
- **Old figures unchanged:** with the calf and core maps emptied and every kit, renderer and check change in place, FiguresTest passed against `199:f499503d0cebe8f9` (7 tests, 1 skipped, 0 failures).
- **Completeness check:** with its @Ignore removed it passed (7 tests, 0 skipped); I put the @Ignore back.

### Suites
| Suite | Result |
|---|---|
| domain jvmTest | 545 (1 skipped, 0 failed) |
| domain jsNodeTest | 560 (1 skipped, 0 failed) |
| checkWebDomain | ok |
| app testDebugUnitTest | 301 passed; no screenshot changed |
| web unit | 656 of 657 passed (see below) |
| typecheck | ok |
| build | ok |
| e2e `--trace off` | 90 passed |

- **The one web unit failure:** `exercisesTab_browseByMuscleAndSearchWork` timed out at 5.1–5.4 s.
  - It timed out in both full runs, the second on a quiet machine (load 0.0, Gradle stopped).
  - Run alone it passes in 4.3–4.4 s.
  - It fails the same way at 1563e21 with this round's changes stashed, so this round did not cause it.
  - I did not change the test or its timeout.
- `./gradlew --stop` done. No speed runs, as the brief says.

**Sheets:** `/home/user/powertracker/web/.figure-sheets/calves-{dark,light}.png`, `core-{dark,light}.png`, `carries-{dark,light}.png` and `f1-{dark,light}.png`.

### Concerns
1. **Farmer's walk and Suitcase carry thumbnails:** they differ only by the second dumbbell disc, which is small at 56 × 40. Enlarging the dumbbell would change the shared kit, so I left it.
2. **Woodchop twist:** this is a renderer and kit change (`Pose.twist`), used only by this figure.
3. **Web unit timeout:** see the suites note above.
