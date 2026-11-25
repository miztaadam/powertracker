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

/**
 * What a step change does to each row: moved by (dx, dy), new (faded in), or gone (faded out where it was). A new row
 * drawn where a gone one was (`crossFades`, a subset of `enters`) fades in as the other fades out.
 */
export type FlipPlan = {
  moves: { key: string; dx: number; dy: number }[]
  enters: string[]
  exits: string[]
  crossFades: string[]
}

/** A move under this many pixels (in the phone's space) is no move. */
const STILL_PX = 0.5

/** The parts of the open card that shift when something above them inside the card comes or goes. */
const CARD_PARTS: [string, string][] = [
  ['.gr-set', 'set'],
  ['.today-last', 'last'],
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
  const plan: FlipPlan = { moves: [], enters: [], exits: [], crossFades: [] }
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
  const overlaps = (a: FlipBox, b: FlipBox) => a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height
  plan.crossFades = plan.enters.filter((key) => plan.exits.some((gone) => overlaps(after.get(key)!, before.get(gone)!)))
  return plan
}

/*
 * The owner's three extras (1.16, Task 2c), as the sketch they approved draws them (its 400 ms window is the guide's
 * move here): the tick of a set logged in the step pops and draws its line, a ring pulses on the button pressed just
 * before a step's result, and the "Skipped today" line rises as it fades in. The guide's own: the real screens are
 * untouched. Transform and opacity only, except the check's line, which draws by `stroke-dashoffset`.
 */

/**
 * The tick: its circle grows from `scaleFrom` past full size (`scalePeak`, at `peakAt` of its time) and settles (at
 * `settleAt`) as it fades in; its line is drawn from `drawFrom` to the end. `lineWidth`: the check's arms, in its 24
 * units.
 */
export const TICK = { scaleFrom: 0.4, scalePeak: 1.12, peakAt: 0.4, settleAt: 0.7, drawFrom: 0.2, lineWidth: 2 }

/**
 * The filled check's (CheckIcon's) centre line, on its 24-unit grid: stroked `TICK.lineWidth` wide with square ends and
 * a mitred corner, it covers what the filled check does, so the drawn line ends as the real tick.
 */
export const CHECK_LINE = 'M4.12 12.705L9 17.585L20.295 6.295'

/** The tap ring: `sizePx` across with a `linePx` line, growing from `scaleFrom` to `scaleTo` as it fades from `opacityFrom`. */
export const RING = { sizePx: 14, linePx: 2, scaleFrom: 0.6, scaleTo: 4.2, opacityFrom: 0.9 }

/** How far the "Skipped today" line rises as it fades in. */
export const RISE_PX = 14

/** A set logged in the step: the key of its row, its done dot, and the dot's box. */
export type FlipTick = { row: string; dot: HTMLElement; box: FlipBox }

/** The done dots of the set rows new in a step (`enters`): one read each, in the step change's read pass. */
export function tickDots(enters: string[], targets: FlipTarget[], phone: HTMLElement, scale: number): FlipTick[] {
  const ticks: FlipTick[] = []
  for (const t of targets) {
    if (!enters.includes(t.key) || !/>set:\d+$/.test(t.key)) continue
    const dot = t.el.querySelector<HTMLElement>('.gr-set-dot-done')
    if (dot == null) continue
    const box = readBoxes([{ key: t.key, el: dot, parent: null }], phone, scale).get(t.key)!
    ticks.push({ row: t.key, dot, box })
  }
  return ticks
}

/** Where Today's control a step can stand for pressing is drawn (the domain's `GuideTap` names). */
export const TODAY_TAP_SELECTORS: Record<string, string> = {
  LOG_SET: '.today-item .today-log .gr-btn-primary',
}

/**
 * The box of the control `tap` in the picture drawn in `screen`, in `phone`'s space: one read. Null if not drawn.
 * `selector` is where the picture draws it (by default Today's).
 */
export function tapTarget(tap: string, screen: HTMLElement, phone: HTMLElement, scale: number, selector: string | undefined = TODAY_TAP_SELECTORS[tap]): FlipBox | null {
  const el = selector == null ? null : screen.querySelector<HTMLElement>(selector)
  if (el == null) return null
  return readBoxes([{ key: tap, el, parent: null }], phone, scale).get(tap)!
}

/** A ring on the go: `seek` moves it to a time into the ring, `finish` takes it away. */
export type RingRun = { seek: (ms: number) => void; finish: () => void }

/**
 * Starts the tap ring on a control drawn at `box`, paused at its start, in `ghosts` (over the screen, taking nothing).
 * Null where the page cannot animate (it would only sit there).
 */
