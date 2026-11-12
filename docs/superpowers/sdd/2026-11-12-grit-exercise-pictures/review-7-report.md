# Review of Task 7 (30 back pictures), commit f52f10f

Verdict: Needs fixes. Critical 0, Important 1 (two figures, one cause), Minor 8.
Checked: all 33 back figures rendered one at a time at full size in dark (r7-<Name>-dark.png), the whole area as a 2-column sheet, a 33-thumbnail montage at 2x, light for six of them; the guides in areas/Back.kt read against every pose and caption; figures/areas/Back.kt, KitBack.kt, Kit.kt, Body.kt, Figure.kt, FigureChecks.kt, FigureRenderer.kt, Poses.kt, Chest.kt diffs; BatchBackTest, KitTest/BodyTest/FigureChecksTest additions; the web test diff; the commit message.

| Figure | Verdict | Figure | Verdict |
|---|---|---|---|
| Row | OK | Chin-up | OK (Minor) |
| Seated cable row | OK | Weighted pull-up | OK (Minor) |
| Wide-grip cable row | OK (Minor) | Weighted chin-up | OK (Minor) |
| One-arm cable row | OK | Assisted pull-up | OK |
| Machine row | OK | Assisted chin-up | OK (Minor) |
| Chest-supported row | Fix | Band-assisted pull-up | OK (Minor) |
| Seal row | OK | Neutral-grip pull-up | OK (Minor) |
| T-bar row | OK | DB pullover | OK |
| Chest-supported T-bar row | Fix | Machine pullover | OK |
| Inverted row | OK (Minor) | Barbell shrug | OK |
| Close-grip pulldown | OK (Minor) | DB shrug | OK |
| Underhand pulldown | OK (Minor) | Trap-bar shrug | OK (Minor) |
| Machine pulldown | OK | Back extension | OK |
| One-arm pulldown | OK (Minor) | Weighted back extension | OK (Minor) |
| Straight-arm pulldown | OK | Face pull | OK |

(Pull-up, Lat pulldown and One-arm DB row are the three older figures: unchanged, see below.)

## Important

1. Chest-supported row and Chest-supported T-bar row: the legs read as kneeling (figures/areas/Back.kt, chestSupportedRow about line 545, ankle at `pelvis.x - 54.0` with `Reach.leg`; chestSupportedTBarRow about line 616, ankle at `pelvis + unit(235.0) * 66.0`). Render: `cd web && node scripts/figure-sheet.mjs --label r7x --columns 1 "Chest-supported row"` (and the T-bar one). Measured on a 2x crop of the START panel:
   - Chest-supported row: thigh about 22 degrees off vertical, then the shin runs almost level back to the toes: the knee is bent about 50 degrees.
   - Chest-supported T-bar row: the thigh hangs straight down from the hip and the shin lies nearly flat back along the platform: bent about 70 degrees. It looks like a person kneeling on a padded platform, with the pad mostly hidden under the torso.
   - A lifter on an incline pad has the legs trailing nearly straight behind, the toes on the floor or platform. Neither guide fixes the knee, so nothing in the guide justifies the bend; it is a by-product of reaching the foot to a spot that is closer than the leg's length.
   - The tests pass because they do not bound the knee at all (chestSupportedRow_... checks the bench angle, chest contact, straight arms, dumbbell clearance, elbow past the back; chestSupportedTBarRow_... checks the pad, platform contact, lever length, handle rise). A kneeling pose passes them.
   - Fix: put the feet further back/lower so the knee is about 10 to 25 degrees (for the T-bar row, move the platform back and down, or lengthen the 66 cm chord to about 85 cm; for the dumbbell row, move the ankle back to about 80 cm from the hip), keep the toes on the floor or platform, and add a bound on `pose.nearLeg.knee` (for example 0..30) to both tests. Re-check the thumbnails; the pad must still read as under the chest.

## Minor

