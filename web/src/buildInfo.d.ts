// Put in at build time by vite.config.ts and vitest.config.ts (plugins/buildInfo.ts).
/** package.json's version, for example "1.1.0". */
declare const __APP_VERSION__: string
/** The build's 7-character commit id, or '' when the build has none. */
declare const __BUILD_ID__: string
