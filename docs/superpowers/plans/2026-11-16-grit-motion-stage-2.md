# The new look and motion, stage 2: press feedback and vibration (version 1.7) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:**
- Everything you tap on either app sinks a little when pressed and springs back on release.
- The main actions buzz once: in the Android app, and in the web app on Android phones.
- A Settings switch, "Vibrate on taps", turns the buzz off. iPhones show no buzz and no switch.
- Tests can turn motion off, and anyone who asks their phone for reduced motion gets none.
- Nothing else changes.

**Architecture:**
- **Press feedback** is added once per shared component. On Android this is a `Modifier.pressScale()` that uses a `spring`. On the web it is a CSS rule on the shared components (`transform: scale(.97)` on `:active`, with an overshooting ease on release).
- **Haptics** go through one small helper per app:
  - Android: `LocalHapticFeedback` for taps, plus `HapticFeedbackType.LongPress` for a new best (PR).
  - Web: `navigator.vibrate`, only where it exists.

  Each helper reads the new setting.
- **The motion-off switch:**
  - Android: a `LocalMotion` composition local. Tests set it to off, and the system's "remove animations" setting turns it off too.
  - Web: a `data-motion="off"` attribute on `<html>`. Playwright sets it and screenshots use it; `prefers-reduced-motion` already kills transitions.

**Tech Stack:** Kotlin, Compose BOM 2026.02.01, Robolectric; React, CSS, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-16-grit-look-and-motion-design.md`, §2–4, stage 2.

## Owner decisions (approved by the owner on 2026-11-16, all three as written)
1. **The press.**
   - Every tappable thing sinks to 97% of its size, in about 90 ms, and springs back with a small overshoot in about 260 ms. These are the timings in the examples you tried.
   - The tab bar's icons sink to 92%, because they are small.
   - These are the designer's numbers.
2. **What buzzes.** One short tick each for:
   - Log set and Missed;
   - Apply on a coach card;
   - Add to plan or Add to today;
   - a step on the weight, reps or RPE steppers.

   A new best (PR) gets a stronger double buzz. Nothing else buzzes, so it stays meaningful.
3. **The setting.**
   - "Vibrate on taps" is on by default and sits under Rest alert in Settings. The rest alert keeps its own setting.
   - On iPhone the setting is hidden, as the rest alert's vibrate option already is.
   - **Owner, 2026-11-16 (option A):** the setting is NOT in the backup. It is a per-device preference, and after a restore it is on, its default. Backups stay exactly as in 1.6 and restore on every version.
     - The 1.6 readers reject unknown keys in `settings`, which is why it stays out.
     - 1.7's readers also ignore unknown keys inside `settings`, so a future setting can be added without this problem. Unknown keys elsewhere are still rejected.

## Global Constraints
- Every feature stays. Existing tests pass unedited, except tests that pin animation timing; any test changed is listed with its reason.
- Press feedback never moves the layout: it is a visual scale only, so nothing around it reflows. Tap targets stay at least 48 dp/px.
- **Speed:** no new work on the picker's or the Exercises tab's first open.
  - Press styles are CSS only on the web, with no JS per row.
  - The two speed checks are timed against 1.6.2, alternating builds, at least 16 opens each.
- **Motion off:** every screenshot and position-reading test runs with motion off, and a few new tests run with it on.
  - Android: Robolectric screenshots use `LocalMotion` off.
  - Web: Playwright passes `animations: 'disabled'` already; also set `data-motion="off"` for the e2e project.
- **Haptics:**
  - Each buzz goes with something you can see on screen.
  - No buzz while the setting is off.
  - On the web, none where `navigator.vibrate` is missing.
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`;
  - both apps' versions agree.

## Review Focus
1. **A press that turns into a scroll** (finger down on a row, then the list scrolls): the item springs back and nothing fires.
2. **Disabled buttons** do not sink or buzz.
3. **Fast repeated taps on a stepper:** each tap buzzes at most once, with no queue of buzzes afterwards.
4. **With the setting off, or on iPhone,** nothing calls the vibrator.
5. **Reduced motion or motion off:** no scale at all, and tests stay stable.

---

### Task 1: The motion-off switch and the setting (both apps)
- **Android:**
  - `LocalMotion` (on/off). Read the system's animator duration scale, so 0 means off. Robolectric test rules set it off.
  - The setting "Vibrate on taps" (boolean, default true) in settings storage, not in the backup.
- **Web:**
  - `data-motion="off"` on `<html>` when Playwright sets it (an e2e fixture) or the user prefers reduced motion. CSS motion rules sit under `:root:not([data-motion="off"])`.
  - The same setting in `db/settings.ts`. It is not in the backup (owner, option A).
  - Settings UI on both apps; hidden on iPhone.
  - Both backup readers ignore unknown keys inside `settings` only. Tests: a file with an extra settings key restores, an extra key elsewhere is still corrupt, and the existing fixtures restore unchanged.
- Commit: `feat: motion-off switch and the "Vibrate on taps" setting`.

### Task 2: Android press and haptics (after Task 1)
- `Modifier.pressScale()` in `ui/components`, built on `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)` and an interaction source. Apply it in `Buttons.kt`, `Choice.kt`, `Stepper.kt`, `PtCard` (when clickable), the picker rows and the tab bar.
- `rememberHaptics()`, which reads the setting. Calls go at the points in decision 2.
- Tests:
  - a pressed button scales and released springs back (motion on);
  - with motion off there is no scale;
  - haptics are called once per action, and not when the setting is off;
  - disabled buttons get no scale and no haptics.
- Screenshots: none should change.
- Commit: `feat(app): press feedback and haptics`.

### Task 3: Web press and haptics (after Task 1, beside Task 2)
- A CSS press rule on `Button`, `SegmentedChoice`, `StepperField` buttons, `Card` when it is a button, the library rows, the tab bar and `AdviceCard` buttons. Disabled buttons are excluded.
- `haptic(kind)` in `platform/`, using `navigator.vibrate` (10 ms tick, `[30, 40, 60]` for a PR) and reading the setting. It is called at the points in decision 2.
- Tests:
  - vitest for the helper;
  - e2e for `:active` scale with motion on, and none with `data-motion="off"`;
  - the speed timing against 1.6.2.
- Screenshots: none should change.
- Commit: `feat(web): press feedback and haptics`.

### Task 4: Version 1.7, notes, full run
- Version 1.7 on both apps, with the Settings screenshots, which now show the new switch.
- Update `docs/next-update.md` and the README phone checklist: press any button and it springs; Log set buzzes on Android; there is no switch on iPhone.
- Full run, then five quiet speed runs.
- Commit: `chore: version 1.7; press and vibration`.

Then:
1. independent reviews per platform;
2. fixes;
3. your yes before the merge.
