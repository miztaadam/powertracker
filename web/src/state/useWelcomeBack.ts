import { liveQuery } from 'dexie'
import { useCallback, useEffect, useRef, useState } from 'react'
import { planFor } from '../db/catalogue'
import { clearLeftOn, getSettings, type GritDb } from '../db/db'
import type { TrainingMode } from '../db/schema'
import { grit } from '../domain'

export type WelcomeBack = {
  /** True while the card is due: this side has a plan and was left more than 30 days ago (spec §7.7). */
  show: boolean
  /** "Resume my plan": the card goes; the first week back is eased in by the coach. */
  resume: () => Promise<void>
  /** "My goals have changed": the card goes, and the caller opens this side's Plan (on the bodybuilding side, its builder). */
  goalsChanged: () => Promise<void>
  /** The side the card is for: the side the app is on. */
  side: TrainingMode
}

type Stored = { show: boolean; side: TrainingMode }

async function readStored(db: GritDb, today: string): Promise<Stored> {
  const settings = await getSettings(db)
  const side = settings.trainingMode
  const leftOn = side === 'BODYBUILDING' ? settings.bodybuildingLeftOn : settings.powerliftingLeftOn
  // A side entered for the first time has never been left: no card.
  if (leftOn == null) return { show: false, side }
  const hasPlan = (await planFor(db, side)).length > 0
  return { show: grit.welcomeBack(leftOn, today, hasPlan), side }
}

/**
 * The welcome-back card of the side the app is on, read live: on Today and at the top of Coach until either button
 * is tapped. Either answer forgets the day the side was left, so the card shows once. `today` is the real clock.
 */
export function useWelcomeBack(db: GritDb, today: () => string): WelcomeBack {
  const clock = useRef(today)
  clock.current = today
  const [stored, setStored] = useState<Stored>({ show: false, side: 'POWERLIFTING' })
  const [answered, setAnswered] = useState(false)
  useEffect(() => {
    // A failed read leaves the card away; the screen's own data reports a broken database.
    const subscription = liveQuery(() => readStored(db, clock.current())).subscribe({ next: setStored, error: () => {} })
    return () => subscription.unsubscribe()
  }, [db])

  const answer = useCallback(async () => {
    // Gone at once; if storing fails it comes back with the next read.
    setAnswered(true)
    try {
      await clearLeftOn(db, (await getSettings(db)).trainingMode)
    } finally {
      setAnswered(false)
    }
  }, [db])

  return { show: stored.show && !answered, resume: answer, goalsChanged: answer, side: stored.side }
}
