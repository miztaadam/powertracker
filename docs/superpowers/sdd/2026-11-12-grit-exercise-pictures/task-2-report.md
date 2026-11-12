# Task 2 report: Android pictures

**Status:** DONE. One commit on d8f7d10: `feat(android): exercise pictures in the guide sheet and Exercises tab`.

## What was built
- `app/.../ui/library/FigureCanvas.kt`
  - `FigureColors` + `figureColors(scheme, pt, surface)` (pure, used by the contrast test) and a `@Composable figureColors(surface)`. BODY = `onSurface`, BODY_FAR = `lerp(onSurface, surface, 0.45f)`, EQUIPMENT = `muted`, ACCENT = `accent`, ACCENT_FILL = accent at alpha 0.10, GAP = the surface passed in.
  - `FigureCanvas(shapes, colors, modifier)`: draws the shapes in list order on a Compose `Canvas`, fitted and centred with the 400×300 view box kept. Capsules are round-capped lines; polygons are `Path`s with `PathFillType.NonZero`, built once per shape list.
- `app/.../ui/library/ExercisePicture.kt`
  - `ExercisePicture(name, modifier, figureFor = Library::figure)`: two panels (`aspectRatio(4f/3f)`, equal weights, 8 dp apart), a body-small centred caption under each (softWrap, no max lines). One accessibility element (`clearAndSetSemantics`: label `Picture: <start>, then <end>.`, role Image). Composes nothing when there is no figure.
  - `ExerciseThumbnail(name: String?, modifier, figureFor = Library::figure)`: a 56 × 40 dp tile in `surfaceContainerHigh` with the figure's `thumbnail` pose; empty tile when `name` is null or no figure is found (R6). No semantics beyond a test tag, so a screen reader skips it.
  - `pictureLabel(figure)`, `thumbnailSurface(scheme)`.
- `FormGuideSheet.kt`: `FormGuideContent(..., figureFor = Library::figure, actions)`; the picture sits under the header (name, area · equipment), above the purpose. Custom exercises are unchanged.
- `ExerciseBrowser.kt`: `showThumbnails: Boolean = false`; rows get the thumbnail left of the name (catalogue name passed, so the lifter's own rows get the empty tile). `ExercisesScreen.kt` passes `showThumbnails = true`; the picker passes nothing.
- Screenshots: new `form-guide-picture-{dark,light}.png` (Bench guide). Re-recorded: `exercises-*`, `exercises-muscles-*`, `exercise-day-chooser-*` (the last two show the tab behind a sheet). No other screenshot changed.

## Tests (`ExercisePictureTest`, 10)
guideSheet_showsTwoPosePicture_withCaptions, guideSheet_pictureLabel_readsBothCaptions, guideSheet_noFigure_showsNoPictureArea (seam `figureFor = { null }`, plus a custom exercise), exercisesTab_rowsShowThumbnail, exercisesTab_customExercise_showsEmptyTile, picker_hasNoThumbnails, picture_noOverflow_atPhoneWidth (360 dp, guide and tab rows; info button still ≥ 48 dp), picture_contrast_darkAndLight (≥ 3:1 for BODY/EQUIPMENT/ACCENT on the sheet surface and on the thumbnail tile, both themes, and the token mapping), picture_largeText_captionsWrap (font scale 2.0: ≥ 2 lines, no ellipsis, all text laid out, panels 4:3 inside the sheet), picture_followsThemeChange (theme flipped under the composed sheet; pixels recolour).
Drawing is checked on real pixels (Robolectric native graphics): role colours present in panels/thumbnail, the two panels differ, the empty tile is flat.

- **RED:** `./gradlew :app:testDebugUnitTest --tests '*ExercisePicture*'` failed to compile: no parameter `figureFor`, unresolved `figureColors`, `thumbnailSurface`.
- **GREEN:** 10/10. On the way, three of my own assertions were corrected (not the code): the Exercises tab's sideways-scrolling chip rows fail the whole-screen `assertTextsFit`, so the tab check is scoped to the row; `didOverflowWidth` is true for any wrap-content text; pixel counts across themes differ by ~1% from anti-aliasing and the translucent plate, so that check allows 10%.
- **Full suites (once):** domain JVM 242 (1 skipped), domain JS 258 (1 skipped), `checkWebDomain` ok, app 301 (was 290: +10, +1 screenshot test), 0 failures. Web: 642 passed, typecheck clean, build ok, e2e `--trace off` 76 passed.

## Files
New: FigureCanvas.kt, ExercisePicture.kt, ExercisePictureTest.kt, docs/design/screens/form-guide-picture-{dark,light}.png.
Modified: FormGuideSheet.kt, ExerciseBrowser.kt, ExercisesScreen.kt, LibraryShowcase.kt, ScreenshotTest.kt, six Exercises-tab PNGs.

## Sources
None needed (no new content or positions).

## Self-review
- Draw order untouched; non-zero fill; colours resolved at composition from the theme, never stored.
- Shapes come from the renderer's cache and are `remember`ed; only visible rows compose (LazyColumn).
- No schema/setting/backup change. Web untouched.

## Concerns / decisions for the controller
1. **Design choices not fixed by the brief** (Task 3 should match): the thumbnail tile is `surfaceContainerHigh` with the extra-small corner (6 dp), and thumbnails with a figure are drawn on the same tile (GAP = tile colour); guide panels have no frame or background (drawn straight on the sheet surface, like the draft); captions use `onSurfaceVariant`; 4 dp between panel and caption; 12 dp above the picture; 12 dp between thumbnail and name.
2. **Screenshot list:** only Bench exists, so the spec's squat and lat pulldown guide screenshots are left for the final task. Until other figures exist the tab screenshot shows mostly empty tiles.
3. **Thumbnail legibility:** at 53 × 40 dp the Bench reads, but the plate ring and J-hooks are very fine; worth a look in the first-batch review (Task 4).
4. **Commit attribution:** constraints.md lists "Claude Opus 5.5" lines, written for the Task 1 implementer. This session's harness instruction gives the Fable 5.2 lines, which I used since they are accurate for this task. Amend if the owner wants them uniform.
5. `ExercisePicture`/`ExerciseThumbnail`/`FormGuideContent` take a `modifier` and `figureFor` in addition to the brief's `name` (R2).
