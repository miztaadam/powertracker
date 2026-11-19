import { liveQuery } from 'dexie'
import { useCallback, useEffect, useRef, useState } from 'react'
import { planFor, planSnapshot, replacePlan, type PlanSnapshot } from '../db/catalogue'
import { getSettings, updateSettings, type AppSettings, type GritDb } from '../db/db'
import type { TrainingMode } from '../db/schema'
import { grit, type JsLibraryExercise, type JsPlanDraft } from '../domain'
import { draftItems } from './usePlanBuilder'

/** A plan template as the domain gives it (1.14): fixed data, the same on both apps. */
export type PlanTemplate = ReturnType<typeof grit.planTemplatesFor>[number]

/** The templates screen's two steps: the side's list, then the chosen template's draft. */
export type TemplateStep = 'list' | 'draft'

export type TemplatesState = {
  /** The side the templates are for: the one whose plan and training days a save replaces. */
  mode: TrainingMode
  loading: boolean
  step: TemplateStep
  /** `mode`'s templates, fewest days first. */
  templates: readonly PlanTemplate[]
  chosen: PlanTemplate | null
  /** The ticked weekdays (1 = Monday), in week order. The draft is laid on them once there are exactly the template's count. */
  days: number[]
  /**
   * The chosen template's week, with any swaps. Its sessions sit on the last weekdays that matched the template's count:
   * while the ticked count is off, the screen heads them "Day 1", "Day 2", … instead.
   */
  draft: JsPlanDraft | null
  /** The side's training days in Settings, where the weekday choice starts from. */
  sideDays: number[]
  /** The `showStrictPress` setting when the screen opened: the powerlifting templates include strict press only with it on. */
  strictPress: boolean
  /** The side already has plan items: Use this plan asks first. */
  hasPlan: boolean
  confirming: boolean
  saving: boolean
  /** Stored, plan and days: the screen returns to Plan. */
  saved: boolean
  /** Once saved over a plan: what Plan's snackbar can Undo (1.14.1). Null when the side had no plan. */
  undo: PlanSnapshot | null
  /** The save failed; nothing was stored, the draft stays and Use this plan can be tried again. */
  failed: boolean
  /** Exactly the template's number of weekdays is ticked. */
  daysMatch: boolean
  canUse: boolean
  /** Powerlifting items have fixed reps: the draft shows "3 × 5 @ 8" and saves no top of a range. */
  fixedReps: boolean
}

export type TemplatesActions = {
  /** Opens `id`'s draft on the suggested weekdays: the side's own days when they are as many, else a fixed spread. */
  choose: (id: string) => void
  /** Ticks or unticks `day`. Once exactly the template's count is ticked, the sessions move onto those days, swaps kept. */
  toggleDay: (day: number) => void
  /** What `name` on `day` can be swapped for: none for a main lift, else the domain's list. */
  alternatives: (day: number, name: string) => JsLibraryExercise[]
  /** `from` on `day` becomes `to`, with the same sets, reps and RPE; only to one of `alternatives`. */
  swap: (day: number, from: string, to: string) => void
  /** Use this plan: asks first when the side has a plan, else saves. Nothing happens unless `canUse`. */
  use: () => void
  confirmReplace: () => void
  cancelReplace: () => void
  /** From the draft, back to the list (true); from the list, false: back leaves the screen. Nothing is stored. */
  back: () => boolean
}

const MAIN_LIFTS = new Set(['SQUAT', 'BENCH', 'DEADLIFT', 'STRICT_PRESS'])

/** A main lift (squat, bench, deadlift, strict press) has no Swap; everything else in a template has one. */
export function canSwap(name: string): boolean {
  const category = grit.findInLibrary(name)?.category
  return category == null || !MAIN_LIFTS.has(category)
}

/** `draft`'s sessions, in order, on `days` in week order. The volume is unchanged: each group keeps its day count. */
export function onDays(draft: JsPlanDraft, days: readonly number[]): JsPlanDraft {
  const week = [...days].sort((a, b) => a - b)
  if (week.length !== draft.sessions.length) throw new Error(`days: ${week.length}; the draft has ${draft.sessions.length}`)
  return draft.copy(draft.sessions.map((session, i) => session.copy(week[i]!)))
}

/** The side's training days as stored. */
export const sideDaysOf = (settings: AppSettings, mode: TrainingMode): number[] => (mode === 'BODYBUILDING' ? settings.bodybuildingTrainingDays : settings.trainingDays)

/** "Replace your powerlifting plan?" or "Replace your bodybuilding plan?". */
export const replaceTitle = (mode: TrainingMode) => `Replace your ${mode === 'BODYBUILDING' ? 'bodybuilding' : 'powerlifting'} plan?`

/** The snackbar on Plan after a save: "Plan set: 4-day upper/lower." */
export const planSetText = (name: string) => `Plan set: ${name}.`

type Inner = Omit<TemplatesState, 'daysMatch' | 'canUse' | 'fixedReps'>

const daysMatchOf = (s: Inner) => s.chosen != null && s.days.length === s.chosen.dayCount
const canUseOf = (s: Inner) => s.step === 'draft' && s.draft != null && daysMatchOf(s) && !s.saving && !s.saved

/**
 * Choosing a plan template (1.14) for `mode`'s side: the list, then a draft of the chosen one whose weekdays can be
 * changed and whose exercises can be swapped. Nothing is stored until Use this plan; then, in one transaction, the
 * side's whole plan is replaced (`replacePlan`, catalogue items by name) and that side's training days become the
 * chosen weekdays. A side with a plan is asked first. The other side is never touched.
 */
