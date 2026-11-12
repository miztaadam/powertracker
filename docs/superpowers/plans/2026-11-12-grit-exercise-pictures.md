# Exercise Pictures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give all 225 catalogue exercises a code-drawn, two-pose picture. It is shown in the guide sheet and as a thumbnail on Exercises tab rows, in both apps.

**Architecture:**
- **Shared domain:**
  - the body model, with fixed segment lengths and joint angles;
  - the equipment kit;
  - one figure entry per exercise;
  - a renderer that turns a scene into colour-role shapes;
  - geometry checks.
- **Each app** draws the shapes: Android on a Compose `Canvas`, the web as inline SVG.

**Tech Stack:** Kotlin Multiplatform (`:domain`, JVM and JS), Android Compose, React/TypeScript/Vite, Vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md`

## Global Constraints

- **Style:**
  - follows `docs/library/pictures/bench-press-figure-draft.svg`: a solid figure, equipment at real-world scale, the bar and plates in the chalk accent;
  - two still poses per exercise, with no animation.
- **Sources:**
  - allowed: acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov and powerlifting.sport;
  - search with WebSearch `allowed_domains` only;
  - no blogs or forums, and never fetch these sites directly or work around bot checks.
- **Content:**
  - every picture agrees with its exercise's guide;
  - captions are at most 24 characters, non-blank, in the guide's wording;
  - where sources disagree, use the most conservative position;
  - pins or spotter appear only where the guide's safety line asks for them (heavy barbell rack work and weights moved over the face).
- **Guide sheet:**
  - the picture goes under the name and the area and equipment line, above the purpose text;
  - two panels side by side, each with its caption underneath;
  - one accessibility element, labelled `Picture: <start caption>, then <end caption>.`;
  - an exercise with no figure shows no picture area. Custom exercises keep `No form guide for your own exercises.`
- **Exercises tab:**
  - a thumbnail about 56 × 40 to the left of each row's name, hidden from screen readers;
  - custom exercises show an empty muted tile of the same size;
  - the row target stays at least 48;
  - the Plan and Today picker shows no thumbnails.
- **Colours:** role → token mapping:
  - `BODY` → main text colour;
  - `BODY_FAR` → main text colour mixed toward the surface;
  - `EQUIPMENT` → muted;
  - `ACCENT` → chalk accent;
  - `ACCENT_FILL` → chalk accent at low opacity;
  - `GAP` → the picture's surface.

  `BODY`, `EQUIPMENT` and `ACCENT` must each be at least 3:1 against the sheet surface in dark and light.
- **Layout:** no overflow at 360 dp or 375 px, and the captions are never cut off with an ellipsis.
- **Release:**
  - no schema, setting or backup change;
  - the work stays on branch `ccr-2f9842b5-owq43n`;
  - the merge to `main` happens once, after all 225 are done, with the owner's go-ahead.
- **Testing practice (owner):**
  - focused tests while building;
  - the full domain, app, web and e2e suites once per task, before its commit;
  - e2e always with `--trace off`;
  - never `playwright install`.
- **Commits:**
  - one squashed commit per task, using the title given in the task;
  - no model names;
  - each ends with the attribution lines given in the dispatch.

## Review Focus

- **Built-in rows named by an alias or a former name:** for example an "Overhead press" row from before the rename. Expected: it shows the Strict press picture. (Pinned in Task 1: `figure_resolvesAliasesAndFormerNames`.)
- **Large text:** at 200% font scale, the captions wrap and are never cut off, and the panels don't overflow. (Pinned in Tasks 2 and 3: `picture_largeText_captionsWrap`.)
- **Overlapping limbs:** for example an arm crossing the torso, or the far arm behind a bar. Expected: still readable. Near limbs are drawn after the body with a `GAP` outline, and far limbs before it. (Pinned in Task 1: `renderer_drawOrder_farBeforeBody_nearAfterWithGap`.)
- **Scrolling all 225 thumbnails:** expected to stay responsive on the web. (Pinned in Task 3: the Exercises tab `@speed` check, extended to include thumbnails.)
- **Switching theme while a sheet is open:** expected to recolour without a reopen, because colours are roles resolved at draw time. (Pinned in Tasks 2 and 3: `picture_followsThemeChange`.)

---

## File structure

**Domain** (`domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/`):

| File | Holds |
|---|---|
| `Body.kt` | `Segment` lengths, `Joint`, `JointLimits`, `Pose`, `Skeleton`, and forward kinematics `Pose.skeleton(): Skeleton` |
| `Kit.kt` | `Prop`: sealed kit pieces with real-world sizes, and their placement and grip rules |
| `Figure.kt` | `View`, `Which`, `Scene`, `Contact`, `KeyCheck`, `ExerciseFigure` |
| `Shapes.kt` | `Role`, `Shape`, `ViewBox` |
| `FigureRenderer.kt` | `shapes(figure, which)`, auto-fit, draw order and cache |
| `FigureChecks.kt` | geometry checks returning `List<FigureProblem>` |
| `Figures.kt` | `Figures.all: Map<String, ExerciseFigure>`, merged from the area files |
| `areas/*.kt` | figure entries by body area, mirroring `library/areas/` |

**Domain tests:** `domain/src/commonTest/.../library/figures/`: `BodyTest`, `RendererTest`, `FigureChecksTest`, `FiguresTest` (integrity, completeness, fingerprint).

**JS facade:** `domain/src/jsMain/.../web/GritDomain.kt`, with tests in `GritDomainTest.kt`.

**Android** (`app/src/main/kotlin/com/powertracker/ui/library/`):
- `FigureCanvas.kt` (draws shapes);
- `ExercisePicture.kt` (the two panels with captions; the thumbnail);
- modified `FormGuideSheet.kt` and `ExerciseBrowser.kt`.

Tests go in `app/src/test/.../ui/library/ExercisePictureTest.kt`.

**Web** (`web/src/screens/library/`):
- `FigureSvg.tsx`;
- `ExercisePicture.tsx`;
- modified `FormGuideSheet.tsx`, `ExerciseBrowser.tsx` and `library.css`.

Tests go in `ExercisePicture.test.tsx`, with e2e in `web/e2e/pictures.spec.ts`.

**Tools:**
- `web/scripts/figure-sheet.mjs` and the npm script `figures:sheet`: renders figures to PNG contact sheets in dark and light, in `web/.figure-sheets/` (gitignored);
- `web/scripts/pictures-page.mjs` and the npm script `figures:page`: writes `docs/library/pictures.html`.

---

### Task 1: Figure kit (domain)

**Files:**
- Create: everything under `library/figures/` listed above (the area files start empty).
- Modify: `Library.kt` (add `figure`), `GritDomain.kt`.
- Test: `BodyTest`, `RendererTest`, `FigureChecksTest`, `FiguresTest`, `GritDomainTest`.

**Interfaces (produces):**

```kotlin
enum class View { SIDE, FRONT }
enum class Which { START, END }
enum class Role { BODY, BODY_FAR, EQUIPMENT, ACCENT, ACCENT_FILL, GAP }

sealed interface Shape { val role: Role }
data class Circle(override val role: Role, val cx: Double, val cy: Double, val r: Double) : Shape
data class Capsule(override val role: Role, val x1: Double, val y1: Double, val x2: Double, val y2: Double, val width: Double) : Shape
data class RoundRect(override val role: Role, val x: Double, val y: Double, val w: Double, val h: Double, val radius: Double) : Shape
data class Polygon(override val role: Role, val points: List<Double>) : Shape  // x,y pairs

data class ViewBox(val width: Double, val height: Double)   // every panel: 400 x 300 (4:3)

data class Pose(/* pelvis x,y (cm), torso angle, and near/far joint angles in degrees */)
data class Scene(val pose: Pose, val props: List<Prop>)
data class Contact(val point: BodyPoint, val surface: Surface, val at: Set<Which>)
sealed interface KeyCheck   // e.g. BarTouches(BodyPoint.CHEST), BarOverMidfoot, LockoutOverShoulders, HipDepth(...)

