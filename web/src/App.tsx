import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import { onDbVersionChange, type GritDb } from './db/db'
import type { SchemaVersion } from './db/schema'
import { singleTabGuard, type SingleTabGuard, type TabRole } from './platform/singleTab'
import { acceptPrivateMode, detectPrivateMode, privateModeAccepted, requestPersistence } from './platform/storage'
import { UPDATE_READY, useUpdatePrompt, type RegisterSW } from './platform/update'
import { goBack, leave, navigate, useRoute, type Route } from './router'
import { CheckInScreen } from './screens/checkin/CheckInScreen'
import { CoachScreen } from './screens/coach/CoachScreen'
import { InstallCard } from './screens/install/InstallCard'
import { ExercisesScreen } from './screens/library/ExercisesScreen'
import { BlankScreen } from './screens/lock/GateScreen'
import { LockGateHost } from './screens/lock/LockGateHost'
import { PlanBuilderScreen } from './screens/plan/builder/PlanBuilderScreen'
import { PLAN_SAVED, PlanScreen, type PlanMessage } from './screens/plan/PlanScreen'
import { TemplatesScreen } from './screens/plan/templates/TemplatesScreen'
import { planSetText } from './state/useTemplates'
import { ExerciseHistoryScreen } from './screens/progress/ExerciseHistoryScreen'
import { LiftHistoryScreen } from './screens/progress/LiftHistoryScreen'
import { ProgressScreen } from './screens/progress/ProgressScreen'
import { ErrorBoundary } from './screens/recovery/ErrorBoundary'
import { OtherTabScreen, ReloadScreen } from './screens/recovery/OtherTabScreen'
import { PrivateModeScreen } from './screens/recovery/PrivateModeScreen'
import { RecoveryScreen } from './screens/recovery/RecoveryScreen'
import { StorageFullDialog } from './screens/recovery/StorageFullDialog'
import { SettingsScreen } from './screens/settings/SettingsScreen'
import { RestBar } from './screens/today/RestBar'
import { TodayView } from './screens/today/TodayScreen'
import { todayIso } from './domain'
import { useExerciseBrowser, type ExerciseBrowser } from './state/exercisePicker'
import { useGritDb } from './state/useGritDb'
import { useRest, type Rest } from './state/useRest'
import { useToday, type TodayActions, type TodayState } from './state/useToday'
import { useTapHaptics } from './state/useTapHaptics'
import { useTrainingMode } from './state/useTrainingMode'
import { TrainingModeContext } from './ui/components/ModeLabel'
import { RestFlash } from './ui/components/RestFlash'
import { TabBar } from './ui/components/TabBar'
import { ModeSwitchOverlay } from './ui/motion/ModeSwitchOverlay'
import { useElementHeight } from './ui/useElementHeight'
import './ui/shell.css'

export type AppProps = {
  /** The database to open. Default `grit`; tests use their own. */
  dbName?: string
  /** For tests: the schema versions to open the database with. */
  schemaVersions?: SchemaVersion[]
  /** For tests: the service worker registration. */
  registerSW?: RegisterSW
}

/**
 * What outlives the screens: today's session, shared by the Today and Exercises tabs, the Exercises tab's search and
 * filters, and Plan's message after a template is saved ("Plan set: …"), which Plan shows once and clears.
 */
type Kept = { session: TodayState & TodayActions; browser: ExerciseBrowser; planMessage: PlanMessage | null; setPlanMessage: (message: PlanMessage | null) => void }

