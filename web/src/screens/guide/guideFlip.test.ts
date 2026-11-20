import { afterEach, describe, expect, it, vi } from 'vitest'
import { CHECK_LINE, FLIP_EASING, RING, RISE_PX, TICK, planFlip, readBoxes, startFlip, startRing, tapTarget, tickDots, todayTargets, type FlipBox } from './guideFlip'

/** The guide's layout motion (1.16, Task 2b), on boxes stubbed in: jsdom lays nothing out. */

afterEach(() => {
  document.body.replaceChildren()
  vi.restoreAllMocks()
})

const box = (x: number, y: number, width = 100, height = 40): FlipBox => ({ x, y, width, height })

/** A stub of the Web Animations API on one element: what it was asked to animate, and the animation's time. */
type Call = { keyframes: Keyframe[]; options: KeyframeAnimationOptions; animation: { currentTime: number | null; paused: boolean; cancelled: boolean } }

function stubAnimate(el: HTMLElement, calls: Call[] = []) {
  el.animate = ((keyframes: Keyframe[], options: KeyframeAnimationOptions) => {
    const animation = {
      currentTime: null as number | null,
      paused: false,
      cancelled: false,
      pause() {
        this.paused = true
      },
      cancel() {
        this.cancelled = true
      },
    }
    calls.push({ keyframes, options, animation })
    return animation as unknown as Animation
  }) as HTMLElement['animate']
  return calls
}

/** An element whose box, on screen, is `r` (a rect read is all the guide asks of layout). */
function placed(el: HTMLElement, r: FlipBox) {
  el.getBoundingClientRect = () => ({ left: r.x, top: r.y, width: r.width, height: r.height, right: r.x + r.width, bottom: r.y + r.height, x: r.x, y: r.y, toJSON: () => ({}) }) as DOMRect
  return el
}