data class ExerciseFigure(
    val view: View, val start: Scene, val end: Scene,
    val startCaption: String, val endCaption: String,
    val thumbnail: Which, val guideLines: List<String>,
    val contacts: List<Contact>, val checks: List<KeyCheck>,
)

object FigureRenderer {
    val viewBox: ViewBox
    fun shapes(figure: ExerciseFigure, which: Which): List<Shape>   // cached per figure+which
}
object FigureChecks { fun check(name: String, figure: ExerciseFigure): List<FigureProblem> }
object Figures { val all: Map<String, ExerciseFigure>; fun fingerprint(): String }
fun Library.figure(name: String): ExerciseFigure?   // resolves like Library.find
```

**JS facade:**
- `figureInfo(name)` returns `{ startCaption, endCaption, thumbnail: "start"|"end", viewBox: [400, 300] } | null`.
- `figureShapes(name, which: "start"|"end")` returns `Array<{ kind: "circle"|"capsule"|"rect"|"polygon", role, ...numbers }> | null`. The number fields use the Kotlin property names.

**Decisions:**
- **Units:** world units are centimetres, with y pointing down. Height is 175 cm.
- **Segment lengths:** fractions of height from a body-segment table on an allowed source (PMC). Cite it in `Body.kt`. Upper arm, forearm, hand, thigh, shin and foot must come from the table. Head and torso may be derived from it.
- **Joint ranges:** from an allowed source (ACSM, ExRx or PMC normal range-of-motion tables), cited in `Body.kt`. Use the most conservative range where sources differ.
- **Auto-fit:** fit the union of both scenes' bounds into the view box, with uniform scale and a 6% margin. Use the same transform for START and END so the two panels match. When the scene has a floor, put it on the bottom margin.
- **Draw order:** far limbs (`BODY_FAR`), then fixed equipment, then the body, then a `GAP` capsule under each near limb, then near limbs, then held equipment (`ACCENT` / `ACCENT_FILL`).
- **Contact tolerance:** 1.5 cm. The intersection check uses segment capsules against surface rectangles, with 0.5 cm slack.
- **Kit pieces in this task:** only those the first batch needs:
  - floor;
  - flat bench;
  - power rack with J-hooks and safety arms;
  - barbell with 45 cm plates;
  - dumbbell;
  - cable stack with a lat bar;
  - lat pulldown seat and thigh pad;
  - 45° leg press;
  - pull-up bar.
- **Proving fixture:** draw the bench press draft geometry as a test fixture. It is used only in tests, and Task 4 owns the real entries.

**Steps:**

- [ ] **Step 1: Write the failing tests.**
  - `BodyTest`:
    - `segmentLengths_matchCitedTable` (each length equals its fraction × 175 cm, within 0.1);
    - `forwardKinematics_straightArmAt90_handDirectlyAboveShoulder`;
    - `jointOutsideLimits_isReported`.
  - `RendererTest`:
    - `renderer_drawOrder_farBeforeBody_nearAfterWithGap`;
    - `bothPanels_shareTransform` (a fixed point maps to the same view-box point in START and END);
    - `allShapesInsideViewBox`;
    - `shapesAreCached` (the same instance is returned twice).
  - `FigureChecksTest`, on the fixture:
    - `barTouchesChest_passesForDraft_failsWhenBarRaised5cm`;
    - `footOffFloor_reportsContact`;
    - `limbThroughBench_reportsIntersection`;
    - `captionOver24_reported`;
    - `guideLineNotInGuide_reported`.
  - `FiguresTest`:
    - `everyFigureNameIsInCatalogue`;
    - `everyFigurePassesChecks`;
    - `fingerprint_matchesGolden` (the golden is the empty-set fingerprint for now);
    - `figure_resolvesAliasesAndFormerNames`, using a test-only registration of the fixture under "Strict press" and looking it up as "Overhead press" and "OHP".
  - `GritDomainTest`: `figureInfo_andShapes_plainData_nullForUnknown`.
- [ ] **Step 2:** Run `./gradlew :domain:jvmTest --tests '*figures*'`. Expected: FAIL (the types don't exist yet).
- [ ] **Step 3:** Implement the files above.
- [ ] **Step 4:** Run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` and `cd web && npm test && npm run typecheck`. Expected: all pass.
- [ ] **Step 5:** Commit `feat(domain): exercise figure kit, renderer and checks`.

