# Task 9 report: arm pictures (31 new figures)

**Status:** DONE_WITH_CONCERNS. Base 779d924, then the controller's docs commit e3c219f. One commit on top of e3c219f: c010525 `feat(domain): arm pictures`. It is local and not pushed.

**Count:** the catalogue has 31 `BodyArea.ARMS` entries (library/areas/Arms.kt). None had a figure, so all 31 are new. JM press is one of the 31: it is in the Arms catalogue, although the brief's list did not name it. The brief's "French press" and "standing dumbbell" are aliases of Skull crusher and DB overhead triceps extension. `BatchArmsTest.arms_everyExerciseHasAFigure_thatPassesItsChecks` checks three things: there are 31 names, every one passes FigureChecks, and every one is drawn from the side.

**Golden:** `145:f9b0a07e5c64b174`. To prove no existing figure moved, I emptied `armFigures` after every kit and Chest.kt change. FiguresTest then passed against the old golden `114:817892063d845bbc` (7 tests, 1 skipped, 0 failures). I ran this twice: once at the RED run, and again after the last kit change.

**Attribution:** the commit ends with my harness's lines (Co-Authored-By: Claude Opus 5.5 and the session line). The brief allows this when the harness gives different lines.

## Where the code is
- All the figures are in `figures/areas/Arms.kt`. The camera choice is explained in its header.
- Every figure is a `private val x by lazy`. The map `armFigures` is a `by lazy` map of `lazy { }` entries. AreaMapsLazyTest passes.
- The new kit is in `figures/KitArms.kt`.

## Changes to shared code
No existing figure's drawing changed; the golden proves it.
- **Chest.kt:** these went from `private` to `internal`, with no other change: `SIDE_ARMS_DOWN`, `FLAT_TORSO`, `FLAT_PELVIS`, `flatLegs`, `flatBare`, `flatPose`, `LYING_CONTACTS`, `PUSH_UP_WRIST`, `Support`, `pronePose`, `handOn`, `pushArm`, `handBottom`, `solveAngle` and `CHEST_GAP`. This lets the lying extensions reuse the approved lying pose, and the close push-ups reuse the push-up builder.
- **Kit.kt:**
  - `Dumbbell.squareToForearm` (default false). With `neutral`, the side view's profile runs square to the hand, not along the torso.
  - Wiring for the four new pieces: surfaces, solids, drawing, drawFront, center and draw.
- **Figure.kt:** new `Surface.ARM_PAD`.
- **FigureChecks.kt:** bar radii for the EZ bar and the overhead dumbbell. Both count as two-handed for the held check.

## Kit (KitArms.kt)
Each piece has a typical-size comment with units. KitTest has 5 new cases, 47 in the class.

| Piece | What |
|---|---|
| `EZBar` | A 120 cm bar, drawn with 10 kg iron plates 30 cm across. From the side it is end-on like a barbell, so the smaller plates tell it apart from the barbell's 45 cm plates. The front view draws the zigzag middle. |
| `OverheadDumbbell` | One dumbbell in both hands, under the top plate. Its handle hangs straight down from the hands, so it shows in profile. |
| `PreacherBench` | A 30 cm seat, and a 35 cm pad sloped 45°. The pad's block is 14 cm deep under its top face, so it shows below the arm in front of the chest. Only the seat is an obstacle. The pad is an ARM_PAD surface. |
| `ArmPadMachine` | Used for both the curl machine and the triceps extension machine. It has a seat, an arm pad, a 150 cm stack column and a lever. The lever turns about a pivot at the elbows and reaches to an end-on handle. It is drawn in front of the forearm with gap outlines. |
| `Dumbbell.squareToForearm` | The palms-in profile for hammer grips. |

## Per figure (camera side for all; sources in each code comment)
Cameras: every figure is a side view. The reason is in the Arms.kt header: elbow flexion and extension happen in the sagittal plane.

Grip rule:
- **Palms in (neutral):** the dumbbell is drawn in profile, square to the forearm.
- **Palms up or down:** the dumbbell is drawn end-on.
- **Guide gives no grip:** the default end-on drawing (DB lying extension, kickback), as the back batch ruled.

**Standing curls**

