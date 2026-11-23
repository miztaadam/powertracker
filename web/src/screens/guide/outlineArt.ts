/**
 * The least height of the guide's drawn phone, in CSS px (spec point 11): 112 × 47.5, two thirds of the shared phone on
 * a 320 × 568 screen at the default text size (168 × 71). Below it the phone's rows no longer read as the app's screen,
 * so no phone is drawn smaller. Screen 8, whose controls can be taller, is held at it while its own text still fits.
 */
export const OUTLINE_MIN = 112

/**
 * What the guide's layout gives the outline's measure, all in CSS px. `budget`: the pages and the controls under them
 * together, the same on every page (on screen 8 the pages are shorter by just what its controls take). `tallest`: the
 * tallest outlined text, either side, at the pages' width; `lastText`: screen 8's own text on the side shown.
 * `standardControls`: the controls with one row (Back, Next); `lastControls`: screen 8's, with its own first button's
 * words and its second button, if any. `chrome`: what the picture's place takes beyond its phone (its padding, the
 * Example label and its gap).
 */
export type OutlineMeasures = { budget: number; tallest: number; lastText: number; standardControls: number; lastControls: number; chrome: number }

/** The height of the picture's place (its phone plus `chrome`): `shared` on PLAN to MONTHS_LATER, `last` on YOUR_TURN. */
export type OutlineArt = { shared: number; last: number }

/**
 * The outline's place on the outlined screens (spec point 11). The five before the last share what the pages leave
 * below the tallest text with one row of buttons; screen 8 takes the same less only what its taller controls take, and
 * is never larger. A page's size depends on that page only, never on which page is current. The text always shows in
 * full beside a phone: where the phone at `OUTLINE_MIN` and the full text do not both fit (large text), the place is 0
 * and the text has the page, scrolling only if it still does not fit. Screen 8 alone is held at `OUTLINE_MIN` while its
 * own text fits in full above it.
 */
export function outlineArt({ budget, tallest, lastText, standardControls, lastControls, chrome }: OutlineMeasures): OutlineArt {
  const least = Math.ceil(chrome + OUTLINE_MIN)
  const fits = Math.floor(budget - standardControls - tallest)
  const shared = fits >= least ? fits : 0
  const lastPages = budget - lastControls
  const lastFits = Math.floor(lastPages - tallest)
  const last = shared === 0 ? 0 : lastFits >= least ? lastFits : lastPages - least >= lastText ? least : 0
  return { shared, last }
}
