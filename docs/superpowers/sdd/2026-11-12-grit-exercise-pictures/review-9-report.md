# Review of Task 9 (31 arm pictures), commit c010525

Verdict: Needs fixes. Critical 0, Important 2, Minor 12.

Checked: all 31 figures rendered one at a time at full size in dark and light (web/.figure-sheets/r9-<Name>-dark.png and -light.png, names with underscores), the whole area as a 4-column sheet (r9arms), 2x to 3x crops of the Concentration curl, Preacher curl, Cable overhead and DB overhead extensions and both close push-ups, and the thumbnails in every render. The guides in areas/Arms.kt read against every pose and caption. Read: figures/areas/Arms.kt, KitArms.kt, the Kit.kt, Figure.kt, FigureChecks.kt and Chest.kt diffs, BatchArmsTest, the KitTest additions, the commit message, the builder's report.

| Figure | Verdict | Figure | Verdict |
|---|---|---|---|
| JM press | OK (Minor) | Triceps pushdown | OK (Minor) |
| Curl | OK (Minor) | Rope pushdown | OK (Minor) |
| EZ-bar curl | OK | One-arm pushdown | OK (Minor) |
| DB curl | OK | Cable overhead triceps extension | OK (Minor) |
| Seated DB curl | OK (Minor, test) | DB overhead triceps extension | OK (Minor) |
| Hammer curl | OK | Skull crusher | OK |
| Cable hammer curl | OK (Minor) | DB lying triceps extension | OK (Minor) |
| Incline DB curl | OK | Machine triceps extension | OK |
| Preacher curl | OK (Minor) | Triceps kickback | OK |
| DB preacher curl | OK (Minor) | Triceps dip | OK (Minor) |
| Machine curl | OK (Minor) | Close-grip push-up | OK |
| Cable curl | OK | Triangle push-up | Fix (test only) |
| One-arm cable curl | OK | Wrist curl | OK (Minor) |
| Concentration curl | Fix | Reverse wrist curl | OK (Minor) |
| Drag curl | OK | Reverse curl | OK (Minor) |
| Spider curl | OK | | |

## Important

