# Task 13b report: every exercise has a picture; pictures page

**Status:** DONE_WITH_CONCERNS (concerns are small; see the end). One commit on edec171: `feat: all exercise pictures; pictures page for the owner`.

## 1. Completeness check
- `FiguresTest.everyCatalogueExercise_hasFigure`: `@Ignore` and its import removed. JVM: FiguresTest 7/7 passed, this test included. Also passes on JS in the full domain run. 225 of 225, nothing missing.
- Fingerprint golden `225:4cef8580b4d880a3` unchanged. No file under `domain/src/commonMain` changed.

## 2. Leftovers from "until every exercise has a figure"
Changed:
- `app/.../ui/library/ExercisePicture.kt` and `web/src/screens/library/ExercisePicture.tsx`: the KDoc said the picture is empty for "a custom exercise, or a catalogue exercise not drawn yet". It now says the lifter's own exercise (every catalogue exercise has one). Behaviour is the same: no figure gives no picture area and an empty tile.
- `domain/src/jvmTest/.../AreaMapsLazyTest.kt`: `names.size >= 61` is now `== 225`. The comment "(The areas not drawn yet hold a plain empty map.)" was untrue. It now says Calves and Core hold plain maps of lazy entries, and that their figures are checked among the delegates.
- `web/scripts/pictures-page.mjs`: removed the "Not drawn yet: …" line per area and its `.todo` CSS. Each area heading now shows its count. The intro says "All N exercises, by body area." The script now fails, naming the exercises, if any catalogue exercise has no figure. The header comment no longer says "drawn so far".
- `web/scripts/figure-sheet.mjs`: the usage line "every figure drawn so far" now reads "every exercise's figure". The group skip stays, for an exercise added before its figure. Its doc comment and console line ("No figure yet, so left out") were reworded.

Kept on purpose:
- The no-figure tests in both apps (`guideSheet_noFigure_showsNoPictureArea`; the unit tests where a lookup that finds nothing stands for an exercise without a figure). They inject a null lookup or use the lifter's own exercise, so they still pin the code path for custom exercises and do not depend on the catalogue being incomplete.
- e2e `pictures.spec.ts` `withFigure(...)`: still correct, since it now equals every name.

Left for the controller (not edited, as the brief says): `web/e2e/library.spec.ts:166`, in the @speed test "the picker still settles within 100 ms with a few hundred exercises to list". The comment "The catalogue grows to about 250 entries; until it has, the lifter's own exercises stand in for them." is now untrue. The catalogue is complete at 225, and the test lists 225 plus 250 added entries, so it is a few-hundred stress case rather than a stand-in. The steps, numbers and threshold were not touched.

## 3. Screenshots
New tests, written the way the existing ones are:
- Android `ScreenshotTest.formGuideLatPulldown` (with `LibraryShowcase.FormGuideLatPulldownShowcase`, 412×1370 dp).
- Web `screenshots.spec.ts` "the lat pulldown's form guide with its picture". It searches, opens the Lat pulldown guide, waits for the picture and shoots it.

The squat and bench guide shots already existed in both apps (squat = `form-guide-*`, bench = `form-guide-picture-*`). Both apps were re-recorded: Android with `-Pscreenshots.update=true` (ScreenshotTest), web with a full `UPDATE_SCREENSHOTS=1` run of the e2e project.

Changed or added, all looked at:
- **Android `form-guide-lat-pulldown-{dark,light}`** (new): the lat pulldown guide with its two panels ("Arms fully stretched" / "Bar to your upper chest"). The whole sheet fits.
- **Android `form-guide-{dark,light}`** (squat): the capture was cut off. The safety line at the bottom was clipped at 1370 dp since the picture was added in Task 4. The capture is now 1480 dp tall, so the whole guide including the safety block shows. The drawing is unchanged.
- **Web `form-guide-lat-pulldown-{dark,light}`** (new): the lat pulldown guide with its picture, at 375 px.
- **Web `exercise-guide-actions-{dark,light}`**: the Goblet squat guide now shows its picture ("Dumbbell against chest" / "Thighs just past level"). The committed PNG was older than the quads batch.
- **Web `exercises-muscles-chosen-{dark,light}`**: the Quads + Calves list now shows thumbnails for Barbell calf raise, Barbell lunge, Barbell reverse lunge and Bodyweight squat, where it had empty tiles before. These were stale since before those batches.
- **Web `exercise-day-chooser-{dark,light}`**: the thumbnails in the tab behind the scrim are now drawn (same reason as above).
- **Web `exercises-{dark,light}`**: only edge pixels of the thumbnails differ (2,712 pixels, max channel difference 53, 12 pixels over 40). This comes from the thumbnails now being worked out at build time (13a rounding). It looks the same.

Unchanged: Android `exercises-*` and `form-guide-picture-*`, and web `form-guide-*` and `form-guide-picture-*`. Their committed copies already showed the final pictures.

## 4. Pictures page
- `cd web && npm run figures:page` writes `docs/library/pictures.html`: 3,535,073 B (about 3.4 MB), 292,990 B gzipped. Running it twice gives the same file (same md5).
- `docs/library/pictures/` holds only the style reference files (bench draft SVG, example JPG and its brief). They are not tool output and were left as they are. The tool writes no other files, so nothing stale is left behind.
- Opened from `file://` in headless Chromium with the context offline, at 375×812 and 1280×900, in dark and light:
  - 225 cards, 225 unique names, in 9 sections: Main lifts 25, Chest 30, Back 33, Shoulders 24, Arms 31, Quads 28, Hamstrings & glutes 28, Calves 9, Core 17.
  - Each card has two panel SVGs, two non-blank captions, one thumbnail and its name.
  - No console errors or warnings, no page errors, and no requests apart from the file itself.
  - No horizontal overflow, and no clipped captions.
  - The page follows the system theme; the Light button switches the theme and sets `aria-pressed`.
  - Screenshots looked at: top of the page at 375 dark and 1280 light, and the Back section at 375 light.

## 5. Full suites
- `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest`: BUILD SUCCESSFUL. jvmTest 545, jsNodeTest 560, app 302; 0 failed, 0 skipped. The domain sync made no change.
- web: `npm test` 661 passed (41 files); `typecheck` clean; `build` ok (precache 19 entries, 1798.82 KiB).
- e2e: `npm run e2e -- --trace off --project e2e` 88 passed. `--project e2e` was added so the @speed project was not run, as the brief asks.
- No flakes this time. `./gradlew --stop` was run.

## Concerns
1. The `library.spec.ts:166` comment is untrue now (see 2). It is left for the controller.
2. Several web Exercises-tab PNGs were stale since earlier batches; they were refreshed here. The Android squat guide capture had been clipping its safety line, which the new height fixes.
3. The page is about 3.4 MB raw. That is fine for one file to send, but large for a phone over a slow connection; gzipped it is about 290 KB.
