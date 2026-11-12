# Task 4 report: first ten exercise pictures

**Status:** DONE_WITH_CONCERNS. All ten figures pass FigureChecks, and the full suites are green. The concerns are listed at the end and need the controller's or the owner's eyes.

**Base:** 0b11d33. There is one commit on top of it: `feat(domain): first ten exercise pictures`.

## Sheets

- `web/.figure-sheets/batch1-dark.png`
- `web/.figure-sheets/batch1-light.png`

They were written with `cd web && npm run figures:sheet -- --label batch1 <ten names>`. The sheets are gitignored and were reviewed visually.

## Per exercise

Every figure is drawn in side view, apart from the one deviation noted under DB shoulder press. Captions use the guide's wording and are 24 characters or fewer. `guideLines` are copied verbatim from the guide; the integrity check enforces this.

### Squat
**Poses**
- START: standing. Shins 4° forward, knees 0°, torso 78°.
- END: shins 33° forward, torso 44°. The hip crease is 2.2 cm below the knee.

**Equipment**
- The bar sits on the upper back (`Barbell(UPPER_BACK)`). The hands are turned out to reach it; see `Arm.turnedOut`.
- A rack stands in front of the lifter: J-hooks are 8 cm under the bar, and the safety arms are 6 cm under the bar's lowest point. This follows the owner's direction for heavy barbell rack work.

**Checks**
- `HipDepth(2.0, END)`
- `BarOverMidfoot` at START and END
- FEET on FLOOR

**Captions:** "Stand tall, knees locked" / "Hip crease below knee". The thumbnail shows END.

