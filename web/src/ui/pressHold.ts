/**
 * Makes a quick tap show its sink. On a touch screen the finger can go down and up within one frame, so `:active`
 * starts and ends before anything is drawn and press.css never shows the press. One listener on the document (not one
 * per row) puts `PRESSING` on the pressed target for at least `PRESS_HOLD_MS` from the finger going down; press.css
 * treats it as pressed. A press that turns into a scroll (pointercancel) lets go at once, so the item springs back.
 * Disabled controls, and everything while motion is off, are left alone.
 */

export const PRESSING = 'gr-pressing'
export const PRESS_HOLD_MS = 90

/** The controls press.css sinks, as their own pressed element (the tab, the Today row and the Settings row stand in for what sinks). */
export const PRESS_TARGETS = [
  '.gr-btn',
  '.gr-icon-btn',
  '.gr-segment',
  '.gr-chip',
  '.gr-step-btn',
  '.gr-stat-button',
  'button.gr-set',
  '.gr-switch',
  '.gr-menu-item',
  '.gr-snackbar-action',
  '.shell-update',
  '.lib-row-main',
  '.lib-dropdown',
  '.lib-ticked-chip',
  '.lib-muscle',
  '.lib-option',
  '.bb-row',
  '.gr-tab',
  '.today-row',
  'label.settings-row',
].join(', ')

const NOT_PRESSABLE = ":disabled, [aria-disabled='true'], .lib-option-disabled"

/** Starts listening on `root`; returns the function that stops it. */
export function installPressHold(root: Document = document): () => void {
  let pressed: Element | null = null
  let downAt = 0
  let timer: ReturnType<typeof setTimeout> | undefined

  const letGo = () => {
    clearTimeout(timer)
    timer = undefined
    pressed?.classList.remove(PRESSING)
    pressed = null
  }
  const onDown = (event: Event) => {
    letGo()
    if (root.documentElement.getAttribute('data-motion') === 'off') return
    const target = event.target instanceof Element ? event.target.closest(PRESS_TARGETS) : null
    if (target == null || target.matches(NOT_PRESSABLE)) return
    pressed = target
    downAt = performance.now()
    target.classList.add(PRESSING)
  }
  const onUp = () => {
    if (pressed == null) return
    clearTimeout(timer)
    timer = setTimeout(letGo, Math.max(0, downAt + PRESS_HOLD_MS - performance.now()))
  }

  const options = { capture: true, passive: true }
  root.addEventListener('pointerdown', onDown, options)
  root.addEventListener('pointerup', onUp, options)
  root.addEventListener('pointercancel', letGo, options)
  return () => {
    root.removeEventListener('pointerdown', onDown, options)
    root.removeEventListener('pointerup', onUp, options)
    root.removeEventListener('pointercancel', letGo, options)
    letGo()
  }
}
