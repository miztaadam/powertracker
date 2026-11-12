# Review of Task 6 (30 chest pictures), commit c88b4b3

Verdict: Needs fixes. Critical 0, Important 1, Minor 5.
Checked: all 30 rendered in dark at full size (sheet, 2 columns, cropped) and as thumbnails; 12 of them again one at a time at larger size (Archer, Lying cable fly, DB fly, Chest dip, Push-up, Cable crossover, Pec deck, Incline bench cable fly, Decline push-up, Knee push-up, Assisted dip, Decline bench press); the light sheet glanced at (second half); catalogue guides read against the poses; the code (figures/areas/Chest.kt, KitChest.kt, Kit.kt, Body.kt, FigureChecks.kt), BatchChestTest, AreaMapsLazyTest, the check and body tests; the commit message.

| Figure | Verdict | Figure | Verdict |
|---|---|---|---|
| DB press | OK | Cable crossover | OK |
| Incline bench press | OK | Low-to-high cable fly | OK |
| Decline bench press | OK | Cable chest press | OK |
| Incline DB press | OK | Lying cable fly | OK (Minor) |
| Decline DB press | OK | Incline bench cable fly | OK (Minor) |
| DB fly | OK (Minor) | Seated cable fly | OK (Minor) |
| Incline DB fly | OK | Chest dip | OK |
| Decline DB fly | OK | Assisted dip | OK |
| Machine chest press | OK | Push-up | OK |
| Incline machine press | OK | Incline push-up | OK |
| Decline machine press | OK | Decline push-up | OK |
| Pec deck | OK (Minor) | Knee push-up | OK |
| Smith bench press | OK | Archer push-up | Fix |
| Smith incline press | OK | Clap push-up | OK |
| Smith decline press | OK | Weighted push-up | OK |

