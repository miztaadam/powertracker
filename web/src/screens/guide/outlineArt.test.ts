import { describe, expect, it } from 'vitest'
import { OUTLINE_MIN, outlineArt, type OutlineMeasures } from './outlineArt'

// The guide's pages and controls take 568 px together; one row of buttons (Back, Next) 68 of it; the tallest outlined
// text 200, screen 8's own 150; the picture's place 42 px more than its phone (its padding and the Example label).
const base: OutlineMeasures = { budget: 568, tallest: 200, lastText: 150, standardControls: 68, lastControls: 68, chrome: 42 }
const least = 42 + OUTLINE_MIN

describe('outlineArt (spec point 11)', () => {
  it('the five screens before the last share what the pages leave below the tallest text with one row of buttons', () => {
    expect(outlineArt(base).shared).toBe(568 - 68 - 200)
  })

  it('depends on no page: the shared size is the same whatever screen 8 has under its buttons', () => {
    for (const lastControls of [68, 124, 180]) expect(outlineArt({ ...base, lastControls }).shared).toBe(300)
  })

  it('the last screen is the shared size with the same controls, and gives up only what its taller controls take', () => {
    expect(outlineArt(base).last).toBe(300)
    expect(outlineArt({ ...base, lastControls: 124 }).last).toBe(244)
    for (const lastControls of [68, 90, 124, 180]) {
      const art = outlineArt({ ...base, lastControls })
      expect(art.last).toBeLessThanOrEqual(art.shared)
      expect(art.shared - art.last).toBeLessThanOrEqual(lastControls - 68)
    }
  })

  it('holds screen 8 at the least size, never smaller, while its own text still fits in full above it', () => {
    // 320 × 568, bodybuilding: three rows of buttons; 300 − 112 would leave the phone absurdly small.
    const art = outlineArt({ budget: 492, tallest: 214, lastText: 158, standardControls: 68, lastControls: 180, chrome: 42 })
    expect(art.shared).toBe(492 - 68 - 214)
    expect(art.last).toBe(least)
    // Its own text one pixel taller than the room the least phone leaves: the picture goes.
    expect(outlineArt({ budget: 492, tallest: 214, lastText: 159, standardControls: 68, lastControls: 180, chrome: 42 }).last).toBe(0)
  })

  it('drops the picture where it and the full text do not both fit: no phone below the least size, no scrolling beside one', () => {
    // Large text: the tallest text is taller than the pages leave.
    expect(outlineArt({ budget: 346, tallest: 700, lastText: 300, standardControls: 68, lastControls: 68, chrome: 42 })).toEqual({ shared: 0, last: 0 })
    // Room for a phone, but smaller than its least size.
    expect(outlineArt({ budget: 400, tallest: 250, lastText: 100, standardControls: 68, lastControls: 68, chrome: 42 })).toEqual({ shared: 0, last: 0 })
    // Just the least size.
    expect(outlineArt({ budget: 68 + 250 + least, tallest: 250, lastText: 100, standardControls: 68, lastControls: 68, chrome: 42 }).shared).toBe(least)
  })

  it('screen 8 is never drawn where the other screens have no picture', () => {
    expect(outlineArt({ budget: 400, tallest: 250, lastText: 10, standardControls: 68, lastControls: 68, chrome: 42 }).last).toBe(0)
  })

  it('rounds the measured size down to whole pixels, and the least size up', () => {
    expect(outlineArt({ ...base, budget: 568.7, tallest: 200.2 }).shared).toBe(300)
    expect(outlineArt({ budget: 492, tallest: 214, lastText: 100, standardControls: 68, lastControls: 180, chrome: 42.3 }).last).toBe(Math.ceil(42.3 + OUTLINE_MIN))
  })
})