1. Concentration curl, the far (free) arm goes through the body and the bench (figures/areas/Arms.kt:372, `seated(down, HANG, lean.torso)`). Render: `cd web && node scripts/figure-sheet.mjs --label r9x --columns 1 "Concentration curl"`, or r9-Concentration_curl-dark.png, 2x crop at scratchpad m/conc_start.png.
   - What I saw: in both panels a grey stub sticks out under the near thigh, through the bench top, pointing down and back at the hip. That is the far arm. `HANG` is `Arm(0.0, 0.0)`, whose shoulder angle is measured from the torso's line, not from straight down. The torso here leans about 55 degrees, so the arm lies along the torso, behind the body, and runs about 10 cm past the hip into the bench. It is visible at full size, and it is the "arm through the body, floating limb" kind of defect the brief lists.
   - No test catches it: the Concentration test bounds only the near arm, the elbow's distance from the thigh, the torso lean and the knees.
   - Fix: give the far arm a real pose. Either rest the far hand on the far thigh near the knee (the guide leaves the free hand unplaced; the kickback figure builds the same thing with `armTo(bare.shoulder, handOnThigh, torso)`), or hang it straight down with the shoulder angle solved (270 minus (torso + 180), as the incline curl's `hangDown` does). Add a test bound on the far wrist (below the far shoulder, in front of or on the thigh, not below the seat).

2. Triangle push-up, the test would not catch a wrong-way elbow (BatchArmsTest.kt:452-473; figure at Arms.kt:1086-1101). This is one of the lessons the brief asked to apply (every figure with an out-of-plane arm: its own test must catch a wrong-way elbow). The Triangle's arms have out about minus 18 degrees at START and a 3D-solved bent arm at END (`pushArm` with the hint V3(-0.5, -1.0, 0.8)). The test bounds START only (hands under the chest, out -25..-10, straight arms) and the chest height at END (ChestOffFloor 8..13, through the checks list). Nothing bounds where the END elbow is. A bent arm whose elbow pointed forward (towards the head) or down would pass. The Close-grip test does bound the END elbow (`ce.near.elbow.x < ce.shoulder.x`).
   - The picture itself is fine today: in END the elbow is above the back behind the shoulder, the forearm is near vertical, the hand is under the chest (crop scratchpad m/tri_end.png).
   - Fix: add to the push-up test, for the Triangle END: elbow behind the shoulder (`te.near.elbow.x < te.shoulder.x`), elbow above the hand, hand still under the chest (as START), and the forearm's out-of-plane lean within a range.

## Minor

1. Triceps dip (builder concern 1), judged. The END panel shows the shoulder about 34 cm above the bars, the elbow behind the body and about 15 px (at 1.6 px per cm) below the shoulder, the forearm leaning back, the torso close to upright and the legs bent back, against START with straight arms and the shoulders over the hands. It reads as the bottom of a bar dip (not a chest dip: the torso stays upright), so the picture still reads as a dip to depth. The caption "Lower, stay upright" is true of what is drawn. What does not show is the guide's "Upper arms level, no deeper" cue (22 degrees short, which is the 60 degree shoulder limit with a 10 degree forward torso: with the torso at 80 degrees the shoulder extension is 58 degrees). That is on the conservative side of the guide, and the code comment says so. Accept. The test bounds the short-of-level angle at 15..30, so a pose that is too deep would fail.

2. Wrist curl and Reverse wrist curl (builder concern 2), judged. The two are pixel-for-pixel the same pair of poses, with the same end-on dumbbell, in the same order (hand down, then up), the same thumbnail (END). I looked for anything visible that could separate them:
   - Hand angle: the figure's wrist angle is signed the same way for both, and the drawn hand shows the same bend. The bend is real at full size (50 degrees each way) and small at thumbnail.
   - Palm up versus palm down: from the side, a dumbbell held end-on is a disc at the hand's centre either way. The one real difference is where the weight sits relative to the hand: on the palm side. With palm up, the barbell or dumbbell sits above the hand; palm down, below it. That is a 3 cm offset, not visible at app scale.
   - A different camera: the front view would put the forearms on the thighs pointing at the camera, foreshortened, with no hand detail in the kit to show a palm.
   - So the captions are the honest limit: "Down to your fingers" and "Curl up with your wrist" against "Knuckles point down" and "Knuckles point up". Both name what the hand shows. If the controller wants one visible cue: use a different thumbnail pair (Wrist curl thumbnail END, Reverse wrist curl thumbnail START) so the two rows differ at 56 x 40, or note it as a known limit.
   - Test: both figures run through the same `assertWristCurl`, so a pose swap would fail, but nothing in the tests (or the figures) tells the two apart. That is by design.

3. Reverse curl against Curl (builder concern 3), judged. The two are the same drawing in both panels. Nothing visible separates them from the side. Captions are the honest limit; "Lined up with shoulders" (START) is something the picture shows (hands, elbows and shoulders in a line), "Forearms upright" is the guide's wording and is true to within the 27 degree stop. The Curl, EZ-bar curl and Reverse curl thumbnails differ only by plate size (the EZ plates are 30 cm against 45 cm); the Curl and Reverse curl thumbnails are the same.

4. JM press (builder concern 5), judged against the guide. Guide steps: lift the bar out and hold it over the shoulders with straight arms; lower the bar toward the upper chest keeping the elbows high; stop when the forearms press against the upper arms; press back up until the elbows lock. Cue: "Bar between your chin and upper chest." Mistakes: elbows dropping (turns into a close-grip bench); lowering to the lower chest.
   - Drawn: START bar over the shoulder joint, arms straight. END upper arms pointing 30 degrees above level towards the feet (elbows high and forward), the forearms folded against the upper arms (elbow bend at its 148 degree limit), the bar's centre over the line from the chin to the upper chest, about 17 cm above the body's axis (12 cm above the throat), pins at 60 cm under it, rack drawn.
   - This is the JM press path (the elbows travel forward over the bar and the bar stops near the chin and throat, not the lower chest). Both guide cues hold, and the figure is not a close-grip bench (elbows high, forearms folded against the upper arms). It looks safe: the pins are under it and the bar clears the face by the bar's own height.
   - One thing to note: the end-on 45 cm plate is drawn centred over the neck with its disc covering the head, so at a glance the bar looks as though it is over the face. The centre dot (the bar) is 18 cm past the head's centre towards the feet. Accept.

5. DB lying triceps extension (builder concern 6), judged against the guide. The guide says "Bend your elbows to lower the dumbbells slowly until they are beside your head" and "Start with your arms straight and the dumbbells held up above your shoulders, not over your face". It does not fix the upper arm's angle. With fixed segment lengths the only way to put the dumbbell beside the head is to tip the upper arms back towards the head. By eye the tilt at END is about 10 to 15 degrees; the elbow stays almost over the shoulder. It reads right: elbows up, forearms folded, dumbbells at ear level, spotter's hands near the wrists. The skull crusher guide lists "upper arms drift back" as a mistake and the skull crusher figure keeps them upright; that is a different exercise with a different guide, so no conflict. One note: the test bound for the tilt is 5..40 degrees, which would also pass a 40 degree drift that looks like the skull crusher's mistake. Suggest tightening to 5..20.

6. Curl tops (builder concern 4), judged. Standing curls end with the forearm 27 degrees short of vertical, leaning forward, the bar (or dumbbell) at about chin height; the incline curl 35 degrees short. On the render the top of each curl reads as fully curled: the hand is up at the front of the shoulder and the plate or dumbbell sits at the chin. Preacher, spider, machine and concentration are at 145, 145, 130+ and 145 degrees and read the same way. Accept.

7. Preacher curl, DB preacher curl, Machine curl, Machine triceps extension: the pad is only a small dark wedge between the chest and the upper arm, with no visible stand, so the "arm pad" reads more as a shading patch than a pad (builder concern 7). The seat, the sloped arm, and for the machines the lever and the stack column make the exercise recognisable. At thumbnail the pad is not visible. Accept.

8. Heads hidden: Cable overhead triceps extension START and DB overhead triceps extension END draw the head almost completely behind the near upper arm (a sliver shows). A viewer sees "arms folded over a missing head". The position is right (arms beside the head); a short neck forward or the head drawn nearer would help, but it is a side-view limit. Accept.

9. Near-identical thumbnails, inherent to the side camera: Triceps pushdown, Rope pushdown and One-arm pushdown (the attachment is a 5 cm stick); Cable curl, Cable hammer curl and One-arm cable curl; Curl, EZ-bar curl and Reverse curl. The captions differ.

10. Test gaps (apart from the two Important ones):
   - The pushdown test (BatchArmsTest.kt:296-313) does not assert the attachment (bar, rope, handle), nor that the cable runs from the pulley to the hand in both panels (the cable-curl test does, lines 144-149). A Rope pushdown drawn with a bar would pass. Same for the Cable overhead extension.
   - The skull crusher, DB lying extension and JM press tests do not bound the knees (`assertLyingWithSpotter` has none). The feet-on-floor contact would catch a straight leg today, but the lesson was to bound legs everywhere the guide implies bent or straight legs.
   - The Seated DB curl gets only part of `assertStandingCurl`: no bound on the near elbow being straight at START, no torso-upright bound, no END bend bound (only the forearm's angle).
   - The preacher and machine pad figures' knee bound is 35..100, wide enough to pass a mid-kneel.

11. Kit comments: OverheadDumbbell (KitArms.kt, doc at the top of the object) says "A typical dumbbell, as [Dumbbell]" and gives no size with units of its own; the other three pieces (EZBar 120 cm and 30 cm plates, PreacherBench 30 cm seat and 30-35 cm pad, ArmPadMachine 35 cm seat and 150 cm column) do. The Dumbbell doc in Kit.kt says `squareToForearm` is for "a hammer curl's or a kickback's dumbbell", but the Kickback figure uses the default end-on dumbbell, not this option.

12. Citations not in Sources.kt: two (the table below). Both are ExRx pages; the controller should check them.

## Builder's concerns, judged
1. Triceps dip: Minor 1. 2. Wrist curls: Minor 2. 3. Reverse curl: Minor 3. 4. Curl tops: Minor 6. 5. JM press: Minor 4. 6. DB lying extension: Minor 5. 7. Pads small: Minor 7. 8. Speed run 1 picker failure: not re-run (the controller's job). 9. Neutral angles: each is marked "neutral" in its comment as asked.

## BatchArmsTest: 21 tests for 31 figures
The 21: the area test, then 20 group tests. 12 figures have a test of their own; 19 figures share 9 tests.
- Own test: Hammer curl, Drag curl, Incline DB curl, Spider curl, Concentration curl, Cable overhead triceps extension, DB overhead triceps extension, Skull crusher, DB lying triceps extension, JM press, Triceps kickback, Triceps dip.
- Shared:
  - Curl, EZ-bar curl, Reverse curl (one test).
  - DB curl, Seated DB curl (one test; DB curl also gets `assertStandingCurl`).
  - Cable curl, Cable hammer curl, One-arm cable curl (one test, full standing-curl bounds on each).
  - Wrist curl, Reverse wrist curl (one test, `assertWristCurl`; the two cannot be told apart by it).
  - Preacher curl, DB preacher curl (one test).
  - Machine curl, Machine triceps extension (one test, plus its own extra bounds for each).
  - Triceps pushdown, Rope pushdown, One-arm pushdown (one test).
  - Close-grip push-up, Triangle push-up (one test).
- Key bounds still covered for each shared figure, with the exceptions noted: upper arm from hanging, the elbow bends, forearm angles, knees, contacts, the pad and pivot positions are bounded per figure in the loops. Gaps: pushdown attachment and cable (Minor 10); Triangle END elbow (Important 2); Seated DB curl START elbow and torso (Minor 10); lying figures' knees (Minor 10).
- Three tests reasoned through:
  1. Skull crusher: upper arm 0..1 degrees from upright in both panels, START bend 3..8, END grip 1.5..4 cm clear of the head, over the forehead, above the face, spotter in both panels, head, upper back and feet contacts. A drifting upper arm, a bar through the head or one that stops short would fail. Strong.
  2. Hammer curl: `assertStandingCurl` on both arms (upper arm 0..10 from hanging, wrist 0, forearm 55..72 above level at END, torso exactly 90), the dumbbells palms-in in both panels, front foot 20..40 ahead, knees 0..25, and a check that the DB curl's END dumbbell is end-on. A drifting elbow or a wrong grip would fail. Strong.
  3. Pushdowns (shared): upper arm 0..6 from hanging, torso 90, START forearm 0..20 above level, END bend 5..20, column in front, pulley above head height, knees 0..1. A drifted elbow, a straight START arm or a wrong stance would fail. But a wrong attachment or a missing cable would pass (Minor 10).
- Out-of-plane arms (`out != 0`): the only new figure with a non-zero `out` is the Triangle push-up. START is bounded (out -25..-10); the END elbow is not (Important 2). Close-grip has out 0 at START and bounds its END elbow. All other new figures have out 0 and bound the elbow's position through the bends and upper-arm angles.

## Counts and catalogue
- 31 BodyArea.ARMS entries in library/areas/Arms.kt (the test asserts 31); `armFigures` has the same 31 canonical names; Figures.all is 145 (114 + 31); nothing else got a figure. The golden is checked by FiguresTest (passes).
- Captions: all 24 characters or fewer (the longest, at 24: "Palm faces your shoulder", "Just above your forehead", "Arms straight on the pad"), all from the guides' wording. Every caption names something its panel shows from the side camera, with the two that lean on the caption alone being Reverse curl START and the wrist curls (Minor 2, 3).
- Pins and spotters: JM press has the rack with pins under the bar (guide's RACK_SAFETY); Skull crusher and DB lying triceps extension have a kneeling spotter at the head (guide's OVER_FACE_SPOT). No other arms guide has a safety line of that kind and no other figure draws one. OK.
- Laziness: `armFigures` is a `by lazy` map of `lazy {}` entries; AreaMapsLazyTest passes.
- Commit message: no model names except the two attribution lines (Claude Opus 5.5 and the session line, as in the earlier tasks; the brief allows the harness's own). No model names in code, tests or comments.

## Existing figures (114)
All 114 are pixel-identical to the base. How: I extracted the base commit's web folder with `git archive e3c219f web | tar -x -C <scratchpad>/base` (no worktree, no checkout, node_modules linked), rendered `all` there (114 figures, label r9base-all, dark and light, 6 columns), then rendered the same 114 names in the same order from the current tree (label r9new114). Full sheets compared with Pillow, dark and light, thumbnails included: both 2836 x 5120, the only differing pixels are in the label line (bounding box x 43..223, y 31..45). That covers shoulders, back, chest, the main lifts, the ten Task 4 figures, DB shoulder press and the rest. The shared changes (the Chest.kt visibility changes, `Dumbbell.squareToForearm`, the new Surface.ARM_PAD, the FigureChecks bar radii, the kit wiring) all leave the old drawing.

## Sources (citations in the new arms code)
All in figures/areas/Arms.kt. KitArms.kt has no URLs; Kit.kt, Figure.kt, FigureChecks.kt and Chest.kt gained none (the Chest.kt URLs are old). "In Sources.kt" is an exact string match in library/Sources.kt. The numbers the builder marks "neutral" carry no citation. I could not check the pages (no WebSearch).

| Line | URL | Claim | In Sources.kt |
|---|---|---|---|
| 77, 96 | https://exrx.net/WeightExercises/Biceps/BBCurl | curl: elbows travel forward slightly at the top; the pose | yes |
| 97 | https://www.acefitness.org/resources/everyone/exercise-library/70/bicep-curl/ | curl | yes |
| 140 | https://exrx.net/WeightExercises/Brachioradialis/BBReverseCurl | reverse curl | yes |
| 161 | https://exrx.net/WeightExercises/Biceps/DBCurl | DB curl | yes |
| 182 | https://www.acefitness.org/resources/everyone/exercise-library/10/hammer-curl/ | hammer curl | yes |
| 183 | https://exrx.net/WeightExercises/Brachioradialis/DBHammerCurl | hammer curl | yes |
| 208 | https://exrx.net/WeightExercises/Biceps/CBCurl | cable curl | yes |
| 227 | https://exrx.net/WeightExercises/Brachioradialis/CBHammerCurl | cable hammer curl | yes |
| 247 | https://exrx.net/WeightExercises/Biceps/CBOneArmCurl | one-arm cable curl | yes |
| 269 | https://exrx.net/WeightExercises/Biceps/BBDragCurl | drag curl | yes |
| 305 | https://exrx.net/WeightExercises/Biceps/DBSeatedCurl | seated DB curl | yes |
| 364 | https://exrx.net/WeightExercises/Brachialis/DBConcentrationCurl | concentration curl | yes |
| 404 | https://exrx.net/Testing/JointROM | wrist ROM 60..80 degrees (the figure's 60 degree limit) | NO (it is cited in Body.kt:317, not in Sources.kt) |
| 414 | https://exrx.net/WeightExercises/WristFlexors/DBWristCurl | wrist curl | yes |
| 437 | https://exrx.net/WeightExercises/WristExtensors/DBReverseWristCurl | reverse wrist curl | yes |
| 460 | https://exrx.net/WeightExercises/Biceps/DBInclineCurl | incline bench at 45-60 degrees | yes |
| 521 | https://exrx.net/WeightExercises/Brachialis/BBProneInclineCurl | spider curl | yes |
| 582 | https://exrx.net/WeightExercises/Brachialis/DBStandingPreacherCurlInclineBench | pad at 45-55 degrees when no preacher bench is to hand | NO |
| 596 | https://exrx.net/WeightExercises/Brachialis/BBPreacherCurl | preacher curl | yes |
| 649 | https://exrx.net/WeightExercises/Brachialis/LVPreacherCurlH | machine curl | yes |
| 650 | https://www.acefitness.org/resources/everyone/exercise-library/184/seated-biceps-curl/ | machine curl | yes |
| 673 | https://exrx.net/WeightExercises/Triceps/LVTriExt | machine triceps extension | yes |
| 703 | https://exrx.net/WeightExercises/Triceps/CBPushdown | pushdown returns until the forearm is close to the upper arm | yes |
| 732 | https://exrx.net/WeightExercises/Triceps/CBPushdownRope | rope pushdown | yes |
| 751 | https://exrx.net/WeightExercises/Triceps/CBOneArmPushdown | one-arm pushdown | yes |
| 776 | https://exrx.net/WeightExercises/Triceps/CBStandingTricepsExtensionRope | cable overhead extension | yes |
| 801 | https://www.acefitness.org/resources/everyone/exercise-library/74/triceps-extension/ | DB overhead extension | yes |
| 802 | https://exrx.net/WeightExercises/Triceps/DBTriExt | DB overhead extension | yes |
| 827 | https://www.acefitness.org/resources/everyone/exercise-library/19/chest-press/ | where the spotter stands | yes |
| 855 | https://www.acefitness.org/resources/everyone/exercise-library/36/lying-barbell-triceps-extensions/ | skull crusher | yes |
| 856 | https://exrx.net/WeightExercises/Triceps/BBLyingTriExtSC | skull crusher | yes |
| 886 | https://exrx.net/WeightExercises/Triceps/DBLyingTriExt | DB lying extension | yes |
| 916 | https://exrx.net/WeightExercises/Triceps/BBJMPress | JM press | yes |
| 943 | https://www.acefitness.org/resources/everyone/exercise-library/55/triceps-kickback/ | kickback: upper arm parallel and close to the torso; elbow 90 degrees or forearm hanging | yes |
| 944 | https://exrx.net/WeightExercises/Triceps/DBKickback | kickback: upper arm parallel to the floor | yes |
| 996 | https://exrx.net/WeightExercises/Triceps/WtTriDip | dip: shoulders above the hands at the top | yes |
| 1061 | https://exrx.net/WeightExercises/Triceps/BWCloseGripPushup | close-grip push-up | yes |
| 1084 | https://www.acefitness.org/certifiednewsarticle/3008/ace-study-identifies-best-triceps-exercises/ | triangle push-up (the study) | yes |

Non-URL citations: PMC13392859 (shoulder extension 60 degrees, lines 994, from Body.kt) and PMC9603242 (the dip's depth, in areas/Arms.kt:651, the catalogue) are named by number only; both are in Body.kt/Chest.kt as full URLs. The controller should check that the DBStandingPreacherCurlInclineBench page gives 45-55 degrees, that DBInclineCurl gives 45-60, and that the two kickback pages say what lines 943-944 claim.

## Tests run
- `./gradlew :domain:jvmTest` (once as-is, once with `--rerun-tasks --tests '*figures*'`): all green. Whole-module count from the result XMLs: 444 tests, 1 skipped, 0 failures, 0 errors (BatchArmsTest 21, KitTest 47, FiguresTest 7 with 1 skipped, AreaMapsLazyTest 1, BatchShouldersTest 24, BatchChestTest 33, BatchBackTest 31, BatchMainLiftsTest 20, BatchOneTest 11).
- `cd web && npm test`: 39 files, 657 tests passed.
- Not run (the controller's): speed tests, domain JS (`allTests`, `checkWebDomain`), Android, e2e, the full-area sheet in light for every figure beyond a glance (I did look at the light render of the DB overhead, skull crusher and DB lying extension; role colours are shared so a theme fault would show in every figure).
- `./gradlew --stop` done. `git status` is clean (I wrote only web/.figure-sheets/r9* and this report; the base copy and crops are in the scratchpad).

## Seen and fine
Curl, EZ-bar curl (smaller plates tell it from the Curl), DB curl (palms-in profile START, palm-up disc END), Seated DB curl, Hammer curl (profile dumbbell in both panels, staggered stance, distinct from the DB curl), the three cable curls (cable to the hand in both panels), Drag curl (bar on the upper abdomen, elbows behind the back), Incline DB curl (arms hanging behind the torso's line, upper arm vertical in both panels), Spider curl (chest on the incline, EZ bar, upper arms vertical), Preacher and DB preacher curls (arms on the pad, upright forearms), Machine curl and Machine triceps extension (lever pivot at the elbow, same length in both panels), the three pushdowns (elbows at the sides, forearm about level to nearly straight, cable to the hand), Cable overhead and DB overhead extensions (arms beside the head), Skull crusher (upper arms vertical, bar just over the forehead, spotter), Kickback (upper arm along the torso, forearm vertical then straight), Close-grip push-up, Triangle push-up (hands further towards the feet than the close-grip's, visible at full size and thumbnail).
