# Perf report: Exercises tab opens too slowly with thumbnails (web)

**Status:** DONE_WITH_CONCERNS
**Commit:** 2405ad0 `perf(web): exercises tab thumbnails stay within the speed budget`, one new commit on 285a275 (not pushed, branch unchanged).

## Profile (at 285a275, before the change)

How I measured:
- I used a scratch Playwright script against `vite preview` of the production build. It has the same iPhone 13 device and fixed clock as the e2e tests, and the same wait before opening: Today up, service worker ready, network idle.
- I timed the click on the Exercises tab plus two frames, as the @speed check does.
- I also took CDP CPU profiles of the open and node timings of the generated domain module.

| Case | Open time (ms) | Notes |
|---|---|---|
| As shipped | 115–181 (5 runs: 152 115 149 181 138) | 16 thumbnails drawn |
| No thumbnails at all (temporary hack) | 62–79 (77 68 62 68 79) | This is the tab's own cost. It leaves about 25–35 ms of the 100 ms budget for thumbnails. |
| Domain caches warmed by hand before opening | 65–75 | Thumbnails cost about nothing once worked out. Building the DOM for 16 SVGs is not the problem. |

Where the time went, on a cold open:
- **First `grit.figureInfo` call: 50–68 ms** (node 48–57 ms; CPU profile 62 ms). The first look-up forces `Figures.all`, which builds every figure of every area and runs their pose solvers.
  - Leg press alone is about 28 ms (`sledFor` / `ankleOn`).
  - The 25 main-lift figures take about 22 ms.
- **Cold `figureShapes`:** about 1.2 ms median and up to 6 ms per figure in node. That is about 30 ms in the browser profile for the 16 drawn.
- **Cold catalogue (`library` + `searchLibrary`):** about 30 ms on its first use. This was also the main reason the picker "few hundred exercises" @speed check sat at 93–109 ms and failed 3 of 5 runs at BASE:
  - its open was about 25 ms of cold domain work;
  - most of the rest is laying out 475 rows.

## What changed and why

The fix is idle-time warm-up: the cheapest change, and it meets the target with headroom.

