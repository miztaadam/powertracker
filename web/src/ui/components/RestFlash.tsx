import { useEffect, useState } from 'react'
import './RestFlash.css'

/** Longer than either animation. Once it has played the overlay is clear and takes no taps, so it can wait. */
const REMOVE_AFTER_MS = 1200

/**
 * The flash that marks the end of a rest: a 300 ms accent overlay over the whole screen, or a slow
 * fade where the lifter asked for reduced motion. `showId` 0 shows nothing; each new id flashes once.
 * It never takes a tap and says nothing to a screen reader.
 */
export function RestFlash({ showId }: { showId: number }) {
  const [endedId, setEndedId] = useState(0)
  useEffect(() => {
    if (showId === 0) return
    const timer = setTimeout(() => setEndedId(showId), REMOVE_AFTER_MS)
    return () => clearTimeout(timer)
  }, [showId])
  if (showId === 0 || showId === endedId) return null
  return <div key={showId} className="gr-rest-flash" data-testid="rest-flash" aria-hidden="true" />
}
