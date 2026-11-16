# The new look and motion, stage 3: things slide into place (version 1.8) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When something is added, removed, opened or moved, the change slides smoothly instead of jumping. This covers both apps and both sides. Nothing else changes.

**Architecture:**
- **Android (Compose):**
  - `Modifier.animateItem()` on lazy-list items. Coach already has it; this stage adds it to the Today list and the Plan day rows.
  - `animateContentSize()` on Today's exercise card, which opens in place, and on the logged-set list inside it.
  - `AnimatedVisibility` (fade plus expand) for a set row that appears or goes.
  - All of it is gated by `LocalMotion`, so with motion off nothing animates. That covers tests, screenshots and the phone's "remove animations".
- **Web (React):**
  - The View Transitions API (`document.startViewTransition`), only around the state changes listed below. The DOM update runs inside it with `flushSync`.
  - Every animated element gets a stable `view-transition-name`.
  - The header and the tab bar get their own names and stay still, as in the owner's iPhone guide, item 4. The old view fades out in about 80 ms, before the new one fades in.
  - Where the browser has no View Transitions, the change is instant, as today.
  - Gated by `data-motion`: with motion off, nothing transitions.

**Tech Stack:** Kotlin, Compose BOM 2026.02.01, Robolectric; React 18, CSS, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-16-grit-look-and-motion-design.md` §2–4, stage 3.

## Owner decisions needed before building
1. **What moves (from the inventory of both apps):**
   - **Today:**
     - Opening another exercise: the open card closes and the tapped one opens in place, with the rows below sliding.
     - A logged set slides in under the card's sets.
     - A deleted set slides out.
     - "Add another set" and "Add exercise" slide the new row in.
     - Removing an exercise slides it out.
   - **Coach:** an applied or dismissed card slides away and the others close up. Android already does this; the web gets it.
   - **Plan:**
     - Move up and move down slide the two rows past each other.
     - A new or deleted row slides in or out.
   - **Not moving:** tab changes, the picker and the Exercises tab, dialogs and sheets (they keep today's fade and rise), and anything on Progress.
2. **Timing (the designer's numbers):**
   - Moves and opens take about 280 ms on an ease that settles softly. Android uses a spring with low bounce, so a heavy card doesn't wobble.
   - Fades take 150 ms.
   - Nothing exceeds 350 ms, so logging stays quick between sets.
3. **No separate session screen.** The examples showed a card that opens into a full session view. Grit's card opens in place, so this stage makes that opening smooth; it doesn't add a new screen.

## Global Constraints
- **Every feature stays.** Existing tests pass unedited, except those that pin timing or read positions, which now run with motion off. List any test that changes.
- **Motion off means nothing moves**, on both apps:
  - all tests and screenshots;
  - reduced motion, or the phone's "remove animations";
  - a few new tests with motion on.
- **No input is ever blocked by an animation.** A tap during a transition goes through, and on the web an unfinished transition is skipped (`skipTransition`). Log set during a slide still logs once.
- **Speed:**
  - Nothing new on the picker's or the Exercises tab's first open, and no view transitions there.
  - The two speed checks are timed against 1.7.0 (alternating builds, 16+ opens each).
  - Today's Log set must not get slower: time the save, motion on, against 1.7.0.
- **Web view transitions:**
  - Names are unique per element (for example `set-<id>`, `item-<id>`). The header and tab bar names are fixed.
  - The new look's shadows stay on the moving cards during the move.
  - React state is committed inside the transition with `flushSync`.
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e runs with `--trace off`;
  - both apps' versions agree;
  - `git fetch` before any merge: the owner also pushes to main.

## Review Focus
1. **Fast logging:** three Log set taps in quick succession log three sets, each slides in once, and the list ends in the right order.
2. **The open card changes while a set is sliding:** nothing is lost and nothing is drawn twice.
3. **Delete the last set, then add one at once:** the rows end right.
4. **Narrow phones (306/320 px) and 130% zoom:** nothing overflows during or after a move.
5. **iPhone:** the tab bar and header stay still during any web transition, and no page text shows over the tabs.

---

### Task 1: Android layout motion
Files: `ui/today/` (`TodayScreen.kt`, `ExerciseCard.kt`), `ui/coach/` (check that it already uses `LocalMotion`), `ui/plan/PlanScreen.kt`.
- Add `animateItem` and `animateContentSize`, plus `AnimatedVisibility` for set rows, all gated on `LocalMotion`. Use a low-bounce spring, about 280 ms.
- Tests, motion on, with the clock driven:
  - a logged set appears and settles;
  - a deleted set goes;
  - the open card changes;
  - Plan move up and move down.
- With motion off, everything is instant and the existing tests are unchanged. Screenshots must not change.
- Commit: `feat(app): things slide into place`.

### Task 2: Web layout motion (beside Task 1)
Files: `web/src/state/` or `ui/` (a small `withTransition(update)` helper), `screens/today`, `screens/coach`, `screens/plan`, CSS.
- `withTransition` does nothing with motion off or with no API. Otherwise it runs `startViewTransition` with `flushSync`.
- Give the header and tab bar their names and keep them still. The CSS follows the owner's guide item 4.
- Use it at exactly the changes in decision 1.
- Tests:
  - vitest: the helper is a no-op when off or unsupported;
  - e2e with motion on: three quick Log set taps give three sets in order, the tab bar never moves during a transition, and narrow and zoom still pass;
  - speed timed against 1.7.0.
- Screenshots must not change.
- Commit: `feat(web): things slide into place`.

### Task 3: Version 1.8, notes, full run
- Version 1.8 on both apps (versionCode 13), and the Settings screenshots.
- Update `docs/next-update.md`, and the README phone checks: log a set and it slides in; opening another exercise is smooth; reduce motion turns it off.
- Full run, then five quiet speed runs.
- Commit: `chore: version 1.8; things slide into place`.

Then:
1. independent reviews for each platform;
2. fixes;
3. the owner's yes before the merge.
