/*
 * The guide's own layout motion (1.16, Task 2b): the web's real screens slide only from their actions (view
 * transitions), so a picture fed new state would jump. At a step change the guide reads the boxes of the picture's rows
 * once before the commit and once after it, then moves each row from its old box to its new with the Web Animations
 * API (transform only), fades in what is new and fades out what has gone (opacity only). Every animation is created
 * paused and is moved by the guide's clock (`seek`), as the mode switch moves its reveal, so a held clock holds it too.
 * Nothing here reads layout while a move runs.
 */

/** A box in the phone's own space: unscaled pixels from the phone's top left. */
export type FlipBox = { x: number; y: number; width: number; height: number }

/** A row of the picture, by an identity that holds across steps (never its index). */
export type FlipTarget = { key: string; el: HTMLElement; parent: string | null }

/** What a step change does to each row: moved by (dx, dy), new (faded in), or gone (faded out where it was). */
export type FlipPlan = {
  moves: { key: string; dx: number; dy: number }[]
  enters: string[]
  exits: string[]
}

/** A move under this many pixels (in the phone's space) is no move. */
const STILL_PX = 0.5

/** The parts of the open card that shift when something above them inside the card comes or goes. */
const CARD_PARTS: [string, string][] = [
  ['.gr-set', 'set'],
  ['.today-cues', 'cues'],
  ['.today-hairline', 'hairline'],
  ['.today-steppers', 'steppers'],
  ['.gr-plates-note', 'plates-note'],
  ['.gr-plates:not(.gr-plates-note)', 'plates'],
  ['.today-log', 'log'],
  ['.today-next', 'next'],
]

/**
 * The rows of the Today picture drawn in `screen`, each keyed by what it is: an exercise by its name and whether it is
 * the open card or a folded row; a part of the open card by the exercise and the part (a set by its number); then the
 * Add exercise button and the "Skipped today" line.
 */
export function todayTargets(screen: HTMLElement): FlipTarget[] {
  const targets: FlipTarget[] = []
  for (const item of screen.querySelectorAll<HTMLElement>('.today-item')) {
    const name = item.querySelector('.today-name')?.textContent ?? ''
    const card = item.querySelector('.today-card-head') != null
    const key = `item:${name}:${card ? 'card' : 'row'}`
    targets.push({ key, el: item, parent: null })
    if (!card) continue
    for (const [selector, part] of CARD_PARTS) {
      item.querySelectorAll<HTMLElement>(selector).forEach((el, i) => targets.push({ key: `${key}>${part}:${i}`, el, parent: key }))
    }
  }
  for (const [selector, key] of [
    ['.today-add', 'add'],
    ['.today-skipped', 'skipped'],
  ] as const) {
    const el = screen.querySelector<HTMLElement>(selector)
    if (el != null) targets.push({ key, el, parent: null })
  }
  return targets
}

/** The targets' boxes in `phone`'s own space, drawn at `scale`: one layout read for them all. */
export function readBoxes(targets: FlipTarget[], phone: HTMLElement, scale: number): Map<string, FlipBox> {
  const origin = phone.getBoundingClientRect()
  const boxes = new Map<string, FlipBox>()
  for (const t of targets) {
    const r = t.el.getBoundingClientRect()
    boxes.set(t.key, { x: (r.left - origin.left) / scale, y: (r.top - origin.top) / scale, width: r.width / scale, height: r.height / scale })
  }
  return boxes
}

/**
 * What a step change does: a row in both is moved from its old box to its new (less what its parent row's own move
 * already carries it by), a row only after is new, a row only before has gone.
 */
