# The 1.10 follow-ups (version 1.11) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** close the four follow-ups the owner listed after 1.10 ("Do them all", 2026-11-17):
1. the web picker and plan builder get thumbnails, through a cheaper picture;
2. six stale web screenshots are re-shot;
3. the web Exercises tab stops breaking names inside a word at 288 px;
4. the Android Exercises tab is checked at 130% text, and fixed if it breaks names.

**Spec:**
- `docs/superpowers/plans/2026-11-17-grit-picker-thumbnails.md`, its owner decisions and its Outcome;
- `docs/next-update.md`, "Web picker thumbnails: tried, waiting" and "Open follow-ups".

## Decisions (the owner's "Do them all" covers the work; the owner still says yes before any merge)
1. **One text-room rule for every list with thumbnails, on both apps.**
   - A row shows its thumbnail only when the text room is at least 320 (the picker and the Exercises tab), or at least 360 (the plan builder's tick-box rows). *(As built: Android 309 for the picker and 311 for the tab, the loosest floors that keep every name whole, per Task 3. The web tab is under 320 px.)*
   - Text room is the screen width divided by the font scale on Android, or the viewport width in CSS px on the web, which already includes zoom.
   - Below the rule, there is no tile at all. This is the 1.10 picker rule, extended to the tab.
   - At default text on phones 320 wide and up, nothing changes.
2. **The cheaper picture (web).**
   - **The current approach:** each tile is a React subtree of SVG shapes, worked out per row, plus a per-row `useLayoutEffect` that reads the row's box. 1.10 measured about 10 ms of drawing in the first quarter second, however it was scheduled.
   - **The aim:** a tile with no React subtree of shapes and no per-row JS work, so the browser does as little as possible per row. The pictures must look the same as 1.10's, at both pixel densities, in dark and light, on both sides.
   - **Three candidates,** chosen by a spike in Task 1, not up front:
     - (A) SVG data-URI backgrounds, or a CSS mask with the theme colour;
     - (B) a raster sprite sheet, made by a build-time rasteriser that also runs on the deploy build;
     - (C) prebuilt inline markup, with merged paths per colour, set once and not drawn by React shape by shape. This keeps theming through `currentColor` and CSS variables for free.
   - **Feasibility first:** read how many colours a figure uses (the `.fig` shape classes) and whether they change with theme and side. One colour means a mask works. More means an image set per theme and side, which multiplies the size.
   - **Size and start-up limit:**
     - the pictures must not go into the main stylesheet, which blocks first paint;
     - compare the app's first-load and precache size with 1.10;
     - if the tab moves to the new picture, the old shape data goes.
   - **The bar** is timed against the commit Task 1 starts from, not 1.10.0, because noise of about 7.5 ms between two builds of the same code made 1.10's "within 3 ms" undecidable:
     - Each run has two baseline builds plus the candidate, 160 quiet rounds each.
     - **Pass** when the candidate's paired difference from the baseline is no bigger than the baseline-to-baseline difference, for both the picker open median and the keystroke-after-open median. The share of slow opens must be no higher, every keystroke must be within 100 ms, and the tab must be no slower.
     - On-screen pictures must be in within 250 ms. For a CSS background, "in" means the image has loaded and decoded; observe it with an `Image().decode()` of the same URL, or a resource-timing entry, and say which.
     - Speed project 5 of 5.
   - **If it misses:** the web picker keeps waiting, and the owner gets the numbers. The Exercises tab moves to the new picture only if it passes the same bar. Any changed pixel in the tab's screenshots is a finding.
   - **Outcome (2026-11-17):** it missed, so the web waits. The web picker and plan builder ship in 1.11 without thumbnails, as in 1.10, and the web tab keeps its SVG pictures. Only a raster sheet decoded before the first open met the bar; building it needs the owner's decision. The findings and numbers are in `docs/next-update.md`, "Web picker thumbnails: tried, waiting".
3. **Stale screenshots** (`today-install`, `lock-setup`, `session-saved`, dark and light): re-shoot only. Their wording is already right in the app, since ca5877f.

## Global Constraints
- **Every feature stays.** Existing tests pass unedited, except tests that pin how a thumbnail is drawn (`svg.fig` inside `.lib-thumb`), which move to the new picture. List each change.
- **Screenshots:**
  - On the web, only the 6 stale ones, plus picker and plan-builder shots (and the tab's, if the new picture changes a pixel) may change.
  - On Android, only the tab's large-text shots may change, if any are added.
- **Accessibility:** thumbnails stay decorative (`aria-hidden`).
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`;
  - both apps' versions agree;
  - `git fetch` before any merge.

## Review Focus
1. **A fast search in the picker:** each row's picture is its own exercise's, never one left from an earlier row.
2. **Dark and light, both sides:** the picture's colours follow the theme as 1.10's do. A baked-in image must not keep one theme's colours.
3. **Offline (PWA):** the picture file, if any, is precached, so thumbnails show offline.
4. **Your own exercise:** an empty tile of the same size; below the text-room rule, no tile.
5. **288 px (375 at 130% zoom) and Android at fontScale 1.3 on 320 and 360 dp:** no catalogue name breaks inside a word on any of the three lists.

---

### Task 1: Web, the cheaper picture (after Tasks 2 and 3, alone: it needs quiet timing)
- **Feasibility first.** Check the colours, then the main-thread cost of each format, and write the findings in the report.
- **Spike.** Try candidates A, B and C with the picker on, about 40 rounds each, against two baselines. This also shows whether the cost is React or the browser's SVG work. Then build only the winner.
- **Build the winner.** A build-time step (`plugins/figureThumbnails.ts`) emits it. Keep the 320/360 rules.
- **Tests:**
  - the picture shows for Bench and its colour follows dark and light;
  - your own exercise gets an empty tile;
  - Review Focus 1, offline precache, and the timing bar above.
- **If it passes:** re-shoot the screenshots, and commit `feat(web): thumbnails drawn as one image; in the picker too`.
- **If it misses:** stop with the numbers and commit nothing.

### Task 2: Web tab at 288 px, and the stale screenshots (first, beside Task 3)
- The Exercises tab follows decision 1: no tile when the viewport is under 320 px. Add `exercisesTab_namesWholeAt288`.
- Re-shoot all web screenshots. Only the expected ones may change; list them.
- Commit: `fix(web): Exercises tab names stay whole at large zoom; fresh screenshots`.

### Task 3: Android tab at large text (first, beside Task 2)
- **Probe** all three lists (tab, picker, plan builder) at font scales 1.15 and 1.3, on 320, 360 and 412 dp. For each width and scale, record the names that break inside a word.
- **Pick** the loosest floor for the tab that keeps every name whole. Report whether the picker's 320 and the builder's 360 could be loosened too, without changing them.
- **If that floor removes the tab's pictures on a common setting** (360 or 412 dp at 1.15), stop and report: that is the owner's call.
- **If not:** set it from `ExercisesScreen.kt` with `thumbnailMinRoomDp`. Add `exercisesTab_namesWholeAtLargeText` and the warm-up test (a tile-less tab asks for no warm-up). Commit `fix(app): ...`.
- **If nothing breaks:** record the evidence and change no code.

### Task 4: Version 1.11, notes, full run
- Version 1.11 on both apps (versionCode 16), and the Settings screenshots.
- Update `docs/next-update.md`: the outcome of each follow-up and the timings. Update the README phone checks if what you see changed.
- Full run, then five quiet speed runs.
- Commit: `chore: version 1.11; the 1.10 follow-ups`.

Then:
1. independent reviews per platform;
2. fixes;
3. the owner's yes before the merge.

Note (2026-11-17): the owner's standing permission ("Don't ask before merging, just do it") supersedes step 3, "the owner's yes before the merge".
