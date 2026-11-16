import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { canVibrate, isAndroid, isIos, vibrateRestEnd, type DeviceNavigator } from './device'

const SCRIPT = readFileSync(resolve(process.cwd(), 'public/mode-head-start.js'), 'utf8')

const IPHONE = 'Mozilla/5.0 (iPhone; CPU iPhone OS 26_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Mobile/15E148 Safari/604.1'
const IPAD_OLD = 'Mozilla/5.0 (iPad; CPU OS 12_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1 Mobile/15E148 Safari/604.1'
const MAC_SAFARI = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15'
const ANDROID = 'Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36'
const WINDOWS = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36'

/** Each device: what it reports, and whether it is iOS / Android. */
const DEVICES: { name: string; nav: DeviceNavigator; ios: boolean; android: boolean }[] = [
  { name: 'iPhone', nav: { userAgent: IPHONE, platform: 'iPhone', maxTouchPoints: 5 }, ios: true, android: false },
  { name: 'old iPad', nav: { userAgent: IPAD_OLD, platform: 'iPad', maxTouchPoints: 5 }, ios: true, android: false },
  // iPadOS 13+ asks for the desktop site and says it is a Mac; only the touch screen tells.
  { name: 'iPad as a Mac', nav: { userAgent: MAC_SAFARI, platform: 'MacIntel', maxTouchPoints: 5 }, ios: true, android: false },
  { name: 'Mac', nav: { userAgent: MAC_SAFARI, platform: 'MacIntel', maxTouchPoints: 0 }, ios: false, android: false },
  { name: 'Android phone', nav: { userAgent: ANDROID, platform: 'Linux armv8l', maxTouchPoints: 5 }, ios: false, android: true },
  { name: 'Windows', nav: { userAgent: WINDOWS, platform: 'Win32', maxTouchPoints: 0 }, ios: false, android: false },
]

/** Runs the head script as the browser does, on the faked device, in a browser tab or from the Home Screen. */
function runHeadScript(nav: DeviceNavigator, how: { displayMode?: boolean; navigatorStandalone?: boolean }) {
  vi.stubGlobal('navigator', { ...nav, standalone: how.navigatorStandalone })
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: q === '(display-mode: standalone)' && how.displayMode === true }))
  try {
    new Function(SCRIPT)()
  } finally {
    vi.unstubAllGlobals()
  }
  return document.documentElement.classList.contains('ios-standalone')
}

afterEach(() => {
  vi.unstubAllGlobals()
  document.documentElement.classList.remove('ios-standalone')
  delete (navigator as unknown as Record<string, unknown>).vibrate
})

describe('which device this is', () => {
  for (const device of DEVICES) {
    it(`${device.name}: iOS ${device.ios}, Android ${device.android}`, () => {
      expect(isIos(device.nav)).toBe(device.ios)
      expect(isAndroid(device.nav)).toBe(device.android)
    })
  }
})

describe('the ios-standalone class from the head script', () => {
  for (const device of DEVICES) {
    it(`${device.name}: set only in the Home Screen app`, () => {
      // A browser tab: never.
      expect(runHeadScript(device.nav, {})).toBe(false)
      // From the Home Screen, by either sign: only on iOS, the same answer as isIos.
      expect(runHeadScript(device.nav, { displayMode: true })).toBe(device.ios)
      document.documentElement.classList.remove('ios-standalone')
      expect(runHeadScript(device.nav, { navigatorStandalone: true })).toBe(device.ios)
      document.documentElement.classList.remove('ios-standalone')
    })
  }

  it('the jsdom page itself is a browser tab: no class', () => {
    new Function(SCRIPT)()
    expect(document.documentElement.classList.contains('ios-standalone')).toBe(false)
  })
})

describe('vibration', () => {
  it('cannot vibrate where the browser has no navigator.vibrate (iPhone, jsdom)', () => {
    expect(canVibrate()).toBe(false)
    expect(() => vibrateRestEnd()).not.toThrow()
  })

  it('buzzes twice at the end of a rest where it can (Android)', () => {
    const vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', { value: vibrate, configurable: true })
    expect(canVibrate()).toBe(true)
    vibrateRestEnd()
    expect(vibrate).toHaveBeenCalledWith([200, 100, 200])
  })

  it('a vibrate that throws does not break the screen', () => {
    Object.defineProperty(navigator, 'vibrate', {
      value: () => {
        throw new Error('blocked')
      },
      configurable: true,
    })
    expect(() => vibrateRestEnd()).not.toThrow()
  })
})
