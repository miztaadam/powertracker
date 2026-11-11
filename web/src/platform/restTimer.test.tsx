import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { RestBar } from '../screens/today/RestBar'
import { IDLE, RestTimerStore, adjust, isRunning, remainingSec, useRestTimer, type RestTimerState, type RestTimerStorage } from './restTimer'

const NOW = 1_800_000_000_000

function memoryStorage(saved: RestTimerState | null = null) {
  const box = { saved, saves: [] as RestTimerState[] }
  const storage: RestTimerStorage = {
    load: async () => box.saved,
    save: async (state) => {
      box.saved = state.endsAtMs == null ? null : state
      box.saves.push(state)
    },
  }
  return { box, storage }
}

/** Lets the store's reads and writes settle. */
const settle = () => act(async () => void (await new Promise((done) => setTimeout(done, 0))))

describe('rest timer arithmetic', () => {
  it('remainingSec_neverNegative', () => {
    const state = { endsAtMs: NOW + 90_000, totalSec: 90 }
    expect(remainingSec(state, NOW)).toBe(90)
    // Rounded up, so the clock reads 0:00 only when the rest is really over.
    expect(remainingSec(state, NOW + 89_001)).toBe(1)
    expect(remainingSec(state, NOW + 90_000)).toBe(0)
    expect(remainingSec(state, NOW + 500_000)).toBe(0)
    expect(remainingSec(IDLE, NOW)).toBe(0)
  })

  it('adjust_clampsToNow', () => {
    const state = { endsAtMs: NOW + 20_000, totalSec: 180 }
    const shortened = adjust(state, -30, NOW)
    expect(shortened.endsAtMs).toBe(NOW)
    expect(remainingSec(shortened, NOW)).toBe(0)
    expect(isRunning(shortened, NOW)).toBe(false)
    expect(adjust(state, -10, NOW)).toEqual({ endsAtMs: NOW + 10_000, totalSec: 180 })
    expect(adjust(IDLE, 30, NOW)).toEqual(IDLE)
  })

  it('adjust_growsTheTotalWhenTheRestOutgrowsIt', () => {
    const state = { endsAtMs: NOW + 170_000, totalSec: 180 }
    expect(adjust(state, 30, NOW)).toEqual({ endsAtMs: NOW + 200_000, totalSec: 200 })
  })
})

describe('RestTimerStore', () => {
  it('starts, adjusts and skips', () => {
    const store = new RestTimerStore()
    const heard = vi.fn()
    store.subscribe(heard)
    store.start(180, NOW)
    expect(store.get()).toEqual({ endsAtMs: NOW + 180_000, totalSec: 180 })
    store.adjust(30, NOW + 10_000)
    expect(remainingSec(store.get(), NOW + 10_000)).toBe(200)
    store.skip()
    expect(store.get()).toEqual(IDLE)
    expect(heard).toHaveBeenCalledTimes(3)
    // Nothing running: nothing to adjust.
    store.adjust(30, NOW)
    expect(store.get()).toEqual(IDLE)
  })

  it('a rest of no length stops the one that is running', () => {
    const store = new RestTimerStore()
    store.start(180, NOW)
    store.start(0, NOW + 1_000)
    expect(store.get()).toEqual(IDLE)
  })

  it('taking off all that is left ends the rest', () => {
    const store = new RestTimerStore()
    store.start(20, NOW)
    store.adjust(-30, NOW + 1_000)
    expect(store.get()).toEqual(IDLE)
  })

  it('expires only at zero and says how late it is', () => {
    const store = new RestTimerStore()
    expect(store.expire(NOW)).toBeNull()
    store.start(90, NOW)
    expect(store.expire(NOW + 89_999)).toBeNull()
    expect(store.expire(NOW + 92_500)).toBe(2_500)
    expect(store.get()).toEqual(IDLE)
    expect(store.expire(NOW + 93_000)).toBeNull()
  })

  it('saves each change and picks up a running rest after a reload', async () => {
    const { box, storage } = memoryStorage()
    const first = new RestTimerStore()
    first.attach(storage)
    await settle()
    const endsAtMs = Date.now() + 120_000
    first.start(120, endsAtMs - 120_000)
    await settle()
    expect(box.saved).toEqual({ endsAtMs, totalSec: 120 })

    const reloaded = new RestTimerStore()
    reloaded.attach(storage)
    await settle()
    expect(reloaded.get()).toEqual({ endsAtMs, totalSec: 120 })
    reloaded.skip()
    await settle()
    expect(box.saved).toBeNull()
  })

  it('drops a saved rest that ended while the app was closed', async () => {
    const { box, storage } = memoryStorage({ endsAtMs: Date.now() - 1_000, totalSec: 90 })
    const store = new RestTimerStore()
    store.attach(storage)
    await settle()
    expect(store.get()).toEqual(IDLE)
    expect(box.saved).toBeNull()
  })

  it('a rest started before the saved one is read wins', async () => {
    const { storage } = memoryStorage({ endsAtMs: Date.now() + 60_000, totalSec: 90 })
    const store = new RestTimerStore()
    store.attach(storage)
    store.start(180, NOW)
    await settle()
    expect(store.get()).toEqual({ endsAtMs: NOW + 180_000, totalSec: 180 })
  })

  it('a storage that fails does not stop the timer', async () => {
    const store = new RestTimerStore()
    store.attach({ load: () => Promise.reject(new Error('no storage')), save: () => Promise.reject(new Error('QuotaExceededError')) })
    await settle()
    store.start(90, NOW)
    await settle()
    expect(store.get()).toEqual({ endsAtMs: NOW + 90_000, totalSec: 90 })
  })
})

