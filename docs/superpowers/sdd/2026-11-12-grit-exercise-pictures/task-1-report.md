# Task 1 report: figure kit (domain)

**Status:** DONE_WITH_CONCERNS. One commit on bf1ab78: `feat(domain): exercise figure kit, renderer and checks`.

## What was built
Package `domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/`:
- `Body.kt`: `Segment` (fractions of 175 cm), `Body` (drawing widths, ankle height, grip point), `Joint`, `JointRange`, `JointLimits` (per view), `Arm`, `Leg`, `Pose`, `LimbPoints`, `Skeleton`, `Pose.skeleton(view)` (forward kinematics), and `Reach.arm` / `Reach.leg` (two-joint IK, so entries can put a grip or ankle on a target and still be pure angles). Angle conventions are documented in the file: screen-CCW degrees; torso 90 = upright, 180 = supine with the head left; shoulder/hip = flexion from in line with the torso; knee/elbow = flexion; ankle = dorsiflexion from 90° to the shin. FRONT view: the same formulas read as abduction, with the near side to the right and the far side mirrored about the torso axis.
- `Kit.kt`: sealed `Prop` / `FixedProp` / `HeldProp`. Pieces: `Floor`, `FlatBench`, `PowerRack` (uprights, J-hooks, safety arms), `Barbell` (45 cm plates, `BarHold.HANDS` / `UPPER_BACK`), `Dumbbell`, `CableStack`, `LatBar`, `LatPulldownSeat` (seat and roller thigh pad), `LegPress45` (seat, reclined back pad, 45° rail, foot plate on a `sled` travel), `PullUpBar`. Every piece draws in both SIDE and FRONT. Internally: surfaces and solids as oriented slabs (signed distance), and placement rules (held pieces from grips or the upper back).
- `Figure.kt`: `View`, `Which`, `Scene`, `BodyPoint`, `Surface`, `Contact`, `KeyCheck` (`BarTouches`, `LockoutOverShoulders`, `BarOverMidfoot`, `HipDepth`), `ExerciseFigure`, body probes, and `Skeleton.point`.
- `Shapes.kt`: `Role`, `Shape` (`Circle`, `Capsule`, `RoundRect`, `Polygon`), `ViewBox`, `Bounds`, `Shape.bounds()`.
- `FigureRenderer.kt`: `viewBox` 400×300 and `MARGIN` 0.06. One shared fit per figure, from the union of both scenes. Uniform scale; the floor sits on the bottom margin and spans the panel. Tall uprights (rack posts, cable column, bar supports) are `fit = false` and clipped to the panel, as in the draft, so they don't shrink the figure. Draw order: far limbs, fixed kit, body, then per near limb a GAP outline and the limb, then held kit. Output is rounded to 0.01. Shapes and fits are cached with copy-on-write `@Volatile` maps, so the same instance is returned. `toViewBox(figure, point)`.
- `FigureChecks.kt`: `FigureProblem(name, which, message)`. It checks:
  - the catalogue name;
  - captions: blank, or over 24 characters;
  - guide lines against setup + steps;
  - joint limits;
  - contacts (±1.5 cm);
  - intersections (capsules against solids, 0.5 cm slack; arms are stopped only by the floor, because they pass beside narrow pads);
  - two-handed grips on one bar;
  - key checks;
  - every shape inside the view box.
