# Grit accent: chalky grey options

Exploration only. No product files were changed. Every option was rendered on the real web app
(production preview, iPhone 13 viewport) by overriding the five accent CSS variables from
`web/src/ui/tokens.css`. The Android tokens in `ui/theme/Color.kt` (`accent`, `onAccent`, `accentText`,
`accentContainer`, `onAccentContainer`) map one to one.

- Grids: `compare-dark.png`, `compare-light.png` (baseline row first, then A to D; Today mid-session, Coach, Progress).
- Individual screenshots: `shots/<option>-<screen>-<theme>.png`, plus full-height `-full-` versions of Coach and Progress.
- Icon: `icon-preview.png`.
- Scripts: `options.mjs` (tokens and contrast maths), `capture.mjs`, `compare.mjs`, `table.mjs`.

## Recommendation: B, Warm chalk

Dark `#DCD6C8` with ink `#1A1812`; light `#46423B` with ink `#FBF8F1`.

Reason: it is the only option that still looks like chalk and is told apart from the rest of the UI by
hue as well as brightness. The app's text is already chalk white (`#F3F4EF`) and its surfaces are cool
greys, so a neutral grey accent can only differ by value. The slight warmth makes buttons, the tab pill,
the rest line and the coach chip read as one family that is not "just more text colour". All its
contrast figures clear the targets with room to spare (label 12.3:1 dark, 9.4:1 light).

Second choice: C, Steel chalk, if warm reads as beige to the owner. It separates just as well but
looks more blue-grey than chalk, and its chip sits close to the blue "info" tone.

## What the screenshots show (honest notes)

- No option disappears as a button fill: a light fill on near-black is 6:1 or better against every
  surface, and far from the secondary and disabled fills.
- What is lost against volt, in every option: the accent no longer differs from text by colour.
  The Progress sparkline and the rest line look like plain white lines. This cannot be fixed by a
  token value; it is the cost of a grey accent.
- Clash to resolve before shipping any option: the selected segment ("6 mo" on Progress) and selected
  chips are inverse chalk-on-ink. With A it is identical to a primary button; with B and C it is
  slightly brighter than the accent; with D it clearly outshines it. The selected state needs a
  quieter style (tonal fill) if the accent becomes chalk.
- A, first try: the coach chip (`#303130` dark, `#D4D4CD` light) vanished into the secondary-button
  grey. Adjusted to `#3A3B39` / `#CBCBC4`; it now separates by value, but still looks like a plain grey box.
- A "Block chalk" variant (pure `#FFFFFF` with `#000000` ink, 21:1) was rendered and dropped: on screen
  it was indistinguishable from A. Its shots remain in `shots/d-block-chalk-*` (a cleanup command was blocked).
  It was replaced by D, the literal mid-grey, to test how far "grey" can go.
- D is the weakest. In dark it looks like a dimmed button; in light a mid-grey button with white text
  looks disabled, although it passes on numbers (5.8:1 label, 4.2:1 fill). Not recommended.
- Light theme: every option's dark graphite fill is clearer than today's volt, whose fill is only
  1.0 to 1.25:1 against the light surfaces.
- Focus rings already use `--text` (chalk) by design, so they match any option without a change.
- Icon: the launcher icon is already chalk letters on graphite with no volt, so it needs no change. In
  the preview, B's warmer letters are a subtle shift; D's letters look dull. I would leave the icon as it is.
- Also uses the accent outside the token files: `app/src/main/res/values/colors.xml`
  (`notification_accent`, currently `#3F6100`) would take the light accent-text value.
- DESIGN.md rules that mention volt ("one volt button per region", "don't use volt for charts")
  would need rewording; the sparkline already uses accent-text.

## Options

Character:

- **A. Bright chalk**: fresh chalk on a black bar. The strongest contrast, but it is the same colour
  as text and as selected chips, so the accent stops being its own thing.
- **B. Warm chalk**: used magnesium chalk, slightly bone-coloured. Calm, clearly a different material
  from the cool UI greys; in light it becomes a warm graphite.
- **C. Steel chalk**: chalk dust on a steel bar, cool and bluish. Crisp and technical; in light it is
  a slate button. Leans towards a blue app.
- **D. Dusty chalk (true mid-grey)**: the most literal grey. Button text is black in dark and white in
  light. Reads as muted or disabled; kept as the counter-example.

Contrast is WCAG 2 relative luminance. "Surfaces" are background, surface, container, container-high
and container-highest; the worst case is shown. The disabled fill is 12% text on the card surface.

### Baseline: volt

| Token | Dark | Light |
|---|---|---|
| `--accent` (fill) | `#C9F73A` | `#C9F73A` |
| `--accent-ink` (on-accent) | `#121700` | `#121700` |
| `--accent-text` | `#C9F73A` | `#3F6100` |
| `--accent-container` | `#25300A` | `#E6F9A8` |
| `--accent-on-container` | `#DDFB86` | `#2A3F00` |

| Measured contrast | Dark | Light | Target |
|---|---|---|---|
| Button label on fill | 14.7:1 | 14.7:1 | 4.5 |
| Fill vs surfaces (worst of bg to container-highest) | 10.7:1 | 1.0:1 | 3 |
| Accent text / rest line vs surfaces (worst) | 10.7:1 | 5.2:1 | 4.5 |
| Chip tag on chip | 12.1:1 | 10.2:1 | 4.5 |
| Secondary text on chip (worst text on chip) | 6.3:1 | 6.9:1 | 4.5 |
| Fill vs secondary-button fill | 10.7:1 | 1.1:1 | 3 |
| Fill vs disabled-button fill | 10.6:1 | 1.0:1 | 3 |
| Chip vs card (how much the chip separates) | 1.31:1 | 1.14:1 | - |
| Fill vs plain text colour (distinctness) | 1.13:1 | 14.94:1 | - |

