# Chalk accent and typed numbers on Today: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace volt green with "Warm chalk" everywhere in both apps. On Today's exercise card, replace the hidden tap-to-type dialog with inline number fields.

**Architecture:**
- Both apps already route every accent through one token set: web `web/src/ui/tokens.css` and Android `ui/theme/Color.kt` (`GritColors`). Swapping the tokens recolours everything.
- Selected chips and segments move from the inverse style to a tonal style, so the primary button stays the brightest filled element.
- The Today figures become controlled numeric inputs that commit through the existing editor actions (`edit`). No domain, schema or backup changes.

**Tech Stack:**
- Web: React 18 + TypeScript, Vitest + Testing Library, Playwright with Chromium in iPhone 13 emulation.
- Android: Kotlin + Jetpack Compose, Robolectric + Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-11-11-grit-chalk-and-typing-design.md`

## Global Constraints

- **Accent tokens.** Exact values from spec §2.1:

  | Token | Dark | Light |
  |---|---|---|
  | accent | `#DCD6C8` | `#46423B` |
  | on-accent | `#1A1812` | `#FBF8F1` |
  | accent text | `#DCD6C8` | `#4B463E` |
  | accent container | `#332F26` | `#E8E2D4` |
  | on-accent-container | `#EAE4D5` | `#39342B` |

- **Contrast minimums** (WCAG 2), from spec §2.2:
  - button label on the fill: 4.5
  - fill vs every surface: 3
  - accent text vs every surface: 4.5
  - chip tag on the chip: 4.5
  - fill vs secondary-button and disabled fills: 3
- **No volt left.** No volt value (`#C9F73A`, `#121700`, `#25300A`, `#DDFB86`, `#3F6100`, `#E6F9A8`, `#2A3F00`) may remain in either app's sources or resources. Comments must stop calling the accent "volt" and call it "the accent" instead.
- **Selected chips and segments:**
  - fill: the container-highest surface;
  - text: the plain on-surface colour;
  - border: a 1 px outline.

  The primary button is the brightest filled element on every screen.
- **Left alone:**
  - the launcher icon;
  - status colours (success/PR green, warning, error, info);
  - the shared `:domain` module;
  - the schema and the backup format.
- **Number limits:**
  - weight: 0–1000 kg, at most 2 decimals;
  - reps: 1–99, whole numbers;
  - RPE: 6–10, snapped to 0.5.
- **Comma decimals.** `"102,5"` is read as 102.5.
- **Step sizes:**
  - weight: ±2.5 kg, or ±0.5 kg when change plates are on;
  - reps: ±1;
  - RPE: ±0.5.
- **Web inputs:**
  - font size ≥ 16 px;
  - `inputmode="decimal"` for weight and RPE, `inputmode="numeric"` for reps;
  - touch targets ≥ 48 px;
  - must fit three across at 375 px.
- **Field labels:** "Weight, kilograms", "Reps", "RPE". Step buttons keep "Decrease <label>" / "Increase <label>".
- **Commits:** every commit ends with the session's attribution lines.

## Review Focus

1. **Typed but not committed, then Log set.** The user types "140" and taps Log set without blurring the field. The set must be logged at 140, not the previous value. Pinned in W-T2 and A-T4: `logSet_usesTheTypedValue_withoutBlur`.
2. **An invalid entry left in the field.** The user types "abc", then taps +. The step applies to the last good value and the error clears. Pinned in W-T2 and A-T4: `stepAfterInvalidText_stepsFromLastGoodValue`.
3. **A prefilled weight that the coach changes while the field is focused** (for example, Apply on the suggestion chip). Focus stays, and the field shows the applied value once the user leaves it. The user's in-progress typing is never overwritten mid-keystroke. Pinned in W-T2 and A-T4: `applyWhileTyping_doesNotClobberKeystrokes`.
4. **Light theme at outdoor brightness.** The graphite fill on near-white must still separate from the secondary buttons. This is covered by the contrast tests in W-T1 and A-T3. Fable's screenshot check confirms it on every screen.
5. **Clearing the field to empty, then blurring.** An empty field is not a value. The editor keeps its last good value and no error is shown. If there was never a value, Log set stays disabled. Pinned in W-T2 and A-T4: `clearedField_keepsLastGoodValue`.

