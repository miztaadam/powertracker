# Review of Task 8 (23 shoulder pictures), commit 3df8d7e

Verdict: Needs fixes. Critical 0, Important 2, Minor 9.

Checked: all 24 shoulder figures rendered one at a time at full size in dark (web/.figure-sheets/r8-<Name>-dark.png) and light (same names), the whole area as a 4-column sheet (r8sh), a 24-thumbnail montage at 2x in dark and light, 2x to 4x crops of the rear-delt and press START panels, the cable reverse fly and cable rear delt row; the guides in areas/Shoulders.kt read against every pose and caption; the diffs of figures/areas/Shoulders.kt, KitShoulders.kt, Kit.kt, KitChest.kt, FigureChecks.kt, Back.kt, BatchShouldersTest, KitTest, FiguresTest; the commit message.

| Figure | Verdict | Figure | Verdict |
|---|---|---|---|
| DB shoulder press (existing) | OK, unchanged | Seated rear delt raise | Fix |
| Seated barbell press | OK | Chest-supported rear delt raise | OK (Minor) |
| One-arm DB press | OK | Rear delt raise | OK (Minor) |
| Arnold press | Fix | Machine reverse fly | OK (Minor) |
| Machine shoulder press | OK (Minor) | Cable reverse fly | OK (Minor) |
| Smith shoulder press | OK (Minor) | Cable rear delt row | OK (Minor) |
| Landmine press | OK | Cable external rotation | OK (Minor) |
| Kettlebell press | OK | Side-lying external rotation | OK |
| Lateral raise | OK | Cable internal rotation | OK (Minor) |
| Seated lateral raise | OK (Minor) | Front raise | OK |
| Cable lateral raise | OK | Barbell front raise | OK |
| Machine lateral raise | OK | Cable front raise | OK |

## Important

1. Arnold press (figures/areas/Shoulders.kt:371-386). The whole point of this exercise (guide purpose: "starts with your palms facing you and opens out as you press") does not show. Render: `cd web && node scripts/figure-sheet.mjs --label r8x --columns 1 "Arnold press"`. What I saw, side by side with DB shoulder press, One-arm DB press and Kettlebell press START (crop at scratchpad press_starts.png):
   - START: a standing figure, upper arm forward and down, forearm upright, a dumbbell disc beside the face. It is the same drawing as the DB shoulder press START and the One-arm DB press START, except the figure stands (not seated) and has two arms. The elbow-in-front-of-the-chest cue is a 42 degree forward upper arm, which in a side view looks like the other presses' elbow.
   - END: identical to the One-arm DB press END (standing, straight arm overhead, disc) and, apart from the bench, to the DB shoulder press END. Nothing tells it from them.
   - Caption "Palms facing you": nothing in a side view with end-on discs shows a palm (brief lesson 7).
   - The builder's reasoning is right: the handle runs across the body both with palms facing you and with palms forward, so a side view always sees a disc. That is why the side camera cannot show this exercise. Not a bug in the code, a wrong camera choice for what the guide is about.
   - Fix (my suggestion): draw it from the front (frontStanding exists; lateralArms/arm()/bent() give 3D arms). START: forearms upright, the upper arms pointing forward (towards the camera, foreshortened), the elbows together in front of the chest, the hands 15 to 25 cm apart at chin height, the dumbbells seen in profile (default front view, handles across) in front of the chin. END: arms up and straight, hands about shoulder-width apart. Then START (hands together in front of the face) and END (arms up and apart) differ from the DB shoulder press (elbows out wide) and from the One-arm DB press at thumbnail. Caption START "Elbows in front, palms in" is not needed; "Dumbbells at your chin" or "Elbows in front of you" would be what the panel shows. Add a test bound on the hands' distance apart and the elbows' forward `out` (the elbows are out of the plane; a wrong-way elbow must fail). If the controller prefers to keep the side view, the caption at least must change to something the panel shows ("Elbows in front of you"), and this stays a known limitation.

