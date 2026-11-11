import { describe, expect, it } from 'vitest'
import { entryMessage, parseEntry } from './entry'

describe('parseEntry', () => {
  it('reads a comma as the decimal point', () => {
    expect(parseEntry('102,5', 'kg')).toEqual({ ok: true, value: 102.5 })
    expect(parseEntry(' 102.5 ', 'kg')).toEqual({ ok: true, value: 102.5 })
    expect(parseEntry(',5', 'kg')).toEqual({ ok: true, value: 0.5 })
    expect(parseEntry('102,', 'kg')).toEqual({ ok: true, value: 102 })
  })

  it('refuses what is not a number', () => {
    expect(parseEntry('abc', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('1.2.3', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('1e3', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('1,2,3', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('.', 'rpe')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry(',', 'rpe')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('1 0', 'reps')).toEqual({ ok: false, reason: 'nan' })
  })

  it('refuses a leading sign', () => {
    expect(parseEntry('-5', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('-0', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('+5', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('-5', 'reps')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('-8', 'rpe')).toEqual({ ok: false, reason: 'nan' })
  })

  it('refuses a comma used as a thousands separator', () => {
    // "1,000" is a thousand to whoever typed it, never 1 kg.
    expect(parseEntry('1,000', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('12,500', 'kg')).toEqual({ ok: false, reason: 'nan' })
    expect(parseEntry('1,000', 'reps')).toEqual({ ok: false, reason: 'nan' })
    // Two or four digits after the comma are decimals.
    expect(parseEntry('102,50', 'kg')).toEqual({ ok: true, value: 102.5 })
    expect(parseEntry('1,0000', 'kg')).toEqual({ ok: true, value: 1 })
    // A point is always the decimal point.
    expect(parseEntry('1.000', 'kg')).toEqual({ ok: true, value: 1 })
  })

  it('keeps weight within 0 to 1000 kg, two decimals at most', () => {
    expect(parseEntry('1200', 'kg')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('1000.01', 'kg')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('0', 'kg')).toEqual({ ok: true, value: 0 })
    expect(parseEntry('1000', 'kg')).toEqual({ ok: true, value: 1000 })
    expect(parseEntry('61.255', 'kg')).toEqual({ ok: true, value: 61.26 })
    expect(parseEntry('61.2549', 'kg')).toEqual({ ok: true, value: 61.25 })
  })

  it('takes whole reps from 1 to 99', () => {
    expect(parseEntry('0', 'reps')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('100', 'reps')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('5.5', 'reps')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('5,5', 'reps')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('1', 'reps')).toEqual({ ok: true, value: 1 })
    expect(parseEntry('99', 'reps')).toEqual({ ok: true, value: 99 })
  })

  it('keeps RPE within 6 to 10 and snaps it to the half', () => {
    expect(parseEntry('11', 'rpe')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('5.5', 'rpe')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('8.3', 'rpe')).toEqual({ ok: true, value: 8.5 })
    expect(parseEntry('8,2', 'rpe')).toEqual({ ok: true, value: 8 })
    expect(parseEntry('10', 'rpe')).toEqual({ ok: true, value: 10 })
    expect(parseEntry('6', 'rpe')).toEqual({ ok: true, value: 6 })
  })

  it('calls an empty field empty, whatever it is for', () => {
    for (const kind of ['kg', 'reps', 'rpe'] as const) expect(parseEntry('', kind)).toEqual({ ok: false, reason: 'empty' })
    expect(parseEntry('  ', 'kg')).toEqual({ ok: false, reason: 'empty' })
  })

  it('calls a number too long to hold out of range, not a crash', () => {
    expect(parseEntry('9'.repeat(400), 'kg')).toEqual({ ok: false, reason: 'range' })
    expect(parseEntry('9'.repeat(400), 'reps')).toEqual({ ok: false, reason: 'range' })
  })
})

describe('entryMessage', () => {
  it('says what the field takes, and nothing for an empty one', () => {
    expect(entryMessage('kg', 'nan')).toBe('Numbers only')
    expect(entryMessage('kg', 'range')).toBe('0 to 1000 kg')
    expect(entryMessage('reps', 'range')).toBe('Whole, 1 to 99')
    expect(entryMessage('rpe', 'range')).toBe('6 to 10')
    expect(entryMessage('rpe', 'empty')).toBeUndefined()
  })
})
