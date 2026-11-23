// The user agents the e2e runs choose between (1.17.4). The default project emulates an iPhone 13, whose descriptor
// carries Safari's iPhone user agent, so by default the app takes its iPhone paths (Back through a history entry of its
// own, historyCloses.ts). `E2E_UA=android` swaps in Chrome on Android for a run (playwright.config.ts), with the same
// viewport and touch, for the paths every other phone takes (Back through CloseWatcher, closeRequests.ts).
// Never re-shoot the committed screenshots under it: they are iPhone shots.

export const ANDROID_CHROME_UA = 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Mobile Safari/537.36'

/** Safari on an iPhone, as the iPhone 13 descriptor gives it: the default project's user agent, named for specs that pin it. */
export const IPHONE_SAFARI_UA = 'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/27.2 Mobile/15E148 Safari/604.1'
