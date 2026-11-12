# Task 10 report: quad pictures (27 new figures)

**Status:** DONE_WITH_CONCERNS. One commit on BASE cf1f9e5: `44bdb8d feat(domain): quad pictures` (local, not pushed). Attribution: my harness's lines (Co-Authored-By: Claude Opus 5.5 + the session line), as the brief allows.

**Count:** 28 `BodyArea.QUADS` entries in library/areas/Quads.kt; Leg press existed, so 27 new. The brief's list named 26: **Bulgarian split squat** is the 27th (in the catalogue, missing from the list, like JM press last time). `BatchQuadsTest.quads_everyExerciseHasAFigure_thatPassesItsChecks` asserts 28 names, every one passes FigureChecks, and the camera per figure.

**Golden:** `172:e9c3e46c6c2195c7`. Proof the 145 old figures did not move: with `quadFigures` cut back to Leg press only (after all kit, renderer-wiring and MainLifts/Leg-press refactor changes), FiguresTest passed against the old golden `145:4ac19b1768653a16` (7 tests, 1 skipped, 0 failures). Leg press was refactored into a shared `LegPressRig` with identical arithmetic; the golden proves its drawing is unchanged.

## Shared code changed (no existing drawing changes)
- **Kit.kt:** `BarHold.CROOK` (Zercher: bar in the elbow crook, `crookOf` in KitQuads.kt); wiring (surfaces, solids, draw, drawFront) for the four new pieces; SquatBox doc notes its reuse as the step box.
- **Figure.kt:** `Surface.SHIN_PAD`, `Surface.SLED_HANDLE(grip = true)`; new key check `FeetOffFloor(min, at)` (the jump's in-air panel; the only panel with no floor contact).
- **FigureChecks.kt:** `FeetOffFloor` evaluation.
- **MainLifts.kt:** `private` → `internal` only, for reuse: `barOnBack`, `squat`, `midfootX`, `balancedTorso`, `kneeForDepth`, `backSquatPose`, `squatRack`, `frontSquat`.
- **Renderer:** unchanged.

## Kit (KitQuads.kt), each with a typical-size comment with units; KitTest +6 cases (53 in the class)
| Piece | What |
|---|---|
| `HackSquatMachine` | 45° rail, back pad (75 cm, 6 cm thick) riding it with the hips, shoulder pad (10 cm roll, drawn in front), fixed foot plate (50 cm) square to the rail, base frame. Surfaces BACK_PAD, FOOT_PLATE. |
| `LeverSquatMachine` | Lever from a pivot on a post behind the lifter to a yoke roll (12 cm) on the shoulders; yoke drawn in front, lever behind. The figure sets the pivot so the lever is the same length in both panels. |
| `LegExtensionMachine` | Seat (40 cm, 52 up), back pad (60 cm) hinged at the seat's rear, lever pivoting at the knee to a 12 cm roller on the lower shin (lever + roller drawn in front with gap), side handle, optional far roller for the alternating machine. Surfaces SEAT, BACK_PAD, SHIN_PAD. |
| `PushSled` | 100 cm skid, upright handle post (125 cm, held: SLED_HANDLE), plate post with 45 cm plates in the accent. |
| `BarHold.CROOK` | Bar centre inside the elbow bend, half a forearm + shaft + 1 cm from the elbow. |
| step box | SquatBox reused (45 cm forward step-up, 40 cm lateral; under the 50 cm knee joint). |

## Per figure (camera, sources, judgement calls; every comment in figures/areas/Quads.kt)
Camera: side view for all except **Lateral lunge** (the step is straight out to the side), **Curtsy lunge** (the back leg crosses behind to the other side) and **Lateral step-up** (box beside, sideways step) — front view, lesson 9; reasons in the Quads.kt header.

Grip: "at your sides" drawn palms in (side view: dumbbell in profile square to the hand; front view: end-on), matching the approved DB shrug; Lateral lunge's "palms facing each other" end-on; Step-up's "palms facing in" profile.

Balance rule for light loads (neutral, documented): shoulders over the mid-foot (puts the torso ~57° above level at depth). Barbell/yoke squats use the existing bar-over-midfoot rule.

- **Bodyweight squat:** thighs about level (hip 0.5 cm under knee); shins 33° (the Squat's); END arms straight forward level — ACE 5 variations of the body-weight squat (arms forward to offset the hips). START arms down.
- **Goblet squat:** DB upright against the chest (profile along the torso, centre 23 cm out, 16 cm under the shoulder: neutral), elbows down; just past level (HipDepth 2).
- **DB squat:** arms hang straight down, DBs over the feet; ExRx DBSquat.
- **Kettlebell front squat:** the approved KB press rack arm (15°, 145°), two kettlebells RACK; just past level.
- **Zercher squat:** upper arm 25° forward, elbow 140° (neutral); torso solved so the bar is over the mid-foot (standing: the straight body tips back 2.6°); thighs about level; rack (RACK_SAFETY) with posts 60 cm out, pins 6 cm under the low bar. ExRx, NSCA.
- **Single-leg box squat:** free (far) leg extended forward off the floor in both panels (ExRx BWSingleLegBoxSquat: other leg forward, arms forward); START free leg 30° forward knee 10°; END hips on box (thighs level, the conservative depth), torso 55° (neutral), free leg straight 22° down. Plainly different from Box squat at thumbnail (checked).
- **Jump squat:** START knees 90° (ACE explosive plyometric workout 5869: knees bent to 90 degrees), shins 25°, heels down, arms hanging; END in the air, legs straight, ankles 30° pointed, feet ~20 cm up, no contact in that panel (FeetOffFloor 10).
- **Smith squat:** the Squat's depth and 44° bottom torso; standing torso solved so the bar is on the same vertical line (BarOnRails both panels, BarOverMidfoot both); stops 4 cm under the low bar. ExRx SMSquat.
- **Smith front squat:** the approved Front squat's poses on the rails (already balanced over the mid-foot); HipDepth 0 (the Front squat's depth). ExRx SMFrontSquat.
- **Smith split squat:** the split squat's poses, bar on the back, rails on its line.
- **Hack squat:** a squat tilted back 45° in the plate's frame: END knees 126° ("just short of fully bent"; ExRx SLHackSquat "just short of complete flexion"), shins 33° to the plate; START knees 3°; feet 18 cm forward of the hips' line (ExRx: platform slightly higher than the base); hips slide along the rail; arms resting along the body as the Leg press's.
- **Lever squat:** yoke over the mid-foot (ExRx LVSquat: feet directly beneath the shoulders; thighs just past parallel), the Squat's arms; pivot 120 cm behind (the yoke moves almost straight down, so the equal-length line is near level).
- **Leg extension / Single-leg leg extension:** seat 52 cm, back 15° reclined, thighs 3° down along the seat (neutral); knee at the pivot; roller at 80% of the shin; START knees 90°, END 3° (ExRx LVLegExtension / LVAlternatingLegExtension). Single-leg: alternating machine, far leg stays at 90° under its own pad — plainly apart at thumbnail.
- **Single-leg leg press:** the Leg press's machine, sleds and working leg (asserted identical); far foot flat on the floor in front of the seat, knee ~75° (ExRx SLSingleLeg45LegPress).
- **Sled push:** START torso 80°, hips 72 cm, back foot back on its ball, front foot flat, arms straight level to the post (ExRx WTPushSprint: hips low, one foot back, torso nearly vertical, arms straight; heels off ground at the start). END torso 55° (neutral — no allowed source gives a lean angle; ExRx's own page says nearly vertical), driving back leg, both heels up.
- **Lunge / Reverse / Barbell lunge / Barbell reverse lunge / Walking lunge / Split squat:** shared `splitBottom`: front shin 8° (neutral), front thigh about level (hip 3.5 cm above the knee), back thigh 18° behind vertical (inside 20° extension) so the back knee's round is ~6.7 cm off the floor, back foot on its ball, torso upright. Lunge: back toes where the feet were; Reverse: front foot where the feet were. Walking lunge: START one lunge, END the next lunge one step on with the legs swapped (shows travel). Barbell versions: the Squat's arms, rack in front with pins under the low bar and safety arms ending 8 cm in front of the furthest-forward chest. Split squat / Smith split: hips go straight up; START as high as the back leg's hip and ankle limits allow.
- **Bulgarian split squat:** bench 40 cm (PMC10437032: rear foot elevated to 40 cm), back foot pointed to the 50° limit with the top of the foot on the pad, ankle just past the bench edge; back knee 12 cm up; front shin 10° forward, thigh about level; START front leg 10° short of straight, back foot pivoting on the pad.
- **Step-up:** box 45 cm; START near foot wholly on the box, far leg on the floor; END standing tall on the box, both feet up (guide). ExRx DBStepUp.
- **Lateral lunge (front):** START feet hip-width; END near foot out, shin upright, hips 29 cm back and down to 67 cm, torso lean 35° (neutral), far leg straight; the 40° abduction limit sets the ~100 cm step; arms hang so the weights come down by the near knee. ACE 364, ExRx DBSideLunge.
- **Curtsy lunge (front):** START ankles 22 cm out from the hips; END front (near) foot stays, shin upright, thigh forward; back (far) thigh crosses 12 cm in (inside 20° adduction), knee 20 cm up, foot on its ball behind and past the front foot. Depth neutral (no allowed source). ACE 8 butt-toning moves.
- **Lateral step-up (front):** box 40 cm beside; START near foot on the box 15 cm forward, knee bent forward/out, far leg straight on the floor; END standing on the straight near leg, far foot off the floor beside the box (8° out, knee 30° behind, neutral). ExRx DBLateralStepUp.

Sources: all URLs are the exercises' own Sources.kt pages except ACE 5875 (bodyweight squat arms), ACE 5869 (jump depth) and PMC10437032 (40 cm bench) — each seen in its search result text. Everything else is marked neutral.

## Tests
| Suite | Result |
|---|---|
| domain jvmTest | 473 passed, 1 skipped (BatchQuadsTest 23, KitTest 53, FiguresTest 7, AreaMapsLazyTest 1) |
| domain jsNodeTest | 488 passed, 1 skipped (one run failed a new KitTest message match because JS prints 10 not 10.0; fixed the test, green) |
| checkWebDomain | ok (bridge regenerated) |
| app testDebugUnitTest | 301 passed; no screenshot changed |
| web unit | 657 passed; typecheck clean; build ok |
| e2e `--trace off` | 90 passed |

- **BatchQuadsTest (23 tests):** real bounds per figure: depth and torso angle, shin lean, feet flat, knee bends in both panels for every leg (lesson 5), load position (DB against chest, hanging, racked, crook, bar/yoke over the mid-foot), rails and stops, pad/pivot/roller placement, lever length constant, split-stance front shin/thigh/back knee height/back hip extension/heel up/step length, foot travel (lunge vs reverse vs walking), bench edge and pointed foot, free-leg separation for the one-leg versions, front-view out-of-plane angles, the FeetOffFloor check failing on a grounded foot.
- **RED:** with `quadFigures` cut to Leg press, BatchQuadsTest ran 23 and 23 failed with "no figure for …"; FiguresTest green on the old golden.
- **GREEN:** first runs caught real faults, fixed: Bulgarian front shin leaning back (sign error), split-stance tops past the hip-extension and ankle limits, hip-depth 0 failing by rounding, Zercher standing bar 5 cm off the mid-foot, single-leg box free thigh through the box, Smith squat hip at 141.7°, sled push back leg past −20° and off the floor, lever squat pivot at infinity (empty render).

## Speed (after `./gradlew --stop`, ~45 s settle; load ~2.6–3.0 from the machine)
Run 1: 4/4 passed (14.5 s). Run 2: 4/4 (15.0 s). Run 3: 4/4 (14.8 s).

## Sheets
`/home/user/powertracker/web/.figure-sheets/quads-dark.png`, `quads-light.png` (all 28). Detail sheets q1–q6 (`/home/user/powertracker/web/.figure-sheets/q1-dark.png` … `q6-light.png`; q6 compares the one-leg versions with Box squat, Leg extension, Leg press). Fixed after looking: lever squat empty, hack squat arms looping at the head (now resting along the body), Zercher rack crowding the forearms (posts 60 cm), curtsy crossing hidden (front foot wider, back shin further across), lateral step-up free leg indistinct (8° out), lunges' back shin lying on the floor (back knee raised to ~6.7 cm clearance).

## Concerns
1. **Back shin in every split stance runs back nearly level** to the raised heel: the figure's foot has no toe joint, so with the 50° plantar limit the shin can't rise as a real one does; the back knee is ~6.7 cm off the floor, so it reads as low, close to kneeling at thumbnail.
2. **Split squat / Smith split START** is only ~23 cm above the bottom (front knee ~56°): the 20° hip-extension limit with the back foot fixed stops the rise; reads more like a half-lunge than standing tall.
3. **Curtsy lunge** from the front: the far leg is drawn in BODY colour like the near (front-view renderer rule), so the crossing reads only moderately; the front thigh points at the camera and is short.
4. **Jump squat END** reads as standing at small size; the ~20 cm floor gap is the only cue (caption "Jump up explosively").
5. **Sled push END torso 55°** is neutral; ExRx says nearly vertical with the body dropping low.
6. **Hack squat** head extends past the back pad's top; arms rest along the body (handles not drawn).
7. Lever squat lever arrives at head height from behind at START (pivot 120 cm back, near hip height).
8. Barbell lunges' safety arms are symbolic (they can't catch a lunge) but the guide's RACK_SAFETY line asks for them.
9. Bulgarian START back foot pivots on the pad (ankle moves ~0.3 cm vertically) to stay inside the ankle limit.

## Fix round 1

Brief: task-10-fix1-brief.md, from review-10-report.md. I amended 44bdb8d (message and attribution kept). The commit is now 8ed18d4, on cf1f9e5.

### Fix 1: every split stance read as kneeling (Lunge, Reverse, Walking, Barbell lunge, Barbell reverse, Split squat, Smith split)
- **Cause:** worse than "too little plantar flexion". The old back foot pointed its toes backwards (ankle at −45°, foot pointing back and down), so the shin lay along the floor, and the foot looked like an instep on the ground.
- **Now:** a new `backLegOnBall` stands the back foot steeply on its toes, pointing forward and down at `BALL_FOOT` = 74° below level, so the toes are under the raised heel. That lifts the ankle to about 21.4 cm, and the shin slopes about 16° up from the knee to the heel (15° on the split squats).
- **I relaxed no check.** The back ankle comes out at about 33–34° of dorsiflexion, inside the existing 35° limit: the steep, forward-pointing foot does the work the toe joint would do. So I needed no flag and changed no limit. The brief allowed a flag "for exactly this case", but the case did not need one.
- **Height limit:** 22–26 cm, as the brief asked, is out of reach. With a straight 26.6 cm foot, about 21.7 cm is the most the ankle can rise with the toes on the floor.
- **Back knee:** the front thigh now sits 0.8 cm above level, not 3.5, so the knee's round is about 4 cm off the floor ("just above").
- **Source:** ExRx DBSplitSquat (raised rear heel) is cited in the BALL_FOOT comment.
- **Bulgarian split squat:** it has its own back-foot code and did not change.
- **New `assertSplitBottom` bounds:**
  - the back shin slopes 12..25° up towards the heel;
  - the heel is 14..25 cm up (was 5..30);
  - the toes are forward of the ankle and on the floor;
  - the back knee's round is 2..7 cm off the floor;
  - the stance (ankle to ankle) and the back hip are set per figure.
- **Thumbnails:** I looked at all seven at app size and they read as lunges.

### Fix 2: the split squats' START was a half-lunge
- **Shorter stance:** the two split squats now have their own (`splitSquatBottom`):
  - the back thigh 12° behind vertical;
  - the front shin 15° forward;
  - the front thigh 1.5 cm above level;
  - about 83 cm ankle to ankle.
- **`splitTop`:** the front leg is now 2° short of straight (was 3°).
- **START now:** the front knee is about 9–12°, the hips rise 35.1 cm, and the back foot pivots on its ball.
- **Test bounds:**
  - hips rise 35..50;
  - front knee at the top 5..25;
  - back knee at the top 10..60;
  - stance 80..90;
  - back hip −15..−5.
- The lunges keep their long step (about 92 cm).

### Fix 3: Jump squat END read as standing
- **The jump:** the feet are about 37 cm off the floor. FeetOffFloor's minimum is now 25, and the test bound is 30..50.
- **Arms:** ACE's squat jumps page (Sources.kt ACE_SQUAT_JUMPS, exercise-library/116) says to drive the arms forward and upward during the jump. I saw that in the search result, so the END arms are straight, swung 100° forward and up. They are cited. START keeps the arms hanging (the guide's set-up).
- At thumbnail size the gap and the raised arms are plain.