2. Seated rear delt raise (Shoulders.kt:791-808; caption at line 796). START reads as an upright seated person with hanging arms, not as bent over with the chest on the thighs. Render: `--label r8x --columns 1 "Seated rear delt raise"`; crop at scratchpad rear_starts.png (second panel). What I saw:
   - The head and shoulders are at a normal seated height; the torso is only a little shorter than in Seated lateral raise START. A lifter would read it as "sitting with weights hanging", the same as Seated lateral raise START.
   - Caption "Chest on your thighs": the thighs point at the camera and the chest is not visible against them; the panel shows neither (brief lesson 7; the builder says the chest stops 5 cm above the thighs, the code comment at line 786-787). So the START caption names something the panel does not show.
   - At thumbnail (END, the T) Seated rear delt raise vs Seated lateral raise differ by a head and shoulders sitting about 8 px lower in a 80 px-high thumbnail at 2x. A lifter will not tell the two apart. The guide's difference (bent over) is the only difference between the two exercises, so it must show.
   - Fix: make the lean visible and change the caption to what shows. For example a larger neckOut and a head that sits clearly below the shoulder line (as Rear delt raise now does: head sunk between the shoulders), shoulders drawn lower, torso foreshortened to a short block; or, if the 140 degree hip limit stops it, a START caption such as "Bend over, arms hang" with the head clearly low. Re-check the thumbnail next to Seated lateral raise: the head must clearly sit lower. Also add a test bound on the shoulder height above the seat (or the head's y relative to the shoulder's) so that an upright pose fails; the current test only bounds lean 50..65.

## Minor

