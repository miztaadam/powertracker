# Grit: chalk accent and typed numbers on Today

**Date:** 2026-11-11
**Status:** Approved in brainstorming. Awaiting the owner's review of this spec.
**Applies to:** the Android app (`app/`) and the web app (`web/`). Both ship together.
**Part of:** the next update (`docs/next-update.md`). This is project 1 of 3. Strict press and the
exercise library with form tips get their own specs later.

## 1. Purpose

The owner has two requests:

- **A chalky grey accent.** It replaces the volt green everywhere.
- **Typing a weight on Today.** Getting to a weight from 0 with the + and − buttons is slow. Typing the
  number is possible today, but hidden: you tap the figure to open a dialog.

**Success looks like this:**

- The app reads as chalk and steel, with no green anywhere.
- Buttons are still the most eye-catching thing on each screen.
- A first weight is entered with one tap plus the digits.

## 2. Chalk accent: option B, "Warm chalk"

The owner chose option B from rendered comparisons. Those comparisons are in
`docs/design/chalk-options/`, together with Fable's notes on each option.

### 2.1 Tokens

The new values replace volt in web `web/src/ui/tokens.css` and in Android `ui/theme/Color.kt`, along with
any Android token that maps to them.

| Token | Dark | Light |
|---|---|---|
| accent (fill) | `#DCD6C8` | `#46423B` |
| on-accent (text and icons on the fill) | `#1A1812` | `#FBF8F1` |
| accent text (accent-coloured text, rest line, chart highlight) | `#DCD6C8` | `#4B463E` |
| accent container (coach suggestion chip) | `#332F26` | `#E8E2D4` |
| on-accent-container | `#EAE4D5` | `#39342B` |

The light theme now has its own accent fill. Volt used the same fill in both themes.

### 2.2 Measured contrast

All values use WCAG 2 relative luminance. "Worst" means the lowest result across every surface.

| Check | Dark | Light | Minimum |
|---|---|---|---|
| Button label on fill | 12.3:1 | 9.4:1 | 4.5 |
| Fill against surfaces (worst) | 9.2:1 | 7.2:1 | 3 |
| Accent text and rest line against surfaces (worst) | 9.2:1 | 6.8:1 | 4.5 |
| Chip tag on chip | 10.5:1 | 9.6:1 | 4.5 |
| Fill against secondary-button fill | 9.2:1 | 7.2:1 | 3 |
| Fill against disabled-button fill | 9.1:1 | 7.8:1 | 3 |

### 2.3 Where the accent appears

The accent appears wherever volt did:

- primary buttons
- the selected tab pill
- switch tracks
- rest-timer progress
- the coach suggestion chip
- chart highlights: the total sparkline and the last-point marker
- the today marker
- any volt text

### 2.4 Selected states: a quieter style

The selected segment (for example "6 mo") and selected chips currently use inverse chalk on ink. With a
chalk accent they would match or outshine the primary button. They move to a tonal style instead:
the container-highest surface fill, plain text colour and a 1 px outline. **Rule:** the primary button
is the brightest filled element on any screen.

### 2.5 Unchanged

- **The launcher icon.** It already has chalk letters and no volt.
- **Status colours.** Success, warning, error and info stay as they are. In particular, the PR and
  success green is a status colour, not the accent.

### 2.6 Tests

- **Web:** `tokens.test.ts` asserts the new values and every minimum in §2.2.
- **Android:** `TokenContrastTest` does the same.
- **Repo check:** a grep-style test (or an existing one, extended) fails if `#C9F73A` or any other volt
  value remains anywhere in either app's sources.
- **Screenshots:** both apps' committed screenshots are re-rendered. That means
  `docs/design/web/screens/*` and the Android `docs/design/screens/*`, re-rendered wherever the existing
  tooling can do so.
- **Fable's visual check:** Fable compares the screenshots and confirms the §2.4 rule holds on every
  screen.