describe('useRestTimer', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  /** How often the stand-in for the shell has rendered. */
  let shellRenders = 0

  function Bar({ store, onZero }: { store: RestTimerStore; onZero?: (lateMs: number) => void }) {
    const timer = useRestTimer(store, onZero)
    shellRenders++
    return (
      <>
        <button onClick={() => timer.start(180)}>start</button>
        {timer.running && <RestBar store={store} />}
      </>
    )
  }
  const tap = (name: string) => fireEvent.click(screen.getByRole('button', { name }))
  const clock = () => screen.queryByRole('timer')?.getAttribute('aria-label')

  it('afterScreenLock_showsCorrectRemaining', () => {
    vi.useFakeTimers()
    vi.setSystemTime(NOW)
    render(<Bar store={new RestTimerStore()} />)
    tap('start')
    expect(clock()).toBe('Rest, 3:00 left')

    // The phone locks: the page is suspended, so the clock moves on 90 s and no tick runs.
    vi.setSystemTime(NOW + 90_000)
    act(() => void document.dispatchEvent(new Event('visibilitychange')))
    expect(clock()).toBe('Rest, 1:30 left')
  })

  it('counts down, takes −30 s / +30 s and Skip', () => {
    vi.useFakeTimers()
    vi.setSystemTime(NOW)
    render(<Bar store={new RestTimerStore()} />)
    tap('start')
    act(() => void vi.advanceTimersByTime(12_000))
    expect(clock()).toBe('Rest, 2:48 left')
    tap('+30 s')
    expect(clock()).toBe('Rest, 3:18 left')
    tap('−30 s')
    tap('−30 s')
    expect(clock()).toBe('Rest, 2:18 left')
    tap('Skip')
    expect(clock()).toBeUndefined()
  })

  it('only the bar redraws as the seconds pass, not the shell around it', () => {
    vi.useFakeTimers()
    vi.setSystemTime(NOW)
    render(<Bar store={new RestTimerStore()} />)
    tap('start')
    const before = shellRenders
    act(() => void vi.advanceTimersByTime(5_000))
    expect(clock()).toBe('Rest, 2:55 left')
    expect(shellRenders).toBe(before)
    // A change to the rest itself does reach the shell.
    tap('Skip')
    expect(shellRenders).toBeGreaterThan(before)
  })

  it('ends once at zero and reports how late it noticed', () => {
    vi.useFakeTimers()
    vi.setSystemTime(NOW)
    const onZero = vi.fn()
    render(<Bar store={new RestTimerStore()} onZero={onZero} />)
    tap('start')
    act(() => void vi.advanceTimersByTime(179_900))
    expect(onZero).not.toHaveBeenCalled()
    act(() => void vi.advanceTimersByTime(5_000))
    expect(clock()).toBeUndefined()
    expect(onZero).toHaveBeenCalledOnce()
    expect(onZero.mock.calls[0]![0]).toBeLessThan(250)
  })

  it('skipping never calls the end', () => {
    vi.useFakeTimers()
    vi.setSystemTime(NOW)
    const onZero = vi.fn()
    render(<Bar store={new RestTimerStore()} onZero={onZero} />)
    tap('start')
    tap('Skip')
    act(() => void vi.advanceTimersByTime(600_000))
    expect(onZero).not.toHaveBeenCalled()
  })
})
