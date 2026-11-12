# Review of Task 10 (27 quad pictures), commit 44bdb8d

Verdict: Needs fixes. Critical 0, Important 3, Minor 10.

Checked: all 28 quad figures rendered one at a time at full size in dark and light (web/.figure-sheets/r10-<Name>-dark.png and -light.png, names with underscores), the whole area as a sheet (r10quads, dark and light), a thumbnail contact sheet of 20 figures at 3x, and 3-4x crops of the Lunge back leg, Curtsy lunge END, Single-leg leg press START, Zercher END, Hack squat END and Smith squat START. Read every guide in areas/Quads.kt against every pose and caption; read figures/areas/Quads.kt, KitQuads.kt, the Kit.kt/Figure.kt/FigureChecks.kt/MainLifts.kt diffs, BatchQuadsTest, the KitTest additions, the commit message and the builder's report.

| Figure | Verdict | Figure | Verdict |
|---|---|---|---|
| Bodyweight squat | OK (Minor) | Single-leg leg press | OK (Minor) |
| Goblet squat | OK | Hack squat | OK (Minor) |
| DB squat | OK | Lever squat | OK (Minor) |
| Kettlebell front squat | OK | Leg extension | OK |
| Zercher squat | OK | Single-leg leg extension | OK (Minor) |
| Single-leg box squat | OK | Sled push | OK (Minor) |
| Jump squat | Fix | Lunge | Fix (shared, 1) |
| Smith squat | OK (Minor) | Reverse lunge | Fix (shared, 1) |
| Smith front squat | OK | Walking lunge | Fix (shared, 1) |
| Smith split squat | Fix (1, 2) | Barbell lunge | Fix (shared, 1) |
| Split squat | Fix (1, 2) | Barbell reverse lunge | Fix (shared, 1) |
| Bulgarian split squat | OK | Lateral lunge | OK |
| Leg press (existing) | OK, unchanged | Curtsy lunge | OK (Minor) |
| Step-up | OK | Lateral step-up | OK |

## Important

1. The back leg of every split stance reads as kneeling at app size (Lunge, Reverse lunge, Walking lunge, Barbell lunge, Barbell reverse lunge, Split squat, Smith split squat). This is a judgement call, so the controller decides. figures/areas/Quads.kt:751 `splitBottom`, `backLegOnBall` (708).
   - What I saw, at full size (r10-Lunge-dark.png, 4x crop scratchpad lunge_back.png): the back thigh comes down to a knee about 7 cm off the floor, the shin then runs back almost level (a few degrees), and the foot turns down at about 45 degrees with its toes on the floor. A lifter reads a deep, low lunge on the toes, but only just: the whole lower leg lies along the floor. At thumbnail (56 x 40) the 7 cm gap is about 2 px, the leg lies on the floor and every one of these figures reads as kneeling or half-kneeling. The guide's key cue ("back knee just above the floor, don't let your knee touch the floor") is not visible at thumbnail. Bulgarian split squat is not affected (the foot is up on the bench, the shin slopes, it reads right).
   - Why it happens: the knee height is fixed by the level front thigh (hip about 50 cm up), so the knee cannot go higher; raising the back knee would not help and would make the shin slope the wrong way. What is missing is the heel height: with no toe joint, the ankle only rises to about 18 cm (toes on the floor, ankle at 45 degrees plantar), so the shin slopes about 10 degrees where a real one slopes 20 to 30.
   - Cheapest honest fix: let the back foot's drawn angle go to about 70 degrees from the shin (50 degrees at the ankle plus about 20 degrees at the toes, the ball-of-foot bend the figure lacks) for this pose only, with the ankle limit check relaxed for it in a comment. The ankle then sits about 24 cm up, the shin slopes about 18 degrees and the toes stay on the floor. Not cheap but honest: a small toe joint on the foot capsule. Not recommended: a higher back knee (cannot, see above), or a shorter back thigh angle (the limit is 20 degrees, it gains under 1 cm).
   - If the controller prefers to accept it: change nothing in the code, keep the captions ("Front thigh about level", "Back knee near the floor") and note it as a known limit. No test would catch a kneeling pose either way: `assertSplitBottom` (BatchQuadsTest.kt:341) bounds the knee gap (2..10 cm) and the heel (5..30 cm) but not the shin's slope.

