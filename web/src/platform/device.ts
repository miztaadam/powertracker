/** The parts of `navigator` these checks read, so a test can fake them. */
export type DeviceNavigator = Pick<Navigator, 'userAgent' | 'platform' | 'maxTouchPoints'>

/**
 * True on an iPhone, iPod or iPad. An iPad on iPadOS 13+ says it is a Mac ("MacIntel"); only its touch
 * screen gives it away. Kept in step with public/mode-head-start.js, which sets `ios-standalone` before
 * the first paint.
 */
export function isIos(nav: DeviceNavigator = navigator): boolean {
  return /iPad|iPhone|iPod/.test(nav.userAgent) || (nav.platform === 'MacIntel' && nav.maxTouchPoints > 1)
}

/** True on an Android phone or tablet. */
export function isAndroid(nav: DeviceNavigator = navigator): boolean {
  return /Android/i.test(nav.userAgent) && !isIos(nav)
}

/** Whether this browser can vibrate the phone: Android browsers can; iPhones give web apps no way to. */
export function canVibrate(): boolean {
  return typeof navigator !== 'undefined' && typeof (navigator as Navigator & { vibrate?: unknown }).vibrate === 'function'
}

/** The end-of-rest buzz: two short pulses. Does nothing where the phone cannot vibrate. */
export function vibrateRestEnd(): void {
  if (!canVibrate()) return
  try {
    navigator.vibrate([200, 100, 200])
  } catch {
    // A buzz that cannot run must not break the screen.
  }
}