export function planFlip(before: Map<string, FlipBox>, after: Map<string, FlipBox>, parents: Map<string, string | null>): FlipPlan {
  const plan: FlipPlan = { moves: [], enters: [], exits: [] }
  const delta = (key: string) => {
    const a = before.get(key)
    const b = after.get(key)
    return a && b ? { dx: a.x - b.x, dy: a.y - b.y } : { dx: 0, dy: 0 }
  }
  for (const key of after.keys()) {
    if (!before.has(key)) {
      plan.enters.push(key)
      continue
    }
    const own = delta(key)
    const parent = parents.get(key)
    const carried = parent != null && before.has(parent) ? delta(parent) : { dx: 0, dy: 0 }
    const dx = own.dx - carried.dx
    const dy = own.dy - carried.dy
    if (Math.abs(dx) >= STILL_PX || Math.abs(dy) >= STILL_PX) plan.moves.push({ key, dx, dy })
  }
  for (const key of before.keys()) if (!after.has(key)) plan.exits.push(key)
  return plan
}

/** The timing of one step change's motion: the domain's move and fade, on the web's move curve. */
export type FlipTiming = { moveMs: number; fadeMs: number; easing: string }

/** The web's layout moves (vt.css, viewTransition.ts): the same curve. */
export const FLIP_EASING = 'cubic-bezier(0.2, 0.8, 0.2, 1)'

/** One step change's motion, paused: `seek` moves it to a time into the move, `finish` ends it and leaves no trace. */
export type FlipRun = { animations: Animation[]; seek: (ms: number) => void; finish: () => void }

/**
 * Starts a step change's motion, paused at its start: every moved row drawn at its old box, every new row clear (it
 * fades in over the move's last `fadeMs`), and every gone row (`gone`: its last drawn node and box) drawn again where it was, in `ghosts`, fully there.
 */
export function startFlip(
  plan: FlipPlan,
  after: FlipTarget[],
  gone: Map<string, { el: HTMLElement; box: FlipBox }>,
  ghosts: HTMLElement | null,
  timing: FlipTiming,
): FlipRun {
  const byKey = new Map(after.map((t) => [t.key, t.el]))
  const animations: Animation[] = []
  const placed: HTMLElement[] = []
  const add = (el: HTMLElement, keyframes: Keyframe[], duration: number, easing: string, delay = 0) => {
    // No Web Animations (jsdom): the rows simply are where they are.
    if (typeof el.animate !== 'function') return
    const animation = el.animate(keyframes, { duration, delay, easing, fill: 'both' })
    animation.pause()
    animation.currentTime = 0
    animations.push(animation)
  }
  for (const { key, dx, dy } of plan.moves) {
    const el = byKey.get(key)
    if (el) add(el, [{ transform: `translate(${dx}px, ${dy}px)` }, { transform: 'none' }], timing.moveMs, timing.easing)
  }
  // A new row fades in as the move ends, once the rows around it have made its room (it would show through them
  // while they still slide past it); with nothing moving, at once.
  const enterDelay = plan.moves.length > 0 ? Math.max(0, timing.moveMs - timing.fadeMs) : 0
  for (const key of plan.enters) {
    const el = byKey.get(key)
    if (el) add(el, [{ opacity: 0 }, { opacity: 1 }], timing.fadeMs, 'linear', enterDelay)
  }
  if (ghosts != null) {
    for (const key of plan.exits) {
      const g = gone.get(key)
      if (!g) continue
      const ghost = g.el.cloneNode(true) as HTMLElement
      // Where it was, at its size: placed once, here; only its opacity moves.
      ghost.style.position = 'absolute'
      ghost.style.left = `${g.box.x}px`
      ghost.style.top = `${g.box.y}px`
      ghost.style.width = `${g.box.width}px`
      ghost.style.margin = '0'
      ghosts.appendChild(ghost)
      placed.push(ghost)
      add(ghost, [{ opacity: 1 }, { opacity: 0 }], timing.fadeMs, 'linear')
    }
  }
  return {
    animations,
    seek(ms) {
      for (const a of animations) a.currentTime = Math.max(0, ms)
    },
    finish() {
      for (const a of animations) a.cancel()
      animations.length = 0
      for (const g of placed) g.remove()
      placed.length = 0
    },
  }
}