1. Grip differences that the side view cannot show; same pose family at thumbnail size (accept per the Task 5 ruling on Close-grip bench, but the controller should know):
   - Wide-grip cable row vs Seated cable row: identical at thumbnail and nearly at full size (START pixel difference about 290 px of a 390x320 panel; END differs only by a flared elbow). The START caption "Wider than shoulders" shows nothing wide; the straight bar is a dot end-on.
   - Close-grip pulldown vs Lat pulldown: START is the same pose by design; END differs only by a small handle vs the bar. Identical thumbnails.
   - Weighted chin-up vs Weighted pull-up, Assisted chin-up vs Assisted pull-up, Neutral-grip pull-up vs Pull-up: the END arm differs slightly (3x crop: elbow tucked vs forearm forward); at 56 x 40 a lifter could not tell each pair apart. Neutral-grip shows a small grey handle cap that is faint at thumbnail.
   - Weighted back extension vs Back extension: the plate is an edge-on sliver on the chest; invisible at thumbnail.
   - Chin-up (front) vs Pull-up (side) do look different, but because of the camera, not the grip. The underhand grip cannot be seen from the front either; the narrower hand spacing is the only cue. Underhand pulldown (front) has the same limit: shoulder-width shows, "underhand" does not.
   - A front view for the cable rows would show width; the cost is a new front-view row station. Not needed now.
