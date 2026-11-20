import { describe, expect, it } from 'vitest'
import { OUTLINE_MIN, TEXT_MIN, outlineArt, type OutlineMeasures } from './outlineArt'

// The guide's pages and controls take 568 px together; one row of buttons (Back, Next) 68 of it; the tallest outlined
// text 200; the picture's place 42 px more than its phone (its padding and the Example label).
const base: OutlineMeasures = { budget: 568, tallest: 200, standardControls: 68, lastControls: 68, chrome: 42 }
const least = 42 + OUTLINE_MIN

describe('outlineArt (spec point 11)', () => {
  it('the five screens before the last share what the pages leave below the tallest text with one row of buttons', () => {
    expect(outlineArt(base).shared).toBe(568 - 68 - 200)
  })

  it('depends on no page: the same whichever controls are on screen now (the budget is the pages and the controls together)', () => {
    // The budget is measured as pages + live controls: on screen 8 the pages are shorter by just what its controls take.
    for (const lastControls of [68, 124, 180]) expect(outlineArt({ ...base, lastControls }).shared).toBe(300)
  })

  it('the last screen is the shared size with the same controls, and gives up only what its taller controls take', () => {
    expect(outlineArt(base).last).toBe(300)
    // A second button (56 with its gap), or Next's longer words wrapping.
    expect(outlineArt({ ...base, lastControls: 124 }).last).toBe(244)
    for (const lastControls of [68, 90, 124, 180]) {
      const art = outlineArt({ ...base, lastControls })
      expect(art.last).toBeLessThanOrEqual(art.shared)
      expect(art.shared - art.last).toBeLessThanOrEqual(lastControls - 68)
    }
  })

  it('never draws a phone below its least height while the text keeps its least window: the text scrolls instead', () => {
    // At 320 × 568 with bodybuilding's three rows of buttons: 300 − 112 would leave the phone absurdly small.
    const art = outlineArt({ budget: 492, tallest: 214, standardControls: 68, lastControls: 180, chrome: 42 })
    expect(art.shared).toBe(492 - 68 - 214)
    expect(art.last).toBe(least)
    // Large text: the tallest text is taller than the pages, and the picture keeps its least size while there is room.
    const large = outlineArt({ budget: 346, tallest: 700, standardControls: 124, lastControls: 140, chrome: 42 })
    expect(large.shared).toBe(least)
    expect(large.last).toBe(least)
    expect(346 - 124 - large.shared).toBeGreaterThanOrEqual(TEXT_MIN)
  })

  it('gives the picture up only where its least size and the text\'s least window do not both fit', () => {
    const art = outlineArt({ budget: 208, tallest: 600, standardControls: 124, lastControls: 140, chrome: 42 })
    expect(art).toEqual({ shared: 0, last: 0 })
    // Just room for the shared page, not for the last with its taller controls.
    const edge = outlineArt({ budget: 124 + least + TEXT_MIN, tallest: 600, standardControls: 124, lastControls: 140, chrome: 42 })
    expect(edge).toEqual({ shared: least, last: 0 })
  })

  it('rounds the measured size down to whole pixels, and the least size up', () => {
    expect(outlineArt({ ...base, budget: 568.7, tallest: 200.2 }).shared).toBe(300)
    expect(outlineArt({ budget: 346, tallest: 700, standardControls: 124, lastControls: 124, chrome: 42.3 }).shared).toBe(Math.ceil(42.3 + OUTLINE_MIN))
  })
})