### A. Bright chalk

| Token | Dark | Light |
|---|---|---|
| `--accent` (fill) | `#E6E4DD` | `#2E3135` |
| `--accent-ink` (on-accent) | `#15171A` | `#F4F4F0` |
| `--accent-text` | `#E6E4DD` | `#2E3135` |
| `--accent-container` | `#3A3B39` | `#CBCBC4` |
| `--accent-on-container` | `#EFEDE6` | `#23262A` |

| Measured contrast | Dark | Light | Target |
|---|---|---|---|
| Button label on fill | 14.1:1 | 11.9:1 | 4.5 |
| Fill vs surfaces (worst of bg to container-highest) | 10.5:1 | 9.5:1 | 3 |
| Accent text / rest line vs surfaces (worst) | 10.5:1 | 9.5:1 | 4.5 |
| Chip tag on chip | 9.6:1 | 9.3:1 | 4.5 |
| Secondary text on chip (worst text on chip) | 5.1:1 | 4.8:1 | 4.5 |
| Fill vs secondary-button fill | 10.5:1 | 9.5:1 | 3 |
| Fill vs disabled-button fill | 10.4:1 | 10.2:1 | 3 |
| Chip vs card (how much the chip separates) | 1.62:1 | 1.63:1 | - |
| Fill vs plain text colour (distinctness) | 1.15:1 | 1.42:1 | - |

### B. Warm chalk

| Token | Dark | Light |
|---|---|---|
| `--accent` (fill) | `#DCD6C8` | `#46423B` |
| `--accent-ink` (on-accent) | `#1A1812` | `#FBF8F1` |
| `--accent-text` | `#DCD6C8` | `#4B463E` |
| `--accent-container` | `#332F26` | `#E8E2D4` |
| `--accent-on-container` | `#EAE4D5` | `#39342B` |

| Measured contrast | Dark | Light | Target |
|---|---|---|---|
| Button label on fill | 12.3:1 | 9.4:1 | 4.5 |
| Fill vs surfaces (worst of bg to container-highest) | 9.2:1 | 7.2:1 | 3 |
| Accent text / rest line vs surfaces (worst) | 9.2:1 | 6.8:1 | 4.5 |
| Chip tag on chip | 10.5:1 | 9.6:1 | 4.5 |
| Secondary text on chip (worst text on chip) | 6.0:1 | 6.1:1 | 4.5 |
| Fill vs secondary-button fill | 9.2:1 | 7.2:1 | 3 |
| Fill vs disabled-button fill | 9.1:1 | 7.8:1 | 3 |
| Chip vs card (how much the chip separates) | 1.37:1 | 1.29:1 | - |
| Fill vs plain text colour (distinctness) | 1.31:1 | 1.86:1 | - |

### C. Steel chalk

| Token | Dark | Light |
|---|---|---|
| `--accent` (fill) | `#C3CEDA` | `#3D4A58` |
| `--accent-ink` (on-accent) | `#0D1319` | `#FFFFFF` |
| `--accent-text` | `#C3CEDA` | `#3D4A58` |
| `--accent-container` | `#212C38` | `#DAE3EC` |
| `--accent-on-container` | `#D6E0EA` | `#24303C` |

| Measured contrast | Dark | Light | Target |
|---|---|---|---|
| Button label on fill | 11.7:1 | 9.1:1 | 4.5 |
| Fill vs surfaces (worst of bg to container-highest) | 8.3:1 | 6.6:1 | 3 |
| Accent text / rest line vs surfaces (worst) | 8.3:1 | 6.6:1 | 4.5 |
| Chip tag on chip | 10.6:1 | 10.4:1 | 4.5 |
| Secondary text on chip (worst text on chip) | 6.4:1 | 6.1:1 | 4.5 |
| Fill vs secondary-button fill | 8.3:1 | 6.6:1 | 3 |
| Fill vs disabled-button fill | 8.3:1 | 7.0:1 | 3 |
| Chip vs card (how much the chip separates) | 1.29:1 | 1.30:1 | - |
| Fill vs plain text colour (distinctness) | 1.44:1 | 2.06:1 | - |

### D. Dusty chalk (true mid-grey)

| Token | Dark | Light |
|---|---|---|
| `--accent` (fill) | `#B5B2A9` | `#66655F` |
| `--accent-ink` (on-accent) | `#0B0C0E` | `#FFFFFF` |
| `--accent-text` | `#B5B2A9` | `#575651` |
| `--accent-container` | `#2E2D2A` | `#D6D5CE` |
| `--accent-on-container` | `#CCC9C0` | `#3F3E3A` |

| Measured contrast | Dark | Light | Target |
|---|---|---|---|
| Button label on fill | 9.2:1 | 5.8:1 | 4.5 |
| Fill vs surfaces (worst of bg to container-highest) | 6.3:1 | 4.2:1 | 3 |
| Accent text / rest line vs surfaces (worst) | 6.3:1 | 5.3:1 | 4.5 |
| Chip tag on chip | 8.3:1 | 7.3:1 | 4.5 |
| Secondary text on chip (worst text on chip) | 6.2:1 | 5.3:1 | 4.5 |
| Fill vs secondary-button fill | 6.3:1 | 4.2:1 | 3 |
| Fill vs disabled-button fill | 6.2:1 | 4.5:1 | 3 |
| Chip vs card (how much the chip separates) | 1.33:1 | 1.47:1 | - |
| Fill vs plain text colour (distinctness) | 1.92:1 | 3.18:1 | - |
