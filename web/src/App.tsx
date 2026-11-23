import { Component, lazy, memo, Suspense, useCallback, useContext, useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import { OlderCopyError, onDbVersionChange, type GritDb } from './db/db'
import type { SchemaVersion } from './db/schema'
import { singleTabGuard, type SingleTabGuard, type TabRole } from './platform/singleTab'
import { acceptPrivateMode, detectPrivateMode, privateModeAccepted, requestPersistence } from './platform/storage'
import { UPDATE_READY, useUpdatePrompt, type RegisterSW } from './platform/update'
import { goBack, leave, navigate, routeToHash, useRoute, type Route } from './router'
import { afterDialogEntries } from './ui/components/historyCloses'
import { liveQuery } from 'dexie'
import { pickFirstSide } from './db/db'
import type { TrainingMode } from './db/schema'
import type { GuideExit } from './screens/guide/GuideScreen'
import { guideGateOff, guideSeen, isNewUser, markGuideSeen } from './screens/guide/guideSeen'
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
import { OlderCopyScreen } from './screens/recovery/OlderCopyScreen'
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
import './ui/narrowTabs.css'
import './ui/narrowDeeper.css'

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
  if (route.page === 'settings') return <SettingsScreen db={db} onBack={() => goBack({ tab: 'plan' })} onOpenGuide={() => navigate({ page: 'guide' })} />
  // Drawn by the Shell over everything, in place of the screens and the tab bar.
  if (route.page === 'guide') return null
  if (route.page === 'exercise') return <ExerciseHistoryScreen db={db} exerciseId={route.exerciseId} onBack={() => goBack({ tab: 'progress' })} />
  return <LiftHistoryScreen db={db} lift={route.lift} onBack={() => goBack({ tab: 'progress' })} />
}

/** The guide (1.16), a chunk of its own: nothing of it is loaded until it is opened. */
const GuideScreen = lazy(() => import('./screens/guide/GuideScreen'))

/** Whether `mode`'s side has a plan item, followed live; null until read. */
function useHasPlan(db: GritDb, mode: TrainingMode): boolean | null {
  const [has, setHas] = useState<boolean | null>(null)
  useEffect(() => {
    const subscription = liveQuery(() => db.planItems.filter((p) => p.mode === mode).count()).subscribe({ next: (n) => setHas(n > 0), error: () => setHas(false) })
    return () => subscription.unsubscribe()
  }, [db, mode])
  return has
}

/**
 * The guide as a route, on the side the app is on: opened by the first launch (`firstLaunch`), or from Settings as a
 * replay. Only the first launch's screen 2 picks the side (with no animation and no side dated as left), and "seen" is
 * written by the gate's owner when the route leaves the guide, whichever way; a replay writes nothing. Skip and Done
 * go back where it was opened from, or to Today; a plan button leaves the guide for Plan, then opens the template list
 * or the builder over it, so the browser's Back from either lands on Plan, never on the guide.
 */
function GuideRoute({ db, firstLaunch, onFailed }: { db: GritDb; firstLaunch: boolean; onFailed: () => void }) {
  const mode = useContext(TrainingModeContext)
  const hasPlan = useHasPlan(db, mode)
  // Read once: reduced motion and the e2e flag both put data-motion="off" on <html> before the app starts.
  const [motionOn] = useState(() => document.documentElement.getAttribute('data-motion') !== 'off')
  // Every exit, a failed load included, goes back where it was opened from, or to Today.
  const close = useCallback(() => leave({ tab: 'today' }), [])
  const onClose = useCallback(
    (exit: GuideExit) => {
      if (exit === 'skipped' || exit === 'done') return close()
      // In place of the guide's entry: Plan, then what was asked for over it. As one call, after any entries kept for
      // open dialogs (1.17.4, iPhone and iPad; elsewhere at once).
      afterDialogEntries(() => {
        window.location.replace(routeToHash({ tab: 'plan' }))
        if (exit === 'choose-template') navigate({ page: 'templates', mode })
        else if (exit === 'build-plan') navigate({ page: 'planBuilder' })
      })
    },
    [close, mode],
  )
  const onPickSide = useCallback((side: TrainingMode) => {
    if (firstLaunch) void pickFirstSide(db, side).catch(() => {})
  }, [db, firstLaunch])
  // Screen 8 waits for the plan to be read, so its buttons never change under a finger.
  if (hasPlan == null) return <BlankScreen />
  return (
    <GuideChunkBoundary
      onFailed={() => {
        // A guide that could not load was not seen: the gate's owner shuts it for this page without writing "seen".
        onFailed()
        close()
      }}
    >
      <Suspense fallback={<BlankScreen />}>
        <GuideScreen mode={mode} firstLaunch={firstLaunch} hasPlan={hasPlan} motionOn={motionOn} onPickSide={onPickSide} onClose={onClose} />
      </Suspense>
    </GuideChunkBoundary>
  )
}

