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

