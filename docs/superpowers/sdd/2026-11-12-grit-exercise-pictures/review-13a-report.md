# Review: Task 13a, thumbnails worked out at build time (edec171 on 723b7be)

Verdict: Ready. Critical 0 / Important 0 / Minor 4.

The repo was not touched (git status clean, HEAD edec171). Every experiment ran in copies under
/tmp/claude-0/-home-user-powertracker/530b1e32-ec0a-5578-ba2f-b77a61669676/scratchpad/r13a/{new,base}.

## Findings (all Minor)

1. A running dev server does not pick up a changed domain bundle. web/plugins/figureThumbnails.ts:28 and :47-48.
   - In the new copy I ran `vite --port 5199`, fetched `/@id/__x00__virtual:figure-thumbnails` (md5 ff84...), changed the
     head radius in powertracker-domain.mjs line 7346 from 10.0 to 12.0, waited, and fetched again. The md5 was unchanged.
     I reverted the number and touched the file: still unchanged. An ordinary source edit (thumbnails.ts) was served
     fresh at once, so the file watcher works.
   - A fresh `vite` start after the edit gave a different md5 (fc24...). `vite build` and vitest also read the new number.
   - Why: `this.addWatchFile` in `load` does not link the bundle to the virtual module in the dev module graph.
     The "?v=mtime" re-import only helps when `load` runs again, and it does not.
   - The comment at line 28 reads as if a running server copes. Effect: in dev, after `:domain:syncWebDomain`, the guide
     sheet (which uses the domain directly) updates but the Exercises tab thumbnails stay old until the dev server is restarted.
     Never stale in a build or in tests.
   - Fix: in `configureServer`, watch DOMAIN, then `server.moduleGraph.getModuleById(RESOLVED)` and
     `invalidateModule` it, and send a full reload. Or say in the comment that dev needs a restart.
