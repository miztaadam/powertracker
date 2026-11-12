import type { Shape } from './FigureSvg'

/**
 * How the Exercises tab's thumbnails are written down when the app is built (plugins/figureThumbnails.ts) and read
 * back in the browser (thumbnails.ts): one flat list of whole numbers per pose, short to ship and quick to read.
 *
 * Each shape is a code, `kind * 6 + role`, then its numbers in tenths of a view-box unit:
 * - circle: cx, cy, r;
 * - capsule: x1, y1, x2, y2, width;
 * - joined capsule: x2, y2, width, starting where the capsule just before it ends (a limb's segments);
 * - rect: x, y, w, h, radius;
 * - polygon: its point count, then x, y for each point;
 * - ring (a plate, drawn as the kit's 36-step ring polygon): cx, cy, outer and inner radius, in hundredths.
 *
 * Every number read back is within `TOLERANCE` of the domain's (half a tenth, give or take a float's last digit): 0.007 px on the 56 x 40 tile, where 400 units are
 * 56 px across, far below anything a screen can show.
 */
export const TOLERANCE = 0.05

/** The roles, by their index in the code. Their order is the format's: never reorder, only add. */
const ROLES = ['BODY', 'BODY_FAR', 'EQUIPMENT', 'ACCENT', 'ACCENT_FILL', 'GAP'] as const

const CIRCLE = 0
const CAPSULE = 1
const JOINED = 2
const RECT = 3
const POLYGON = 4
const RING = 5

/** The kit's ring (Kit.kt `ring`): 36 steps round the outer circle one way, then round the inner the other way. */
const RING_STEPS = 36

function ringPoints(cx: number, cy: number, outer: number, inner: number): number[] {
  const points: number[] = []
  for (let i = 0; i <= RING_STEPS; i++) {
    const a = (2 * Math.PI * i) / RING_STEPS
    points.push(cx + outer * Math.cos(a), cy - outer * Math.sin(a))
  }
  for (let i = RING_STEPS; i >= 0; i--) {
    const a = (2 * Math.PI * i) / RING_STEPS
    points.push(cx + inner * Math.cos(a), cy - inner * Math.sin(a))
  }
  return points
}

/** A polygon's centre and radii in hundredths, when it is the kit's ring and reads back within the tolerance. */
function asRing(points: readonly number[]): number[] | null {
  if (points.length !== 4 * (RING_STEPS + 1)) return null
  // The outer circle starts at angle 0 and is half-way round at point 18; the inner one starts at point 37.
  const half = RING_STEPS / 2
  const cx = (points[0]! + points[2 * half]!) / 2
  const cy = points[1]!
  const outer = (points[0]! - points[2 * half]!) / 2
  const inner = points[2 * (RING_STEPS + 1)]! - cx
  const written = [cx, cy, outer, inner].map((v) => Math.round(v * 100))
  const back = ringPoints(written[0]! / 100, written[1]! / 100, written[2]! / 100, written[3]! / 100)
  return back.every((v, i) => Math.abs(v - points[i]!) <= TOLERANCE) ? written : null
}

const tenths = (v: number) => Math.round(v * 10)

/** `shapes` written down, in their order. */
export function encodeShapes(shapes: readonly Shape[]): number[] {
  const out: number[] = []
  // Where the capsule just written ends, as written, or null after any other shape.
  let end: [number, number] | null = null
  for (const s of shapes) {
    const role = ROLES.indexOf(s.role as (typeof ROLES)[number])
    if (role < 0) throw new Error(`thumbnail data: no code for the role ${s.role}`)
    const code = (kind: number) => kind * ROLES.length + role
    let ends: [number, number] | null = null
    switch (s.kind) {
      case 'circle':
        out.push(code(CIRCLE), tenths(s.cx), tenths(s.cy), tenths(s.r))
        break
      case 'capsule': {
        const [x1, y1, x2, y2] = [s.x1, s.y1, s.x2, s.y2].map(tenths) as [number, number, number, number]
        if (end != null && end[0] === x1 && end[1] === y1) out.push(code(JOINED), x2, y2, tenths(s.width))
        else out.push(code(CAPSULE), x1, y1, x2, y2, tenths(s.width))
        ends = [x2, y2]
        break
      }
      case 'rect':
        out.push(code(RECT), tenths(s.x), tenths(s.y), tenths(s.w), tenths(s.h), tenths(s.radius))
        break
      case 'polygon': {
        const ring = asRing(s.points)
        if (ring != null) out.push(code(RING), ...ring)
        else out.push(code(POLYGON), s.points.length / 2, ...s.points.map(tenths))
        break
      }
    }
    end = ends
  }
  return out
}

/** The shapes written down in `data` from `from` on, in their order. */
export function decodeShapes(data: readonly number[], from = 0): Shape[] {
  const shapes: Shape[] = []
  let i = from
  const next = () => data[i++]! / 10
  let end: [number, number] | null = null
  while (i < data.length) {
    const op = data[i++]!
    const kind = Math.floor(op / ROLES.length)
    const role = ROLES[op % ROLES.length]
    if (role == null || kind > RING) throw new Error(`thumbnail data: no shape has the code ${op}`)
    let ends: [number, number] | null = null
    switch (kind) {
      case CIRCLE:
        shapes.push({ kind: 'circle', role, cx: next(), cy: next(), r: next() })
        break
      case CAPSULE:
      case JOINED: {
        if (kind === JOINED && end == null) throw new Error(`thumbnail data: a joined capsule with no capsule before it, at ${i - 1}`)
        const start: [number, number] = kind === JOINED ? end! : [next(), next()]
        const x2 = next()
        const y2 = next()
        shapes.push({ kind: 'capsule', role, x1: start[0], y1: start[1], x2, y2, width: next() })
        ends = [x2, y2]
        break
      }
      case RECT:
        shapes.push({ kind: 'rect', role, x: next(), y: next(), w: next(), h: next(), radius: next() })
        break
      case POLYGON: {
        const count = data[i++]!
        const points: number[] = []
        for (let p = 0; p < 2 * count; p++) points.push(next())
        shapes.push({ kind: 'polygon', role, points })
        break
      }
      case RING: {
        const [cx, cy, outer, inner] = [data[i++]!, data[i++]!, data[i++]!, data[i++]!].map((v) => v / 100) as [number, number, number, number]
        shapes.push({ kind: 'polygon', role, points: ringPoints(cx, cy, outer, inner) })
        break
      }
    }
    end = ends
  }
  return shapes
}
