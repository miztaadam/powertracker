# Task 7 report: back pictures (30 new figures)

**Status:** DONE_WITH_CONCERNS. BASE 13f8a93, one commit on top: f52f10f `feat(domain): back pictures` (local, not pushed).

**Count:** the catalogue has 33 `BodyArea.BACK` entries (all in `library/areas/Back.kt`). Three were drawn already (Pull-up, Lat pulldown, One-arm DB row, unchanged). 30 are new, so the brief's "about 28" is 30. `BatchBackTest.back_everyExerciseHasAFigure_thatPassesItsChecks` asserts all 33.

**Golden:** `91:c7b616b47a2bb62d`. Proof that nothing existing moved: with the new entries removed from `backFigures`, `FiguresTest` passed against the old golden `61:bf681f219217a906`, after every kit, body and renderer change.

**Commit attribution:** the commit ends with my harness's lines (Co-Authored-By: Claude Opus 5.5, plus the session line). The brief said to do this if my harness gave different lines. The session URL is the same as the brief's.

## Where the code is

- All 33 figures are in `figures/areas/Back.kt`, as the brief asks. The new ones are under "Task 7: the rest of the Back area".
- The new figures sit in a `by lazy` map of `Lazy` entries, merged into `backFigures`. So AreaMapsLazyTest still covers them (it scans `BackKt`), and it passes.
- `figures/KitBack.kt` is new: the back batch's kit pieces, wired into `Kit.kt`.

## Changes to shared code (no existing figure's drawing changed; proven by the golden)

**Body.kt**
- `Pose.shrug` (cm, default 0) lifts the arms' roots (the limb shoulder joints) along the torso.
- The torso axis, the neck base and the head stay put.
- `Skeleton.shrug` carries the value to the renderer.
- New `Body.WAIST_ABOVE_HIP` / `Body.WAIST_DEPTH`.

**FigureRenderer.kt**
- With a shrug, the torso outline's shoulder-top points rise by the shrug. The neck-base points rise 0.3 of it.
- With shrug = 0 the outline is exactly as before.
- `cableEnd` also recognises the new cable attachments.
- `drawFront` now gets the skeleton, so the band and the pullover machine's lever can be drawn in front of the body.

**Figure.kt**
- `BodyPoint.NECK` and `BodyPoint.WAIST`.
- `Surface.CHEST_PAD`.
- KeyCheck `UpperArmDown(within)`: the near upper arm lies along the torso's line, pointing down it.

**FigureChecks.kt**
- `UpperArmDown` added.
- Bar radii for the new held pieces.
- Cable attachments, the cambered bar and the pullover dumbbell count as two-handed for the grip check.

**Kit.kt**
- `Dumbbell.endOnFront` (default false): in the front view, a dumbbell held at the side with the palm in is drawn end-on, as a disc.
- `LatPulldownSeat` gains a front view: the seat, and a roller across the thighs drawn in front, with `FRONT_HALF`. The side view is unchanged.
- The `when` branches for the new pieces.

**Poses.kt / Chest.kt**
- Chest.kt's private 3D arm helpers (`V3`, `arm3`, `armFrom`, `DEG`, `FORE_LINK`, `mirror`) moved unchanged to Poses.kt as `internal`, so the back figures can use them.
- No chest output changed.

## Kit (KitBack.kt)

Each piece has a typical, neutral size in its comment and a KitTest case (+9 KitTest tests).

| Piece | What it is |
|---|---|
| `RowStation` | Seated row bench, 35 cm foot plate tilted 15°, low pulley and cable |
| `RowMachine` | Seat, upright chest pad, lever handles |
| `ProneBench` | Chest-support pad rising towards the head, optional foot platform |
| `TBarLever` | Floor pivot, accent tube, plates along it, crossbar |
| `RackBar` | Rack upright with J-hook; the bar is drawn in front of the hands |
| `AssistPullUpMachine` | Column, beam to an end-on bar, knee pad on a lever |
| `ParallelHandles` | Neutral handles, drawn in profile |
| `PullUpBand` | Two strands from the bar to under the near foot, drawn in front |
| `DipBelt` | Chain and plate: a disc in the side view, edge-on in the front view |
| `PulldownLever` | Plate horn, pivot, handle |
| `PulloverMachine` | Seat and back pad; the lever and elbow pad are drawn in front of the arm |
| `HyperBench` | 45° thigh pad, foot plate, ankle roller, frame |
| `ChestPlate` | A plate held flat on the chest, edge-on |
| `Spotter` | A simplified second body in `BODY_FAR` |
| `CableAttachment` | V-handle (grip square to the forearm), straight bar, or rope |
| `CableHandle(hand)` | A single D-handle |
| `CamberedBar` | Plates hang 12 cm below the hands |
| `PulloverDumbbell` | In profile along the forearms, centred back from the palms |

