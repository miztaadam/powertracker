import { describe, expect, it } from 'vitest'
import { outlineArt, outlineArtFor } from './outlineArt'

// The guide's pages are 500 px tall with one row of buttons; the tallest outlined text takes 200; a second button and
// its gap take 60, so with it shown the pages are 440 px tall.
const second = 60

describe('outlineArt (spec point 11)', () => {
  it('the five screens before the last share the height the pages leave below the tallest text, with no reserve', () => {
    expect(outlineArt({ pagesHeight: 500, tallest: 200, second, secondShown: false })).toEqual({ shared: 300, second })
  })

  it('is the same shared height while the second button shows (the pages one button shorter)', () => {
    expect(outlineArt({ pagesHeight: 440, tallest: 200, second, secondShown: true })).toEqual({ shared: 300, second })
  })

  it('the last screen takes the shared height with one row of buttons, and gives up only its second button otherwise', () => {
    const art = outlineArt({ pagesHeight: 500, tallest: 200, second, secondShown: false })
    expect(outlineArtFor(art, false, false)).toBe(300)
    expect(outlineArtFor(art, true, false)).toBe(300)
    expect(outlineArtFor(art, true, true)).toBe(240)
    // Never larger than the others', never by more than its button.
    for (const hasSecond of [false, true]) {
      const last = outlineArtFor(art, true, hasSecond)
      expect(last).toBeLessThanOrEqual(art.shared)
      expect(art.shared - last).toBeLessThanOrEqual(second)
    }
  })

  it('every other screen keeps the shared height whether or not the last has a second button', () => {
    const art = outlineArt({ pagesHeight: 500, tallest: 200, second, secondShown: false })
    expect(outlineArtFor(art, false, true)).toBe(300)
  })

  it('never goes below nothing: large text on a small phone leaves the picture no room', () => {
    const art = outlineArt({ pagesHeight: 150, tallest: 400, second, secondShown: false })
    expect(art.shared).toBe(0)
    expect(outlineArtFor(art, true, true)).toBe(0)
    expect(outlineArtFor(outlineArt({ pagesHeight: 300, tallest: 260, second, secondShown: false }), true, true)).toBe(0)
  })

  it('rounds down to whole pixels', () => {
    expect(outlineArt({ pagesHeight: 500.7, tallest: 200.2, second: 60.4, secondShown: false })).toEqual({ shared: 300, second: 60.4 })
    expect(outlineArtFor({ shared: 300, second: 60.4 }, true, true)).toBe(239)
  })
})
