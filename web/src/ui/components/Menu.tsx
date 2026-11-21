import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState, type CSSProperties, type KeyboardEvent, type ReactNode } from 'react'
import { cx } from './format'
import { MoreVertIcon } from './icons'
import './Button.css'
import './Menu.css'
import { MENU_MARGIN, menuRoom, placeMenu, type MenuPlacement } from './menuPlacement'

export type MenuItem = {
  text: string
  /** An icon element shown before the text, e.g. `<EditIcon />`. Drawn at 20 px. */
  icon?: ReactNode
  /** Something that cannot be undone (Delete): shown in the danger colour. */
  danger?: boolean
  onSelect: () => void
  /** A test id for the item (`data-testid`). */
  testId?: string
}

export type MenuProps = {
  /** The accessible name of the button that opens the menu ("More"). */
  label: string
  items: readonly MenuItem[]
  /** The button's icon. Default is the three dots. */
  icon?: ReactNode
  className?: string
}

const AS_BEFORE: MenuPlacement = { align: 'end', shift: 0, up: false, maxHeight: null }

/**
 * A 48 px icon button that opens a short list of actions under it. Escape, a tap outside or choosing
 * an action closes it; the arrow keys move between the actions. The list opens under the button, right edges aligned;
 * one that would leave the screen there opens where it fits (1.17.1, `placeMenu`).
 */
export function Menu({ label, items, icon = <MoreVertIcon />, className }: MenuProps) {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLSpanElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const list = useRef<HTMLDivElement>(null)
  const listId = useId()
  const [placement, setPlacement] = useState<MenuPlacement>(AS_BEFORE)

  // The placing waiting to be shown: `first` when it opens (then its first action is focused), `again` after a change of
  // height (the focus stays where it is). Kept with the placement it set, so that nothing is scrolled or focused until
  // that placement is on the page.
  const reveal = useRef<{ why: 'first' | 'again'; placement: MenuPlacement } | null>(null)

  /** Measures the menu's own size and the button's place now, and places it (`placeMenu`). */
  const place = useCallback((why: 'first' | 'again') => {
    const button = trigger.current?.getBoundingClientRect()
    const menu = list.current
    if (!button || !menu) return
    const style = getComputedStyle(menu)
    const borders = (parseFloat(style.borderTopWidth) || 0) + (parseFloat(style.borderBottomWidth) || 0)
    // Its whole height even while capped (scrollHeight holds the padding, not the border). The cap is on the whole box:
    // boxes are border-box (tokens.css).
    const placement = placeMenu(button, { width: menu.offsetWidth, height: menu.scrollHeight + borders }, menuRoom(menu))
    reveal.current = { why, placement }
    setPlacement(placement)
  }, [])

  // Placed before it is drawn: measured where it would open as before, then moved only if it leaves the screen.
  useLayoutEffect(() => {
    if (!open) {
      setPlacement(AS_BEFORE)
      return
    }
    place('first')
  }, [open, place])

  // Once the placement is on the page (not before: the click's first commit still shows the menu where it would open as
  // before): the screen scrolls just enough to bring the menu inside its room, and on opening its first action is focused
  // without any scroll of its own.
  useLayoutEffect(() => {
    const pending = reveal.current
    const menu = list.current
    if (!open || pending == null || pending.placement !== placement || !menu) return
    reveal.current = null
    const screen = menu.closest('.shell-main')
    if (screen) {
      const room = menuRoom(menu)
      const box = menu.getBoundingClientRect()
      const over = box.bottom - (room.bottom - MENU_MARGIN)
      const under = room.top + MENU_MARGIN - box.top
      // Its top first, if it cannot have both (a menu taller than the room is capped to fit, so it can).
      if (under > 0) screen.scrollTop -= under
      else if (over > 0) screen.scrollTop += over
    } else {
      menu.scrollIntoView?.({ block: 'nearest' })
    }
    if (pending.why === 'first') menu.querySelector<HTMLElement>('[role="menuitem"]')?.focus({ preventScroll: true })
  }, [open, placement])

  // Turning the phone, or a change of the layout viewport's width, closes it and gives the focus back to its button. A
  // change of height only (the on-screen keyboard coming up or going away, as it does when the menu takes the focus from
  // a field), or of the visual viewport alone (pinch zoom), keeps it open and places it again, once a frame at most.
  useEffect(() => {
    if (!open) return
    const viewport = window.visualViewport
    const width = window.innerWidth
    let frame = 0
    const close = () => {
      setOpen(false)
      trigger.current?.focus()
    }
    const onResize = () => {
      if (window.innerWidth !== width) {
        close()
        return
      }
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(() => place('again'))
    }
    window.addEventListener('resize', onResize)
    window.addEventListener('orientationchange', close)
    viewport?.addEventListener('resize', onResize)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('resize', onResize)
      window.removeEventListener('orientationchange', close)
      viewport?.removeEventListener('resize', onResize)
    }
  }, [open, place])

  useEffect(() => {
    if (!open) return
    const onPress = (e: PointerEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', onPress)
    return () => document.removeEventListener('pointerdown', onPress)
  }, [open])

  const onKeyDown = (e: KeyboardEvent) => {
    if (!open) return
    if (e.key === 'Escape') {
      setOpen(false)
      trigger.current?.focus()
      return
    }
    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return
    e.preventDefault()
    const entries = Array.from(list.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [])
    const at = entries.indexOf(document.activeElement as HTMLElement)
    entries[(at + (e.key === 'ArrowDown' ? 1 : -1) + entries.length) % entries.length]?.focus()
  }

  return (
    <span ref={root} className={cx('gr-menu', className)} onKeyDown={onKeyDown}>
      <button ref={trigger} type="button" className="gr-icon-btn" aria-label={label} aria-haspopup="menu" aria-expanded={open} aria-controls={open ? listId : undefined} onClick={() => setOpen(!open)}>
        {icon}
      </button>
      {open && (
        <div
          ref={list}
          id={listId}
          className={cx('gr-menu-list', placement.align === 'start' && 'gr-menu-list-start', placement.up && 'gr-menu-list-up')}
          style={
            placement.shift !== 0 || placement.maxHeight != null
              ? ({
                  ...(placement.shift !== 0 ? { transform: `translateX(${placement.shift}px)` } : {}),
                  // Taller than the room it has either way: as tall as that room, and it scrolls (focus moves the items into view).
                  ...(placement.maxHeight != null ? { maxHeight: `${placement.maxHeight}px`, overflowY: 'auto' } : {}),
                } as CSSProperties)
              : undefined
          }
          role="menu"
          aria-label={label}
        >
          {items.map((item) => (
            <button
              key={item.text}
              type="button"
              role="menuitem"
              data-testid={item.testId}
              className={cx('gr-menu-item body-large', item.danger && 'gr-menu-item-danger')}
              onClick={() => {
                setOpen(false)
                trigger.current?.focus()
                item.onSelect()
              }}
            >
              {item.icon != null && <span className="gr-menu-item-icon">{item.icon}</span>}
              {item.text}
            </button>
          ))}
        </div>
      )}
    </span>
  )
}