The standing curls share one top position (CURL_TOP): the upper arm 8° forward of hanging and the elbow at 145°.
- That leaves the forearm 27° short of upright.
- ExRx BBCurl lets the elbows travel forward slightly at the top, so the forearms end up near vertical and no further. I saw this in the search result.
- The figure's elbow limit is 150°, and the guide asks for the elbows at the sides.

The figures:
- **Curl:** starts with the bar in front of the thighs.
- **EZ-bar curl:** the same poses with the EZ bar.
- **Reverse curl:** the same as the Curl from the side. Its captions are "Lined up with shoulders" and "Forearms upright", because the turned palms cannot show from the side (lesson 7).
- **DB curl:** starts palms in (profile). At the end one arm is curled with the palm up (end-on), and the other arm hangs.
- **Hammer curl:** a staggered stance, both arms together. Palms in, in profile, in both panels. So it differs from the DB curl: its end dumbbell is in profile where the DB curl's is end-on.
- **Cable curl, Cable hammer curl, One-arm cable curl:** facing a low pulley 50 cm in front. The cable runs from the pulley to the hand in both panels. They use the straight bar, the rope and a single handle.
- **Drag curl:** the end panel is the guide's "once your forearms pass level". The bar is on the upper abdomen (a BarTouches check), the elbows are 5–20 cm behind the back, and the forearm is about level. The top of the drag curl is the same as the Curl's, so it would not tell them apart.

**Seated, incline, prone**
- **Seated DB curl:** on the end of a flat bench, legs together, the end panel as the DB curl.
- **Incline DB curl:** the bench at 60°. ExRx DBInclineCurl gives 45–60°; 60° is the conservative end. The upper arm hangs straight down, 30° behind the torso's line, in both panels. At the end the elbow is at 145° and the palm is up. The forearm is then 35° short of upright: that is the limit with a vertical upper arm.
- **Spider curl:**
  - It uses the approved chest-supported row's 30° incline, body and trailing legs. The pad ends 5 cm past the chest, so "shoulders near the top" shows.
  - It uses the EZ bar. The upper arms are vertical in both panels.
  - The end panel is curled to 145°, "as far as your elbows bend".
- **Concentration curl:** the elbow rests on the near thigh, 8 cm above its centre line near the knee. The torso lean is solved from that. The end panel is 145°, towards the front of the shoulder. From the side, the inner thigh cannot be told from the outer one, so the caption is "Arm against your thigh".
- **Wrist curl and Reverse wrist curl:**
  - The forearms lie along the tops of the thighs, with the wrists 4 cm past the knees. The forearm is the same in both panels; only the near hand bends, 50° down, then 50° up.
  - Both use a dumbbell in one hand, the guides' "or hold a dumbbell in one hand". A 45 cm plate by the knees would cover the legs.
  - The figure measures the wrist with + meaning up. So for the palms-down hand, up is extension. 50° keeps both inside the smaller 60° limit.

**On an arm pad**
- **Preacher curl** (EZ bar) and **DB preacher curl:**
  - The pad is at 45°. ExRx's standing preacher curl on an incline bench gives 45–55°; I saw this in the search result.
  - The armpit is 6 cm from the top of the pad. The elbows rest on the pad (an ELBOWS on ARM_PAD contact).
  - The arm goes from straight to a 135° bend, which puts the forearm exactly upright.
  - In the DB version the free arm rests straight on the pad.
- **Machine curl:** the pad is at 40° (neutral). The bend goes from 15° to 130°, so the forearm ends upright.
- **Machine triceps extension:** the pad is at 35° (neutral). The bend goes from 145° (forearms to the upper arms) to straight. The test checks that the hand stays clear in front of the face at the start.
- Both machines have the pivot at the elbow. The lever's length stays the same in both panels, and the stack column stands beyond the straight arm's hands.

**Pushdowns**
- All three use a high pulley at 195 cm, on a column 60 cm in front. The pulley arm reaches 22 cm towards the lifter.
- The upper arm is 3° forward of hanging in both panels. The bend goes from 100° (the forearm 13° above level: "about level", neutral) to 12° ("nearly straight", neutral).
- They use the straight bar, the rope and a single handle. In the one-arm version the free arm hangs.

**Overhead extensions**
- **Cable overhead triceps extension:**
  - A low pulley 75 cm behind. The cable runs up behind the back to the rope.
  - The upper arms are at 165°: elbows up, 15° forward of vertical.
  - The bend goes from 145° (the rope behind the head, at least 3 cm clear of it) to straight.
