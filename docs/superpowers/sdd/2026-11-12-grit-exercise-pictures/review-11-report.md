# Review of Task 11 (27 hamstring and glute pictures), commit 2091c06

Verdict: Needs fixes. Critical 0, Important 3, Minor 8.

Checked: all 28 area figures rendered one at a time at full size in dark (web/.figure-sheets/r11_<Name>-dark.png and -light.png, names with underscores), the whole area as a sheet (r11hg, dark and light), a 2x thumbnail contact sheet of all 28 in dark and light, and 2x crops of the Good morning END, Reverse hyper START, Machine hip abduction and Hip thrust START. Read every guide in areas/HamstringsGlutes.kt against every pose and caption; read figures/areas/HamstringsGlutes.kt (comments and the key code), KitHamstrings.kt's comments, the Kit.kt, Figure.kt, FigureChecks.kt, FigureRenderer.kt and KitShoulders.kt diffs, BatchHamstringsGlutesTest in full, the KitTest additions, the commit message and the builder's report.

| Figure | Verdict | Figure | Verdict |
|---|---|---|---|
| Hip thrust | OK (Minor 7) | Glute bridge | OK |
| RDL (existing) | OK, unchanged | Single-leg glute bridge | OK |
| Stiff-leg deadlift | OK (Minor 8) | Barbell glute bridge | OK (Minor 4) |
| DB RDL | OK | DB hip thrust | OK (Minor 4) |
| Single-leg RDL | Fix (1) | Machine hip thrust | OK (Minor 4) |
| Good morning | OK (Minor 1) | Kettlebell swing | OK |
| Lying leg curl | OK | Single-arm kettlebell swing | OK |
| Seated leg curl | OK | Reverse hyper | OK (Minor 3) |
| Standing leg curl | OK | Quadruped hip extension | Fix (2) |
| Cable leg curl | OK | Machine hip abduction | OK (Minor 3) |
| Stability ball leg curl | OK | Cable hip abduction | OK |
| Nordic curl | OK (Minor 4) | Side-lying hip abduction | OK |
| Glute-ham raise | OK | Clamshell | Fix (3) |
| Machine hip adduction | OK (Minor 3) | Lateral band walk | OK (Minor 5) |

## Important

1. Single-leg RDL stops short of what the guide says (figures/areas/HamstringsGlutes.kt:210 `singleLegRdl`; test BatchHamstringsGlutesTest `singleLegRdl_...`, "dumbbell off the floor" 5..25 cm). Render r11_Single-leg_RDL-dark.png.
   - The guide's exact words (areas/HamstringsGlutes.kt, Single-leg RDL steps): "Lower until the weight reaches the floor. Stop sooner if your back starts to round." The picture's END has the torso 3 degrees above level and the dumbbell clearly in the air (about 47 px above the floor line at full size, the builder's 14 cm). The step says floor; the picture shows a gap, and the test blesses the gap.
   - Which is right to draw: the guide's. The picture has to agree with the guide, and "stop sooner" is the guide's escape for someone who cannot reach, not the default. The rest of the pose (standing knee 18 degrees, free leg in line, both arms hanging, hips over the foot, standing leg far-side grey) reads right and the thumbnail is plainly different from the DB RDL.
   - Fix, either of: (a) draw the weight resting on or just touching the floor (reach: torso 10 to 15 degrees below level with the back leg kept in line, or the standing knee near 25 to 30 degrees; change the "dumbbell off the floor" bound to about -1..2 cm and the torso bound accordingly); or (b) the controller changes the guide's step to the conservative stop the builder drew ("Lower until your torso is about level with the floor. Stop sooner if your back starts to round.", the Good morning's wording) and the picture stays. (b) is one line and matches what most lifters can do with a flat back. Controller's call; either way picture and guide must agree.

2. Quadruped hip extension: the back slopes about 22 degrees up to the head (figures/areas/HamstringsGlutes.kt:1066 `quadrupedHipExtension`; BatchHamstringsGlutesTest `quadrupedHipExtension_...` "back tilt" 5..30). Render r11_Quadruped_hip_extension-dark.png and look at the thumbnail.
   - What I saw: hip at 120 px up, shoulder about 50 px higher over a 130 px torso (about 22 degrees). With the head up and the hips low, at thumbnail it reads as hips sagging or a lean onto the hands, which is the guide's own mistake ("Letting your lower back sag") and not the "neutral back" the setup asks for. The back is straight, but it is not level.
   - Why: the thigh is vertical with the knee on the floor (the hip is as high as it can be), and the torso is tilted until the vertical, wrist-60-degree arm reaches the floor, so the arm is longer than the thigh plus knee height. Real figures of this exercise show about 10 degrees, so this is about double.
   - Fix: get it nearer 10 degrees. Cheapest: put the hands about 10 cm ahead of the shoulders (the arm leaning forward) with a slight elbow bend, or trim the arm's reach by letting the wrist go to about 75 degrees; then tighten the test to 0..12. If the controller judges that the skeleton's proportions cannot do better than about 15 degrees without a new joint, accept it and downgrade to Minor, but the bound 5..30 should come down to the value drawn plus 3.

