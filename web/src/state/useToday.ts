import { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { getSettings, loadHistory, type AppSettings, type GritDb } from '../db/db'
import { adviceFromJs, adviceToJs, planItemToJs } from '../db/mapping'
import type { AdviceRow, PlanItemRow, SetRow } from '../db/schema'
import { grit, JsCoachSettings, type JsExercise, type JsHistory, type JsSet } from '../domain'

/** The numbers in the steppers for the next set. `weightKg` is null until the lifter enters one. */
export type SetEditor = { weightKg: number | null; reps: number; rpe: number }

export type StoredSet = SetRow & { id: number }
export type PlannedItem = PlanItemRow & { id: number }

export type MissPoint = 'FLOOR' | 'MID' | 'LOCKOUT'

/** The coach's numbers for one exercise today; `sets` is set only when the coach changes the set count. */
export type Target = { exerciseId: number; weightKg: number; reps: number; rpe: number; sets: number | null }

/** An OPEN target for today. Changes nothing until applied. */
export type Suggestion = { id: number; reason: string; target: Target }

export type TodayItem = {
  exercise: JsExercise
  /** Today's plan item, or null for an exercise added during the session. */
  planned: PlannedItem | null
  suggestion: Suggestion | null
  sets: StoredSet[]
  editor: SetEditor
  /** Sets to do: the plan's, or an applied coach target's. Null for an added exercise. */
  targetSets: number | null
  isComplete: boolean
  /** Plates for the editor weight; null when there is no weight or it cannot be loaded exactly. */
  perSide: number[] | null
  /** The weight a typo is judged against: the last set today, else the last top set before today. */
  lastWeightKg: number | null
}

/** What the lifter did, as the editor has it. Never a top set: that is worked out from the session afterwards. */
export type SetInput = { weightKg: number; reps: number; rpe: number | null; failed: boolean }

export type InputWarning = 'HIGH_REPS' | 'BIG_JUMP'

/** A set held back because it looks like a typo; saved only after the lifter confirms. */
export type PendingLog = { exerciseId: number; set: SetInput; warnings: InputWarning[]; lastWeightKg: number | null }

export type FinishSummary = { setCount: number; exerciseCount: number; newNotes: number }

export type TodayState = {
  /** The day this screen logs to, `YYYY-MM-DD`. */
  date: string
  loading: boolean
  sessionId: number | null
  items: TodayItem[]
  pending: PendingLog | null
  summary: FinishSummary | null
  /** Exercises that can still be added to this session. */
  addable: JsExercise[]
  weightStepKg: number
  changePlates: boolean
  /** How many times today's targets have been worked out and stored (on open, again when the plan changes). */
  coachPasses: number
}

export type TodayActions = {
  /** Starts the session for the clock's day; if that is no longer the day on screen, the screen moves to it. */
  startSession: (bodyweightKg: number | null, readiness: number | null) => Promise<void>
  /** Changes the numbers for the next set of an exercise. No set is saved. */
  edit: (exerciseId: number, editor: SetEditor) => void
  /**
   * Saves `set` for the item with this key (its exercise id). Unless `confirmed`, a set that looks
   * like a typo is held in `pending` instead. Rejects when the write fails or there is no session.
   * The set goes into the open session also after midnight: a session belongs to the day it was started.
   */
  logSet: (itemKey: number, set: SetInput, confirmed?: boolean) => Promise<void>
  /** Saves the held set as it is. */
  confirmPending: () => Promise<void>
  /** Back to the editor without saving the held set. */
  cancelPending: () => void
  /** Records where the rep was hardest or missed; null clears it. */
  setMissPoint: (setId: number, point: MissPoint | null) => Promise<void>
  deleteSet: (setId: number) => Promise<void>
  /** Puts the coach's numbers into the editor and marks the advice applied. */
  applyTarget: (adviceId: number) => Promise<void>
  dismissTarget: (adviceId: number) => Promise<void>
  /** Adds an exercise for this session only; it is kept once a set is logged for it. */
  addExercise: (exerciseId: number) => void
  /** Takes back an added exercise that has no sets yet. */
  removeExercise: (exerciseId: number) => void
  /** Runs the coach over the history including today and stores what is new. */
  finishSession: () => Promise<void>
  /** Back to logging after the summary; if midnight has passed meanwhile, on to the new day. */
  dismissSummary: () => void
}

const DEFAULT_REPS = 5
const SHOWN_TIMEOUT_MS = 2000

type Stored = {
  date: string
  sessionId: number | null
  sets: StoredSet[]
  history: JsHistory
  plan: PlannedItem[]
  settings: AppSettings
  targets: (Suggestion & { status: string })[]
}

/** What only this screen knows: edited numbers, exercises added but not yet logged, open prompts. */
type Local = {
  date: string
  /** False until the editor values saved for this day have been read back. */
  restored: boolean
  editors: Record<number, SetEditor>
  added: number[]
  pending: PendingLog | null
  summary: FinishSummary | null
}

const emptyLocal = (date: string): Local => ({ date, restored: false, editors: {}, added: [], pending: null, summary: null })

const editorKey = (date: string, exerciseId: number) => `editor:${date}:${exerciseId}`

/** 1 = Monday .. 7 = Sunday for an ISO date. */
export function weekdayOf(isoDate: string): number {
  return new Date(`${isoDate}T00:00:00Z`).getUTCDay() || 7
}

function parseTarget(payloadJson: string): Target | null {
  try {
    const p = JSON.parse(payloadJson) as Record<string, unknown>
    if (typeof p.type !== 'string' || !p.type.endsWith('.Target')) return null
    if (typeof p.exerciseId !== 'number' || typeof p.weightKg !== 'number' || typeof p.reps !== 'number' || typeof p.rpe !== 'number') return null
    return { exerciseId: p.exerciseId, weightKg: p.weightKg, reps: p.reps, rpe: p.rpe, sets: typeof p.sets === 'number' ? p.sets : null }
  } catch {
    return null
  }
}

/** The TARGET advice stored for `date`, in every status, oldest first. */
function todaysTargets(advice: AdviceRow[], date: string): Stored['targets'] {
  return advice.flatMap((a) => {
    if (a.type !== 'TARGET' || a.createdAt !== date || a.id == null) return []
    const target = parseTarget(a.payloadJson)
    return target ? [{ id: a.id, reason: a.reason, status: a.status, target }] : []
  })
}

function isEditor(value: unknown): value is SetEditor {
  if (typeof value !== 'object' || value === null) return false
  const v = value as Record<string, unknown>
  return (v.weightKg === null || typeof v.weightKg === 'number') && typeof v.reps === 'number' && typeof v.rpe === 'number'
}

async function readStored(db: GritDb, date: string): Promise<Stored> {
  const [session, history, plan, settings, advice] = await Promise.all([
    db.sessions.where('date').equals(date).first(),
    loadHistory(db),
    db.planItems.toArray(),
    getSettings(db),
    db.advice.toArray(),
  ])
  const sets = session?.id != null ? await db.sets.where('sessionId').equals(session.id).sortBy('id') : []
  return {
    date,
    sessionId: session?.id ?? null,
    sets: sets as StoredSet[],
    history,
    plan: plan as PlannedItem[],
    settings,
    targets: todaysTargets(advice, date),
  }
}

async function readEditors(db: GritDb, date: string): Promise<Record<number, SetEditor>> {
  const prefix = editorKey(date, 0).slice(0, -1)
  const rows = await db.meta.where('key').startsWith(prefix).toArray()
  const editors: Record<number, SetEditor> = {}
  for (const row of rows) {
    const exerciseId = Number(row.key.slice(prefix.length))
    if (Number.isInteger(exerciseId) && isEditor(row.value)) editors[exerciseId] = row.value
  }
  return editors
}

const coachSettings = (s: AppSettings) => new JsCoachSettings(s.defaultRpe, s.changePlates)

const withoutId = (row: AdviceRow): AdviceRow => {
  const { id: _id, ...rest } = row
  return rest
}

/**
 * Works out today's targets and stores the ones not stored yet, one per exercise. Returns false and
 * writes nothing when `date` is no longer today: a screen left open past midnight must not store
 * targets dated the day before.
 */
async function coachPass(db: GritDb, date: string, today: () => string): Promise<boolean> {
  if (today() !== date) return false
  // One transaction, so two passes (two tabs, a remount) cannot both find the target missing.
  await db.transaction('rw', [db.sessions, db.sets, db.exercises, db.baselines, db.planItems, db.settings, db.advice, db.meta], async () => {
    const [history, plan, settings, advice] = await Promise.all([loadHistory(db), db.planItems.toArray(), getSettings(db), db.advice.toArray()])
    const known = new Set(todaysTargets(advice, date).map((t) => t.target.exerciseId))
    const prior = advice.map((a) => adviceToJs(a as AdviceRow & { id: number }))
    const fresh: AdviceRow[] = []
    for (const a of grit.targetsFor(weekdayOf(date), history, (plan as PlannedItem[]).map(planItemToJs), coachSettings(settings), date, prior)) {
      const exerciseId = parseTarget(a.payloadJson)?.exerciseId
      if (exerciseId == null || known.has(exerciseId)) continue
      known.add(exerciseId)
      fresh.push(withoutId(adviceFromJs(a)))
    }
    if (fresh.length > 0) await db.advice.bulkAdd(fresh)
    // Editor values are kept for a reload during the session; earlier days' are of no use any more.
    await db.meta
      .where('key')
      .startsWith('editor:')
      .filter((row) => !row.key.startsWith(`editor:${date}:`))
      .delete()
  })
  return true
}

/** The set a session's top set is taken from: flagged, else the heaviest good set, else the heaviest. Ties go to the later set. */
function topSetOf(sets: JsSet[]): JsSet | null {
  const heaviest = (list: JsSet[]) => list.reduce<JsSet | null>((best, s) => (best == null || s.weightKg >= best.weightKg ? s : best), null)
  return heaviest(sets.filter((s) => s.isTopSet).slice(-1)) ?? heaviest(sets.filter((s) => !s.failed)) ?? heaviest(sets)
}

/** The top set of the latest session before `before` that has this exercise. */
function lastTopSet(history: JsHistory, exerciseId: number, before: string): JsSet | null {
  for (let i = history.sessions.length - 1; i >= 0; i--) {
    const session = history.sessions[i]
    if (session == null || session.date >= before) continue
    const top = topSetOf(session.sets.filter((s) => s.exerciseId === exerciseId))
    if (top) return top
  }
  return null
}

function build(d: Stored, l: Local): Pick<TodayState, 'sessionId' | 'items' | 'addable' | 'weightStepKg' | 'changePlates'> {
  const exercises = new Map(d.history.exercises.map((e) => [e.id, e]))
  // One card per exercise: sets are stored per exercise, so a second plan line for the same one merges into the first.
  const planned: PlannedItem[] = []
  for (const p of d.plan.filter((p) => p.weekday === weekdayOf(d.date)).sort((a, b) => a.order - b.order)) {
    if (!planned.some((q) => q.exerciseId === p.exerciseId)) planned.push(p)
  }
  const plannedIds = new Set(planned.map((p) => p.exerciseId))
  const loggedIds = d.sets.map((s) => s.exerciseId)
  const addedIds = [...new Set([...loggedIds.filter((id) => !l.added.includes(id)), ...l.added])].filter((id) => !plannedIds.has(id))

  function item(exercise: JsExercise, plan: PlannedItem | null): TodayItem {
    const sets = d.sets.filter((s) => s.exerciseId === exercise.id)
    const mine = d.targets.filter((t) => t.target.exerciseId === exercise.id)
    const applied = mine.filter((t) => t.status === 'APPLIED').pop()?.target
    const top = lastTopSet(d.history, exercise.id, d.date)
    const lastToday = sets[sets.length - 1]
    const rpe = plan?.targetRpe ?? d.settings.defaultRpe
    const editor: SetEditor =
      l.editors[exercise.id] ??
      (lastToday != null
        ? { weightKg: lastToday.weightKg, reps: lastToday.reps, rpe: lastToday.rpe ?? rpe }
        : applied != null
          ? { weightKg: applied.weightKg, reps: applied.reps, rpe: applied.rpe }
          : { weightKg: top?.weightKg ?? null, reps: plan?.reps ?? top?.reps ?? DEFAULT_REPS, rpe })
    const targetSets = plan ? (applied?.sets ?? plan.sets) : null
    const open = mine.find((t) => t.status === 'OPEN')
    return {
      exercise,
      planned: plan,
      suggestion: open ? { id: open.id, reason: open.reason, target: open.target } : null,
      sets,
      editor,
      targetSets,
      isComplete: targetSets != null && sets.length >= targetSets,
      perSide: editor.weightKg != null ? (grit.platesPerSide(editor.weightKg, d.settings.changePlates) ?? null) : null,
      lastWeightKg: lastToday?.weightKg ?? top?.weightKg ?? null,
    }
  }

  const items: TodayItem[] = []
  for (const p of planned) {
    const exercise = exercises.get(p.exerciseId)
    if (exercise) items.push(item(exercise, p))
  }
  for (const id of addedIds) {
    const exercise = exercises.get(id)
    if (exercise) items.push(item(exercise, null))
  }
  const shown = new Set(items.map((i) => i.exercise.id))
  return {
    sessionId: d.sessionId,
    items,
    addable: d.history.exercises.filter((e) => !e.archived && !shown.has(e.id)).sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase())),
    weightStepKg: grit.loadStep(d.settings.changePlates),
    changePlates: d.settings.changePlates,
  }
}

