import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { decodeBackup, encodeBackup, exportBackup, restoreBackup } from '../backup/backup'
import { updateSettings, type AppSettings, type GritDb } from '../db/db'
import { RestBar } from '../screens/today/RestBar'
import { TodayScreen } from '../screens/today/TodayScreen'
import { TODAY, addDays, exerciseNamed, logSet, plan, set, setsOn, startSession, todayFixture } from '../test/todayFixture'
import { RestFlash } from '../ui/components/RestFlash'
import { REST_TIMER_KEY, useRest } from './useRest'

const NOW = new Date(`${TODAY}T10:00:00`).getTime()

let fx: ReturnType<typeof todayFixture>
let db: GritDb
/** Start times of the oscillators the fake audio context was asked to play. */
let beeps: number[]

class FakeAudioContext {
  state = 'suspended'
  currentTime = 0
  destination = {}
  async resume() {
    this.state = 'running'
  }
  createGain() {
    return { connect: (node: unknown) => node, gain: { setValueAtTime() {}, linearRampToValueAtTime() {} } }
  }
  createOscillator() {
    return { type: 'sine', frequency: { value: 0 }, connect: (node: unknown) => node, start: (at: number) => void beeps.push(at), stop() {} }
  }
}

class FakeSentinel extends EventTarget {
  release = vi.fn(async () => {
    this.dispatchEvent(new Event('release'))
  })
}
let sentinels: FakeSentinel[]
let requestLock: ReturnType<typeof vi.fn<(type: string) => Promise<FakeSentinel>>>