2. No unit test of the plugin itself (brief item 8 asked for one). There is no web/plugins/*.test.ts.
   `thumbnailFormat.test.ts` covers the format, and the data test in ExercisePicture.test.tsx exercises the plugin end
   to end (key set equals the 225 catalogue names, every shape equals the domain's). So behaviour is pinned, but
   `figureThumbnails()` has no test of its own. It also cannot be imported in the default jsdom environment
   (`import.meta.url` fails: "The URL must be of scheme file"), so a test would need `// @vitest-environment node`.
3. `npm run figures:sheet` and `figures:page` draw their thumbnails from the domain through `FigureSvg`, not from the
   shipped data. Sheets and page from the base copy and the new copy are byte-identical (`cmp`), which proves only that
   `FigureSvg` is unchanged. Nothing the owner reads shows the shipped thumbnails; only the data test pins them.
   Not a defect of this commit, but worth knowing. My own render of the shipped data is in point 3 below.
4. Rounding is not invisible at pixel level, only small (see "Same picture"). The report's "0.007 px, below anything a
   screen can show" is true of the geometry; anti-aliasing of thin outline strokes still turns it into some pixels
   differing. Chromium does the same for a 0.001-unit shift (max 41/255 at tile size), so this is edge noise, not an
   encoding error. No fix needed; the claim could be worded softer.

## 1. Design: as decided

- Data comes from the plugin (`virtual:figure-thumbnails`), which imports the generated domain bundle in Node and calls
  `library()`, `figureInfo` and `figureShapes` for each entry. The module is `export default JSON.parse("<literal>")`.
- The same plugin is in vite.config.ts and vitest.config.ts, so build, dev and tests share it.
- Nothing generated is committed. `git ls-files` shows only the plugin, the .d.ts, the format and the tests. The domain bundle itself was already tracked.
- No pose is solved in the browser for a thumbnail. The test spies on `figureInfo`, `figureShapes` and `hasFigure` and sees none called.
- Leftover search: `prepareThumbnails`, `prepareDrawing`, `whenIdle`, `Piece`, `CATALOGUE_NEEDS`, `FIGURE_NEEDS`,
  `requestIdleCallback` and `cancelIdleCallback` appear nowhere in web/src, e2e, scripts, plugins or docs (outside the sdd notes).
  The one remaining word "idle" is the `warmCatalogue` comment saying it is not an idle callback.
- `warmCatalogue()` (web/src/state/exercisePicker.ts:50) is exactly what the ruling says: one `requestAnimationFrame`, then one
  `setTimeout(0)`, then `library()` and `grit.searchLibrary('', [], [])` in a try/catch. It touches no figure. It has one
  caller, main.tsx:31, on the normal (non-showcase) path only.
- warmCatalogue.test.ts mutations in my copy:
  - an added `grit.figureInfo('Bench')` made the test fail ("Mock expected to not be called");
  - a second `searchLibrary` made it fail ("called 1 times, got 2");
  - dropping the `requestAnimationFrame` (timer only) made both tests fail;
  - a stray second `setTimeout(() => library(), 5)` was NOT caught, but `library()` is cached, so it does nothing and the mutation is inert.

## 2. Cannot go stale

- Build: bundle changed with the number (1,582,093 -> 1,582,140 B) with no manual step.
- vitest: with the head radius at 12.0, the data test passed, so tests read the same fresh bundle.
- Fresh dev start: new data. Running dev server: see Minor 1.

## 3. Same picture

- The data test (ExercisePicture.test.tsx:297-323) loops over all 225 catalogue figures. For each it compares the view box,
  the list of "kind role" strings (so a dropped, added or re-roled shape fails), the count of numbers per shape, and then
  the largest number difference against 0.05 + 1e-9. The tolerance is half a tenth, which is what rounding to tenths can cost.
- Mutations (applied in the plugin copy to Squat only, one test run each):
  - drop a middle shape: FAIL (shape list differs);
  - drop the last shape: FAIL;
  - change a role: FAIL;
  - move a capsule x by 0.1: FAIL (worst 0.08 > 0.05);
  - move a polygon point by 0.2: FAIL;
  - swap a capsule's x1 and y1: FAIL;
  - move x by 0.04: also FAIL, because 0.04 plus up to 0.05 of rounding is over the limit. So the test is tight.
  - No mutation: PASS.
- Contact sheet `npm run figures:sheet -- --label r13a all` (225 figures), dark and light, base and new: byte-identical PNGs
  (`cmp`), because the sheet does not use the shipped data (Minor 3).
- So I rendered the shipped data myself (copy only, scripts/cmp-thumbs.mjs): all 225 thumbnails through
  `ShapesSvg(thumbnailOf(name))` against `FigureSvg(name, thumbnail pose)`, in the app's own role CSS and theme tokens,
  in Chromium, dark and light, in a 15 x 15 grid.
  - At the real tile size (56 x 40, 1x): largest channel difference 63/255 (dark) and 65/255 (light); about 7% of pixels
    differ by at least 1 level; 406 and 416 of 504,000 pixels (0.08%) differ by more than 16; at most 4 pixels by more than 64.
  - At 2x: largest 98 (dark) and 100 (light).
  - At 400 x 300 per tile (7 times the app size): largest 134 and 136.
  - Looked at figure 152 (Front squat) side by side with the difference mask: identical to the eye; differences are
    one-pixel edges on thin outlines.
  - Control: the domain's own markup with every number shifted by 0.001 gives a largest difference of 41 (tile) and 96 (400 x 300).
    So these differences are the renderer's edge sensitivity, not an encoding fault.
- Theme: both dark and light renders use role classes (`fig-*`) from figure.css, not baked colours, and look right.

## 4. Behaviour kept

- Tests that run and pass: near tile drawn at first paint; far tile drawn when its row comes near; picker has no thumbnails;
  own exercise shows an empty tile; a name with no figure and no name at all draw nothing; guide sheet unchanged; theme change.
- Mutations: `useState(true)` for `near` fails 3 tests (the far-tile test, the cold-list test and `browseByMuscleAndSearchWork`'s sanity check);
  a `grit.figureShapes` call added to `ThumbnailFigure` fails the cold-list test;
  `grit.hasFigure(name) &&` added to the tile fails the cold-list test.
- Far rows still mount their drawing lazily (the IntersectionObserver logic is untouched; the diff to ExercisePicture.tsx removes only the warm-up).
- The tile is still `aria-hidden="true"` (ExercisePicture.tsx:113 area, unchanged).

## 5. Rewritten tests

- Cold-list test: `exercisesTab_coldList_drawsOnlyTheNearTiles_fromBuildTimeData_withoutTheDomain`. It pins what is now true:
  225 catalogue rows plus an own one in a 64 px row list; each tile's child count equals "near and not the own one";
  none of `figureInfo`, `figureShapes`, `hasFigure` is called; each drawn tile equals the domain's thumbnail pose.
  - What used to be pinned and is now not: "every row asks hasFigure" (no longer true, the tile uses the data's keys) and
    the ordering with leading figured rows (a fixture detail). The deliberate gain is that a far tile is never drawn.
    Nothing real is lost.
  - The "no figure" half of `exercisesTab_customExercise_showsEmptyTile` used to mock `figureInfo`; it now uses a name the data does not have. Fair.
- `exercisesTab_browseByMuscleAndSearchWork`: the diff to that test body only adds the `layOutLikeAPhone()` call and a sanity
  check (more than 0 and fewer than 30 drawn tiles). Every original assertion is unchanged and there is no timeout argument.
  - The helper stubs an `IntersectionObserver` that reports nothing, gives `.lib-scroll` a 700 px box and each `.lib-row`
    a 64 px step in document order. It does not switch thumbnails off: near rows draw and far ones wait. The test looks at row names and headings, not at tiles. `unstubAllGlobals` was added to afterEach.
  - Time: 2337 ms in the new copy (file alone), 4295 ms in the base copy.
- Callers of the thumbnail component: only `ExerciseBrowser.tsx:238`, `<ExerciseThumbnail name={entry.library?.name} />`.
  - `entry.library` is set only by `catalogueEntry`, from a `JsLibraryExercise` taken from `library()`, `searchLibrary`,
    `exercisesByMuscle(s)`, or `findInLibrary` in `createCustomExercise`. All are canonical catalogue names, never an alias.
  - A renamed built-in (a lifter's row with a non-catalogue name) is an `ownEntry` with `library: null`, so no name, and an
    empty tile. At base it was also an empty tile (`hasFigure(undefined)` guard). So there is no change; an alias cannot get an empty tile through this path.

## 6. Size and start-up (checked against my own builds)

| | Base | New | Change |
|---|---|---|---|
| Main bundle, raw | 1,400,178 B | 1,582,093 B | +181,915 (+13%) |
| Main bundle, gzip -9 | 383,680 B | 438,359 B | +54,679 |
| Precache (PWA) | 1621.17 KiB | 1798.82 KiB | +177.65 KiB |
| Data string (225 figures) | | 180,817 B raw, 51,850 B gzip -9 | 804 / 230 B per figure |

- These match the report to the byte. (Vite's own gzip line says 439.50 kB, the report's figure is gzip -9; both are fine.)
- Format: flat whole-number list per figure, shape code = kind * 6 + role, tenths, joined capsules, plate rings as 4 numbers.
  Sensible and well documented in thumbnailFormat.ts, with a round-trip test. 5 times smaller than naive JSON (762 KB raw).
- Parse cost: the report's 0.5 ms is measured (in Chromium). I did not re-run Chromium; in Node I got 0.62 ms average over 50 `JSON.parse` calls of the same string. Decoding is lazy, per figure, on first draw. Not measured: the cost of parsing the 13% larger script itself on a phone (small).
- Build-time cost: the plugin solves 225 poses in about 2.1 s here in a vitest worker (the report said about 0.9 s in plain Node); accepted by R19.

## 7. Scripts

- `npm run figures:sheet -- Bench Squat --label tiny`: works. `--label r13a all`: works (225 figures, two PNGs).
- `npm run figures:page` in the copy: "225 of 225 exercises drawn", writes outside the repo; output byte-identical to the base copy's.
  Neither script needs the virtual module.

## 8. Quality

- Plugin: 53 lines, typed, readable, comment explains it. No model names in the diff; the commit trailer carries the attribution lines. No change to the speed tests, playwright.config.ts, e2e, Kotlin or app/ (`git diff --stat` shows none).
- tsconfig does not include plugins/ directly, but vitest.config.ts imports it, so `tsc` checks it; typecheck is clean.

## 9. Tests (in the new copy, node_modules linked)

- `npm test`: 41 files, 661 passed, 0 failed (52 s).
- `npm run typecheck`: clean.
- `npm run build`: OK. e2e and speed tests not run, as told.

## Not checked

- The 4 @speed checks and the e2e suite (controller's).
- Chromium parse time of the data (only Node, 0.62 ms).
- That warm-up falls after React's first render in a real browser (rAF then timeout; the report's 5 speed runs say yes; not re-measured).
