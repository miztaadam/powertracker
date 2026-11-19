import { useCallback, useEffect, useRef, useState } from 'react'
import type { GritDb } from '../../../db/db'
import type { PlanSnapshot } from '../../../db/catalogue'
import type { TrainingMode } from '../../../db/schema'
import type { JsDraftExercise, JsLibraryExercise, JsPlanDraft } from '../../../domain'
import { canSwap, replaceTitle, useTemplates, type PlanTemplate, type TemplatesActions, type TemplatesState } from '../../../state/useTemplates'
import { dayName } from '../../../state/weekdayNames'
import { IconButton, PrimaryButton, QuietButton, SecondaryButton } from '../../../ui/components/Button'
import { Dialog } from '../../../ui/components/Dialog'
import { CloseIcon } from '../../../ui/components/icons'
import { ModeLabel } from '../../../ui/components/ModeLabel'
import { ScreenHeader } from '../../../ui/components/ScreenHeader'
import { CHOOSE_A_TEMPLATE } from '../PlanScreen'
import { DraftLeftOut, DraftSessionCard, DraftVolume, SAVE_FAILED, SaveFailedLine, SwapSheet, WEEKDAYS_QUESTION, WeekdayChips, chooseDaysText, chosenDaysText } from '../builder/DraftParts'
import '../plan.css'
import '../builder/builder.css'
import './templates.css'

// The templates' words (1.14 plan, Task 4), as on Android.
export const USE_THIS_PLAN = 'Use this plan'
export const REPLACE_KEPT = 'Your history and bests are kept.'

/** "3 days a week · New to powerlifting: 2–3 days a week", under a template's name. */
export const templateDaysLine = (t: PlanTemplate) => `${t.dayCount} days a week · ${t.suits}`

/** "Upper A · Lower A · Upper B · Lower B": the template's day titles in order. */
export const templateDayTitles = (t: PlanTemplate) => t.sessions.map((s) => s.title).join(' · ')

/** A draft day's name: its weekday once the weekdays match the template, else "Day 1", "Day 2", …. */
export const templateDayName = (index: number, day: number, daysMatch: boolean) => (daysMatch ? dayName(day) : `Day ${index + 1}`)

export type TemplatesScreenProps = {
  db: GritDb
  /** The side whose templates these are, and whose plan and training days a save replaces. */
  mode: TrainingMode
  /** Leaves with nothing changed. */
  onLeave: () => void
  /** After Use this plan has saved, with the template's name for Plan's snackbar, and what it can Undo when a plan was replaced (1.14.1). */
  onSaved: (name: string, undo?: PlanSnapshot) => void
}

/** What the draft's history entry carries, beside what the screen's own entry had (its `gritFrom`). */
type DraftEntry = { templatesStep?: 'draft' }
const onDraftEntry = () => (window.history.state as DraftEntry | null)?.templatesStep === 'draft'

/**
 * The draft has its own history entry (same address), so the browser's Back from the draft returns to the list, as
 * Android's system back does; Back from the list leaves the screen. The entry is added when the draft opens and taken
 * off when the list comes back by the screen's own Back. `drop(then)` takes it off before leaving or after a save, so
 * that `then` (which goes one step back in history to where the screen was opened from) starts from the screen's own
 * entry.
 */
