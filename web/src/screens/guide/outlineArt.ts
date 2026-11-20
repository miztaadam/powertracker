/**
 * The least height of the guide's drawn phone, in CSS px (spec point 11, large text): 112 × 47.5, two thirds of the
 * shared phone on a 320 × 568 screen at the default text size (168 × 71). Below it the phone's rows no longer read as
 * the app's screen; at it, large text still leaves the text a window to scroll on the 390 × 844 and 430 × 932 phones at
 * 200%, and on the 320 × 568 phone at 130%.
 */
export const OUTLINE_MIN = 112

/**
 * The least window the text keeps above a phone held at its least height, in CSS px: two lines of body text. Where the
 * phone at its least height and this window do not both fit (200% text on the smallest phones), the picture gives way.
 */
export const TEXT_MIN = 48

/**
 * What the guide's layout gives the outline's measure, all in CSS px. `budget`: the pages and the controls under them
 * together, the same on every page (on screen 8 the pages are shorter by just what its controls take). `tallest`: the
 * tallest outlined text, either side, at the pages' width. `standardControls`: the controls with one row (Back, Next);
 * `lastControls`: screen 8's, with its own first button's words and its second button, if any. `chrome`: what the
 * picture's place takes beyond its phone (its padding, the Example label and its gap).
 */
export type OutlineMeasures = { budget: number; tallest: number; standardControls: number; lastControls: number; chrome: number }

/** The height of the picture's place (its phone plus `chrome`): `shared` on PLAN to MONTHS_LATER, `last` on YOUR_TURN. */
export type OutlineArt = { shared: number; last: number }

/**
 * The outline's place on the outlined screens (spec point 11). The five before the last share what the pages leave
 * below the tallest text with one row of buttons; screen 8 takes the same less only what its taller controls take. A
 * page's size depends on that page only, never on which page is current. Neither phone goes below `OUTLINE_MIN`: past
 * that the text scrolls in what is left, as long as it keeps `TEXT_MIN`; where even that does not fit, the place is 0.
 */
export function outlineArt({ budget, tallest, standardControls, lastControls, chrome }: OutlineMeasures): OutlineArt {
  const least = Math.ceil(chrome + OUTLINE_MIN)
  const place = (controls: number) => {
    const pages = budget - controls
    const fits = Math.floor(pages - tallest)
    if (fits >= least) return fits
    return pages - least >= TEXT_MIN ? least : 0
  }
  return { shared: place(standardControls), last: place(lastControls) }
}