/**
 * The calendar day on screen: the clock's day when the screen opens, read again whenever the app
 * comes back to the front, unless `holds` says an open session keeps the screen (a session belongs
 * to the day it was started, and a phone locks between sets). `syncDay()` reads the clock now, moves
 * the screen to that day and returns it; `showDay` puts a given day on screen.
 */
function useDay(today: () => string, holds: { current: () => boolean }): [date: string, syncDay: () => string, showDay: (date: string) => void] {
  const [date, setDate] = useState(today)
  const clock = useRef(today)
  clock.current = today
  useEffect(() => {
    const refresh = () => {
      if (document.visibilityState === 'visible' && !holds.current()) setDate(clock.current())
    }
    document.addEventListener('visibilitychange', refresh)
    window.addEventListener('pageshow', refresh)
    return () => {
      document.removeEventListener('visibilitychange', refresh)
      window.removeEventListener('pageshow', refresh)
    }
  }, [holds])
  const syncDay = useCallback(() => {
    const now = clock.current()
    setDate(now)
    return now
  }, [])
  return [date, syncDay, setDate]
}

/** When the session of `date` was last started or logged to; kept in `meta` so a reload knows too. */
type Activity = { date: string; at: number }

export const ACTIVITY_KEY = 'today:active'

/** How long after its last set an unfinished session still keeps the screen once its day is over. */
const HOLD_MS = 6 * 60 * 60 * 1000