### Task 2: Android pictures

**Files:**
- Create: `FigureCanvas.kt`, `ExercisePicture.kt`, `ExercisePictureTest.kt`.
- Modify: `FormGuideSheet.kt`, `ExerciseBrowser.kt`, `ExercisesScreen.kt` (pass `showThumbnails = true`), `ScreenshotTest`.

**Interfaces:**
- Consumes: Task 1's `Library.figure`, `FigureRenderer.shapes`, `FigureRenderer.viewBox` and `Role`.
- Produces:
  - `@Composable fun ExercisePicture(name: String)`, which renders nothing when there is no figure;
  - `@Composable fun ExerciseThumbnail(name: String?)`, which shows the empty tile when `name` is null or the exercise is custom;
  - `ExerciseBrowser(..., showThumbnails: Boolean = false)`.

**Decisions:**
- Role colours come from `MaterialTheme`/Grit tokens at draw time:
  - `BODY_FAR` = `lerp(text, surface, 0.45f)`;
  - `ACCENT_FILL` = accent at alpha 0.10.
- Panels use `Modifier.aspectRatio(4f/3f)` and share the row width equally, 8 dp apart.
- Captions use body-small text, centred, `softWrap` with no maximum lines.

**Steps:**

- [ ] **Step 1: Write the failing tests** in `ExercisePictureTest`, using the Task 1 fixture registered as a test figure. All eight spec §5 names:
  - `guideSheet_showsTwoPosePicture_withCaptions`;
  - `guideSheet_pictureLabel_readsBothCaptions`, with the label exactly `Picture: <start>, then <end>.`;
  - `guideSheet_noFigure_showsNoPictureArea`;
  - `exercisesTab_rowsShowThumbnail`;
  - `exercisesTab_customExercise_showsEmptyTile`;
  - `picker_hasNoThumbnails`;
  - `picture_noOverflow_atPhoneWidth` (360 dp);
  - `picture_contrast_darkAndLight` (at least 3:1 for `BODY`, `EQUIPMENT` and `ACCENT` against the sheet surface).

  Plus `picture_largeText_captionsWrap` (font scale 2.0, no ellipsis, no overflow) and `picture_followsThemeChange`.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*ExercisePicture*'`. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the full suites (Global Constraints) and re-record screenshots: the guide sheet with the fixture picture and the Exercises tab with thumbnails, in dark and light. Expected: pass. Only the new screenshots and the Exercises tab ones change.