function screenFor(route: Route, db: GritDb, { session, browser, planMessage, setPlanMessage }: Kept): ReactNode {
  const openSettings = () => navigate({ page: 'settings' })
  const openBuilder = () => navigate({ page: 'planBuilder' })
  const openCheckIn = () => navigate({ page: 'checkIn' })
  if ('tab' in route) {
    const openPlan = () => navigate({ tab: 'plan' })
    if (route.tab === 'today') return <TodayView db={db} session={session} onOpenCoach={() => navigate({ tab: 'coach' })} onOpenSettings={openSettings} onOpenPlan={openPlan} onOpenBuilder={openBuilder} banner={<InstallCard db={db} />} />
    if (route.tab === 'coach') return <CoachScreen db={db} onOpenPlan={openPlan} onOpenBuilder={openBuilder} onOpenCheckIn={openCheckIn} />
    if (route.tab === 'progress') return <ProgressScreen db={db} onOpenLift={(lift) => navigate({ page: 'lift', lift })} onOpenExercise={(exerciseId) => navigate({ page: 'exercise', exerciseId })} onCheckIn={openCheckIn} />
    if (route.tab === 'exercises') return <ExercisesScreen db={db} session={session} browser={browser} onOpenPlan={() => navigate({ tab: 'plan' })} onOpenToday={() => navigate({ tab: 'today' })} />
    return (
      <PlanScreen
        db={db}
        onOpenSettings={openSettings}
        onBuildPlan={openBuilder}
        onChooseTemplate={(mode) => navigate({ page: 'templates', mode })}
        message={planMessage}
        onMessageShown={() => setPlanMessage(null)}
      />
    )
  }
  if (route.page === 'planBuilder')
    return (
      <PlanBuilderScreen
        db={db}
        onLeave={leaveBuilder}
        onDone={(undo) => {
          // "Plan saved." (1.14.1), with Undo when a plan was replaced.
          setPlanMessage({ text: PLAN_SAVED, undo })
          goBack({ tab: 'plan' })
        }}
      />
    )
  if (route.page === 'templates')
    return (
      <TemplatesScreen
        key={route.mode}
        db={db}
        mode={route.mode}
        onLeave={() => leave({ tab: 'plan' })}
        onSaved={(name, undo) => {
          setPlanMessage({ text: planSetText(name), undo })
          goBack({ tab: 'plan' })
        }}
      />
    )
  if (route.page === 'checkIn') return <CheckInScreen db={db} onLeave={leaveCheckIn} onSeeCoach={() => goBack({ tab: 'coach' })} onDone={leaveCheckIn} />
  if (route.page === 'settings') return <SettingsScreen db={db} onBack={() => goBack({ tab: 'plan' })} />
  if (route.page === 'exercise') return <ExerciseHistoryScreen db={db} exerciseId={route.exerciseId} onBack={() => goBack({ tab: 'progress' })} />
  return <LiftHistoryScreen db={db} lift={route.lift} onBack={() => goBack({ tab: 'progress' })} />
}

/** Leaving the check-in, saved or not, goes back where it was opened from (Coach or Progress), or to Coach. */
const leaveCheckIn = () => leave({ tab: 'coach' })

/** Leaving the builder unsaved goes back where it was opened from (Plan, Today or Coach), or to Plan. */
const leaveBuilder = () => leave({ tab: 'plan' })

/** The app itself: the screens, the tab bar and what docks above it. Rendered only while Grit is unlocked. */
function Shell({ db, rest, registerSW }: { db: GritDb; rest: Rest; registerSW?: RegisterSW }) {
  const route = useRoute()
  const update = useUpdatePrompt(db, registerSW)
  // Kept here, above the screens: the session is the same one on the Today and Exercises tabs, and an
  // exercise added from one is there on the other. The Exercises tab's search and filters last while
  // other tabs are on screen, and no longer than the page.
  const session = useToday(db, todayIso, rest.onSetLogged)
  const browser = useExerciseBrowser()
  const [planMessage, setPlanMessage] = useState<PlanMessage | null>(null)
  const activeTab = 'tab' in route ? route.tab : null
  // The session outlives the Today tab, so coming back to the tab reads the clock again, as coming
  // back to the front does (a finish summary up, or a session being logged, keeps the day).
  const { refreshDay } = session
  const shownTab = useRef(activeTab)
  useEffect(() => {
    if (activeTab === 'today' && shownTab.current !== 'today') refreshDay()
    shownTab.current = activeTab
  }, [activeTab, refreshDay])
  // As on Android, the bar is part of Today; the alert at zero shows on every screen.
  const showRestBar = activeTab === 'today' && rest.running
  const showDock = showRestBar || update.ready
  const dock = useRef<HTMLDivElement>(null)
  // Messages rise by the dock's real height, so they clear its buttons at any text size.
  const dockHeight = useElementHeight(dock, showDock)
  return (
    <>
      <div className="shell" style={{ '--rest-dock-height': `${dockHeight}px` } as CSSProperties}>
        <main className="shell-main">{screenFor(route, db, { session, browser, planMessage, setPlanMessage })}</main>
        {showDock && (
          <div className="shell-rest" ref={dock}>
            {update.ready && (
              <button type="button" className="shell-update label-large" onClick={update.refresh}>
                {UPDATE_READY}
              </button>
            )}
            {showRestBar && <RestBar store={rest.store} />}
          </div>
        )}
        <TabBar current={activeTab} />
        <RestFlash showId={rest.flashId} />
        <StorageFullDialog db={db} />
      </div>
      {/* The mode switch animations (1.15), over the whole app; empty at rest. */}
      <ModeSwitchOverlay />
    </>
  )
}

