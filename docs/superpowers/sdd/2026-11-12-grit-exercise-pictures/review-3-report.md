# Review of Task 3 (web pictures and tools), 933006e..5f5fa78

**Spec verdict: ✅ (spec-compliant).**
**Quality verdict: Changes needed** (1 Important, 5 Minor; no Critical).

Checked: brief, constraints, spec §3/§3.4/§5, rulings R1-R6, report, the full diff and the source files. Android parity compared against ExercisePicture.kt and FigureCanvas.kt. `npx vitest run src/screens/library/ExercisePicture.test.tsx` gives 11/11. I did not re-run the e2e suite and rely on the implementer's 89/89.

## Spec compliance

| Point | Result |
|---|---|
| Placement | Under the header, above the purpose (`FormGuideSheet.tsx:49`). Order asserted in both vitest and e2e. |
| Two panels and captions | Grid of `repeat(2, minmax(0,1fr))`; caption is a `span`, `overflow-wrap:anywhere`, no clamp, ellipsis or nowrap. e2e at 375 px checks 1 line and not cut; at 187 px it wraps without being cut. |
| Single a11y element | `figure role="img" aria-label="Picture: <start>, then <end>."`; svgs and captions are `aria-hidden`. e2e checks the aria snapshot is exactly one line. |
| No figure | `ExercisePicture` returns null. R2 mock is in vitest; e2e uses a custom exercise. `NO_GUIDE` kept. |
| Thumbnail | 56x40 `aria-hidden` tile at the left of the name, 12 px gap, `--container-high`, 6 px corners, figure on the tile (`--fig-surface`). Empty tile for custom exercises and for no-figure catalogue exercises (R6). Row targets are checked at >=48 in e2e. |
| Picker | No `thumbnails` prop, so none. Tested in vitest and e2e. |
| Role to variable | `figure.css` matches the brief (BODY_FAR is `color-mix(55% text, surface)`, ACCENT_FILL has opacity .10). Matches Android's `lerp(onSurface, surface, .45)`. |
| Contrast | Body, equipment and accent are checked at >=3:1 on `--surface` and `--container-high`, dark and light. vitest computes it from `tokens.css`; e2e measures computed colours. |
| `fill-rule` | `nonzero` is set on polygons and asserted. |
| Draw order | Shapes are mapped in facade order, and vitest compares the tag and role sequence. |
| Speed | The `@speed` check is extended to the tab: open, 11 keystrokes and 7 filter changes, each under 100 ms, with a check that tiles exist. |
| Ten spec test names | All ten exist in vitest and in e2e, and they assert what they claim. The vitest `noOverflow`, `largeText` and `followsThemeChange` tests are CSS-source or attribute checks (jsdom cannot lay out), and the e2e versions measure the real thing. This split is documented and acceptable. |
| Tools | `figures:sheet` takes `--label`, names (alias or former name), an area (by id or label) or `all`. Output is `<label>-dark.png` and `<label>-light.png`, six per row, name above each figure, captions under each panel. `figures:page` is self-contained with inline SVG, grouped by area, with a dark/light toggle. Both draw with the app's own `FigureSvg`, `tokens.css` and `figure.css`. `.gitignore` entry added. |
| Android parity | The Android tile, panel gap (8), caption spacing (4), caption colour, `surfaceContainerHigh`, extraSmall corners and 4:3 are matched. A round rect's corner radius is `radius` in both. |

### playwright `workers: 1` on the speed project
This is justified and does not weaken anything. Playwright 1.64's `testProject.workers` is documented for exactly this case. It only serialises the two `@speed` files, and the `e2e` project is untouched. The `dependencies: ['e2e']` ordering already ran speed after everything else. The added wait for the service worker and network idle also keeps the measurement honest.

## Findings

### Important