## Per figure

The cameras are explained in the Back.kt header.
- **Front view (5):** Underhand pulldown, One-arm pulldown, Chin-up, Barbell shrug, DB shrug.
- **Side view (25):** everything else.

All captions are 24 characters or fewer, and all guideLines are verbatim (enforced by the checks).

**Values I chose ("neutral") and sources.** Each is marked in its code comment. Sources are the ExRx, ACE or NSCA pages already in Sources.kt, plus one PMC page.

### Rows

| Figure | Camera | What is drawn and why |
|---|---|---|
| Row | side | Torso 20°, knees 25°, shins upright. START: arms straight, the bar in front of the shins over the mid-foot. END: bar touching the upper abdomen ("upper waist"), on a straight-up path, elbows up past the back. See the torso note below. ExRx BBBentOverRow. |
| T-bar row | side | Torso 25° (neutral "torso stays down"). The lever pivots on the floor 150 cm behind; the plates are past the handles. END: the handle moves up its arc (constant lever length) until it touches the torso. ExRx LVTBarRow. |
| Seated cable row | side | 42 cm bench, torso upright in both panels, knees slightly bent. START: arms straight, 15° down towards the pulley. END: V-handle at the waist, elbows close to the ribs (arm in the plane, elbow behind the torso). ExRx CBSeatedRow / CBStraightBackSeatedRow. |
| Wide-grip cable row | side | Straight bar, hands 36 cm out. END: bar at the waist, elbows out and back (Arm.out 25–70°, bounded in the test). ExRx CBWideGripSeatedRow. |
| One-arm cable row | side | Single handle; the far arm hangs. START: the arm reaches forward. END: handle at the side of the waist, 24 cm out. ExRx CBOneArmRow. |
| Machine row | side | 45 cm seat, chest on the pad. START: handles level with the shoulders. END: elbows past the back of the torso. Handles 34 cm out, so the folded arm stays within the elbow limit. The lever pivots low, at constant length. ExRx LVSeatedRow. |
| Chest-supported row | side | 30° incline ("slight", neutral; ExRx DBInclineRow says "gently sloped"). Chest on the pad, toes on the floor. START: arms hang straight, the dumbbells clear of the floor. END: elbows 30° past the torso line, forearms hanging. ExRx gives no grip, so the dumbbells are end-on discs. |
| Seal row | side | 95 cm flat bench (neutral, the plates clear the floor), body flat, cambered bar. END: hands at the bench top over the upper abdomen. ExRx CamberedBarLyingRow. |
| Chest-supported T-bar row | side | 40° pad (neutral), toes on the platform, a T-bar lever under the pad pivoting behind. END: handles up the arc until 32 cm from the shoulder. ExRx LVInclineRow. |
| Inverted row | side | Body rigid; the heels fixed on the floor (the body pivots on them). END: chest touching the bar at 35° (neutral). START: solved for straight arms at the bar ("just high enough"). ExRx BWSupineRow. |

**Row torso:** with the fixed lengths, a level torso and the bar over the feet need a squat's knee bend. So 20° is drawn, close to the guide's strict "level".

### Pulldowns

| Figure | Camera | What is drawn and why |
|---|---|---|
| Close-grip pulldown | side | The approved Lat pulldown's seat, lean and START, rebuilt; START is the same pose (tested). V-handle in profile, 4–8 cm in front of the upper chest at END. |
| Machine pulldown | side | Lat pulldown body. A plate-loaded lever pivots behind; END is level with the upper chest. ExRx LVFrontPulldown. |
| Straight-arm pulldown | side | Seated facing away from a high pulley, straight bar, a fixed 15° elbow bend. START: 160° overhead. END: forward and down until just above the thighs. ExRx CBSeatedStraightArmPullover. |
| Underhand pulldown | front | Shoulder-width grip (25 cm out). END: bar at the upper chest, elbows down at the sides; elbow bounds in the test. ExRx CBUnderhandPulldown. |
| One-arm pulldown | front | START: arm straight up. END: handle at the side of the chest. The far hand rests on the roller. ExRx CBOneArmPulldown. |