**Sources**
- PMC10143703 (anterior knee displacement review): the knees travel forward and the trunk angle of a high-bar squat.
- PMC4262933 (the guide's back-squat assessment source): the shin and trunk lean at depth.
- exrx.net/Kinesiology/DeadliftForm: the bar over mid-foot, the definition BarOverMidfoot uses.

**Neutral:** the rack sizes, and the J-hook and safety heights relative to the bar.

### Bench
**Poses**
- START: the bar on the chest, at `CHEST_BELOW_SHOULDER` 17 cm (lower to mid chest).
- END: lockout over the shoulders.

**Checks**
- `BarTouches(CHEST, START)`
- `LockoutOverShoulders(END)`
- The contacts are the same as in the Task 1 draft (head, upper back and hips on the bench; feet on the floor).
- The spotter and safety pins are kept from the draft, because the bar moves over the face.

**Captions:** "Pause on your chest" / "Press until elbows lock".

**Sources**
- exrx.net/Kinesiology/BenchPress: lower to mid chest, forearms vertical.
- exrx.net/WeightExercises/PectoralSternal/BBBenchPressPowerLift: the bar touches above the bottom of the sternum.
- PMC5968970: the elbow is about 90° in 3D with a grip wider than the shoulders.

**Neutral:** the feet are 32 cm past the hips (the draft's placement).

### Deadlift
**Poses**
- START: shins 3° forward, the bar 2.5 cm off the shins, shoulders 3 cm in front of the bar. Hip flexion comes out at about 129°, which matches the NSCA's inside angle of about 50°.
- END: standing tall, with the arms angled to the front of the thighs.

**Checks**
- `BarOverMidfoot` and `BarNearLegs(4.0)` in both panels
- FEET on FLOOR

**Captions:** "Shins close to the bar" / "Stand tall, knees locked". The thumbnail shows START.

**Sources**
- exrx.net/Kinesiology/DeadliftForm: bar over mid-foot, shoulders slightly in front of the bar.
- NSCA, "Exercise progressions for resuming strength training following posterior chain muscle injury": the bar about one inch from the shins.

**Neutral:** the 3 cm shoulder lead, which sits within "slightly".

### Strict press
**Poses**
- START: the bar at the front of the neck. Because of the elbow limit, the bar sits about 15 cm in front of the shoulder.
- END: straight arms overhead.
- The legs lean 3°.

**Equipment:** a rack in front of the lifter, with J-hooks at shoulder height and safety arms 10 cm under the bar's start.

**Checks**
- `LockoutOverShoulders(END)`
- `BarOverMidfoot(END)`
- FEET on FLOOR

**Captions:** "Front of your neck" / "Press to straight arms".

**Sources:** exrx.net/WeightExercises/DeltoidAnterior/BBMilitaryPress: press until the arms are extended overhead.

### DB shoulder press
**Poses**
- Seated on a new `AdjustableBench` with the back at 90°.
- START: the dumbbells at shoulder height.
- END: elbows straight overhead.

**Checks**
- HIPS on SEAT, UPPER_BACK on BACK_PAD, HEAD on BACK_PAD, FEET on FLOOR
- `LockoutOverShoulders(END)`

**Captions:** "At shoulder height" / "Elbows straight overhead".

**Sources:** exrx.net DBShoulderPress and the ACE seated overhead press (library entry 45) both have the lifter sit upright against the back support.

**Neutral:** the knees at about 90° and the bench sizes.

**Deviation:** the press moves partly in the frontal plane, but a seated body against a back pad can't be read from the front. Side view was chosen; the reason is noted in the comment.

### Lat pulldown
**Poses**
- Seated, with the torso leaning back 10°, a neutral "slightly".
- The thigh pad sits 80% of the way along the thighs.
- START: arms fully stretched.
- END: the bar at the upper chest (new `UPPER_CHEST` point).

**Checks**
- HIPS on SEAT, THIGHS on THIGH_PAD, FEET on FLOOR
- `BarTouches(UPPER_CHEST, END)`

**Captions:** "Arms fully stretched" / "Bar to your upper chest".

**Sources:** exrx.net CBFrontPulldown; ACE seated lat pulldown (entry 158).

**Neutral:** the seat at 48 cm and the machine sizes.

### Leg press
**Poses**
- A 45° sled. The back pad is at 140°, and the feet are in the middle of the plate.
- START: knees at 15°, a neutral "nearly straight" (ExRx has the knees just short of lockout).
- END: knees at 85°, the conservative side of the guide's "about 90".

**Checks**
- UPPER_BACK on BACK_PAD, HIPS on SEAT, FEET on FOOT_PLATE
- A test that the knee angles sit within the guide's range.

**Captions:** "Legs nearly straight" / "Knees about 90 degrees".

**Sources:** exrx.net SL45LegPress.

**Neutral:** `PLATE_RISE` 66 and the machine sizes.

### Pull-up
**Poses**
- START: a dead hang, the legs crossed.
- END: the chin clears the bar.

**Checks**
- HANDS on PULL_UP_BAR
- New `ChinOverBar(END)`

**Captions:** "Hang with straight arms" / "Chin clears the bar".

**Sources:** exrx.net BWPullup (pull until the chin is over the bar); ACE pull-ups (entry 191).

### One-arm DB row
**Poses**
- The far knee and far hand rest on a flat bench; the near foot is on the floor.
- The torso is close to flat at 12°.
- START: the arm straight.
- END: the dumbbell pulled to the ribs.

**Checks**
- FAR_HAND on BENCH, FAR_KNEE on BENCH, NEAR_FOOT on FLOOR
- `Dumbbell(NEAR)` is held.

**Captions:** "Arm straight" / "Pull to your ribs".

**Sources:** exrx.net DBBentOverRow; ACE single-arm row (entry 126).

**Kit change:** the bench's new `nearLimbsBeside` flag. The near leg stands beside the bench, not through it, so near limbs aren't treated as hitting it.

### RDL
**Poses**
- The knees stay at 18° in both panels.
- START: standing, with the bar against the front of the thighs.
- END: torso at 29°, the bar 2 cm below the knee, and the shins vertical.

**Equipment:** the rack is 60 cm in front so its posts clear the head.

**Checks**
- `BarOverMidfoot` and `BarNearLegs(4)` in both panels
- FEET on FLOOR

**Captions:** "Knees slightly bent" / "Weight about at knees".

**Sources**
- ACE Romanian deadlift (entry 317).
- NSCA Kinetic Select RDL: just below the knees.
- The NSCA progression article: the torso at about 30°.

**Neutral:** the 18° knee bend, a "slight" bend.

## Task 1 concerns and how each was resolved (all in Body.kt comments)

| Concern | Resolution |
|---|---|
| Shank fraction | Shank is 0.246H, from PMC13417611 (the Noraxon IMU vs Vicon study; femoral epicondyle to malleolus, derived from Dempster). PMC3582351 confirms thigh 0.245 and foot 0.152. The alternatives (0.221 in PMC11233572 and 0.22 in PMC13672677) were rejected so that the whole table comes from one consistent source; the comment says so. |
| Head/neck/torso split, ankle height | Taken from Winter's landmarks (PMC12637938) and marked as a derived drawing value. Cross-check: the acromion regression in PMC4754207 gives about 144 cm; the model puts it at 143 cm. |
| Widths, rack/bench/machine sizes | The widths are marked as a neutral drawing estimate, with a torso cross-check against PMC4840890 (chest depth 24.5 cm). Every kit size is commented as a typical, manufacturer-neutral estimate. |
| HEAD_RADIUS 10 vs Segment.HEAD 22.75 | The radius rests on a head length of 19–20 cm (PMC6820124). The head circle now sits so that its top is at the crown, and `headCircle_topAtTheCrown` tests this. |
| CHEST_BELOW_SHOULDER / CHEST_DEPTH | Depth is 12.5 cm, half of PMC4840890's 24.5 cm. Height is 17 cm, a neutral value inside ExRx's "lower to mid chest, above the bottom of the sternum". |
| Bench bottom elbow of about 142° | The angle is a 2D projection; the real elbow is about 90° in 3D (PMC5968970) because the upper arm points out to the side and is foreshortened. A fully vertical forearm can't be drawn in 2D with full-length segments, so the test asks for under 20° off vertical (it measures about 16°). |

**Joint limits changed**

These need controller sign-off.

- Ankle dorsiflexion goes from 20 to 35. Sources: weight-bearing lunge results of 38–42.5° in young adults (PMC6249959), and PMC7932177, which treats under 35° as a limitation.
- Side-view hip flexion goes from 120 to 140. The model's hip is the trunk–thigh angle, which includes pelvic tilt (PMC9403702). With a high-bar squat trunk at about 46° from vertical (PMC10143703), that angle comes to about 135–139°.

## Kit, checks and body additions

**Body**
- `Arm.turnedOut`, which lets the hands reach a bar on the back from the side view.
- `UPPER_CHEST_BELOW_SHOULDER` 12 and `UPPER_CHEST_DEPTH` 12.5 (both neutral).

**Points**
- `BodyPoint.UPPER_CHEST`
- `NEAR_KNEE` and `FAR_KNEE`

**Checks**
- `BarNearLegs`
- `ChinOverBar`

**Kit**
- `AdjustableBench`
- `FlatBench.nearLimbsBeside`
- `PowerRack.lifterLeft`, which mirrors the rack
- `PullUpBar.RADIUS`
- `LegPress45.PLATE_RISE` and a drawn carriage arm
- `Solid.stopsNearLimbs`

**Shared pose helpers:** `areas/Poses.kt`.

## Tests

| Suite | Result |
|---|---|
| domain jvmTest | 261 passed, 1 skipped |
| domain jsNodeTest | 277 passed, 1 skipped |
| checkWebDomain | ok |
| app testDebugUnitTest | 301 passed |
| web unit | 653 passed (39 files) |
| typecheck | clean |
| build | ok |
| e2e (`--trace off`) | 90 passed. The reload flake did not occur. |

**Golden:** `FiguresTest` is now `10:5be660af9018f5b4`, and it matches on JVM and JS.

**Screenshots re-recorded**
- Android `docs/design/screens/`: exercise-day-chooser, exercise-guide-actions, exercises, form-guide, form-guide-picture.
- Web `docs/design/web/screens/`: exercises, form-guide, form-guide-end, form-guide-picture.
- Each in dark and light. All were inspected.

**RED/GREEN evidence**
- RED: before the poses existed, the first run of `BatchOneTest` plus the new checks/kit tests gave 13 failures: "no figure for X" for each entry, missing checks, and a golden mismatch.
- GREEN: after the poses were drawn, all tests pass. Along the way, these checks failed and were fixed:
  - HipDepth at exactly 2.0;
  - the row's far thigh 1.5 cm through the bench;
  - the leg-press ankle out of range;
  - the RDL rack post touching the head;
  - the pull-up elbow at 149.7.

## Files changed

**Domain (`domain/src/commonMain/.../library/figures/`)**
- Body.kt
- Figure.kt
- FigureChecks.kt
- Kit.kt
- areas/Poses.kt (new)
- areas/MainLifts.kt
- areas/Shoulders.kt
- areas/Back.kt
- areas/Quads.kt
- areas/HamstringsGlutes.kt

**Domain tests (`domain/src/commonTest/.../library/figures/`)**
- BatchOneTest.kt (new)
- BodyTest.kt
- FigureChecksTest.kt
- FiguresTest.kt
- KitTest.kt

**Generated web bridge:** `web/src/domain/generated/*.mjs`, regenerated by `syncWebDomain`.

**Screenshots:** the 18 PNGs listed above.

**Report:** this file.

## Self-review and concerns

1. **Deadlift start torso:** about 19° from horizontal. That is flat, but it follows from the fixed segment lengths with near-vertical shins and the shoulders slightly in front of the bar.
2. **Squat start stance:** the torso leans 12° and the ankles 4°. That is not fully vertical, but it keeps the bar over mid-foot with the bar on the back.
3. **Strict press start:** the bar is about 15 cm in front of the shoulder. Bending the elbow past 148° would be needed to bring it closer in a side view; the real arm is foreshortened.
4. **DB shoulder press view:** side view instead of the frontal plane (see the deviation above).
5. **Joint limits:** the ankle 35 and side-view hip 140 changes need controller sign-off.
6. **Leg-press carriage:** the carriage arm reads a little like a long "Y". This is a style point for the owner checkpoint.
7. **Equipment sizes:** all rack, bench and machine sizes are neutral estimates, and the comments say so.

## Fix round 1

This round fixes the six Important findings in review-4-report.md, plus the cheap Minor ones. Task 4 is still one commit: `feat(domain): first ten exercise pictures`, amended.

### Important fixes

**1. Deadlift START**

The figure now reads as a conventional pull:
- shins lean 12° forward;
- the bar is 0.5 cm off the shins (ACE has the shins touching);
- shoulders are 1 cm ahead of the bar;
- the torso is 30.7° above level;
- hips are 63 cm up, the hip angle is about 130° and the knees are bent about 82°.

The bar is still over mid-foot and close to the legs.

The proportions limit how far this can go. With straight arms, a 12° shin and a 0.5 cm gap allow a torso of only about 31–32°. A 3–4 cm shoulder lead would flatten it to about 25°.

Source: exrx.net/Kinesiology/Deadlift (hips deeply flexed, knees moderately bent at the start).

The test now asserts:
- shin lean 8–12°;
- torso 30–35°;
- hips at least 20 cm below the shoulders;
- shoulders 0–4 cm ahead of the bar.

**2. Strict press START**

The elbow is folded to its 148° limit and the hand is bent back 55°, so the bar rests at the front of the shoulders. It sits about 9 cm in front of the shoulder joint and below the chin. Before, it was 15 cm out at chin height.

A note on the wrist sign: the model's sign follows the palm side of an unrotated forearm. With the overhand grip, the drawn bend towards the face is extension. It stays within both wrist limits, and the comment says so.

Rack changes:
- The J-hooks are now 6 cm under the bar's start.
- The safety arms are 10 cm under it.

The test asserts the bar is 6–11 cm in front of the shoulder and below the chin.

**3. DB shoulder press START**

New kit feature: `Arm.out`, the upper arm's angle out of the picture plane, for the side view only.
- The upper arm is drawn at its true length × cos(out). This is a projection, not a change of proportions.
- The elbow limit is checked on the true 3D bend (`JointLimits.trueElbow`).
- `out` is kept within 0–90°.
- Tests: `BodyTest.armOutOfThePlane_...`.

How the start pose uses it:
- The upper arm is 50° out.
- The forearm is vertical, with the dumbbell straight above the elbow at about ear height. The true elbow bend is about 120°.

One deviation from the reviewer's "in line with the shoulder ±3": the dumbbell is drawn 13 cm in front of the shoulder joint. At ±3 cm the forearm was drawn across the face and read as a hand on the face. This placement is neutral and the comment explains it.

The test asserts the dumbbell is over the elbow (within 0.5 cm), the elbow is below the shoulder, and the dumbbell is above it.

**4. Leg press END**

Hip flexion is now 113° and the ankle 31.8°; the knee is still 85°.

Why the reviewer's suggested fixes were not enough on their own: with the feet flat on the plate, hip flexion = 180 − back + (plate − 90) − ankle + knee.
- Lowering `PLATE_RISE` alone only raises the ankle past its 35° limit (at 45 it reached 52°).
- Steepening the back pad raises hip flexion rather than lowering it.

What changed instead:
- The back pad reclines to `BACK_ANGLE` 155 (25° above level).
- New `PLATE_TILT` of 10°: the plate is tilted towards upright.
- `PLATE_RISE` is 52.
- All three are neutral machine sizes.

The test now caps hip flexion at under 120°.

Drawing changes:
- The carriage is a solid sled wedge from the rail to the back of the plate, replacing the long "Y" arm.
- The plate is 5 cm thick and set off by a gap line, so the feet clearly sit on its face.
- The seat has a second leg, so it reads as part of the frame.

**5. Pull-up**

The bar:
- It is drawn in front of the hands, with a new `FixedProp.drawFront` step that renders last. It is drawn at a 3 cm radius (real radius 1.6) with a gap ring.
- The upright pole is gone. The bar now sits on a 45 cm level wall bracket behind the lifter (neutral), so its height reads across the picture.

The chin:
- `ChinOverBar` gains a margin, 4 cm by default, and has a test for clearing the bar by less than the margin.
- At END the chin is 5.5 cm over the bar top (shoulder 19 cm behind and 2 cm below the bar centre). The test asserts 5.0–6.5 cm.

The legs are crossed with the knees bent further, which makes the figure shorter so it fills more of the panel.

The front view was considered and not used. It cannot show bent, crossed legs with fixed segment lengths, and with straight legs it is taller, so the figure would come out smaller.

**6. One-arm DB row END**

The shoulder is now at −25°, the elbow 14 cm above the torso line, and the forearm vertical. The dumbbell sits about 10 cm under the torso, at the lower ribs. The test asserts the shoulder at −30..−25° and the dumbbell within 22 cm of the torso's centre line.

### Minor fixes taken

- Rack safety arms now stop 8 cm short of the bar, under its plates (`rackInFront` `armLength = gap − 8`). They no longer run through the legs in Squat, Strict press or RDL.
- This report's earlier line is corrected: the Bench shows safety arms, not a spotter. The guide allows either.

### Minor items not taken

- **Squat START at torso 84°, shin 2°:** this fails BarOverMidfoot (−11.5 cm), so 78°/4° stays, as the reviewer allowed.
- **Lat pulldown END elbow:** the elbow is already 17 cm behind the torso's centre line, behind the back. A vertical forearm would need the bar 46 cm from the shoulder in the drawing, which is impossible. Turning the arm out put the elbow above the shoulder, so that was reverted. Left as it is.
- **Bench elbow overlapping the bench rail:** left as it is; a 2 cm tuck makes no visible difference.
- **Thumbnail cropping:** left for the owner's check. The Pull-up thumbnail is still small.

### Tests

| Suite | Result |
|---|---|
| domain jvmTest | 262 passed, 1 skipped |
| domain jsNodeTest | 278 passed, 1 skipped |
| checkWebDomain | ok |
| app testDebugUnitTest | 301 passed |
| web unit | 653 passed |
| typecheck | clean |
| build | ok |
| e2e (`--trace off`) | 90 passed |

The golden is now `10:d2bea818efe378a3`.

**Screenshots re-recorded:**
- Android: exercise-day-chooser, exercise-guide-actions, exercises, form-guide (dark and light).
- Web: form-guide (dark and light).

### Sheets

Re-rendered and checked at full size and at thumbnail size, in both dark and light:
- `/home/user/powertracker/web/.figure-sheets/batch1-dark.png`
- `/home/user/powertracker/web/.figure-sheets/batch1-light.png`

### Remaining concerns

1. `Arm.out` is a new kit concept: a side-view projection of an upper arm out of the plane. It is used only by the DB shoulder press for now. It could also fix the Bench forearm and the Lat pulldown in a later batch.
2. The leg press back pad now reclines to 25° above level, flatter than many machines. This is the cost of keeping hip flexion under 120° with flat feet and knees at 85°.
3. The DB press dumbbell is 13 cm in front of the shoulder rather than in line with it (see fix 3).
4. The Pull-up figure is still fairly small in its panel and thumbnail. It is height-limited because the two panels share one fit.
