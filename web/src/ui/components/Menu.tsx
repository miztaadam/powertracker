import { useEffect, useId, useLayoutEffect, useRef, useState, type CSSProperties, type KeyboardEvent, type ReactNode } from 'react'
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

const AS_BEFORE: MenuPlacement = { align: 'end', shift: 0, up: false }

/** The room a menu has: the window, above the rest bar and the tab bar. */
function menuRoom() {
  const docked = [...document.querySelectorAll('.shell-rest, .gr-tabbar')].map((el) => el.getBoundingClientRect().top).filter((top) => top > 0)
  return { left: 0, top: 0, right: window.innerWidth, bottom: Math.min(window.innerHeight, ...docked) }
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

  // Placed before it is drawn: measured where it would open as before, then moved only if it leaves the screen.
  useLayoutEffect(() => {
    if (!open) {
      setPlacement(AS_BEFORE)
      return
    }
    const button = trigger.current?.getBoundingClientRect()
    const menu = list.current
    if (!button || !menu) return
    setPlacement(placeMenu(button, { width: menu.offsetWidth, height: menu.offsetHeight }, menuRoom()))
  }, [open])

  useEffect(() => {
    if (!open) return
    list.current?.scrollIntoView?.({ block: 'nearest' })
    list.current?.querySelector<HTMLElement>('[role="menuitem"]')?.focus()
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
          style={placement.shift !== 0 ? ({ transform: `translateX(${placement.shift}px)` } as CSSProperties) : undefined}
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