/** Opens the database, then shows the app behind its lock. Nothing of the log is rendered before the lock has had its say. */
function Opened({ dbName, schemaVersions, registerSW }: AppProps) {
  const db = useGritDb(dbName, schemaVersions)
  // The rest lives here, above the lock and the screens, so it runs on while the lifter looks at
  // another tab or unlocks. Only its start and end redraw: the countdown redraws inside RestBar.
  const rest = useRest(db)
  // The side the app is on: its look on the page root, its label on the top-level screens.
  const mode = useTrainingMode(db)
  // "Vibrate on taps", followed live for the buzz on the main actions (platform/haptics.ts).
  useTapHaptics(db)
  // Null until the browser has been asked.
  const [privateMode, setPrivateMode] = useState<boolean | null>(null)
  const [outdated, setOutdated] = useState(false)

  useEffect(() => {
    let stale = false
    // Every launch: Safari grants it once Grit is on the Home Screen. Settings shows the answer.
    requestPersistence().catch(() => {})
    // "Continue anyway" is remembered: a window wrongly taken for private is not asked twice.
    ;(privateModeAccepted() ? Promise.resolve(false) : detectPrivateMode())
      .catch(() => false)
      .then((value) => {
        if (!stale) setPrivateMode(value)
      })
      .catch(() => {})
    return () => {
      stale = true
    }
  }, [])
  // Another tab opened the database with a newer schema: this page's connection is closed for good.
  useEffect(() => onDbVersionChange(() => setOutdated(true)), [])

  if (outdated) return <ReloadScreen />
  if (db == null || privateMode == null) return <BlankScreen />
  const continueAnyway = () => {
    acceptPrivateMode()
    setPrivateMode(false)
  }
  return (
    <TrainingModeContext.Provider value={mode}>
      <LockGateHost db={db}>{privateMode ? <PrivateModeScreen db={db} onContinue={continueAnyway} /> : <Shell db={db} rest={rest} registerSW={registerSW} />}</LockGateHost>
    </TrainingModeContext.Provider>
  )
}

/** Lets one tab at a time run Grit; a second one says so and offers to take over. Nothing shows until that is settled. */
function SingleTab({ children }: { children: ReactNode }) {
  const [role, setRole] = useState<TabRole | null>(null)
  const guard = useRef<SingleTabGuard | null>(null)
  useEffect(() => {
    const started = singleTabGuard(setRole)
    guard.current = started
    return () => {
      started.dispose()
      guard.current = null
    }
  }, [])
  if (role == null) return <BlankScreen />
  if (role === 'secondary') return <OtherTabScreen onUseHere={guard.current?.canTakeOver ? () => guard.current?.takeOver() : undefined} />
  return <>{children}</>
}

export function App({ dbName = 'grit', schemaVersions, registerSW }: AppProps) {
  return (
    <ErrorBoundary fallback={(error) => <RecoveryScreen error={error} dbName={dbName} />}>
      <SingleTab>
        <Opened dbName={dbName} schemaVersions={schemaVersions} registerSW={registerSW} />
      </SingleTab>
    </ErrorBoundary>
  )
}
