import { memo, useLayoutEffect, useRef, useState } from 'react'
import { grit } from '../../domain'
import { FigureSvg, ShapesSvg, type Pose } from './FigureSvg'
import { hasThumbnail, thumbnailOf } from './thumbnails'
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

/** The margin that makes a watcher see a row `NEAR_PX` before it scrolls in. */
const NEAR_MARGIN = `${NEAR_PX}px 0px`

/**
 * One watcher for each scrolling list, for its tiles still waiting off screen, each by the row it is placed in. It
 * watches against the list itself (or the screen, for a tile in no list): a browser only looks ahead by the margin
 * past the box being watched against, and a list that scrolls inside the page hides its rows before the screen does.
 */
const watchers = new WeakMap<Element | Document, IntersectionObserver>()
const waiting = new WeakMap<Element, () => void>()

function watch(placed: Element, scroller: Element | null, onNear: () => void): () => void {
  const key = scroller ?? document
  let watcher = watchers.get(key)
  if (watcher == null) {
    const made = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue
          made.unobserve(entry.target)
          waiting.get(entry.target)?.()
          waiting.delete(entry.target)
        }
      },
      { root: scroller, rootMargin: NEAR_MARGIN },
    )
    watchers.set(key, made)
    watcher = made
  }
  const watching = watcher
  waiting.set(placed, onNear)
  watching.observe(placed)
  return () => {
    watching.unobserve(placed)
    waiting.delete(placed)
  }
}

/**
 * A row's small picture: the more telling pose of the exercise `name` on a muted tile. The tile is empty with no
 * `name` (the lifter's own exercise) or no figure for it, so names stay aligned down the list. Decorative: the row
 * beside it names the exercise, so it is hidden from screen readers.
 *
 * Its shapes were worked out when the app was built (thumbnails.ts), so it never asks the domain to solve a pose,
 * and whether it has a figure is a look-up in that data, keyed by catalogue name as the rows give it. The drawing is
 * still only made for a tile on or near the screen, and then kept: a list of a few hundred rows opens without putting
 * a few hundred drawings, several thousand SVG elements, on the page.
 */
export const ExerciseThumbnail = memo(function ExerciseThumbnail({ name }: { name?: string | null }) {
  const drawable = name != null && hasThumbnail(name)
  const tile = useRef<HTMLSpanElement>(null)
  const [near, setNear] = useState(false)
  const waits = drawable && !near
  // Before the first paint, so a tile that starts on screen is never seen empty.
  useLayoutEffect(() => {
    if (!waits || tile.current == null) return
    // The row is what is placed: its box is known even while the browser skips laying out what is inside it
    // (`content-visibility` in library.css), and asking for the tile's own box would make it lay every row out.
    const placed = tile.current.closest('.lib-row') ?? tile.current
    // The list the row scrolls in. Near is measured from its edges, or from the screen's for a tile in no list.
    const scroller = placed.closest('.lib-scroll')
    const box = placed.getBoundingClientRect()
    const edges = scroller?.getBoundingClientRect() ?? { top: 0, bottom: window.innerHeight }
    if (typeof IntersectionObserver === 'undefined' || (box.bottom > edges.top - NEAR_PX && box.top < edges.bottom + NEAR_PX)) {
      setNear(true)
      return
    }
    return watch(placed, scroller, () => setNear(true))
  }, [waits])
  return (
    <span ref={tile} className="lib-thumb" aria-hidden="true">
      {drawable && near && <ThumbnailFigure name={name!} />}
    </span>
  )
})

/** The thumbnail's pose of `name`'s figure, as worked out when the app was built. */
function ThumbnailFigure({ name }: { name: string }) {
  const thumbnail = thumbnailOf(name)
  if (thumbnail == null) return null
  return <ShapesSvg shapes={thumbnail.shapes} viewBox={thumbnail.viewBox} />
}
