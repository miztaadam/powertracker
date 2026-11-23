import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { cleanBuildId } from '../src/screens/settings/appVersion'

/**
 * The app version and the build's short commit id, for the line at the bottom of Settings.
 *
 * Reads only inside web/ (the Vercel project's root directory): the version from package.json; the id from
 * GRIT_BUILD_ID when it is set, even to '' (the end-to-end build sets a fixed one, so the screenshots never
 * change with the commit), else VERCEL_GIT_COMMIT_SHA (set on every Vercel deploy), else `git rev-parse HEAD`.
 * No git, or not a repository, gives no id. The id is kept only if it is hex, cut to 7 characters.
 */
// Paths, not URL objects: under jsdom (vitest) the global URL is not Node's.
const WEB = resolve(dirname(fileURLToPath(import.meta.url)), '..')

export function buildInfo(env: Record<string, string | undefined> = process.env): { version: string; buildId: string } {
  const pkg = JSON.parse(readFileSync(resolve(WEB, 'package.json'), 'utf8')) as { version: string }
  return { version: pkg.version, buildId: cleanBuildId(env.GRIT_BUILD_ID ?? env.VERCEL_GIT_COMMIT_SHA ?? gitHead()) }
}

function gitHead(): string {
  try {
    return execFileSync('git', ['rev-parse', 'HEAD'], { cwd: WEB, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'], timeout: 5000 })
  } catch {
    return ''
  }
}

/** Vite `define` entries for src/buildInfo.d.ts. */
export function buildDefines(env?: Record<string, string | undefined>): Record<string, string> {
  const { version, buildId } = buildInfo(env)
  return { __APP_VERSION__: JSON.stringify(version), __BUILD_ID__: JSON.stringify(buildId) }
}