## 3. Typed numbers on Today's exercise card

### 3.1 Behaviour

The exercise card has three big figures: weight, reps and RPE. Each becomes an inline numeric field.

- **Tapping** focuses the field. The keyboard opens at once and the current value is selected, so
  typing replaces it. There is no dialog.
- **Keyboard type:**
  - Weight and RPE use a decimal keypad (web `inputmode="decimal"`; Android `KeyboardType.Decimal`).
  - Reps uses a numeric keypad.
- **Empty weight:** when there is no weight yet, the field is empty and shows a faint "kg" placeholder.
  This replaces the "No weight yet / Tap the number to type it in" note. The plate strip area then
  shows nothing, or the existing empty-state note without the "tap the number" wording.
- **Committing a value:** the value is checked and saved to the editor on blur, on Enter or Done, or
  when Log set or Missed is tapped. The Log tap uses the typed value.
- **Checking a value:** the same limits as today apply.
  - **Weight:** 0–1000 kg, two decimals at most.
  - **Reps:** a whole number, 1–99.
  - **RPE:** 6–10, snapped to 0.5.
  - **Out of range or not a number:** the field shows an error state and a short message, and the
    editor keeps its last good value. Log set uses the last good value, or stays disabled if there is
    none.
  - **Comma decimals:** "102,5" is accepted as 102.5, for keyboards set to a comma locale.
- **Plate strip:** it updates as the weight is typed. A value that doesn't parse yet changes nothing.
- **Typo guard:** the existing "Did you mean…?" check, for a >30% jump or reps above 12, still runs when
  the set is logged.
- **The + and − buttons stay** under each field:
  - weight: ±2.5 kg, or ±0.5 kg with change plates on
  - reps: ±1
  - RPE: ±0.5

  A tap on + or − commits any pending typed value first, then steps from it.
- **Editor values** persist as before. The web stores them in meta under `editor:<date>:<exerciseId>`;
  Android uses its view-model state. Reload and lock behaviour are unchanged.
- **Accessibility:** each field has a label ("Weight, kilograms", "Reps", "RPE"). Step buttons keep
  their "Decrease/Increase <label>" names. Touch targets stay at least 48 px.
- **Web on iOS:** the input font is 16 px or larger, so Safari doesn't zoom. The 375 px three-across
  layout must still fit, and the existing no-overflow e2e check guards this.

### 3.2 Removed

The exercise card's weight and reps entry dialog is removed. Today's `NumberEntryDialog` stays only for
bodyweight on the start card.

### 3.3 Tests

These run on both apps.

- **Typing a weight and logging:** with no weight, type "140" and log. The saved set is 140 kg, and the
  plate strip showed 140.
- **Replacing a value:** tap a pre-filled weight, type, and the new value replaces the old one.
- **Rejecting a value:** "abc", "1200" and RPE "11" are each refused with a message, and nothing is
  logged with them.
- **Comma decimal:** "102,5" is saved as 102.5.
- **Typing then stepping:** type, then tap +. The step applies to the typed value.
- **Typo guard:** typing a big jump still triggers "Did you mean…?".
- **Reload:** typed values survive a reload (web) and a configuration change (Android).
- **Existing tests:** the e2e and Compose UI tests that used the dialog are updated.
  - **Web e2e:** 375 px no-overflow and 48 px target checks still pass. `fullSession` types the weight.

## 4. Data and compatibility

- **No schema or backup format change.** This project is visual and input-only.
- **The shared Kotlin domain is untouched.**

## 5. Delivery

- **Branch:** the existing working branch. Merge to `main` only with the owner's go-ahead. Vercel
  redeploys from `main` automatically.
- **Who builds it:** Fable builds the visual and UI work. A reviewer checks each task, and a final
  review checks the whole branch, the same as the web build.
- **Android release:** the owner's Android Studio build from `main` picks up the change. No store
  release is involved.
