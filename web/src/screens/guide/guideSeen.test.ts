import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { encodeBackup, exportBackup, restoreBackup } from '../../backup/backup'
import { DEFAULT_SETTINGS, getSettings, pickFirstSide, setTrainingMode, updateSettings, withFirstSide, type GritDb } from '../../db/db'
import { grit } from '../../domain'
import { todayFixture } from '../../test/todayFixture'
import { BUILD_MY_PLAN, CHOOSE_A_TEMPLATE, DONE, SEE_MY_PLAN, lastButtons } from './guideLabels'
import { GUIDE_KEY, GUIDE_SEEN_KEY, guideGateOff, guideSeen, isNewUser, markGuideSeen } from './guideSeen'
import { CHOOSE_A_TEMPLATE as PLAN_CHOOSE_A_TEMPLATE } from '../plan/PlanScreen'

/** "Seen", the new-user check and the first-launch pick (1.16, Task 3), as Android's GuideFirstLaunchTest. */

let fx: ReturnType<typeof todayFixture>
let db: GritDb
beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  await fx.dispose()
})

describe('guide seen', () => {
  it('keys: meta guide:seen, localStorage grit:guide', () => {
    expect(GUIDE_SEEN_KEY).toBe('guide:seen')
    expect(GUIDE_KEY).toBe('grit:guide')
  })

  it('every vitest test starts with the gate shut (the shared setup); without the key it is open', () => {
    expect(guideGateOff()).toBe(true)
    localStorage.removeItem(GUIDE_KEY)
    expect(guideGateOff()).toBe(false)
  })

  it('is false until marked, then true', async () => {
    expect(await guideSeen(db)).toBe(false)
    await markGuideSeen(db)
    expect(await guideSeen(db)).toBe(true)
    expect((await db.meta.get(GUIDE_SEEN_KEY))?.value).toBe(true)
  })

  it('a new user has no session and no plan item, on either side', async () => {
    expect(await isNewUser(db)).toBe(true)
    for (const mode of ['POWERLIFTING', 'BODYBUILDING'] as const) {
      const id = await db.planItems.add({ weekday: 1, exerciseId: 1, sets: 3, reps: 5, targetRpe: 8, sortOrder: 0, mode, repsHigh: null } as never)
      expect(await isNewUser(db), `${mode} plan item`).toBe(false)
      await db.planItems.delete(id)
      const session = await db.sessions.add({ date: '2026-11-11', bodyweightKg: null, readiness: null, mode } as never)
      expect(await isNewUser(db), `${mode} session`).toBe(false)
      await db.sessions.delete(session)
    }
    expect(await isNewUser(db)).toBe(true)
  })

  it('aBackupMadeAfterTheGuide_hasNoGuideKey', async () => {
    await markGuideSeen(db)
    const text = encodeBackup(await exportBackup(db, 0))
    expect(text.toLowerCase()).not.toContain('guide')
  })

  it('restore_keepsSeen', async () => {
    const file = await exportBackup(db, 0)
    await markGuideSeen(db)
    await restoreBackup(db, file)
    expect(await guideSeen(db)).toBe(true)
  })
})

describe('the first-launch pick', () => {
  it('sets the side as the switch does, but dates no side as left', async () => {
    const before = await getSettings(db)
    expect(withFirstSide(before, 'POWERLIFTING')).toBe(before)
    const picked = await pickFirstSide(db, 'BODYBUILDING')
    expect(picked.trainingMode).toBe('BODYBUILDING')
    expect(picked.powerliftingLeftOn).toBeNull()
    expect(picked.bodybuildingLeftOn).toBeNull()
    // The bodybuilding days start as a copy of powerlifting's, as the first switch makes them.
    expect(picked.bodybuildingTrainingDays).toEqual(before.trainingDays)
  })

  it('pickingBodybuildingOnFirstLaunch_thenFirstVisitingPowerlifting_showsNoWelcomeBack: 40 days on, through Settings, with a plan', async () => {
    await updateSettings(db, () => DEFAULT_SETTINGS)
    await pickFirstSide(db, 'BODYBUILDING')
    const s = await setTrainingMode(db, 'POWERLIFTING', '2026-12-21')
    expect(s.powerliftingLeftOn).toBeNull()
    expect(s.bodybuildingLeftOn).toBe('2026-12-21')
    // What useWelcomeBack asks, for the side on, with a plan there and no session ever on it: no card.
    expect(grit.welcomeBack(s.powerliftingLeftOn, '2026-12-21', true, null)).toBe(false)
  })
})

describe('screen 8', () => {
  it('offers Choose a template (and Build my plan on bodybuilding) with no plan; Done and See my plan with one', () => {
    expect(lastButtons('POWERLIFTING', false)).toEqual({ primary: [CHOOSE_A_TEMPLATE, 'choose-template'], secondary: null })
    expect(lastButtons('BODYBUILDING', false)).toEqual({ primary: [CHOOSE_A_TEMPLATE, 'choose-template'], secondary: [BUILD_MY_PLAN, 'build-plan'] })
    for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) expect(lastButtons(side, true)).toEqual({ primary: [DONE, 'done'], secondary: [SEE_MY_PLAN, 'see-plan'] })
  })

  it("uses the empty plan's own words", () => {
    expect(CHOOSE_A_TEMPLATE).toBe(PLAN_CHOOSE_A_TEMPLATE)
    expect(BUILD_MY_PLAN).toBe('Build my plan')
  })
})
