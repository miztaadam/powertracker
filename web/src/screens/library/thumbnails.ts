import data from 'virtual:figure-thumbnails'
import type { Shape } from './FigureSvg'
import { decodeShapes } from './thumbnailFormat'

/** A thumbnail's pose, ready to draw: its view box and its shapes in the renderer's back-to-front order. */
export type Thumbnail = { viewBox: readonly [number, number]; shapes: readonly Shape[] }

/** Each read once, on first use: most of the list is never scrolled to. */
const read = new Map<string, Thumbnail>()

/**
 * The thumbnail pose of the catalogue exercise `name` (its catalogue name, as the Exercises tab's rows give it),
 * worked out when the app was built (plugins/figureThumbnails.ts); null when it has no figure.
 */
export function thumbnailOf(name: string): Thumbnail | null {
  let found = read.get(name)
  if (found == null) {
    if (!Object.hasOwn(data, name)) return null
    const written = data[name]!
    found = { viewBox: [written[0]!, written[1]!], shapes: decodeShapes(written, 2) }
    read.set(name, found)
  }
  return found
}

/** Whether the catalogue exercise `name` has a thumbnail: every catalogue exercise with a figure has one. */
export const hasThumbnail = (name: string): boolean => Object.hasOwn(data, name)