### Pull-ups and chin-ups

| Figure | Camera | What is drawn and why |
|---|---|---|
| Chin-up | front | Shoulder-width underhand grip. END: elbows at the sides, the chin about at the bar. Legs bent back; the crossing is not visible from the front. ExRx BWUnderhandChinup. |
| Weighted pull-up | side | The approved Pull-up poses (equal, tested) plus a dip belt; the plate hangs below the legs (60 cm chain, neutral). ExRx WtPullup. |
| Weighted chin-up | side | Moved from front to side so the plate shows (edge-on from the front it was a sliver). END: elbows at the sides (upper arm 8° off the torso line, elbow 145°). ExRx WtUnderhandChinup. |
| Assisted pull-up | side | Kneeling on the pad (ExRx AsPullupKneeling). The pad rides up on a lever of constant length. END: neck at hand height. |
| Assisted chin-up | side | Same machine. END: elbows at the sides. ExRx AsUnderhandChinup. |
| Band-assisted pull-up | side | Banded leg straight, the free foot on top of it, the band drawn in front. END: chin above the bar. ExRx ASPullupBand. |
| Neutral-grip pull-up | side | Parallel handles in profile. END: neck at hand height, the handles 17 cm in front of the shoulder. ExRx BWPullupN. |

### Pullovers

**DB pullover** (side):
- Upper back across the end-on bench, hips let down (torso 12°), feet flat.
- One dumbbell in profile along the forearms, a fixed 15° bend.
- START: over the chest. END: upper arms 10° short of in line with the torso.
- The guide's safety line is "Use a spotter.", so a spotter half-kneels at the head with the hands near the wrists. This is the first spotter in the plan.
- Half-kneeling and hands near the wrists are from ACE's chest press (id 19) and lying chest fly (id 21) pages, as the search result showed them.
- ExRx DBPullover.

**Machine pullover** (side):
- The pivot is at the shoulder joint, elbows at a fixed 100°.
- START: upper arms 170° (in line, the conservative side), hands behind the head.
- END: elbows at the sides.
- ExRx LVPullover.

### Shrugs

The shrug is 7.5 cm. PMC9937811 gives a shrug's clavicle elevation as 25° (SD 5°). On a clavicle of about 15 cm (neutral), 25–30° lifts its outer end 6–7.5 cm, so 7.5 is the upper end, chosen so the rise shows at phone size.

| Figure | Camera | What is drawn |
|---|---|---|
| Barbell shrug | front | Grip 25 cm out, bar in front of the thighs. ExRx BBShrug. |
| DB shrug | front | Dumbbells end-on at the sides, clear of the thighs. ExRx DBShrug. |
| Trap-bar shrug | side | Side view, so the frame shows. ExRx TBShrug. |

### Back extensions and face pull

**Back extension / Weighted back extension** (side):
- The 45° line. The thigh pad's top edge is 12 cm below the hip, so the belly is clear of it at the bottom.
- Heels under the roller, soles on the plate.
- START: in line with the legs (hip 0, no arch). END: hip at 90°.
- The hands are crossed on the chest. Weighted: a 35 cm plate on the chest.
- ExRx BW45HyperextensionHips / Wt45BackExtension.

**Face pull** (side):
- Knees 10°, feet parallel, rope on a high pulley.
- START: arms straight in front of the face.
- END: hands beside the face, upper arms out of the plane (55–90°), elbows level with the shoulders, forearms up.
- NSCA face pull and ExRx CBStandingRearDeltRowRope.

## Tests

| Suite | Result |
|---|---|
| domain jvmTest | 389 passed, 1 skipped |
| domain jsNodeTest | 404 passed, 1 skipped |
| checkWebDomain | ok (bridge regenerated) |
| app testDebugUnitTest | 301 passed |
| web unit | 657 passed |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

Flakes and reruns:
- **JS:** the known GoldenScenarioTest 2 s Mocha timeout came up once in the combined run; the rerun passed.
- **e2e:** the first run failed `plan.spec.ts:186` ("the plan is edited through its dialogs and survives a reload", 375 px). The rerun passed 90/90. This is a reload test, but not the reload.spec the brief names.

