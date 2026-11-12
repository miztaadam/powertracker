# Task 3 report: web pictures and tools

**Status:** DONE_WITH_CONCERNS. One commit on 933006e: `feat(web): exercise pictures, contact sheet and pictures page tools`.

## What was built

- `web/src/screens/library/FigureSvg.tsx` — `<FigureSvg name pose viewBox />`: one pose as an inline `<svg class="fig" viewBox="0 0 400 300" aria-hidden>`. Shapes are drawn in the facade's list order: circle → `<circle>`, capsule → round-capped `<line>` of the capsule's width, rect → `<rect rx>`, polygon → `<polygon fill-rule="nonzero">`. Each shape carries only a role class (`fig-body`, `fig-body-far`, `fig-equipment`, `fig-accent`, `fig-accent-fill`, `fig-gap`) and paints with `currentColor`. The built elements are memoised per name and pose (module map).
- `web/src/screens/library/figure.css` — the role → token mapping (the only place colours are decided):
  BODY `var(--text)`; BODY_FAR `color-mix(in srgb, var(--text) 55%, var(--fig-surface, var(--surface)))`; EQUIPMENT `var(--text-muted)`; ACCENT `var(--accent)`; ACCENT_FILL `var(--accent)` at `fill-opacity: .10` (and `stroke-opacity`, for a capsule); GAP `var(--fig-surface, var(--surface))`. `forced-color-adjust: none` under forced colours, so gaps and roles are not all forced to one colour.
- `web/src/screens/library/ExercisePicture.tsx`
  - `<ExercisePicture name />`: `<figure class="lib-picture" role="img" aria-label="Picture: <start>, then <end>.">`, two panels in a two-equal-column grid (8 px gap), a `body-small` caption under each (`--text-secondary`, centred, wraps, `aria-hidden` so it is not read twice). Renders nothing when `grit.figureInfo(name)` is null.
  - `<ExerciseThumbnail name? />`: a 56 × 40 `aria-hidden` tile in `--container-high` with 6 px corners; the figure's `thumbnail` pose is drawn on it 4:3 and centred, with `--fig-surface` set to the tile so GAP and BODY_FAR use the tile colour (as Android). Empty tile with no name (the lifter's own) or no figure (R6).
  - `pictureLabel(figure)`.
  - Thumbnails are drawn only for rows on or near the screen (within 600 px), then kept: checked before first paint by the row's box, otherwise one shared `IntersectionObserver` on the row. See "Design choices".
- `FormGuideSheet.tsx`: `<ExercisePicture name />` first inside the guide, i.e. under the header (name, area · equipment) and above the purpose. Custom exercises unchanged (`NO_GUIDE`).
- `ExerciseBrowser.tsx`: `thumbnails?: boolean` (default false); a row's button leads with the thumbnail, by the catalogue name. `ExercisesScreen.tsx` passes `thumbnails`; the picker passes nothing.
- `library.css`: `.lib-picture`, `.lib-picture-panel`, `.lib-picture .fig`, `.lib-picture-caption`, `.lib-thumb`, `.lib-thumb .fig`. Row height unchanged (64 px min; the tile is 40).
- `web/scripts/figure-sheet.mjs` (`npm run figures:sheet -- [--label <name>] [--columns <n>] <all | area | names…>`): writes `web/.figure-sheets/<label>-dark.png` and `-light.png`. Six figures to a row by default; each has its name above, its thumbnail at real size (56 × 40) beside the name, two 216 × 162 panels and the captions under them. Area by id or label, any case (`chest`, `main-lifts`, `"Hamstrings & glutes"`); names by name, alias or former name (deduplicated). `all` and areas skip exercises with no figure and say how many; a name with no figure, or an unknown argument, fails with exit 1. Default label: `all`, the area id (`main-lifts`), else `figures`. 2× pixels up to 12 rows, 1× beyond.
- `web/scripts/pictures-page.mjs` (`npm run figures:page`): writes self-contained `docs/library/pictures.html` — inline SVG, grouped by body area with a contents list and counts, Dark/Light buttons (follows the system until one is pressed), the thumbnail beside each name, and a "Not drawn yet" line per area while the catalogue is incomplete. No timestamp, so re-running gives the same file.
- Both tools draw with the app's own `FigureSvg` (loaded through Vite SSR + `renderToStaticMarkup`) and colour with the app's own `tokens.css` and `figure.css`, so a sheet shows exactly what the app shows.
- `package.json` scripts `figures:sheet`, `figures:page`; `.gitignore` gets `web/.figure-sheets/`.
- Screenshots (`docs/design/web/screens/`): new `form-guide-picture-{dark,light}.png` (Bench guide, new test in `screenshots.spec.ts`); re-rendered because the tab now has thumbnails: `exercises-*`, `exercises-muscles-chosen-*`, `exercise-day-chooser-*`. No other PNG changed in a full `UPDATE_SCREENSHOTS=1` run.