beforeEach(async () => {
  // Only the clock is faked: the database needs real timers, and the countdown must not count ticks anyway.
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(NOW)
  beeps = []
  sentinels = []
  renders = { shell: 0, screen: 0 }
  vi.stubGlobal('AudioContext', FakeAudioContext)
  requestLock = vi.fn(async () => {
    const sentinel = new FakeSentinel()
    sentinels.push(sentinel)
    return sentinel
  })
  Object.defineProperty(navigator, 'wakeLock', { configurable: true, value: { request: requestLock } })
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
  Reflect.deleteProperty(navigator, 'wakeLock')
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

/** How often the shell stand-in and the screen inside it have rendered. */
let renders = { shell: 0, screen: 0 }

function Screen() {
  renders.screen++
  return null
}

/** The shell's wiring: sets start rests, the bar shows them, the flash marks zero. */
function Host({ database, today }: { database: GritDb; today?: boolean }) {
  const rest = useRest(database)
  renders.shell++
  return (
    <>
      {today && <TodayScreen db={database} today={() => TODAY} onOpenCoach={() => {}} onSetLogged={rest.onSetLogged} />}
      <Screen />
      <button onClick={() => rest.onSetLogged(true)}>main set</button>
      <button onClick={() => rest.onSetLogged(false)}>accessory set</button>
      {rest.running && <RestBar store={rest.store} />}
      <RestFlash showId={rest.flashId} />
    </>
  )
}

const settings = (change: Partial<AppSettings>) => updateSettings(db, (s) => ({ ...s, ...change }))
const tap = (name: string | RegExp) => userEvent.click(screen.getByRole('button', { name }))
const clock = () => screen.queryByRole('timer')?.getAttribute('aria-label')
const showsClock = (text: string) => waitFor(() => expect(clock()).toBe(text))
const restOver = () => waitFor(() => expect(clock()).toBeUndefined())
/** Long enough for a tick and for anything it set off. */
const aMoment = () => act(async () => void (await new Promise((done) => setTimeout(done, 450))))
const storedRest = async () => (await db.meta.get(REST_TIMER_KEY))?.value

describe('useRest', () => {
  it('startsAfterFailedSetsToo', async () => {
    const squat = await exerciseNamed(db, 'Squat')
    await plan(db, squat.id)
    // Last week's top set gives today's editor its weight.
    await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squat.id, 150))
    await startSession(db, TODAY)
    render(<Host database={db} today />)
    await screen.findByRole('button', { name: 'Log set 1' })
    expect(clock()).toBeUndefined()
    await tap('Missed')
    await waitFor(async () => expect((await setsOn(db, TODAY)).map((s) => s.failed)).toEqual([true]))
    await showsClock('Rest, 3:00 left')
  })

  it('rests as long as Settings says: main lifts and accessories apart', async () => {
    await settings({ restMainSec: 240, restAccessorySec: 75 })
    render(<Host database={db} />)
    await tap('main set')
    await showsClock('Rest, 4:00 left')
    // The next set replaces the rest that is running.
    await tap('accessory set')
    await showsClock('Rest, 1:15 left')
  })

  it('a rest length of zero starts nothing', async () => {
    await settings({ restAccessorySec: 0 })
    render(<Host database={db} />)
    await tap('main set')
    await showsClock('Rest, 3:00 left')
    await tap('accessory set')
    await restOver()
  })

  it('the shell and its screen do not re-render as the seconds pass', async () => {
    render(<Host database={db} />)
    await tap('main set')
    await showsClock('Rest, 3:00 left')
    await aMoment()
    const before = { ...renders }
    for (const [seconds, text] of [[1, '2:59'], [2, '2:58'], [3, '2:57']] as const) {
      vi.setSystemTime(NOW + seconds * 1000)
      await showsClock(`Rest, ${text} left`)
    }
    await aMoment()
    expect(renders).toEqual(before)
    // Only a change to the rest itself reaches them.
    await tap('+30 s')
    await showsClock('Rest, 3:27 left')
    expect(renders.shell).toBeGreaterThan(before.shell)
  })

  describe('alertModes', () => {
    async function restToZero(restAlert: AppSettings['restAlert'], lateMs = 0) {
      await settings({ restAlert })
      render(<Host database={db} />)
      await tap('main set')
      await showsClock('Rest, 3:00 left')
      vi.setSystemTime(NOW + 180_000 + lateMs)
      await restOver()
      await aMoment()
    }

    it('SOUND plays the tone once and flashes', async () => {
      await restToZero('SOUND')
      // One tone is two beeps.
      expect(beeps).toHaveLength(2)
      expect(screen.getByTestId('rest-flash')).toBeInTheDocument()
    })

    it('SILENT only flashes', async () => {
      await restToZero('SILENT')
      expect(beeps).toEqual([])
      expect(screen.getByTestId('rest-flash')).toBeInTheDocument()
    })

    it('OFF does nothing', async () => {
      await restToZero('OFF')
      expect(beeps).toEqual([])
      expect(screen.queryByTestId('rest-flash')).toBeNull()
    })

    it('a rest found over long after zero ends without a stale alert', async () => {
      // The phone was locked through the end of the rest and opened a minute later.
      await restToZero('SOUND', 60_000)
      expect(beeps).toEqual([])
      expect(screen.queryByTestId('rest-flash')).toBeNull()
    })

    it('a rest found over a moment after zero still alerts', async () => {
      await restToZero('SOUND', 3_000)
      expect(beeps).toHaveLength(2)
      expect(screen.getByTestId('rest-flash')).toBeInTheDocument()
    })

    it('a restored rest that reaches zero before the settings are read still alerts', async () => {
      await settings({ restAlert: 'SILENT' })
      await db.meta.put({ key: REST_TIMER_KEY, value: { endsAtMs: NOW + 30_000, totalSec: 180 } })
      // The settings are slow to arrive after the reload.
      let arrive = () => {}
      const held = new Promise<void>((done) => (arrive = done))
      const read = db.settings.toArray.bind(db.settings)
      const slowRead = async () => {
        await held
        return read()
      }
      vi.spyOn(db.settings, 'toArray').mockImplementation(slowRead as unknown as typeof db.settings.toArray)
      render(<Host database={db} />)
      await showsClock('Rest, 0:30 left')
      vi.setSystemTime(NOW + 30_000)
      await restOver()
      await aMoment()
      expect(screen.queryByTestId('rest-flash')).toBeNull()

      arrive()
      expect(await screen.findByTestId('rest-flash')).toBeInTheDocument()
    })

    it('Skip is not an alert', async () => {
      await settings({ restAlert: 'SOUND' })
      render(<Host database={db} />)
      await tap('main set')
      await showsClock('Rest, 3:00 left')
      await tap('Skip')
      await restOver()
      await aMoment()
      expect(beeps).toEqual([])
      expect(screen.queryByTestId('rest-flash')).toBeNull()
    })
  })

  describe('wakeLock_onlyWhenKeepAwakeAndRunning', () => {
    it('holds the screen awake while a rest runs and lets go when it ends', async () => {
      render(<Host database={db} />)
      await aMoment()
      expect(requestLock).not.toHaveBeenCalled()

      await tap('main set')
      await showsClock('Rest, 3:00 left')
      await waitFor(() => expect(requestLock).toHaveBeenCalledExactlyOnceWith('screen'))
      await aMoment()
      expect(sentinels[0]!.release).not.toHaveBeenCalled()

      await tap('Skip')
      await waitFor(() => expect(sentinels[0]!.release).toHaveBeenCalledOnce())
      expect(requestLock).toHaveBeenCalledOnce()
    })

    it('lets go at zero', async () => {
      render(<Host database={db} />)
      await tap('main set')
      await waitFor(() => expect(sentinels).toHaveLength(1))
      vi.setSystemTime(NOW + 180_000)
      await waitFor(() => expect(sentinels[0]!.release).toHaveBeenCalledOnce())
    })

    it('never asks when "Keep screen awake during rests" is off', async () => {
      await settings({ keepAwake: false })
      render(<Host database={db} />)
      await tap('main set')
      await showsClock('Rest, 3:00 left')
      await aMoment()
      expect(requestLock).not.toHaveBeenCalled()
    })

    it('takes the lock again when the page comes back while the rest still runs', async () => {
      render(<Host database={db} />)
      await tap('main set')
      await waitFor(() => expect(sentinels).toHaveLength(1))
      // The page was hidden: the browser drops the lock on its own.
      sentinels[0]!.dispatchEvent(new Event('release'))
      act(() => void document.dispatchEvent(new Event('visibilitychange')))
      await waitFor(() => expect(requestLock).toHaveBeenCalledTimes(2))
    })

    it('a refused lock leaves the rest running', async () => {
      requestLock.mockRejectedValue(new DOMException('low power mode', 'NotAllowedError'))
      render(<Host database={db} />)
      await tap('main set')
      await waitFor(() => expect(requestLock).toHaveBeenCalled())
      await aMoment()
      expect(clock()).toBe('Rest, 3:00 left')
    })
  })

  it('a restore stops the rest that was running', async () => {
    render(<Host database={db} />)
    await tap('main set')
    await showsClock('Rest, 3:00 left')
    await waitFor(async () => expect(await storedRest()).toBeDefined())

    const file = decodeBackup(encodeBackup(await exportBackup(db, NOW)))
    if (!file.ok) throw new Error('the export must decode')
    await act(() => restoreBackup(db, file.file))

    await restOver()
    expect(await storedRest()).toBeUndefined()
    // It ended; it did not reach zero: no tone and no flash, now or later.
    vi.setSystemTime(NOW + 181_000)
    await aMoment()
    expect(beeps).toEqual([])
    expect(screen.queryByTestId('rest-flash')).toBeNull()
    expect(await storedRest()).toBeUndefined()
  })

  describe('reload mid-rest', () => {
    it('keeps the countdown', async () => {
      const first = render(<Host database={db} />)
      await tap('main set')
      await showsClock('Rest, 3:00 left')
      await waitFor(async () => expect(await storedRest()).toEqual({ endsAtMs: NOW + 180_000, totalSec: 180 }))
      first.unmount()

      vi.setSystemTime(NOW + 60_000)
      render(<Host database={db} />)
      await showsClock('Rest, 2:00 left')
    })

    it('forgets a rest that ended meanwhile, without an alert', async () => {
      const first = render(<Host database={db} />)
      await tap('main set')
      await waitFor(async () => expect(await storedRest()).toBeDefined())
      first.unmount()

      vi.setSystemTime(NOW + 181_000)
      render(<Host database={db} />)
      await waitFor(async () => expect(await storedRest()).toBeUndefined())
      await aMoment()
      expect(clock()).toBeUndefined()
      expect(beeps).toEqual([])
      expect(screen.queryByTestId('rest-flash')).toBeNull()
    })

    it('skipping clears the stored rest', async () => {
      render(<Host database={db} />)
      await tap('main set')
      await waitFor(async () => expect(await storedRest()).toBeDefined())
      await tap('Skip')
      await waitFor(async () => expect(await storedRest()).toBeUndefined())
    })
  })
})
