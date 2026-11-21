import { useEffect, useRef, useState, type MouseEvent } from 'react'

/**
 * How long, in ms, a sheet opened from a row menu ignores taps on its answers once it is open: it opens under the finger
 * that chose the menu item, where a double tap's second tap would land (as Plan's add form, ADD_FORM_TAP_WINDOW_MS).
 * Counted from the end of its slide up, as on Android, so the slide does not use the moment up; taps meanwhile are
 * ignored too. The skip sheet's answers (1.13) and the plate sheet's choices (1.17) both use it.
 */
export const SHEET_TAP_WINDOW_MS = 300

/** The time now, on the clock events are stamped with (as Plan's add form reads it). */
const eventClock = () => new Event('now').timeStamp

/**
 * Holds early taps on a sheet's answers: put `ref` on a wrapper around the sheet and `onClickCapture` on it. A click made
 * by a tap (detail 1 or more) inside `answers` (a selector), while the sheet slides up or within SHEET_TAP_WINDOW_MS after,
 * stops there and does nothing (it neither reaches the answer nor checks a radio). A tap elsewhere (outside the sheet,
 * on Done) is not held, and nor is the keyboard (Enter, Space), as on Plan's add form.
 */
export function useEarlyTapGuard(answers: string) {
  // When the sheet finished sliding up, on the events' clock; null while it is still moving.
  const [openedAt, setOpenedAt] = useState<number | null>(null)
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    let gone = false
    const panel = ref.current?.querySelector('.gr-dialog')
    const moving = panel?.getAnimations?.() ?? []
    Promise.all(moving.map((a) => a.finished.catch(() => {}))).then(() => {
      if (!gone) setOpenedAt(eventClock())
    })
    return () => {
      gone = true
    }
  }, [])
  const onClickCapture = (e: MouseEvent<HTMLDivElement>) => {
    if (e.detail === 0 || (openedAt != null && e.timeStamp - openedAt >= SHEET_TAP_WINDOW_MS)) return
    const target = e.target as Element
    if (target.closest(answers) == null) return
    e.stopPropagation()
    // A radio's own check (or a label's click through to it) is undone too.
    e.preventDefault()
  }
  return { ref, onClickCapture }
}