2. Split squat and Smith split squat START is a half-lunge, not standing (figures/areas/Quads.kt:930-974 `splitTop`). Render r10-Split_squat-dark.png and r10-Smith_split_squat-dark.png.
   - What I saw: START is only about 23 cm above END (hips 19 cm higher by eye at 1.6 px per cm), the front knee is still bent about 56 degrees and the back knee is bent too, the back shin is level. Both panels show a lunge. The guide's step is "Stand back up by straightening your front leg", and the picture does not show it. The test blesses it (BatchQuadsTest.kt `splitSquats_hipsStraightDown...`: "hips rise 15..40", "front knee at the top 30..80").
   - Is standing taller physically right for this stance? No, the figure is right not to stand: the feet are about 110 cm apart (front ankle to back toes, test bound 80..125), which is about the straight-leg reach. With the hips low at the bottom, a 20 degree hip-extension limit and the back foot fixed, the hips cannot rise further. A real split squat stance (about 75 to 90 cm ankle to ankle) does let the hips rise to nearly standing. The cost is nothing at the bottom: the front shin and the back thigh stay in their bounds.
   - Fix: shorten SPLIT_FRONT (Quads.kt:797, 40.0) so the ankle-to-ankle gap is about 85 to 90 cm (the Lunge and Barbell lunge use the same value; they need a long step, so give the split squats their own value, or leave the lunge as is). Then raise the START bound: front knee at the top 5..25, hips rise 35..50. The Smith split squat then has a bar that travels farther on the rails, which also helps.

