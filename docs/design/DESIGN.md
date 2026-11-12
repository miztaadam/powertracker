# Grit design system

The app is called **Grit**. The repository, the Gradle modules and the package (`com.powertracker`,
`PowertrackerTheme`, …) keep the original name, Powertracker.

**Direction.** A training instrument, not a lifestyle app: graphite surfaces, chalk-white type and
big condensed tabular numerals you can read from the floor of a dim gym. One accent — warm chalk — marks
the single next action; every other colour means a state (PR, stall, deload) or a real object (a plate).

Mockups: `docs/design/screens/` (rendered by `ScreenshotTest` from the real components; the
`today-*`, `coach-*`, `progress-*` and `lift-history-*` ones are the real screens with sample state). A normal
test run only renders them to `app/build/screenshots/` as a smoke check. To update the committed
PNGs after a visual change, run
`./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots.update=true` and look at them.
Code: tokens in `ui/theme/`, components in `ui/components/`.

## Colour

Dark is the primary theme; light mirrors it. Dynamic colour (Android 12+) is off on purpose.

| Role | Dark | Light | Use |
|---|---|---|---|
| Background | `#0B0C0E` | `#F4F4F0` | Screen |
| Surface (card) | `#131518` | `#FFFFFF` | `PtCard`, bottom bar |
| Surface container | `#1A1D21` | `#EEEEE9` | Blocks inside a card (stat tile, plate strip) |
| Container high / highest | `#22262B` / `#2B3036` | `#E6E6E0` / `#DCDCD5` | Rest bar, segmented track / stepper and secondary buttons |
| Hairline | `#262A30` | `#E0E0D9` | Card borders, row dividers |
| Text | `#F3F4EF` | `#111315` | Values, titles |
| Text secondary | `#A9AEB6` | `#4D525A` | Body copy |
| Text muted | `#959BA4` | `#595E66` | Eyebrows, units, axis labels |
| Outline | `#717A86` | `#787C83` | Selected chip and segment border, planned-set ring, switch-off border |
| **Accent fill (warm chalk)** + ink | `#DCD6C8` + `#1A1812` | `#46423B` + `#FBF8F1` | Primary button, current tab, switch on |
| Accent as text | `#DCD6C8` | `#4B463E` | Rest progress, links, the total sparkline |
| Accent container + text | `#332F26` + `#EAE4D5` | `#E8E2D4` + `#39342B` | Coach suggestion chip |
| Success | `#3DD68C` on `#0F2A1D` | `#0F6B3D` on `#DDF5E7` | PR, progress, set done, in range |
| Warning | `#FFB627` on `#33250A` | `#7A5200` on `#FFEFC7` | Stall, out of range |
| Danger | `#FF6B6B` on `#3A1416` | `#B3232B` on `#FDE1E0` | Deload, failed rep, delete |
| Info | `#6AA8FA` on `#10233F` | `#1A55AD` on `#DFEAFB` | Weak point, re-entry |
| Chart: squat / bench / deadlift / strict press | `#3987E5` / `#D95926` / `#199E70` / `#A98BF0` | `#2A78D6` / `#EB6834` / `#1BAF7A` / `#7C52CC` | Lift identity in charts only |
| Plates (both themes) | 25 `#D8262C` · 20 `#1F5FBF` · 15 `#F2C417` · 10 `#1E9E4A` · 5 `#F2F2EE` · 2.5 `#08080A` · ≤1.25 `#B4BAC2` | | `PlateStrip` only |

Every text colour (text, secondary, muted, accent-as-text, the four state colours) meets WCAG AA
(4.5:1) on every surface from background up to container highest, and on its own container;
`TokenContrastTest` enforces it. It also pins the accent values and the 3:1 minimums around them:
the accent fill against every surface and against the secondary and disabled button fills, and the
outline against container high and highest (the outline is what marks a selected chip or segment).
`NoVoltTest` fails if the retired green accent comes back, by value or by name, in the app's sources
or in this document. The chart and plate colours are graphics, not text: they follow
the 3:1 rule for marks, are always named by a legend or figures, and must never colour text
(light-theme deadlift green is 2.8:1 on white, which is why the legend with values is mandatory).
Read colours through `MaterialTheme.colorScheme`, `PowertrackerTheme.colors` and `Tone.color()` / `Tone.container()`.

## Type

Barlow (text) and Barlow Condensed (numerals, titles), bundled in `res/font`, SIL OFL 1.1
(`assets/licenses/Barlow-OFL.txt`). Tabular figures (`tnum`) are on in every style.

| Style | Face | Size / line | Use |
|---|---|---|---|
| displayLarge | Condensed Bold | 64 / 64 | Hero number (total) |
| displayMedium | Condensed Bold | 44 / 46 | Stepper values |
| displaySmall | Condensed SemiBold | 30 / 34 | Stat tiles, rest clock |
| headlineLarge | Condensed Bold | 34 / 38 | Screen title |
| headlineMedium | Condensed SemiBold | 26 / 30 | Card title, exercise name |
| headlineSmall | Condensed SemiBold | 22 / 26 | Numbers in rows (155 × 5 @ 8) |
| titleLarge / Medium / Small | Barlow SemiBold | 20 / 16 / 14 | Row labels |
| bodyLarge / Medium / Small | Barlow Regular | 16 / 14 / 12 | Copy, captions |
| labelLarge | Barlow SemiBold | 15 | Buttons, chips |
| labelMedium | Barlow SemiBold | 12 | Pills, tab labels |
| labelSmall | Barlow SemiBold, +1.1 tracking, upper case | 11 | Eyebrows (`Eyebrow`) |