2. Inverted row START (builder's concern 1): arms lean about 35 degrees toward the feet and the bar is over the belly, so it reads as reaching. The END (chest to the bar, elbow below) reads right and is the thumbnail. Geometrically consistent. Accept; if touched, raise the bar about 10 cm so the arms hang nearer vertical.
3. Band-assisted pull-up (concern 3): the two band strands run from the bar through the hands and torso to the foot, so the body is hard to read behind them; the 56 x 40 thumbnail is a thin vertical sliver. Physically right for a side view (hands, shoulders and feet are in line). Accept. The free foot is a small grey shape, visible.
4. Weighted pull-up / Weighted chin-up: the belt and chain are drawn behind the thigh (KitBack.kt DipBelt.drawOf, chain from the hip to the plate), so only a tick of chain shows and the plate looks like a ring hanging below the feet. Reads as "weight hanging" anyway. Suggest drawing the chain after the leg or ending it where it leaves the thigh.
5. Chin-up (concern 4): the crossed legs are not visible from the front (two straight legs side by side). The guide's HANG_BRACE line is a bracing cue, not a position, so accept.
6. Shrugs (concern 5): 7.5 cm is visible at full size in front view (shoulder tops and bar rise about 14 px on a 280 px figure; the shoulders come up beside the neck). Trap-bar shrug (side view) is subtler: the near shoulder rises 7.5 cm but a side-on standing figure shows little; compare START and END at 3x to see it. The thumbnails show END only, so no shrug thumbnail can show the rise. Accept; 7.5 cm sits in the sourced range and is not too big.
7. Kit size comments missing: the brief asks every new piece to carry a typical size. Missing on Spotter, CableAttachment (STRAIGHT_HALF = 45.0 and HANDLE = 12.0 have no unit note), CableHandle and PulloverDumbbell (KitBack.kt about lines 166-200). All have KitTest cases. Add a one-line size note each.
8. One-arm pulldown test (BatchBackTest.oneArmPulldown_...): the near arm is out of the plane, and the test has no explicit bound on the elbow's sideways position or on `out` (the brief asked for one); it bounds only the grip, the elbow's height (15..33 below the shoulder) and the figure has UpperArmDown(25). A wrong-way elbow that points inward by up to 25 degrees would pass. In the front view the other way (towards or away from the camera) draws the same, so no visible harm; add `en.near.elbow.x - en.near.shoulder.x` in about 0..12 as Underhand pulldown and Chin-up already do.

## Builder's concerns, judged
- Inverted row START: Minor 2 (reads a little like reaching; accept).
- Chest-supported T-bar row reading like kneeling: confirmed, Important 1, and the dumbbell version has the same problem.
- Band crossing the body: Minor 3, accept.
- Chin-up crossed legs: Minor 5, accept.
- Shrug 7.5 cm: right. Visible in front view, subtle in side view (Minor 6). Not too big.
- DB pullover spotter: the guide's safety line is OVER_FACE_SPOT = "Use a spotter." (areas/Shared.kt:10, used at areas/Back.kt:663), and constraints.md says a spotter belongs where a weight moves over the face. So the spotter is right; it is the only spotter/pin in the batch (no other back figure draws pins or a spotter; the inverted-row bar sits in a rack with no pins, and no other back guide has a rack safety line). The half-kneeling spotter at the head with straight arms to the wrists reads fine at full size; the thumbnail is busy but legible. OK.
- Pull-up / chin-up pairs: Minor 1: not tellable apart at thumbnail for Weighted pull-up vs Weighted chin-up, Assisted pull-up vs Assisted chin-up, and nearly so for Neutral-grip vs Pull-up. Chin-up vs Pull-up differ by camera.

## Counts and catalogue
- 33 BodyArea.BACK entries (BatchBackTest asserts 33), 33 figures by canonical name (3 old + 30 new in `backFigures`); `Figures.all` is 91 (61 + 30); the golden is `91:c7b616b47a2bb62d`. BatchBackTest checks every catalogue name has a figure that passes FigureChecks and that the 5 front-view ones are FRONT. Nothing else got a figure.

## Existing figures (61)
- All 61 are pixel-identical to the base. How: I extracted the base commit's web folder with `git archive 13f8a93 web` into the scratchpad (not a worktree; no checkout), linked node_modules, rendered `all` there (61 figures, label r7base-all), and rendered the same 61 names, in the same order, from the current tree (label r7new61). Full sheets compared with Pillow, dark and light, thumbnails included: same size (5672 x 5984), the only differing pixels are in the label line (y 60..88). So the committed JS bridge draws the same 61 pictures as before, which includes Pull-up, Lat pulldown, One-arm DB row, the whole chest area, Squat, Bench, Deadlift, RDL, the main lifts and DB shoulder press.
- Also rendered with the same arguments as the earlier sheets under new labels and compared with the older sheets: r7b (the ten Task 4 figures) vs cmpb and vs r6b; r7main (Main lifts) vs mainlifts and vs r6main; r7chest (chest, 6 columns) vs chest (rendered after the base commit): all identical apart from the label. The chest sheet from review 6 (r6chest) differs, but that was rendered before the Task 6 fix, so it is not a valid baseline; I did not use it.
- What changed in Chest.kt and Poses.kt: Chest.kt lost its private 3D-arm helpers (`V3`, `v3`, `DEG`, `FORE_LINK`, `arm3`, `armFrom`, `mirror`; 56 lines) and Poses.kt gained them as `internal` (59 lines). Moved unchanged, and the chest render is pixel-identical.
- Shared changes that touch drawing paths: `Pose.shrug` (0 by default, outline unchanged at 0; BodyTest covers both), the torso outline's shoulder-top lift, `cableEnd` recognising the new attachments, `drawFront` now taking the skeleton, `Dumbbell.endOnFront` (default false), `LatPulldownSeat` front view (side unchanged). None moved an existing figure (pixel compare above).

## Sources (citations in the new back code)
Every URL in the new code, with the claim it supports and whether the exact URL is in domain/.../library/Sources.kt. Lines are in figures/areas/Back.kt unless noted. The 6 older citations (lines 74-75, 115-116, 166-167; all in Sources.kt) belong to the approved Pull-up, Lat pulldown and One-arm DB row and were not changed.

| Line | URL | Claim | In Sources.kt |
|---|---|---|---|
| 296 | exrx.net/WeightExercises/BackGeneral/BBBentOverRow | torso bent over close to horizontal (Row) | yes |
| 335 | exrx.net/WeightExercises/BackGeneral/LVTBarRow | lever T-bar row set-up | yes |
| 373 | exrx.net/WeightExercises/BackGeneral/CBSeatedRow | seated cable row body | yes |
| 374 | exrx.net/WeightExercises/BackGeneral/CBStraightBackSeatedRow | straight-back seated row | yes |
| 432 | exrx.net/WeightExercises/BackGeneral/CBWideGripSeatedRow | wide-grip seated row | yes |
| 458 | exrx.net/WeightExercises/BackGeneral/CBOneArmRow | one-arm seated row | yes |
| 486 | exrx.net/WeightExercises/BackGeneral/LVSeatedRow | lever seated row | yes |
| 540 | exrx.net/WeightExercises/BackGeneral/DBInclineRow | "a gently sloped bench" (30 degrees neutral) | yes |
| 580 | exrx.net/WeightExercises/BackGeneral/CamberedBarLyingRow | cambered bar lying row | yes |
| 614 | exrx.net/WeightExercises/BackGeneral/LVInclineRow | lever incline row | yes |
| 656 | exrx.net/WeightExercises/BackGeneral/BWSupineRow | inverted row (35 degrees marked neutral) | yes |
| 722 | exrx.net/WeightExercises/LatissimusDorsi/CBCloseGripPulldown | close-grip pulldown | yes |
| 749 | exrx.net/WeightExercises/LatissimusDorsi/LVFrontPulldown | lever pulldown | yes |
| 779 | exrx.net/WeightExercises/LatissimusDorsi/CBSeatedStraightArmPullover | straight-arm pulldown | yes |
| 851 | exrx.net/WeightExercises/LatissimusDorsi/CBUnderhandPulldown | underhand pulldown | yes |
| 877 | exrx.net/WeightExercises/LatissimusDorsi/CBOneArmPulldown | one-arm pulldown | yes |
| 922 | exrx.net/WeightExercises/LatissimusDorsi/WtPullup | weighted pull-up | yes |
| 946 | exrx.net/WeightExercises/LatissimusDorsi/BWPullupN | neutral-grip pull-up | yes |
| 974 | exrx.net/WeightExercises/LatissimusDorsi/ASPullupBand | band-assisted pull-up | yes |
| 1028 | exrx.net/WeightExercises/LatissimusDorsi/AsPullupKneeling | assisted pull-up, kneeling | yes |
| 1053 | exrx.net/WeightExercises/LatissimusDorsi/AsUnderhandChinup | assisted chin-up | yes |
| 1108 | exrx.net/WeightExercises/LatissimusDorsi/BWUnderhandChinup | chin-up | yes |
| 1133 | exrx.net/WeightExercises/LatissimusDorsi/WtUnderhandChinup | weighted chin-up | yes |
| 1162 | exrx.net/WeightExercises/PectoralSternal/DBPullover | dumbbell pullover | yes |
| 1165 | acefitness.org/resources/everyone/exercise-library/19/chest-press/ (https://www.) | spotter half-kneeling at the head | yes |
| 1166 | acefitness.org/resources/everyone/exercise-library/21/lying-chest-fly/ (https://www.) | spotter's hands near the wrists | yes |
| 1228 | exrx.net/WeightExercises/LatissimusDorsi/LVPullover | lever pullover | yes |
| 1265 | https://pmc.ncbi.nlm.nih.gov/articles/PMC9937811/ | shrug clavicle elevation 25 degrees (SD 5) in a whole-cadaver study | NO: not in Sources.kt |
| 1283 | exrx.net/WeightExercises/TrapeziusUpper/BBShrug | barbell shrug | yes |
| 1304 | exrx.net/WeightExercises/TrapeziusUpper/DBShrug | dumbbell shrug | yes |
| 1324 | exrx.net/WeightExercises/TrapeziusUpper/TBShrug | trap-bar shrug | yes |
| 1377 | exrx.net/WeightExercises/ErectorSpinae/BW45HyperextensionHips | 45 degree bench | yes |
| 1401 | exrx.net/WeightExercises/ErectorSpinae/Wt45BackExtension | weighted back extension | yes |
| 1425 | https://www.nsca.com/education/articles/kinetic-select/face-pull-machine/ | face pull | yes |
| 1426 | exrx.net/WeightExercises/DeltoidPosterior/CBStandingRearDeltRowRope | rope face pull | yes |

(All ExRx rows are `https://exrx.net/...`.) Only the PMC page is new to Sources.kt; it is a domain on the allowed list. I could not check the pages (no WebSearch); the controller should check the 25 degree clavicle figure on PMC9937811 and that its 6 to 7.5 cm arithmetic (a 15 cm clavicle at 25 to 30 degrees) is the builder's own. Other numbers are marked "neutral" with no citation, as the brief asked. Kit.kt:15 (IPF rulebook) is a pre-existing citation. No other URLs in KitBack.kt or Poses.kt.

## Code and tests
- Kit: 18 new pieces; each has a doc comment, and 14 of them a typical size (missing on four, Minor 7). KitTest has 9 new cases (37 in the class) covering the pieces, plus the front-view LatPulldownSeat. BodyTest (shrug) and FigureChecksTest (UpperArmDown pass and fail) have one new case each.
- Laziness: `backBatchFigures` is a `by lazy` map of `lazy { figure }`, merged into `backFigures`; AreaMapsLazyTest passes.
- BatchBackTest: 31 tests, one per new figure plus the area test. Reasoned through three:
  1. Seated cable row: torso 90 at both ends, knees 10..40, straight arms, reach 55..68 cm and 5..25 below the shoulders, pulley beyond the feet, HeldBeside waist, elbow behind the torso 0..25, upper arm 20..50 off the torso, `out` 0: a wrong torso, reach or handle height fails. Strong.
  2. Face pull: out 55..90 at the end, elbow level with the shoulder 0..10, elbow below the hand, elbow behind the hand, hands within 16 cm of the head, high pulley, straight start: a wrong-way elbow fails. Strong.
  3. Chest-supported row: bench 20..35, chest on pad, straight hanging arms, elbow up and back at the end. It has no knee or leg bound, so the kneeling pose passes (Important 1). The Seal row, Row, Machine row and Weighted pull-up tests are real; the Inverted row test does not bound the START arm angle but the contact and straight-arm bounds pin it.
- Out-of-plane arms (`out != 0`) and whether a wrong-way elbow would be caught:
  - Wide-grip cable row: yes (out 25..70, elbow 5..30 behind the shoulder and 0..25 below it).
  - One-arm cable row: yes (elbow at least 10 cm behind the shoulder, out 0..40).
  - Machine row: yes (elbow behind the back's line, 0..20 below the shoulder).
  - Underhand pulldown: yes (elbow 15..33 below, 0..12 outside, out -30..30, UpperArmDown 25).
  - Chin-up: yes (elbow 15..33 below, -2..12 outside, out 0..60).
  - Face pull: yes (above).
  - One-arm pulldown: partly; see Minor 8.
- No model names in code, tests, generated bridge or the commit body other than the two attribution lines (grep over the diff). The commit's attribution lines are "Claude Opus 5.5" plus the session line, which is what the builder's harness gave; the brief allowed that.
- Web test ExercisePicture.test.tsx (`exercisesTab_coldList_buildsOnlyTheNearTilesFigures`): not weakened. The old test relied on the catalogue order putting the eight drawn exercises first and un-drawn ones next; the back figures now sort high in the catalogue (for example "Assisted chin-up"). The new version builds the list as the first 8 drawn exercises, then all un-drawn ones, then the remaining drawn ones, and every assertion is kept: the near rows with a figure equal the leading eight, drawn rows far down exceed that by 10, `hasFigure` is asked for every name, `figureInfo` only for the near drawn rows, and the SVG count matches. It is a little more synthetic (it no longer uses the catalogue's own order). It will fail once every exercise has a figure (the un-drawn middle block becomes empty and drawn rows enter the near window); the builder said so and the controller should plan for it at Task 13.

## Tests run
- `./gradlew :domain:jvmTest --rerun-tasks`: 389 tests, 1 skipped, 0 failures, 0 errors (BatchBackTest 31, KitTest 37, AreaMapsLazyTest 1).
- `cd web && npm test`: 39 files, 657 tests passed.
- Not run (controller's): speed tests, domain JS, Android, e2e.
- `./gradlew --stop` done. `git status` is clean.

## Other
- Sheets I rendered (all new labels, nothing overwritten): web/.figure-sheets/r7back-{dark,light}.png (all 33, 2 columns), r7-<Name>-{dark,light}.png (33 single figures including Pull-up, Lat pulldown, One-arm DB row), r7b-*, r7main-*, r7chest-*, r7chest2-*, r7new61-*. The base-commit renders (r7base-all, r7tmp) and the crops are in the scratchpad under .../scratchpad/base/web/.figure-sheets and .../scratchpad/c.
- Seen and fine: Row, T-bar row (lever from the floor behind, plates past the handles), Seated cable row (V-handle to the waist, elbows to the ribs), Machine row, Seal row (high bench, cambered plates clear the floor, hands up at the bench), Pulldowns (machine lever raises its plate as the handle comes down), Straight-arm pulldown (bend fixed, bar from overhead to above the thighs), Assisted machines (knee pad rides up on a lever), Back extension (45 degrees, in line at the top, right angle at the bottom, belly clear of the pad), Face pull (rope to the face, elbows level), Machine pullover, DB pullover.
