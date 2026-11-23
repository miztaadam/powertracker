import type { ReactElement } from 'react'
import { grit } from '../../domain'
import './figure.css'

/** One of a figure's two poses. */
export type Pose = 'start' | 'end'

// What the domain's shapes carry, by `kind`. The generated classes type only `kind` and `role` on their base.
type Circle = { kind: 'circle'; role: string; cx: number; cy: number; r: number }
type Capsule = { kind: 'capsule'; role: string; x1: number; y1: number; x2: number; y2: number; width: number }
type Rect = { kind: 'rect'; role: string; x: number; y: number; w: number; h: number; radius: number }
type Polygon = { kind: 'polygon'; role: string; points: readonly number[] }
export type Shape = Circle | Capsule | Rect | Polygon

/** The class that colours a role (figure.css): `BODY_FAR` is `fig-body-far`. */
const roleClass = (role: string) => `fig-${role.toLowerCase().replace(/_/g, '-')}`

function element(shape: Shape, key: number): ReactElement {
  const className = roleClass(shape.role)
  switch (shape.kind) {
    case 'circle':
      return <circle key={key} className={className} cx={shape.cx} cy={shape.cy} r={shape.r} fill="currentColor" />
    case 'capsule':
      // A round-capped line of the capsule's width.
      return <line key={key} className={className} x1={shape.x1} y1={shape.y1} x2={shape.x2} y2={shape.y2} stroke="currentColor" strokeWidth={shape.width} strokeLinecap="round" />
    case 'rect':
      return <rect key={key} className={className} x={shape.x} y={shape.y} width={shape.w} height={shape.h} rx={shape.radius} fill="currentColor" />
    case 'polygon': {
      const points: string[] = []
      for (let i = 0; i + 1 < shape.points.length; i += 2) points.push(`${shape.points[i]},${shape.points[i + 1]}`)
      // Non-zero: a plate ring is one polygon, its outer edge wound one way and its inner edge the other.
      return <polygon key={key} className={className} points={points.join(' ')} fill="currentColor" fillRule="nonzero" />
    }
  }
}

/** A pose's drawing, by name and pose: built once, however many rows and sheets show it. */
const drawings = new Map<string, readonly ReactElement[] | null>()

/** The shapes of `name`'s `pose` as SVG elements, in the renderer's back-to-front order; null when it has no figure. */
function drawing(name: string, pose: Pose): readonly ReactElement[] | null {
  const key = `${pose}\n${name}`
  let found = drawings.get(key)
  if (found === undefined) {
    found = (grit.figureShapes(name, pose) as unknown as Shape[] | null | undefined)?.map(element) ?? null
    drawings.set(key, found)
  }
  return found
}

export type FigureSvgProps = {
  /** The exercise, by any name the catalogue knows it under. */
  name: string
  pose: Pose
  /** The figure's view box, [width, height], from `grit.figureInfo`. */
  viewBox: readonly number[]
}

/**
 * One pose of an exercise's figure as an inline SVG, as large as its box lets it be. Its colours are the roles'
 * tokens (figure.css), so it follows the theme. It says nothing to a screen reader: whatever shows it names it, or
 * leaves it as decoration. Draws nothing for a name with no figure.
 */
export function FigureSvg({ name, pose, viewBox }: FigureSvgProps) {
  const shapes = drawing(name, pose)
  if (shapes == null) return null
  return <Svg viewBox={viewBox}>{shapes}</Svg>
}

function Svg({ viewBox, children }: { viewBox: readonly number[]; children: readonly ReactElement[] }) {
  return (
    <svg className="fig" viewBox={`0 0 ${viewBox[0]} ${viewBox[1]}`} aria-hidden="true" focusable="false">
      {children}
    </svg>
  )
}

/** Shapes' drawings, by the list they were made from: built once, however many rows show it. */
const drawn = new WeakMap<readonly Shape[], readonly ReactElement[]>()

/**
 * Shapes already worked out (the Exercises tab's thumbnails, worked out when the app is built) drawn as `FigureSvg`
 * draws a pose: the same elements, colours and order, with nothing asked of the domain.
 */
export function ShapesSvg({ shapes, viewBox }: { shapes: readonly Shape[]; viewBox: readonly number[] }) {
  let elements = drawn.get(shapes)
  if (elements == null) {
    elements = shapes.map(element)
    drawn.set(shapes, elements)
  }
  return <Svg viewBox={viewBox}>{elements}</Svg>
}
