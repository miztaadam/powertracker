# Review 3 fix 1 (288ce94 -> 0b11d33): scoped re-review

## Verdict
- I1 addressed: YES
- M2 addressed: YES
- New findings: none.

## I1
ExercisePicture.tsx `watch` now creates one IntersectionObserver per scrolling list, with `{ root: scroller, rootMargin: '600px 0px' }`.
The scroller is `placed.closest('.lib-scroll')`, which is the single `overflow-y:auto` div in ExerciseBrowser.tsx:83.
With that element as root, the margin applies to the box that actually clips the rows, so the 600 px lookahead works.
The synchronous "already near" check at mount uses the same scroller edges, so it agrees with the observer.

## M2
ExercisePicture.test.tsx now records the constructor options. It asserts `{ root: <the .lib-scroll element>, rootMargin: '600px 0px' }` and that the root is an HTMLUListElement.
It also checks the near boundary against the list's edges: a row 550 px under the list is drawn at once, and one 650 px under waits.
It checks that a second list gets its own watcher.

## New-defect checks
- Observer leaks: the effect cleanup unobserves the row and deletes it from `waiting`, using the watcher captured in a closure.
  Watchers live in a WeakMap keyed by the scroller, so a removed scroller and its observer can be collected. No leak.
- Rows moving between lists on filter: rows stay inside the same `.lib-scroll` div. A remounted row re-runs the effect and re-resolves its scroller.
  Rows that survive keep their state. `scrollTop = 0` on filter change still applies. Fine.
- Resize: nothing is cached except the mount-time box check. The observer re-evaluates root bounds on its own. Fine.
- No scroller (tile outside `.lib-scroll`): the root is null (the viewport) and the key is `document`. The mount check falls back to 0..innerHeight. Fine.
- No IntersectionObserver: the tile is drawn at once, as before.

## Run
- `npx vitest run src/screens/library/`: 5 files, 64 tests, all passed.
- `npx playwright test e2e/pictures.spec.ts --trace off`: the whole e2e suite ran (87 tests) and all passed.
  That includes the new real-browser test `exercisesTab_thumbnailFarOffScreen_isDrawnWhenItsRowComesNear` and the @speed test.
  The new test checks that a row 900 px below the list is not drawn, that one 400 px below is drawn while still out of view, and that the drawing stays after scrolling away.
  With the old viewport-root observer this test would fail, because the rows are clipped by `.lib-scroll`.
