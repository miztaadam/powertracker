/** Whether a box scrolls up and down on its own (its overflow, as styled). */
export const scrollsOnItsOwn = (el: Element) => ['auto', 'scroll'].includes(getComputedStyle(el).overflowY)

/** The nearest box round `el` that scrolls up and down, or null. */
export function scrollerAround(el: Element): HTMLElement | null {
  for (let box = el.parentElement; box != null; box = box.parentElement) if (scrollsOnItsOwn(box)) return box
  return null
}

/**
 * The media query of a very narrow screen (a 320 px phone at 200 % text), where the Exercises tab and the plan builder
 * scroll as a whole (narrowTabs.css, narrowDeeper.css). Read once; null where the page has no `matchMedia`.
 */
const veryNarrowQuery = typeof window !== 'undefined' && typeof window.matchMedia === 'function' ? window.matchMedia('(max-width: 239px)') : null
export const isVeryNarrow = () => veryNarrowQuery?.matches === true

/**
 * Takes the exercise list back to its top for a new search or filter.
 *
 * Where the list scrolls on its own (every width but a very narrow one, and the picker at that width too) that is the
 * list's own top, as before. Where it does not (the Exercises tab and the plan builder on a very narrow screen, where
 * the whole screen scrolls with the search and the chips in it), the screen is scrolled back, but never past the search
 * field: at most to where the field's top meets the screen's top, so the field being typed in stays on screen and the
 * list starts under it. A screen scrolled less than that is left where it is.
 *
 * Only a very narrow screen reads any style or layout beyond the list's own reset, so the search costs no more at
 * other widths.
 */
export function backToTop(list: HTMLElement, field: HTMLElement | null, veryNarrow = isVeryNarrow()) {
  if (!veryNarrow || scrollsOnItsOwn(list)) {
    list.scrollTop = 0
    return
  }
  const screen = scrollerAround(list)
  if (screen == null || field == null) return
  const fieldTop = field.getBoundingClientRect().top - screen.getBoundingClientRect().top + screen.scrollTop
  if (screen.scrollTop > fieldTop) screen.scrollTop = Math.max(0, fieldTop)
}