- `web/src/screens/library/ExercisePicture.tsx` gains a new `prepareThumbnails()`. It works through a queue of pieces in `requestIdleCallback` slices, falling back to 1 ms timers with 8 ms slices where there are no idle callbacks (Safari). The pieces, in order:
  1. the catalogue (`library()`, which the picker's cache also uses);
  2. its list order;
  3. the figure look-up, which builds them all;
  4. then one piece per thumbnail, in list order: `prepareDrawing(name, thumbnailPose)`.
- Each slice does one piece, then as many more as `timeRemaining()` allows, and returns a stop function. Anything a screen needs before warm-up reaches it is still worked out on demand, into the same cache, exactly as before.
- `web/src/screens/library/FigureSvg.tsx` gains `prepareDrawing(name, pose)`, which fills the existing drawing cache.
- `web/src/main.tsx` calls `prepareThumbnails()` after `app.render`. This is the app path only, not the dev showcase.
- Rendering is unchanged:
  - on-screen tiles are still drawn in the layout effect before first paint;
  - near tiles are still drawn at once;
  - far tiles still wait for the IntersectionObserver;
  - theme, accessibility and the picker are untouched.
- No Kotlin changes; the geometry and the fingerprint are untouched.

Rejected along the way, on evidence:
- **A "piece needs N ms of idle time" rule**, meant to keep the one heavy piece out of the gap between two frames. Playwright's clock fakes `requestIdleCallback` with `timeRemaining()` always 0, so the warm-up never ran under test: the tab went back to 117–154 ms. Reverted.
- **Deferring near-but-off-screen tiles to idle time.** Not needed once warm: allwarm and noThumbs cost the same. It would also have broken the pinned unit behaviour "a row 550 px under the list is drawn at once".

## Before and after (quiet runs, Gradle stopped)

All runs used `npx playwright test --project=speed --no-deps --trace off`. The open times came from temporary `console.log` lines added to the spec files, which I reverted; the specs, threshold and config are unchanged.

**Before (285a275):** 3 runs, each 2 failed.

| Check | Run 1 | Run 2 | Run 3 |
|---|---|---|---|
| Exercises tab open | 117 | 178 | 126 |
| Picker, few hundred, open | failed | 109 | 106 |

**After, instrumented:** all 4 checks passed in each of the 3 runs.

| Check (open / worst step, ms) | Run 1 | Run 2 | Run 3 |
|---|---|---|---|
| Picker | 71 / 71 | 57 / 57 | 69 / 69 |
| Picker, few hundred | 55 / 58 | 77 / 77 | 75 / 75 |
| Browse by muscle | 60 / 60 | 57 / 57 | 51 / 51 |
| Exercises tab | 63 / 63 | 54 / 54 | 64 / 64 |

**After, the plain command:** 3 more consecutive runs, 4 passed each time.

## 225-scale measurement

How I made it:
- A temporary local hack, never committed and reverted, remapped every catalogue row's thumbnail to one of the 31 existing figures, cycling through them. All 225 rows then have a figure.
- The page exposed `grit` on `window` so the script could build the map.
- Same harness and same waits as above, 5 runs each.

Results:
- **Open:** 51–85 ms (55 53 85 52 51).
- **Filter changes with every row figured** (Chest / Back / Arms / All): 28–60 ms, with 16–32 thumbnails drawn.
- **Shipped data, same harness:** 48–62 ms. No thumbnails at all: 41–68 ms.

The open cost no longer grows with the number of figures on screen. With every row figured, 16 rows are on screen or near, which is about the same as now.

Limitation: the hack reuses 31 distinct figures. With 225 distinct figures, the extra cold work lands in the idle warm-up, not in the open; see concern 1.

## Tests

- **New vitest tests** in `ExercisePicture.test.tsx`:
  - `exercisesTab_thumbnailsArePreparedInIdleTime_soTheTabOpensWithoutWorkingThemOut`: the warm-up does nothing until idle and at least one piece per slice. It works out every figured catalogue entry's thumbnail pose exactly once, in list order. Afterwards a rendered thumbnail calls `figureShapes` zero times.
  - `exercisesTab_thumbnailPreparation_withoutIdleCallbacks_usesTimers_andCanBeStopped`.
- **RED/GREEN:** both failed first with `prepareThumbnails is not a function`, then passed. The "at least one piece, no domain calls before idle" assertions were also checked against an altered loop.
- **Web suites:** vitest 39 files / 655 tests passed; typecheck clean; build OK; `npm run e2e -- --trace off`: 90 passed. This was the full run on the final code; an earlier full run on the first version also gave 90 passed. The reload.spec flake did not occur.
- **Gradle:** I skipped `:domain:allTests :domain:checkWebDomain :app:testDebugUnitTest`, because no Kotlin or app files changed.

## Concerns

1. **One unsplittable piece that grows with the figure count.**
   - The domain's first figure look-up builds every figure at once: about 50–68 ms now with 31 figures. It will likely be a few hundred ms with 225 (Leg press alone is 28 ms).
   - It now runs in one idle slice after start-up, not in the tab's open. But it is a main-thread block: a tap landing during it waits.
   - In a loaded run (load average 6, not quiet) the picker "few hundred" check hit 103 ms once. That run opens right after a reload, so the piece can fall between its frames.
   - The real fix is per-figure laziness in `Figures`: for example, area maps of `"Name" to ::figureVal` as `() -> ExerciseFigure`, with `byName` forcing only that figure. That is pure performance with no output change, but it changes the area-file authoring pattern, so I left it for a decision. I suggest doing it before many more figures land.
2. **A tab opened before the warm-up finishes still pays the cold cost on first render** (for example, launching straight to `#/exercises`). This is the same as before, with no regression.
3. **The warm-up costs idle CPU at every launch**, even for lifters who never open the tab. It works out only the thumbnail pose, about 1–3 ms per figure, in slices.
4. **The picker "few hundred" check was already marginal at BASE** (93–109 ms). It now passes mainly because the catalogue is warm. Laying out 475 rows still costs about 50 ms.

## Lazy figures

**Commit:** 178d6a6 `perf(domain): figures are built on first use`, a new commit on top of 2405ad0. 2405ad0 was not amended.

### What changed

**The area files** (Arms, Back, Calves, Chest, Core, HamstringsGlutes, MainLifts, Quads, Shoulders):
- Each exposes `internal val <area>Figures: Map<String, Lazy<ExerciseFigure>>`.
- Entries are `"Name" to lazy { privateFigure }`, over the existing private `by lazy` vals.
- Building the map solves no pose. No figure code changed.

**`Figures.kt`:**
- The new `internal fun lazyFigures(areas)` merges the areas. It still refuses a name that appears twice, without building either figure.
- It returns a read-only map:
  - `keys`, `size`, `containsKey` and `in` build nothing;
  - `get` builds only the figure asked for;
  - walking `entries` or `values` builds every figure.
- `Figures.all` is that map. `byName` is unchanged: `testFigures[name] ?: all[name]`.
- So these all still work exactly as before:
  - alias and former-name resolution;
  - `registerForTest`;
  - FigureChecks over all figures;
  - the fingerprint;
  - `everyCatalogueExercise_hasFigure`;
  - the sheet and pictures-page tools.
- A KDoc on `Figures` states the rule for future area files: an area's map must not build its figures, and looking up one exercise must never build another.

**`FiguresTest.figures_areBuiltOnFirstUse`** pins the laziness with counting figures:
- listing the names, counting them and testing membership builds none;
- `get` builds only that figure, and only once;
- walking the entries builds the rest, in order;
- a duplicate name is refused without building anything.

RED: it failed with `Unresolved reference 'lazyFigures'`. GREEN after the change. The fingerprint golden is still `31:29075f43d649812c`.

**Web (`ExercisePicture.tsx`):** each warm-up piece is now one figure (its `figureInfo` and its thumbnail drawing), not "build all figures" in one block. The existing unit tests still cover it.

### The cold first look-up

Measured in node on the generated module: the first `figureInfo('Bench')` call, 3 runs each.

| | First look-up (ms) |
|---|---|
| Before | 48–57 (CPU profile in the browser: 62) |
| After | 5.2–8.0 |

After the change, each figure is built only when looked up:
- **One figure, info plus thumbnail shapes, cold:** about 1.1–1.4 ms median.
- **Slowest figures:** Leg press 17–27 ms, Front squat 6–9 ms, Strict press 6–8 ms.
- **All 31 figures:** 63–87 ms in total.

### The warm-up: kept, by evidence

The tab still looks up `figureInfo` for every row as it renders. Without the warm-up, opening the tab therefore builds every figure cold.

| Speed runs with the warm-up switched off (temporary edit, quiet) | Run 1 | Run 2 |
|---|---|---|
| Exercises tab open (ms) | 154, failed | 138, failed |
| Picker opens (ms) | 89, 87 | 84, 93 |
| Browse by muscle (ms) | 106, failed | 76 |

The warm-up's pieces are now small: the largest is Leg press, about 20 ms. Before this change, one piece took about 55 ms and would have grown with every figure added.

### @speed runs on the final code

All runs used `npx playwright test --project=speed --no-deps --trace off`, with Gradle stopped. In the instrumented runs, the times came from temporary logs, since reverted.

| Instrumented run | Picker | Picker, few hundred | Browse by muscle | Exercises tab |
|---|---|---|---|---|
| 1 | 60 | 52 (worst step 58) | 67 | 64 |
| 2 | 60 | 65 | 45 | 51 (worst step 55) |
| 3 | 46 | 96 | 75 | 54 |

Each instrumented run: 4 passed. After the full e2e, 3 more plain runs: 4 passed each time. Those plain runs started at load average 5.

### 225-scale (temporary hack, not committed, reverted)

The same hack as before: every catalogue row mapped to an existing figure, 5 runs each.

| Case | Open (ms) | Filter changes (ms) |
|---|---|---|
| All rows figured | 53–81 (57 81 53 55 62) | Chest / Back / Arms / All: 27–57, with 16–32 drawn |
| Shipped data | 52–92 | — |
| No thumbnails | 32–67 | — |

### Full suites (final code)

- **Gradle:** `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` reported BUILD SUCCESSFUL. The JUnit XML result files add up to 909 tests, 2 skipped (ignored on purpose), 0 failed.
- **Web:** vitest 39 files / 655 tests passed; typecheck clean; build OK.
- **e2e** (`npm run e2e -- --trace off`): 90 passed.

### Remaining concerns

1. **A tab opened before the warm-up finishes still builds every row's figure on first render.** For example, launching straight to `#/exercises`: about 63–87 ms for 31 figures now, and it will grow toward 225. Fixing it would mean not calling `figureInfo` for rows that are not near the screen, which needs a cheap "has a figure" query in the bridge. I left it as is; it needs a decision.
2. **The picker "few hundred" check measured 96 ms once.** It opens right after a reload, so a warm-up piece can land between its frames. The worst such piece is now Leg press, about 20 ms.
3. **Leg press is the one expensive figure** (17–27 ms to build cold). If more figures use the same sled solver, they will cost about as much.

## Cheap figure check

**Status: DONE_WITH_CONCERNS.**

**Commit:** 93b9a43 `perf: exercises tab checks for a figure without building it`, one commit on 178d6a6. It is the WIP squashed with `git reset --soft 178d6a6`. Not pushed.

### What changed

1. **`Library.hasFigure(name)`** and the bridge call **`GritDomain.hasFigure(name)`** (`grit.hasFigure`).
   - They resolve names, aliases and former names through `Library.find`, the same way `figureInfo` does.
   - They answer from `Figures.has`, which checks `name in testFigures || name in all`. `in` on the lazy map builds nothing.
   - Cost over all 225 rows: 5–7 ms cold, 1–2 ms warm (node).
   - Tests:
     - `FiguresTest.hasFigure_resolvesNamesLikeFigure` covers aliases, a former name, "Overhead press" and "OHP", the lifter's own exercise, an empty name, and an undrawn catalogue exercise before, during and after `registerForTest`. It also checks that `hasFigure` gives the same answer as `Library.figure` for every catalogue name.
     - `GritDomainTest` checks the bridge call on an alias, a former name and the lifter's own exercise.
2. **`ExerciseThumbnail` asks `grit.hasFigure`** to decide whether its tile waits for a drawing.
   - A new `ThumbnailFigure` calls `figureInfo` and draws only once the tile is near.
   - The guide sheet's `ExercisePicture` still uses `figureInfo`, because it needs the captions. No other caller only tested `figureInfo != null`.
   - New vitest test `exercisesTab_coldList_buildsOnlyTheNearTilesFigures`, on a fresh domain with 233 rows:
     - every row asks `hasFigure`;
     - only the 8 near rows with a figure call `figureInfo`;
     - only 8 drawings are made.
   - RED: before the change, `figureInfo` was called for every row. GREEN after.
3. **Idle warm-up (option B).**
   - Each idle callback, or fallback timer, runs exactly one piece unconditionally. A piece is either the catalogue piece or one figure.
   - The catalogue piece is `library()`, the list order, and `hasFigure` for every row; it needs 45 ms.
   - A figure piece builds one figure and draws its thumbnail; it needs 10 ms.
   - Further pieces run only while `timeRemaining()` has room for the next one.
   - A code comment explains the unconditional piece: an idle callback is called only when the browser is idle, and a fixed test clock reports 0 ms of room.
   - The fallback timer reports no room, so it runs one piece per turn.
   - I merged the catalogue and its order into one piece, so that the picker's catalogue is warm one callback earlier.
   - Tests: the warm-up test pins:
     - a 9 ms slice runs only its one piece (the catalogue);
     - with no room, one figure per callback;
     - 9 ms is no room for a second figure, and 10 ms is room for one more.

     The fallback test pins at most one figure per timer. Both tests fail if the unconditional piece is removed (checked).

### @speed (quiet, Gradle stopped)

Each run used `npx playwright test --project=speed --no-deps --trace off`. The times came from temporary logs, since reverted. Open times in ms; the worst step is in brackets where it differs from the open.

| Run | Picker | Picker, few hundred | Browse by muscle | Exercises tab | Result |
|---|---|---|---|---|---|
| 1 | 45 (48) | **98** | 54 | 71 | 4 passed |
| 2 | 49 | **48** (58) | 54 | 43 (45) | 4 passed |
| 3 | 61 | **55** (63) | 61 | 53 (57) | 4 passed |
| Extra 1 | — | **71** | — | 75 | 4 passed |
| Extra 2 | — | **72** | — | 53 (56) | 4 passed |
| Extra 3 | — | **54** (58) | — | 62 | 4 passed |

Earlier tries of the same rule, before the catalogue pieces were merged, gave picker "few hundred" 103 (failed), 82 and 93.

Separately, my harness (fixed clock, 10 opens) gave the picker "few hundred" open 55–100 ms.

### The cold tab, opened before the warm-up

I opened the tab as soon as Today's first screen was up, with no fixed clock and no wait. 10 opens, 14 tiles drawn each time:

| Batch | Open (ms) |
|---|---|
| 1 | 116, 97, 97, 92, 98 |
| 2 | 131, 102, 87, 78, 96 |

Range 78–131 ms. Before `hasFigure` it was 106–149 ms, and 115–181 ms at BASE. No further change was made here, as asked.

### Full suites (final code)

- **Gradle:** `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain :app:testDebugUnitTest` passed in the last run (`--continue`): 911 tests, 2 skipped (ignored on purpose), 0 failed. The fingerprint test passed on the JVM and on JS (golden 31:29075f43d649812c).
- **Web:** vitest 39 files / 656 tests passed; typecheck clean; build OK.
- **e2e** (`--trace off`): 90 passed.

### Concerns

1. **JS test flake.** `GoldenScenarioTest.noStrictPressHistory_coachOutputUnchanged[js, node]` hit mocha's 2 s timeout in 3 combined Gradle runs here: 2.0–3.0 s, while the JVM and app tests ran beside it.
   - It reproduced at 178d6a6's domain sources too (2.002 s), so it is not caused by this change. The test is coach code and does not touch figures.
   - Alone, or in the last run, it takes 1.0 s.
   - The default 2 s mocha timeout is close to the limit on a loaded machine. Worth a separate look; I did not touch it.
2. **The picker "few hundred" check stays close to its limit** (98 ms once in six passing runs). It opens right after a reload, so whether its catalogue is warm depends on whether the first idle callback ran before the click.
3. **The cold tab, opened before the warm-up, is about 80–130 ms.** It builds only the near tiles' figures now. Left for your re-check at 225 figures.