/**
 * Catches the guide failing, its chunk not loading above all (offline before the service worker has it, or a stale
 * chunk after an update): the guide closes as if skipped, writing nothing, and the app shows as it was. Without it the
 * failure would reach the root boundary, the recovery screen. (React keeps a failed lazy import, so a second open in
 * the same page load closes again.)
 */
class GuideChunkBoundary extends Component<{ onFailed: () => void; children: ReactNode }, { failed: boolean }> {
  state = { failed: false }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  componentDidCatch() {
    this.props.onFailed()
  }

  render() {
    return this.state.failed ? <BlankScreen /> : this.props.children
  }
}

/** Leaving the check-in, saved or not, goes back where it was opened from (Coach or Progress), or to Coach. */
const leaveCheckIn = () => leave({ tab: 'coach' })

/** Leaving the builder unsaved goes back where it was opened from (Plan, Today or Coach), or to Plan. */
const leaveBuilder = () => leave({ tab: 'plan' })

/**
 * The app itself: the screens, the tab bar and what docks above it. Rendered only while Grit is unlocked. Memoised:
 * a change of the side shown (the mode switch's moment) redraws only what reads it, not every screen's parent.
 */
const Shell = memo(function Shell({ db, rest, registerSW, firstLaunchGuide, onGuideFailed }: { db: GritDb; rest: Rest; registerSW?: RegisterSW; firstLaunchGuide: boolean; onGuideFailed: () => void }) {
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
  // The guide covers the whole app: no tab bar, no rest dock.
  const guide = 'page' in route && route.page === 'guide'
  return (
    <>
      {guide ? (
        <GuideRoute db={db} firstLaunch={firstLaunchGuide} onFailed={onGuideFailed} />
      ) : (
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
            <StorageFullDialog db={db} />
          </div>
          {/* The mode switch animations (1.15), over the whole app; empty at rest. */}
          <ModeSwitchOverlay />
        </>
      )}
      {/*
       * In the same place whether the guide is up or not, so it is never remounted: closing the guide does not replay
       * an earlier rest's flash, and a rest that ends while the guide is up flashes once, over it.
       */}
      <RestFlash showId={rest.flashId} />
    </>
  )
})

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
  // The guide's first-launch gate (1.16), kept here above the lock, so a re-lock while it is up neither closes it nor
  // writes "seen". Shut at once by the tests' seam, with no query; else "seen" is read first, then whether this is a
  // new user (no session, no plan item). Open, it is the `#/guide` route; when the route leaves the guide by any way
  // (Skip, Done, a plan button, the browser's Back), "seen" is written, once, and it stays shut for this page.
  const [gate, setGate] = useState<'deciding' | 'open' | 'shut'>(() => (guideGateOff() ? 'shut' : 'deciding'))
  const route = useRoute()
  const onGuide = 'page' in route && route.page === 'guide'
  const gateEntered = useRef(false)
  useEffect(() => {
    if (db == null || gate !== 'deciding') return
    let stale = false
    ;(async () => (await guideSeen(db)) ? false : await isNewUser(db))()
      .catch(() => false)
      .then((open) => {
        if (stale) return
        if (open) navigate({ page: 'guide' })
        setGate(open ? 'open' : 'shut')
      })
      .catch(() => {})
    return () => {
      stale = true
    }
  }, [db, gate])
  // The guide failed to load (its chunk): shut for this page, and not seen, so a new user is shown it next time.
  const guideFailed = useRef(false)
  const onGuideFailed = useCallback(() => {
    guideFailed.current = true
    setGate((g) => (g === 'open' ? 'shut' : g))
  }, [])
  useEffect(() => {
    if (db == null || gate !== 'open' || guideFailed.current) return
    if (onGuide) {
      gateEntered.current = true
      return
    }
    if (!gateEntered.current) return
    setGate('shut')
    void markGuideSeen(db).catch(() => {})
  }, [db, gate, onGuide])
  // Another tab opened the database with a newer schema: this page's connection is closed for good.
  useEffect(() => onDbVersionChange(() => setOutdated(true)), [])

  if (outdated) return <ReloadScreen />
  if (db == null || privateMode == null || gate === 'deciding' || (gate === 'open' && !onGuide && !gateEntered.current)) return <BlankScreen />
  const continueAnyway = () => {
    acceptPrivateMode()
    setPrivateMode(false)
  }
  return (
    <TrainingModeContext.Provider value={mode}>
      <LockGateHost db={db}>{privateMode ? <PrivateModeScreen db={db} onContinue={continueAnyway} /> : <Shell db={db} rest={rest} registerSW={registerSW} firstLaunchGuide={gate === 'open'} onGuideFailed={onGuideFailed} />}</LockGateHost>
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
    // A log a newer Grit wrote gets its own screen, which never touches the database; recovery would read it.
    <ErrorBoundary fallback={(error) => (error instanceof OlderCopyError ? <OlderCopyScreen /> : <RecoveryScreen error={error} dbName={dbName} />)}>
      <SingleTab>
        <Opened dbName={dbName} schemaVersions={schemaVersions} registerSW={registerSW} />
      </SingleTab>
    </ErrorBoundary>
  )
}
