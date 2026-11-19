import { useEffect, useRef } from 'react'
import { grit } from '../../domain'
import { currentScheme } from '../theme'
import { screenSize, switchCentre, useModeSwitch, type ModeSwitchKind, type ModeSwitchPainter } from './useModeSwitch'
import './ModeSwitchOverlay.css'

/**
 * The layer the mode switch animations are drawn on (1.15), above the whole app: inline SVG, built when an animation
 * starts and emptied when it ends, so at rest it is one empty element and nothing is worked out. Every number comes
 * from the shared motion model (`grit.modeSwitch*`); this file only puts them on the page, drawn imperatively on the
 * controller's one clock (a React render per frame would not keep time).
 */
export function ModeSwitchOverlay() {
  const controller = useModeSwitch()
  const host = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const painter = createPainter(host.current!)
    controller.setPainter(painter)
    return () => {
      controller.setPainter(null)
      painter.end()
    }
  }, [controller])
  return <div ref={host} className="ms-overlay" aria-hidden="true" />
}

const NS = 'http://www.w3.org/2000/svg'

/** The reveal's clip runs linearly from 0 to this radius over as many ms, so its time is its radius in px. */
const REVEAL_MAX = 100_000

/**
 * Light scheme (spec "Settled" 1): the effects keep the prototype's colours, but the flash is a dark punch at half
 * the opacity, and the cracks, the chalk dust and the veins get a darker edge. These are the web's values; Android
 * mirrors them.
 */
export const LIGHT_EDGES = {
  /** The dark under-stroke of each crack: the model's crack shadow at this opacity instead of .55. */
  crackShadowOpacity: 0.9,
  /** The chalk dust's gradient: the model's centre, then a grey rim. */
  chalk: [
    { offset: 0, color: '#E4E6E2', opacity: 0.9 },
    { offset: 0.55, color: '#8A9098', opacity: 0.5 },
    { offset: 1, color: '#8A9098', opacity: 0 },
  ],
  /** Under each core vein: a navy edge this much wider, at this opacity. */
  veinEdge: { color: '#1E3F7A', extraWidth: 1.6, opacity: 0.55 },
} as const

/** An SVG element whose attributes are written only when they change. */
type Node = { el: Element; last: Record<string, string> }

function node(tag: string, attrs: Record<string, string | number>, parent: Element): Node {
  const el = document.createElementNS(NS, tag)
  for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, String(v))
  parent.appendChild(el)
  return { el, last: {} }
}

function put(n: Node, name: string, value: string | number) {
  const text = typeof value === 'number' ? (Number.isInteger(value) ? String(value) : value.toFixed(3)) : value
  if (n.last[name] === text) return
  n.last[name] = text
  n.el.setAttribute(name, text)
}

/** Paths from the model name gradients by their own ids; on the page they carry the overlay's prefix. */
const own = (paint: string | null | undefined) => (paint == null ? 'none' : paint.replace('url(#', 'url(#ms-'))

type Measured = { el: HTMLElement; cy: number }

/** What hops (the drop) and swells (the pump): the header and each block of the screen. */
function measureBlocks(shell: HTMLElement | null): Measured[] {
  if (shell == null) return []
  const els = [...shell.querySelectorAll<HTMLElement>('.shell-main > .screen > .gr-screen-header, .shell-main > .screen > .screen-body > *')]
  for (const el of els) el.style.transform = ''
  return els.map((el) => {
    const box = el.getBoundingClientRect()
    return { el, cy: box.top + box.height / 2 }
  })
}

type Drop = {
  shadow: Node
  cracks: Node
  crackPaths: Node[]
  flash: Node
  ring1: Node
  ring2: Node
  dust: Node[]
  burst: Node
  ghosts: Node[]
  hammer: Node
  rune: Node
  sparks: Node[]
}

type Pump = {
  tint: Node
  halos: Node[]
  rings: Node[]
  edgeSoft: Node
  edge: Node
  veinsLayer: Node
  glowLayer: Node
  veins: { n: Node; d0: number; len: number; width: number; swell: boolean }[]
  nodes: { n: Node; d0: number; r: number }[]
  pulses: { n: Node; len: number }[]
}