## Important
1. Archer push-up does not read as an archer push-up (figures/areas/Chest.kt, archer figure around line 1454; render `node scripts/figure-sheet.mjs --label r6 --columns 1 "Archer push-up"`). Seen at full size and thumbnail:
   - END (front view): a head high in the middle, a wide torso block, two long arms splayed down to the floor, and two legs hanging straight down below the torso. It reads as a person standing or kneeling with the arms braced wide, a tent shape. Nothing says "plank".
   - START (the thumbnail): a bundled head and torso with legs dropping below it, one arm flat out to the left, one folded on the right. At 56 x 40 it is a blob.
   - Why: an orthographic front view draws the legs from the hips down to the toes, so they stand under the torso. The geometry is right but a lifter would not see a body lying at about 20 degrees.
   - The test passes because it checks the arms, hands and heights only, not how the body reads.
   - Fix, in order of preference:
     a. Draw the legs short and mostly hidden behind the torso/hips (foreshorten them more, or end them at the hip's height with the toes at the floor), so the body reads as lying away from the camera, and check the thumbnail again.
     b. Or keep the side view with the extended arm drawn foreshortened (as in the lying flys) and a caption that names it ("One arm out to the side" still fits). The wide arm then is not seen, so this is the weaker choice.
     c. Or the controller rules to accept it as drawn. It is the one picture a lifter could misread.

## Minor
- Lying cable fly START, DB fly / Incline DB fly / Decline DB fly START or END at the bottom (figures/areas/Chest.kt lying-fly figures): side-on, the arm is a short foreshortened loop at the shoulder, so the picture reads as a bent arm with the hand at the shoulder, not a wide arc. The height limit the guide gives ("level with shoulders") does show, and the bend is fixed in both panels. Accept; a caption already says "Level with shoulders/your chest".
- Lying cable fly and Incline bench cable fly: the cable is not drawn in the side view (only a 75 cm housing behind the head), so the cable source is implied. The kit says the side view of the CableStack is unchanged. Accept.
- Pec deck and Seated cable fly (front view): the seated thighs point at the camera and are drawn as short stubs. Readable with the seat and pad. Accept.
- Cable crossover: the 8 degree forward lean and slightly bent knees ("slightly") cannot be seen from the front. Accept; the guide's cue is "stay upright".
- Check loosening (see "Loosened checks" below): for arms out of the picture plane, an elbow bent the wrong way, and a shoulder raised beyond its flexion limit, are no longer caught by the checks. No current figure is affected; worth a comment or a test if a later batch uses out-of-plane arms.

## Builder's concerns, judged
- Archer END reading as kneeling: confirmed, see Important 1.
- Lying flys side-on with the arm foreshortened: acceptable (Minor above). Side-on is the right camera for a height limit, and the front view would hide the bench contact.
- Push-up hands at 60 degrees, palm heel raised: acceptable. At the sheet size the hands read as small wedges on the floor, hands plant and stay put between panels, and the picture is not wrong to a lifter. The incline push-up's flat hands on the bench read right.
- Loosened arm checks: see below.

## Loosened checks (Body.kt JointLimits.problems, trueShoulderBack, elbowBend)
What changed, for an arm with `out != 0` only (in-plane arms take exactly the old path):
- The drawn shoulder angle is not checked any more; instead the true 3D direction may point at most 60 degrees behind the body.
- The elbow bend is `abs(trueElbow)`, so its sign is ignored.

Could a visibly wrong arm pass?
- Yes, in two narrow ways, only for out-of-plane arms: (a) an elbow bent the wrong way (negative drawn bend) cannot fail, because the sign is thrown away (the new BodyTest case uses `Arm(0, -18, out = 40)` as the legal fly arm, so it is by design); (b) the upper limit of shoulder flexion is no longer checked for such arms (only "behind the body"). Both are anatomically defensible for a projected arm, and the per-figure tests (ElbowBend ranges 10..30, lockout over the shoulders, grip positions) bound every current out-of-plane arm closely.
- Nothing currently drawn is wrong because of it: I looked at every out-of-plane arm in the chest figures and they are right.

Did it change any check on the 31 existing figures?
- Three of them have `Arm.out != 0`: DB shoulder press (areas/Shoulders.kt:53), Wide-grip bench (areas/MainLifts.kt:741-742) and Barbell floor press (areas/MainLifts.kt:772-777). Their shoulder and elbow checks now take the new path. All three still pass (domain jvmTest green), and their drawing is pixel-identical (below), so no verdict or picture changed. The other 28 take the old path unchanged. The wider range for a negative `Leg.out` (front view only) and for `Arm.out` below 0 loosens a range no existing figure uses.

## What passed
- Poses vs guides: DB presses and incline/decline variants (bench angle, dumbbells beside the chest, lockout over the shoulders), bar presses (bar to the chest at START, rack with pins only on incline and decline), Smith presses (bar on the rail, stops under the bar), machine presses (seat, back on the pad, handles at the guide's height and direction: level, up about 35, down about 20; clearly different at thumbnail), flys (fixed bend, over chest then level), cable flys (high, low, shoulder, seated), cable press split stance, dips (straight arms with shoulders over hands, upper arms level, elbows flared), push-ups (straight line, chest near the floor, hands and toes stay put).
- Near-duplicates side by side: flat vs incline vs decline presses, DB flys, machine presses and Smith presses are all told apart at thumbnail size; Crossover vs Low-to-high differ in both panels.
- Captions: enforced at 24 characters or fewer and guide wording by FigureChecks; the ones I read fit their poses.
- Cameras: front view for the hugging flys, pec deck and archer; side view elsewhere. Right for everything except the weak archer (Important 1).
- Pins/spotter: a rack with pins only on Incline and Decline bench press, whose guides carry RACK_SAFETY. Smith stops are drawn because the guide says to set them. No dumbbell figure has a spotter.
- Catalogue: 30 BodyArea.CHEST entries (29 `chestEntry(` calls plus "DB press"), 30 figures by canonical name in `chestFigures`; `Figures.all` has 61 (31 + 30), the golden count agrees; BatchChestTest asserts the set. Nothing else got a figure.
- Kit: every new piece has a size comment (decline bench, Smith, press machine, pec deck, seat, dip bars, assisted dip machine, back plates, extended CableStack with the real-station note) and a KitTest case (7 new). Check and body changes each have pass and fail tests.
- Sources in comments: only exrx.net, acefitness.org and pmc.ncbi.nlm.nih.gov. I could not run WebSearch in this session (the tool is not available to me), so I did not spot-check five citations on their pages; the URLs and what they are cited for read as plausible (ExRx page names match the exercises, PMC7579505 for incline/decline angles, PMC9659300 for dip lean). The controller may want to check them.
- Tests are real. Three reasoned through: (1) Incline bench press: bench 45, pins 4..6 cm under the bar's lowest point, bar over the upper chest within 2 cm, arms straight 0..1: a wrong angle, pins or lockout fails. (2) Chest dip: shoulders over hands 0..1, elbow 85..95, torso lean 30..37, upper arm level 0..2: a visibly wrong depth or lean fails. (3) Push-up: chest 4..9 cm off the floor, bottom elbow 90..145, hands on the floor -0.6..1.5, toes and hands put: fails if the body sags, floats or the hands move. The weakest bounds: the lying-fly "arm out to the side" (grip within 25 cm of the shoulder) and the Archer test (arms and heights only, see Important 1).
- Laziness: `chestFigures` is `by lazy { mapOf("X" to lazy { x }) }`. AreaMapsLazyTest loads the domain again in a child-first class loader (so no other test's state is in it), builds `Figures.all` through its keys, size and `in`, then checks by reflection that every area file's `$delegate` Lazy (except the map delegates) is unbuilt, every map entry's Lazy is unbuilt, no area file holds an eager ExerciseFigure/Pose/Skeleton/Scene field, and that looking up "DB press" builds only dbPress and not the others. It would fail if an area built eagerly: `lazyOf(figure)` or `.value` initialises a delegate (caught by the second and third assertions); a top-level eager `val` of those types is caught by the field-type scan; an eagerly built map would fail the `Lazy` cast. Small gap: an eager field typed as something else (a List<Pose>) would not be flagged. The builder's RED run (with `lazyOf(pushUpFigure)`) is consistent with this reading; I did not repeat it.
- Commit: title `feat(domain): chest pictures`, short body, ends with the two attribution lines; no model names in the diff or message apart from those. No file outside the expected set changed: docs/design/screens/exercises-muscles-{dark,light}.png (Android), the web domain bridge, domain code and tests.

## Tests
- `./gradlew :domain:jvmTest` (rerun, not cached): 343 tests, 1 skipped, 0 failures, 0 errors (includes BatchChestTest 31 and AreaMapsLazyTest 1).
- `cd web && npm test`: 39 files, 657 tests passed.
- Not run (controller's): domain JS, Android, e2e, speed.
- `./gradlew --stop` done.

## How I checked the existing 31 figures
- Rendered, with the same arguments as before the task, under new labels:
  - `node scripts/figure-sheet.mjs --label r6b --columns 6 Squat Bench Deadlift "Strict press" "DB shoulder press" "Lat pulldown" "Leg press" Pull-up "One-arm DB row" RDL` (the 10 Task 4 figures);
  - `node scripts/figure-sheet.mjs --label r6main --columns 4 "Main lifts"` (the 25 main-lift figures: the 21 Task 5 ones plus Squat, Bench, Deadlift and Strict press, including Sumo deadlift, Wide-grip bench and Barbell floor press).
  Together that is all 31.
- Compared pixel by pixel with Pillow against `cmpb-{dark,light}.png` (the same 10, rendered at Task 4 review) and `mainlifts-{dark,light}.png` (Task 5), full sheets including the thumbnails. Same size; the only differing pixels are in the label line at the top (y 60..88). Dark and light both.
- My mistake to own: my first render of the 10 used the label `batch1`, which overwrote web/.figure-sheets/batch1-{dark,light}.png. The bytes are the same size as before (305439 and 293487), and the sheets also match cmpb exactly apart from the label, so the comparison stands, but the original batch1 files are replaced by an identical render of the current code.
- The builder's own proof (golden unchanged at 31:29075f43d649812c with the chest map emptied) agrees; I did not repeat it.

## Other
- Sheets I rendered: /home/user/powertracker/web/.figure-sheets/r6chest-{dark,light}.png (all 30, 2 columns), r6-<Name>-{dark,light}.png (12 single figures), r6b-*, r6main-*. Crops are in the scratchpad.
- The working tree is clean (git status shows no changes).
