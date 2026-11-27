/** A box on screen, in CSS px from the window's top left. */
export type Box = { left: number; top: number; right: number; bottom: number }

/**
 * Where a menu opens beside its button (1.17.1). `align: 'end'` lines the menu's right edge up with the button's, as
 * menus always opened; `'start'` its left edge with the button's. `shift` moves it right by that many px. `up` opens it
 * above the button instead of under it. `maxHeight`, when set, caps its height (it then scrolls).
 */
export type MenuPlacement = { align: 'start' | 'end'; shift: number; up: boolean; maxHeight: number | null }

/** How far a menu keeps from the edge of its room. */
export const MENU_MARGIN = 8

/** How far the menu overlaps its button (the stylesheet's `--space-2`), above or below. */
const OVERLAP = 4

/**
 * Places a menu of `size` for the button at `trigger`, inside `room` (the window, above the docked bars). A menu that
 * fits opens as it always did: under the button, right edges aligned. Only one that would leave the room moves: lined up
 * with the button's left edge, else moved over until it is inside; and above the button when it has no room under it
 * but has room above. When it has room neither way it opens under, and the screen scrolls to it; one taller than the
 * whole room is capped at the room's height and scrolls, so it is never cut.
 */
export function placeMenu(trigger: Box, size: { width: number; height: number }, room: Box, margin = MENU_MARGIN): MenuPlacement {
  const minLeft = room.left + margin
  const maxRight = room.right - margin
  let align: MenuPlacement['align'] = 'end'
  let shift = 0
  const endLeft = trigger.right - size.width
  if (endLeft < minLeft) {
    if (trigger.left + size.width <= maxRight) align = 'start'
    else shift = minLeft - endLeft
  }
  const below = room.bottom - margin - (trigger.bottom - OVERLAP)
  const above = trigger.top + OVERLAP - (room.top + margin)
  if (size.height <= below) return { align, shift, up: false, maxHeight: null }
  if (size.height <= above) return { align, shift, up: true, maxHeight: null }
  // Room on neither side: under the button, as before, and the screen scrolls to it; capped at the room's height (and
  // scrolling itself) only when it is taller than the whole room.
  const tallest = room.bottom - room.top - 2 * margin
  return { align, shift, up: false, maxHeight: size.height > tallest ? Math.max(0, tallest) : null }
}

/**
 * The room a menu has (1.17.1): inside the screen it scrolls in, past that screen's padding on every side (the safe
 * area: the status bar, the notch, the iOS Home Screen app's extra band, a landscape phone's side insets), and above the
 * rest bar and the tab bar. Outside the app shell, the window.
 */
export function menuRoom(from: Element): Box {
  const docked = [...document.querySelectorAll('.shell-rest, .gr-tabbar')].map((el) => el.getBoundingClientRect().top).filter((top) => top > 0)
  let room: Box = { left: 0, top: 0, right: window.innerWidth, bottom: Math.min(window.innerHeight, ...docked) }
  const screen = from.closest('.shell-main')
  if (screen) {
    const box = screen.getBoundingClientRect()
    const style = getComputedStyle(screen)
    const pad = (value: string) => parseFloat(value) || 0
    room = {
      left: Math.max(room.left, box.left + pad(style.paddingLeft)),
      top: Math.max(room.top, box.top + pad(style.paddingTop)),
      right: Math.min(room.right, box.right - pad(style.paddingRight)),
      bottom: Math.min(room.bottom, box.bottom),
    }
  }
  return room
}

/**
 * Where a menu's button is on the screen it scrolls in (1.23.2): its place in the window plus how far that screen is
 * scrolled (outside the app shell, the page). Scrolling leaves it as it is; anything drawn or taken away before the
 * button, or a change of its row's layout, changes it.
 */
export function buttonSpot(button: Element): { left: number; top: number } {
  const box = button.getBoundingClientRect()
  const screen = button.closest('.shell-main')
  return { left: box.left + (screen ? screen.scrollLeft : window.scrollX), top: box.top + (screen ? screen.scrollTop : window.scrollY) }
}