---

### Task W-T1: Web chalk accent

**Files:**
- Modify: `web/src/ui/tokens.css` (the accent tokens, dark at ~:52–56 and light at ~:118–120; add a light `--accent` and `--accent-ink`, since they're no longer shared).
- Modify: `web/src/ui/components/SegmentedChoice.css` (the selected state at ~:37–38), plus any chip CSS that uses `--inverse-surface` for a selected state. The Snackbar keeps its inverse style.
- Modify: the comments that say "volt" in `Button.tsx`, `SegmentedChoice.tsx`, `Switch.tsx`, `TabBar.tsx` and `screens/today/ExerciseCard.tsx`, and the "Web" section of `DESIGN.md`.
- Modify: `web/public/manifest.webmanifest` and `web/index.html` `theme-color`, but only if they carry a volt value.
- Test: `web/src/ui/tokens.test.ts`, and a new `web/src/ui/noVolt.test.ts`.

**Interfaces:**
- Consumes: nothing.
- Produces: the CSS variables `--accent`, `--accent-ink`, `--accent-text`, `--accent-container` and `--accent-on-container`, with the Global Constraints values in both themes.

- [ ] **Step 1: Write the failing tests**
  - In `tokens.test.ts`, assert the 10 hex values above per theme, and assert every contrast minimum in Global Constraints against every surface token in each theme, reusing the file's existing contrast helper.
  - Add `noVolt.test.ts`. It recursively reads `web/src/**/*.{ts,tsx,css}`, `web/index.html` and `web/public/manifest.webmanifest`, excluding `web/src/domain/generated/**`. It asserts that no volt hex from Global Constraints appears, case-insensitively.
- [ ] **Step 2: Run `cd web && npx vitest run src/ui`.** Expected: FAIL on the token values and on volt hexes found in `tokens.css`.
- [ ] **Step 3: Implement the token values and the tonal selected style** (container-highest fill, `--on-surface` text, 1 px `--outline` border). Reword the comments.
- [ ] **Step 4: Run `npm test && npm run typecheck && npm run build`.** Expected: PASS.
- [ ] **Step 5: Re-render the screenshots.**
  - Run `UPDATE_SCREENSHOTS=1 npm run e2e -- --trace off` to re-render `docs/design/web/screens/*`.
  - Check every image in both themes against the rule "primary button is the brightest filled element", and fix any screen that breaks it.
  - Run `npm run e2e -- --trace off` again. Expected: all pass.
- [ ] **Step 6: Commit** as `feat(web): warm chalk accent replaces volt`.

### Task W-T2: Web typed numbers on Today

**Files:**
- Create: `web/src/ui/components/NumberField.tsx` and `NumberField.css`, with tests in `components.test.tsx` or `NumberField.test.tsx`.
- Modify: `web/src/ui/components/StepperField.tsx` and `.css`. Replace `onValueClick` with an optional `field` slot, or an `input` prop that renders `NumberField` where the figure was. Keep the non-input figure for showcase and other uses.
- Modify: `web/src/screens/today/ExerciseCard.tsx`. Remove the weight and reps `NumberEntryDialog` usages and the "Tap the number to type it in" caption. Wire the three fields.
- Modify: `web/src/screens/today/TodayDialogs.tsx` only if something becomes unused. `NumberEntryDialog` stays for bodyweight in `SessionCards.tsx`.
- Modify: the e2e specs that opened the dialog (`e2e/today.spec.ts`, `e2e/session.spec.ts`, `e2e/helpers.ts`), and `showcase` if it shows the stepper.
- Test: `web/src/screens/today/ExerciseCard.test.tsx` (or the existing Today test file), and the e2e specs.

**Interfaces:**
- Consumes: the existing `actions.edit(exerciseId, editor)` and `log(failed)` in `ExerciseCard.tsx`, and the `MAX_KG`, `MAX_REPS`, `MIN_RPE` and `MAX_RPE` constants.
- Produces:
  - `parseEntry(text: string, kind: 'kg' | 'reps' | 'rpe'): { ok: true; value: number } | { ok: false; reason: 'empty' | 'nan' | 'range' }` in `web/src/screens/today/entry.ts`. It is pure and applies the Global Constraints limits, the comma decimal and the 0.5 RPE snap.
  - `NumberField` props: `{ label: string; valueText: string; placeholder?: string; inputMode: 'decimal' | 'numeric'; error?: string; onDraft(text: string): void; onCommit(text: string): void }`.

- [ ] **Step 1: Write the failing tests.**
  - `entry.test.ts`:
    - `'102,5'` → 102.5;
    - `'abc'` → nan;
    - `'1200'` kg → range;
    - `'0'` kg → ok 0;
    - `'0'` reps → range;
    - `'11'` rpe → range;
    - `'8.3'` rpe → ok 8.5;
    - `''` → empty.
  - Today component tests:
    - `typeWeight_thenLog_logs140`: with no weight, type 140, tap Log set 1, and the saved set has `weightKg` 140. The plate strip showed 140 kg's breakdown (per side `25 + 25 + 10` with the standard plates).
    - `tapPrefilled_selectsAndReplaces`;
    - `invalid_showsMessage_andDoesNotLog`;
    - `typeThenPlus_stepsFromTyped`;
    - `bigJump_stillAsksDidYouMean`;
    - `reload_keepsTypedValue`;
    - plus the five Review Focus tests by name.
- [ ] **Step 2: Run `npx vitest run src/screens/today src/ui`.** Expected: FAIL (missing module and missing input).
- [ ] **Step 3: Implement `parseEntry`, `NumberField` and the wiring.**
  - On focus: select all.
  - On Enter: blur.
  - On commit (blur, Enter, a step tap, or Log set / Missed): call `parseEntry`. If ok, call `edit` with the value. If not, keep the last good value; show the error unless the reason is `empty`.
  - Log set and Missed commit any draft first, then log.
  - Weight placeholder: `kg` when empty.
- [ ] **Step 4: Update the e2e specs to type into the fields.** `fullSession` types the weight. Keep the 375 px no-overflow and 48 px target assertions.
- [ ] **Step 5: Run `npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`.** Expected: PASS. Re-render the Today screenshots (`UPDATE_SCREENSHOTS=1`).
- [ ] **Step 6: Commit** as `feat(web): type weight, reps and RPE directly on Today`.

### Task A-T3: Android chalk accent

**Files:**
- Modify: `app/src/main/kotlin/com/powertracker/ui/theme/Color.kt`. Remove `Volt` and `VoltInk`. Set the dark and light `GritColors` accent fields to the Global Constraints values, and add light-specific accent and on-accent values. Also update the Material `ColorScheme` mapping in `Theme.kt` if it references volt or the accent.
- Modify: `app/src/main/kotlin/com/powertracker/ui/components/Choice.kt` (~:35, :42, :62, :69). Selected becomes `surfaceContainerHighest` with `onSurface` text and a 1 dp `outline` border.
- Modify: `app/src/main/res/values-night/colors.xml`. Set `notification_accent` to `#DCD6C8`. If `values/colors.xml` has a light `notification_accent`, set it to `#46423B`.
- Modify: the comments that say "volt" in `Buttons.kt`, `RestTimerBar.kt`, `Choice.kt`, `AppShell.kt` and `today/ExerciseCard.kt`, and `app/src/test/.../design/Showcase.kt`.
- Test: `app/src/test/kotlin/com/powertracker/ui/design/TokenContrastTest.kt`, and a new `NoVoltTest.kt` in the same package.

**Interfaces:**
- Consumes: nothing.
- Produces: `GritColors.accent`, `onAccent`, `accentText`, `accentContainer` and `onAccentContainer`, with the Global Constraints values in `DarkGritColors` and `LightGritColors` (keep the existing object names).

- [ ] **Step 1: Write the failing tests.**
  - `TokenContrastTest`: assert the hex values, and every contrast minimum in Global Constraints against every surface in each scheme.
  - `NoVoltTest`: walk `app/src/main/**/*.{kt,xml}` and assert that no volt hex appears in either the `0xFFC9F73A` or the `#C9F73A` form.
- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests '*TokenContrastTest' --tests '*NoVoltTest'`.** Expected: FAIL.
- [ ] **Step 3: Implement the token values, the tonal selected chips and the comment rewording.** Keep `LauncherIconTest` green, since the icon is unchanged.
- [ ] **Step 4: Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`.** Expected: PASS.
- [ ] **Step 5: Re-render the screenshots.**
  - Run `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest' -Pscreenshots.update=true` to re-render `docs/design/screens/*`.
  - Check every image in both themes against the brightest-filled-element rule.
- [ ] **Step 6: Commit** as `feat(android): warm chalk accent replaces volt`.

### Task A-T4: Android typed numbers on Today

**Files:**
- Modify: `app/src/main/kotlin/com/powertracker/ui/components/Stepper.kt` (`StepperField` at ~:80). Add an inline numeric `BasicTextField` mode in place of `onValueClick`, styled like the current big figure.
- Create: `app/src/main/kotlin/com/powertracker/ui/today/Entry.kt` with `parseEntry`.
- Modify: `app/src/main/kotlin/com/powertracker/ui/today/ExerciseCard.kt` (~:224 and the entry dialogs). Remove the weight and reps dialog paths and the "tap the number" note. `TodayDialogs.NumberEntryDialog` stays for bodyweight.
- Test: `app/src/test/kotlin/com/powertracker/ui/today/EntryTest.kt`, plus `app/src/test/kotlin/com/powertracker/ui/TodayScreenTest.kt`, updated and extended.

**Interfaces:**
- Consumes: the existing `TodayActions.onEdit(exerciseId, SetEditor)` and `onLogSet(item, failed)`, and `SetEditor`.
- Produces:
  - `sealed interface EntryResult { data class Ok(val value: Double) : EntryResult; enum class Error : EntryResult { EMPTY, NAN, RANGE } }`
  - `fun parseEntry(text: String, kind: EntryKind): EntryResult`, with `enum class EntryKind { KG, REPS, RPE }`. Same rules as the web `parseEntry`.
  - `StepperField(..., entry: StepperEntry? = null)`, where `data class StepperEntry(val keyboard: KeyboardType, val placeholder: String?, val error: String?, val onDraft: (String) -> Unit, val onCommit: (String) -> Unit)`.

- [ ] **Step 1: Write the failing tests.**
  - `EntryTest`: the same cases as the web `entry.test.ts`.
  - `TodayScreenTest`: the same scenarios as W-T2 Step 1 and the five Review Focus tests. Use `performTextReplacement` / `performImeAction` and `performClick` on "Log set 1".
  - Existing dialog-based tests: rewrite them to type into the field.
- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests '*EntryTest' --tests '*TodayScreenTest'`.** Expected: FAIL.
- [ ] **Step 3: Implement, matching the W-T2 commit rules.**
  - Focus selects all, via `TextFieldValue` with a full `selection` on focus.
  - The IME action is Done, and it commits.
  - Losing focus commits.
  - Log set and Missed commit the draft first.
  - Configuration changes keep the draft, via `rememberSaveable`.
- [ ] **Step 4: Run `./gradlew :domain:allTests :app:testDebugUnitTest :app:assembleDebug`.** Expected: PASS. Re-render the Today screenshots with `-Pscreenshots.update=true`.
- [ ] **Step 5: Commit** as `feat(android): type weight, reps and RPE directly on Today`.
