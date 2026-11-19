import { useEffect, useReducer, useRef } from 'react'
import { Snackbar } from '../../ui/components/Snackbar'

/**
 * Failed saves, held by whoever opened the dialogs that can say them (Plan's add form for Change's picker and its create
 * form) until one of them has said it through. Not an event checked against which dialog is open at one moment: a
 * dialog that closes before its message has run its course, or that another dialog comes over, leaves it held, and the
 * dialog on top then says it. So a failure is said once wherever the lifter is looking, whenever Close lands.
 */
export type HeldFailures = {
  /** Every failure so far. */
  count: number
  /** A failure no dialog has said through yet. */
  unsaid: boolean
  add: () => void
  /** Every failure up to `upTo` has been said through. */
  said: (upTo: number) => void
}

/**
 * Held failures, for the component that owns them. `onGone` is told of a failure still held when that component unmounts,
 * and of one that comes after: nothing of its own can say it any more.
 */
export function useHeldFailures(onGone?: () => void): HeldFailures {
  // Kept in refs, so a failure and an unmount in the same moment (before any render) still see each other.
  const total = useRef(0)
  const saidUpTo = useRef(0)
  const mounted = useRef(true)
  const gone = useRef(onGone)
  gone.current = onGone
  const [, rerender] = useReducer((n: number) => n + 1, 0)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
      if (total.current > saidUpTo.current) {
        saidUpTo.current = total.current
        gone.current?.()
      }
    }
  }, [])
  return {
    count: total.current,
    unsaid: total.current > saidUpTo.current,
    add: () => {
      if (!mounted.current) return void gone.current?.()
      total.current += 1
      rerender()
    },
    said: (upTo) => {
      if (upTo <= saidUpTo.current) return
      saidUpTo.current = upTo
      rerender()
    },
  }
}

const SAVE_FAILED = "Couldn't save — try again."

/**
 * The message for `failures`, inside a dialog so that it is drawn over that dialog's scrim. Shown while `here` (this
 * dialog is the one on top) and a failure is held; once it has been up for its time, every failure up to it is said. Should
 * the dialog close first, or `here` turn false, the failure stays held for the dialog on top.
 */
export function HeldMessage({ failures, here }: { failures: HeldFailures; here: boolean }) {
  const upTo = failures.count
  return <Snackbar message={here && failures.unsaid ? SAVE_FAILED : null} showId={upTo} onDismiss={() => failures.said(upTo)} />
}
