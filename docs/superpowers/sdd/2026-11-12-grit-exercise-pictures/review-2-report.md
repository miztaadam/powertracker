# Review of Task 2 (Android pictures), BASE d8f7d10 -> HEAD adae3f3

**Spec verdict: PASS (✅)**
**Quality verdict: Approved** (no Critical or Important findings; Minor items optional)

Counts: Critical 0, Important 0, Minor 5.

## Spec compliance (checked against brief, constraints, spec §3/§5, R2/R6)
- Placement: `FormGuideSheet.kt` puts `ExercisePicture` after the header (name, area/equipment) and before the purpose text; custom exercises keep `NO_GUIDE` and no picture. The test asserts eyebrow.bottom <= picture.top and picture.bottom <= purpose.top. OK.
- Two panels, equal weight, 8 dp apart, `aspectRatio(4/3)`, caption under each (bodySmall, centred, softWrap, no maxLines). OK.
- One a11y element: `clearAndSetSemantics` with label `Picture: <start>, then <end>.` and Role.Image; children are not read. OK.
- No figure -> `ExercisePicture` returns before composing anything (no spacing either). R2 seam `figureFor` is used by the test. OK.
- Thumbnail 56x40 dp left of the name, no semantics (decorative), empty muted tile when name is null or no figure (R6), row height >= 48 asserted, picker unchanged (`showThumbnails` default false; only `ExercisesScreen` sets true). OK.
- Colours: BODY=onSurface, BODY_FAR=lerp(onSurface, surface, 0.45), EQUIPMENT=muted, ACCENT=accent, ACCENT_FILL=accent alpha 0.10, GAP=surface passed in. Resolved at composition from the theme, never stored. Contrast >= 3:1 tested dark and light on the sheet surface (`scheme.surface`, which is the ModalBottomSheet `containerColor`) and on the thumbnail tile. OK.
- Drawing: shapes iterated in list order; polygons use `PathFillType.NonZero`; held equipment order is left to the renderer. Shapes come from the renderer cache and are `remember`ed. Thumbnails only compose for visible `LazyColumn` rows. OK.
- Screenshots: bench guide picture and re-recorded Exercises tab, dark and light. The squat and lat pulldown guide screenshots are deferred (only Bench has a figure); this is consistent with the brief and R1, and must be picked up in Task 13.

## Spec §5 test names (all 10 present, plus 2 extra checks in the brief)
All of the following exist in `ExercisePictureTest.kt` and assert what the name says:
- `guideSheet_showsTwoPosePicture_withCaptions`: geometry (side by side, same size, 4:3, 8 dp, full width), captions under their panels, real pixel colours in both panels, the two poses differ, vertical order. Not hollow.
- `guideSheet_pictureLabel_readsBothCaptions`: literal expected string, role, no children, captions not exposed separately. Not hollow.
- `guideSheet_noFigure_showsNoPictureArea`: seam returning null plus a custom exercise. Not hollow (the spacing bound is loose, see M4).
- `exercisesTab_rowsShowThumbnail`: size, left offset, order, vertical centring, pixels, no semantics, row height, one per row.
- `exercisesTab_customExercise_showsEmptyTile`: same size and alignment as a drawn thumbnail, >97% flat tile colour, no role colours.
- `picker_hasNoThumbnails`: scrolls to a catalogue row and asserts zero thumbnails.
- `picture_noOverflow_atPhoneWidth` (360 dp): bounds, no clipping, margins, no ellipsis, and the tab row text.
- `picture_contrast_darkAndLight`: ratios plus token mapping, both themes, both surfaces.
- `picture_largeText_captionsWrap` (2.0 scale): wraps to >= 2 lines, no ellipsis or visual overflow, the last line ends at the last character, panels keep 4:3.
- `picture_followsThemeChange`: flips the theme under the composed sheet and compares pixel counts.

## Findings

### Minor
- **M1. `FigureCanvas.kt:96,111` (per-frame hashing of polygon keys).** `polygons` is an `associateWith` map keyed by the `Polygon` data class, and `polygons.getValue(shape)` runs on every draw. `Polygon.points` is a `List<Double>`, so each lookup hashes (and on a hit, compares) the whole boxed list for every polygon, every draw pass (cheap for one sheet, but it repeats per visible thumbnail row). Fix: build the draw commands once in `remember(shapes)`, for example a `List<(DrawScope) -> Unit>` or a small `sealed` list of precomputed `Path?` aligned by index (`val paths = remember(shapes) { shapes.map { (it as? Polygon)?.toPath() } }`, then `paths[i]!!` inside an indexed loop). That also avoids collapsing identical polygons into one key.
- **M2. `ExercisePicture.kt:35` (duplicated aspect constant).** `PANEL_ASPECT = 4f/3f` is hardcoded while `FigureCanvas` fits to `FigureRenderer.viewBox`. If the view box ever changes, the panels would letterbox. Fix: derive it as `(FigureRenderer.viewBox.width / FigureRenderer.viewBox.height).toFloat()`.
- **M3. `ExercisePicture.kt:93` (extra work per thumbnail recomposition).** `figureColors(tile)` runs a `lerp` and allocations for every visible row on every recomposition, and the figure lookup `name?.let(figureFor)` is not remembered. Both are cheap, but `remember(name)` for the lookup and keying colours on the scheme would cost nothing and keep rows light while scrolling 225 items.
- **M4. `ExercisePictureTest.kt` (weak spots).**
  - `guideSheet_noFigure_showsNoPictureArea` bounds the header-to-purpose gap with a loose `< 40 dp`; compare it with the exact `Space.m` top padding instead.
  - R6 (a catalogue exercise without a figure shows the empty tile) is covered only indirectly through the custom row, because `ExercisesContent` has no `figureFor` seam. Optional: add one so R6 is pinned.
  - The thumbnail "decorative" check proves only the absence of a content description and role; the stronger merged-tree check (`onAllNodes(hasTestTag("exercise-thumbnail"))` is empty) is already there, which is sufficient.
- **M5. Commit attribution.** The implementer used different attribution lines from `constraints.md` (already noted in the report and progress.md for normalising at squash). Not a code issue, so repeating it here only for tracking.

## Code quality notes (no action needed)
- Composition is clean: pure `figureColors(scheme, pt, surface)` for the test, with a composable wrapper; `remember` keyed on figure and pose; `figureFor` defaulting to `Library::figure` keeps production call sites unchanged (`FormGuideContent`'s trailing `actions` lambda still binds, and both existing callers compile).
- `ExerciseThumbnail` uses the tile colour as GAP, so overlapping limbs read correctly on the tile.
- Name clash between `androidx.compose.ui.semantics.Role` and the figures `Role` is handled by importing only one in each file.