- [ ] **Step 5:** Commit `feat(android): exercise pictures in the guide sheet and Exercises tab`.

### Task 3: Web pictures and tools

**Files:**
- Create: `FigureSvg.tsx`, `ExercisePicture.tsx`, `ExercisePicture.test.tsx`, `web/e2e/pictures.spec.ts`, `web/scripts/figure-sheet.mjs`, `web/scripts/pictures-page.mjs`.
- Modify: `FormGuideSheet.tsx`, `ExerciseBrowser.tsx` (`thumbnails?: boolean`), `ExercisesScreen.tsx` (passes `thumbnails`), `library.css`, `package.json` (scripts `figures:sheet` and `figures:page`), `.gitignore` (`web/.figure-sheets/`), and the screenshot spec.

**Interfaces:**
- Consumes: the facade's `grit.figureInfo` and `grit.figureShapes`.
- Produces:
  - `<ExercisePicture name />`;
  - `<ExerciseThumbnail name? />`;
  - `npm run figures:sheet -- <area|names…|all>`, which writes `web/.figure-sheets/<label>-dark.png` and `-light.png` (6 figures per row, captions under each panel, the name above each figure);
  - `npm run figures:page`, which writes a self-contained `docs/library/pictures.html` (inline SVG, grouped by body area, dark and light toggle).

**Decisions:**
- Colours are CSS variables:
  - `BODY_FAR` = `color-mix(in srgb, var(--text) 55%, var(--surface))`;
  - `ACCENT_FILL` = `var(--accent)` at `fill-opacity: .10`.
- Shapes are memoised per name and pose.
- The thumbnail SVG has `aria-hidden="true"`. The guide picture has `role="img"` with the label.

**Steps:**

- [ ] **Step 1: Write the failing tests:**
  - in `ExercisePicture.test.tsx`: the same ten test names as Task 2, using 375 px for the width check;
  - in `pictures.spec.ts`: the guide sheet picture fits at 375 px, and the Exercises tab thumbnails render.
  - Extend the Exercises tab `@speed` check: open, type and filter, each within 100 ms, with thumbnails.
- [ ] **Step 2:** Run `npx vitest run src/screens/library/ExercisePicture.test.tsx`. Expected: FAIL.
- [ ] **Step 3:** Implement, including both scripts. Check `figures:sheet` on the fixture.
- [ ] **Step 4:** Run the full suites and re-render the screenshots. Expected: pass.
- [ ] **Step 5:** Commit `feat(web): exercise pictures, contact sheet and pictures page tools`.

### Task 4: First batch (10 figures), then the owner checkpoint