## Tests

`web/src/screens/library/ExercisePicture.test.tsx` (11): the ten names from Task 2 — guideSheet_showsTwoPosePicture_withCaptions, guideSheet_pictureLabel_readsBothCaptions, guideSheet_noFigure_showsNoPictureArea (mock of `grit.figureInfo`, R2, plus a custom exercise), exercisesTab_rowsShowThumbnail, exercisesTab_customExercise_showsEmptyTile (own exercise, and a catalogue exercise with no figure, R6), picker_hasNoThumbnails, picture_noOverflow_atPhoneWidth, picture_contrast_darkAndLight, picture_largeText_captionsWrap, picture_followsThemeChange — plus exercisesTab_thumbnailFarOffScreen_isDrawnWhenItsRowComesNear.

`web/e2e/pictures.spec.ts` (10 + 1 @speed): the same ten names, measured in Chromium at 375 px: panel geometry (side by side, equal, 4:3), captions under their panel and above the purpose, one `img` in the accessibility tree, thumbnail 56 × 40 and 12 px left of the name, row targets ≥ 48, empty tile aligned with a drawn one, no overflow at 375 px, computed role colours = tokens and ≥ 3:1 on the sheet and on the tile in dark and light, captions wrapping at 200% (a 187 px wide layout) without being cut, and recolouring under an open sheet when the colour scheme flips (same DOM node). The `@speed` test: the Exercises tab with thumbnails settles within 100 ms on opening, on 11 keystrokes and on 7 filter changes.

Why two files under the same names: jsdom lays nothing out and resolves no CSS variables. The vitest versions of the four `picture_*` tests check what jsdom can (no sizes or colours written into the drawing; the CSS rules; contrast computed from tokens.css); the e2e versions measure the real thing.

- **RED:** `npx vitest run src/screens/library/ExercisePicture.test.tsx` → `Failed to resolve import "./ExercisePicture"`.
- **GREEN:** 11/11 vitest; pictures.spec 10/10 + speed.
- **Full suites (once, before the commit):**
  - `./gradlew :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest` → BUILD SUCCESSFUL (all test tasks up to date: nothing in domain/ or app/ changed since Task 2). Results on disk: domain JVM 242 (1 skipped), domain JS 258 (1 skipped), app 301, 0 failures.
  - web: `npm test` 653 passed (was 642, +11); `npm run typecheck` clean; `npm run build` ok; `npm run e2e -- --trace off` 89 passed (was 76: +10 pictures, +1 speed, +2 screenshot tests). Two earlier full runs each had one speed failure, both explained and fixed in Concerns 1.

## Files

New: `web/src/screens/library/{FigureSvg.tsx,ExercisePicture.tsx,ExercisePicture.test.tsx,figure.css}`, `web/e2e/pictures.spec.ts`, `web/scripts/{figure-sheet.mjs,pictures-page.mjs}`, `docs/design/web/screens/form-guide-picture-{dark,light}.png`.
Modified: `FormGuideSheet.tsx`, `ExerciseBrowser.tsx`, `ExercisesScreen.tsx`, `library.css`, `web/package.json`, `web/playwright.config.ts`, `.gitignore`, `web/e2e/screenshots.spec.ts`, six Exercises-tab PNGs.

## Design choices

- Matches Android (Task 2): tile `--container-high` (= surfaceContainerHigh) with `--radius-xs` 6 px, figure drawn on the tile with GAP = tile; guide panels with no frame or background, GAP = sheet `--surface`; captions `--text-secondary` (= onSurfaceVariant) in body-small; 4 px panel-to-caption; 8 px between panels; 12 px thumbnail-to-name; 12 px above the picture (8 px header padding + 4 px margin).
- BODY_FAR mixes toward `--fig-surface` when a parent sets it (the tile) and toward `--surface` otherwise, so the brief's value holds verbatim on the sheet and the tile matches Android's `lerp(onSurface, tile, .45)`.
- **Lazy thumbnails.** The web list renders all rows (it relies on `content-visibility: auto`, which skips layout and paint but not DOM creation). Measured in a temporary experiment with every one of the 225 rows given the Bench figure (not committed): drawing all 225 eagerly made the tab open in 84–104 ms (8,325 SVG elements); drawing only those near the screen 51–82 ms; no thumbnails 39–49 ms. So off-screen tiles stay empty until their row is within 600 px. The check reads the row's box, not the tile's: asking for the box of something inside a skipped `content-visibility` row forces the browser to lay every row out (first version: 70–101 ms).
- Captions are `<span>`, not `<p>`, and `aria-hidden` inside the `role="img"` figure: the label already reads both.

## Deviations from the brief

