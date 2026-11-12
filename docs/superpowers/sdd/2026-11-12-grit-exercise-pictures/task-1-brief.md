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

