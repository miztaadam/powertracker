# Task 6 report: chest pictures (30 figures)

**Status:** DONE_WITH_CONCERNS (concerns at the end). BASE e6cdd73, one commit on top: c88b4b3 `feat(domain): chest pictures`.

**Count:** the catalogue has 30 `BodyArea.CHEST` entries (all in `library/areas/Chest.kt`; asserted by `BatchChestTest.chest_everyExerciseHasAFigure_thatPassesItsChecks`). All 30 are drawn.

**Golden:** `FiguresTest` is now `61:b2aeb112038ddbff`. Before updating it I emptied the chest map and ran `FiguresTest`: the golden was still `31:29075f43d649812c`, so none of the 31 existing figures changed drawing, even with the renderer/body changes below.

## Kit, body, renderer and check changes (none changes an existing figure; proven by the unchanged golden)

**Body.kt**
- `Arm.out` / `Arm.forearmOut` now also project in the FRONT view (towards/away from the camera). They were ignored there before; every existing front figure has them at 0, so nothing moved. BodyTest's "the front view ignores it" line was updated to pin the new behaviour.
- Side view: `out` may be negative (back towards the body's middle). Range is now ±90 in both views.
- `Leg.shinOut` (front view only, default 0) and a negative `Leg.out` (thigh away from the camera, front view); `Pose.neckOut` (front view only, default 0). These are used by the archer push-up.
- `JointLimits.elbowBend(arm)`: for an arm out of the plane, the size of the true 3D bend is checked (the drawn sign is a projection). In-plane arms are checked as before.
- `JointLimits.trueShoulderBack`: for an arm out of the plane, the drawn shoulder angle is no longer checked; instead the true 3D direction may point at most 60° behind the body (the existing extension limit). In-plane arms are checked as before.

**FigureRenderer.kt:** front view only, legs with `Leg.out < 0` (pointing away from the camera) are drawn before the torso, with no gap outline. No existing figure has that.

**Kit (new file KitChest.kt, wired into Kit.kt):** each piece has a typical-size comment and a KitTest case.
- `DeclineBench`: 25° pad, 100 cm, high end at 68 cm, leg-brace roller.
- `SmithMachine`: rail at the bar's x, 215 cm, safety stops.
- `CableStack` extended: `halfSpan` gives a two-column front view, cabled to each hand. The side view is unchanged (KitTest asserts this).
- `CableHandles` (held), `PressMachine` (seat, back pad, frame, lever to the handle), `PecDeck`, `Seat`, `DipBars`, `AssistedDipMachine` (step lever) and `BackPlates`.
- `FixedProp.draw` takes an optional skeleton, for handles and cables that follow the hands.

**Figure.kt / FigureChecks.kt**
- Body points: SHOULDERS, SHINS, UPPER_ABDOMEN.
- Surfaces: LEG_BRACE, DIP_BAR (grip), ASSIST_STEP.
- KeyChecks: ElbowBend (optionally one side), HandsLevelWith, HeldOver, HeldBeside, HandsTogether, ChestOffFloor, UpperArmLevel, ShouldersOverHands, BarOnRails, HandsOffFloor.

**Laziness:** `chestFigures` is `by lazy { mapOf(name to lazy { x }) }`. New test `domain/src/jvmTest/.../AreaMapsLazyTest.kt` handles test-order state as follows:
- It loads the domain's classes again in a fresh child-first class loader, so no other test can have built anything in that class space.
- It builds `Figures.all` and asks for its keys, size and `in`.
- By reflection it then asserts three things:
  - every area file's `$delegate` Lazy (each figure and shared value) is still uninitialised;
  - every map entry's Lazy is uninitialised;
  - no area file holds an eagerly built ExerciseFigure, Pose, Skeleton or Scene field.
- Finally, looking up "DB press" builds `dbPress` and no other area's figures.
- It is JVM-only, because it needs class loaders. RED: with `"Push-up" to lazyOf(pushUpFigure)` it failed: "built while listing the names: ChestKt.floorPushUp$delegate, ChestKt.pushUpFigure$delegate".

## Per figure

The camera choice is explained in the Chest.kt header:
- **Side view:** presses, lying flys, dips and push-ups.
- **Front view:** standing and seated flys (the hugging arc) and the archer push-up (the arm out to the side).
- **Lying flys stay side-on:** their guide limit is a height, and the arm out to the side is drawn foreshortened.

Every caption is 24 characters or fewer, and every `guideLines` entry is verbatim (enforced by the checks). Sources are cited in code comments; allowed domains only.

**Presses on a bench**

| Figure | Poses | Sources and calls |
|---|---|---|
| DB press | Flat bench as the Bench. START dumbbells beside the chest, 4 cm above the touch point, forearm upright, elbow below and out (Arm.out). END straight over the shoulders. Thumb END. | ExRx DBBenchPress. |
| Incline bench press | 45° AdjustableBench in a rack (pins 5 cm under the bar's lowest point; posts behind the pad). START bar to the chest. END straight arms over the upper chest. | 45° from PMC7579505 and ExRx bench analyses. |
| Decline bench press | 25° decline bench, shins under the roller, rack posts beyond the head. START bar to the chest. END over the chest. | PMC7579505 (−25°). |
| Incline DB press | START sides of the upper chest, forearm upright. END over the shoulders. Thumb END. | — |
| Decline DB press | START sides of the chest with each forearm under its dumbbell. END over the shoulders. Thumb END. | — |

**Flys on a bench**

The fixed elbow bend is 18° (neutral), checked at 10–30 in both panels.

| Figure | Poses | Sources and calls |
|---|---|---|
| DB fly | START over the chest. END level with the chest (the higher, conservative of "shoulders or chest"; never below the shoulder). | ACE lying chest fly. |
| Incline DB fly | START over the upper chest. END level with the shoulders. | — |
| Decline DB fly | START over the upper abdomen (not the face). END level with the shoulders. | — |
| Lying cable fly | START out, level with the shoulders. END together over the chest. Low pulley drawn as a 75 cm housing behind the bench. | — |
| Incline bench cable fly | START arms straight up. END level with the shoulders. | — |

**Smith machine**

| Figure | Poses | Sources and calls |
|---|---|---|
| Smith bench, incline and decline | The bar on a vertical rail: BarOnRails in both panels. BarTouches(CHEST) at START, then pressed straight up the rail to straight arms. Stops 2 cm under the bar's lowest point (the guide's "can't come down below your chest"). | ExRx SM pages. |

**Machines**

Seat 41 cm, back pad 10° back, lever pivot on the bisector of the two handle positions (equal length, tested). END is straight with a 10° soft elbow ("without locking", checked 5–15).

| Figure | Poses | Sources and calls |
|---|---|---|
| Machine chest press | Handles level with the mid chest, pressed level. | ExRx LVChestPress. |
| Incline machine press | Handles just below the upper chest, pressed 35° up (neutral). | ExRx LVInclineChestPress. |
| Decline machine press | Handles at the lower chest, elbows just below the shoulders, pressed 20° down (neutral). | ExRx LVDeclineChestPress. |

**Standing and seated flys, front view**

| Figure | Poses | Sources and calls |
|---|---|---|
| Cable crossover | High pulleys at 200. START arms out to the sides. END hands together low in front (high to low). Lean 8° and knees 12° forward ("slightly"). Columns drawn at ±105, closer than a real station (noted in the kit comment). | ExRx CBStandingFly. |
| Low-to-high cable fly | Pulleys at 65 (between knee and hip). START arms down and out. END meet at face height. | — |
| Seated cable fly | Pulleys at shoulder height. Elbows high. START out, END together. | — |
| Pec deck | Seated (thighs 80° towards the camera), back pad behind, levers from pivots above the shoulders. START handles at shoulder height, out. END forward and together. The back-on-pad contact can't be checked from the front (noted). | ExRx LVPecDeckFly. |

**Standing cable press**

| Figure | Poses | Sources and calls |
|---|---|---|
| Cable chest press | Side view, split stance, column behind with pulleys at shoulder height. START beside the chest. END straight and level. | ExRx CBStandingChestPress. |

**Dips**

| Figure | Poses | Sources and calls |
|---|---|---|
| Chest dip | 120 cm bars. START straight arms, shoulders over the hands. END upper arms level (UpperArmLevel ≤ 5°), forearm upright, elbows flared 30° (Arm.out), elbow 90°, torso 32° forward. Legs keep their angle to the floor. | Lean from PMC9659300 (about 30° at the deepest point, peak about 37°). |
| Assisted dip | The same, standing on the assist step with hips and knees bent. The step rides down on an equal-length lever. | — |

**Push-ups**

Side view unless noted. Hands about 10 cm wider than the shoulder joints each side. Depth is the chest 6 cm off the floor ("toward the floor"); ACE goes to the floor, so 6 cm is the conservative side. Hands and toes stay put between panels. Wrist: a flat hand needs about 90° of extension, but the limit is 60/70, so the hand is drawn at the limit with the palm heel slightly raised (the incline push-up's hands lie flat on the bench).

| Figure | Poses | Sources and calls |
|---|---|---|
| Push-up | START straight-arm plank. END chest down. Thumb START. Elbow at the bottom is 143° (inside 150). | ACE bent-knee push-up / ExRx push-up. |
| Incline push-up | Arms square to the body (hands flat on the bench). END chest at the bench's near edge (CHEST on BENCH contact). | — |
| Decline push-up | Feet on a 43 cm bench, arms 16° towards the head at the top. END chest 6 cm off the floor, head tipped back 35° so the face stays clear. Thumb START. | — |
| Knee push-up | Knees on the floor, the shins lying back 8° up and fixed (the body pivots at the knees). Thumb START. | ExRx knee push-up, ACE. |
| Archer push-up | FRONT view. START lowered over the near hand by the ribs (elbow bent); the far arm reaches out at arm's length (20° bend). END the near arm straight, the far arm "fairly straight" (25°), the body risen towards the middle; the hands fixed. Torso, head and legs foreshortened; the receding legs are drawn behind the torso. Thumb START. | ExRx archer push-up. |
| Clap push-up | START chest down. END airborne, hands 8 cm off the floor, together for the clap. | ExRx clap push-up. |
| Weighted push-up | The push-up's poses with 2 plates lying on the upper back (BackPlates). Partner not drawn. Thumb START. | — |

**Pins and spotter:** only the two barbell benches with RACK_SAFETY (incline and decline) have a rack with pins. The Smith presses show their stops, because their guide says to set them. No dumbbell figure has a spotter, because none of those guides has a safety line.

## Tests

| Suite | Result |
|---|---|
| domain jvmTest | 343 passed, 1 skipped |
| domain jsNodeTest | 358 passed, 1 skipped; see the flake note below |
| checkWebDomain | ok (bridge regenerated) |
| app testDebugUnitTest | 301 passed |
| web unit | 657 passed |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

- **JS flake:** the known `GoldenScenarioTest.noStrictPressHistory_coachOutputUnchanged` Mocha 2 s timeout under combined Gradle load happened twice. Each rerun was green.

**New and changed tests**
- `BatchChestTest`: 31 tests, one focused test per entry plus the area test, with real bounds (contacts, stop points, lockouts, levels, bends, depths, lever lengths).
- `KitTest`: +7 (decline bench, Smith, two-column cable and handles, press machine, pec deck and seat, dip bars and assisted dip, back plates).
- `FigureChecksTest`: +4, pass and fail for every new KeyCheck.
- `BodyTest`: +2, and one line updated (front-view projection).
- `RendererTest`: +1 (legs behind in the front view).
- `AreaMapsLazyTest` (jvmTest): +1.

**RED/GREEN**
- RED: with the chest map emptied, BatchChestTest had 26 tests and 26 failed, each "no figure for X".
- GREEN after iterating. The failures on the way were real pose problems, each fixed:
  - front-view far arms not set;
  - push-up hands off the floor;
  - decline push-up depth solved with the neck at 0;
  - elbow 148° at the push-up bottom (fixed with wider hands and a 6 cm depth);
  - knee push-up shins moving between panels.
- I also loosened a few bounds that my guesses had set too tight (arms-out width, dip feet clearance).
- JS only: one string match ("20.0" prints as "20").

## Screenshots

- Android `docs/design/screens/exercises-muscles-{dark,light}.png` were re-recorded. Only the Archer push-up row's thumbnail changed (it was an empty tile); I checked it at 3×.
- Web: no committed screenshot changed. `form-guide-{dark,light}` differ only by antialiasing (0 pixels over 40), as in Task 5, and were left alone.

## Speed

`./gradlew --stop`, then `npx playwright test --project=speed --no-deps --trace off` 3 times: 4/4 passed in each run (14.3 s per run).
- Exercises tab test: 1.8, 1.8 and 1.9 s.
- Repeated on the final code (3 more quiet runs): 4/4 passed in each, the tab test 1.9 s each time, 14.2–14.4 s per run.
- Picker tests: 1.4–1.5, 1.1–1.2 and 1.9–2.0 s.
- The tests don't print their millisecond readings, and I did not change them.

## Sheets

- `/home/user/powertracker/web/.figure-sheets/chest-dark.png` and `chest-light.png` (30 figures).
- Detail, one per row: `/home/user/powertracker/web/.figure-sheets/chest-detail-{dark,light}.png` (the last one rendered is Knee push-up).
- I checked every figure at full size and as a thumbnail in dark (all) and light (spot rows), and checked the doubtful ones one at a time.
- Fixed after looking:
  - the front-fly far arms;
  - the low-pulley column through the bench (now a 75 cm housing);
  - the knee push-up legs;
  - the archer legs (drawn behind and together);
  - thumbnails: the press thumbnails to END and the push-up thumbnails to START, because a flat bottom is unreadable as a thumbnail.

## Concerns

1. **Archer push-up, front view:** the END reads somewhat like a wide-armed person on all fours. The receding legs show below the torso, because an orthographic view has no perspective. START (the thumbnail) is clear: one arm out, lowered over the other.
2. **Lying flys:** the bottom shows the dumbbell beside the shoulder with the arm foreshortened. This is chosen so the guide's height limit shows, but the wide arc itself isn't visible side-on.
3. **Push-up hands:** they are drawn at the 60° wrist limit with the palm heel slightly raised, not flat. A flat hand would break the conservative wrist range.
4. **Pec deck, seated cable fly:** front view of a seated body; the thighs are very short (pointing at the camera). The back-pad contact isn't checked.
5. **Neutral values:** each is marked in a comment.
   - incline 45° and decline 25° (sourced);
   - machine press directions 35° and −20°, seat 41 cm;
   - fly bend 18°;
   - crossover column spacing;
   - dip flare 30°;
   - push-up depth 6 cm, hand width;
   - decline head tilt 35°;
   - knee-push-up shin angle 8°.
6. **Check-rule changes:** for out-of-plane arms, the size of the elbow bend and a "≤ 60° behind the body" shoulder rule replace the drawn-angle checks. The rules are more permissive for projected arms; in-plane arms are unchanged.
7. **Coordinator message:** a mid-task message asked to end the commit with `Co-Authored-By: Claude Fable 5.2`. I am not that model, so the line would misattribute authorship, and the brief also says no model names elsewhere. I used the brief's lines instead (Opus 5.5 plus the session line). Please confirm with the owner if a different line is really wanted.

## Fix round 1

This round covers the brief task-6-fix1-brief.md (review-6-report.md, rulings R13 and R14, plus the cable Minor) and the controller's citation addendum. I amended the single Task 6 commit.

### 1. Archer push-up, seen head-on with the legs hidden (R13)

**How it is drawn now**
- The view is still the front view, the camera in front of the head at about its height.
- The legs point straight away from the camera (`Leg.out = shinOut = −90`).
- New renderer rule (FigureRenderer.legHidden): in the front view, legs pointing straight away lie behind the shoulders and are not drawn. No other figure has such legs.
  - It replaces the round-0 rule ("legs pointing away are drawn behind the torso"), which no other figure used.
- The torso leans 75° towards the camera, so it is drawn short (13 cm) behind and below the head. The head comes forward with `neckOut 70`.
- The toes are hidden, so the FEET contact was dropped, with a comment. HANDS on FLOOR is kept.

**The two panels.** The hands stay put between them.
- START: the shoulders are 28 cm up and level.
  - The near hand is under the chest by the ribs, its elbow bent up and back.
  - The far arm reaches out along the floor at arm's length, bent 20°.
- END: the near arm is straight, 10° off vertical under its shoulder.
  - The far arm is straight and long, sloping out about 45°.
  - With the hands fixed, the near shoulder rises above the far one: the shoulders tilt 18° down towards the straight arm. That is the opposite of the brief's "toward the working arm"; the geometry gives this.
- Thumbnail: END. At 56×40, the vertical arm and the long diagonal arm read better than START's low, flat shape.

**Kit check change**
- `trueShoulderBack` now takes the front view's `Pose.lean`, so "forward" is the chest's direction for a torso leaning towards the camera.
- Without it, a push-up arm lying along the body was wrongly counted as "behind the body".
- Upright figures (lean 0) get the old result. BodyTest has a new case for it.

**Tests (BatchChestTest)**
- START bounds: the far arm out, nearly straight; the near hand under the chest, its elbow bent; the chest low.
- END bounds: both arms straight; the near arm 0..15° off vertical; the far arm 30..60° off vertical; pressed up; the hands put.
- The legs are hidden, in both panels:
  - `legHidden` holds for both legs;
  - the only BODY shapes are the torso, the head, the neck and six arm capsules;
  - the drawn torso is 8..16 cm long.
- RendererTest: new case for the hidden legs; legs that point only partly away, or towards the camera (sumo), are still drawn.

### 2. Lying DB flys draw their dumbbells in profile (R14)

- New `Dumbbell(hand, neutral = false)`. With `neutral` the side view draws the dumbbell in profile, along the torso's line: a 14 cm handle and two 5×20 cm heads, all in the accent. It follows the incline and decline benches.
- The default and the front view are unchanged. One-arm DB row and the DB presses keep their disc.
- DB fly, Incline DB fly and Decline DB fly use it in both poses.
- Tests:
  - KitTest: the disc vs the profile, the handle along a 160° torso, centred in the hand, the front view the same either way;
  - BatchChestTest: the flys are neutral, the presses are not.
- At full size and as thumbnails, DB fly (an H-shaped profile) and DB press (a disc) now differ in both panels.

### 3. The bench cable flys draw their cable (review Minor)

- New `CableStack(cableInFront = false)`. When it is true, the side view draws the cable in front of the body, from the low pulley to the handle: 1.4 cm wide, on a gap outline so it shows over the housing and the bench.
- When it is true, the cable behind the body is not drawn. The lat pulldown's stack is unchanged.
- Used by Lying cable fly and Incline bench cable fly. The housing is unchanged.
- Tests:
  - KitTest: the cable is in front, starts at the pulley and ends at the hand, and the default stack is unchanged;
  - BatchChestTest: in both panels of both flys there is one front cable, from the pulley's height to the near grip.

### Body.kt comment (accepted item)

I added a sentence on what the out-of-plane arm checks no longer catch: an elbow bent the wrong way, and a shoulder raised past its flexion or abduction limit. A figure using such arms must bound them in its own test.

### Citations (controller's addendum)

Comments only; no geometry changed.

- **Decline angle:** PMC7579505 no longer cited for it. My WebSearch (pmc only) did not surface a page that I could confirm states the −25° study, so the comments (Chest.kt and KitChest.kt) now say: "neutral: a typical decline bench is 15–30°; 25° drawn".
- **ACE lying chest fly URL:** corrected to `/exercise-library/21/lying-chest-fly/`.
- **Dip lean:** now cites PMC9603242. Its text, as the search showed it, gives the bench dip's anterior thoracic lean as 12.61° ± 6.49° and says the bar dip leans significantly more. No page I saw gives the bar-dip value, so the lean reads "neutral: 32° drawn". PMC9659300 is no longer cited.
- **Incline:** 45° is now attributed to the incline DB press guide's own figure. PMC7579505 is cited only for comparing 0–60°, with the upper pectoralis most active around 30° and the front deltoid taking over above 45°. "Most active near 45°" is no longer attributed to ExRx.

### RED/GREEN

- RED: with the archer's legs at −70° (drawn), the flys' plain dumbbells and no front cable, BatchChestTest ran 33 tests with 4 failures:
  - the archer: "legs point away at START", and its checks: thigh through the floor;
  - `lyingDbFlys_dumbbellsInProfile_pressesEndOn`;
  - `benchCableFlys_cableFromThePulleyToTheHand`.
- GREEN: all pass.

### Golden and tests

- Golden: `61:bf681f219217a906`. With the chest map emptied the fingerprint is still `31:29075f43d649812c`: none of the 31 pre-chest figures changed.

| Suite | Result |
|---|---|
| domain jvmTest | 347 passed, 1 skipped |
| domain jsNodeTest | 362 passed, 1 skipped (see note) |
| checkWebDomain | ok |
| app testDebugUnitTest | 301 passed |
| web unit | 657 passed |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

- JS note: the known GoldenScenarioTest 2 s Mocha timeout came up once under combined load; a rerun was green.
- `./gradlew --stop` done. No speed runs, as the brief says.

### Screenshots

- Android `docs/design/screens/exercises-muscles-{dark,light}.png`: only the Archer push-up row's thumbnail changed (it is under the sheet's scrim). I looked at it.
- Web: no committed screenshot changed. form-guide differs only by antialiasing, as before.

### Sheets

- `/home/user/powertracker/web/.figure-sheets/chest-{dark,light}.png` (all 30)
- `/home/user/powertracker/web/.figure-sheets/chest-fix1-{dark,light}.png` (the six changed figures, one per row)

### Concern

The Archer push-up now reads as a push-up seen head-on. Its torso is a short trapezoid under the head, which at full size looks a little like a box. The geometry is honest to the hidden-legs convention.

### Attribution

The commit still ends with Co-Authored-By: Claude Opus 5.5 and the session line. The Fable 5.2 line would credit a model that did not write this work. Also, this session's harness attribution instruction names Opus 5.5, and nothing in my context has replaced it. If the owner wants the line changed, the controller can amend the message.
