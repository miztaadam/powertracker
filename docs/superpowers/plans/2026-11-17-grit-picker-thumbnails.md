# Thumbnails in the exercise picker (version 1.10) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The exercise picker's rows lead with the same small picture the Exercises tab shows. This applies on both apps and both sides. The picker still opens, filters and searches within its 100 ms checks, and nothing else changes.

**Architecture:** Both apps already have the thumbnail; this switches it on in the picker, reusing what the tab has, with no new drawing code.
- **Android:** `ExerciseBrowser(showThumbnails = true)` in `ExercisePickerScreen.kt` and the plan builder. Thumbnails come from the existing `ThumbnailLoader` worker, off the main thread. A row draws a ready thumbnail in its first frame, and otherwise shows its empty tile at the same size until it is ready.
- **Web:** `<ExerciseBrowser thumbnails>` in `ExercisePicker.tsx` and `PlanBuilderScreen.tsx`. The tab's `ExerciseThumbnail` already draws only tiles on or near the screen, from shapes worked out at build time (`thumbnails.ts`).
- **The web's speed:**
  - **Where it stands:** a quick try on 2026-11-16 made the picker's first open about 8 ms slower on average, and 3 of 12 speed runs went over 100 ms (worst 114 ms).
  - **The fix:** in the picker, rows are drawn first and their pictures one moment later, after the list has settled. The empty tile is there from the start at full size, so nothing moves when a picture arrives.

**Tech Stack:** Kotlin, Compose BOM 2026.02.01, Robolectric; React 18, vitest, Playwright.

**Spec:** the owner's request (2026-11-16), "thumbnails in the exercise picker when adding to the plan, both modes", and the speed note in `docs/superpowers/specs/2026-11-16-grit-look-and-motion-design.md` §5. The Exercises tab's thumbnail work: `docs/next-update.md`, "Android thumbnails off the main thread".

## Owner decisions (to approve)
1. **Where.** Every place you choose exercises gets the thumbnails, on both sides:
   - Plan's **Add** on a day;
   - Today's **Add exercise**;
   - the plan builder's exercise list.

   Your own exercises have no figure, so they get the empty tile, as on the Exercises tab, which keeps the names lined up. The muscle grid and the chips are unchanged.
2. **Rows first, then pictures.** When the picker opens, or after you type or change a filter, the names appear at once as today, and the pictures fill in a moment later.
   - On Android, a picture shows in the first frame if it is ready. The tab's warm-up and now the picker's make most pictures ready.
   - The 100 ms speed checks stay exactly as they are and must pass.
3. **If the web can't keep its speed.** Android ships with the thumbnails anyway. On the web the picker stays without them, and I come back to you with the timings. Nothing ships slower.

## Global Constraints
- **Every feature stays.** Existing tests pass unedited, except these two, which pinned "the picker has no thumbnails" and are turned around:
  - Android `ExercisePictureTest.picker_hasNoThumbnails` and `ThumbnailsTest.picker_startsNoThumbnailWork`;
  - web `pictures.spec.ts`, `ExercisePicture.test.tsx` `picker_hasNoThumbnails`.

  List any other test that changes, with the reason.
- **Speed:** the speed checks are timed against 1.9.0, alternating builds, 16+ opens each. The web checks are `library.spec.ts` (picker open, keystrokes, filters, a few hundred exercises, the muscle grid) and `pictures.spec.ts`; the Android one is "no thumbnail worked out on the main thread".
  - **The bar:** the picker's median first open is within 3 ms of 1.9.0, its worst open within 10 ms, and 5 quiet speed runs pass 5 of 5.
  - The Exercises tab must not get slower.
- **Layout:**
  - At 320 and 375 px and at 130% zoom, nothing overflows.
  - The info (ⓘ) button keeps a 48 px target.
  - Long names wrap as they do on the tab.