### Fix 4: Hack squat START arm
- The hands now rest on the handles by the hips: 12 cm up the torso from the hip joint and 12 cm in front of its axis, solved with Reach.arm. That is the same place on the body in both panels.
- The test bounds the grip to within 0.5 cm of that point in both panels.

### Fix 5: citations (comments)
- **Jump squat:** the 90° knees are now marked neutral, and the ACE 5869 citation is removed. My own allowed-domain search did not show "90 degrees" on ACE 116 or 222 either.
- **Smith split squat:** Sources.kt has `https://exrx.net/WeightExercises/Quadriceps/SMSplitSquat`, so I kept it. The comment says that is Sources.kt's URL, and that ExRx also lists the page under GluteusMaximus.

### Minor 3: Smith squat START torso
- The straight legs now tip forward over the ankles just enough (0.5° steps) that the torso, solved for the rail line, is at least 80°. The bar is still on the rails (BarOnRails) and over the mid-foot.
- The test bounds the standing torso to 80..90 and the legs' lean to 0..10.

### RED/GREEN
- **RED:** with 44bdb8d's Quads.kt restored and the new tests in place, BatchQuadsTest ran 23 and 6 failed:
  - Jump squat: FeetOffFloor 25 is not in the checks;
  - Hack squat: the hand is 26.5 cm off the handle at START;
  - Smith squat: the standing torso is 76.1;
  - Lunge and Walking lunge: the back heel is 13.5 cm up, under the new 14 minimum;
  - Split squat: the back hip is at −18.
