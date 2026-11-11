import { liveQuery } from 'dexie'
import { useEffect, useMemo, useRef, useState } from 'react'
import { loadHistory, type GritDb } from '../db/db'
import { grit, type JsHistory, type JsPr, type JsSet } from '../domain'
import type { Lift } from '../router'

/** `index` counts from 1 within the exercise in that session. */
export type HistorySet = { index: number; set: JsSet; isPr: boolean }

/** The sets of one exercise in a session: the lift itself or one of its variations. */
export type HistoryGroup = { exerciseName: string; isVariation: boolean; sets: HistorySet[] }

export type HistorySession = { sessionId: number; date: string; groups: HistoryGroup[] }

export type LiftHistoryState = {
  loading: boolean
  currentE1rmKg: number | null
  best: JsPr | null
  /** Newest first. */
  sessions: HistorySession[]
}

const E1RM_DAYS = 28
const LOADING: LiftHistoryState = { loading: true, currentE1rmKg: null, best: null, sessions: [] }

/** Every session with a set of `lift` or one of its variations, newest first, with the lift's records marked. */
export function buildLiftHistory(history: JsHistory, lift: Lift, today: string): LiftHistoryState {
  // Everything that trains the lift: the lift itself (main) and its variations.
  const trains = history.exercises.filter((e) => grit.mainLiftOf(e) === lift)
  const main = new Map(trains.filter((e) => grit.isMainLift(e)).map((e) => [e.id, e.name]))
  const variations = new Map(trains.filter((e) => !grit.isMainLift(e)).map((e) => [e.id, e.name]))

  // Walks the sets in the order the domain's `prs` does, so each record lands on the set that made it.
  const prs = grit.prs(history, lift)
  let nextPr = 0
  const sessions = [...history.sessions]
    .sort((a, b) => a.date.localeCompare(b.date))
    .flatMap((session): HistorySession[] => {
      const groups = new Map<number, HistorySet[]>()
      for (const set of session.sets) {
        if (!main.has(set.exerciseId) && !variations.has(set.exerciseId)) continue
        const pr = prs[nextPr]
        const isPr =
          pr != null &&
          !set.failed &&
          main.has(set.exerciseId) &&
          pr.date === session.date &&
          pr.weightKg === set.weightKg &&
          pr.reps === set.reps &&
          (pr.rpe ?? null) === (set.rpe ?? null)
        if (isPr) nextPr++
        const sets = groups.get(set.exerciseId) ?? []
        groups.set(set.exerciseId, sets)
        sets.push({ index: sets.length + 1, set, isPr })
      }
      if (groups.size === 0) return []
      const named = [...groups].map(([id, sets]): HistoryGroup => ({ exerciseName: main.get(id) ?? variations.get(id) ?? '', isVariation: !main.has(id), sets }))
      // The lift itself first, each in the order it was first logged.
      return [{ sessionId: session.id, date: session.date, groups: [...named.filter((g) => !g.isVariation), ...named.filter((g) => g.isVariation)] }]
    })

  return {
    loading: false,
    currentE1rmKg: grit.currentE1rm(history, lift, today, E1RM_DAYS) ?? null,
    best: prs.at(-1) ?? null,
    sessions: sessions.reverse(),
  }
}

/** One lift's sessions, read live from the database. `today` is the real clock. */
export function useLiftHistory(db: GritDb, lift: Lift, today: () => string): LiftHistoryState {
  const clock = useRef(today)
  clock.current = today
  const [stored, setStored] = useState<{ today: string; history: JsHistory }>()
  const [readError, setReadError] = useState<unknown>(null)
  useEffect(() => {
    const subscription = liveQuery(async () => ({ today: clock.current(), history: await loadHistory(db) })).subscribe({ next: setStored, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db])
  // A failed read is for the app's error boundary, not for a history that looks empty.
  if (readError != null) throw readError

  return useMemo(() => (stored ? buildLiftHistory(stored.history, lift, stored.today) : LOADING), [stored, lift])
}