3. Clamshell START caption names something the panel does not show (figures/areas/HamstringsGlutes.kt:1316 `clamshell`, startCaption "Hips stacked, knees bent"). Render r11_Clamshell-dark.png.
   - What I saw: START is a side-lying figure with two short, straight, stacked legs. The front view hides the 90-degree knee bend and the 45-degree hip bend in depth, so nothing in the panel says "knees bent" (lesson 7). The legs also look shorter than the Side-lying hip abduction's, which reads as foreshortened, not as bent. END reads as a clamshell (the top thigh opens to a V, band above the knees), and the thumbnail is END, so the exercise is recognisable. Another camera would read better at START (from above or from the feet, the knees show), but the repo has only side and front, so I would keep the front view.
   - Fix: change the START caption to guide wording the panel shows: "Feet together" (guide: "Keep your feet together.", 13 characters) or "Hips stacked, feet together" is too long (27), so "Feet together" or "Hips stacked" alone. Keep END "Lift your top knee". Optional: the builder's note that the START reads as straight legs stays true; no code change needed.

## Minor

1. Good morning END (figures/areas/HamstringsGlutes.kt:246): the builder's concern, judged. At 2x (scratchpad gm_end.png) the bar is over the forefoot (about 10 cm past the mid-foot), the shin leans back about 10 degrees (the knee behind the ankle), the knees are bent 22 degrees and the hips are well behind the heel. It reads as a lifter sitting back with a level torso, the bar over the front of the foot: believable and balanced-looking, a little more knee bend than a "slight" one. The RDL and the Squat put the bar over the mid-foot; here it is not, and the rigid figure cannot do better with a level torso. Accept. Two small things: the bar test only requires the bar anywhere between heel and toe (`bar over the foot at the bottom`), so a worse drift would pass: tighten to mid-foot..toe. The head touches the rack's safety arm at END (the arm's bar meets the face at the head's right edge), a touch only; the test bounds the posts 8..40 cm clear but not the safety arm's tip.
2. Single-leg RDL START caption "Back tall, knees bent" drops the guide's "slightly" ("back tall and knees slightly bent"); a lifter reads "knees bent" as a squat. Use "Back tall, knees soft" or "Stand tall, knees soft" (not guide wording) or "Knees slightly bent" (19 characters, guide wording and shown: 10 degrees).
3. Hands and handles at the hip machines and the reverse hyper (Machine hip abduction, Machine hip adduction, Reverse hyper): the guides say to grip the handles, and the builder's comments say handles are drawn, but at full size the hands hang beside the knee pads (Machine hip abduction r11 crop mha.png) and the Reverse hyper's hand ends beside a faint grey strip behind the forearm, not visibly on a handle. Readable, not wrong. The machines' tests do not bound the hand-to-handle gap (the Reverse hyper test only has a HANDS-on-HANDLE contact).
4. Wrong-way elbow lesson, partial: the thrusts and the Barbell glute bridge use `handsOn` (an `arm3` with the elbow out to the side). Their tests bound the hands (together on the bar, 5..12 cm from the pad) but nothing in them would fail if the elbow bent the wrong way or into the bench or floor. The Nordic curl's START elbow (20 degrees, out 55) is not bounded either (only END's 0..30). The Stability ball curl's arm has elbow 0, so it is safe. The renders show all of these fine; the gap is in the tests. Suggested: assert the elbow's depth sign (or the forearm's out) once in `handsOn`'s users, and bound the Nordic START elbow.
5. Lateral band walk: the travel (legs 3 degrees to 12 degrees, feet about 28 cm wider) is visible at full size as narrow stance to wider A, but at thumbnail (the thumbnail is END) it is a stance with a band, and the step is not plainly a step. Accept: a still of a sideways step cannot do more. A wider END (legs 15 to 18 degrees) would help if the controller wants it.
6. Citations not in Sources.kt (the controller checks them): PMC3418110 (clamshell's hips 45 degrees and knees 90 degrees) and the ACE "Play Ball!" page (the 65 cm ball). See the table.
7. Hip thrust START (and DB and Machine hip thrust): torso about 55 degrees up from level, head high with the neck extended. That is a faithful "sit on the floor with your upper back on the bench" start, so accept. The head's height makes the thumbnail's START tall, but the thumbnail is END.
8. Captions that do not name what the picture shows (lesson 7, low value): Stiff-leg deadlift START "Bend knees slightly" over an upright figure whose 8-degree knee bend is not visible; Glute bridge END "Squeeze your glutes" (the lifted hips show, the squeeze does not). Both are guide wording and acceptable; "Stand tall with the bar" and "Hips up, body in a line" would show more.

Builder's other concerns, judged:
- Seated hip machines drawing the seat over the far thigh's stub (builder 5): I saw a small overlap in Machine hip adduction START (the seat edge crosses the near thigh stub); it reads as sitting on the seat. Accept.
- Neutral angles that a lifter might call wrong: none wrong. The 22-degree Good morning knees are the high side of "slightly" (Minor 1). Curl tops 115 (lying, standing), 100 (seated), 120 (cable), the Nordic's 30, the 30-degree abduction (machine, cable, side-lying), the clamshell's 40, the bridge shins 8 degrees back, the thigh 4 degrees down at the thrust top, the Reverse hyper's 6 degrees and the swing's 22-degree knees all look right in the renders.
- Reverse hyper has no pendulum (builder 8): fine for the bodyweight bench the guide names.

## Checks the brief asked for, in this batch
- Hinges: flat back and hips back in all five (Stiff-leg deadlift, DB RDL, Single-leg RDL, Good morning, and the Kettlebell swings' bottom). Stiff-leg deadlift differs from the RDL by straighter knees (8 against 18 degrees) and a lower torso (the test asks at least 4 degrees lower; at thumbnail the RDL has its rack and the SLDL does not, and the SLDL's torso and legs are visibly straighter). DB RDL is end-on discs, no bar, no rack. Rack with safety arms only on Good morning (the guide's RACK_SAFETY line); RDL's existing rack unchanged.
- Leg curls: Lying (pad on the calf above the heels, thighs and hips on the bench, handles past the head), Seated (back on the pad, lap pad above the knees, roller under the lower legs), Standing (thigh pad on the working thigh, roller behind the lower leg, bent forward, far leg standing), Cable (cuff on the ankle, cable to it in both panels, arms straight to the stack, foot just off the floor, far leg stepped back): all have the pad on the lower leg and thighs or hips supported. Stability ball curl (ball under the shins at START, soles at END, shoulders and head on the floor) and the Nordic and glute-ham raise (knees on a pad, ankles held, body in one line, the GHR level and no further) read right and differ plainly from each other (floor and fingertips against a machine).
- Bridges and thrusts: at the top, shoulders, hips and knees are in line (test `lineBend` at most 1 degree for bridges, at most 6 for thrusts; about 4 drawn), shins near vertical (bridges 8 degrees back, thrusts 0). Single-leg bridge's raised leg is in line with the hips and differs from the two-leg bridge at thumbnail; Hip thrust, DB hip thrust and Machine hip thrust differ by bar plates, small discs, and a lever and platform; the Barbell glute bridge is on the floor, not on a bench.
- Six front-view figures: Machine hip abduction (legs together then 30 degrees out, pads outside) and Machine hip adduction (legs 30 degrees out then together, pads inside meeting), Cable hip abduction (leg crossed in then 30 degrees out, cable to the cuff, hand on the frame), Side-lying hip abduction (top leg resting then lifted 30 degrees, plain), Clamshell (END plain, START see Important 3), Lateral band walk (Minor 5). The two machines' thumbnails differ plainly (legs apart against together). The side-lying lifter shown from the front of the body shows the top leg rising.
- Camera: the six abduction/adduction figures are front, the other 22 side (asserted by `hamstringsGlutes_everyExerciseHasAFigure_thatPassesItsChecks`), as the brief says.
- Captions: all 24 characters or fewer (FigureChecks passes for all 28), from the guides' wording, each fits except Important 3 and Minor 2 and 8.
- Pins and spotter: only the Good morning (and the existing RDL) draw a rack; no other figure does. OK.

## Counts and catalogue
- 28 BodyArea.HAMSTRINGS_GLUTES entries; the sheet command `figures:sheet -- "hamstrings & glutes"` lists 28 figures; `hamstringGluteFigures` = RDL plus a 27-entry map, every one `lazy { }` (a `Map<String, Lazy<ExerciseFigure>>` built `by lazy`); Figures.all is 199 (172 + 27); the new test asserts the 28 by canonical name and nothing else got a figure. RDL's figure is unchanged (it is in the pixel comparison below).

## Existing figures (172)
All 172 are pixel-identical to the base. How: `git archive b0a8382 web | tar -x -C <scratchpad>/base11` (no worktree, no checkout, node_modules linked), rendered `all` there (172 figures, label r11base, dark and light, 6 columns), then the same 172 names in the same order from the current tree (label r11new172). Full sheets compared with Pillow, dark and light: both 2836 x 7780, the only differing pixels are in the label line (bounding box about x 46..224, y 31..45). The thumbnails are in the sheets. I also rendered `all` from the current tree (label r11all): 199 figures. The golden is 199:8d220156333cb4a8 and FiguresTest passes against it. The Kit.kt, Figure.kt, FigureRenderer.kt (an AnkleStrap counts as the cable's end), FigureChecks.kt (AnkleStrap radius 0) and KitShoulders.kt (the new KettlebellHold.SWING; RACK and OVERHEAD unchanged) changes leave the old drawing.

## Sources (citations in the new code)
All in figures/areas/HamstringsGlutes.kt except the last. "In Sources.kt" is an exact string match with the URL as written (trailing slash included) in library/Sources.kt. Numbers marked neutral carry no citation. I could not check the pages (no WebSearch). The RDL's own header (lines 20 to 25) is existing, approved code.

| Line | URL | Claim | In Sources.kt |
|---|---|---|---|
| 176 | https://www.acefitness.org/continuing-education/prosource/january-2016/5767/ace-technique-series-romanian-deadlift/ | DB RDL: dumbbells close to the legs, about at the knees | yes |
| 241 | https://exrx.net/WeightExercises/GluteusMaximus/BBBentKneeGoodMorning | Good morning: torso down to level (parallel) | yes |
| 290 | https://exrx.net/WeightExercises/Kettlebell/KBTwoArmSwing | swing bottom: "forearms make contact with inner thighs permitting kettlebell to swing back under hips" | yes |
| 389 | https://exrx.net/WeightExercises/Hamstrings/LVLyingLegCurl | lying curl: "raise lever pad to back of thighs by flexing knees" | yes |
| 439 | https://exrx.net/WeightExercises/Hamstrings/LVSeatedLegCurl | seated curl: "pull the far padded lever toward the back of the thighs by flexing the knees" | yes |
| 494 | https://exrx.net/WeightExercises/Hamstrings/LVStandingLegCurl | standing curl set-up (angles neutral) | yes |
| 541 | https://exrx.net/WeightExercises/Hamstrings/CBStandingLegCurl | cable curl set-up (angles neutral) | yes |
| 577 | https://www.acefitness.org/resources/everyone/exercise-library/59/stability-ball-hamstring-curl/ | ball curl: torso in line with the thighs, heels in | yes |
| 663 | https://exrx.net/WeightExercises/Hamstrings/ASHamstringRaiseSelfFloor | Nordic: fingertips touch down when the body starts to fall | yes |
| 712 | https://exrx.net/WeightExercises/Hamstrings/BWHamstringRaise | glute-ham raise: level, no further | yes |
| 794 | https://www.acefitness.org/resources/everyone/exercise-library/49/glute-bridge/ | bridge top: shoulders, hips, knees in line | yes |
| 815 | https://www.acefitness.org/resources/everyone/exercise-library/145/glute-bridge-single-leg-progression/ | single-leg bridge: straight leg in line with the hips | yes |
| 859 | https://www.acefitness.org/resources/everyone/exercise-library/318/hip-bridge/ | barbell bridge | yes |
| 937 | https://exrx.net/WeightExercises/GluteusMaximus/BBHipThrust | hip thrust: body straight at the top | yes |
| 964 | https://www.acefitness.org/resources/everyone/exercise-library/367/elevated-glute-bridge/ | DB hip thrust | yes |
| 989 | https://exrx.net/WeightExercises/GluteusMaximus/LVHipThrustPL | machine hip thrust | yes |
| 1029 | https://exrx.net/WeightExercises/GluteusMaximus/BWReverseHyperextension | reverse hyper: legs straight, hips extended as high as possible | yes |
| 1062 | https://www.acefitness.org/resources/everyone/exercise-library/270/quadruped-bent-knee-hip-extensions/ | quadruped: move only at the hip | yes |
| 1130 | https://exrx.net/WeightExercises/HipAbductor/LVSeatedHipAbduction | machine abduction (30 degrees out neutral) | yes |
| 1153 | https://exrx.net/WeightExercises/HipAdductors/LVSeatedHipAdduction | machine adduction | yes |
| 1194 | https://www.acefitness.org/resources/pros/expert-articles/9158/ankles-knees-and-hips-10-joint-friendly-exercises-for-the-lower-extremity/ | band walk (angles neutral) | yes |
| 1219 | https://www.acefitness.org/resources/everyone/exercise-library/38/side-lying-hip-abduction/ | cable abduction: stop when the hips tilt, about 45 degrees at most | yes |
| 1221 | https://exrx.net/WeightExercises/HipAbductor/CBHipAbduction | cable hip abduction | yes |
| 1286 | https://www.acefitness.org/resources/everyone/exercise-library/38/side-lying-hip-abduction/ | side-lying abduction: stop when the hips tilt, about 45 degrees at most | yes |
| 1310 | https://pmc.ncbi.nlm.nih.gov/articles/PMC3418110/ | clamshell set-up: "hips at 45° and knees at 90°" | NO |
| KitHamstrings.kt:30 | https://www.acefitness.org/certifiednewsarticle/3235/play-ball/ | stability ball 65 cm for a 5'8" to 6'3" lifter | NO |

The controller should check that PMC3418110 says hips at 45 degrees and knees at 90 degrees for the clam's set-up, and that the ACE Play Ball page gives 65 cm for that height range. Kit.kt:15 (an IPF rulebook URL) is old code, not part of this diff.

## BatchHamstringsGlutesTest: 28 tests for 28 figures (RDL included in the area test)
Three tests reasoned through, would they fail if the pose were visibly wrong?
1. Seated leg curl: knees 0..5 and 95..105, contacts hips/back/thighs/shins, lever equal in both panels, pivot on the knee, legs out level (ankle to knee 0..5 cm), the shin leaning 5..20 degrees at END, lap pad 8..20 cm above and 4..14 cm behind the knee. A pad on the wrong side of the leg, a bent START or a lap pad on the shin would fail. Strong.
2. Glute-ham raise: knees 90 and 0, torso 90 and level, hips straight 0..0.01, shins level, the knees fixed on the pad, contacts to pad, plate and brace, the bench laid level. A body that sags below level or bends at the hips would fail. Strong.
3. Quadruped hip extension: hands under shoulders, knees under hips, knee kept 90, the thigh in line with the back, the other leg unchanged. A cocked knee or an arched thigh would fail; the back tilt bound 5..30 would not catch a 22-degree slope (Important 2). Medium.
Also reasoned: Single-leg RDL bounds the free leg, the arms and the hips, but its weight-height bound 5..25 cm is what lets the guide mismatch pass (Important 1); the Machine hip abduction and adduction bound the pads' offset and the true abduction angles, so a leg that does not move fails; the Clamshell bounds hips flexion 40..47, feet together, the knee rising 12..25 cm, so a leg that does not open fails.
Lessons the brief asked for: legs bounded in every figure (knees per panel); captions name something the panel shows, except Important 3 and Minor 2 and 8; every new kit piece has a typical-size comment with units (MiniBand, StabilityBall, AnkleStrap, NordicStation, ReverseHyperBench, LyingLegCurlMachine, SeatedLegCurlMachine, StandingLegCurlMachine, HipThrustMachine, HipMachine) and a KitTest case (9 new; KitTest is 62); the out-of-plane arms are only partly covered (Minor 4). No model names in code, tests or comments; the commit message carries only the two attribution lines (Opus 5.5 and the session line, as in earlier tasks; the harness's lines).
Laziness: the area map is a `Map<String, Lazy<ExerciseFigure>>` built `by lazy`, each entry `lazy { }`; AreaMapsLazyTest passes.

## Tests run
- `./gradlew :domain:jvmTest --offline`: all green. Whole-module count from the result XMLs: 510 tests, 1 skipped, 0 failures, 0 errors (BatchHamstringsGlutesTest 28, KitTest 62, FiguresTest 7 with 1 skipped, AreaMapsLazyTest 1, BatchQuadsTest 23, BatchBackTest 31, BatchChestTest 33, BatchArmsTest 21, BatchMainLiftsTest 20, BatchOneTest 11, BatchShouldersTest 24).
- `cd web && npm test`: 39 files, 657 tests passed.
- Not run (the controller's): speed tests, domain JS, Android, e2e.
- `./gradlew --stop` done. `git status` is clean (I wrote only web/.figure-sheets/r11* and this report; the base copy, crops and thumbnail sheets are in the scratchpad).