export function useTemplates(db: GritDb, mode: TrainingMode): TemplatesState & TemplatesActions {
  const [inner, setInner] = useState<Inner>(() => ({
    mode,
    loading: true,
    step: 'list',
    templates: grit.planTemplatesFor(mode),
    chosen: null,
    days: [],
    draft: null,
    sideDays: [],
    strictPress: false,
    hasPlan: false,
    confirming: false,
    saving: false,
    saved: false,
    undo: null,
    failed: false,
  }))
  const [readError, setReadError] = useState<unknown>(null)
  // The state as last set, for the actions: a second tap reads what the first one did, not the last render.
  const current = useRef(inner)
  const update = useCallback((change: (s: Inner) => Inner) => {
    const next = change(current.current)
    current.current = next
    setInner(next)
  }, [])

  useEffect(() => {
    let stale = false
    let unsubscribe = () => {}
    // The strict-press setting as it is when the screen opens; the side's plan and days live.
    getSettings(db)
      .then((settings) => {
        if (stale) return
        update((s) => ({ ...s, strictPress: settings.showStrictPress }))
        const subscription = liveQuery(async () => ({ hasPlan: (await planFor(db, mode)).length > 0, sideDays: sideDaysOf(await getSettings(db), mode) })).subscribe({
          next: ({ hasPlan, sideDays }) => update((s) => ({ ...s, loading: false, hasPlan, sideDays: [...sideDays] })),
          error: setReadError,
        })
        unsubscribe = () => subscription.unsubscribe()
      })
      .catch((error: unknown) => {
        if (!stale) setReadError(error)
      })
    return () => {
      stale = true
      unsubscribe()
    }
  }, [db, mode, update])

  const fixedReps = mode === 'POWERLIFTING'

  const choose = useCallback(
    (id: string) => {
      const s = current.current
      const template = s.templates.find((t) => t.id === id)
      if (template == null || s.loading || s.saving) return
      const days = [...grit.planTemplateSuggestedDays(template.dayCount, s.sideDays)]
      const draft = grit.planTemplateDraft(template.id, days, s.strictPress)
      update((x) => ({ ...x, step: 'draft', chosen: template, days, draft, confirming: false, failed: false }))
    },
    [update],
  )

  const toggleDay = useCallback(
    (day: number) => {
      const s = current.current
      const template = s.chosen
      if (template == null || s.step !== 'draft' || s.saving) return
      const days = s.days.includes(day) ? s.days.filter((d) => d !== day) : [...s.days, day].sort((a, b) => a - b)
      const draft = s.draft && days.length === template.dayCount ? onDays(s.draft, days) : s.draft
      update((x) => ({ ...x, days, draft, failed: false }))
    },
    [update],
  )

  const alternatives = useCallback((day: number, name: string): JsLibraryExercise[] => {
    const { chosen, draft } = current.current
    if (chosen == null || draft == null || !canSwap(name)) return []
    return [...grit.planTemplateAlternatives(chosen.id, draft, day, name)]
  }, [])

  const swap = useCallback(
    (day: number, from: string, to: string) => {
      const s = current.current
      if (s.draft == null || s.saving) return
      if (!alternatives(day, from).some((e) => e.name.toLowerCase() === to.toLowerCase())) return
      let swapped: JsPlanDraft
      try {
        swapped = grit.planTemplateSwap(s.draft, day, from, to)
      } catch {
        return
      }
      update((x) => ({ ...x, draft: swapped }))
    },
    [alternatives, update],
  )

  const save = useCallback(() => {
    const s = current.current
    const draft = s.draft
    if (draft == null || !canUseOf(s)) return
    update((x) => ({ ...x, saving: true, failed: false }))
    // Every template exercise is a catalogue exercise: saved by name, its row found or made in the transaction.
    const items = draftItems(draft, [], fixedReps)
    const days = [...s.days].sort((a, b) => a - b)
    // One transaction: the plan and that side's days stand or fall together, so a failure changes nothing.
    // The plan as it was is read in the same transaction, for Undo: null when the side had none.
    db.transaction('rw', db.exercises, db.planItems, db.skips, db.settings, async () => {
      const before = await planSnapshot(db, mode)
      const added = await replacePlan(db, mode, items)
      await updateSettings(db, (st) => (mode === 'BODYBUILDING' ? { ...st, bodybuildingTrainingDays: days } : { ...st, trainingDays: days }))
      return before && { ...before, replacedBy: added }
    }).then(
      (undo) => update((x) => ({ ...x, saving: false, saved: true, undo })),
      () => update((x) => ({ ...x, saving: false, failed: true })),
    )
  }, [db, mode, fixedReps, update])

  const use = useCallback(() => {
    const s = current.current
    if (!canUseOf(s)) return
    if (s.hasPlan) update((x) => ({ ...x, confirming: true }))
    else save()
  }, [save, update])

  const confirmReplace = useCallback(() => {
    if (!current.current.confirming) return
    update((x) => ({ ...x, confirming: false }))
    save()
  }, [save, update])

  const cancelReplace = useCallback(() => update((x) => ({ ...x, confirming: false })), [update])

  const back = useCallback(() => {
    const s = current.current
    if (s.step === 'list') return false
    if (s.saving) return true
    update((x) => ({ ...x, step: 'list', chosen: null, days: [], draft: null, confirming: false, failed: false }))
    return true
  }, [update])

  // A failed read is for the app's error boundary, not for a screen that looks as if nothing was stored.
  if (readError != null) throw readError

  return {
    ...inner,
    daysMatch: daysMatchOf(inner),
    canUse: canUseOf(inner),
    fixedReps,
    choose,
    toggleDay,
    alternatives,
    swap,
    use,
    confirmReplace,
    cancelReplace,
    back,
  }
}