function createPainter(host: HTMLDivElement): ModeSwitchPainter {
  const root = document.documentElement
  let kind: ModeSwitchKind | null = null
  let w = 0
  let h = 0
  let light = false
  let shell: HTMLElement | null = null
  let blocks: Measured[] = []
  let drop: Drop | null = null
  let pump: Pump | null = null
  let src = { x: 0, y: 0 }
  let labelBox: { left: number; top: number } | null = null
  let stamp: HTMLElement | null = null
  let pill: HTMLElement | null = null
  // The reveal.
  let transition: ViewTransition | null = null
  let clip: Animation | null = null
  let capturing = false
  let radius = 0
  let full = false

  function gradients(defs: Element) {
    for (const g of grit.modeSwitchGradients()) {
      const el = node(g.radial ? 'radialGradient' : 'linearGradient', g.radial ? { id: `ms-${g.id}` } : { id: `ms-${g.id}`, x1: g.x1, y1: g.y1, x2: g.x2, y2: g.y2 }, defs)
      for (const s of g.stops)
        node(
          'stop',
          {
            offset: s.offset,
            'stop-color': s.color,
            'stop-opacity': s.opacity,
          },
          el.el,
        )
    }
    if (light) {
      const chalk = node('radialGradient', { id: 'ms-gChalkLight' }, defs)
      for (const s of LIGHT_EDGES.chalk)
        node(
          'stop',
          {
            offset: s.offset,
            'stop-color': s.color,
            'stop-opacity': s.opacity,
          },
          chalk.el,
        )
    }
  }

  function buildDrop(svg: Element): Drop {
    const colors = grit.modeSwitchColors()
    const hit = grit.modeSwitchHitPoint(w, h)
    const defs = node('defs', {}, svg).el
    gradients(defs)
    const shape = node('g', { id: 'ms-hammer' }, defs).el
    for (const p of grit.modeSwitchHammerPaths()) {
      node(
        'path',
        {
          d: p.d,
          fill: own(p.fill),
          stroke: own(p.stroke),
          'stroke-width': p.strokeWidth,
          opacity: p.opacity,
          'stroke-linecap': p.lineCap,
          'stroke-linejoin': p.lineJoin,
        },
        shape,
      )
    }
    const shadow = node('ellipse', { fill: colors.shadow, opacity: 0 }, svg)
    const cracks = node(
      'g',
      {
        fill: 'none',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        opacity: 0,
      },
      svg,
    )
    const crackPaths = grit.modeSwitchCracks(w, h, hit.x, hit.y).map((p) => {
      const shade = light && p.stroke === colors.crackShadow
      return node(
        'path',
        {
          class: 'ms-crack',
          d: p.d,
          stroke: p.stroke ?? 'none',
          'stroke-width': p.strokeWidth,
          opacity: shade ? LIGHT_EDGES.crackShadowOpacity : p.opacity,
          pathLength: 1,
          'stroke-dasharray': 1,
          'stroke-dashoffset': 1,
        },
        cracks.el,
      )
    })
    const flash = node(
      'rect',
      {
        class: 'ms-flash',
        x: 0,
        y: 0,
        width: w,
        height: h,
        fill: light ? colors.flashLight : colors.flash,
        opacity: 0,
      },
      svg,
    )
    const ring1 = node(
      'circle',
      {
        cx: hit.x,
        cy: hit.y,
        r: 0,
        fill: 'none',
        stroke: colors.ring1,
        opacity: 0,
      },
      svg,
    )
    const ring2 = node(
      'circle',
      {
        cx: hit.x,
        cy: hit.y,
        r: 0,
        fill: 'none',
        stroke: colors.ring2,
        opacity: 0,
      },
      svg,
    )
    const dustLayer = node('g', {}, svg).el
    const dust = grit.modeSwitchDust(hit.x, hit.y).map(() =>
      node(
        'circle',
        {
          fill: light ? 'url(#ms-gChalkLight)' : 'url(#ms-gChalk)',
          opacity: 0,
          r: 0,
        },
        dustLayer,
      ),
    )
    const points = grit
      .modeSwitchBurst(hit.x, hit.y)
      .map((p) => `${p.x},${p.y}`)
      .join(' ')
    const burst = node('polygon', { points, fill: colors.burst, opacity: 0 }, svg)
    const ghostLayer = node('g', {}, svg).el
    const ghosts = [0, 1, 2].map(() => node('use', { href: '#ms-hammer', opacity: 0 }, ghostLayer))
    const hammer = node('g', { opacity: 0 }, svg)
    node('use', { href: '#ms-hammer' }, hammer.el)
    const r = grit.modeSwitchHammerRune()
    const rune = node(
      'path',
      {
        d: r.d,
        fill: 'none',
        stroke: r.stroke ?? 'none',
        'stroke-width': r.strokeWidth,
        'stroke-linecap': r.lineCap,
        'stroke-linejoin': r.lineJoin,
        opacity: 0,
      },
      hammer.el,
    )
    const sparkLayer = node(
      'g',
      {
        stroke: colors.spark,
        'stroke-width': grit.modeSwitchSparkWidth(),
        'stroke-linecap': 'round',
      },
      svg,
    ).el
    const sparks = grit.modeSwitchSparks(hit.x, hit.y).map(() => node('line', { opacity: 0 }, sparkLayer))
    return {
      shadow,
      cracks,
      crackPaths,
      flash,
      ring1,
      ring2,
      dust,
      burst,
      ghosts,
      hammer,
      rune,
      sparks,
    }
  }

  function buildPump(svg: Element): Pump {
    const colors = grit.modeSwitchColors()
    const defs = node('defs', {}, svg).el
    gradients(defs)
    const tint = node('rect', { x: 0, y: 0, width: w, height: h, fill: colors.tint, opacity: 0 }, svg)
    const haloLayer = node('g', {}, svg).el
    const halos = [0, 1, 2].map(() => node('circle', { cx: src.x, cy: src.y, r: 0, fill: 'url(#ms-gHalo)', opacity: 0 }, haloLayer))
    const ringLayer = node('g', { fill: 'none', stroke: colors.pumpRing }, svg).el
    const rings = [0, 1].map(() => node('circle', { cx: src.x, cy: src.y, r: 0, opacity: 0 }, ringLayer))
    const edgeSoft = node(
      'circle',
      {
        cx: src.x,
        cy: src.y,
        r: 0,
        fill: 'none',
        stroke: colors.edgeSoft,
        opacity: 0,
      },
      svg,
    )
    const edge = node(
      'circle',
      {
        cx: src.x,
        cy: src.y,
        r: 0,
        fill: 'none',
        stroke: colors.edge,
        opacity: 0,
      },
      svg,
    )
    const veinsLayer = node(
      'g',
      {
        fill: 'none',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        opacity: 0,
      },
      svg,
    )
    const layers: Record<string, Element> = {}
    const glowLayer = node('g', {}, veinsLayer.el)
    layers.glow = glowLayer.el
    if (light)
      layers.edge = node(
        'g',
        {
          stroke: LIGHT_EDGES.veinEdge.color,
          opacity: LIGHT_EDGES.veinEdge.opacity,
        },
        veinsLayer.el,
      ).el
    layers.core = node('g', {}, veinsLayer.el).el
    layers.hot = node('g', {}, veinsLayer.el).el
    const nodeLayer = node('g', { fill: colors.node, stroke: 'none' }, veinsLayer.el).el
    layers.pulse = node('g', {}, veinsLayer.el).el
    const veins: Pump['veins'] = []
    const pulses: Pump['pulses'] = []
    for (const v of grit.modeSwitchVeins(w, h, src.x, src.y)) {
      if (v.layer === 'pulse') {
        pulses.push({
          n: node(
            'path',
            {
              d: v.d,
              stroke: v.color,
              'stroke-width': v.width,
              pathLength: 1,
              'stroke-dasharray': '.06 3',
              opacity: 0,
            },
            layers.pulse,
          ),
          len: v.len,
        })
        continue
      }
      if (v.layer === 'core' && layers.edge != null) {
        const width = v.width + LIGHT_EDGES.veinEdge.extraWidth
        veins.push({
          n: node(
            'path',
            {
              class: 'ms-vein',
              d: v.d,
              'stroke-width': width,
              pathLength: 1,
              'stroke-dasharray': 1,
              'stroke-dashoffset': 1,
              opacity: 0,
            },
            layers.edge,
          ),
          d0: v.d0,
          len: v.len,
          width,
          swell: v.swell,
        })
      }
      const parent = layers[v.layer] ?? layers.core!
      veins.push({
        n: node(
          'path',
          {
            class: 'ms-vein',
            d: v.d,
            stroke: v.color,
            'stroke-width': v.width,
            pathLength: 1,
            'stroke-dasharray': 1,
            'stroke-dashoffset': 1,
            opacity: 0,
          },
          parent,
        ),
        d0: v.d0,
        len: v.len,
        width: v.width,
        swell: v.swell,
      })
    }
    const nodes = grit.modeSwitchVeinNodes(w, h, src.x, src.y).map((n) => ({
      n: node('circle', { cx: n.x, cy: n.y, r: 0, opacity: 0 }, nodeLayer),
      d0: n.d0,
      r: n.r,
    }))
    return {
      tint,
      halos,
      rings,
      edgeSoft,
      edge,
      veinsLayer,
      glowLayer,
      veins,
      nodes,
      pulses,
    }
  }

  /** The content's transform: on the shell, and on the old side's picture while the reveal runs. */
  function moveContent(transform: string) {
    if (shell == null) return
    if (capturing) transform = ''
    shell.style.transform = transform
    if (transition != null) root.style.setProperty('--ms-old-transform', transform || 'none')
  }

  function paintDrop(t: number, d: Drop) {
    const f = grit.modeSwitchDropFrame(t, w, h)
    put(d.shadow, 'cx', f.shadowCx)
    put(d.shadow, 'cy', f.shadowCy)
    put(d.shadow, 'rx', f.shadowRx)
    put(d.shadow, 'ry', f.shadowRy)
    put(d.shadow, 'opacity', f.shadowOpacity)
    const pose = f.hammer
    if (pose != null) {
      put(d.hammer, 'opacity', 1)
      put(d.hammer, 'transform', poseTransform(pose))
    } else put(d.hammer, 'opacity', 0)
    f.ghosts.forEach((g, i) => {
      const n = d.ghosts[i]
      if (n == null) return
      put(n, 'opacity', g.opacity)
      if (g.opacity > 0) put(n, 'transform', poseTransform(g.pose))
    })
    put(d.rune, 'opacity', f.runeOpacity)
    put(d.flash, 'opacity', light ? f.flashOpacity / 2 : f.flashOpacity)
    put(d.burst, 'opacity', f.burstOpacity)
    put(d.burst, 'transform', `translate(${f.hitX} ${f.hitY}) scale(${f.burstScale.toFixed(3)})`)
    for (const [n, c] of [
      [d.ring1, f.ring1],
      [d.ring2, f.ring2],
    ] as const) {
      put(n, 'opacity', c.opacity)
      put(n, 'r', c.radius)
      put(n, 'stroke-width', c.strokeWidth)
    }
    put(d.cracks, 'opacity', f.cracksOpacity)
    const offset = 1 - f.crackDrawn
    for (const c of d.crackPaths) put(c, 'stroke-dashoffset', offset)
    f.dust.forEach((s, i) => {
      const n = d.dust[i]
      if (n == null) return
      put(n, 'opacity', s.opacity)
      if (s.opacity <= 0) return
      put(n, 'cx', s.cx)
      put(n, 'cy', s.cy)
      put(n, 'r', s.r)
    })
    f.sparks.forEach((s, i) => {
      const n = d.sparks[i]
      if (n == null) return
      put(n, 'opacity', s.opacity)
      if (s.opacity <= 0) return
      put(n, 'x1', s.x1)
      put(n, 'y1', s.y1)
      put(n, 'x2', s.x2)
      put(n, 'y2', s.y2)
    })
    // The content shakes inside the screen; the prototype's phone drops too, carrying the effects with it.
    const still = f.shakeX === 0 && f.shakeY === 0 && f.shakeRotation === 0 && f.shakeScale === 1 && f.deviceShakeY === 0
    moveContent(still ? '' : `translate(${f.shakeX.toFixed(2)}px, ${(f.shakeY + f.deviceShakeY).toFixed(2)}px) rotate(${f.shakeRotation.toFixed(3)}deg) scale(${f.shakeScale.toFixed(4)})`)
    host.style.transform = f.deviceShakeY === 0 ? '' : `translateY(${f.deviceShakeY.toFixed(2)}px)`
    for (const b of blocks) {
      const y = grit.modeSwitchHop(t, b.cy, f.hitY)
      b.el.style.transform = y === 0 ? '' : `translateY(${y.toFixed(2)}px)`
    }
    if (stamp != null) {
      stamp.style.opacity = f.stampOpacity.toFixed(3)
      stamp.style.transform = `scale(${f.stampScale.toFixed(3)})`
    }
    radius = f.revealRadius
    full = f.revealFull
    moveClip()
  }

  function paintPump(t: number, p: Pump) {
    const f = grit.modeSwitchPumpFrame(t, w, h, src.x, src.y)
    // Most of the swell is the prototype's phone, which carries the effects too; a little is the screen's content.
    const content = f.contentScale * f.deviceScale
    moveContent(content === 1 ? '' : `scale(${content.toFixed(4)})`)
    host.style.transform = f.deviceScale === 1 ? '' : `scale(${f.deviceScale.toFixed(4)})`
    for (const b of blocks) {
      const s = grit.modeSwitchPumpElementScale(t, w, b.cy, src.x, src.y)
      b.el.style.transform = s === 1 ? '' : `scale(${s.toFixed(4)})`
    }
    put(p.tint, 'opacity', f.tintOpacity)
    f.halos.forEach((c, i) => {
      const n = p.halos[i]
      if (n == null) return
      put(n, 'opacity', c.opacity)
      put(n, 'r', c.radius)
    })
    f.rings.forEach((c, i) => {
      const n = p.rings[i]
      if (n == null) return
      put(n, 'opacity', c.opacity)
      put(n, 'r', c.radius)
      put(n, 'stroke-width', c.strokeWidth)
    })
    put(p.edgeSoft, 'opacity', f.edgeSoftOpacity)
    put(p.edgeSoft, 'r', f.floodRadius)
    put(p.edgeSoft, 'stroke-width', f.edgeSoftWidth)
    put(p.edge, 'opacity', f.edgeOpacity)
    put(p.edge, 'r', f.floodRadius)
    put(p.edge, 'stroke-width', f.edgeWidth)
    put(p.veinsLayer, 'opacity', f.veinsOpacity)
    put(p.glowLayer, 'opacity', f.glowLayerOpacity)
    const front = f.veinFront
    if (f.veinsOpacity > 0) {
      for (const v of p.veins) {
        const progress = grit.modeSwitchVeinProgress(front, v.d0, v.len)
        if (progress <= 0) {
          put(v.n, 'opacity', 0)
          continue
        }
        put(v.n, 'opacity', 1)
        put(v.n, 'stroke-dashoffset', (1 - progress).toFixed(4))
        if (v.swell) put(v.n, 'stroke-width', (v.width * f.veinWidthScale).toFixed(2))
      }
      for (const n of p.nodes) {
        const on = grit.modeSwitchNodeShown(front, n.d0)
        put(n.n, 'opacity', on ? f.nodeOpacity : 0)
        if (on) put(n.n, 'r', (n.r * f.nodeScale).toFixed(2))
      }
      for (const q of p.pulses) {
        const o = grit.modeSwitchPulseOpacity(f.pulseOpacity, front, q.len)
        put(q.n, 'opacity', o)
        if (o > 0) put(q.n, 'stroke-dashoffset', (-grit.modeSwitchPulseStart(front, q.len)).toFixed(4))
      }
    }
    if (pill != null) pill.style.transform = f.pillScale === 1 ? '' : `scale(${f.pillScale.toFixed(3)})`
    radius = f.floodRadius
    full = f.floodFull
    moveClip()
  }

  function moveClip() {
    if (clip == null) return
    if (full) {
      clip.finish()
      clip = null
    } else clip.currentTime = Math.min(REVEAL_MAX - 1, radius)
  }

  /** Straight after the new side is put on: what hops or swells, and the label, are the new side's. */
  function afterSwap() {
    blocks = measureBlocks(shell)
    if (kind === 'drop' && shell != null) {
      stamp = document.createElement('span')
      stamp.className = 'gr-mode-label-pill ms-stamp'
      stamp.setAttribute('aria-hidden', 'true')
      stamp.textContent = 'POWERLIFTING'
      stamp.style.left = `${labelBox?.left ?? 20}px`
      stamp.style.top = `${labelBox?.top ?? 20}px`
      stamp.style.opacity = '0'
      shell.appendChild(stamp)
    }
    if (kind === 'pump') pill = shell?.querySelector<HTMLElement>('.gr-mode-label-pill') ?? null
  }

  function endReveal() {
    const running = transition
    transition = null
    clip = null
    capturing = false
    running?.skipTransition()
    root.classList.remove('ms-revealing')
    root.style.removeProperty('--ms-old-transform')
    root.style.removeProperty('--ms-x')
    root.style.removeProperty('--ms-y')
  }

  return {
    begin(next, source) {
      this.end()
      kind = next
      ;({ w, h } = screenSize())
      light = currentScheme() === 'light'
      shell = document.querySelector<HTMLElement>('.shell')
      if (shell != null) shell.style.transform = ''
      blocks = measureBlocks(shell)
      const svg = node('svg', { viewBox: `0 0 ${w} ${h}`, width: w, height: h, focusable: 'false' }, host).el
      if (next === 'drop') {
        const label = shell?.querySelector('.gr-mode-label-pill') ?? shell?.querySelector('.gr-screen-header-text')
        const box = label?.getBoundingClientRect()
        labelBox = box != null ? { left: box.left, top: box.top } : null
        drop = buildDrop(svg)
      } else {
        // From the switch's centre, wherever Settings is scrolled; from the bottom centre if it cannot be found.
        src = switchCentre(source) ?? grit.modeSwitchHitPoint(w, h)
        root.classList.add('ms-pumping')
        pump = buildPump(svg)
      }
    },
    paint(t) {
      if (drop != null) paintDrop(t, drop)
      else if (pump != null) paintPump(t, pump)
    },
    reveal(swap) {
      const start = (
        document as Document & {
          startViewTransition?: (update: () => void) => ViewTransition
        }
      ).startViewTransition
      const center = kind === 'drop' ? grit.modeSwitchHitPoint(w, h) : src
      const update = () => {
        if (swap()) afterSwap()
      }
      if (typeof start !== 'function') return update()
      root.style.setProperty('--ms-x', `${center.x}px`)
      root.style.setProperty('--ms-y', `${center.y}px`)
      root.classList.add('ms-revealing')
      let mine: ViewTransition
      try {
        // The content's picture is taken still: its shake or swell starts again once the reveal is under way.
        capturing = true
        if (shell != null) shell.style.transform = ''
        mine = start.call(document, update)
      } catch {
        capturing = false
        root.classList.remove('ms-revealing')
        return update()
      }
      transition = mine
      mine.ready
        .then(() => {
          if (transition !== mine) return
          capturing = false
          if (full) return
          const at = `at ${center.x}px ${center.y}px`
          clip = root.animate(
            {
              clipPath: [`circle(0px ${at})`, `circle(${REVEAL_MAX}px ${at})`],
            },
            {
              duration: REVEAL_MAX,
              easing: 'linear',
              fill: 'forwards',
              pseudoElement: '::view-transition-new(root)',
            },
          )
          clip.pause()
          moveClip()
        })
        .catch(() => {})
      mine.updateCallbackDone.catch(() => {})
      mine.finished
        .catch(() => {})
        .finally(() => {
          if (transition === mine) endReveal()
        })
    },
    end() {
      endReveal()
      host.replaceChildren()
      host.style.transform = ''
      if (shell != null) shell.style.transform = ''
      for (const b of blocks) b.el.style.transform = ''
      if (pill != null) pill.style.transform = ''
      stamp?.remove()
      root.classList.remove('ms-pumping')
      blocks = []
      drop = null
      pump = null
      stamp = null
      pill = null
      labelBox = null
      kind = null
    },
  }
}

function poseTransform(p: { x: number; y: number; rotation: number; scaleX: number; scaleY: number }): string {
  return `translate(${p.x.toFixed(2)} ${p.y.toFixed(2)}) rotate(${p.rotation.toFixed(2)}) scale(${p.scaleX.toFixed(3)} ${p.scaleY.toFixed(3)})`
}
