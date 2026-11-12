# Task 11 report: hamstring and glute pictures (27 new figures)

**Status:** DONE_WITH_CONCERNS.
- **Commit:** `2091c06 feat(domain): hamstring and glute pictures`, on top of b0a8382 (the controller's docs commit, as its message asked; BASE 8ed18d4 below it). One commit, local, not pushed. Attribution: my harness's lines (Co-Authored-By: Claude Opus 5.5 plus the session line), as the brief allows.
- **Count:** `BodyArea.HAMSTRINGS_GLUTES` has 28 catalogue entries. RDL already had a picture, so this task adds 27. They match the brief's list.
- **Golden:** `199:8d220156333cb4a8`.
- **Proof that the 172 old figures did not move:** I cut the area map back to RDL only, with every kit, renderer and check change in place. FiguresTest then passed against the old golden `172:84a1fde1ff223652` (7 tests, 1 skipped, 0 failures).

## Shared code changed (no existing drawing changes)
- **Kit.kt:** wires the new pieces into `surfaces`, `solids`, `draw` and `drawFront`. It also adds AnkleStrap to the held pieces' `center` and `draw`.
- **Figure.kt:**
  - new surfaces BALL, HANDLE (a grip surface), KNEE_PAD and HIP_PAD;
  - new body points NEAR_THIGH and NEAR_SHIN, so a one-leg machine's pad can be a contact.
- **FigureRenderer.kt:** an AnkleStrap counts as the cable's end. The cable now runs to the cuff.
- **FigureChecks.kt:** AnkleStrap's radius in the held-piece `when` is 0.
- **KitShoulders.kt:** new `KettlebellHold.SWING`. The bell's centre sits `SWING_DROP` (15.5 cm) past the grip, along the forearm. RACK and OVERHEAD are unchanged.
- **Not changed:** the renderer's draw order, the joint limits and the existing checks.

## Kit (KitHamstrings.kt)
Each piece has a typical-size comment with units. KitTest gained 9 cases and now has 62.

| Piece | What it is |
|---|---|
| MiniBand (KNEES / ANKLES) | A strip between the legs, 7 cm above the knee or 6 cm above the ankle. Drawn in front, with a gap outline. |
| StabilityBall | 65 cm; ACE "Play Ball!" chart gives 65 cm for 5'8"–6'3". Surface BALL; solid. |
| AnkleStrap (held) | Cuff on the lower shin, 5 cm above the ankle, drawn in the accent. |
| NordicStation | 6 cm floor pad (KNEE_PAD) and an ankle roller (LEG_BRACE) on a post behind the toes. |
| ReverseHyperBench | 95 cm pad, 45 cm long, two posts clear of the feet, handles under the head end. No pendulum is drawn. |
| LyingLegCurlMachine | Level pad 60 cm up. Lever from the knee pivot to the calf roller, drawn in front. Handles. |
| SeatedLegCurlMachine | Seat, reclined back pad, calf roller and lap pad over the thighs. Lever and pads drawn in front. Side handle. |
| StandingLegCurlMachine | Post, thigh pad, lever to the calf roller (in front), handles. |
| HipThrustMachine | End-on back pad, 5 cm platform, and a lever from a pivot to a hip roller. The roller is drawn in front. |
| HipMachine (front view) | Seat, back pad and foot bars. Handles at the grips. Knee pads outside the knees (abduction) or inside them (adduction), drawn in front; levers behind. |

Reused unchanged:
- FlatBench, 30 cm long seen end on, as the hip thrust bench;
- HyperBench at 0°, as the glute-ham machine;
- CableStack, PowerRack (`rackInFront`), Barbell, Dumbbell and Kettlebell.

## Per figure (comments in figures/areas/HamstringsGlutes.kt)
**Camera:**
- **Side view** for everything that bends at the hips and knees: the hinges, swings, curls, Nordic, glute-ham raise, bridges, thrusts, reverse hyper and quadruped.
- **Front view** for the six abduction and adduction figures (lesson 9): the two machines, the band walk, cable abduction, side-lying abduction and the clamshell. The file header gives the reason for each.
- The side-lying pair are seen from in front of the body, as Side-lying external rotation is, so the top leg or knee rises in the picture.

**Hinges and swings:**
- **Stiff-leg deadlift:**
  - The RDL's own torso solver, with knees at 8° (neutral) against the RDL's 18°.
  - The END torso sits lower than the RDL's; the test asks for at least 4° lower.
  - Bar at the knees, over the mid-foot and close to the legs. No rack: the bar comes from the floor.
- **DB RDL:**
  - The RDL's 18° knee bend, with end-on dumbbell discs just clear of the thighs and knees (lesson 2).
  - No rack.
- **Single-leg RDL:**
  - START: both feet down, knees 10°.
  - END: standing on the far leg (knee 18°, shin 8°); the near leg straight and in line with the body; torso 3° above level.
  - Both arms hang vertically (lesson 11). The dumbbell ends about 14 cm off the floor (concern 1).
- **Good morning:**
  - Bar on the upper back, with the Squat's arms.
  - START: tipped over the ankles (shins 12°), torso solved so the bar is over the mid-foot.
  - END: torso 3° above level, knees 22°. The shins tip back up to 10°, and the bar ends over the forefoot.
  - Rack with J-hooks and safety arms (the guide's safety line). The posts clear the head in both panels.
- **Kettlebell swing:**
  - Bottom: knees 22°, shins 4°. The torso is solved so the straight arm's wrist lands on the thigh line 55% of the way down: ExRx KBTwoArmSwing, "forearms make contact with inner thighs permitting kettlebell to swing back under hips".
  - The bell is under the hips, behind the knees.
  - Top: standing tall, arms straight, hands at chest height (the guide's lower height).
- **Single-arm kettlebell swing:**
  - The same legs and bell arm.
  - The free arm swings back at START (−30°, elbow 15°) and forward and lower at END.
  - Its thumbnail is START, and the two-arm swing's is END (lesson 10).

**Leg curls:**
- **Shared roller:** `calfRoller` puts the roller on the calf side, touching it, 80% down the shin (85% for the seated curl). The lever is the same length in both panels; the tests check this.
- **Lying leg curl:**
  - Torso level, thighs tipped 4° to the pad, knee 4 cm past the edge, knees 0° to 115°.
  - Handles past the head end, low.
  - Cites ExRx LVLyingLegCurl ("raise lever pad to back of thighs by flexing knees").
- **Seated leg curl:**
  - Seat 50 cm, torso 100°. The back pad is hinged where the torso's back meets the seat, so it touches the back.
  - Lap pad above the knees; knees 2° to 100°.
  - Cites ExRx LVSeatedLegCurl.
- **Standing leg curl:** torso 60° (neutral), the far leg standing, the near thigh 15° forward, knees 0° to 115°.
- **Cable leg curl:**
  - The far leg stepped back, straight and in line with a torso at 80°.
  - Arms straight to the stack, working foot 1–8 cm off the floor, knees 0° to 120°.
  - Low pulley, with the cable to the cuff in both panels (lesson 3).
- **Stability ball leg curl:**
  - START: a straight line from the shoulders, the ball tangent to the shins 80% down.
  - END: knees 100°, torso in line with the thighs, ball rolled in under the soles.
  - Head on the floor (the neck bent to the tilt). Arms out to the sides on the floor, drawn foreshortened (out 60°).
- **Nordic curl:**
  - Kneeling on a 6 cm pad, toes on the floor, roller over the ankles.
  - START: upright, knees 90°, arms out to the sides (out 55°, elbows 20°).
  - END: 30° above level, knees 30°, elbows 10°, fingertips on the floor. Neutral angles.
- **Glute-ham raise:**
  - The level HyperBench, knees at 95 cm. Knees on the near end of the pad, shins level, soles on the plate, roller over the calves.
  - Knees 90° to 0°, torso 90° to 0° ("level, no further"). Arms crossed.

**Bridges and thrusts:**
- **Bridges (`bridgeBase`):**
  - Shoulders and head on the floor.
  - At the top, shoulders, hips and knees are in one straight line, with the shins 8° back from vertical (neutral, "near vertical").
  - The feet are set so the shoulders don't slide. Knees: START about 124°, END about 107°.
- **Single-leg glute bridge:** the near leg straight and in line with the hips in both panels; the planted leg is the two-leg bridge's.
- **Barbell glute bridge:**
  - Bar over the hip crease, hands on it, with the elbows out of the picture (`arm3`; an in-plane bend went through the floor).
  - START: hips lowered until the 45 cm plates rest on the floor; the hips are about 1 cm off it.
- **Hip thrust:**
  - 43 cm bench seen end on. The torso's back touches the bench edge; the torso turns about it.
  - END: torso level, shins vertical, line bend at most 6° (about 4°: the thigh tips 4° down to a knee whose shin is vertical).
  - START: tilted until the plates rest on the floor.
- **DB hip thrust:** the same poses, with end-on discs resting on the hips.
- **Machine hip thrust:**
  - The same body; feet on a 5 cm platform; a hip roller with the hands on it.
  - The pivot is on the two pad positions' perpendicular bisector, 18 cm past the ankle, so the lever is the same length in both panels.

**Hip extensions:**
- **Reverse hyper:**
  - 95 cm bench, hips 8 cm past the edge, torso level, hands on the handles.
  - Hip 90° (legs hanging, feet 12 cm clear of the floor) to 6° ("nearly in line"; neutral).
- **Quadruped hip extension:**
  - Knees under the hips, hands under the shoulders, wrists 60° (the push-up hand).
  - The torso tilts up to the head by about 20° (concern 3).
  - END: the near hip at 0° (thigh in line with the back), knee kept at 90°.

**Front view:**
- **Machine abduction and adduction:**
  - Thighs 5° down; drawn projected (`seatedLeg`); shins hang vertically.
  - Abduction 0° → 30°, with pads outside the knees.
  - Adduction 30° → about 6°, where the two inside pads meet.
  - The thumbnails are the legs apart (abduction) and together (adduction), so the two differ (lesson 10).
- **Lateral band walk:**
  - Hips and knees slightly bent (thigh out 15°, torso lean 15°), band above the knees.
  - Legs at 3° then 12°: the feet step about 28 cm wider.
- **Cable hip abduction:**
  - Pulley low on the standing leg's side; the far hand on the frame.
  - The cuffed leg goes from −12° in and 10° forward (crossed in front, foot 1–6 cm off the floor) to 30° out.
  - Pelvis level. ACE's "stop when hips tilt" (about 45° at most) is cited; 30° is the conservative side.
- **Side-lying hip abduction:**
  - Bottom leg resting on the floor; top leg resting on it, then lifted by 30°, with the knee straight and not turned.
  - Head on the bottom arm; the top hand on the floor in front of the chest.
- **Clamshell:**
  - Hips 45°, knees 90°, feet together. PMC3418110: "hips at 45° and knees at 90°", seen in the search result.
  - END: the top thigh turned 40° (neutral) about the line from the hip to the foot. The knee rises 12–25 cm and the feet stay put.
  - Mini band above the knees.

**Sources:**
- Cited only as Sources.kt URLs, except:
  - ACE Play Ball! (ball size);
  - PMC3418110 (clam set-up);
  - the quoted lines on ExRx KBTwoArmSwing, LVLyingLegCurl and LVSeatedLegCurl, each seen in the search results.
- I removed three statements I had first written but not verified in a result: an ACE single-leg RDL torso quote, ExRx's stiff-leg "straight or slightly bent", and an ExRx swing knee quote. Those positions are now marked neutral.

## Tests
| Suite | Result |
|---|---|
| domain jvmTest | 510 (1 skipped, 0 failed): BatchHamstringsGlutesTest 28, KitTest 62, FiguresTest 7, AreaMapsLazyTest 1 |
| domain jsNodeTest | 525 (1 skipped, 0 failed) |
| checkWebDomain | ok (bridge regenerated) |
| app testDebugUnitTest | 301 passed; no screenshot changed |
| web unit | 657 passed |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

**BatchHamstringsGlutesTest (28 tests):**
- every figure passes FigureChecks;
- each has the camera given above;
- each has real bounds, legs everywhere (lesson 5): knee bends per panel, torso angles, shins, feet flat or off the floor by a range, contacts, loads (bar/dumbbell/bell position), levers of equal length, pivots on the knee, rollers along the shin, the straight-line bend at bridge and thrust tops, abduction angles, pad positions, and the free arm and back leg poses (lesson 11).

**RED/GREEN:**
- **RED:** with the map cut to RDL, 28 of 28 batch tests failed with "no figure for …", and FiguresTest was green on the old golden.
- **GREEN, first full run, 5 fails:**
  - hip thrust hands apart (the far arm wasn't copied);
  - the seated curl had no HANDLE surface;
  - the bridge hips were 1.5 cm off the floor;
  - the barbell bridge elbows went through the floor (fixed with `arm3`);
  - the quadruped wrist was past its limit;
  - the side-lying feet went into the floor.
- **GREEN, then focused tests:** the GM standing torso (70° had been the range edge; now tipped over the ankles, about 80°), the seated curl sign, the Nordic fingertips (now an arm swung to touch), the GM bar past the toes (the shin range widened to −10°), and the clamshell flexion bound (40–47°).

## Speed
Run after `./gradlew --stop` and about 45 s of settling; machine load about 2.4.
- Run 1: 4/4 passed (14.2 s).
- Run 2: 4/4 passed (14.7 s).
- Run 3: 4/4 passed (14.6 s).

## Sheets
- **Whole area, all 28:** `/home/user/powertracker/web/.figure-sheets/hamglutes-dark.png` and `hamglutes-light.png`.
- **Detail sheets:** h1–h7 (`/home/user/powertracker/web/.figure-sheets/h1-light.png` … `h7-dark.png`) and `hamglutes-adduction-*.png`.
- **Fixed after looking:**
  - the machine thrust's pivot far behind the bench;
  - the lying curl's arm folded under the bench;
  - the reverse hyper's folded arm;
  - the hip machine's levers crossing the shins, and its back pad hiding the far thigh;
  - the adduction thumbnail matching abduction's;
  - the GM rack touching the face at START.

## Concerns
1. **Single-leg RDL** stops with the torso level and the dumbbell about 14 cm off the floor. The guide says "until the weight reaches the floor"; reaching it needs the torso below level or a deep knee bend. This is the conservative stop, marked neutral.
2. **Good morning END:** the bar is over the forefoot, not the mid-foot, and the shins tip back about 10°. With the torso level, a rigid figure can't get the hips further back. There is no BarOverMidfoot check at END.
3. **Quadruped:** the rigid torso tilts about 20° up to the head, because of the 60° wrist limit and the arm and thigh lengths. The back is straight but not level.
4. **Clamshell START** reads as short, straight, stacked legs. From the front, the 90° knee bend is hidden in depth. The END V-shape shows it.
5. **Seated hip machines:** the renderer draws a front view's far leg before the machine, so the seat overlaps the far thigh's stub a little.
6. **Hip thrust START:** the torso is steep against the bench edge, and the head sits high.
7. **Neutral angles:** the curl tops (115°, 100°, 120°), the Nordic's 30°, the 30° abduction, the clamshell's 40°, and the GM's 22° knees.
8. **Reverse hyper:** no pendulum is drawn (the bodyweight bench).

## Advisor
No advisor tool was available in this session.

## Fix round 1

Brief: task-11-fix1-brief.md, from review-11-report.md. I amended 2091c06; the commit is now `37843b9`, still on b0a8382, with the same message and attribution. No guide text changed.

- **Fix 1, Single-leg RDL:**
  - **Pose:** the dumbbell now rests on the floor at END, 0.5 cm clear (test bound 0..3). The standing knee bends 35° with the shin 12° forward. ExRx's DBSingleLegStiffLegDeadlift page, seen in an allowed-domain search result, says to lower the weights toward the floor and to bend the supporting knee more if needed; it is in Sources.kt. The torso angle is solved so the weight touches down: about 16° below level, the back straight, the free leg straight and in line.
  - **Why 16°, not "a few degrees":** the drawn hand cannot curl round the handle, so it would poke 3.4 cm past the disc and through the floor. I flexed the weight hand's wrist 48° to stop that, which costs about 3°. Even with no wrist flex, a 30° knee needs about 13°.
  - **Free arm:** bent 40° at the elbow, so its hand clears the floor (lesson 11).
  - **START caption:** now "Knees slightly bent".
  - **Test bounds:** standing knee 25..35, torso −18..0, elbows under the shoulders, free hand 1..30 cm off the floor.
- **Fix 2, Quadruped hip extension:**
  - The elbows are softened, bent back towards the knees, using the least whole-degree bend that brings the back's slope down to 15° (`QUADRUPED_SLOPE`).
  - The wrists are still straight under the shoulders, solved per bend.
  - I also replaced the brute-force searches with bisection (Chest.kt's `solveAngle`). The first version timed out the JS test at Mocha's 2 s limit.
  - **Test bounds:** slope 5..16, elbow 0..35, elbow behind the wrist, both arms the same.
  - **Before:** the old poses measured 24.5° on this test (the RED run below).
- **Fix 3, Clamshell START caption:** now "Feet together". The guide says "Keep your feet together.", and the test asserts the caption.
- **Fix 4, Good morning rack:**
  - Posts are 30 cm past the face in both panels.
  - The safety arms sit at about head height when the torso is level, so they now end 6 cm in front of the face at the bottom. They still run below the bar's lowest height, and like the other racks' arms they stop short of its path.
  - **Tests:** the posts 25..60 cm clear of the head, and the head at least 5 cm from the safety arm.
- **Fix 5, minor items:**
  - **Hip machines:** HipMachine has a new `handle` grip, the far one mirrored. It adds HANDLE surfaces, posts drawn behind the body, and grips drawn across the hands in front. Both hip machines get a HANDS-on-HANDLE contact, and the tests bound each grip to 0.5 cm of its handle.
  - **Reverse hyper:** the handle's grip is now drawn end on, in front of the hand, with a gap ring (`drawFrontOf`). The test bounds the grip to 0.5 cm of the handle.
  - **`handsOn` arms** (the three thrusts and the barbell bridge): the new `assertElbowsOut` requires the upper arm 20..80° out towards the camera, the elbow bent 20..140°, the elbow above the floor, and the elbow clear of the bench's pad.
  - **Nordic START elbow:** bounded 15..25.
  - **Clamshell citation:** 45° hips now cites PMC4595911 (its search result text listed the clam variants at 45° hips). The 90° knees are marked neutral, and the PMC3418110 citation is gone.

### RED/GREEN
- **RED:** the new tests run against 2091c06's figure file failed 6 of 28:
  - Good morning: posts 12 cm from the head;
  - Single-leg RDL: caption and knee;
  - Quadruped: slope 24.5;
  - Clamshell: caption;
  - both hip machines: no HANDS-on-HANDLE contact.
- **Already passing before the fix:** the reverse hyper's hand bound and the `handsOn` elbow bounds. They now guard against those faults coming back.
- **GREEN:** 28 of 28.

### Golden and tests
- **Golden:** `199:f499503d0cebe8f9`.
- **Old figures unchanged:** with the area map cut back to RDL, the fingerprint is `172:84a1fde1ff223652`.

| Suite | Result |
|---|---|
| domain jvmTest | 510, 1 skipped |
| domain jsNodeTest | 525, 1 skipped (see note) |
| checkWebDomain | ok |
| app testDebugUnitTest | 301, no screenshot changes |
| web unit | 657 |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

Note on jsNodeTest: one run hit the known GoldenScenarioTest Mocha 2 s timeout; the re-run with `--continue` was green.

`./gradlew --stop` done; no speed runs, as the brief says.

**Sheets:** `/home/user/powertracker/web/.figure-sheets/hamglutes-{dark,light}.png`, re-rendered, plus `f1-*.png` and `f2-*.png`.

### Not done / concerns
- The Single-leg RDL's torso is about 16° below level, not "a few degrees". To touch the floor without a deeper knee bend, the figure needs about 13–16°.
- The Quadruped's softened elbows are nearer 30° than "slightly"; that is what a 15° slope costs with these proportions.
