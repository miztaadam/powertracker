import { afterEach, describe, expect, it, vi } from 'vitest'
import { FLIP_EASING, planFlip, readBoxes, startFlip, todayTargets, type FlipBox } from './guideFlip'

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

  it('guideFlip_usesOnlyTransformAndOpacity: moves, fades in and fades out', () => {
    const moved = placed(document.createElement('div'), box(0, 0))
    const added = placed(document.createElement('div'), box(0, 60))
    const gone = document.createElement('div')
    const ghosts = document.createElement('div')
    document.body.append(moved, added, ghosts)
    const calls = stubAnimate(moved)
    const addedCalls = stubAnimate(added)
    // The ghost is a copy of the gone row, made inside: the copy gets the stub as it is made.
    const ghostCalls: Call[] = []
    const clone = HTMLElement.prototype.cloneNode
    vi.spyOn(HTMLElement.prototype, 'cloneNode').mockImplementation(function (this: HTMLElement, deep?: boolean) {
      const copy = clone.call(this, deep) as HTMLElement
      stubAnimate(copy, ghostCalls)
      return copy
    })
    const run = startFlip(
      { moves: [{ key: 'moved', dx: 0, dy: -12 }], enters: ['added'], exits: ['gone'], crossFades: [] },
      [
        { key: 'moved', el: moved, parent: null },
        { key: 'added', el: added, parent: null },
      ],
      new Map([['gone', { el: gone, box: box(0, 120) }]]),
      ghosts,
      { moveMs: 280, fadeMs: 150, easing: FLIP_EASING },
    )
    // The gone row is drawn again where it was, over the screen, and fades.
    expect(ghosts.children).toHaveLength(1)
    const ghost = ghosts.firstElementChild as HTMLElement
    expect([ghost.style.position, ghost.style.top]).toEqual(['absolute', '120px'])
    const all = [...calls, ...addedCalls, ...ghostCalls]
    expect(all).toHaveLength(3)
    for (const { keyframes } of all) {
      for (const frame of keyframes) for (const property of Object.keys(frame)) expect(['transform', 'opacity', 'offset', 'easing']).toContain(property)
    }
    expect(addedCalls[0]!.keyframes).toEqual([{ opacity: 0 }, { opacity: 1 }])
    // Not in a gone row's place, with rows moving: it fades in over the move's last stretch.
    expect(addedCalls[0]!.options).toMatchObject({ delay: 130, duration: 150 })
    expect(ghostCalls.at(-1)!.keyframes).toEqual([{ opacity: 1 }, { opacity: 0 }])
    run.finish()
    expect(ghosts.children).toHaveLength(0)
  })
})