describe('guideFlip', () => {
  it('guideFlip_movesARowFromItsOldBoxToItsNew', () => {
    const before = new Map([
      ['item:Squat:card', box(0, 0, 100, 300)],
      ['add', box(0, 400)],
      ['item:Bench:row', box(0, 320)],
    ])
    const after = new Map([
      ['item:Squat:card', box(0, 0, 100, 360)],
      ['add', box(0, 380)],
      ['skipped', box(0, 440)],
    ])
    const plan = planFlip(before, after, new Map([...after.keys()].map((k) => [k, null])))
    // Add exercise slides up from where it was; the card did not move (it grew: its box is its own).
    expect(plan.moves).toEqual([{ key: 'add', dx: 0, dy: 20 }])
    expect(plan.enters).toEqual(['skipped'])
    expect(plan.exits).toEqual(['item:Bench:row'])
    expect(plan.crossFades).toEqual([])

    // On the element: paused, at its old box (translated by old - new), and back to its own box at the move's end.
    const add = placed(document.createElement('div'), { x: 0, y: 380, width: 100, height: 40 })
    const calls = stubAnimate(add)
    const run = startFlip(plan, [{ key: 'add', el: add, parent: null }], new Map(), null, { moveMs: 280, fadeMs: 150, easing: FLIP_EASING })
    expect(calls).toHaveLength(1)
    expect(calls[0]!.keyframes).toEqual([{ transform: 'translate(0px, 20px)' }, { transform: 'none' }])
    expect(calls[0]!.options).toMatchObject({ duration: 280, easing: FLIP_EASING })
    expect(calls[0]!.animation.paused).toBe(true)
    expect(calls[0]!.animation.currentTime).toBe(0)
    // The guide's clock moves it.
    run.seek(140)
    expect(calls[0]!.animation.currentTime).toBe(140)
    run.finish()
    expect(calls[0]!.animation.cancelled).toBe(true)
  })

  it('a part of the open card moves only by what its card does not carry it', () => {
    const before = new Map([
      ['item:A:card', box(0, 100)],
      ['item:A:card>log:0', box(0, 200)],
    ])
    const after = new Map([
      ['item:A:card', box(0, 90)],
      ['item:A:card>log:0', box(0, 250)],
    ])
    const plan = planFlip(before, after, new Map([
      ['item:A:card', null],
      ['item:A:card>log:0', 'item:A:card'],
    ]))
    expect(plan.moves).toEqual([
      { key: 'item:A:card', dx: 0, dy: 10 },
      { key: 'item:A:card>log:0', dx: 0, dy: -60 },
    ])
  })

  it('a new row where a gone one was cross-fades with it, at once (the plates in place of "No weight yet")', () => {
    const before = new Map([
      ['item:A:card>plates-note:0', box(0, 200, 100, 48)],
      ['item:A:card>log:0', box(0, 260)],
    ])
    const after = new Map([
      ['item:A:card>plates:0', box(0, 200, 100, 52)],
      ['item:A:card>log:0', box(0, 264)],
    ])
    const plan = planFlip(before, after, new Map([
      ['item:A:card>plates:0', null],
      ['item:A:card>log:0', null],
    ]))
    expect(plan.crossFades).toEqual(['item:A:card>plates:0'])
    const strip = placed(document.createElement('div'), box(0, 200, 100, 52))
    const log = placed(document.createElement('div'), box(0, 264))
    const stripCalls = stubAnimate(strip)
    stubAnimate(log)
    const targets = [
      { key: 'item:A:card>plates:0', el: strip, parent: null },
      { key: 'item:A:card>log:0', el: log, parent: null },
    ]
    startFlip(plan, targets, new Map(), null, { moveMs: 280, fadeMs: 150, easing: FLIP_EASING })
    expect(stripCalls[0]!.options).toMatchObject({ delay: 0 })
  })

  it('reads boxes in the phone\'s own space: less the phone\'s place, divided by its scale', () => {
    const phone = placed(document.createElement('div'), { x: 40, y: 100, width: 195, height: 460 })
    const row = placed(document.createElement('div'), { x: 48, y: 150, width: 180, height: 20 })
    const boxes = readBoxes([{ key: 'row', el: row, parent: null }], phone, 0.5)
    expect(boxes.get('row')).toEqual({ x: 16, y: 100, width: 360, height: 40 })
  })

  it('keys the Today rows by what they are, never by their place', () => {
    document.body.innerHTML = `
      <div class="screen">
        <div class="today-item"><div class="gr-card"><div class="today-card-head"><h2 class="today-name">Squat</h2></div>
          <div class="gr-set gr-set-done"></div><hr class="today-hairline"><div class="today-steppers"></div>
          <div class="gr-plates"></div><div class="today-log"></div></div></div>
        <div class="today-item"><button class="today-row"><span class="today-name">Bench</span></button></div>
        <button class="today-add"></button>
        <section class="today-skipped"></section>
      </div>`
    const keys = todayTargets(document.body.firstElementChild as HTMLElement).map((t) => [t.key, t.parent])
    expect(keys).toEqual([
      ['item:Squat:card', null],
      ['item:Squat:card>set:0', 'item:Squat:card'],
      ['item:Squat:card>hairline:0', 'item:Squat:card'],
      ['item:Squat:card>steppers:0', 'item:Squat:card'],
      ['item:Squat:card>plates:0', 'item:Squat:card'],
      ['item:Squat:card>log:0', 'item:Squat:card'],
      ['item:Bench:row', null],
      ['add', null],
      ['skipped', null],
    ])
  })

  it('guideFlip_usesOnlyTransformAndOpacity: moves, fades in and fades out; only the tick\'s line draws by its dash', () => {
    const moved = placed(document.createElement('div'), box(0, 0))
    const added = placed(document.createElement('div'), box(0, 60))
    const gone = document.createElement('div')
    const ghosts = document.createElement('div')
    const { row, dot } = setRow()
    document.body.append(moved, added, ghosts, row)
    const calls = stubAnimate(moved)
    const addedCalls = stubAnimate(added)
    const dotCalls = stubAnimate(dot)
    // The ghost (a copy of the gone row) and the tick's stand-in are made inside: each copy gets the stub as it is made.
    const made = stubClones()
    const run = startFlip(
      { moves: [{ key: 'moved', dx: 0, dy: -12 }], enters: ['added'], exits: ['gone'], crossFades: [] },
      [
        { key: 'moved', el: moved, parent: null },
        { key: 'added', el: added, parent: null },
      ],
      new Map([['gone', { el: gone, box: box(0, 120) }]]),
      ghosts,
      { moveMs: 280, fadeMs: 150, easing: FLIP_EASING },
      [{ row: 'added', dot, box: box(300, 60, 24, 24) }],
    )
    // The gone row is drawn again where it was, over the screen, and fades.
    expect(ghosts.children).toHaveLength(2)
    const [ghost, stand] = [...ghosts.children].sort((a) => ((a as HTMLElement).style.top === '120px' ? -1 : 1)) as HTMLElement[]
    expect([ghost!.style.position, ghost!.style.top]).toEqual(['absolute', '120px'])
    const ghostCalls = made.find((m) => m.el === ghost)!.calls
    const tickCalls = made.find((m) => m.el === stand)!.calls
    const all = [...calls, ...addedCalls, ...ghostCalls, ...dotCalls, ...tickCalls]
    expect(all).toHaveLength(6)
    for (const { keyframes } of all) {
      const properties = keyframes.flatMap((frame) => Object.keys(frame).filter((p) => !['offset', 'easing'].includes(p)))
      // The one exception: the check's line, drawn by its dash and nothing else.
      if (properties.includes('strokeDashoffset')) expect(new Set(properties)).toEqual(new Set(['strokeDashoffset']))
      else for (const property of properties) expect(['transform', 'opacity']).toContain(property)
    }
    expect(all.filter((c) => c.keyframes.some((k) => 'strokeDashoffset' in k))).toHaveLength(1)
    expect(addedCalls[0]!.keyframes).toEqual([{ opacity: 0 }, { opacity: 1 }])
    // Not in a gone row's place, with rows moving: it fades in over the move's last stretch.
    expect(addedCalls[0]!.options).toMatchObject({ delay: 130, duration: 150 })
    expect(ghostCalls.at(-1)!.keyframes).toEqual([{ opacity: 1 }, { opacity: 0 }])
    run.finish()
    expect(ghosts.children).toHaveLength(0)
  })

  /** A logged set's row as SetRow draws it: the dot with its filled check. */
  function setRow() {
    const row = document.createElement('div')
    row.className = 'gr-set gr-set-done'
    row.innerHTML = '<span class="gr-set-dot gr-set-dot-done"><svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"></path></svg></span>'
    return { row, dot: row.querySelector<HTMLElement>('.gr-set-dot-done')! }
  }

  /** Every element made by cloning, stubbed for animation as it is made; and the calls on each. */
  function stubClones() {
    const made: { el: HTMLElement; calls: Call[] }[] = []
    const clone = Node.prototype.cloneNode
    vi.spyOn(Node.prototype, 'cloneNode').mockImplementation(function (this: Node, deep?: boolean) {
      const copy = clone.call(this, deep) as HTMLElement
      const calls: Call[] = []
      stubAnimate(copy, calls)
      copy.querySelectorAll?.('*').forEach((el) => stubAnimate(el as HTMLElement, calls))
      made.push({ el: copy, calls })
      return copy
    })
    return made
  }

  it('guideFlip_tick_popsItsCircleAndDrawsItsLine_asItsRowComesIn', () => {
    const { row, dot } = setRow()
    const ghosts = document.createElement('div')
    document.body.append(row, ghosts)
    const rowCalls = stubAnimate(row)
    const dotCalls = stubAnimate(dot)
    const made = stubClones()
    const timing = { moveMs: 280, fadeMs: 150, easing: FLIP_EASING }
    const run = startFlip(
      { moves: [{ key: 'item:Squat:card>steppers:0', dx: 0, dy: -48 }], enters: ['set'], exits: [], crossFades: [] },
      [{ key: 'set', el: row, parent: null }],
      new Map(),
      ghosts,
      timing,
      [{ row: 'set', dot, box: box(300, 200, 24, 24) }],
    )
    // A stand-in over the real dot, where it is, at its size: the real one is hidden while it draws.
    expect(ghosts.children).toHaveLength(1)
    const stand = ghosts.firstElementChild as HTMLElement
    expect(stand.classList.contains('gr-set-dot-done')).toBe(true)
    expect([stand.style.position, stand.style.left, stand.style.top, stand.style.width, stand.style.height]).toEqual(['absolute', '300px', '200px', '24px', '24px'])
    expect(dotCalls.map((c) => c.keyframes)).toEqual([[{ opacity: 0 }, { opacity: 0 }]])
    expect(dotCalls[0]!.options).toMatchObject({ duration: 280, fill: 'both' })
    // The row fades in over the move's last stretch; the tick plays as it does, and is over when the move is.
    expect(rowCalls[0]!.options).toMatchObject({ delay: 130, duration: 150 })
    const own = made.find((m) => m.el === stand)!.calls
    const circle = own.find((c) => c.keyframes.some((k) => 'transform' in k))!
    expect(circle.keyframes).toEqual([
      { opacity: 0, transform: `scale(${TICK.scaleFrom})`, offset: 0 },
      { opacity: 1, transform: `scale(${TICK.scalePeak})`, offset: TICK.peakAt },
      { opacity: 1, transform: 'none', offset: TICK.settleAt },
      { opacity: 1, transform: 'none', offset: 1 },
    ])
    expect(circle.options).toMatchObject({ delay: 130, duration: 150, fill: 'both' })
    // The check is the same line, stroked: drawn from its start to its end.
    const path = stand.querySelector('path')!
    expect(path.getAttribute('d')).toBe(CHECK_LINE)
    expect([path.getAttribute('fill'), path.getAttribute('stroke'), path.getAttribute('pathLength'), path.getAttribute('stroke-dasharray')]).toEqual(['none', 'currentColor', '1', '1'])
    const line = own.find((c) => c.keyframes.some((k) => 'strokeDashoffset' in k))!
    expect(line.keyframes).toEqual([
      { strokeDashoffset: 1, offset: 0 },
      { strokeDashoffset: 1, offset: TICK.drawFrom },
      { strokeDashoffset: 0, offset: 1 },
    ])
    expect(line.options).toMatchObject({ delay: 130, duration: 150 })
    for (const c of [...own, ...dotCalls]) expect(c.animation.paused).toBe(true)
    // The guide's clock moves it all.
    run.seek(200)
    for (const c of [...own, ...dotCalls]) expect(c.animation.currentTime).toBe(200)
    // Settled: the stand-in gone, the real dot as it was.
    run.finish()
    expect(ghosts.children).toHaveLength(0)
    expect(dotCalls[0]!.animation.cancelled).toBe(true)
  })

  it('the stroked check covers what the filled one does: the same arms, as wide', () => {
    // The filled check's arms are 2 units across (its edges 1.41 apart along both axes); its centre line runs between.
    expect(CHECK_LINE).toBe('M4.12 12.705L9 17.585L20.295 6.295')
    expect(TICK.lineWidth).toBe(2)
  })

  it('tickDots: the done dot of each new set row, never one already there', () => {
    const { row, dot } = setRow()
    const old = setRow()
    placed(dot, { x: 330, y: 230, width: 24, height: 24 })
    const phone = placed(document.createElement('div'), box(30, 30, 412, 920))
    const targets = [
      { key: 'item:Squat:card>set:0', el: old.row, parent: 'item:Squat:card' },
      { key: 'item:Squat:card>set:1', el: row, parent: 'item:Squat:card' },
    ]
    expect(tickDots(['item:Squat:card>set:1'], targets, phone, 1)).toEqual([{ row: 'item:Squat:card>set:1', dot, box: box(300, 200, 24, 24) }])
    expect(tickDots([], targets, phone, 1)).toEqual([])
  })

  it('guideFlip_skippedLine_risesWhileFadingIn', () => {
    const skipped = placed(document.createElement('div'), box(0, 600))
    const calls = stubAnimate(skipped)
    startFlip({ moves: [{ key: 'add', dx: 0, dy: 60 }], enters: ['skipped'], exits: [], crossFades: [] }, [{ key: 'skipped', el: skipped, parent: null }], new Map(), null, { moveMs: 280, fadeMs: 150, easing: FLIP_EASING })
    expect(RISE_PX).toBe(14)
    expect(calls.map((c) => c.keyframes)).toEqual([[{ opacity: 0, transform: `translateY(${RISE_PX}px)` }, { opacity: 1, transform: 'none' }]])
    // The fade it had: over the move's last stretch, once the rows around it have made its room.
    expect(calls[0]!.options).toMatchObject({ delay: 130, duration: 150, easing: 'linear' })
  })

  it('guideRing: one ring on the button, growing as it fades, on the guide\'s time; gone when finished', () => {
    const ghosts = document.createElement('div')
    const made: Call[] = []
    const create = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation(((tag: string) => {
      const el = create(tag)
      stubAnimate(el, made)
      return el
    }) as typeof document.createElement)
    const ring = startRing(ghosts, box(200, 700, 160, 48), 280)!
    expect(ghosts.children).toHaveLength(1)
    const el = ghosts.firstElementChild as HTMLElement
    expect(el.className).toBe('guide-tap-ring')
    // Centred on the button.
    expect([el.style.left, el.style.top, el.style.width, el.style.height]).toEqual([`${280 - RING.sizePx / 2}px`, `${724 - RING.sizePx / 2}px`, `${RING.sizePx}px`, `${RING.sizePx}px`])
    expect(el.style.borderWidth).toBe(`${RING.linePx}px`)
    expect(made).toHaveLength(1)
    expect(made[0]!.keyframes).toEqual([
      { opacity: RING.opacityFrom, transform: `scale(${RING.scaleFrom})` },
      { opacity: 0, transform: `scale(${RING.scaleTo})` },
    ])
    expect(made[0]!.options).toMatchObject({ duration: 280, easing: 'linear', fill: 'both' })
    expect(made[0]!.animation.paused).toBe(true)
    ring.seek(140)
    expect(made[0]!.animation.currentTime).toBe(140)
    ring.finish()
    expect(ghosts.children).toHaveLength(0)
    expect(made[0]!.animation.cancelled).toBe(true)
  })

  it('guideRing: nothing is drawn where the page cannot animate it (it would sit there, still)', () => {
    const ghosts = document.createElement('div')
    expect(startRing(ghosts, box(0, 0), 280)).toBeNull()
    expect(ghosts.children).toHaveLength(0)
  })

  it('tapTarget: Log set on the open card, in the phone\'s space', () => {
    const screen = document.createElement('div')
    screen.innerHTML = '<div class="today-item"><div class="today-card-head"></div><div class="today-log"><button class="gr-btn gr-btn-secondary"></button><button class="gr-btn gr-btn-primary today-grow"></button></div></div>'
    placed(screen.querySelector<HTMLElement>('.gr-btn-primary')!, { x: 230, y: 730, width: 160, height: 48 })
    const phone = placed(document.createElement('div'), box(30, 30, 412, 920))
    expect(tapTarget('LOG_SET', screen, phone, 1)).toEqual(box(200, 700, 160, 48))
    expect(tapTarget('LOG_SET', document.createElement('div'), phone, 1)).toBeNull()
  })
})