3. Jump squat END reads as standing (figures/areas/Quads.kt:296-315; thumbnail is END). Render r10-Jump_squat-dark.png.
   - What I saw: a straight, upright figure with arms hanging, feet pointed, 20 cm off the floor line. The gap is about 32 px at full size and 4 px at thumbnail, and nothing else says "air". At thumbnail it is a plain standing figure with no floor contact, which a lifter reads as "Stand" (the thumbnail of the Jump squat in the Exercises list is just a standing person).
   - Fix, in order of cost: (a) a bigger gap, 35 to 40 cm (a real jump squat clears 30 to 50 cm; bounds 10..30 in BatchQuadsTest.kt become 30..50, FeetOffFloor min 25); (b) swing the arms up and forward (about 60 to 90 degrees; the guide's arms-at-sides is the setup, not the jump); (c) a slight tuck is not right for this exercise, so skip it. (a) plus (b) should read as a jump at thumbnail. A small ground line or shadow under the feet would also help but is new drawing code.

## Minor

1. Curtsy lunge (builder concern 3), judged. At full size the crossing is readable (r10-Curtsy_lunge-dark.png, crop curtsy_end.png): the back leg comes from the far hip, the knee is low and the shin crosses behind the front shin to a foot on the other side. It is the same colour as the front leg, so at the hip the two merge, and at thumbnail the legs are one clump that reads as a narrow kneeling stance. Changing the far leg to the far-limb colour in the front view would change the existing front-view figures (a renderer change; the controller should not take it for this batch). Accept. A cheap partial fix: put the back foot 5 to 10 cm further across so the crossing shows in the silhouette.

2. Sled push (builder concern 5), judged. START (torso 80 degrees, hips 72 cm, back foot on its ball) matches the guide's set-up. END at 55 degrees reads clearly as "lean into the sled" and is plausible (neutral, no source, as marked). Acceptable: 45 to 65 degrees would all read the same. The guide's own mistake ("standing up tall") is covered because the END is clearly leaning. If the controller wants it nearer ExRx's "nearly vertical", 60 degrees would still show the lean. No change needed.

3. Smith squat START stands with the whole body leaning forward a few degrees and the head forward (r10-Smith_squat-dark.png, crop smith_start.png): legs lean 4 degrees and the torso about 10 degrees off vertical, because the torso is solved to put the bar over the same rail as at the bottom. It reads as a slight stoop rather than "standing tall". No test bounds the START torso. Accept, or bound it at 80..90.

4. Single-leg leg press (builder concern: free leg). The far leg is on the floor at the front of the seat with a bent knee; at full size it is clear, and at thumbnail the grey leg under the seat tells it from the Leg press. One detail: the sled's lower rail ends at the far shin, so it looks like the grey leg runs into the rail (r10-Single-leg_leg_press-dark.png, crop slp_start.png). The rail is the near one, which is in front of a far leg, so it is correct. Accept.

5. Single-leg leg extension: the free leg's difference is a grey leg under the seat at END and the same pose as the two-leg figure at START (alternating machine, as ExRx). At thumbnail it is a small grey stub, but visible next to the Leg extension. Accept.

6. Hack squat: the head pokes past the top of the back pad and the arms rest along the body (hands not on handles; builder concern 6). Both read fine (crop hack_end.png). Accept.

7. Lever squat: at START the lever comes at head height from behind and the yoke is at the shoulders. Reads as a pendulum machine. Accept (builder concern 7).

8. Bodyweight squat START caption "Lift your chest" shows only a standing figure; nothing in the panel shows a lifted chest (lesson 7). The guide wording is right; "Arms down, chest up" is not guide wording. Accept, or use "Feet a little wider than hips" if the stance shows (it does not from the side). Low value.

9. Near-identical thumbnails, inherent to the side camera and the shared split stance: Lunge, Reverse lunge, Split squat and Walking lunge all show the same lunge bottom (the Walking lunge's legs are swapped in colour only). The guide's difference (step forward, step back, stay put, step through) is in the START panel and the full-size pair, not in the thumbnail. The Walking lunge's two panels (bottom, then the next bottom with legs swapped and moved on) do show travel at full size. Accept.

10. Citations (see the table): three are not in Sources.kt (ACE 5875, ACE 5869, PMC10437032). Sources.kt has a different PMC page for the Bulgarian split squat (PMC12819420) than the one the figure cites for the 40 cm bench (PMC10437032). The controller should check that PMC10437032 states a 40 cm rear-foot height.

Builder concerns not listed above: Barbell lunge safety arms are symbolic (builder 8): the guide's RACK_SAFETY line asks for them; accept. Bulgarian START back foot pivots on the pad (builder 9): reads fine.

## Counts and catalogue
- 28 BodyArea.QUADS entries in areas/Quads.kt; `quadFigures` has the same 28 canonical names (Leg press existing, 27 new, Bulgarian split squat among them); Figures.all is 172 (145 + 27); nothing else got a figure. `quads_everyExerciseHasAFigure_thatPassesItsChecks` asserts the 28, every figure passing FigureChecks, and the camera (front: Lateral lunge, Curtsy lunge, Lateral step-up; side for the rest). The camera choices match each guide: the three front-view figures move across the camera and the others bend in the sagittal plane.
- Captions: all 24 characters or fewer (the longest at 24: "Shoulders under the pads", "Other foot off the floor", "One foot on the platform", "Lower legs under the pad", "Back knee near the floor", "Bar on back of shoulders"), from the guides' wording; each fits its pose except the weak fit in Minor 8.
- Pins and spotters: Zercher squat, Barbell lunge, Barbell reverse lunge have the rack with pins under the bar (the guides' RACK_SAFETY); Smith squat, Smith front squat and Smith split squat have stops under the bar (the guides' SMITH_SAFETY). No other quad figure draws one. OK.
- Legs: knees bend the right way in every figure (front-view legs bounded by their out/shinOut angles: Lateral lunge near thigh out 30..60, Curtsy near thigh out 55..85 and far thigh/shin behind, Lateral step-up near knee out 30..75). Feet flat where they should be (assertSquatBottom, feetFlat in every squat, the Step-up and the front feet of the split stances). No leg passes through kit in any render; two touches only: the Zercher END safety arm's tip touches the knee (Zercher_squat crop), and the Single-leg leg press's far leg meets the near rail's end (Minor 4). Free legs of the single-leg figures are clearly apart at full size: Single-leg box squat (free leg out and down, thumbnail clear), Single-leg leg press, Single-leg leg extension (Minor 5).
- Lessons applied: legs bounded in every test (knees in both panels); captions from the chosen camera; every new kit piece has a typical-size comment with units (HackSquatMachine, LeverSquatMachine, LegExtensionMachine, PushSled; BarHold.CROOK is a hold, with its offset in `crookOf`) and a KitTest case (6 new: hack, lever, leg extension, sled, crook, FeetOffFloor); no new figure has an out-of-plane arm (arms hang or point in plane); out-of-plane legs are bounded as above. Commit message: no model names except the two attribution lines (Opus 5.5 and the session line, as in earlier tasks); no model names in code, tests or comments.
- Laziness: `quadFigures` is a `by lazy` map of `lazy {}` entries; AreaMapsLazyTest passes.

## BatchQuadsTest: 23 tests for 28 figures
Three tests reasoned through, would they fail if the pose were visibly wrong?
1. Hack squat (own test): torso 134..136 along the rail, shoulder pad 7..14 cm from the shoulder, plate fixed, the foot's line within 1 degree of the plate, knees 0..8 and 120..132, hips travel along the rail. A wrong depth, a floating body or a tilted foot would fail. Strong.
2. Lunges (shared): front shin 0..15, front thigh -3..8 below level, back knee 2..10 cm off, back hip -20..-10, back heel 5..30 up, step length 80..125. This catches a crouch, a kneel with the knee on the floor, a stand-up and a back-leaning shin. It does not catch the shin's slope (Important 1) and it lets a front-knee 30..80 START through the split squats (Important 2). Medium.
3. Jump squat (own test): START knees 85..95, feet flat, hips back; END legs straight, feet 10..30 cm up, toes pointed, no floor contact, FeetOffFloor fails on a grounded foot, arms hanging. A pose that lands on the floor fails; a pose that looks like standing but is 10 cm up passes (Important 3). Medium.
Other tests I read: Curtsy lunge bounds the crossing's numbers, so a back leg that does not cross fails; Zercher and Smith squat bound the bar over the mid-foot, the rails and the stops; Leg extensions bound pad, seat, pivot, roller and the free leg. Gap: the Smith squat START torso is not bounded (Minor 3). The Walking lunge test asserts the split bottom on both panels and the travel.

## Existing figures (145)
All 145 are pixel-identical to the base. How: I extracted the base commit's web folder with `git archive cf1f9e5 web | tar -x -C <scratchpad>/base10` (no worktree, no checkout, node_modules linked), rendered `all` there (145 figures, label r10base-all, dark and light, 6 columns), then rendered the same 145 names in the same order from the current tree (label r10new145). Full sheets compared with Pillow, dark and light, thumbnails included: both 2836 x 6716, the only differing pixels are in the label line (bounding box about x 49..233, y 31..45). That covers the arms, shoulders, back, chest, the main lifts, the ten Task 4 figures and Leg press (which was refactored to a shared rig: drawing unchanged). The golden is 172:e9c3e46c6c2195c7 and FiguresTest passes against it. The Kit.kt changes (new surfaces, solids, draw, drawFront branches and BarHold.CROOK) and the MainLifts.kt visibility changes (private to internal only) leave the old drawing.

## Sources (citations in the new quads code)
All in figures/areas/Quads.kt (KitQuads.kt, Kit.kt, Figure.kt, FigureChecks.kt and MainLifts.kt gained no URLs). "In Sources.kt" is an exact string match in library/Sources.kt. Numbers marked neutral carry no citation. I could not check the pages (no WebSearch).

| Line | URL | Claim | In Sources.kt |
|---|---|---|---|
| 85 | https://www.acefitness.org/resources/everyone/exercise-library/362/goblet-squat/ | goblet squat: the weight in front helps you stay upright | yes |
| 101 | https://www.acefitness.org/resources/pros/expert-articles/5875/5-variations-of-the-body-weight-squat/ | bodyweight squat: arms forward to offset the hips going back | NO |
| 157 | https://exrx.net/WeightExercises/Quadriceps/DBSquat | DB squat: the dumbbells hang over the feet | yes |
| 180 | https://exrx.net/WeightExercises/Kettlebell/KBFrontSquat | kettlebell front squat | yes |
| 215 | https://exrx.net/WeightExercises/GluteusMaximus/BBZercherSquat | Zercher squat | yes |
| 216 | https://www.nsca.com/education/articles/kinetic-select/zercher-squat-field-alternative-for-front-squat/ | Zercher: an upright torso | yes |
| 255 | https://exrx.net/WeightExercises/Quadriceps/BWSingleLegBoxSquat | single-leg box squat: other leg forward, arms forward | yes |
| 290 | https://www.acefitness.org/resources/pros/expert-articles/5869/explosive-plyometric-workout/ | jump squat: knees bent to 90 degrees | NO |
| 330 | https://exrx.net/WeightExercises/Quadriceps/SMSquat | Smith squat | yes |
| 367 | https://exrx.net/WeightExercises/Quadriceps/SMFrontSquat | Smith front squat | yes |
| 399 | https://exrx.net/WeightExercises/Quadriceps/SLHackSquat | hack squat: "just short of complete flexion" | yes |
| 448 | https://exrx.net/WeightExercises/Quadriceps/LVSquat | lever squat: feet beneath the shoulders, thighs just past parallel | yes |
| 541 | https://exrx.net/WeightExercises/Quadriceps/LVLegExtension | leg extension: 90 degrees to nearly straight | yes |
| 564 | https://exrx.net/WeightExercises/Quadriceps/LVAlternatingLegExtension | single-leg leg extension | yes |
| 626 | https://exrx.net/WeightExercises/Quadriceps/SL45LegPress | leg press (the single-leg leg press's working leg) | yes |
| 664 | https://exrx.net/WeightExercises/Quadriceps/SLSingleLeg45LegPress | single-leg leg press | yes |
| 802 | https://www.acefitness.org/resources/everyone/exercise-library/94/forward-lunge/ | lunge: front thigh level | yes |
| 803 | https://exrx.net/WeightExercises/Quadriceps/DBLunge | lunge: back knee just above the floor | yes |
| 827 | https://exrx.net/WeightExercises/Quadriceps/DBWalkingLunge | walking lunge | yes |
| 848 | https://www.acefitness.org/resources/everyone/exercise-library/319/reverse-lunge/ | reverse lunge | yes |
| 849 | https://exrx.net/WeightExercises/Quadriceps/DBRearLunge | reverse lunge | yes |
| 891 | https://exrx.net/WeightExercises/Quadriceps/BBLunge | barbell lunge | yes |
| 928 | https://exrx.net/WeightExercises/Quadriceps/DBSplitSquat | split squat | yes |
| 952 | https://exrx.net/WeightExercises/Quadriceps/SMSplitSquat | Smith split squat | yes |
| 979 | https://pmc.ncbi.nlm.nih.gov/articles/PMC10437032/ | Bulgarian split squat: rear foot elevated to 40 cm | NO (Sources.kt has PMC12819420 for this exercise) |
| 1032 | https://exrx.net/WeightExercises/Power/WTPushSprint | sled push: hips low, one foot back, torso nearly vertical, arms straight, heels off the ground at the start | yes |
| 1084 | https://exrx.net/WeightExercises/Quadriceps/DBStepUp | step-up | yes |
| 1190 | https://www.acefitness.org/resources/everyone/exercise-library/364/lateral-lunge/ | lateral lunge | yes |
| 1191 | https://exrx.net/WeightExercises/Quadriceps/DBSideLunge | lateral lunge | yes |
| 1231 | https://www.acefitness.org/resources/pros/expert-articles/6355/8-butt-toning-moves/ | curtsy lunge | yes |
| 1285 | https://exrx.net/WeightExercises/GluteusMaximus/DBLateralStepUp | lateral step-up | yes |

The controller should check that ACE 5875 says the arms go forward in a body-weight squat, that ACE 5869 says to squat until the knees bend to 90 degrees, that PMC10437032 gives a 40 cm rear-foot height, and that the ExRx WTPushSprint page says what the sled push comment claims. The sled push's 55 degree lean is marked neutral, with no citation, as asked.

## Tests run
- `./gradlew :domain:jvmTest` (once the figures package, once the whole module): all green. Whole-module count from the result XMLs: 473 tests, 1 skipped, 0 failures, 0 errors (BatchQuadsTest 23, KitTest 53, FiguresTest 7 with 1 skipped, AreaMapsLazyTest 1, BatchArmsTest 21, BatchBackTest 31, BatchChestTest 33, BatchMainLiftsTest 20, BatchOneTest 11, BatchShouldersTest 24).
- `cd web && npm test`: 39 files, 657 tests passed.
- Not run (the controller's): speed tests, domain JS, Android, e2e.
- `./gradlew --stop` done. `git status` is clean (I wrote only web/.figure-sheets/r10* and this report; the base copy and crops are in the scratchpad).

## Seen and fine
Goblet squat (dumbbell upright against the chest, elbows down, differs from DB squat's hanging dumbbells and from the bodyweight squat's forward arms), Kettlebell front squat (racked bells), Zercher squat (bar in the elbow crooks in both panels, rack with pins, forearms rising), Single-leg box squat (free leg out in front off the floor in both panels, buttocks on the box, plainly different from the Box squat at thumbnail), Smith front squat, Hack squat (back on the pad, feet flat on the plate, knees just short of fully bent), Lever squat (yoke over the mid-foot, same length lever), Leg extension (back on the pad, roller on the lower shin, 90 degrees to straight), Bulgarian split squat (rear foot on the 40 cm bench, back knee near the floor, front thigh about level), Step-up (whole foot on the box, then standing tall on it), Lateral lunge (front view, near shin upright, far leg straight, weights down by the near foot), Lateral step-up (front view, box beside, free leg off the floor beside it), Sled push (START and END, arms straight to the post, both heels up at END).
