import { grit } from '../../domain'

/*
 * The mode switch's art (1.15): the hammer and the pump as inline SVG, built from the shared motion model and painted
 * one frame at a time. Moved out of ModeSwitchOverlay.tsx unchanged (1.16), so the guide's screen 2 draws them too; the
 * overlay keeps what moves the page (the shake, the hops, the stamp, the reveal).
 */

export const NS = 'http://www.w3.org/2000/svg'

/** One frame of the drop, and of the pump, as the shared model gives them. */
export type DropFrame = ReturnType<typeof grit.modeSwitchDropFrame>
export type PumpFrame = ReturnType<typeof grit.modeSwitchPumpFrame>

/** An SVG element whose attributes are written only when they change. */
export type Node = { el: Element; last: Record<string, string> }

export function node(tag: string, attrs: Record<string, string | number>, parent: Element): Node {
  const el = document.createElementNS(NS, tag)
  for (const [k, v] of Object.entries(attrs)) el.setAttribute(k, String(v))
  parent.appendChild(el)
  return { el, last: {} }
}

export function put(n: Node, name: string, value: string | number) {
  const text = typeof value === 'number' ? (Number.isInteger(value) ? String(value) : value.toFixed(3)) : value
  if (n.last[name] === text) return
  n.last[name] = text
  n.el.setAttribute(name, text)
}

/** Paths from the model name gradients by their own ids; on the page they carry the overlay's prefix. */
const own = (paint: string | null | undefined) => (paint == null ? 'none' : paint.replace('url(#', 'url(#ms-'))

export type Drop = {
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

export type Pump = {
  tint: Node
  halos: Node[]
  rings: Node[]
  edgeSoft: Node
  edge: Node
  veinsLayer: Node
  glowLayer: Node
  /** `width` is the core's; an `edge` vein is the light scheme's navy edge under it, `modeSwitchVeinEdgeWidth` wide. */
  veins: { n: Node; d0: number; len: number; width: number; swell: boolean; edge: boolean }[]
  nodes: { n: Node; d0: number; r: number }[]
  pulses: { n: Node; len: number }[]
}

function gradients(defs: Element, light: boolean) {
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
    for (const s of grit.modeSwitchLightColors().chalk)
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

export function buildDrop(svg: Element, w: number, h: number, light: boolean): Drop {
  const colors = grit.modeSwitchColors()
  const lightSet = grit.modeSwitchLightColors()
  const hit = grit.modeSwitchHitPoint(w, h)
  const defs = node('defs', {}, svg).el
  gradients(defs, light)
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
        opacity: shade ? lightSet.crackShadowOpacity : p.opacity,
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
      fill: light ? lightSet.flash : colors.flash,
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
      stroke: light ? lightSet.ring1 : colors.ring1,
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
  const burst = node(
    'polygon',
    light ? { points, fill: colors.burst, stroke: lightSet.burstEdge, 'stroke-width': lightSet.burstEdgeWidth, 'stroke-linejoin': 'round', opacity: 0 } : { points, fill: colors.burst, opacity: 0 },
    svg,
  )
  const ghostLayer = node('g', {}, svg).el
  const ghosts = [0, 1, 2].map(() => node('use', { href: '#ms-hammer', opacity: 0 }, ghostLayer))
  const hammer = node('g', { opacity: 0 }, svg)
  node('use', { href: '#ms-hammer' }, hammer.el)
  const rune = node('g', { opacity: 0 }, hammer.el)
  for (const r of grit.modeSwitchHammerRunePaths()) {
    node(
      'path',
      {
        d: r.d,
        fill: own(r.fill),
        stroke: own(r.stroke),
        'stroke-width': r.strokeWidth,
        opacity: r.opacity,
        'stroke-linecap': r.lineCap,
        'stroke-linejoin': r.lineJoin,
      },
      rune.el,
    )
  }
  const sparkLayer = node(
    'g',
    {
      stroke: light ? lightSet.spark : colors.spark,
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

export function buildPump(svg: Element, w: number, h: number, light: boolean, src: { x: number; y: number }): Pump {
  const colors = grit.modeSwitchColors()
  const lightSet = grit.modeSwitchLightColors()
  const defs = node('defs', {}, svg).el
  gradients(defs, light)
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
        stroke: lightSet.veinEdge,
        opacity: lightSet.veinEdgeOpacity,
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
      const width = grit.modeSwitchVeinEdgeWidth(v.width, 1)
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
        width: v.width,
        swell: v.swell,
        edge: true,
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
      edge: false,
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

/** The drop's art at one frame of `grit.modeSwitchDropFrame` (`flashScale`: the flash's share in the light scheme). */
export function paintDropArt(d: Drop, f: DropFrame, light: boolean, flashScale: number) {
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
  put(d.flash, 'opacity', light ? f.flashOpacity * flashScale : f.flashOpacity)
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
}

/** The pump's art at one frame of `grit.modeSwitchPumpFrame`. */
export function paintPumpArt(p: Pump, f: PumpFrame) {
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
      if (v.edge) put(v.n, 'stroke-width', grit.modeSwitchVeinEdgeWidth(v.width, v.swell ? f.veinWidthScale : 1).toFixed(2))
      else if (v.swell) put(v.n, 'stroke-width', (v.width * f.veinWidthScale).toFixed(2))
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
}

export function poseTransform(p: { x: number; y: number; rotation: number; scaleX: number; scaleY: number }): string {
  return `translate(${p.x.toFixed(2)} ${p.y.toFixed(2)}) rotate(${p.rotation.toFixed(2)}) scale(${p.scaleX.toFixed(3)} ${p.scaleY.toFixed(3)})`
}
