# Task 13a report: thumbnails worked out at build time (web)

**Final status (after ruling R19): DONE.**
- Commit: edec171 `perf(web): thumbnails are worked out when the app is built`, one commit on 723b7be. Not pushed.
- The "After ruling R19" section at the end has the final results. The sections before it are the first round, as reported at the time.

**First-round status: NEEDS_CONTEXT.** The design as briefed (Ruling R15) is built and its unit tests pass, but the @speed gate fails with it in **0 of 5** quiet runs. On the same machine, in the same session, BASE passes **5 of 5**. As the brief says, I stopped there and did not fall back to another design.

**No commit was made.** The work is uncommitted in the working tree, on top of the new HEAD 723b7be (the controller's docs commit, not amended). The temporary timing spec is kept outside the repo, at /tmp/claude-0/-home-user-powertracker/530b1e32-ec0a-5578-ba2f-b77a61669676/scratchpad/tmp-timing.spec.ts.

No advisor model was available in this session, so the CLAUDE.md advisor step was not possible.

## Why the gate now fails

The idle warm-up did more than work out thumbnails. Its first piece was the **catalogue**: `library()`, `searchLibrary('', [], [])` and `hasFigure` for every row. The perf report had already noted that the picker "few hundred" check "now passes mainly because the catalogue is warm".

Cold, the catalogue costs about 30 ms in Node on this machine (`library()` 15 ms, then `searchLibrary` 15–18 ms), and more in the browser. With the warm-up gone, that cost lands in the picker's first open after a reload:

| Picker opened straight after load (temporary spec, `--repeat-each 10`, ms) | Values | Range |
|---|---|---|
| BASE (idle warm-up) | 79 59 73 60 63 54 61 44 64 39 | 39–79 |
| R15 as briefed (no warm-up at all) | 108 110 107 94 89 98 88 76 103 82 | 76–110 |
| R15 plus a temporary catalogue-only idle call (diagnosis only, reverted) | 88 69 65 56 81 56 60 70 59 56 | 56–88 |

| Exercises tab opened straight after load (same spec, ms; 16 tiles drawn) | Values | Range |
|---|---|---|
| BASE | 115 106 133 114 118 108 102 163 95 112 | 95–163 |
| R15 as briefed | 141 93 124 95 98 111 89 95 92 99 | 89–141 |
| R15 plus the catalogue idle call (diagnosis) | 100 75 104 73 91 86 81 76 67 78 | 67–104 |

The thumbnails themselves are fixed by this change: the tab never solves a pose. What is left of the tab's cold open is the same cold catalogue.

## @speed gate

Each run: `./gradlew --stop; pkill -f kotlin-daemon`, a 60 s wait, then `npx playwright test --project=speed --no-deps --trace off`. The checks run serially, so the first failure skips the checks after it.

| Run | BASE (723b7be code) | R15 as briefed |
|---|---|---|
| 1 | 4 passed | picker open failed (1 passed, the Exercises tab) |
| 2 | 4 passed | picker open failed (1 passed) |
| 3 | 4 passed | picker "few hundred" open 104 ms, failed (2 passed) |
| 4 | 4 passed | picker "few hundred" open 102 ms, failed (2 passed) |
| 5 | 4 passed | picker open failed (1 passed) |

- A separate rerun of the first picker check alone failed at 105 ms.
- An earlier set of 3 runs with no wait after stopping the daemons: 1 failed (picker open), then 4 passed twice.
- With the temporary catalogue idle call: 3 runs gave 4 passed, then a "few hundred" failure at 103 ms, then 4 passed. So warming the catalogue alone may not be enough for 5 consecutive passes either.
- The Exercises tab check passed in every run of R15.

## Options for the controller (not taken)

1. **Keep a single idle piece that warms the catalogue only** (`library()` + `searchLibrary('', [], [])`), with the thumbnails from build-time data as built here. This is the "competing with the first taps" pattern again, but a single piece of about 30 ms, and none per figure.
   - Measured: picker 56–88 ms, tab 67–104 ms, gate 2 of 3 consecutive runs.
   - It probably needs more to pass 5 runs in a row.
2. **Make the cold catalogue cheaper in the domain**, or ship the catalogue list or search index as build-time data too. This changes Kotlin or widens R15.
3. **Accept that the "few hundred" check is marginal** (about 50 ms of it is laying out 475 rows), which needs a ruling on the gate.

## What is built (uncommitted, in the working tree)

- **`web/plugins/figureThumbnails.ts`** (new): a Vite plugin providing `virtual:figure-thumbnails`.
  - It imports `src/domain/generated/powertracker-domain.mjs` straight into Node; no Vite is needed, since the generated bundle is plain ESM.
  - For each `grit.library()` entry with `figureInfo`, it stores the view box plus the encoded `figureShapes(name, thumbnail)`.
  - The module is `export default JSON.parse("<string literal>")`.
  - `addWatchFile` covers the domain bundle, and the domain is re-imported with `?v=<mtime>`, so a re-synced bundle is read afresh in dev.
  - It is used by `vite.config.ts` and `vitest.config.ts`. Nothing generated is committed.
- **`web/src/screens/library/thumbnailFormat.ts`** (new): the format, with `encodeShapes` and `decodeShapes`, shared by the plugin and the browser.
- **`web/src/screens/library/thumbnails.ts`** (new): `thumbnailOf(name)` decodes once per name, on first draw; `hasThumbnail(name)` is a key look-up.
- **`web/src/screens/library/figure-thumbnails.d.ts`** (new): the virtual module's type.
- **`FigureSvg.tsx`**: exports `Shape`, adds `ShapesSvg({ shapes, viewBox })` (the same elements, classes and order, cached per shape list), and drops `prepareDrawing`, which only the warm-up used. `FigureSvg` itself is unchanged.
- **`ExercisePicture.tsx`**:
  - `prepareThumbnails`, `whenIdle`, `Piece` and the constants are removed, along with the `library` import.
  - `ExerciseThumbnail` decides on `hasThumbnail` (the data's key set) and draws `ShapesSvg` from the data.
  - The IntersectionObserver near/far logic is kept unchanged: drawing all 225 tiles would put about 6,700 SVG elements on the page.
  - `ExercisePicture` (the guide sheet) still asks the domain on demand.
- **`main.tsx`**: the `prepareThumbnails()` call is removed.

**Empty-tile decision: the data's key set, not `grit.hasFigure`.**
- Rows always pass the canonical catalogue name (`entry.library?.name`, resolved in `pickerList`). The data is keyed by that same name, so the decision and the drawing come from one place, and the tab asks the domain nothing at all: `hasFigure` over 225 rows cost 8–10 ms cold in Node.
- A test pins that the key set equals the set of catalogue names for which `hasFigure` is true (all 225).

**Scripts:** `figure-sheet.mjs` and `pictures-page.mjs` load `FigureSvg.tsx` only, which does not import the virtual module, so they need no plugin.
- Checked: `npm run figures:sheet -- Bench Squat --label tmp13a` wrote both PNGs.
- `npm run figures:page` reported 225 of 225 drawn.
- I deleted both outputs afterwards; `docs/library/pictures.html` is untracked.

## Data format and sizes (225 figures)

The format is one flat list of whole numbers per figure: the view box width and height, then shapes, each a code (`kind * 6 + role index`) followed by its numbers in tenths:

| Kind | Numbers |
|---|---|
| circle | 3 |
| capsule | 5 |
| joined capsule (starts at the previous capsule's end, as a limb's segments do) | 3 |
| rect | 5 |
| polygon | count, then 2n |
| ring | 4 numbers in hundredths: centre and two radii |

A polygon is written as a ring only if it is the kit's 36-step ring and the regenerated points come back within the tolerance; anything else is written in full.

| | Raw | gzip -9 |
|---|---|---|
| Data string | 180,817 B (804 B per figure) | 51,850 B (230 B per figure) |
| Naive JSON objects at 0.1 (my measurement, 225 figures) | 762 KB | 124 KB |
| Controller's naive figure (145 figures) | 309 KB (2,131 B per figure) | 69 KB (476 B per figure) |
| Main bundle | 1,400,178 → 1,581,981 B (+181,803, +13%) | 383,680 → 438,265 B (+54,585) |
| Precache | 1621.17 → 1798.71 KiB (+177.5 KiB) | |

Shapes across the 225 thumbnails: 5,370 capsules, 623 circles, 219 rects and 527 polygons. 185 of the polygons are 74-point rings, written as 4 numbers each.

**Load choice: the main bundle.** A tile on screen must not be empty at first paint, and the parse cost is small.

**Start-up parse cost**, in Chromium (the preinstalled one):
- `JSON.parse` of the string: 0.49 ms on average over 20 runs.
- Evaluating the literal and parsing it: 0.5–2.4 ms over 10 runs.
- Decoding one figure on first draw is a few hundred numbers.

**Tolerance: every number within 0.05 view-box units** (half a tenth), plus float error.
- A view box is 400 units across; the 56×40 tile draws that in 56 px, so 0.05 units is 0.007 px.
- The contact sheets and the guide sheet draw through `FigureSvg` from the domain, not from this data, so they cannot show any difference at all.

## Tests

- **New: `thumbnailFormat.test.ts`, 4 tests.**
  - Every kind round-trips within the tolerance, and every encoded number is an integer.
  - A ring is written as 5 numbers; a bent ring, or a ring with another step count, is written in full.
  - A joined capsule is written without its start, and only straight after a capsule.
  - An unknown role or code is refused.
- **`ExercisePicture.test.tsx`:**
  - Removed the three warm-up tests: `..._thumbnailsArePreparedInIdleTime_...`, `..._withoutIdleCallbacks_usesTimers_...` and `..._aPieceThatFails_isSkipped_...`.
  - `exercisesTab_coldList_buildsOnlyTheNearTilesFigures` is replaced by `exercisesTab_coldList_drawsOnlyTheNearTiles_fromBuildTimeData_withoutTheDomain`. On a fresh domain, with all 225 catalogue rows plus an own exercise, only the near tiles are drawn (the own tile stays empty), and `figureInfo`, `figureShapes` and `hasFigure` are never called. Each drawn tile equals the domain's thumbnail pose in kinds and roles.
  - New: `exercisesTab_thumbnailData_isEveryFiguresThumbnailPose_asTheDomainDrawsIt`. The keys equal the catalogue names with figures (225). For every figure, the view box, kinds and roles are equal, and every number is within 0.05.
  - `exercisesTab_customExercise_showsEmptyTile`: the "no figure" half used to mock `figureInfo`, which the tile no longer asks. It now uses a name the data does not have.
- **RED:** both new files failed to resolve `./thumbnailFormat` and `virtual:figure-thumbnails`. **GREEN:** 17 of 17 in those two files after implementing.
- **Full vitest (after):** 40 files, 658 passed, 1 failed.
  - The failure is `exercisesTab_browseByMuscleAndSearchWork`, at 5066 ms.
  - At BASE in this session it also failed: 5137 ms in the full run, and 5357 ms alone.
- **Typecheck:** clean. **Build:** OK.
- **Not run:** `npm run e2e`, because I stopped at the gate. Gradle was skipped, since no Kotlin changed.

### Why `exercisesTab_browseByMuscleAndSearchWork` is still slow

Pose solving was a small part of it: alone, it went from 5357 to 5081 ms. I timed each step with temporary logs, since reverted:

| Step | ms |
|---|---|
| `showTab` (renders 226 rows, every thumbnail drawn) | 1015 |
| each `shownNames()` / `queryAllByRole('listitem')` | about 260–270 |
| each `getByRole` row or heading | about 260 |
| `search('calf')` | 600 |
| `search('')` | 505 |
| each chip click | 250–300 |
| `tick` | 500 |

- With thumbnails switched off (a temporary hack), the test takes 2153 ms.
- **Cause:** jsdom lays nothing out, so every row's box is 0×0 at the top, and every one of the 226 tiles counts as near and is drawn. That puts about 6,700 SVG shape elements in the DOM. Each Testing Library role query walks every element and works out its role and whether it is hidden.
- **Possible fixes, which I did not make:** model layout in that test so far tiles wait, or have the test query in a narrower way. Changing what the product draws in jsdom was not in the brief, and I left the timeout alone.

## Concerns

1. The gate regression above. A ruling is needed before this can be committed.
2. `exercisesTab_browseByMuscleAndSearchWork` fails on its 5 s timeout in full runs both before and after on this machine. It is not fixed by this change; the cause is given above.
3. The plugin solves all 225 poses each time a build, dev server or vitest run starts: about 0.9 s in Node.


## After ruling R19

### What changed since the first round

**The catalogue warm-up: `warmCatalogue()` in `web/src/state/exercisePicker.ts`, called from `main.tsx` after `app.render`.**
- It runs once, in one `setTimeout(0)` task after a `requestAnimationFrame`, so it starts right after the first paint. It is not chunked and does not wait for an idle callback.
- It warms `library()` (the picker's cached catalogue) and `grit.searchLibrary('', [], [])`.
- It does not warm `hasFigure`: the tab no longer asks it, because it decides from the thumbnail data's keys.
- It catches any throw, and its comment says it covers the catalogue only, never figures.

**New test file `web/src/state/warmCatalogue.test.ts`, 2 tests:**
- `warmCatalogue_runsOnceRightAfterTheFirstPaint_andBuildsNoFigure`:
  - nothing runs before or inside the frame; it runs in the task after;
  - `library` and `searchLibrary('', [], [])` are called once each, with no further frames or timers;
  - `figureInfo`, `figureShapes` and `hasFigure` are never called;
  - the picker's cache is warm afterwards.
- `warmCatalogue_aFailure_isSwallowed_andThePickerStillWorksItOutLater`.
- RED: `picker.warmCatalogue is not a function`. GREEN: 2 of 2.

**`exercisesTab_browseByMuscleAndSearchWork`: only its set-up changed.**
- The new `layOutLikeAPhone()` stubs `IntersectionObserver`, gives `.lib-scroll` a height of 700 px, and places each `.lib-row` 64 px apart in document order. Far tiles now wait, as they do in a browser.
- A comment says why. A sanity check pins that only fewer than 30 tiles drew.
- Every assertion about browsing by muscle and searching is unchanged, and so is the timeout.
- The file's `afterEach` now also calls `vi.unstubAllGlobals()`.
- Its time in the full run: **2346 ms**, down from 5137 ms at BASE (which failed on the timeout) and 5066 ms in the first round.

### Measurements, 20 runs each, quiet machine (Gradle stopped, 60 s wait)

The temporary spec ran with `--repeat-each 20`. BASE means 723b7be's code, measured from a `git stash`.

| Opened straight after load | Values (ms) | Median |
|---|---|---|
| Picker "few hundred", BASE | 65 62 48 81 74 85 70 60 63 58 64 45 75 88 47 49 55 58 55 55 | **61** |
| Picker "few hundred", new | 80 73 61 73 61 79 44 57 70 79 59 67 85 64 54 52 67 52 61 50 | **62.5** |
| Exercises tab, BASE | 105 90 95 115 143 130 105 121 103 90 103 93 105 122 89 117 108 95 85 106 | **105** |
| Exercises tab, new | 89 64 69 78 77 64 77 64 82 72 76 79 67 66 67 80 64 79 74 80 | **75** |

- The picker is at parity with BASE.
- The tab opened straight after load is about 30 ms faster at the median, and its worst run went from 143 to 89 ms: it no longer solves any pose.

### @speed gate

Each run used `npx playwright test --project=speed --no-deps --trace off`, after `./gradlew --stop; pkill -f kotlin-daemon` and a 60 s wait.

| Run | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| Result | 4 passed | 4 passed | 4 passed | 4 passed | 4 passed |

No picker check failed, so the 10-run failure-rate comparison and the profile of one picker open were not needed.

### Sizes (final)

| | Before (BASE) | After | Change |
|---|---|---|---|
| Main bundle, raw | 1,400,178 B | 1,582,093 B | +181,915 B (+13%) |
| Main bundle, gzip -9 | 383,680 B | 438,359 B | +54,679 B |
| Precache | 1621.17 KiB | 1798.82 KiB | +177.7 KiB |

The thumbnail data is 180,817 B raw and 51,850 B gzip. Start-up `JSON.parse` takes about 0.5 ms, as measured in the first round.

### Suites (final code)

| Suite | Result |
|---|---|
| vitest | 41 files, **661 passed** |
| typecheck | clean |
| build | OK |
| `npm run e2e -- --trace off` | **90 passed**; the reload flake did not occur |

Gradle was skipped, because no Kotlin changed. The plugin solving 225 poses at each build, dev-server start and vitest run (about 0.9 s) was accepted in R19, step 6.

### Files in edec171

- New:
  - `web/plugins/figureThumbnails.ts`
  - `web/src/screens/library/thumbnailFormat.ts`
  - `web/src/screens/library/thumbnailFormat.test.ts`
  - `web/src/screens/library/thumbnails.ts`
  - `web/src/screens/library/figure-thumbnails.d.ts`
  - `web/src/state/warmCatalogue.test.ts`
- Changed:
  - `web/src/main.tsx`
  - `web/src/screens/library/ExercisePicture.tsx`
  - `web/src/screens/library/ExercisePicture.test.tsx`
  - `web/src/screens/library/ExercisesScreen.test.tsx`
  - `web/src/screens/library/FigureSvg.tsx`
  - `web/src/state/exercisePicker.ts`
  - `web/vite.config.ts`
  - `web/vitest.config.ts`

The temporary timing spec was deleted, and no app/, Kotlin, e2e spec or playwright.config.ts change was made.

### Remaining concerns

1. The picker "few hundred" check keeps the margin it had at BASE (median about 61 ms). It is not worse, but it has no more headroom.
2. `ThumbnailFigure`'s data is keyed by catalogue name. A caller passing an alias would get an empty tile. Today the only caller, the Exercises tab, always passes `entry.library.name`, and a test pins that the keys match the catalogue names that have a figure.
