/** A box on screen, in CSS px from the window's top left. */
export type Box = { left: number; top: number; right: number; bottom: number }

/**
 * Where a menu opens beside its button (1.17.1). `align: 'end'` lines the menu's right edge up with the button's, as
 * menus always opened; `'start'` its left edge with the button's. `shift` moves it right by that many px. `up` opens it
 * above the button instead of under it.
 */
export type MenuPlacement = { align: 'start' | 'end'; shift: number; up: boolean }

/** How far a menu keeps from the edge of its room. */
export const MENU_MARGIN = 8

/** How far the menu overlaps its button (the stylesheet's `--space-2`), above or below. */
const OVERLAP = 4

/**
 * Places a menu of `size` for the button at `trigger`, inside `room` (the window, above the docked bars). A menu that
 * fits opens as it always did: under the button, right edges aligned. Only one that would leave the room moves: lined up
 * with the button's left edge, else moved over until it is inside; and above the button when it has no room under it
 * but has room above. When it has room neither way it opens under, and the screen scrolls to it.
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
  const downBottom = trigger.bottom - OVERLAP + size.height
  const upTop = trigger.top + OVERLAP - size.height
  const up = downBottom > room.bottom - margin && upTop >= room.top + margin
  return { align, shift, up }
}