- `Figures.kt`: `all`, merged from the area files with a duplicate guard; `internal registerForTest` / `unregisterForTest` (test-only, never in `all` or the fingerprint); `byName`; `fingerprint()` (FNV-1a 64 over the shape lists in hundredths, `"<count>:<hex>"`).
- `areas/*.kt`: nine files. Eight are empty maps. `MainLifts.kt` holds the real **Bench** entry (R1):
  - side view, START "Pause on your chest", END "Press until elbows lock", thumbnail END;
  - guideLines: RACK_SAFETY, the BENCH_LIE line, "Pause with the bar still on your chest.", "Press the bar up until your elbows lock.";
  - contacts: FEET on FLOOR; HEAD, UPPER_BACK and HIPS on BENCH;
  - checks: BarTouches(CHEST) at START, LockoutOverShoulders at END;
  - kit: 43 cm bench; rack with J-hooks at 110 cm (the IPF's highest rest); safety arms with their tops at 60 cm, under the chest (the bar touches it at about 65 cm).
- `Library.kt`: `fun Library.figure(name)` = `find(name)` → `Figures.byName(entry.name)`. It is the single lookup (R2).
- JS facade:
  - `GritDomain.figureInfo(name)` returns `JsFigureInfo{startCaption, endCaption, thumbnail "start"|"end", viewBox [400,300]}` or null;
  - `GritDomain.figureShapes(name, "start"|"end")` returns `Array<JsFigureShape>` or null. Other values of `which` throw IllegalArgumentException, like the other enum parsers;
  - `JsFigureShape` is an exported sealed class with `kind` and `role`. Its subclasses are `JsFigureCircle` (cx, cy, r), `JsFigureCapsule` (x1, y1, x2, y2, width), `JsFigureRect` (x, y, w, h, radius) and `JsFigurePolygon` (points), using the Kotlin property names;
  - regenerated with `syncWebDomain`; `web/src/domain/index.ts` is not changed (Task 3 can re-export the types).

## Tests
- BodyTest (4): segmentLengths_matchCitedTable, forwardKinematics_straightArmAt90_handDirectlyAboveShoulder, reach_placesTheGripAndAnkleOnTarget (extra), jointOutsideLimits_isReported.
- RendererTest (4): renderer_drawOrder_farBeforeBody_nearAfterWithGap, bothPanels_shareTransform (equipment identical across panels, pelvis maps to the same point, floor on the bottom margin), allShapesInsideViewBox, shapesAreCached.
- FigureChecksTest (6), on the fixture `FigureFixtures.benchDraft(barLift, footLift)`: barTouchesChest_passesForDraft_failsWhenBarRaised5cm, lockoutOverShoulders_failsWithBentElbows (extra), footOffFloor_reportsContact, limbThroughBench_reportsIntersection, captionOver24_reported, guideLineNotInGuide_reported.
- FiguresTest (5): everyFigureNameIsInCatalogue, everyFigurePassesChecks, fingerprint_matchesGolden (golden `1:0a7ed5dd154a8db0`, with Bench: identical on JVM and JS), figure_resolvesAliasesAndFormerNames ("Strict press" test registration found as "Overhead press", "OHP" and " overhead-press "; Bench via "bench press" and "Barbell bench press"), and everyCatalogueExercise_hasFigure, `@Ignore // switched on in the final task` (R4).
- GritDomainTest: figureInfo_andShapes_plainData_nullForUnknown (every JS shape matches the domain shape field by field; all four kinds appear; null for unknown names; a bad `which` throws).

**RED:** `./gradlew :domain:jvmTest --tests '*figures*'` failed to compile (Unresolved reference 'Segment' and others), as expected. The first GREEN run had one failure, the golden placeholder "TBD" against the actual `1:0a7ed5dd154a8db0`. The JS run gave the same value. Golden set after checking the render.

**Full suites (once, before the commit):**
- `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest`: exit 0. Domain JVM 228 (1 skipped), domain JS 244 (1 skipped), app 290, 0 failures.
- Web: `npm test` 642 passed, `typecheck` clean, `build` ok, `e2e --trace off` 76 passed.

**Visual check:** rendered Bench through the generated JS module to SVG and a PNG (scratchpad, not committed). The two panels share the frame. Plates are translucent, so the body shows through. Safety arms sit under the chest, uprights are clipped at the top as in the draft, and the feet are flat.

## Sources used (all cited in code comments)
- Segment lengths (Body.kt):
  - https://pmc.ncbi.nlm.nih.gov/articles/PMC12637938/: humeral 0.186, radioulnar 0.146 and hand 0.108 of height (Winter);
  - https://pmc.ncbi.nlm.nih.gov/articles/PMC3582351/: Winter-normalised foot 0.152H and thigh 0.245H; 0.246H appears in that paper, but its label was not visible;
  - cross-check: https://pmc.ncbi.nlm.nih.gov/articles/PMC11233572/ (adult thigh 0.231, leg 0.221).
- Joint ranges (Body.kt):
  - PMC3512326 standards table (shoulder flexion 180, ankle dorsiflexion 20 / plantar flexion 50, wrist);
  - PMC13392859 expected values (shoulder extension 60, hip flexion 120 / extension 20, knee 135, hip abduction 40 / adduction 20);
  - PMC4613894 (elbow 0–150, knee 0–135);
  - exrx.net/Testing/JointROM (wrist flexion 60–80; 60 used);
  - PMC1250253 and PMC8075604 (cervical flexion about 80; extension 50, the conservative value);
  - PMC9730981 (shoulder abduction 170–180; 170 used).
- Kit (Kit.kt): IPF Technical Rules 2026 on powerlifting.sport (bench at least 122 cm long and 42–45 cm high, bar rests 75–110 cm, discs at most 45 cm), and the 2021 rules (shaft 28–29 mm).

## Concerns
1. **Shank 0.246H:** the search found 0.246H in PMC3582351's Winter-normalised calculation, but not its row label. One unrelated PMC paper (PMC9602786) gives 0.246 as the torso. I used 0.246 for the shank because it closes Winter's table: 0.039 + 0.246 + 0.245 = 0.530 hip height. Task 4's sourced review should confirm the value, or switch to PMC11233572's labelled adult leg value of 0.221.
2. **Uncited drawing values:**
   - Head and torso are derived, as the brief allows: torso 0.288, neck 0.052 and head 0.130 split the 0.470H left above the hip. The split itself and the ankle height of 0.039H are not tabled on an allowed source.
   - Drawing widths, the front-view shoulder and hip half-widths (18 and 9 cm), the chest point (17 cm below the shoulder, 12.5 cm deep) and the rack, cable stack and machine sizes are neutral typical values without a citation.
3. **2D projection:** the side view uses true segment lengths, so at the bottom of the Bench the arm projects with about 37° of shoulder extension and about 142° of elbow flexion. That is inside the cited limits and matches the approved draft (about 147° there), but a real abducted arm is foreshortened. Task 4 may want to look at it.
4. **Draw order versus the draft:** the brief says held equipment is drawn last. The draft drew the plate behind the body. I followed the brief: the plate face is ACCENT_FILL (translucent) with an ACCENT ring (a single polygon annulus, non-zero fill), so the body still reads through it. Both apps must fill polygons with the non-zero rule (the default for SVG and Compose).
5. **Generated JS:** the regenerated `web/src/domain/generated/` files have large diffs, including third-party stdlib modules (new stdlib use: ULong, lazy, Volatile). This is the plain output of `syncWebDomain`, and `checkWebDomain` passes.

## Rulings applied
- R1: real Bench entry; the golden includes it.
- R2: `Library.figure` is the only lookup.
- R4: ignored completeness test with the comment.
- The alias hook `Figures.registerForTest` is `internal`.

---

# Fix report (review round 1)

## Changes
1. **Alias test is future-proof** (FiguresTest `figure_resolvesAliasesAndFormerNames`). It reads `val before = Library.figure("Overhead press")` before registering the fixture, checks that the fixture is returned while it is registered, then checks that "Overhead press" and "OHP" go back to `before` after unregistering. The old `assertNull` is gone, so a real Strict press figure added later won't break the test.
2. **Every check now has a passing and a failing case.** These were added to FigureChecksTest, on small hand-built figures (`FigureFixtures.figureOf`, `standing`):
   - `barOverMidfoot_passesOverTheFoot_failsInFrontOfIt`;
   - `hipDepth_passesBelowTheKnee_failsStanding`;
   - `grips_bothHandsOnOneBar_sideView` (two hands on one bar, side view; a bar on the upper back needs no grip);
   - `shapeOutsideViewBox_reported`. To make this testable, the view-box check moved into `internal FigureChecks.outsideViewBox(shapes)`, which `check` still calls.

   The plural and one-sided body points (THIGHS, KNEES, HANDS, NEAR_/FAR_HAND, NEAR_/FAR_FOOT, FEET) are covered in KitTest below. Each negative case asserts the panel or panels it fails in and the message.
3. **Kit and view coverage** in the new `KitTest` (10 tests). Each renders inside the view box with the roles it should have, passes its contact checks when touching, and fails them when lifted off:
   - `dumbbells_sideView_inBothHands_feetOnFloor`: Dumbbell in the side view, with FEET reported per foot.
   - `frontView_dumbbellPress_mirrorsTheFarSide_andStandsOnTheFloor`: front-view skeleton, drawing and surfaces; FlatBench in the front view; no BODY_FAR in the front view; the far side mirrors the near side.
   - `frontView_barbellLockoutInARack_levelGrips`: front-view Barbell, PowerRack and LockoutOverShoulders; uneven grips are reported as "hands are" and as bent elbows.
   - `latPulldown_seatPadCableAndBar_holdContacts`: LatPulldownSeat, CableStack and LatBar in the side view; HIPS on SEAT, FEET on FLOOR, THIGHS on THIGH_PAD; the cable ends at the bar; raising the pad reports both thighs in both panels.
   - `frontView_latBarOnACable_rendersAndKeepsGripsLevel`.
   - `pullUpBar_frontAndSide_handsOnTheBar`: PullUpBar in both views; HANDS on PULL_UP_BAR passes, and a bar 4 cm higher gives 4 problems.
   - `oneHandOnTheBench_nearAndFarPointsAreSeparate`: NEAR_HAND and NEAR_FOOT pass; FAR_HAND fails; with the far leg raised, only FAR_FOOT fails.
   - `kneesOnTheFloor_pluralPoint`.
   - `legPress45_angledPads_holdTheBackAndHips`: LegPress45; the two angled slabs are drawn as polygons; HIPS on SEAT and UPPER_BACK on BACK_PAD pass, and both fail when lifted; the foot plate moves 20 cm along 45° with the sled while the seat and back pad stay put.
   - `slab_signedDistance_andRing`.
4. **Bugs the new tests found, now fixed:**
   - Front view: the far foot's sole offset was mirrored wrongly and pointed up (`dir(mirrored(a) - 90)` becomes `dir(mirrored(a - 90))`).
   - `HipDepth` returned "no bar in the scene" when there was no held piece. It now needs no bar, and only the bar checks look for one.
   - The lat pulldown roller pad was a square slab. `Slab` now has a `radius`, so the roller is a true circle, which fixes false intersections and contact gaps at the corners.
   - Grip surfaces: hands wrap around a pull-up bar, so the new `Surface.grip` flag (PULL_UP_BAR) makes a hand contact there measure from the grip point, which may sit inside the bar.
5. **Optional item:** `Body.HEAD_RADIUS` now has a comment. The drawn head is 20 cm across (its depth in profile), centred halfway up `Segment.HEAD`. The 22.75 cm segment is the chin-to-crown height, which places the head but does not set its drawn size.

The fingerprint golden is unchanged (`1:0a7ed5dd154a8db0`), because no change touches Bench's side-view shapes.

## Tests and commands
- `./gradlew :domain:jvmTest --tests '*figures*'`: 33 tests, 0 failures, 1 skipped (the @Ignore'd completeness test). By class: BodyTest 4, RendererTest 4, FigureChecksTest 10, FiguresTest 5, KitTest 10.
- `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain`: exit 0. Domain JVM 242 (1 skipped), domain JS 258 (1 skipped), 0 failures. `syncWebDomain` regenerated the web package because main code changed, and `checkWebDomain` passes.
- Web, rerun because the generated package changed: `npm run typecheck` clean, `npm test` 642 passed.

## Concerns
Concerns 1–5 from the first report are unchanged.