- **DB overhead triceps extension:** the upper arms are at 175°. The arm goes from straight to a 95° bend, which makes the forearm level. The test checks that the dumbbell's box stays at least 3 cm clear of the head.

**Lying**
- **Skull crusher:**
  - The approved lying pose (Chest.kt) and the EZ bar.
  - The upper arms are upright in both panels. The start is a 5° bend ("not locked").
  - The end bend is solved so the bar shaft is 3 cm above the head.
  - A half-kneeling spotter at the head, like the DB pullover's. The guide asks for a spotter.
- **DB lying triceps extension:**
  - The start is straight over the shoulders (LockoutOverShoulders).
  - At the end the dumbbell centres are at the head's centre, front to back, and 2 cm above it.
  - With fixed lengths this is only reachable with the upper arms tipped back towards the head. Upright, the dumbbells could only get over the neck. The guide does not ask for upright upper arms here. The test bounds the tilt at 5–40°, towards the head.
  - A spotter at the head.
- **JM press:**
  - The rack with pins at 60 cm, as the Bench; the guide's line is RACK_SAFETY.
  - The start is the lockout.
  - At the end the upper arms point 30° above level towards the feet ("elbows high", neutral), and the elbow is at its 148° working limit.
  - The bar then stops over the shoulder line, between the chin and the upper chest (the guide's cue), about 17 cm above the body's axis.

**Kickback**
- A staggered stance with the working side's foot back. The torso is 25° above level (neutral).
- The upper arm is 12.5° up from the torso's line. That is between ACE ("parallel and close to the torso") and ExRx ("parallel to the floor").
- At the start the forearm hangs vertical. ACE: 90°, or at least the forearm hanging.
- At the end the arm is straight. The free hand rests on the front thigh near the knee.

**Triceps dip**
- The chest dip's 120 cm bars.
- **Start:** the shoulders over the hands, the torso 3° forward, the hips 10°, the knees 45°.
- **End:** the torso 10° forward (the Chest dip leans 32°). The elbows are back, in the picture plane, behind the hands. The forearm leans back 20° (neutral).
- **Judgement:** an upright torso with the upper arms level would put the shoulder 90° behind the body. That is past the figure's 60° extension limit (PMC13392859). So the dip stops at that limit, with the upper arms 22° short of level. That is the conservative side of the guide's "no deeper".
- The end caption is "Lower, stay upright", not "upper arms level".

**Push-ups**
- Both use Chest.kt's push-up builder, with the hand position as a parameter.
- **Close-grip push-up:**
  - The hands are under the shoulders, at the shoulder joints' width (out 0).
  - At the end the elbows go back along the body. The chest is 6 cm off the floor, the Push-up's depth.
- **Triangle push-up:**
  - The hands are together at the body's middle (out about -18°), 18° back towards the feet, which puts them under the chest. The 18° is neutral.
  - At the end the chest is 10 cm off the floor, above the hands (neutral).
- The difference that shows from the side is where the hands are along the body. The test checks that the triangle's hands are more than 10 cm further towards the feet.

Sources: every URL cited is the exercise's own page from Sources.kt, except two.
- ExRx DBStandingPreacherCurlInclineBench (the 45–55° pad), which I saw in the search result.
- ExRx JointROM (the wrist), already cited in Body.kt.
- The 45–60° incline and the curl top's elbow travel were also seen in the search results, on the pages cited.
- Everything else is marked neutral in its comment.

## Tests
| Suite | Result |
|---|---|
| domain jvmTest | 444 passed, 1 skipped (BatchArmsTest 21, KitTest 47, FiguresTest 7, AreaMapsLazyTest 1) |
| domain jsNodeTest | 459 passed, 1 skipped. The first full run hit the known GoldenScenarioTest Mocha timeout once; the rerun was green. |
| checkWebDomain | ok (bridge regenerated) |
| app testDebugUnitTest | 301 passed |
| web unit | 657 passed. The first run, made while Gradle's app tests were running, had 1 failure; it passed on the quiet rerun. Typecheck clean; build ok. |
| e2e `--trace off` | 90 passed |

- New BatchArmsTest: 21 tests. One is the area test; the other 20 group the figures by type. Every figure has real bounds:
  - upper arm from hanging or upright, in both panels;
  - forearm angles;
  - bends;
  - knees for every figure (lesson 5);
  - contacts;
  - pad and pivot positions;
  - lever length;
  - head clearance;
  - spotter or pins;
  - the cable runs to the hand in both panels;
  - which way the dumbbell faces (grip);
  - the dip's upright torso against the Chest dip's;
  - the push-ups' hand positions.
- KitTest: 5 new cases.
- **RED:** with `armFigures` emptied, BatchArmsTest ran 21 tests and 21 failed with "no figure". FiguresTest passed against the old golden.
- **GREEN:** the first runs failed on real problems, which I fixed:
  - the thighs went through the short seats (the hips moved back on the seat);
  - the skull crusher's caption was over 24 characters;
  - the triangle push-up's wrist was at 78°;
  - the DB lying dumbbells could not reach beside the head with an upright upper arm;
  - the JM press elbow went low, below the shoulder;
  - one test bound for the pad seats' knees was too tight once I moved the feet forward.
- The KitTest cases were written alongside the kit, not before it.

## Screenshots
- Android: re-recorded with `-Pscreenshots.update=true --rerun`. No file changed.
- Web: no committed screenshot changed (e2e passed).

## Speed
Run after `./gradlew --stop`, with `npx playwright test --project=speed --no-deps --trace off`.
- **Run 1:** 1 failed. The picker test "a few hundred exercises" failed, so the serial picker-browse test did not run; the Exercises tab test passed. 14.7 s in all. The test prints no reading. This is the same picker test that failed once in Task 8. It does not draw thumbnails. I did not change it.
- **Run 2:** 4/4 passed, 14.4 s.
- **Run 3:** 4/4 passed, 15.1 s.
- The speed project also passed inside the full e2e run (Exercises tab 1.9 s).

## Sheets
- `/home/user/powertracker/web/.figure-sheets/arms-dark.png` and `arms-light.png`: all 31, in 4 columns.
- Detail sheets: `/home/user/powertracker/web/.figure-sheets/p1-{dark,light}.png` (the pad pieces) and `p2-{dark,light}.png` (the dips, the overhead extensions and the push-ups).
- I fixed these after looking:
  - **Preacher and machine pads:** they were hidden by the thighs and torso. I deepened the pad block and moved the feet forward.
  - **Machine stack column:** it sat right behind the arms. I moved it past the hands.
  - **Pushdown columns:** they touched the hands. I moved them out.
  - **Triceps dip:** the legs read as a kick. I changed them to the chest dip's legs with the hips straighter.

## Concerns
1. Triceps dip: the end panel stops 22° short of the guide's "upper arms level". That is the shoulder's 60° extension limit with an upright torso.
2. Wrist curl and Reverse wrist curl: the hand's 50° bend shows at full size, but it is small at thumbnail. The two figures are the same two poses, in the same order. Only the captions tell them apart.
3. Reverse curl matches the Curl from the side. Only the captions differ.
4. The curl tops stop 27° short of an upright forearm (35° on the incline curl), because of the elbow's 150° limit with the elbows kept back. The concentration curl and the spider curl stop at 145°.
5. JM press: the bar stops over the shoulder line, about 12 cm above the throat, not at the upper chest. With elbows high and the forearms against the upper arms, the fixed lengths put it there.
6. DB lying triceps extension: the upper arms tip back towards the head at the end, where the skull crusher's stay upright. The end-on dumbbells cover the head in the side view.
7. The pads on the preacher bench and the arm machines are shown as a wedge in front of the chest. At thumbnail size they are small.
8. Speed run 1 had one failure in the picker test, which does not draw thumbnails. Runs 2 and 3 passed.
9. Many angles are neutral and marked so in the code: the curl top, the pushdown start, the kickback lean, the pad angles, the 165° and 175° overhead upper arms, the JM elbow angle and the triangle hands.

## Fix round 1

Brief: task-9-fix1-brief.md, from review-9-report.md. I amended c010525 and kept its message and attribution lines. The commit stays on top of e3c219f.

### Fix 1: Concentration curl's free arm
- **Before:** the free (far) arm was `HANG`, which hangs along the torso's line. With the torso leaning 49°, it lay behind the body and went into the hip and the bench.
- **First try:** the hand resting on the far thigh near the knee, as in the kickback. The shoulder leans out over the knee, so the hand is only about 31 cm away. The arm cannot reach it in the picture's plane: the elbow folds up behind the back, and the stub showed above the back. I dropped it.
- **Now:** the free arm hangs straight down from the shoulder (shoulder angle = 270 − (torso + 180), as the incline curl's `hangDown`). It is in front of the bench, behind the legs, in both panels.
- **New test bounds, both panels:**
  - the far upper arm within 1° of straight down, and the far arm straight;
  - the far wrist more than 20 cm in front of the hip and in front of the bench's end;
  - the free hand 2–60 cm off the floor.
- **The other arm figures:** I checked every figure that uses `HANG` or the default far arm. Every other `HANG` arm is on an upright torso (90°). The leaning figures already have solved far arms: the wrist curls, kickback, incline curl, spider curl and triceps dip. The spotter's arms are solved too. Nothing else had this fault.

### Fix 2: Triangle push-up, END elbow bounded
- **New END bounds:**
  - the elbow more than 15 cm behind the shoulder (towards the feet);
  - the elbow more than 10 cm above the wrist;
  - the hands within 6 cm of under the chest;
  - `forearmOut` between −55° and −25°;
  - the bend 90–148°.
- **One picture change:** this was not expected. With the arms at −108° the END hands were 15 cm behind the chest, because the body pivots on the toes and the chest moves towards the head as it lowers. So "hands under the chest" did not hold at the bottom.
  - The top arm angle is now −97° (`TRIANGLE_ARM`): 7° back from straight down, not 18°.
  - The hands are now 4.9 cm from under the chest at START and 4.5 cm at END.
- **The test against the Close-grip push-up:** "the triangle's hands are further towards the feet" now needs more than 5 cm, not 10. The drawn difference is 6.7 cm.

### Fix 3: Minors
- **Thumbnails:** Wrist curl END (the hand up), Reverse wrist curl START (the hand down). Nothing else changed.
- **DB lying triceps extension:** the upper-arm tip-back bound is now 5–20°. 10.2° is drawn.
- **Tests:**
  - **Pushdowns:** each asserts its attachment (bar, rope, handle) in both panels.
  - **Cable from the pulley to the hand:** a shared `assertCableToTheHand`, used by the pushdowns, the cable overhead extension (which also asserts the rope) and the cable curls. It now checks the cable's pulley end as well as its hand end.
  - **Lying figures:** knees 90–105° for the skull crusher, DB lying extension and JM press. 98.3° is drawn.
  - **Seated DB curl:** the arm straight at START, a 140–148° bend at END, and the torso at 90° in both panels.
  - **Pad figures:** knees 40–60°, not 35–100°. Drawn: preacher 42.6°, machines 54.2°.
- **Kit comments:**
  - OverheadDumbbell now gives its own size: a 14 cm handle and 20 × 5 cm heads.
  - The Dumbbell doc now says `squareToForearm` is for a palms-in curl's dumbbell (the hammer curl, and the DB curls before the palm turns up). It no longer mentions the kickback.

### RED/GREEN
- **RED:** I ran the new tests on the round's starting code (the main-source changes stashed). BatchArmsTest ran 21 and 2 failed:
  - Triangle push-up: "hands still under the chest 15.0 not in 0.0..6.0";
  - Concentration curl: the free-arm bound, which the old along-the-torso arm fails.
- **GREEN:** all 21 pass.

### Golden and tests
- **Golden:** `145:4ac19b1768653a16`.
- **Old figures unchanged:** with `armFigures` emptied after this round's changes, FiguresTest passed against `114:817892063d845bbc`.

| Suite | Result |
|---|---|
| domain jvmTest | 444 passed, 1 skipped |
| domain jsNodeTest | 459 passed, 1 skipped (the known GoldenScenarioTest Mocha timeout in the first two full runs; green on rerun) |
| checkWebDomain | ok |
| app testDebugUnitTest | 301 passed, no screenshot changed |
| web unit | 657 passed |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

- `./gradlew --stop` is done. No speed runs, as the brief says.

### Sheets
- `/home/user/powertracker/web/.figure-sheets/arms-{dark,light}.png`, re-rendered.
- Detail: `/home/user/powertracker/web/.figure-sheets/f1-{dark,light}.png`.

### Not done / concerns
- The free arm hangs; it does not rest on the far thigh, which the brief preferred. Reaching the thigh would take an out-of-plane elbow.
- The Triangle picture changed slightly: the hands are 7 cm nearer the shoulders.
