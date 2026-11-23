// This build's version and commit id, put in by vite.config.ts (plugins/buildInfo.ts).
/** package.json's version, for example "1.1.0". */
export const APP_VERSION: string = __APP_VERSION__
/** The 7-character commit id, or '' when the build has none. */
export const BUILD_ID: string = __BUILD_ID__
