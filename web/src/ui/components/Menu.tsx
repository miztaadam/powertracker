import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState, type CSSProperties, type KeyboardEvent, type ReactNode } from 'react'
import { cx } from './format'
import { MoreVertIcon } from './icons'
import './Button.css'
import './Menu.css'
import { placeMenu, type MenuPlacement } from './menuPlacement'

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
 * The room a menu has: the window, above the rest bar and the tab bar, and below the top of the screen it scrolls in,
 * past that screen's safe-area padding (the status bar, the notch, the iOS Home Screen app's extra band).
 */
function menuRoom(from: Element) {
  const docked = [...document.querySelectorAll('.shell-rest, .gr-tabbar')].map((el) => el.getBoundingClientRect().top).filter((top) => top > 0)
  const screen = from.closest('.shell-main')
  const top = screen ? screen.getBoundingClientRect().top + parseFloat(getComputedStyle(screen).paddingTop || '0') : 0
  return { left: 0, top: Math.max(0, top), right: window.innerWidth, bottom: Math.min(window.innerHeight, ...docked) }
}

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

  // `first`: the placing when it opens (then it is brought into view and its first action focused); else a placing again
  // after a change of height, which brings it into view and leaves the focus where it is.
  const reveal = useRef<'first' | 'again' | null>(null)

  /** Measures the menu's own size and the button's place now, and places it (`placeMenu`). */
  const place = useCallback((why: 'first' | 'again') => {
    const button = trigger.current?.getBoundingClientRect()
    const menu = list.current
    if (!button || !menu) return
    const style = getComputedStyle(menu)
    const px = (key: 'paddingTop' | 'paddingBottom' | 'borderTopWidth' | 'borderBottomWidth') => parseFloat(style[key]) || 0
    const borders = px('borderTopWidth') + px('borderBottomWidth')
    // Its whole height even while capped (scrollHeight holds the padding, not the border).
    const where = placeMenu(button, { width: menu.offsetWidth, height: menu.scrollHeight + borders }, menuRoom(menu))
    // The cap is on the whole box: max-height counts the content only, so take the padding and the border off.
    const chrome = px('paddingTop') + px('paddingBottom') + borders
    reveal.current = why
    setPlacement(where.maxHeight != null ? { ...where, maxHeight: Math.max(0, where.maxHeight - chrome) } : where)
  }, [])

  // Placed before it is drawn: measured where it would open as before, then moved only if it leaves the screen.
  useLayoutEffect(() => {
    if (!open) {
      setPlacement(AS_BEFORE)
      return
    }
    place('first')
  }, [open, place])

  // A change of the window's width, or turning the phone, closes it. A change of height only (the on-screen keyboard
  // coming up or going away, as it does when the menu takes the focus from a field) keeps it open and places it again,
  // once per frame however many events come.
  useEffect(() => {
    if (!open) return
    const viewport = window.visualViewport
    const widths = () => [window.innerWidth, viewport?.width ?? null]
    const opened = widths()
    let frame = 0
    const onResize = () => {
      const now = widths()
      if (now[0] !== opened[0] || now[1] !== opened[1]) {
        setOpen(false)
        return
      }
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(() => place('again'))
    }
    const close = () => setOpen(false)
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

  // Once it is placed (the render after a placing): brought into view, and, when it has just opened, its first action
  // focused.
  useEffect(() => {
    if (!open || reveal.current == null) return
    const why = reveal.current
    reveal.current = null
    list.current?.scrollIntoView?.({ block: 'nearest' })
    if (why === 'first') list.current?.querySelector<HTMLElement>('[role="menuitem"]')?.focus()
  }, [open, placement])

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