## Space and shape

`Space`: 2, 4, 8, 12, 16, 24, 32 dp. Screen gutter 16. Card padding 16. Gap between cards 12.
Touch targets ≥ 48 dp (`Space.touch`). Corners: 6 (xs), 10 (small: step buttons, segments),
14 (medium: buttons, blocks inside cards), 20 (large: cards), pills fully round.
Depth comes from tone steps and a 1 dp hairline. No shadows.

## Components (`ui/components/`)

- `ScreenHeader` — eyebrow + big title + actions; top of every screen.
- `SectionHeader` — upper-case group label with optional action.
- `PtCard` — the standard container (surface + hairline).
- `PrimaryButton` / `SecondaryButton` / `QuietButton` — accent fill, tonal, text; `compact` inside cards.
- `NumberDisplay`, `Eyebrow` — label, big tabular value, small unit on the baseline.
- `Stepper` — one-line − value + (settings, dialogs). `StepperField` — big value over two wide buttons (logging); with `entry` the value is a number field typed straight into (select-all on focus, Done or leaving it commits, the message of a refused entry underneath), and `labelAction` puts a small action right of its caption (the RPE info button) without adding height.
- `SetRow` — set number, weight × reps @ RPE, planned / done / failed, optional "Top set" and "PR".
- `SegmentedChoice` — pick one of a few (miss point Floor / Mid / Lockout, chart range). Segments share the width equally; when a label does not fit an equal share ("Off the shoulders" for a strict press), each gets its label's width plus an equal share of the rest, and the miss-point question goes above the segments instead of beside them. `ChoiceChip` — toggle (training days). Selected is tonal in both: container-highest fill, text colour, a 1 dp outline.
- `Pill` — small status label in a tone.
- `CoachSuggestion` — inline "Coach suggests 155 × 5 @ 8" with reason, Apply, Dismiss.
- `AdviceCard` — coach card shell: toned icon + tag, title (what), body (why), detail slot, actions.
- `StatTile` — labelled figure with a note; with `onClick` it is a button with a chevron (lift tiles open the lift's history). Three lift tiles share a row; with strict press shown they go two to a row.
- `PlateStrip` — one side of the bar drawn in IPF colours plus "25 + 25 + 15 + 2.5". `PlateStripNote` — the same block in words when there is nothing to draw ("Can't load 181 kg exactly").
- `RestTimerBar` — countdown, progress line, −30 s, +30 s, Skip; docks above the bottom bar. The same rest shows as an ongoing notification (`platform/RestTimerService`): barbell status icon, accent tint, "Rest" with a countdown, +30 s and Skip.
- `LineChart`, `Sparkline`, `BarStrip`, `ChartSeries` — e1RM over time with legend and latest values (the legend wraps when four lifts do not fit one line) (a null value is a week with no reading; the line runs across it); tiny trend line; a row of bars for one quantity per week (volume), with the figure in text beside it. Give `Sparkline` a `minSpan` when a flat trend must look flat (the stall card uses 5% of the lift).
- `RatioBar` — lift ratio against its typical range.
- `EmptyState` — icon, title, body, optional action.
- `PtBottomBar` (in `ui/AppShell.kt`) — four tabs, accent indicator on the current one.
- `InfoButton` (in `ui/library/`) — the outlined info icon beside an exercise's name, wherever one is shown (Today card and row, Plan rows, the coach's accessory lists, the lift-history header, picker rows). A 48 dp target labelled "Form guide for <name>"; it opens the sheet and does nothing else. Custom exercises keep it, so rows line up.
- `FormGuideSheet` (in `ui/library/`) — the form guide as a bottom sheet, read between sets: name, area and equipment, purpose, then Setup (dots), How to do it (numbered), Cues and Common mistakes (dots) in 16 sp body text, and the safety line last in its own neutral block. Mockups `form-guide-*`.
- `RpeInfoButton` and `RpeSheet` (in `ui/library/`) — the same outlined info icon beside an RPE label: the RPE caption on the Today card, "RPE" in the Plan item dialog, "Default RPE" in Settings. A 48 dp target labelled "What RPE means". `inline` draws it at 18 dp and takes only the icon's room and no height, for captions and row titles. The sheet is the form guide's sheet with the domain's `RpeGuide`: title, one line of what the scale counts, the values as a dotted list, the app's input range. No safety line: it is not an exercise. Mockups `rpe-sheet-*`.
- `ExercisePickerScreen` (in `ui/library/`) — the full-screen exercise picker of Plan and Today: search, one row of body-area chips (one at a time) and one of equipment chips (any of the chosen) as `ChoiceChip`s that scroll sideways, then "Your exercises" and the catalogue. A row is name, muscles, equipment and the info button; tapping the row picks it. While nothing is searched and no body area is chosen, "Browse by muscle" leads the list: a grid of muscle tiles, three to a row, each a tonal card (surface, hairline, medium radius, 56 dp tall, the name in `titleMedium`). A tile lists that muscle's exercises under "<Muscle> exercises" with a tonal "All muscles" button back to the grid; equipment chips still narrow it, and a search or a body area leaves it. Mockups `exercise-picker-*` (the grid) and `exercise-picker-muscle-*`.
- `LockScreen` (in `ui/lock/`) — what stands in for the app while app lock holds it shut: the Grit icon, one line on why, one primary button (Unlock; Continue when the phone has no screen lock). Mockups `lock-*` and `lock-no-screen-lock-*`.

