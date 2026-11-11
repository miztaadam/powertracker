import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { todayIso } from './index'

describe('todayIso', () => {
  const originalTz = process.env.TZ
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => {
    vi.useRealTimers()
    if (originalTz === undefined) delete process.env.TZ
    else process.env.TZ = originalTz
  })

  it('uses the local date west of UTC, not the UTC date', () => {
    process.env.TZ = 'America/New_York' // UTC-5 in November
    vi.setSystemTime(new Date('2026-11-11T23:30:00-05:00'))
    expect(new Date().toISOString().slice(0, 10)).toBe('2026-11-12') // proves UTC would differ
    expect(todayIso()).toBe('2026-11-11')
  })

  it('uses the local date east of UTC, not the UTC date', () => {
    process.env.TZ = 'Pacific/Auckland' // UTC+13 in November
    vi.setSystemTime(new Date('2026-11-11T00:30:00+13:00'))
    expect(new Date().toISOString().slice(0, 10)).toBe('2026-11-10')
    expect(todayIso()).toBe('2026-11-11')
  })

  it('zero-pads month and day', () => {
    process.env.TZ = 'UTC'
    vi.setSystemTime(new Date('2026-01-05T12:00:00Z'))
    expect(todayIso()).toBe('2026-01-05')
  })
})