function useDraftEntry(step: TemplatesState['step'], back: () => boolean) {
  const stepNow = useRef(step)
  stepNow.current = step
  // What the next popstate is for when it is not the browser's Back from the draft: nothing, or `then`.
  const nextPop = useRef<null | 'stay' | (() => void)>(null)
  const pushed = useRef(false)
  // Opened on a draft entry left from before (a reload on the draft): the screen starts on the list, so it steps off
  // that entry once, or Close and a save would land on it rather than on the screen's own entry. Once: StrictMode runs
  // this twice, and a second step back would leave the screen.
  const steppedOff = useRef(false)
  useEffect(() => {
    if (steppedOff.current || !onDraftEntry()) return
    steppedOff.current = true
    nextPop.current = 'stay'
    window.history.back()
  }, [])
  useEffect(() => {
    const onPop = () => {
      const then = nextPop.current
      nextPop.current = null
      if (then === 'stay') return
      if (then != null) then()
      else if (stepNow.current === 'draft') back()
    }
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [back])
  useEffect(() => {
    if (step === 'draft' && !pushed.current) {
      pushed.current = true
      window.history.pushState({ ...((window.history.state as object | null) ?? {}), templatesStep: 'draft' } satisfies DraftEntry, '')
    } else if (step === 'list' && pushed.current) {
      pushed.current = false
      // Back to the list by the screen's own Back: its entry goes too. After the browser's Back it is gone already.
      if (onDraftEntry()) {
        nextPop.current = 'stay'
        window.history.back()
      }
    }
  }, [step])
  return useCallback((then: () => void) => {
    if (!pushed.current || !onDraftEntry()) {
      then()
      return
    }
    pushed.current = false
    nextPop.current = then
    window.history.back()
  }, [])
}

/** Choosing a plan template for one side (1.14): the list, then the chosen template's draft to adjust and use. */
export function TemplatesScreen({ db, mode, onLeave, onSaved }: TemplatesScreenProps) {
  const templates = useTemplates(db, mode)
  const drop = useDraftEntry(templates.step, templates.back)
  const done = useRef(onSaved)
  done.current = onSaved
  const name = templates.saved ? (templates.chosen?.name ?? null) : null
  const undo = useRef(templates.undo)
  undo.current = templates.undo
  useEffect(() => {
    if (name == null) return
    const replaced = undo.current
    drop(() => (replaced != null ? done.current(name, replaced) : done.current(name)))
  }, [name, drop])
  const leave = useRef(onLeave)
  leave.current = onLeave
  const close = useCallback(() => drop(() => leave.current()), [drop])
  if (templates.loading) return null
  return <TemplatesContent state={templates} actions={templates} onLeave={close} />
}

export type TemplatesContentProps = { state: TemplatesState; actions: TemplatesActions; onLeave: () => void }

/** The templates without their data source: the list, or the chosen template's draft with Back and Use this plan. */
export function TemplatesContent({ state, actions, onLeave }: TemplatesContentProps) {
  const drafting = state.step === 'draft' && state.chosen != null && state.draft != null
  return (
    <section className="screen pb-screen">
      <ScreenHeader
        title={drafting ? state.chosen!.name : CHOOSE_A_TEMPLATE}
        eyebrow={drafting ? CHOOSE_A_TEMPLATE : undefined}
        label={<ModeLabel mode={state.mode} />}
        navigation={<IconButton label="Close" icon={<CloseIcon />} onClick={onLeave} />}
        className="tpl-header"
      />
      {drafting ? <TemplateDraft template={state.chosen!} draft={state.draft!} state={state} actions={actions} /> : <TemplateList templates={state.templates} onChoose={actions.choose} />}
      {drafting && (
        <div className="pb-actions">
          <SecondaryButton text="Back" onClick={() => actions.back()} enabled={!state.saving} />
          <PrimaryButton text={USE_THIS_PLAN} onClick={actions.use} enabled={state.canUse} />
        </div>
      )}
      {state.confirming && (
        <Dialog
          open
          title={replaceTitle(state.mode)}
          onDismiss={actions.cancelReplace}
          actions={
            <>
              <QuietButton text="Cancel" onClick={actions.cancelReplace} />
              <PrimaryButton text="Replace" compact onClick={actions.confirmReplace} />
            </>
          }
        >
          <p className="body-medium">{REPLACE_KEPT}</p>
        </Dialog>
      )}
    </section>
  )
}

function TemplateList({ templates, onChoose }: { templates: readonly PlanTemplate[]; onChoose: (id: string) => void }) {
  return (
    <div className="screen-body pb-body">
      <ul className="tpl-list" aria-label="Templates">
        {templates.map((t) => (
          <li key={t.id}>
            {/* The whole card opens the template; its name is the button's name, the lines describe it. */}
            <button type="button" className="gr-card tpl-row" onClick={() => onChoose(t.id)} aria-labelledby={`tpl-${t.id}-name`} aria-describedby={`tpl-${t.id}-days tpl-${t.id}-titles`}>
              <span id={`tpl-${t.id}-name`} className="tpl-name headline-small">
                {t.name}
              </span>
              <span id={`tpl-${t.id}-days`} className="tpl-days body-medium">
                {templateDaysLine(t)}
              </span>
              <span id={`tpl-${t.id}-titles`} className="tpl-titles body-medium">
                {templateDayTitles(t)}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

type Swapping = { day: number; where: string; exercise: JsDraftExercise; options: JsLibraryExercise[] }

function TemplateDraft({ template, draft, state, actions }: { template: PlanTemplate; draft: JsPlanDraft; state: TemplatesState; actions: TemplatesActions }) {
  const [swapping, setSwapping] = useState<Swapping | null>(null)
  // Use this plan is at the foot of the page: bring the line into view when a save fails.
  const failure = useRef<HTMLParagraphElement>(null)
  useEffect(() => {
    if (state.failed) failure.current?.scrollIntoView?.({ block: 'nearest' })
  }, [state.failed])
  return (
    <div className="screen-body pb-body tpl-draft">
      {state.failed && <SaveFailedLine text={SAVE_FAILED} lineRef={failure} />}
      <h2 className="pb-question headline-small">{WEEKDAYS_QUESTION}</h2>
      <WeekdayChips days={state.days} onDay={actions.toggleDay} />
      <p className="pb-hint body-medium" role="status">
        {state.daysMatch ? chosenDaysText(state.days) : chooseDaysText(template.dayCount)}
      </p>
      {draft.sessions.map((session, i) => {
        // The day as its card names it, which the Swap sheet names too.
        const where = templateDayName(i, session.day, state.daysMatch)
        return (
          <DraftSessionCard
            key={i}
            session={session}
            where={where}
            fixedReps={state.fixedReps}
            canSwap={(e) => canSwap(e.name)}
            onSwap={(exercise) => setSwapping({ day: session.day, where, exercise, options: actions.alternatives(session.day, exercise.name) })}
          />
        )
      })}
      {/* The bodybuilding draft shows each body part's weekly sets, as the builder's does; powerlifting has none. */}
      {state.mode === 'BODYBUILDING' && <DraftVolume volume={draft.volume} />}
      <DraftLeftOut leftOut={draft.leftOut} />
      {swapping && (
        <SwapSheet
          name={swapping.exercise.name}
          where={swapping.where}
          options={swapping.options}
          onPick={(to) => {
            actions.swap(swapping.day, swapping.exercise.name, to)
            setSwapping(null)
          }}
          onClose={() => setSwapping(null)}
        />
      )}
    </div>
  )
}
