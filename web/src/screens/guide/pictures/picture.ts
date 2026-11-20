import type { ReactNode } from 'react'
import type { TrainingMode } from '../../../db/schema'
import type { FlipTarget } from '../guideFlip'

/** What a picture is drawn from at one moment: its step, and how far into the move into it the guide's clock is. */
export type PictureProps = {
  mode: TrainingMode
  step: string
  stepIndex: number
  /** 0 to 1, eased, through the move into `step`; 1 while it holds. */
  progress: number
  /** Whether the picture moves at all (motion off: its last step, still). */
  motionOn: boolean
  /** A held loop time (tests): a picture with a loop of its own (screen 2) draws that moment and runs no loop. */
  heldLoopMs?: number
}

/** A control a step stands for pressing: where it is drawn in the picture, and the ink its ring is drawn in. */
export type PictureTap = { selector: string; ink: 'accent-ink' | 'accent-text' }

/**
 * One screen's picture (1.16): what it draws, on a phone `height` tall (or as art filling its place), and what the guide's layout motion moves in it
 * (`targets`, none for a picture that moves itself), what it rings (`taps`, by `GuideTap` name) and which new rows
 * rise as they fade in (`rises`, by target key).
 */
export type PictureDef = {
  Draw: (props: PictureProps) => ReactNode
  /**
   * A drawn phone, in its outline, one size on every such screen (spec point 11; screen 8 gives up the room of its
   * second button when it has one); false for art that is no screen of the app (WELCOME, SIDE): it fills the picture's
   * place, with no phone and no outline.
   */
  outlined: boolean
  /** The drawn phone's height (outlined pictures). */
  height: number
  targets?: (screen: HTMLElement) => FlipTarget[]
  taps?: (mode: TrainingMode) => Record<string, PictureTap>
  rises?: (key: string) => boolean
}

/**
 * The blocks matching `selector` in `root`, each keyed by `prefix`, its class and its first heading (or label), so a
 * block keeps its key when others come or go before it; a key met twice gets its count. `parent` is their parent row.
 */
export function keyedBlocks(root: ParentNode, selector: string, prefix: string, parent: string | null = null): FlipTarget[] {
  const seen = new Map<string, number>()
  return [...root.querySelectorAll<HTMLElement>(selector)].map((el) => {
    const name = el.getAttribute('aria-label') ?? el.querySelector('h1, h2, h3, .gr-eyebrow, .gr-section-title')?.textContent ?? ''
    const base = `${prefix}${el.className}:${name}`
    const n = seen.get(base) ?? 0
    seen.set(base, n + 1)
    return { key: n === 0 ? base : `${base}#${n}`, el, parent }
  })
}