- **Accessibility:** a thumbnail is decorative, as on the tab. The row's spoken name is unchanged, and so is its ticked state in the plan builder.
- **Look:** the tile, the picture and its colours are the tab's, in dark and light and on both sides. There are no shadows on picker rows (the spec, §3: no per-row shadows on those lists).
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`;
  - both apps' versions agree;
  - `git fetch` before any merge.

## Review Focus
1. **Typing quickly in the search:** each new set of first rows shows its names at once. No picture is left from an earlier row, so a recycled row never shows the wrong exercise's picture. A test types three letters in a row and checks that each row's picture belongs to its own name.
2. **Opening the picker before the tab has ever been opened** (a fresh start straight to Plan): names first, then pictures, with nothing moved. This is checked on both apps.
3. **Scrolling the picker fast to the bottom of a few hundred exercises:** rows keep their place and pictures fill in. The tab's 600 px look-ahead is used on the web.
4. **The plan builder's ticked rows:** the tick, the picture and the name all fit at 320 px.
5. **Closing the picker while pictures are still coming:** nothing is left running. On Android the worker keeps going, which is fine, because its results are kept. On the web no observer is left behind (the tab's watcher map is weak).

---

### Task 1: Android picker thumbnails
**Files:**
- `ui/library/ExercisePickerScreen.kt` and `ui/plan/builder/PlanBuilderScreen.kt`: pass `showThumbnails = true`.
- `ui/library/ExerciseBrowser.kt`: only if the warm-up's order needs the picker's list.
- Tests: `ExercisePictureTest.kt`, `ThumbnailsTest.kt`, and screenshots of the picker.

**Steps:**
- [ ] Turn `picker_hasNoThumbnails` into `picker_rowsShowThumbnail`, in both modes: Bench's row has its picture, and your own exercise has an empty tile of the same size with its name lined up.
- [ ] Turn `picker_startsNoThumbnailWork` into `picker_neverWorksAThumbnailOutOnTheMainThread`, which uses the existing recording loader.
- [ ] Add `pickerSearch_eachRowShowsItsOwnPicture`, for Review Focus 1.
- [ ] Add `planBuilder_tickedRow_fitsAt320`.
- [ ] Switch it on in both places and run `:app:testDebugUnitTest`.
- [ ] Re-shoot the screenshots with `-Pscreenshots.update=true`. Only picker and plan-builder PNGs should change; list them.
- [ ] Commit: `feat(app): thumbnails in the exercise picker`.

### Task 2: Web picker thumbnails (beside Task 1)
**Files:**
- `screens/library/ExercisePicker.tsx` and `screens/plan/builder/PlanBuilderScreen.tsx`: pass `thumbnails`.
- `screens/library/ExercisePicture.tsx`: add `ExerciseThumbnail({ name, later }: { name?: string | null; later?: boolean })`. With `later`, a tile's drawing waits until the list has settled, after the next two frames, and after any later search, through the existing watcher.
- `screens/library/ExerciseBrowser.tsx`: pass `later` through only for the picker. The Exercises tab is unchanged.

**Steps:**
- [ ] Turn vitest `picker_hasNoThumbnails` into `picker_rowsShowThumbnail`: once settled, Bench's tile holds its picture, and your own exercise's tile is empty and the same size.
- [ ] Add `picker_firstFrame_namesWithEmptyTiles`: the first render has the names and empty tiles, with no `svg.fig`.
- [ ] Turn e2e `pictures.spec.ts` `picker_hasNoThumbnails` into `picker_rowsShowThumbnail`, and add:
  - `picker_search_eachRowShowsItsOwnPicture`;
  - `picker_noOverflow_at320_and375`;
  - `planBuilder_tickedRow_fitsAt320`.
- [ ] **Time it.** Run the 1.9.0 build and this build alternately, 16+ opens each, with `library.spec.ts` and `pictures.spec.ts`. Record the medians and worst opens in the report.
  - If it meets the bar in Global Constraints, carry on.
  - If not, try `requestIdleCallback` (with a timeout of 200 ms) in place of two frames, and time it again.
  - If it still misses, stop and report. That is owner decision 3.
- [ ] Run `npm test`, typecheck and the full e2e. Re-shoot the screenshots with `UPDATE_SCREENSHOTS=1`. Only picker and plan-builder PNGs should change, apart from the known form-guide noise; list them.
- [ ] Commit: `feat(web): thumbnails in the exercise picker`.

### Task 3: Version 1.10, notes, full run
- Version 1.10 on both apps (versionCode 15), and the Settings screenshots.
- `docs/next-update.md`: a "Done: thumbnails in the picker (1.10)" section, with the timings. Take the thumbnails off the follow-up list.
- README phone checks: open Add on a Plan day, and the names come up at once with the pictures straight after; your own exercise has an empty tile.
- Full run, then five quiet speed runs.
- Commit: `chore: version 1.10; thumbnails in the picker`.

Then:
1. independent reviews per platform;
2. fixes;
3. the owner's yes before the merge.
