# The new look and motion, stage 4: charts that fill in (version 1.9) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Progress's charts and bars fill in when you open the screen. Lines draw from left to right and bars grow from zero. This covers both apps and both sides. The numbers themselves never animate; they show at once. Nothing else changes.

**Architecture:**
- **Android:** the charts are drawn on Compose `Canvas` (`ui/components/Charts.kt`: `LineChart`, `Sparkline`, `BarStrip`; `RatioBar.kt`; `progress/LineChart.kt`). Each takes a `progress: Float` from 0 to 1, from an `Animatable` that starts when the chart first appears. A line is drawn up to `progress` of its length; a bar's height is its value times `progress`. With `LocalMotion` off, `progress` is 1 from the start.
- **Web:** the charts are inline SVG polylines (`LineChart`, `Sparkline`) and div bars (`BarStrip`, `RatioBar`).
  - Lines use `stroke-dasharray` and `stroke-dashoffset` with `pathLength="1"`.
  - Bars use `transform: scaleY` (or `scaleX` for ratio bars) from 0 with `transform-origin` at the base.
  - Both use CSS keyframes that run once when the element mounts, scoped under `:root:not([data-motion="off"])`.
  - No JS animation loop.

**Tech Stack:** Kotlin, Compose BOM 2026.02.01, Robolectric; React, CSS, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-16-grit-look-and-motion-design.md` §2–4, stage 4.

## Owner decisions needed before building
1. **What fills in**, from the inventory of both apps:
   - **The powerlifting side:**
     - the total sparkline on the summary card;
     - the trend line chart;
     - the weekly volume bars;
     - the ratio bars and their markers;
     - the bodyweight line;
     - lift history's chart.
   - **The bodybuilding side:** its Progress is text rows and ratings with no charts, so nothing fills in there.
   - **Never animated:** numbers, stat tiles, labels and axes. They show at once, so you can read a value straight away.
2. **When:**
   - Each time you open Progress, or a lift's history.
   - Each time you change the chart's range (for example 4 weeks to 12 weeks), when the line redraws.
   - Not when the data updates while you're looking, for example after a sync, and not when you scroll back to a chart already shown.
3. **Timing (the designer's numbers):**
   - Lines draw in 600 ms, on an ease that slows at the end.
   - Bars grow in 450 ms, each starting 30 ms after the one before, and the whole strip finishes within 700 ms.
   - The ratio marker slides to its place in 450 ms.

## Global Constraints
- **Every feature stays.** Existing tests pass unedited, except tests that read chart geometry, which run with motion off. List any test changed.
- **Motion off** (tests, screenshots, reduced motion, the phone's "remove animations") means charts are drawn complete at once, exactly as in 1.8. Screenshots must not change.
- **Accessibility:**
  - A chart's accessible description and values are there from the first frame. A screen reader never hears a half-drawn chart.
  - Nothing flashes.
- **Speed:**
  - No work on the picker or Exercises tab.
  - Opening Progress must not get slower to its first paint. The animation only changes what is drawn, not when the screen appears. Time Progress's first paint against 1.8.0 on the web, alternating builds, 16+ opens each.
  - The two picker and tab speed checks still pass.
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`;
  - both apps' versions agree;
  - `git fetch` before any merge.

## Review Focus
1. **Leaving Progress mid-animation and coming back** starts cleanly. Nothing is left half-drawn.
2. **Changing range quickly, twice:** the line ends complete for the last range chosen.
3. **A chart with one point, or none,** still renders as in 1.8, with no division by zero.
4. **Reduced motion:** complete from the first frame.
5. **Dark and light, both palettes:** the drawing line uses the chart's own colour throughout.

---

### Task 1: Android charts fill in
Files: `ui/components/Charts.kt`, `RatioBar.kt`, `progress/LineChart.kt`, and their callers in `progress/` (`ProgressScreen.kt`, `LiftHistoryScreen.kt`).
- A shared `rememberDrawIn(key)` returns the animated progress, gated on `LocalMotion`. `key` is the chart's range, so a range change restarts it.
- Apply it to each chart in decision 1.
- Tests, with motion on and the clock driven:
  - at about mid-animation, a line is partly drawn and a bar is partly grown;
  - by about 700 ms, everything is complete;
  - a range change restarts the line;
  - one point or none is fine;
  - with motion off, everything is complete at frame 0.
- Screenshots must not change.
- Commit: `feat(app): charts fill in`.

### Task 2: Web charts fill in (beside Task 1)
Files: `ui/components/LineChart.*`, `Sparkline.*`, `BarStrip.*`, `RatioBar.*`, and the charts CSS.
- Use the CSS keyframes from the architecture, keyed so a range change remounts the line. Stagger the bars with `animation-delay`, set from a CSS custom property per bar.
- Tests:
  - vitest that the parts are in the DOM complete;
  - e2e with motion on:
    - mid-animation, the line's `stroke-dashoffset` is between 0 and 1;
    - by 700 ms, it is 0 and the bars are at full scale;
    - a quick double range change ends complete;
  - with `data-motion="off"`, no animation runs (`getAnimations()` is empty).
- Speed: time Progress's first paint against 1.8.0.
- Screenshots must not change.
- Commit: `feat(web): charts fill in`.

### Task 3: Version 1.9, notes, full run
- Version 1.9 on both apps (versionCode 14), and the Settings screenshots.
- In `docs/next-update.md`, mark the look and motion project done, and list the follow-ups.
- Add to the README phone checks: open Progress and the charts draw in.
- Full run, then five quiet speed runs.
- Commit: `chore: version 1.9; charts fill in`.

Then:
1. independent reviews per platform;
2. fixes;
3. the owner's yes before the merge;
4. after 1.9, the picker thumbnails (owner, 2026-11-16).
