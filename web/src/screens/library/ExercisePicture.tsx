import { memo, useLayoutEffect, useRef, useState } from 'react'
import { grit } from '../../domain'
import { FigureSvg, type Pose } from './FigureSvg'
import './library.css'

type FigureInfo = NonNullable<ReturnType<typeof grit.figureInfo>>

/** What a screen reader says for a figure's picture. */
export const pictureLabel = (figure: Pick<FigureInfo, 'startCaption' | 'endCaption'>): string => `Picture: ${figure.startCaption}, then ${figure.endCaption}.`

/**
 * The picture of the exercise `name`: its start and end pose side by side, sharing the width equally, each with its
 * caption underneath. One image to a screen reader, named by both captions. Nothing at all, taking no room, when the
 * catalogue has no figure for the name: the lifter's own exercise, or a catalogue exercise not drawn yet.
 */
export function ExercisePicture({ name }: { name: string }) {
  const figure = grit.figureInfo(name)
  if (figure == null) return null
  const panels: [Pose, string][] = [
    ['start', figure.startCaption],
    ['end', figure.endCaption],
  ]
  return (
    <figure className="lib-picture" role="img" aria-label={pictureLabel(figure)}>
      {panels.map(([pose, caption]) => (
        <div key={pose} className="lib-picture-panel">
          <FigureSvg name={name} pose={pose} viewBox={figure.viewBox} />
          {/* Read as part of the picture's name, not a second time. It wraps onto as many lines as it needs. */}
          <span className="lib-picture-caption body-small" aria-hidden="true">
            {caption}
          </span>
        </div>
      ))}
    </figure>
  )
}

/** How far outside the screen a row's thumbnail is already drawn, so it is there before the row scrolls in. */
const NEAR_PX = 600

/** One watcher for every tile still waiting off screen, each by the row it is placed in. */
let watcher: IntersectionObserver | null = null
const waiting = new WeakMap<Element, () => void>()

function watch(placed: Element, onNear: () => void): () => void {
  watcher ??= new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        watcher!.unobserve(entry.target)
        waiting.get(entry.target)?.()
        waiting.delete(entry.target)
      }
    },
    { rootMargin: `${NEAR_PX}px 0px` },
  )
  waiting.set(placed, onNear)
  watcher.observe(placed)
  return () => {
    watcher!.unobserve(placed)
    waiting.delete(placed)
  }
}

/**
 * A row's small picture: the more telling pose of the exercise `name` on a muted tile. The tile is empty with no
 * `name` (the lifter's own exercise) or no figure for it, so names stay aligned down the list. Decorative: the row
 * beside it names the exercise, so it is hidden from screen readers.
 *
 * The drawing is only made for a tile on or near the screen, and then kept: a list of a few hundred rows opens
 * without building a few hundred drawings.
 */
export const ExerciseThumbnail = memo(function ExerciseThumbnail({ name }: { name?: string | null }) {
  const figure = name != null ? grit.figureInfo(name) : null
  const tile = useRef<HTMLSpanElement>(null)
  const [near, setNear] = useState(false)
  const waits = figure != null && !near
  // Before the first paint, so a tile that starts on screen is never seen empty.
  useLayoutEffect(() => {
    if (!waits || tile.current == null) return
    // The row is what is placed: its box is known even while the browser skips laying out what is inside it
    // (`content-visibility` in library.css), and asking for the tile's own box would make it lay every row out.
    const placed = tile.current.closest('.lib-row') ?? tile.current
    const box = placed.getBoundingClientRect()
    if (typeof IntersectionObserver === 'undefined' || (box.bottom > -NEAR_PX && box.top < window.innerHeight + NEAR_PX)) {
      setNear(true)
      return
    }
    return watch(placed, () => setNear(true))
  }, [waits])
  return (
    <span ref={tile} className="lib-thumb" aria-hidden="true">
      {figure != null && near && <FigureSvg name={name!} pose={figure.thumbnail === 'start' ? 'start' : 'end'} viewBox={figure.viewBox} />}
    </span>
  )
})
