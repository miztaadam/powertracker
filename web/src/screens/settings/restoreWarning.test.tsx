import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { encodeBackup, exportBackup } from '../../backup/backup'
import { addPlanItem } from '../../db/catalogue'
import { readSafetyCopy, writeSafetyCopy, type GritDb } from '../../db/db'
import type { TrainingMode } from '../../db/schema'
import { TODAY, exerciseNamed, todayFixture } from '../../test/todayFixture'
import { SettingsScreen } from './SettingsScreen'

/** 1.14.1: a restore that would remove a side's plan says so first. */

const V5 = JSON.parse(readFileSync(resolve(__dirname, '../../backup/__fixtures__/web-v5.json'), 'utf8')) as { planItems: { id: number; mode?: string }[]; skips: { planItemId: number | null }[] }
const NOW = new Date(2026, 10, 19, 9, 41).getTime()

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

/** The v5 fixture with only `planItems`, its skips pointing at no other plan item. */
function backupOf(planItems: { id: number; mode?: string }[]) {
  const ids = new Set(planItems.map((p) => p.id))
  return JSON.stringify({ ...V5, planItems, skips: V5.skips.map((s) => ({ ...s, planItemId: s.planItemId != null && ids.has(s.planItemId) ? s.planItemId : null })) })
}

/** The v5 fixture keeping only `sides`' plan items (it has both). */
const backupWith = (...sides: TrainingMode[]) => backupOf(V5.planItems.filter((p) => sides.includes((p.mode ?? 'POWERLIFTING') as TrainingMode)))

async function planOn(...sides: TrainingMode[]) {
  const squat = (await exerciseNamed(db, 'Squat')).id
  for (const mode of sides) await addPlanItem(db, { weekday: 1, exerciseId: squat, sets: 3, reps: 5, targetRpe: 8 }, null, mode)
}

const show = () => render(<SettingsScreen db={db} onBack={() => {}} today={() => TODAY} now={() => NOW} />)
const pick = async (text: string) => userEvent.upload(await screen.findByLabelText('Backup file'), new File([text], 'grit-backup.json', { type: 'application/json' }))
const data = async () => encodeBackup(await exportBackup(db, 0))
const dialog = () => within(screen.getByRole('dialog'))

describe('the restore warning', () => {
  it('namesTheBodybuildingSide_whenOnlyItLosesItsPlan', async () => {
    await planOn('POWERLIFTING', 'BODYBUILDING')
    show()
    await pick(backupWith('POWERLIFTING'))
    expect(await screen.findByRole('dialog', { name: 'This backup has no bodybuilding plan' })).toBeInTheDocument()
    expect(dialog().getByText("Restoring it removes your current bodybuilding plan. Your other data is replaced by the backup's too.")).toBeInTheDocument()
    expect(dialog().getAllByRole('button').map((b) => b.textContent)).toEqual(['Cancel', 'Restore anyway'])
  })

  it('namesThePowerliftingSide_whenOnlyItLosesItsPlan', async () => {
    await planOn('POWERLIFTING')
    show()
    await pick(backupWith('BODYBUILDING'))
    expect(await screen.findByRole('dialog', { name: 'This backup has no powerlifting plan' })).toBeInTheDocument()
    expect(dialog().getByText("Restoring it removes your current powerlifting plan. Your other data is replaced by the backup's too.")).toBeInTheDocument()
  })

  it('namesBothSides_inOneDialog', async () => {
    await planOn('POWERLIFTING', 'BODYBUILDING')
    show()
    await pick(backupWith())
    expect(await screen.findByRole('dialog', { name: 'This backup has no powerlifting or bodybuilding plan' })).toBeInTheDocument()
    expect(dialog().getByText("Restoring it removes your current powerlifting and bodybuilding plans. Your other data is replaced by the backup's too.")).toBeInTheDocument()
    expect(screen.getAllByRole('dialog')).toHaveLength(1)
  })

  it('cancel_restoresNothing_andLeavesTheSafetyCopy', async () => {
    await planOn('BODYBUILDING')
    await writeSafetyCopy(db)
    const copy = await readSafetyCopy(db)
    show()
    const before = await data()
    await pick(backupWith('POWERLIFTING'))
    await screen.findByRole('dialog', { name: 'This backup has no bodybuilding plan' })
    await userEvent.click(dialog().getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await data()).toBe(before)
    expect(await readSafetyCopy(db)).toEqual(copy)
  })

  it('restoreAnyway_restores', async () => {
    await planOn('BODYBUILDING')
    show()
    await pick(backupWith('POWERLIFTING'))
    await screen.findByRole('dialog', { name: 'This backup has no bodybuilding plan' })
    await userEvent.click(dialog().getByRole('button', { name: 'Restore anyway' }))
    expect(await screen.findByText('Backup restored.')).toBeInTheDocument()
    const plan = (await exportBackup(db, 0)).planItems
    expect(plan.length).toBeGreaterThan(0)
    expect(plan.every((p) => p.mode === 'POWERLIFTING')).toBe(true)
  })

  it('restoringTheSafetyCopy_warnsToo', async () => {
    // The copy was made before the bodybuilding plan existed.
    await writeSafetyCopy(db)
    await planOn('BODYBUILDING')
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Restore safety copy' }))
    expect(await screen.findByRole('dialog', { name: 'This backup has no bodybuilding plan' })).toBeInTheDocument()
  })

  it('noWarning_whenTheBackupHasThePlan_orTheSideHadNone', async () => {
    await planOn('BODYBUILDING')
    show()
    // The backup has a bodybuilding plan; the empty powerlifting side loses nothing.
    await pick(backupWith('BODYBUILDING'))
    expect(await screen.findByRole('dialog', { name: 'Replace all data?' })).toBeInTheDocument()
  })

  it('anOldBackupWithNoModes_isThePowerliftingSidesPlan', async () => {
    await planOn('POWERLIFTING')
    show()
    await pick(backupOf(V5.planItems.filter((p) => p.mode === 'POWERLIFTING').map(({ mode: _mode, ...p }) => p)))
    expect(await screen.findByRole('dialog', { name: 'Replace all data?' })).toBeInTheDocument()
  })
})
