/**
 * The quiet line at the very bottom of Settings: "Grit 1.1 · a1b2c3d · AppsbyAdam".
 *
 * The version comes from package.json and the id from the build's commit, both put in at build time by
 * vite.config.ts (plugins/buildInfo.ts; read in ./build.ts); package.json itself is never part of the bundle.
 * Plain functions only: plugins/buildInfo.ts uses them while the config loads.
 */
/** "1.1.0" is shown as "1.1"; a patch that is not 0 stays ("1.1.1"). */
export function shownVersion(version: string): string {
  return version.replace(/^(\d+\.\d+)\.0$/, '$1')
}

/** The first 7 characters of a commit id, lower case; anything that is not hex gives no id (''). */
export function cleanBuildId(id: string | null | undefined): string {
  const trimmed = (id ?? '').trim()
  return /^[0-9a-fA-F]{7,40}$/.test(trimmed) ? trimmed.slice(0, 7).toLowerCase() : ''
}

export function appVersionLine(version: string, buildId: string | null | undefined): string {
  const id = cleanBuildId(buildId)
  return ['Grit ' + shownVersion(version), id, 'AppsbyAdam'].filter((part) => part !== '').join(' · ')
}

/** The same line as a screen reader says it, without the middle dots. */
export function appVersionSpoken(version: string, buildId: string | null | undefined): string {
  const id = cleanBuildId(buildId)
  return `Grit version ${shownVersion(version)}${id !== '' ? `, build ${id}` : ''}, by AppsbyAdam`
}
