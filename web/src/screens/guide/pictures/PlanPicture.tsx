import { useMemo } from 'react'
import type { TrainingMode } from '../../../db/schema'
import { grit } from '../../../domain'
import { PlanContent } from '../../plan/PlanScreen'
import { TemplatesContent } from '../../plan/templates/TemplatesScreen'
import type { FlipTarget } from '../guideFlip'
import { PHONE_HEIGHT } from '../guideLabels'
import { NO_PLAN_ACTIONS, NO_TEMPLATES_ACTIONS, guidePlanState, guideTemplatesState } from '../guideSampleState'
import { keyedBlocks, type PictureDef, type PictureProps, type PictureTap } from './picture'

const none = () => {}

/**
 * The empty Plan screen (screen 3's first step and screen 8's picture): the side's day cards with nothing in them, and
 * its Choose a template (and on bodybuilding Build my plan); no-op actions.
 */
export function EmptyPlan({ mode }: { mode: TrainingMode }) {
  const state = useMemo(() => guidePlanState(mode, [], grit.guideSampleExercises(mode)), [mode])
  return <PlanContent state={state} actions={NO_PLAN_ACTIONS} onOpenSettings={none} onChooseTemplate={none} onBuildPlan={mode === 'BODYBUILDING' ? none : undefined} />
}

/**
 * Screen 3: the empty Plan; then the real templates, their list, the sample template's draft, the draft with its one
 * Swap made; then Plan with the week saved (`guideSampleFilledPlan`). Every tap is ringed just before its result.
 */
function PlanPicture({ mode, step }: PictureProps) {
  const filled = useMemo(() => (step === 'PLAN_FILLED' ? guidePlanState(mode, grit.guideSampleFilledPlan(mode), grit.guideSampleFilledExercises(mode)) : null), [mode, step])
  const templates = useMemo(() => (step === 'PLAN_EMPTY' || step === 'PLAN_FILLED' ? null : guideTemplatesState(mode, step)), [mode, step])
  // A scrollport the size of the phone's screen: the draft's bar (Back, Use this plan) sits at its foot, as on a phone.
  return (
    <div className="guide-scrollport">
      {step === 'PLAN_EMPTY' && <EmptyPlan mode={mode} />}
      {templates != null && <TemplatesContent state={templates} actions={NO_TEMPLATES_ACTIONS} onLeave={none} />}
      {filled != null && <PlanContent state={filled} actions={NO_PLAN_ACTIONS} onOpenSettings={none} onChooseTemplate={none} />}
    </div>
  )
}

/**
 * The rows screen 3 moves: the screen as a whole (a step onto another screen cross-fades it), then within it its header,
 * its blocks and their rows. A row is keyed by its exercise and day, so the swapped row cross-fades in its place.
 */
export function planTargets(screen: HTMLElement): FlipTarget[] {
  const root = screen.querySelector<HTMLElement>('.screen')
  if (root == null) return []
  const kind = root.classList.contains('pb-screen') ? 'templates' : 'plan'
  const key = `screen:${kind}`
  const body = root.querySelector<HTMLElement>('.screen-body')
  const targets: FlipTarget[] = [{ key, el: root, parent: null }]
  const header = root.querySelector<HTMLElement>('.gr-screen-header')
  if (header != null) targets.push({ key: `${key}>header`, el: header, parent: key })
  if (body != null) {
    for (const block of keyedBlocks(body, ':scope > *', `${key}>`, key)) {
      targets.push(block)
      for (const row of keyedBlocks(block.el, '.plan-row, .tpl-row', `${block.key}>`, block.key)) targets.push(row)
    }
  }
  const bar = root.querySelector<HTMLElement>('.pb-actions')
  if (bar != null) targets.push({ key: `${key}>actions`, el: bar, parent: key })
  return targets
}

/** CSS.escape where there is one (jsdom has none). */
const escape = (text: string) => (typeof CSS !== 'undefined' && typeof CSS.escape === 'function' ? CSS.escape(text) : text.replace(/["\\]/g, '\\$&'))

function planTaps(mode: TrainingMode): Record<string, PictureTap> {
  const template = grit.guideSampleTemplate(mode).id
  const swap = grit.guideSampleSwap(mode)
  return {
    CHOOSE_TEMPLATE: { selector: '.plan-empty .gr-btn-primary', ink: 'accent-ink' },
    TEMPLATE: { selector: `.tpl-row[aria-labelledby="tpl-${escape(template)}-name"]`, ink: 'accent-text' },
    SWAP: { selector: `.tpl-draft button[aria-label="Swap ${escape(swap.from)}"]`, ink: 'accent-text' },
    USE_THIS_PLAN: { selector: '.pb-actions .gr-btn-primary', ink: 'accent-ink' },
  }
}

export const PLAN_PICTURE: PictureDef = { Draw: PlanPicture, outlined: true, height: PHONE_HEIGHT, targets: planTargets, taps: planTaps }
