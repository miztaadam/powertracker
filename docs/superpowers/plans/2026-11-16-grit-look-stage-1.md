# The new look, stage 1: soft cards and the grey accent (version 1.6) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every card on both apps becomes a soft layered card, with tone steps and a soft shadow instead of a 1 dp hairline border, and the powerlifting accent becomes dark grey. Dark and light both change. Nothing behaves differently.

**Architecture:**
- The look lives in the design tokens: Android `ui/theme/Color.kt`, `Dimens.kt` and `Theme.kt`; web `ui/tokens.css`.
- The shared cards (`PtCard`, web `Card`) take a new elevation token rather than a border.
- `docs/design/DESIGN.md` is rewritten first, so it stays the source of truth.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.02.01), Robolectric; React and CSS, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-16-grit-look-and-motion-design.md`, §1–3 and stage 1 of §4. **Approved by the owner on 2026-11-16** (the example-only features stay out; picker thumbnails stay paused).

## Global Constraints
- **Every feature stays.** Existing tests pass unedited, except screenshots and the tests that pin the old look. Those are the contrast and token tests, and any test that asserts a card border. Each change to such a test is listed in the report, with the reason.
- **Powerlifting accent:**
  - dark: `#868A91` with ink `#0B0C0E`;
  - light: `#4A4D52` with ink `#F6F7F8`.
  This covers every place the chalk accent was used: the accent fill, accent text, the accent container, and the on-container colour.
  - Pick the container pair so that each pair passes the existing contrast tests (4.5:1 for text, 3:1 for non-text). Record the values in the report.
  - The bodybuilding palette does not change.
- **Cards:** no 1 dp hairline border on any card or container (`PtCard`, `Card`, Menu, picker tiles, shell button).
  - The elevation is a token per palette and scheme:
    - dark: an inner top highlight of white at 5%, plus a soft drop shadow of black at about 35%, blur about 24, y offset about 8;
    - light: two soft shadows of near-black at 6–8%.
  - Android uses `Modifier.shadow` with a tinted ambient and spot colour, or the tonal elevation of `Surface`. It must match the web closely.
  - Card radius stays as it is.
- **Row dividers inside cards stay**, but use a softer tone (`hairline` at reduced strength, or the container tone). The rows must still be told apart in both schemes.
- **State outlines stay unchanged:**
  - the selected chip and segment;
  - the planned-set ring;
  - the switch-off border;
  - text-field borders and focus or error rings;
  - the `NumberField` underline;
  - the focus ring.
- **No per-row shadows** in the exercise picker or the Exercises tab. Their rows are not cards. The 100 ms speed checks still pass, and the picker and tab are timed against 1.5.0, alternating builds, 16 or more opens each, before and after.
- **Fixed rules:** no model names in code or commit bodies; new commits only; stage files by path; never commit `.claude/`, `CLAUDE.md` or `.superpowers/`; never `playwright install`; e2e with `--trace off`.

## Review Focus
1. **Light mode:** soft cards on the near-white background must still read as separate cards (both palettes). The screenshots are checked by eye.
2. **The grey accent beside the grey text:** buttons, the current tab and switches must still stand out from secondary text and outlines. Contrast tests, and a look at Today, Plan and Settings.
3. **Charts:** the accent-coloured lines and dots (the total sparkline, rest progress) stay legible on the new cards.
4. **Nothing moved:** apart from the border pixels, layout sizes are unchanged. The large-text and narrow-phone tests still pass.
5. **Bodybuilding is unchanged** except for its cards.

---

### Task 1: The design doc and tokens (both apps)
**Files:**
- `docs/design/DESIGN.md`
- Android `ui/theme/Color.kt`, `Dimens.kt`, `Theme.kt`, `Tone.kt` as needed
- web `ui/tokens.css`, `ui/noVolt.test.ts`
- the contrast tests: Android `TokenContrastTest` (find it under `app/src/test`), web `ui/tokens.test.ts`

- [ ] **DESIGN.md:** replace the "No shadows" rule and the hairline-as-card-border rule with the soft-card rule and the elevation tokens. Record the new powerlifting accent pairs. Keep the state-outline rules.
- [ ] **Failing tests first:** update the contrast and token tests to the new accent values, and add the elevation tokens. Run them and see them fail.
- [ ] **Tokens:** change the tokens on both apps. The tests now pass.
- [ ] **Commit:** `feat(design): soft card elevation and the grey powerlifting accent`.

### Task 2: Android cards (after Task 1)
**Files:**
- `ui/components/PtCard.kt` and `AdviceCard.kt`
- the muscle tiles in `library/ExerciseBrowser.kt`
- `HorizontalDivider` uses: soften them, keep them
- Robolectric tests and screenshots

- [ ] `PtCard` uses the elevation token with no `BorderStroke`. Any other card or container border goes too. State outlines stay.
- [ ] Re-shoot every Android screenshot (`-Pscreenshots.update=true`). Look at the dark and light shots of Today, Plan, Coach, Progress and Settings on both sides. List every changed PNG.
- [ ] Run `./gradlew :domain:checkWebDomain :app:testDebugUnitTest`.
- [ ] Commit: `feat(app): soft layered cards`.

### Task 3: Web cards (beside Task 2)
**Files:**
- `ui/components/Card.css`
- `Menu.css`, `ui/shell.css`
- `screens/library/library.css` (tiles only, not rows)
- divider rules in the screen CSS: soften them, keep them
- tests and screenshots

- [ ] Same rules as Task 2. Re-shoot every web screenshot (`UPDATE_SCREENSHOTS=1`); this time the `form-guide-*` and `exercise-guide-actions-*` changes are real.
- [ ] Time the picker and the tab against the 1.5.0 build, alternating builds, 16 or more opens each.
- [ ] Run `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`.
- [ ] Commit: `feat(web): soft layered cards`.

### Task 4: Version 1.6 and notes
- [ ] Android versionName 1.6, versionCode 9; web 1.6.0. Update the Settings screenshots.
- [ ] `docs/next-update.md`: a section on the new look, including the owner's choices and the new accent values.
- [ ] Full run, then three quiet speed runs. Commit: `chore: version 1.6; the new look`.

Then:
1. an independent review per platform, then of the whole branch;
2. publish a before-and-after page of real screenshots for the owner;
3. the owner's yes before any merge.
