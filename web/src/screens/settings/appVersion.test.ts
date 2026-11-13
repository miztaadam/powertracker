import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { buildInfo } from '../../../plugins/buildInfo'
import { appVersionLine, appVersionSpoken, cleanBuildId, shownVersion } from './appVersion'
import { APP_VERSION, BUILD_ID } from './build'

const WEB = resolve(__dirname, '../../..')
const GRADLE = resolve(WEB, '../app/build.gradle.kts')
const packageVersion = (JSON.parse(readFileSync(resolve(WEB, 'package.json'), 'utf8')) as { version: string }).version

describe('the version line', () => {
  it('shows the version without a .0 patch, then the build id, then AppsbyAdam', () => {
    expect(appVersionLine('1.1.0', 'a1b2c3d')).toBe('Grit 1.1 · a1b2c3d · AppsbyAdam')
  })
  it('leaves the id out, with no gap, when the build has none', () => {
    expect(appVersionLine('1.1.0', '')).toBe('Grit 1.1 · AppsbyAdam')
    expect(appVersionLine('1.1.0', undefined)).toBe('Grit 1.1 · AppsbyAdam')
  })
  it('keeps a patch that is not 0', () => {
    expect(shownVersion('1.1.1')).toBe('1.1.1')
    expect(appVersionLine('1.1.1', 'a1b2c3d')).toBe('Grit 1.1.1 · a1b2c3d · AppsbyAdam')
    expect(shownVersion('1.10.0')).toBe('1.10')
  })
  it('drops an id that is not hex', () => {
    expect(cleanBuildId('a1b2c3g')).toBe('')
    expect(cleanBuildId('<b>hi</b>')).toBe('')
    expect(appVersionLine('1.1.0', 'not-hex')).toBe('Grit 1.1 · AppsbyAdam')
  })
  it('cuts a full commit id to 7 characters', () => {
    expect(cleanBuildId('A1B2C3D4E5F60718293a4b5c6d7e8f9012345678')).toBe('a1b2c3d')
  })
  it('is spoken without the middle dots', () => {
    expect(appVersionSpoken('1.1.0', 'a1b2c3d')).toBe('Grit version 1.1, build a1b2c3d, by AppsbyAdam')
    expect(appVersionSpoken('1.1.0', '')).toBe('Grit version 1.1, by AppsbyAdam')
  })
  it('gets the version from package.json and a fixed id under test', () => {
    expect(APP_VERSION).toBe(packageVersion)
    expect(BUILD_ID).toBe('a1b2c3d')
  })
})

describe('the build id at build time', () => {
  it('prefers GRIT_BUILD_ID, even when empty', () => {
    expect(buildInfo({ GRIT_BUILD_ID: 'abc1234', VERCEL_GIT_COMMIT_SHA: 'f'.repeat(40) }).buildId).toBe('abc1234')
    expect(buildInfo({ GRIT_BUILD_ID: '', VERCEL_GIT_COMMIT_SHA: 'f'.repeat(40) }).buildId).toBe('')
  })
  it('uses the Vercel commit, cut to 7', () => {
    expect(buildInfo({ VERCEL_GIT_COMMIT_SHA: '0123456789abcdef0123456789abcdef01234567' }).buildId).toBe('0123456')
  })
  it('gives no id for anything that is not hex', () => {
    expect(buildInfo({ VERCEL_GIT_COMMIT_SHA: 'zzzzzzzzzz' }).buildId).toBe('')
  })
  it('reads the version from package.json', () => {
    expect(buildInfo({ GRIT_BUILD_ID: '' }).version).toBe(packageVersion)
  })
})

// Android's versionName and the web's shown version are bumped together; this fails when they are not.
// A host that has only web/ (the Vercel build) skips it.
describe('Android and web versions', () => {
  it.skipIf(!existsSync(GRADLE))('agree', () => {
    const versionName = /versionName\s*=\s*"([^"]+)"/.exec(readFileSync(GRADLE, 'utf8'))?.[1]
    expect(versionName).toBeDefined()
    expect(shownVersion(packageVersion)).toBe(versionName)
  })
})