**Files:**
- Modify: `figures/areas/MainLifts.kt`, `Shoulders.kt`, `Back.kt`, `Quads.kt` and `HamstringsGlutes.kt` (wherever each exercise's catalogue entry lives). Also `Kit.kt` if a piece needs adjusting, and the fingerprint golden.

**Exercises:**
- Squat;
- Bench;
- Deadlift;
- Strict press;
- DB shoulder press;
- Lat pulldown;
- Leg press;
- Pull-up;
- One-arm DB row;
- Romanian deadlift.

**Decisions:**
- **Sources:** each entry's poses come from its guide's setup and steps (`guideLines`). Any position the guide doesn't fix is sourced (Global Constraints) and noted in a code comment with its source.
- **Key checks, at least:**
  - Bench: `BarTouches(CHEST)` at START, `LockoutOverShoulders` at END.
  - Squat: bar across the upper back, the hip depth the guide gives, and feet on the floor.
  - Deadlift and RDL: `BarOverMidfoot`, with the bar close to the legs.
  - Strict press: lockout over the shoulders, and feet on the floor.
  - Pull-up: hands on the bar, and the chin above the bar at END if the guide says so.
  - Lat pulldown: seated, thighs under the pad.
  - Leg press: back on the pad, feet on the plate.
  - One-arm DB row: hand and knee on the bench.
- **Camera:** side view unless the movement is in the frontal plane.

**Steps:**

- [ ] **Step 1:** Add the 10 entries, test-first: each entry's checks fail before the poses are correct.
- [ ] **Step 2:** Run `./gradlew :domain:allTests`. Expected: `everyFigurePassesChecks` passes. Update the fingerprint golden.
- [ ] **Step 3:** Run `npm run figures:sheet -- batch1` to write the contact sheets.
- [ ] **Step 4:** Run the full suites. Expected: pass.
- [ ] **Step 5:** Commit `feat(domain): first ten exercise pictures`.
- [ ] **Step 6 (controller):**
  1. Independent review of the sheets against the guides and sources.
  2. Fixes.
  3. **Stop.** Send the owner the dark and light sheets with a phone notification, and wait for their go-ahead on the style. Any style changes go into the kit and renderer before Task 5.

### Tasks 5–12: Body-area batches

One task per batch, each in the same shape as Task 4: entries, kit pieces as needed, key checks, contact sheets, independent review. Each batch covers the rest of its area, leaving out the first-batch exercises.

| Task | Area file | About | Commit title |
|---|---|---|---|
| 5 | MainLifts | 21 | `feat(domain): main lift pictures` |
| 6 | Chest | 30 | `feat(domain): chest pictures` |
| 7 | Back | 31 | `feat(domain): back pictures` |
| 8 | Shoulders | 23 | `feat(domain): shoulder pictures` |
| 9 | Arms | 31 | `feat(domain): arm pictures` |
| 10 | Quads | 27 | `feat(domain): quad pictures` |
| 11 | HamstringsGlutes | 27 | `feat(domain): hamstring and glute pictures` |
| 12 | Calves + Core | 26 | `feat(domain): calf and core pictures` |

**Each task:**
- [ ] **Step 1:** Add entries test-first.
- [ ] **Step 2:** Run `./gradlew :domain:allTests` and update the fingerprint golden.
- [ ] **Step 3:** Run `npm run figures:sheet -- <area>`.
- [ ] **Step 4:** Run the full suites.
- [ ] **Step 5:** Commit.

Kit pieces are added as the batches need them: incline and decline bench, EZ bar, trap bar, kettlebell, cable handle and rope, dip bars, and the area's machines. Each piece needs its real-world size from a manufacturer-neutral typical dimension, noted in a comment.

### Task 13: Finish

**Files:**
- Modify: `FiguresTest` (switch on `everyCatalogueExercise_hasFigure`) and the screenshot specs (bench, squat and lat pulldown guide sheets, plus the Exercises tab, in dark and light).
- Create: `docs/library/pictures.html`, via `npm run figures:page`.

**Steps:**

- [ ] **Step 1:** Switch on `everyCatalogueExercise_hasFigure` and run it. Expected: PASS (225/225).
- [ ] **Step 2:** Re-record the spec §5 screenshots in both apps.
- [ ] **Step 3:** Run `npm run figures:page` and check that the page renders in Chromium.
- [ ] **Step 4:** Run the full suites. Expected: pass.
- [ ] **Step 5:** Commit `feat: all exercise pictures; pictures page for the owner`.
- [ ] **Step 6 (controller):**
  1. Send the owner `docs/library/pictures.html`.
  2. Fix any flags.
  3. Final whole-branch review, then one fix wave.
  4. Merge decision, with a phone notification.
