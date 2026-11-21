import { describe, expect, it } from 'vitest'
import { placeMenu, type Box } from './menuPlacement'

// 1.17.1: a menu opens under its button, right edges aligned, as before; only a menu that would leave the room it has
// (the screen, above the docked bars) moves.
const box = (left: number, top: number, width: number, height: number): Box => ({ left, top, right: left + width, bottom: top + height })
const room = box(0, 0, 306, 580)
const size = { width: 266, height: 114 }

describe('placeMenu', () => {
  it('a menu that fits opens as before: under the button, right edges aligned', () => {
    expect(placeMenu(box(250, 100, 48, 48), size, room)).toEqual({ align: 'end', shift: 0, up: false })
  })

  it('no room to the left of the button: it lines up with the button left edge instead', () => {
    expect(placeMenu(box(24, 100, 48, 48), size, room)).toEqual({ align: 'start', shift: 0, up: false })
  })

  it('no room either way: it is moved over until it is inside, a margin from the edge', () => {
    // End-aligned it would start at 175 + 48 - 266 = -43; start-aligned it would end at 175 + 266 = 441.
    expect(placeMenu(box(175, 100, 48, 48), size, room)).toEqual({ align: 'end', shift: 51, up: false })
  })

  it('wider than the room: it starts at the margin', () => {
    expect(placeMenu(box(100, 100, 48, 48), { width: 400, height: 114 }, room)).toEqual({ align: 'end', shift: 8 - (148 - 400), up: false })
  })

  it('no room under the button: it opens above it', () => {
    expect(placeMenu(box(250, 500, 48, 48), size, room)).toEqual({ align: 'end', shift: 0, up: true })
  })

  it('no room above either: it opens under, as before (the screen scrolls to it)', () => {
    expect(placeMenu(box(250, 60, 48, 48), { width: 266, height: 560 }, room)).toEqual({ align: 'end', shift: 0, up: false })
  })
})
