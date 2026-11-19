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

/** The gap between the title's bottom and the stamp, px (Android: Space.xxs). */
const STAMP_GAP = 2

/** The reveal's clip runs linearly from 0 to this radius over as many ms, so its time is its radius in px. */
const REVEAL_MAX = 100_000

/*
 * Light scheme (spec "Settled" 1): the effects keep the prototype's colours, except the shared model's light set
 * (`grit.modeSwitchLightColors()`, the one Android draws too): the flash is a dark punch at half the opacity; the
 * cracks' under-strokes are stronger; the chalk dust has a grey rim; the core veins have a navy edge (one layer under
 * them); ring 1, the sparks and the starburst's edge are darker, so they read once the punch has gone.
 */

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
  /** `width` is the core's; an `edge` vein is the light scheme's navy edge under it, `modeSwitchVeinEdgeWidth` wide. */
  veins: { n: Node; d0: number; len: number; width: number; swell: boolean; edge: boolean }[]
  nodes: { n: Node; d0: number; r: number }[]
  pulses: { n: Node; len: number }[]
}

function createPainter(host: HTMLDivElement): ModeSwitchPainter {
  const root = document.documentElement
  let kind: ModeSwitchKind | null = null
  let w = 0
  let h = 0
  let light = false
  let flashScale = 0.5
  let shell: HTMLElement | null = null
  let blocks: Measured[] = []
  let drop: Drop | null = null
  let pump: Pump | null = null
  let src = { x: 0, y: 0 }
  let stamp: HTMLElement | null = null
  let pill: HTMLElement | null = null
  /** The new side is on, and its blocks are still to be measured (at the next frame's start). */
  let settling = false
  // The reveal.
  let transition: ViewTransition | null = null
  let clip: Animation | null = null
  /**
   * The old side's picture moves with the content: a held animation on its own pseudo-element, whose one value is
   * changed each frame, so only the picture is restyled (a custom property on <html> restyled the whole page).
   */
  let oldMove: Animation | null = null
  let oldTransform = 'none'
  /** The content's transform the last frame drew, held off while the pictures are taken. */
  let wanted = ''
  let capturing = false
  let radius = 0
  let full = false
  /** Without view transitions: the new side's circle is a clip on the shell, round this centre. */
  let plainAt: { x: number; y: number } | null = null

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

  function buildDrop(svg: Element): Drop {
    const colors = grit.modeSwitchColors()
    const lightSet = grit.modeSwitchLightColors()
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

  function buildPump(svg: Element): Pump {
    const colors = grit.modeSwitchColors()
    const lightSet = grit.modeSwitchLightColors()
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

  /**
   * The content's transform: on the shell, and on the old side's picture while the reveal runs. While the pictures are
   * taken it is held off, and put on again as soon as they are (the reveal's `ready`), whichever frame comes first.
   */
  function moveContent(transform: string) {
    if (shell == null) return
    wanted = transform
    if (capturing) transform = ''
    shell.style.transform = transform
    if (transition == null) return
    const next = transform || 'none'
    if (next === oldTransform) return
    oldTransform = next
    ;(oldMove?.effect as KeyframeEffect | null | undefined)?.setKeyframes([{ transform: next }, { transform: next }])
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
    if (pill != null) pill.style.transform = f.pillScale === 1 ? '' : `scale(${f.pillScale.toFixed(3)})`
    radius = f.floodRadius
    full = f.floodFull
    moveClip()
  }

  function moveClip() {
    if (plainAt != null && shell != null) {
      if (full) endReveal()
      else shell.style.clipPath = `circle(${Math.max(0, radius).toFixed(1)}px at ${plainAt.x}px ${plainAt.y}px)`
      return
    }
    if (clip == null) return
    if (full) {
      // The new side covers the screen: the old side's animation is finished too, so the transition can end. It is
      // kept (still following the content, as its fill) until the reveal ends, which cancels it: a finished animation
      // left behind would hold the next reveal's old side.
      clip.finish()
      clip = null
      oldMove?.finish()
    } else clip.currentTime = Math.min(REVEAL_MAX - 1, radius)
  }

  /**
   * The POWERLIFTING stamp, just below the screen's title so it never covers it: inside the header (which hops as one
   * block), taking no room of its own. Put in unseen with the new side; placed by `placeStamp`.
   */
  function addStamp() {
    const header = shell?.querySelector<HTMLElement>('.shell-main > .screen > .gr-screen-header') ?? null
    if (header?.querySelector('.gr-screen-title') == null) return
    stamp = document.createElement('span')
    stamp.className = 'gr-mode-label-pill ms-stamp'
    stamp.setAttribute('aria-hidden', 'true')
    stamp.textContent = 'POWERLIFTING'
    stamp.style.opacity = '0'
    header.classList.add('ms-stamp-host')
    header.appendChild(stamp)
  }

  /** Places the stamp from the new side's layout, with every transform taken off. */
  function placeStamp() {
    const header = stamp?.parentElement ?? null
    const title = header?.querySelector<HTMLElement>('.gr-screen-title') ?? null
    if (shell == null || stamp == null || header == null || title == null) return
    const shellTransform = shell.style.transform
    const headerTransform = header.style.transform
    shell.style.transform = ''
    header.style.transform = ''
    const h = header.getBoundingClientRect()
    const t = title.getBoundingClientRect()
    shell.style.transform = shellTransform
    header.style.transform = headerTransform
    stamp.style.left = `${t.left - h.left}px`
    stamp.style.top = `${t.bottom - h.top + STAMP_GAP}px`
  }

  /**
   * Straight after the new side is put on, inside the reveal's update: what hops or swells, and the label, are the new
   * side's, and the stamp goes in, unseen. Nothing is measured here: a measure would lay the new side out inside the
   * update, and the browser lays it out again for the reveal straight after. The blocks are measured, and the stamp
   * placed, at the next frame's start (`settle`), from the layout the reveal has just made. Until then nothing hops or
   * swells: the blocks kept from the old side are let go, as a measure would.
   */
  function afterSwap() {
    for (const b of blocks) b.el.style.transform = ''
    blocks = []
    settling = true
    if (kind === 'drop') addStamp()
    if (kind === 'pump') pill = shell?.querySelector<HTMLElement>('.gr-mode-label-pill') ?? null
  }

  /** Measured as a measure in the update would have been: with the content's own transform off. */
  function settle() {
    settling = false
    const moved = shell?.style.transform ?? ''
    if (shell != null) shell.style.transform = ''
    blocks = measureBlocks(shell)
    placeStamp()
    if (shell != null) shell.style.transform = moved
  }

  function endReveal() {
    const running = transition
    transition = null
    clip = null
    oldMove?.cancel()
    oldMove = null
    oldTransform = 'none'
    capturing = false
    running?.skipTransition()
    if (plainAt != null) {
      plainAt = null
      if (shell != null) shell.style.clipPath = ''
      root.classList.remove('ms-revealing-plain')
      root.style.removeProperty('--ms-old-bg')
    }
    root.classList.remove('ms-revealing')
    root.style.removeProperty('--ms-x')
    root.style.removeProperty('--ms-y')
  }

  /**
   * Browsers without view transitions: the new side is put on and let in through a circle that grows from the hit or
   * the switch, as the reveal does, instead of all at once. Outside the circle there is no picture of the old side
   * here, so the page shows the old side's background colour until the circle has covered it.
   */
  function plainReveal(center: { x: number; y: number }, update: () => void) {
    const oldBackground = getComputedStyle(document.body).backgroundColor
    update()
    if (shell == null || kind == null || full) return
    plainAt = { x: center.x, y: center.y }
    root.style.setProperty('--ms-old-bg', oldBackground)
    root.classList.add('ms-revealing-plain')
    moveClip()
  }

  return {
    begin(next, source) {
      this.end()
      kind = next
      ;({ w, h } = screenSize())
      light = currentScheme() === 'light'
      flashScale = grit.modeSwitchLightColors().flashOpacityScale
      shell = document.querySelector<HTMLElement>('.shell')
      if (shell != null) shell.style.transform = ''
      blocks = measureBlocks(shell)
      const svg = node('svg', { viewBox: `0 0 ${w} ${h}`, width: w, height: h, focusable: 'false' }, host).el
      if (next === 'drop') {
        drop = buildDrop(svg)
      } else {
        // From the switch's centre, wherever Settings is scrolled; from the bottom centre if it cannot be found.
        src = switchCentre(source) ?? grit.modeSwitchHitPoint(w, h)
        root.classList.add('ms-pumping')
        pump = buildPump(svg)
      }
    },
    paint(t) {
      if (settling) settle()
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
      if (typeof start !== 'function') return plainReveal(center, update)
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
          moveContent(wanted)
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
          oldMove = root.animate([{ transform: oldTransform }, { transform: oldTransform }], {
            duration: 1,
            fill: 'both',
            pseudoElement: '::view-transition-old(root)',
          })
          oldMove.pause()
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
      stamp?.parentElement?.classList.remove('ms-stamp-host')
      stamp?.remove()
      root.classList.remove('ms-pumping')
      blocks = []
      settling = false
      wanted = ''
      drop = null
      pump = null
      stamp = null
      pill = null
      kind = null
    },
  }
}

function poseTransform(p: { x: number; y: number; rotation: number; scaleX: number; scaleY: number }): string {
  return `translate(${p.x.toFixed(2)} ${p.y.toFixed(2)}) rotate(${p.rotation.toFixed(2)}) scale(${p.scaleX.toFixed(3)} ${p.scaleY.toFixed(3)})`
}