## Launcher icon

Concept "Barbell I, steel grey" from `docs/design/logo/` (see `BRIEF.md` there): chalk G, R, T on
graphite, the I a steel barbell, no green. In the app it is three vector drawables converted from
the SVGs without changing the paths (`ic_launcher_foreground`, `ic_launcher_background`,
`ic_launcher_monochrome`) behind the adaptive icons `mipmap-anydpi-v26/ic_launcher(.round).xml`.
`LauncherIconTest` draws it as a launcher would (circle, rounded square, themed) to
`docs/design/screens/launcher-icon.png`, with the same `-Pscreenshots.update=true` switch.

## Rules for later screens

Do
- Build from these components and tokens; add a component here before styling one-off in a screen.
- Put numbers in a condensed style (`headlineSmall` or a display style) and units in muted small text.
- Keep one primary button per card or screen region: the thing to do next. It is the brightest filled
  element on every screen.
- Map advice to tones: TARGET → accent chip, STALL → warning, DELOAD → danger, WEAK_POINT / RE_ENTRY → info.
- Pair every state colour with words or an icon ("Failed", "Below range", tick, cross).
- Use the squat / bench / deadlift / strict press colours only for chart marks, always with a legend; text stays in text colours.
- Give `SetRow`-style composed rows one spoken sentence (`clearAndSetSemantics { text = … }`).
- Check both themes; add the screen to `Showcase.kt` / `ScreenshotTest` and look at the PNGs.

Don't
- Don't use the accent for decoration, selection or success. Selected chips and segments are tonal, not
  inverse: chalk on ink would outshine a chalk button.
- Don't use raw hex, default Material purple, dynamic colour, or elevation shadows.
- Don't use state colours as chart series, or plate colours anywhere but plates.
- Don't put two y-scales on one chart; show the total as its own figure.
- Don't shrink a tap target below 48 dp or body text below 12 sp.
- Don't use arrow or tick characters in text (the font has none): use icons.

## Web (`web/`)

The iPhone web app uses the same tokens (`web/src/ui/tokens.css`, CSS variables; light follows the
system) and the same components, ported to React in `web/src/ui/components/` with the Android names
and props in camelCase (`PtCard` is `Card`, `PtBottomBar` is `TabBar`; `Dialog`, `Snackbar`,
`Switch` and `TextField` stand in for the Material ones). Icons are inline SVG in `icons.tsx`.
Every component in its states is on the dev-only page `#/showcase` (`npm run dev`); it is not in
production builds. Mockups: `docs/design/web/screens/showcase-{dark,light}.png`, rendered by
`web/e2e/showcase.spec.ts`. A normal `npm run e2e` only writes them to `web/test-results/`; to update
the committed PNGs run `UPDATE_SCREENSHOTS=1 npm run e2e` and look at them.

The exercise library is in `web/src/screens/library/`: `InfoButton`, `FormGuideSheet` and
`ExercisePicker`, as on Android. `Dialog` has two more variants for them: `screen` fills the phone
(the picker), `sheet` rises from the bottom edge (the form guide, drawn at the end of the page so it
can open from inside a row or another dialog). With one dialog over another, Escape closes the one
on top. The picker's chip rows scroll sideways; its rows are a button to pick and the info button
beside it. The muscle grid and its list are as on Android (`.lib-muscles`, `.lib-muscle`). Mockups: `exercise-picker-*`, `exercise-picker-muscle-*`, `form-guide-*` and `form-guide-end-*`, rendered by
`web/e2e/screenshots.spec.ts`.

**Accent: warm chalk**, the values in the colour table above.

- The primary button is the brightest filled element on every screen. Keep one per card or screen region.
- Selected segments and chips are tonal: container-highest fill, text colour, a 1 px outline. They are
  not inverse, because chalk on ink would outshine a chalk button. The `Snackbar` keeps the inverse style.
- The accent differs from text mostly by warmth, so the rest line and the sparkline read as plain light
  lines. That is accepted.
- `tokens.test.ts` checks the values and the contrast minimums (label on fill 4.5, fill against every
  surface and against the secondary and disabled fills 3, accent text 4.5, chip text 4.5).
  `noVolt.test.ts` fails if a retired green value comes back.