- **GREEN:** all 23 pass.

### Golden and tests
- **Golden:** `172:84a1fde1ff223652`.
- **Old figures unchanged:** with the quad map cut back to Leg press, FiguresTest passed against `145:4ac19b1768653a16`.

| Suite | Result |
|---|---|
| domain jvmTest | 473 passed, 1 skipped |
| domain jsNodeTest | 488 passed, 1 skipped. The first full run hit the known GoldenScenarioTest Mocha 2 s timeout under load; the rerun was green. |
| checkWebDomain | ok. The bridge was regenerated; kotlin-kotlin-stdlib.mjs changed too. |
| app testDebugUnitTest | 301 passed; no screenshot changed |
| web unit | 657 passed |
| typecheck | clean |
| build | ok |
| e2e `--trace off` | 90 passed |

- `./gradlew --stop` is done. No speed runs.
- **Sheets:** `/home/user/powertracker/web/.figure-sheets/quads-{dark,light}.png`, re-rendered. Detail sheets: `f1-*.png` (the lunges, split squats, jump and hack squat) and `f2-*.png` (Smith squat, Bulgarian, sled push).

### Not done / concerns
- The back ankle tops out at about 21.4 cm, under the brief's 22–26: that is the most a straight foot allows.
- The back foot stands steeply, 16° forward of vertical, so it can look slightly en pointe at full size.
- The Sled push back foot still uses the older on-the-ball solver. It points forward and its shin is mostly upright, so it is not affected.
