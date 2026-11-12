import { describe, expect, it } from 'vitest'
import type { Shape } from './FigureSvg'
import { decodeShapes, encodeShapes, TOLERANCE } from './thumbnailFormat'

/** A plate ring as the domain's kit draws it (Kit.kt `ring`): the outer circle one way, the inner the other. */
function ring(cx: number, cy: number, outer: number, inner: number, steps = 36): number[] {
  const points: number[] = []
  const r2 = (v: number) => Math.round(v * 100) / 100
  for (let i = 0; i <= steps; i++) points.push(r2(cx + outer * Math.cos((2 * Math.PI * i) / steps)), r2(cy - outer * Math.sin((2 * Math.PI * i) / steps)))
  for (let i = steps; i >= 0; i--) points.push(r2(cx + inner * Math.cos((2 * Math.PI * i) / steps)), r2(cy - inner * Math.sin((2 * Math.PI * i) / steps)))
  return points
}

/** The numbers a shape is drawn from, in a fixed order. */
const numbers = (s: Shape): number[] => {
  switch (s.kind) {
    case 'circle':
      return [s.cx, s.cy, s.r]
    case 'capsule':
      return [s.x1, s.y1, s.x2, s.y2, s.width]
    case 'rect':
      return [s.x, s.y, s.w, s.h, s.radius]
    case 'polygon':
      return [...s.points]
  }
}

/** Every shape the same kind and role as its original, with every number within the format's tolerance. */
function expectClose(decoded: readonly Shape[], original: readonly Shape[]) {
  expect(decoded.map((s) => `${s.kind} ${s.role}`)).toEqual(original.map((s) => `${s.kind} ${s.role}`))
  decoded.forEach((s, i) => {
    const want = numbers(original[i]!)
    const got = numbers(s)
    expect(got).toHaveLength(want.length)
    got.forEach((v, j) => expect(Math.abs(v - want[j]!), `shape ${i} number ${j}: ${v} for ${want[j]}`).toBeLessThanOrEqual(TOLERANCE + 1e-9))
  })
}

describe('thumbnail data format', () => {
  it('thumbnailFormat_roundTripsEveryKind_toWithinItsTolerance', () => {
    const notARing = ring(174.34, 157.04, 34, 32.03)
    notARing[40] = notARing[40]! + 3
    const shapes: Shape[] = [
      { kind: 'capsule', role: 'BODY_FAR', x1: 135.92, y1: 218.19, x2: 200.62, y2: 214.86, width: 21.15 },
      // Joined to the one before: it starts where that one ends.
      { kind: 'capsule', role: 'BODY_FAR', x1: 200.62, y1: 214.86, x2: 165.19, y2: 269.42, width: 16.62 },
      { kind: 'capsule', role: 'GAP', x1: 10.05, y1: 0, x2: 399.95, y2: 300, width: 0.25 },
      { kind: 'circle', role: 'BODY', cx: 211.3, cy: 64.27, r: 13.6 },
      { kind: 'rect', role: 'EQUIPMENT', x: 87.6, y: 200.19, w: 224.28, h: 11.03, radius: 3.68 },
      { kind: 'polygon', role: 'EQUIPMENT', points: [1.01, 2.02, 30.33, 2.02, 30.33, 40.44, 1.01, 40.44] },
      { kind: 'polygon', role: 'ACCENT', points: ring(174.34, 157.04, 34, 32.03) },
      { kind: 'polygon', role: 'ACCENT', points: notARing },
      { kind: 'circle', role: 'ACCENT_FILL', cx: 174.34, cy: 157.04, r: 34 },
    ]
    const encoded = encodeShapes(shapes)
    // Whole numbers only: they are the shortest to write and to read.
    for (const n of encoded) expect(Number.isInteger(n), String(n)).toBe(true)
    expectClose(decodeShapes(encoded), shapes)
  })

  it('thumbnailFormat_writesARingAsItsCentreAndRadii_andAnythingElseInFull', () => {
    const exact = encodeShapes([{ kind: 'polygon', role: 'ACCENT', points: ring(174.34, 157.04, 34, 32.03) }])
    // One code and four numbers, not 148 coordinates.
    expect(exact).toHaveLength(5)
    const bent = ring(174.34, 157.04, 34, 32.03)
    bent[40] = bent[40]! + 3
    // A code, the point count and every coordinate.
    expect(encodeShapes([{ kind: 'polygon', role: 'ACCENT', points: bent }])).toHaveLength(2 + 148)
    // A different number of steps is not the kit's ring either.
    expect(encodeShapes([{ kind: 'polygon', role: 'ACCENT', points: ring(10, 10, 5, 4, 24) }])).toHaveLength(2 + 100)
  })

  it('thumbnailFormat_writesAJoinedCapsuleWithoutItsStart', () => {
    const first: Shape = { kind: 'capsule', role: 'BODY', x1: 1, y1: 2, x2: 3, y2: 4, width: 5 }
    const joined: Shape = { kind: 'capsule', role: 'BODY_FAR', x1: 3, y1: 4, x2: 6, y2: 7, width: 8 }
    const apart: Shape = { kind: 'capsule', role: 'BODY', x1: 3.2, y1: 4, x2: 6, y2: 7, width: 8 }
    expect(encodeShapes([first, joined])).toHaveLength(6 + 4)
    expect(encodeShapes([first, apart])).toHaveLength(6 + 6)
    // Only a capsule straight after a capsule is joined to it.
    const circle: Shape = { kind: 'circle', role: 'BODY', cx: 3, cy: 4, r: 1 }
    expect(encodeShapes([first, circle, joined])).toHaveLength(6 + 4 + 6)
    expectClose(decodeShapes(encodeShapes([first, circle, joined, joined])), [first, circle, joined, joined])
  })

  it('thumbnailFormat_refusesWhatItCannotWrite', () => {
    expect(() => encodeShapes([{ kind: 'circle', role: 'PURPLE', cx: 1, cy: 1, r: 1 }])).toThrow(/PURPLE/)
    expect(() => decodeShapes([99, 1, 2, 3])).toThrow(/99/)
  })
})