1. **One extra file, `figure.css`.** The brief lists `library.css` for the styles. Layout is in `library.css`; the role colours are in their own small file so the two tools can include exactly those rules (and nothing else of the app's CSS) and can never drift from the app.
2. **`--columns <n>`** on `figures:sheet` (default 6, as the brief). A six-across sheet is about 2,850 CSS px wide; a reviewer whose viewer scales images down will see small panels. `--columns 3` (or sheets of a few names) gives larger ones. Also added: the thumbnail on the sheet and on the page, since "reads clearly at thumbnail size" is one of the review points.
3. **`docs/library/pictures.html` is not committed.** The tool was run and checked (375 px and 900 px, both themes, no console errors), then the file was removed: with one figure it would be stale as soon as Task 4 lands. The final task generates it.
4. **Screenshots:** only Bench exists, so the spec's squat and lat pulldown guide screenshots are left for the final task (same as Android).
5. The extra unit test for the off-screen thumbnail path (the lazy path has no natural e2e case while Bench, near the top, is the only figure).

## Concerns

1. **The speed checks were made to run on one worker (`web/playwright.config.ts`, not in the brief's file list).** With a second file holding a `@speed` test, Playwright ran the two files side by side on two workers, and the existing picker check "a few hundred exercises" then failed every time (103–123 ms; 3/3 passes on the base commit, 0/4 with both files in parallel). The `speed` project now has `workers: 1`; after that the whole speed project passed 4/4 and the full suite 89/89. Separately, the first version of the new check started timing while the app was still launching (service worker precache) and measured 105 and 147 ms for the open step; it now waits for the Today screen, `serviceWorker.ready` and network idle first (then 6/6 alone; a typical open is 48–60 ms). Headroom will shrink as figures arrive: with all 225 present the open step measured 51–82 ms in the experiment. If it gets flaky by the final task, the options are a smaller near-screen margin than 600 px, or drawing only the first screenful before paint.
2. **The speed check cannot yet exercise 225 real thumbnails** — only Bench has a figure. The numbers above come from the temporary experiment, not from a committed test. Worth re-running the check's timings after a few batches.
3. **Thumbnail legibility** (same as Task 2's concern 3): at 53 × 40 the Bench reads, but the plate ring and J-hooks are hairlines. The contact sheet now shows the thumbnail at real size for the Task 4 review.
4. The Gradle suites reported UP-TO-DATE rather than re-executing, because nothing they depend on changed. The counts are from the result files of the Task 2 run.
5. The controller added untracked `.claude/` files and `CLAUDE.md` during this task; they are not staged or committed here, so `git status` shows them as untracked.

## Rulings used

R1 (tests use "Bench"), R2 (no-figure via mocked `grit.figureInfo`), R3 (`--label`), R6 (empty tile for catalogue exercises without a figure).

---

## Fix round 1 (review-3-report.md)

One new commit on 288ce94: `fix(web): lazy thumbnails observe the scrolling list`.

- **I1 (fixed).** The reviewer was right: the shared `IntersectionObserver` watched against the screen, and `.lib-scroll` clips its rows before the screen does, so the 600 px margin never applied and a thumbnail was only drawn once its row was in view.
  - `ExercisePicture.tsx`: a row is now watched against the list it scrolls in (`placed.closest('.lib-scroll')`, or the screen for a tile in no list), with one observer per list kept in a `WeakMap`. The check made before first paint measures from the same list's edges instead of the window's.
  - Proved in a real browser, new e2e test `exercisesTab_thumbnailFarOffScreen_isDrawnWhenItsRowComesNear` in `pictures.spec.ts`: forty own exercises push Bench far down the list; at 900 px below the list its tile is empty, at 400 px below (still out of sight) it is drawn, and it stays drawn after scrolling back to the top.
  - RED before the fix: at 400 px below the list `drawn` was false. GREEN after.
  - Needed no stub of `grit.figureInfo`: a search that leaves Bench out and is cleared again remounts its row where it belongs. (On first load the catalogue is listed before the lifter's own exercises are read, so Bench starts near the top and is drawn at once.)
- **M2 (fixed).** The unit test now asserts the observer's options are exactly `{ root: <the .lib-scroll list>, rootMargin: '600px 0px' }`, and that near is measured from the list's edges (a row 550 px under the list is drawn at once, one 650 px under waits).
- **M5 (done with I1).** The `watcher!` non-null assertions are gone.
- M1, M3, M4, M6: left, as instructed.

**Tests:** web unit 653 passed; typecheck clean; e2e `--trace off` 90 passed (was 89, +1); speed project alone 3/3 after the fix. Gradle suites not re-run (web-only change).

**Note for M1:** with the margin now working, a few more tiles are built ahead while scrolling. Opening is unchanged in the speed check (only rows within 600 px of the list at mount were ever drawn before paint), but the timings are still worth re-measuring once a batch of figures exists.