**I1. The 600 px lookahead for lazy thumbnails does not work inside the scrolling list.**
`web/src/screens/library/ExercisePicture.tsx:36-47` (`watch`), with `library.css:19-24` (`.lib-scroll`) and `ui/shell.css:10-16` (`.shell-main`).
- The `IntersectionObserver` is created with the implicit (viewport) root and `rootMargin: 600px 0px`.
- The rows sit inside two `overflow-y:auto` ancestors (`.lib-scroll` and `.shell-main`).
- Per the IntersectionObserver algorithm, a target is first clipped by every overflow-clipping ancestor with no margin applied.
- `rootMargin` only grows the root rectangle at the end.
- So a tile is drawn only once its row actually enters the scroller, not 600 px before.
- The spec §3.4 and the report's design intent ("drawn before the row scrolls in") are therefore not met for any row that was off-screen at mount.
- When the user scrolls, rows arrive as empty tiles and fill in a frame later (pop-in). No test catches this, because the only off-screen unit test mocks the observer.
- Only the initial `getBoundingClientRect` check (window height + 600) behaves as described.
- Fix:
  - Observe with the scroller as root, `root: tile.closest('.lib-scroll')`, and keep one observer per root in a `WeakMap<Element, IntersectionObserver>` so `rootMargin` applies.
  - Alternatively, keep the shared observer and set the margin by giving `.lib-scroll` no clip.
  - Add an e2e case: scroll the tab by about 3000 px, then assert that tiles of rows within 600 px below the fold already contain an `svg`. This needs several figures, so it can wait for the first batch. A real-browser check of the rootMargin behaviour could be done now with a temporary `grit.figureInfo` stub.

### Minor

**M1. The speed check cannot exercise the real load.**
`web/e2e/pictures.spec.ts` (@speed test).
- Only Bench has a figure, so "225 thumbnails" is never timed. The report's 51-82 ms comes from an uncommitted experiment.
- Fix: when T4 lands, add a figure-count assertion, for example `withFigure(library()).length` drawn after scrolling. Re-measure at T13 as noted.
- The lookahead in I1 changes how many tiles are built at open, so re-measure after fixing it.

**M2. The unit test for the lazy path hides I1.**
`ExercisePicture.test.tsx` (`exercisesTab_thumbnailFarOffScreen_isDrawnWhenItsRowComesNear`).
- It stubs `IntersectionObserver` and `getBoundingClientRect`, so it tests the callback plumbing only.
- Fix: after I1, assert in the stub that the constructor options carry `root` and `rootMargin`.

**M3. The 4:3 panel aspect is duplicated.**
`library.css` (`.lib-picture .fig`, `.lib-thumb .fig`, `aspect-ratio: 4/3`) and `figure-sheet.mjs` `CARD_CSS`.
- `FigureSvg` receives `viewBox` from `figureInfo` but the CSS hard-codes 4:3, as Android does with `PANEL_ASPECT`.
- This is already parked from T2.
- Fix: set `style={{aspectRatio: w/h}}` or assert `viewBox` is 400x300 in a test. The vitest test already asserts `[400,300]`, so this is a note only.

**M4. Several vitest "picture_*" tests only inspect CSS source text.**
`ExercisePicture.test.tsx`: the `rule()` regex helper on `library.css` and `figure.css`.
- These tests are brittle to formatting, for example `.10` versus `0.1`.
- The e2e twins measure the real thing, so no gap in coverage.
- Fix if desired: none needed now. Consider a short comment pointing each vitest test at its e2e twin.

**M5. Unbounded caches and a no-op guard.**
- `FigureSvg.tsx` `drawings` is a module-level `Map` (at most 450 entries, fine).
- `watch()` stores `watcher!` non-null assertions. It is correct but fragile.
- Fix: no action. It could be tidied together with I1.

**M6. `pictures.html` is not committed.**
This is a documented deviation: it would be stale after T4 and is generated in T13. Accepted. The tool was run and checked per the report, and `guides.html` exists for its link.

## Lazy-thumbnail logic (besides I1)
- Scrolling: rows that are near at mount are drawn in a layout effect before paint, and the rest wait on the observer. Once drawn they stay drawn, so scrolling back is free. Correct.
- Filtering: rows are keyed by catalogue name, so surviving rows keep their state. New or remounted rows run the box check again. `scrollTop = 0` on filter change brings the top rows near. Correct.
- Resize: nothing is cached from the window size except the initial box check, and the observer handles later changes. Fine.
- Cleanup: unmount unobserves the row and deletes it from `waiting`. Correct. No leak.
- Reading `getBoundingClientRect` on the `.lib-row` and not on the tile, to avoid forcing layout under `content-visibility`, is sound.

## Code quality
- `FigureSvg` and `ExercisePicture` are small and clear. The `figure.css` split is well-reasoned, since both tools reuse it. Comments are useful.
- Scripts are well-structured; arguments are validated (label, columns, unknown names), and failures exit 1.
- No Critical issues; only I1 needs changing before Approved.