New tests:
- `BatchBackTest`: 31 tests (one per new figure, plus the area test). Real bounds for torso angles, where the handle ends, straight arms, contacts, constant lever lengths, elbows at the sides, shrug height, the in-line top and the right-angle bottom. Explicit elbow bounds for every out-of-plane arm (wide-grip row, one-arm row, machine row, underhand and one-arm pulldown, chin-up, face pull).
- KitTest +9; FigureChecksTest +1 (UpperArmDown pass and fail); BodyTest +1 (shrug lifts only the arm roots and the shoulder tops; zero shrug unchanged).

**RED/GREEN**
- RED: with the batch removed from `backFigures`, BatchBackTest ran 31 tests and all 31 failed.
- The first full run then failed on real problems, each fixed:
  - captions over 24 characters;
  - limbs out of reach;
  - thighs through the pads;
  - the T-bar shoulder past −60°;
  - the DB pullover torso through the bench;
  - the machine row elbow past 150°.
- GREEN: all pass.

**Web test change outside the brief's files.** `web/src/screens/library/ExercisePicture.test.tsx` (`exercisesTab_coldList_buildsOnlyTheNearTilesFigures`) assumed that the catalogue rows right after its eight leading drawn rows have no figure. The new back figures broke that (e.g. "Assisted chin-up" sorts near the top). The test now puts the rows without figures straight after the leading eight; its intent is unchanged. At Task 13, when every exercise has a figure, this test will need rethinking.

## Screenshots

- Android: I re-recorded with `--rerun -Pscreenshots.update=true`. No file changed (no back row is in the screenshots).
- Web: no committed screenshot changed (e2e passed).

## Speed

After `./gradlew --stop`, I ran `npx playwright test --project=speed --no-deps --trace off` 3 times. Each run passed 4/4 in 14.1 s.

| Test | Times |
|---|---|
| Exercises tab | 1.8, 1.8, 1.8 s |
| Picker | 1.4/1.1/1.9 s, 1.5/1.0/1.9 s, 1.5/1.1/1.9 s |

The tests print no millisecond readings, and I did not change them.

## Sheets

- `/home/user/powertracker/web/.figure-sheets/back-dark.png` and `back-light.png` (all 33, 3 columns).
- Detail renders:
  - `fix1-{dark,light}.png` (12 revised figures);
  - `fix2-{dark,light}.png` (Inverted row, Underhand pulldown, Close-grip pulldown, Band-assisted pull-up).
- I checked every figure at full size and as a thumbnail in dark, and the light sheet's rows.
- Fixed after looking:
  - **T-bar row:** the arm was drawn along the torso, not hanging.
  - **Chest-supported T-bar row:** the lever was redone as a T-bar under the pad.
  - **Machine row:** the lever moved below, so the figure is no longer tiny.
  - **One-arm cable row:** the far arm now hangs straight down.
  - **Weighted pull-up:** the plate was hidden; now it hangs below the legs.
  - **Weighted chin-up:** moved to the side view.
  - **Assisted pull-up / chin-up:** the floor was missing.
  - **Machine pullover:** the lever and pad now draw in front of the arm.
  - **DB pullover:** the spotter now half-kneels.
  - **Shrugs:** the lift made more visible.
  - **Band-assisted pull-up:** the thumbnail is now END.
  - **Inverted row:** the end height was raised.

## Concerns

1. **Inverted row START.** With the heels fixed and the chest meeting the bar at the top, the straight arms at the bottom lean about 35° towards the feet, and the body is nearly flat. This is geometrically consistent, but it may read as reaching.
2. **Chest-supported T-bar row.** The body on the 40° pad, with the toes on a platform behind, reads like kneeling. The pad is mostly hidden under the torso.
3. **Band-assisted pull-up.** The band drawn in front crosses the face and torso. The free leg is a small grey shape at the feet.
4. **Chin-up (front).** The crossed legs don't show from in front: the shins point away and are drawn short.
5. **Shrug.** 7.5 cm is the upper end of the sourced range, chosen for visibility. The trap-bar shrug's rise (side view) is subtler than the front views.
6. **DB pullover spotter.** The spotter makes the figure busier and smaller. Their hands reach towards the wrists but cannot reach them at the start, so the arms are drawn straight towards the wrists.
7. **Weighted chin-up, Assisted chin-up and the Pull-up family** look alike in the side view; the difference is the END arm (elbows at the sides versus the chin over the bar).
8. **Out-of-plane arms.** Their joint checks are looser (Body.kt). Each such arm is bounded in its own test.

