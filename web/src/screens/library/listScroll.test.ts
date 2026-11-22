import { afterEach, describe, expect, it } from 'vitest'
import { backToTop } from './listScroll'

/** A box that remembers its scroll and is placed at `top` (its content starts `top - scrollTop` down the page). */
function box(overflowY: string, top: number, scrollTop = 0): HTMLDivElement {
  const el = document.createElement('div')
  el.style.overflowY = overflowY
  let scrolled = scrollTop
  Object.defineProperty(el, 'scrollTop', { configurable: true, get: () => scrolled, set: (v: number) => (scrolled = v) })
  el.getBoundingClientRect = () => ({ top, bottom: top + 200, left: 0, right: 160, width: 160, height: 200, x: 0, y: top, toJSON: () => ({}) })
  return el
}

/**
 * The Exercises tab as laid out: a screen that scrolls, the search field and the list in it. The field's content sits
 * 100 px down the screen; the list scrolls on its own or not, as given.
 */
function tab(listOverflow: string, screenScroll: number, listScroll = 0) {
  const screen = box('auto', 0, screenScroll)
  const field = box('visible', 100 - screenScroll)
  const list = box(listOverflow, 180 - screenScroll, listScroll)
  screen.append(field, list)
  document.body.append(screen)
  return { screen, field, list }
}

afterEach(() => document.body.replaceChildren())

describe('backToTop', () => {
  it('at other widths takes the list back to its own top and leaves the screen alone', () => {
    const { screen, field, list } = tab('auto', 40, 900)
    backToTop(list, field, false)
    expect(list.scrollTop).toBe(0)
    expect(screen.scrollTop).toBe(40)
  })

  it('on a very narrow screen, a list that scrolls on its own (the picker) goes back to its own top', () => {
    const { screen, field, list } = tab('auto', 40, 900)
    backToTop(list, field, true)
    expect(list.scrollTop).toBe(0)
    expect(screen.scrollTop).toBe(40)
  })

  it('on a very narrow screen where the whole screen scrolls, it goes back to the search field, no further', () => {
    const { screen, field, list } = tab('visible', 2400)
    backToTop(list, field, true)
    // The field's top meets the screen's top: the field stays on screen and the list starts under it.
    expect(screen.scrollTop).toBe(100)
  })

  it('leaves a screen scrolled less than that where it is', () => {
    for (const at of [0, 60, 100]) {
      const { screen, field, list } = tab('visible', at)
      backToTop(list, field, true)
      expect(screen.scrollTop).toBe(at)
      document.body.replaceChildren()
    }
  })
})