export function startRing(ghosts: HTMLElement, box: FlipBox, tapMs: number, ink: 'accent-ink' | 'accent-text' = 'accent-ink'): RingRun | null {
  const ring = document.createElement('div')
  if (typeof ring.animate !== 'function') return null
  // In the pressed control's own ink: a filled button's, or the accent text of a card or an outlined button.
  ring.className = ink === 'accent-text' ? 'guide-tap-ring guide-tap-ring-text' : 'guide-tap-ring'
  ring.style.left = `${box.x + box.width / 2 - RING.sizePx / 2}px`
  ring.style.top = `${box.y + box.height / 2 - RING.sizePx / 2}px`
  ring.style.width = `${RING.sizePx}px`
  ring.style.height = `${RING.sizePx}px`
  ring.style.borderWidth = `${RING.linePx}px`
  ghosts.appendChild(ring)
  const animation = ring.animate(
    [
      { opacity: RING.opacityFrom, transform: `scale(${RING.scaleFrom})` },
      { opacity: 0, transform: `scale(${RING.scaleTo})` },
    ],
    { duration: tapMs, easing: 'linear', fill: 'both' },
  )
  animation.pause()
  animation.currentTime = 0
  return {
    seek(ms) {
      animation.currentTime = Math.max(0, ms)
    },
    finish() {
      animation.cancel()
      ring.remove()
    },
  }
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
  ticks: FlipTick[] = [],
  rises: (key: string) => boolean = (key) => key === 'skipped',
): FlipRun {
  const byKey = new Map(after.map((t) => [t.key, t.el]))
  const animations: Animation[] = []
  const placed: HTMLElement[] = []
  const add = (el: HTMLElement | SVGElement, keyframes: Keyframe[], duration: number, easing: string, delay = 0) => {
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
  // while they still slide past it); with nothing moving, or in a gone row's place (a cross-fade), at once.
  const enterDelay = plan.moves.length > 0 ? Math.max(0, timing.moveMs - timing.fadeMs) : 0
  const delayOf = (key: string) => (plan.crossFades.includes(key) ? 0 : enterDelay)
  for (const key of plan.enters) {
    const el = byKey.get(key)
    // The "Skipped today" line (and what else the picture says) rises as it fades in.
    const frames: Keyframe[] = rises(key) ? [{ opacity: 0, transform: `translateY(${RISE_PX}px)` }, { opacity: 1, transform: 'none' }] : [{ opacity: 0 }, { opacity: 1 }]
    if (el) add(el, frames, timing.fadeMs, 'linear', delayOf(key))
  }
  // A set logged in the step: its tick plays as its row comes in, and is over when the move is. A stand-in draws it
  // over the real dot, which is hidden meanwhile: the real check is a filled shape, the stand-in's a line drawn in.
  if (ghosts != null) {
    for (const tick of ticks) {
      if (typeof tick.dot.animate !== 'function') continue
      const delay = delayOf(tick.row)
      const duration = Math.max(1, timing.moveMs - delay)
      const stand = tick.dot.cloneNode(true) as HTMLElement
      stand.style.position = 'absolute'
      stand.style.left = `${tick.box.x}px`
      stand.style.top = `${tick.box.y}px`
      stand.style.width = `${tick.box.width}px`
      stand.style.height = `${tick.box.height}px`
      stand.style.margin = '0'
      const path = stand.querySelector('path')
      ghosts.appendChild(stand)
      placed.push(stand)
      add(tick.dot, [{ opacity: 0 }, { opacity: 0 }], timing.moveMs, 'linear')
      add(
        stand,
        [
          { opacity: 0, transform: `scale(${TICK.scaleFrom})`, offset: 0 },
          { opacity: 1, transform: `scale(${TICK.scalePeak})`, offset: TICK.peakAt },
          { opacity: 1, transform: 'none', offset: TICK.settleAt },
          { opacity: 1, transform: 'none', offset: 1 },
        ],
        duration,
        'linear',
        delay,
      )
      if (path != null) {
        path.setAttribute('d', CHECK_LINE)
        path.setAttribute('fill', 'none')
        path.setAttribute('stroke', 'currentColor')
        path.setAttribute('stroke-width', String(TICK.lineWidth))
        path.setAttribute('stroke-linecap', 'butt')
        path.setAttribute('stroke-linejoin', 'miter')
        path.setAttribute('pathLength', '1')
        path.setAttribute('stroke-dasharray', '1')
        add(
          path,
          [
            { strokeDashoffset: 1, offset: 0 },
            { strokeDashoffset: 1, offset: TICK.drawFrom },
            { strokeDashoffset: 0, offset: 1 },
          ],
          duration,
          'linear',
          delay,
        )
      }
    }
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
