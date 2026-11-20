/**
 * The room the guide's outlined screens leave their picture (spec point 11), from a measure of the page.
 * `shared`: what PLAN, TODAY, TARGETS, HOLDS_BACK and MONTHS_LATER keep, the same on each; `second`: the room the second
 * button under the controls takes, with its gap (screen 8's See my plan, or Build my plan).
 */
export type OutlineArt = { shared: number; second: number }

/**
 * `pagesHeight` is the pages' height as measured; `tallest` the tallest outlined text at that width; `second` the second
 * button's room; `secondShown` whether it is on screen now (the pages are then that much shorter). The shared height is
 * what the pages leave below the tallest text with one row of buttons, so it is the same whether or not the second
 * button shows: no page keeps room for it.
 */
export function outlineArt({ pagesHeight, tallest, second, secondShown }: { pagesHeight: number; tallest: number; second: number; secondShown: boolean }): OutlineArt {
  return { shared: Math.max(0, Math.floor(pagesHeight + (secondShown ? second : 0) - tallest)), second }
}

/** One screen's picture height: the shared height, less the second button's room on the last screen when it has one. */
export function outlineArtFor(art: OutlineArt, isLast: boolean, hasSecond: boolean): number {
  return isLast && hasSecond ? Math.max(0, Math.floor(art.shared - art.second)) : art.shared
}