function recentActivity(value: unknown): Activity | null {
  if (typeof value !== 'object' || value === null) return null
  const v = value as Record<string, unknown>
  if (typeof v.date !== 'string' || typeof v.at !== 'number') return null
  return Date.now() - v.at < HOLD_MS ? { date: v.date, at: v.at } : null
}

/** Whether the stored `today:active` note says a session is being logged: started or logged to recently, and not finished. */
export function sessionInProgress(note: unknown): boolean {
  return recentActivity(note) != null
}

/**
 * Today's session: the plan for the day, the sets logged so far and the numbers for the next one.
 * Reads follow the database live. `today` is the real clock; the screen keeps the day it opened on
 * until the app comes back to the front, and an unfinished session keeps it past midnight. `onSetLogged` fires after each saved set (true for a main
 * lift or variation), so a rest timer can start.
 */
export function useToday(db: GritDb, today: () => string, onSetLogged?: (isMainOrVariation: boolean) => void): TodayState & TodayActions {
  const clock = useRef(today)
  clock.current = today
  const holds = useRef<() => boolean>(() => false)
  const [date, syncDay, showDay] = useDay(today, holds)

  // After a reload past midnight, come back to the session that was being logged, not to the new day's
  // start card. Nothing is read or written for the clock's day until that is settled.
  const activity = useRef<Activity | null>(null)
  const [resumed, setResumed] = useState(false)
  useEffect(() => {
    let stale = false
    void (async () => {
      try {
        const recent = recentActivity((await db.meta.get(ACTIVITY_KEY))?.value)
        const open = recent != null && recent.date < clock.current() && (await db.sessions.where('date').equals(recent.date).count()) > 0
        if (stale) return
        activity.current = recent
        if (recent && open) showDay(recent.date)
      } catch {
        // Without the note the screen simply opens on the clock's day.
      }
      if (!stale) setResumed(true)
    })()
    return () => {
      stale = true
    }
  }, [db, showDay])

  const [stored, setStored] = useState<Stored>()
  const [readError, setReadError] = useState<unknown>(null)
  useEffect(() => {
    if (!resumed) return
    const subscription = liveQuery(() => readStored(db, date)).subscribe({ next: setStored, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db, date, resumed])
  // A failed read is for the app's error boundary, not for an empty screen that looks like no data.
  if (readError != null) throw readError
  const [localState, setLocal] = useState<Local>(() => emptyLocal(date))
  const [coachPasses, setCoachPasses] = useState(0)

  // Local state belongs to one day; on a new day it starts empty.
  const local = localState.date === date ? localState : emptyLocal(date)
  const update = useCallback((fn: (l: Local) => Local) => setLocal((prev) => fn(prev.date === date ? prev : emptyLocal(date))), [date])

  // Bring back the editor values saved before a reload.
  useEffect(() => {
    if (!resumed) return
    let stale = false
    readEditors(db, date)
      .then((saved) => {
        if (!stale) update((l) => ({ ...l, restored: true, editors: { ...saved, ...l.editors } }))
      })
      // Once the screen has moved on (or the database closed under it) the failure concerns nobody.
      .catch((e: unknown) => {
        if (!stale) setReadError(e)
      })
    return () => {
      stale = true
    }
  }, [db, date, update, resumed])

  const current = stored?.date === date ? stored : undefined
  const built = useMemo(() => (current ? build(current, local) : null), [current, local])
  const state: TodayState = {
    date,
    loading: !resumed || built == null || !local.restored,
    sessionId: built?.sessionId ?? null,
    items: built?.items ?? [],
    pending: local.pending,
    summary: local.summary,
    addable: built?.addable ?? [],
    weightStepKg: built?.weightStepKg ?? grit.loadStep(false),
    changePlates: built?.changePlates ?? false,
    coachPasses,
  }

  // Actions read the newest state and callbacks through refs, so a tap never acts on a stale screen.
  const latest = useRef(state)
  latest.current = state
  const setLogged = useRef(onSetLogged)
  setLogged.current = onSetLogged

  // An unfinished session that was started or logged to in the last few hours keeps the screen when
  // its day ends; one left open for longer does not stand in the way of the next day. A finish summary
  // keeps it until it is dismissed, so a phone that locks meanwhile does not throw it away unseen.
  holds.current = () => {
    if (state.summary != null) return true
    const recent = activity.current
    return state.sessionId != null && recent != null && recent.date === date && Date.now() - recent.at < HOLD_MS
  }
  const touch = useCallback(
    (day: string) => {
      activity.current = { date: day, at: Date.now() }
      // Only a reload after midnight needs the stored copy.
      db.meta.put({ key: ACTIVITY_KEY, value: activity.current }).catch(() => {})
    },
    [db],
  )

  // Ask the coach for today's targets when the screen opens and again if the plan changes.
  const planKey = current ? JSON.stringify([current.plan, current.settings.defaultRpe, current.settings.changePlates]) : null
  useEffect(() => {
    if (planKey == null) return
    let stale = false
    coachPass(db, date, () => clock.current())
      .then((ran) => {
        if (ran && !stale) setCoachPasses((n) => n + 1)
      })
      .catch((e: unknown) => {
        if (!stale) setReadError(e)
      })
    return () => {
      stale = true
    }
  }, [db, date, planKey])

  // True while a set is being saved; further log taps are ignored until the screen shows it.
  const saving = useRef(false)
  const starting = useRef(false)
  const waiting = useRef<{ setId: number; done: () => void }[]>([])
  const loadWaiting = useRef<(() => void)[]>([])
  const closed = useRef(false)
  useEffect(() => {
    if (!state.loading) loadWaiting.current.splice(0).forEach((done) => done())
    const onScreen = new Set(state.items.flatMap((i) => i.sets.map((s) => s.id)))
    waiting.current = waiting.current.filter((w) => {
      if (onScreen.has(w.setId)) w.done()
      return !onScreen.has(w.setId)
    })
  })
  useEffect(() => {
    closed.current = false
    return () => {
      closed.current = true
      waiting.current.splice(0).forEach((w) => w.done())
      loadWaiting.current.splice(0).forEach((done) => done())
    }
  }, [])

  /** Waits until the screen has its data, however often it starts loading again meanwhile. False only when the screen was closed. */
  const loaded = useCallback(async () => {
    while (latest.current.loading && !closed.current) await new Promise<void>((resolve) => loadWaiting.current.push(resolve))
    return !closed.current
  }, [])

  const shown = useCallback(
    (setId: number) =>
      new Promise<void>((resolve) => {
        const timer = setTimeout(done, SHOWN_TIMEOUT_MS)
        function done() {
          clearTimeout(timer)
          resolve()
        }
        waiting.current.push({ setId, done })
      }),
    [],
  )

  const edit = useCallback(
    (exerciseId: number, editor: SetEditor) => {
      update((l) => ({ ...l, editors: { ...l.editors, [exerciseId]: editor } }))
      // The numbers are on screen either way; a failed write only costs them on a reload.
      db.meta.put({ key: editorKey(date, exerciseId), value: editor }).catch(() => {})
    },
    [db, date, update],
  )

  const startSession = useCallback(
    async (bodyweightKg: number | null, readiness: number | null) => {
      if (starting.current) return
      starting.current = true
      try {
        if (!(await loaded())) return
        // A session belongs to the day it is started. Started after midnight on a card opened the day
        // before, it is dated today and the screen moves to today with it.
        const day = syncDay()
        await db.transaction('rw', db.sessions, async () => {
          const existing = await db.sessions.where('date').equals(day).first()
          if (!existing) await db.sessions.add({ date: day, bodyweightKg, readiness })
        })
        touch(day)
      } finally {
        starting.current = false
      }
    },
    [db, syncDay, loaded, touch],
  )

  const logSet = useCallback(
    async (itemKey: number, set: SetInput, confirmed = false) => {
      // A fast double tap must not save the set twice.
      if (saving.current) return
      saving.current = true
      try {
        // A set goes into the open session, also after midnight: the session belongs to the day it was started.
        // A tap is never dropped: it waits for the screen's data, and without a session it fails out loud.
        if (!(await loaded())) return
        const now = latest.current
        if (now.sessionId == null) throw new Error('There is no session to log to.')
        const item = now.items.find((i) => i.exercise.id === itemKey)
        const lastWeightKg = item?.lastWeightKg ?? null
        if (!confirmed) {
          const warnings = grit.checkInput(set.weightKg, set.reps, lastWeightKg) as InputWarning[]
          if (warnings.length > 0) {
            update((l) => ({ ...l, pending: { exerciseId: itemKey, set, warnings, lastWeightKg } }))
            return
          }
        }
        // The top set is worked out from the session afterwards (heaviest good set), never flagged here.
        const setId = await db.sets.add({ sessionId: now.sessionId, exerciseId: itemKey, weightKg: set.weightKg, reps: set.reps, rpe: set.rpe, failed: set.failed, missPoint: null, isTopSet: false })
        update((l) => ({ ...l, pending: null }))
        touch(now.date)
        setLogged.current?.(item?.exercise.category !== 'ACCESSORY')
        // Hold the guard until the new set is on screen, so the button the lifter sees is never stale.
        await shown(setId as number)
      } finally {
        saving.current = false
      }
    },
    [db, update, shown, loaded, touch],
  )

  const confirmPending = useCallback(async () => {
    const held = latest.current.pending
    if (held) await logSet(held.exerciseId, held.set, true)
  }, [logSet])

  const cancelPending = useCallback(() => update((l) => ({ ...l, pending: null })), [update])

  const setMissPoint = useCallback(
    async (setId: number, point: MissPoint | null) => {
      await db.sets.update(setId, { missPoint: point })
    },
    [db],
  )

  const deleteSet = useCallback((setId: number) => db.sets.delete(setId), [db])

  const applyTarget = useCallback(
    async (adviceId: number) => {
      const suggestion = latest.current.items.map((i) => i.suggestion).find((s) => s?.id === adviceId)
      if (!suggestion) return
      // Status first: if storing it fails, the editor keeps its numbers and the chip stays.
      await db.advice.update(adviceId, { status: 'APPLIED' })
      const { exerciseId, weightKg, reps, rpe } = suggestion.target
      edit(exerciseId, { weightKg, reps, rpe })
    },
    [db, edit],
  )

  const dismissTarget = useCallback(
    async (adviceId: number) => {
      await db.advice.update(adviceId, { status: 'DISMISSED' })
    },
    [db],
  )

  const addExercise = useCallback((exerciseId: number) => update((l) => (l.added.includes(exerciseId) ? l : { ...l, added: [...l.added, exerciseId] })), [update])

  const removeExercise = useCallback(
    (exerciseId: number) => {
      update((l) => {
        const { [exerciseId]: _removed, ...editors } = l.editors
        return { ...l, added: l.added.filter((id) => id !== exerciseId), editors }
      })
      db.meta.delete(editorKey(date, exerciseId)).catch(() => {})
    },
    [db, date, update],
  )

  const finishSession = useCallback(async () => {
    const now = latest.current
    if (now.loading || now.sessionId == null) return
    // Advice is dated by the real clock: a session finished after midnight stores nothing under the day before.
    const clockDay = clock.current()
    const newNotes = await db.transaction('rw', [db.sessions, db.sets, db.exercises, db.baselines, db.planItems, db.settings, db.advice], async () => {
      const [history, plan, settings, advice] = await Promise.all([loadHistory(db), db.planItems.toArray(), getSettings(db), db.advice.toArray()])
      const prior = advice.map((a) => adviceToJs(a as AdviceRow & { id: number }))
      const fresh = grit.evaluate(history, (plan as PlannedItem[]).map(planItemToJs), coachSettings(settings), clockDay, prior)
      if (fresh.length > 0) await db.advice.bulkAdd(fresh.map((a) => withoutId(adviceFromJs(a))))
      return fresh.length
    })
    // Finished: it no longer keeps the screen, here or after a reload.
    activity.current = null
    db.meta.delete(ACTIVITY_KEY).catch(() => {})
    const done = now.items.filter((i) => i.sets.length > 0)
    update((l) => ({ ...l, summary: { setCount: done.reduce((n, i) => n + i.sets.length, 0), exerciseCount: done.length, newNotes } }))
  }, [db, update])

  const dismissSummary = useCallback(() => {
    update((l) => ({ ...l, summary: null }))
    // A session finished after midnight is behind us now: on to the new day.
    syncDay()
  }, [update, syncDay])

  return {
    ...state,
    startSession,
    edit,
    logSet,
    confirmPending,
    cancelPending,
    setMissPoint,
    deleteSet,
    applyTarget,
    dismissTarget,
    addExercise,
    removeExercise,
    finishSession,
    dismissSummary,
  }
}