## Fix round 1

Brief: task-7-fix1-brief.md, from review-7-report.md. I amended the Task 7 commit: it is now b35cabb. The message and attribution lines are unchanged.

### Fix 1: Chest-supported row and Chest-supported T-bar row read as kneeling

- New constant `TRAIL`: hip to ankle for legs trailing nearly straight, the knee bent 15° (neutral).
- **Chest-supported row:** the ankle sits `TRAIL` from the hip, on the toes on the floor.
- **Chest-supported T-bar row:** the ankle sits `TRAIL` from the hip at 225°, and the platform sits under the toes.
- The pad still reads as under the chest.
- Both thumbnails checked: they read as lying face down on the pad with the legs behind.
- New test bound: `nearLeg.knee` in 0..30 for both figures.
- RED: at f52f10f the knees were 54.2° and 79.6°.

### Fix 2: Inverted row START (could not fully meet the 15° bound)

**Why 15° cannot be met.** With the hands and heels fixed and the body rigid, the shoulders swing towards the head as the body lowers. Here is the geometry:
- Let r be the distance from the heels to the point that meets the bar. The point's position along the floor is heel − r·cos(angle).
- To fix that, I tried every end angle from 20° to 45°.
- For each, the full hang must have straight arms on the same bar and stay at least 3° above level.
- The smallest lean any of them gives is about 28° from vertical.
- Below about 25°, no rigid body on fixed heels can both hang at full stretch and bring its chest to the same bar.

**What I changed.** The figure now picks that end angle by search. The contact moved to the upper chest, which is closer to the shoulder and leans less. Compared with f52f10f:
- The back at the hang is now within 15 cm of the floor (it was 27 cm).
- The lean is 28° (it was about 35°).

**Tests.** New bounds:
- the arms within 30° of vertical at the hang, the hands towards the feet;
- the back 0..15 cm off the floor;
- the upper chest touching the bar at END.

RED: at f52f10f the back was 27.1 cm off the floor.

**For the controller.** Getting under 15° would need one of these: the heels sliding between the panels, bent knees (not this guide), or an end that is barely higher than the hang.

### Fix 3: Wide-grip cable row START caption

- The START caption is now "Arms straight", from the guide's "Return slowly until your arms are straight". The pose is unchanged.
- The area test asserts the caption. RED: it was "Wider than shoulders".

### Fix 4: Weighted pull-up and Weighted chin-up chain

- New `DipBelt.drawFrontOf`: in the side view the chain is drawn over the near leg, from the belt's hook to the top of the plate, with a gap outline. The front view is unchanged.
- The plate now reads as hanging from the belt.
- The weighted pull-up test checks that the chain runs from the hook to within 4 cm of the plate's top.

### Fix 5: Size comments and the one-arm pulldown elbow bound

- KitBack.kt has size notes on Spotter, CableAttachment (cm for `STRAIGHT_HALF` and `HANDLE`), CableHandle and PulloverDumbbell.
- The One-arm pulldown test now bounds the elbow's sideways position: 0..12 cm outside the shoulder.

### Golden and tests

- Golden: `91:b4bc0637353c0a91`. With the new back entries removed it is still `61:bf681f219217a906` (re-proved).

| Suite | Result |
|---|---|
| domain jvmTest | 389 passed, 1 skipped |
| domain jsNodeTest | 404 passed, 1 skipped |
| checkWebDomain | ok |
| app testDebugUnitTest | 301 passed |
| web unit | 657 passed |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

- No flakes this round.
- I re-recorded the Android screenshots; none changed.
- `./gradlew --stop` done. No speed runs, as the brief says.

### Sheets

- `/home/user/powertracker/web/.figure-sheets/back-{dark,light}.png` (all 33).
- `/home/user/powertracker/web/.figure-sheets/fx-{dark,light}.png` (the six changed figures).

### Concern

The Inverted row START lean is about 28°, not under 15° (see Fix 2).
