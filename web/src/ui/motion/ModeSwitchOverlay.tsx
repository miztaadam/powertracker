import { useEffect, useRef } from 'react'
import { grit } from '../../domain'
import { currentScheme } from '../theme'
import { screenSize, switchCentre, useModeSwitch, type ModeSwitchKind, type ModeSwitchPainter } from './useModeSwitch'
import { NS, buildDrop, buildPump, node, paintDropArt, paintPumpArt, type Drop, type Pump } from './modeSwitchPaint'
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
  /** The new side's circle, while it still grows. */
  let clip: Animation | null = null
  /** The same animation, kept once finished too, so the reveal's end cancels it (a finished one would stay on <html>). */
  let clipHeld: Animation | null = null
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
    paintDropArt(d, f, light, flashScale)
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
    paintPumpArt(p, f)
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
    clipHeld?.cancel()
    clipHeld = null
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
   * An animation on one of the reveal's pseudo-elements, or null where the browser cannot hold one there: if `animate`
   * throws for it, or ignores the pseudo-element and would animate <html> itself (then it is cancelled at once, before
   * a frame is drawn).
   */
  function onPseudo(keyframes: Keyframe[] | PropertyIndexedKeyframes, options: KeyframeAnimationOptions & { pseudoElement: string }): Animation | null {
    let animation: Animation
    try {
      animation = root.animate(keyframes, options)
    } catch {
      return null
    }
    if ((animation.effect as KeyframeEffect | null)?.pseudoElement === options.pseudoElement) return animation
    animation.cancel()
    return null
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
        drop = buildDrop(svg, w, h, light)
      } else {
        // From the switch's centre, wherever Settings is scrolled; from the bottom centre if it cannot be found.
        src = switchCentre(source) ?? grit.modeSwitchHitPoint(w, h)
        root.classList.add('ms-pumping')
        pump = buildPump(svg, w, h, light, src)
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
          clip = onPseudo(
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
          // No circle: the new side is put on at once, as a switch with no reveal (the animation's effects play on).
          if (clip == null) return endReveal()
          clipHeld = clip
          clip.pause()
          // No moving picture of the old side: the reveal runs with the old side still.
          oldMove = onPseudo([{ transform: oldTransform }, { transform: oldTransform }], {
            duration: 1,
            fill: 'both',
            pseudoElement: '::view-transition-old(root)',
          })
          oldMove?.pause()
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
