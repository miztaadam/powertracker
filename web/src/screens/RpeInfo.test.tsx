import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { getSettings, type GritDb } from '../db/db'
import { checkRpeInfoOpensSheet } from '../test/guideChecks'
import { TODAY, exerciseNamed, plan, setsOn, startSession, todayFixture } from '../test/todayFixture'
import { PlanScreen } from './plan/PlanScreen'
import { SettingsScreen } from './settings/SettingsScreen'
import { TodayScreen } from './today/TodayScreen'

// Ported from Android's RpeInfoTest: the "What RPE means" button and its sheet, from each place RPE is set (strict press spec §5.4).

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  await plan(db, (await exerciseNamed(db, 'Squat')).id)
  await startSession(db, TODAY)
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

describe('RPE info', () => {
  it('rpeInfo_opensFromTodayPlanAndSettings', async () => {
    // Today: beside the RPE field's label on the exercise card. Reading it logs and changes nothing.
    const today = render(<TodayScreen db={db} today={() => TODAY} onOpenCoach={() => {}} />)
    await screen.findByRole('button', { name: 'Log set 1' })
    await checkRpeInfoOpensSheet()
    expect(screen.getByRole('button', { name: 'Log set 1' })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'RPE' })).toHaveValue('8')
    expect(await setsOn(db, TODAY)).toEqual([])
    today.unmount()

    // Plan: beside the RPE in the item dialog, which stays open behind the sheet.
    const planScreen = render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} />)
    const wednesday = await screen.findByRole('region', { name: 'Wednesday' })
    await userEvent.click(within(wednesday).getByRole('button', { name: 'More' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Edit' }))
    const dialog = screen.getByRole('dialog', { name: 'Edit Wednesday' })
    expect(within(dialog).getByText('RPE')).toBeInTheDocument()
    await checkRpeInfoOpensSheet(dialog)
    expect(screen.getByRole('dialog', { name: 'Edit Wednesday' })).toBeInTheDocument()
    expect(within(within(dialog).getByRole('group', { name: 'RPE' })).getByText('8')).toBeInTheDocument()
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    planScreen.unmount()

    // Settings: beside the default RPE.
    render(<SettingsScreen db={db} onBack={() => {}} today={() => TODAY} />)
    await screen.findByText('Default RPE')
    await checkRpeInfoOpensSheet()
    expect((await getSettings(db)).defaultRpe).toBe(8)
  })
})