1. Rear delt raise (standing) START (concern 1): reads as a hunched figure bending towards the camera, with the dumbbells at the knees; the head sunk between the shoulders helps; recognisable to a lifter who knows the exercise, but abstract. END (the T with the low head, thumbnail) reads well. A side view would read the hinge much better for START, but END would then show the arms as stubs pointing at the camera, so the front view is the right choice. Accept.
2. Chest-supported rear delt raise START: the bench is a post with a thin pad, the torso a block on it, the head above, arms hanging to the floor; at first glance it looks like a person standing behind a stool. The caption "Lie face down" is not shown by anything but the bench; END (the T over a post) is fine. Accept; a bench top wider than the torso would help.
3. Machine reverse fly START: arms foreshortened to nubs at the shoulders, the pad drawn over the chest, levers as a tent above the head. A lifter who knows the pec deck recognises it from the pad and the levers; a stranger may not. END (arms out, levers as wings) reads well. Accept.
4. Cable reverse fly (concern 5; Shoulders.kt:887-913): drawing order. In END the image-left arm passes behind its column and the image-right arm passes in front of its column (crop scratchpad crf_end.png): the far arm is drawn before the props, the near arm after. The header says the columns are beyond the lifter (seen from behind), so both arms should be in front of both columns. In START the crossed forearms are drawn over the torso, which from behind would be hidden. The crossed cables are a thin line on one arm at END and not visible at thumbnail. Minor: the exercise still reads (arms out at shoulder height between two towers). Suggest drawing the columns before the far limbs, or moving the columns outwards.
5. Machine shoulder press START (Shoulders.kt:273-307): the lever's grey line runs behind the head to the hand, so the head sits across the lever; hands at about ear height (8 cm above the shoulder joint, test bound 0..12). Reads right to a lifter ("level with the shoulders or a little above"); the caption "Level with shoulders" is fair.
6. Smith shoulder press (concern 4): the bench back is reclined 10 degrees and there is no LockoutOverShoulders check, since the rail is 8.5 cm in front of the shoulder joint. At END the straight arm leans forward about 10 degrees to the bar. The guide says "press overhead until your arms are straight" and "pull your head back slightly", so a slight recline and a rail in front of the face are consistent; the stops are drawn under the START bar (the guide's safety line is "Set the safety stops", so drawing them is right). Accept. The test does bound the head clearance 0.3..3 cm in both panels and the straight arm, so a wrong pose fails.
7. Seated lateral raise (Shoulders.kt:667-685): the bench seen end-on is a grey post and a small pad; from the front the figure can look standing next to a stool. At thumbnail the bench post is the only sign of "seated" (visible in the montage). Accept.
8. Rotations (concern 6, judged): each rotation reads. External: the elbow is at the side (upper arm straight down), the forearm goes from across the belly (START, hand at the middle of the body) to pointing out to the side (END, hand away). Internal is the same two poses the other way round. They can be told apart at full size and thumbnail by the pulley's side and the START/END order (External thumbnail: pulley on the left, forearm out to the right; Internal thumbnail: pulley on the right, forearm across the belly), and the captions differ. A lifter who knows the exercises will see the difference; to a stranger the two thumbnails are mirror images, which is inherent. The end points (35 degrees across, 45 degrees out) carry no source and are marked neutral in the comment at Shoulders.kt:921-927, as the brief asked; 45 degrees out is on the conservative side of the usual ~60 to 90 degrees. Cable external rotation START: the cable crosses the belly in front of the hand as a line over the torso; fine. Side-lying external rotation reads clearly (the elbow along the torso, the forearm hanging then pointing up with the dumbbell in profile; the thumbnail shows END, a person lying with a dumbbell up).
9. Test coverage of legs (lesson 5): BatchShouldersTest bounds the legs only for the Kettlebell press (knee 0..20), Landmine press (front knee 15..60, back 0..40), Rear delt raise (knees out 15..35), Seated lateral raise (legs together) and Seated rear delt raise (thighs). The Cable rear delt row has "knees only slightly bent" in its guide and no knee bound (it reuses the back batch's rowPose, and the render shows slightly bent legs, so nothing is wrong today). Seated presses, standing presses and raises have no leg bound either; their legs are built by a fixed helper and look right in the renders. Suggest `pose.nearLeg.knee` 0..40 on the cable rear delt row test, and 0..10 on the standing figures built on `tall`.
   Also the rear delt raises' tests (assertRearRaise) bound `out == 0` but not `forearmOut`, so a forearm angled the other way (towards the back) passes; in a front view it is hardly visible. Add forearmOut 10..40 as assertLateral does.

## Builder's concerns, judged

1. Head-on rear-delt START poses. Standing Rear delt raise: a lifter would recognise it as bending over, but with effort (Minor 1); a side view would read the hinge better, but would hide the raise at END, so keep the front view. Seated rear delt raise: not recognisable as bent over (Important 2). Chest-supported: reads as a body over a bench pad, weak (Minor 2). Machine reverse fly: recognisable by the pad and levers (Minor 3). Cable reverse fly: seen from behind it is two towers and arms out; fine at END (Minor 4).
2. Seated rear delt raise chest 5 cm above the thighs: invisible from the front, so harmless in itself; the harm is that nothing else shows the bend (Important 2).
3. Arnold press: START and END cannot be told from the DB shoulder press at all except for seated versus standing (Important 1). What would show the turn: the front view (elbows and hands together in front of the chest at START, arms up and apart at END), as written under Important 1.
4. Smith press bench reclined 10 degrees, no lockout check: accept (Minor 6).
5. Rotation end points: each rotation reads; external and internal can be told apart by the pulley side and the order (Minor 8).

## Counts and catalogue
- 24 BodyArea.SHOULDERS entries in library/areas/Shoulders.kt (BatchShouldersTest asserts 24); 24 figures by canonical name (1 old + 23 new); `Figures.all` is 114 (91 + 23); the golden is `114:f101883ab2331f52` (FiguresTest). BatchShouldersTest checks that every catalogue name has a figure that passes FigureChecks and which of them are FRONT. Nothing else got a figure.
- Captions: all 24 characters or fewer (the longest, "Hand away from your body" and "Arms level, out to sides", are exactly 24), all from the guides' wording. The two that name something the panel cannot show: Arnold press START "Palms facing you" and Seated rear delt raise START "Chest on your thighs" (both under Important).
- Pins/spotter: Seated barbell press has the rack with J-hooks under the bar and safety arms 10 cm under it (guide: RACK_SAFETY); Smith shoulder press has stops just under the bar (guide: "Set the safety stops"). No other shoulder figure draws pins or a spotter; no other shoulder guide has such a line. OK.

## Existing figures (91)
All 91 are pixel-identical to the base. How: I extracted the base commit's web folder with `git archive b35cabb web | tar -x -C <scratchpad>/base` (no worktree, no checkout), linked node_modules, rendered `all` there (91 figures, label r8base-all, dark and light, 6 columns), then rendered the same 91 names in the same order from the current tree (label r8new91). Full sheets compared with Pillow, dark and light, thumbnails included: both 2836 x 4322, the only differing pixels are in the label line (bounding box x 43..217, y 31..45). That covers back, chest, main lifts, the ten Task 4 figures, DB shoulder press and the rest. So the committed JS bridge draws the same 91 pictures as before. The shared changes (CableStack.crossed, cableInFront in a front view single column, PressMachine.handleEndOn, PecDeck.facingPad, the Kettlebell and LateralRaiseMachine wiring, Back.kt's four `internal` visibility changes) are all defaults that leave the old drawing; confirmed by the compare.

## Sources (citations in the new shoulders code)
Every URL in figures/areas/Shoulders.kt (the file has no other source comments; KitShoulders.kt, Kit.kt and KitChest.kt have no URLs). Lines are in figures/areas/Shoulders.kt. "In Sources.kt" is an exact string match in library/Sources.kt. Lines 66-67 (DB shoulder press) belong to the approved figure, unchanged.

| Line | URL | Claim | In Sources.kt |
|---|---|---|---|
| 66 | https://exrx.net/WeightExercises/DeltoidAnterior/DBShoulderPress | (old figure) upright back | yes |
| 67 | https://www.acefitness.org/resources/everyone/exercise-library/45/seated-overhead-press/ | (old figure) upright back | yes |
| 166 | https://exrx.net/WeightExercises/DeltoidPosterior/DBLyingRearLateralRaise | fixed elbow bend 10 to 30 degrees (RAISE_BEND 18) | yes |
| 167 | https://exrx.net/WeightExercises/DeltoidLateral/DBOneArmLateralRaise | same 10 to 30 degree elbow bend | NO: not in Sources.kt |
| 203 | https://exrx.net/WeightExercises/DeltoidAnterior/BBSeatedMilitaryPress | seated barbell press body | yes |
| 233 | https://exrx.net/WeightExercises/DeltoidAnterior/SMShoulderPress | Smith shoulder press | yes |
| 271 | https://exrx.net/WeightExercises/DeltoidAnterior/LVShoulderPress | machine shoulder press | yes |
| 345 | https://exrx.net/WeightExercises/DeltoidAnterior/DBOneArmShoulderPress | one-arm DB press | yes |
| 368 | https://exrx.net/WeightExercises/DeltoidAnterior/DBArnoldPress | Arnold press | yes |
| 393 | https://www.acefitness.org/resources/everyone/exercise-library/395/single-arm-overhead-press/ | kettlebell single-arm press | yes |
| 424 | https://exrx.net/WeightExercises/DeltoidAnterior/LVOneArmShoulderPress | landmine press (lever one-arm) | yes |
| 482 | https://exrx.net/WeightExercises/DeltoidAnterior/DBFrontRaise | front raise | yes |
| 483 | https://www.acefitness.org/resources/everyone/exercise-library/54/front-raise/ | front raise | yes |
| 505 | https://exrx.net/WeightExercises/DeltoidAnterior/BBFrontRaise | barbell front raise | yes |
| 532 | https://exrx.net/WeightExercises/DeltoidAnterior/CBFrontRaise | cable front raise | yes |
| 559 | https://exrx.net/WeightExercises/DeltoidPosterior/CBRearDeltRow | cable rear delt row | yes |
| 641 | https://exrx.net/WeightExercises/DeltoidLateral/DBLateralRaise | lateral raise | yes |
| 642 | https://www.acefitness.org/resources/everyone/exercise-library/26/lateral-raise/ | lateral raise | yes |
| 665 | https://exrx.net/WeightExercises/DeltoidLateral/DBSeatedLateralRaise | seated lateral raise | yes |
| 692 | https://exrx.net/WeightExercises/DeltoidLateral/CBOneArmLateralRaise | cable lateral raise | yes |
| 716 | https://exrx.net/WeightExercises/DeltoidLateral/LVLateralRaise | machine lateral raise | yes |
| 763 | https://exrx.net/WeightExercises/DeltoidPosterior/DBRearLateralRaise | rear delt raise | yes |
| 789 | https://exrx.net/WeightExercises/DeltoidPosterior/DBSeatedRearLateralRaise | seated rear delt raise | yes |
| 816 | https://exrx.net/WeightExercises/DeltoidPosterior/DBLyingRearLateralRaise | chest-supported rear delt raise (bench close to horizontal) | yes |
| 849 | https://exrx.net/WeightExercises/DeltoidPosterior/LVReverseFlyOverhand | machine reverse fly | yes |
| 885 | https://exrx.net/WeightExercises/DeltoidPosterior/CBStandingReverseFly | cable reverse fly | yes |
| 925 | https://exrx.net/WeightExercises/Infraspinatus/CBStandingExternalRotation | external rotation: elbow at side 90 degrees, forearm across the belly at start | yes |
| 964 | https://exrx.net/WeightExercises/Subscapularis/CBStandingInternalRotation | internal rotation | yes |
| 993 | https://exrx.net/WeightExercises/Infraspinatus/DBLyingExternalRotation | side-lying external rotation | yes |

Only the DBOneArmLateralRaise page (line 167) is new to Sources.kt; ExRx is an allowed domain. I could not check the pages (no WebSearch); the controller should check that the two ExRx pages at lines 166-167 state a 10 to 30 degree elbow angle, and that CBStandingExternalRotation (line 925) says the forearm starts across the belly. Other numbers (stance and lengths, 42 degree Arnold elbow, 35/45 degree rotation ends, 10/12 degree bends, kit sizes) are marked "neutral" in their comments with no citation, as the brief asked.

## Code and tests
- Kit: two new pieces, Kettlebell and LateralRaiseMachine (KitShoulders.kt), each with a typical-size comment with units (16 kg bell 21 cm across; 40 cm seat about 45 cm up, 20 cm pads 9 cm thick). KitTest has 5 new cases (42 in the class): kettlebell, lateral raise machine, pec deck facing the pad, cable stack crossed/front single column, press machine end-on handle. The new options (handleEndOn, facingPad, crossed) each have a doc comment and a KitTest case.
- Laziness: `shoulderBatchFigures` is a `by lazy` map of `lazy { figure }` merged into the shoulders map; AreaMapsLazyTest passes.
- BatchShouldersTest: 24 tests, one per new figure plus the area test. Reasoned through three:
  1. Seated barbell press: bar 4..12 cm above the shoulder joint, 0..4 cm clear of the face, elbow 5..20 forward and 10..33 below, out 0..45, lockout, pins 5..15 under the bar, seat/back/feet contacts. A wrong height, wrong-way elbow or bar over the face would fail. Strong.
  2. Smith shoulder press: bar on rails both panels, head clearance 0.3..3 cm in both panels, bar level with the shoulders, stops 0..6 under it, elbow 15..33 below, out 5..60, straight arm, bar above the head at END. A bar through the head or a bent END arm fails. Strong. No knee bound (the feet contact only).
  3. Seated rear delt raise: assertRearRaise (arms hang 28..33 below the shoulder, END elbow level and 31..33 out, bend 10..30, out 0, dumbbells clear of the floor) plus lean 50..65 and thighs 75..85. It would fail a wrong arm pose but not an upright seated pose with the lean the code sets, and it does not bound how low the head sits: that is why Important 2 is not caught by the test. Weak on the one thing the figure is about.
  Also checked: Cable rear delt row (elbow 0.5..5 behind the back, out 55..85, elbow bend 100..150, bar touches the upper chest): strong; no knee bound (Minor 9). Machine reverse fly (arms forward out 50..90 at START, out -20..-5 at END): catches a wrong-way elbow.
- Out-of-plane arms (`out != 0`) and whether a wrong-way elbow is caught: Seated barbell press yes (elbow forward and below, out 0..45); Smith yes (out 5..60 and elbow 15..33 below); Machine press yes (out 10..60, elbow 15..40 below the handle); One-arm DB press yes (out 30..60); Machine reverse fly yes (out ranges); Cable reverse fly yes (out 50..90 / 0..15, elbow level); Cable rear delt row yes (out 55..85 and elbow behind the back). Rotations and lateral raises have `out == 0` and a bound on forearmOut where it matters (lateral raises 10..30; external/internal rotation 25..60).
- No model names in code, tests, the generated bridge or the commit body other than the two attribution lines (grep over the diff, 0 hits). The commit ends with "Claude Opus 5.5" and the session line, as in the earlier tasks (the brief allowed the harness's own lines).

## Tests run
- `./gradlew :domain:jvmTest`: 418 tests, 1 skipped, 0 failures, 0 errors (BatchShouldersTest 24, KitTest 42, FiguresTest 7, AreaMapsLazyTest 1).
- `cd web && npm test`: 39 files, 657 tests passed.
- Not run (the controller's): speed tests, domain JS, Android, e2e.
- `./gradlew --stop` done. `git status` is clean (I wrote only web/.figure-sheets/r8* and this report).

## Seen and fine
Seated barbell press (bar to the chin, pins just under it, rack beyond the feet, lockout over the shoulders); One-arm DB press; Machine shoulder press (back 80, lever pivot behind the pad, same length both panels); Landmine press (split stance, back leg on the ball of the foot, plates before the hand, arc kept); Kettlebell press (racked bell in front of the forearm; overhead bell behind it; the KB-side foot forward); Lateral raise (arms level, hands not above the elbows, T at thumbnail); Cable lateral raise (pulley on the free arm's side, cable in front); Machine lateral raise (pads outside the arms, on top at level); Front raise, Barbell front raise, Cable front raise (cable drawn from the pulley to the hand in both panels, arm 12 degrees back at START); Cable rear delt row (bar on the upper chest, elbow level with the shoulder and behind the back; the out-ness cannot show from the side, which is the cost of the camera); Side-lying external rotation; the rotations.

Sheets I rendered (all new labels): web/.figure-sheets/r8-<Name>-{dark,light}.png (24 single figures, names with underscores), r8sh-{dark,light}.png (the area, 4 columns), r8new91-{dark,light}.png (the 91 existing figures); the base render (r8base-all) and my crops/montages are in the scratchpad under .../scratchpad/ (base/web/.figure-sheets, thumbs-dark.png, thumbs-light.png, rear_starts.png, press_starts.png, crf_end.png, crf_start.png, crdr_end.png, mp.png).
