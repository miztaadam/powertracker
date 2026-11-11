# Grit — app icon concepts

## Brief

Grit is a private, offline Android powerlifting logbook with a built-in coach (squat, bench,
deadlift; RPE; plates; PRs). The identity follows `docs/design/DESIGN.md`: a training instrument,
not a lifestyle app — graphite surfaces, chalk-white type, one volt accent, Barlow Condensed Bold.

**Change of scope (owner).** "Logo" means the **Android launcher icon**, and the icon must contain
the word **Grit / GRIT**. So each concept is an adaptive icon first, not a wordmark-plus-symbol
pair; a separate wordmark lockup is optional and was not drawn. Legibility of the word at real
launcher sizes comes before powerlifting detail.

Rules worked to:

- Adaptive icon, 108 × 108 dp: a background layer and a foreground layer. Launchers show the
  central 72 dp and mask it (circle, squircle, rounded square); the word and every detail that
  matters sit inside the central 66 dp safe circle.
- A monochrome layer per concept for Android 13 themed icons.
- Palette: volt `#C9F73A`, ink `#121700`, graphite `#131518`, chalk `#F3F4EF`. No plate colours
  were needed. Each icon carries its own background, so it works on dark and light wallpapers
  without an `-on-light` variant.
- All lettering is outlined paths (no `<text>`, no fonts). Letter shapes come from the bundled
  Barlow Condensed Bold (SIL OFL), then are modified per concept.
- No flames, flexed arms, or borrowed brand shapes.

## Files

Per concept `<id>` (`a-knurl`, `b-barbell`, `c-plate`, `d-stamp`, `e-dot`):

| File | What |
|---|---|
| `<id>-icon-foreground.svg` | Adaptive foreground, 108 dp, transparent |
| `<id>-icon-background.svg` | Adaptive background, 108 dp, full bleed |
| `<id>-icon-mono.svg` | Monochrome layer for themed icons (black; the launcher tints it) |
| `<id>-icon.svg` | Both layers flattened, unmasked, for previews |

`montage-dark.png` and `montage-light.png` show every icon at 192, 96 and 48 px in circle,
squircle and rounded-square masks, plus the themed icon. `build_icons.py` is the script that drew
everything (fontTools + skia-pathops); it also writes the montage HTML next to itself, which was
then screenshotted with Playwright.

## Concepts

**A · Knurl.** Idea: the word at full effort, sitting on the bar. Construction: GRIT in Barlow
Condensed Bold, narrowed 10 %, thickened and slanted 11°, in ink on a flat volt background; under
it a bar shaft with three knurled sections and two smooth gaps, the layout of a power bar. The
background has a very faint knurl cross-hatch. Fit: knurling is the part of the sport your hands
know. Trade-off: the loudest icon on a home screen, and the knurl turns into a plain underline at
48 px.

**B · Barbell I.** Idea: the I is a loaded bar stood on end. Construction: upright chalk caps on
graphite; the I is a steel bar with two volt plates at each end, standing slightly taller than the
other letters, so the plates read as the serifs of an I. Fit: the plates are the one accent,
exactly as volt is used in the app. Trade-off: at 48 px the I gets busy and the word reads a
little slower than in C or E.

**C · Plate G.** Idea: the G is a competition plate seen face-on. Construction: a geometric G made
from a plate ring, with the crossbar running into a hub and a bar hole at the centre, in volt;
RIT follows in chalk caps. The G is slightly larger than the other letters, like a plate next to
a bar. Fit: one object every lifter recognises, and the G alone can serve later as a small mark
(notification icon, favicon). Trade-off: caps this wide leave the word smaller than in E.

**D · Stamp.** Idea: the whole icon is a plate face and the word is cast into it. Construction: a
volt background with a rim ring, GRIT in ink across the middle, "POWERLIFTING" and "LOGBOOK" set
around the rim like the lettering on a cast plate. Fit: reads as a piece of equipment, and it
suits the circle mask best. Trade-off: the smallest word of the five, and the rim lettering is
texture only below 96 px.

**E · Plate dot.** Idea: the app's own headline type with one change. Construction: title-case
"Grit" in chalk on graphite, set exactly like the screen titles (Today, Coach); the dot of the i
is replaced by a volt plate with a rim groove and a bar hole. Fit: one accent, used once, on a
real object — the rule the design system already follows. Title case is narrower than caps, so
the letters are the largest of the five and the word is the easiest to read at 48 px.

## Recommendation

**E · Plate dot.** It has the largest and most legible word at launcher sizes, it is the only
concept that looks like the app it opens (same type, same case, same single accent), and its
themed version loses nothing. **C · Plate G** is the runner-up if a caps wordmark is preferred;
its G would also make a good stand-alone symbol.

Before shipping the chosen concept: convert the three layers to Android vector drawables
(`ic_launcher_foreground`, `ic_launcher_background`, `ic_launcher_monochrome`), and check it on a
device, since these were only checked in rendered montages.
