import { describe, expect, it } from 'vitest'
import { placeMenu, type Box } from './menuPlacement'

// 1.17.1: a menu opens under its button, right edges aligned, as before; only a menu that would leave the room it has
// (the screen, above the docked bars) moves.
const box = (left: number, top: number, width: number, height: number): Box => ({ left, top, right: left + width, bottom: top + height })
const room = box(0, 0, 306, 580)
const size = { width: 266, height: 114 }

describe('placeMenu', () => {
  it('a menu that fits opens as before: under the button, right edges aligned', () => {
    expect(placeMenu(box(250, 100, 48, 48), size, room)).toEqual({ align: 'end', shift: 0, up: false, maxHeight: null })
  })

  it('no room to the left of the button: it lines up with the button left edge instead', () => {
    expect(placeMenu(box(24, 100, 48, 48), size, room)).toEqual({ align: 'start', shift: 0, up: false, maxHeight: null })
  })

  it('no room either way: it is moved over until it is inside, a margin from the edge', () => {
    // End-aligned it would start at 175 + 48 - 266 = -43; start-aligned it would end at 175 + 266 = 441.
    expect(placeMenu(box(175, 100, 48, 48), size, room)).toEqual({ align: 'end', shift: 51, up: false, maxHeight: null })
  })

  it('wider than the room: it starts at the margin', () => {
    expect(placeMenu(box(100, 100, 48, 48), { width: 400, height: 114 }, room)).toEqual({ align: 'end', shift: 8 - (148 - 400), up: false, maxHeight: null })
  })

  it('no room under the button: it opens above it', () => {
    expect(placeMenu(box(250, 500, 48, 48), size, room)).toEqual({ align: 'end', shift: 0, up: true, maxHeight: null })
  })

  it('room neither under nor above: it opens under, as before, and the screen scrolls to it', () => {
    expect(placeMenu(box(250, 60, 48, 48), { width: 266, height: 560 }, room)).toEqual({ align: 'end', shift: 0, up: false, maxHeight: null })
    expect(placeMenu(box(250, 460, 48, 48), { width: 266, height: 560 }, room)).toEqual({ align: 'end', shift: 0, up: false, maxHeight: null })
  })

  it('taller than the whole room: capped at the room less its margins, and it scrolls', () => {
    expect(placeMenu(box(250, 60, 48, 48), { width: 266, height: 700 }, room)).toEqual({ align: 'end', shift: 0, up: false, maxHeight: 564 })
    expect(placeMenu(box(250, 60, 48, 48), { width: 266, height: 700 }, box(0, 47, 306, 533))).toEqual({ align: 'end', shift: 0, up: false, maxHeight: 517 })
  })

  it('exactly 8 px from the edge is inside', () => {
    // End-aligned: 274 - 266 = 8, the margin itself.
    expect(placeMenu(box(226, 100, 48, 48), size, room)).toEqual({ align: 'end', shift: 0, up: false, maxHeight: null })
    // One pixel more is not: start-aligned from 225 would end at 491, so it is moved over by 1.
    expect(placeMenu(box(225, 100, 48, 48), size, room)).toEqual({ align: 'end', shift: 1, up: false, maxHeight: null })
    // Under: ends exactly 8 px above the room's bottom.
    expect(placeMenu(box(250, 414, 48, 48), size, room)).toEqual({ align: 'end', shift: 0, up: false, maxHeight: null })
  })

  it('moved over and opened above at once', () => {
    expect(placeMenu(box(175, 500, 48, 48), size, room)).toEqual({ align: 'end', shift: 51, up: true, maxHeight: null })
  })

  it('a button near the right edge opens as before', () => {
    expect(placeMenu(box(258, 100, 48, 48), size, room)).toEqual({ align: 'end', shift: 0, up: false, maxHeight: null })
  })

  it('the room can start below the top of the window (the status bar): an upward menu stays under it', () => {
    const below = box(0, 47, 306, 533)
    // Above would start at 104 + 4 - 114 = -6 in the window, 53 px over the room's top: it opens under instead.
    expect(placeMenu(box(250, 104, 48, 48), size, below)).toEqual({ align: 'end', shift: 0, up: false, maxHeight: null })
    // With room above it inside the room it goes up.
    expect(placeMenu(box(250, 500, 48, 48), size, below)).toEqual({ align: 'end', shift: 0, up: true, maxHeight: null })
  })
})
