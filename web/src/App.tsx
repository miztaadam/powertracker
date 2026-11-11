import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import { onDbVersionChange, type GritDb } from './db/db'
import type { SchemaVersion } from './db/schema'
import { singleTabGuard, type SingleTabGuard, type TabRole } from './platform/singleTab'
import { acceptPrivateMode, detectPrivateMode, privateModeAccepted, requestPersistence } from './platform/storage'
import { UPDATE_READY, useUpdatePrompt, type RegisterSW } from './platform/update'
import { goBack, navigate, useRoute, type Route } from './router'
import { CoachScreen } from './screens/coach/CoachScreen'
import { InstallCard } from './screens/install/InstallCard'
import { BlankScreen } from './screens/lock/GateScreen'
import { LockGateHost } from './screens/lock/LockGateHost'
import { PlanScreen } from './screens/plan/PlanScreen'
import { LiftHistoryScreen } from './screens/progress/LiftHistoryScreen'
import { ProgressScreen } from './screens/progress/ProgressScreen'
import { ErrorBoundary } from './screens/recovery/ErrorBoundary'
import { OtherTabScreen, ReloadScreen } from './screens/recovery/OtherTabScreen'
import { PrivateModeScreen } from './screens/recovery/PrivateModeScreen'
import { RecoveryScreen } from './screens/recovery/RecoveryScreen'
import { StorageFullDialog } from './screens/recovery/StorageFullDialog'
import { SettingsScreen } from './screens/settings/SettingsScreen'
import { RestBar } from './screens/today/RestBar'
import { TodayScreen } from './screens/today/TodayScreen'
import { useGritDb } from './state/useGritDb'
import { useRest, type Rest } from './state/useRest'
import { RestFlash } from './ui/components/RestFlash'
import { TabBar } from './ui/components/TabBar'
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

function screenFor(route: Route, db: GritDb, onSetLogged: (isMainOrVariation: boolean) => void): ReactNode {
  const openSettings = () => navigate({ page: 'settings' })
  if ('tab' in route) {
    if (route.tab === 'today') return <TodayScreen db={db} onOpenCoach={() => navigate({ tab: 'coach' })} onSetLogged={onSetLogged} onOpenSettings={openSettings} banner={<InstallCard db={db} />} />
    if (route.tab === 'coach') return <CoachScreen db={db} />
    if (route.tab === 'progress') return <ProgressScreen db={db} onOpenLift={(lift) => navigate({ page: 'lift', lift })} />
    return <PlanScreen db={db} onOpenSettings={openSettings} />
  }
  if (route.page === 'settings') return <SettingsScreen db={db} onBack={() => goBack({ tab: 'plan' })} />
  return <LiftHistoryScreen db={db} lift={route.lift} onBack={() => goBack({ tab: 'progress' })} />
}

/** The app itself: the screens, the tab bar and what docks above it. Rendered only while Grit is unlocked. */
function Shell({ db, rest, registerSW }: { db: GritDb; rest: Rest; registerSW?: RegisterSW }) {
  const route = useRoute()
  const update = useUpdatePrompt(db, registerSW)
  const activeTab = 'tab' in route ? route.tab : null
  // As on Android, the bar is part of Today; the alert at zero shows on every screen.
  const showRestBar = activeTab === 'today' && rest.running
  const showDock = showRestBar || update.ready
  const dock = useRef<HTMLDivElement>(null)
  // Messages rise by the dock's real height, so they clear its buttons at any text size.
  const dockHeight = useElementHeight(dock, showDock)
  return (
    <div className="shell" style={{ '--rest-dock-height': `${dockHeight}px` } as CSSProperties}>
      <main className="shell-main">{screenFor(route, db, rest.onSetLogged)}</main>
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
  )
}

/** Opens the database, then shows the app behind its lock. Nothing of the log is rendered before the lock has had its say. */
function Opened({ dbName, schemaVersions, registerSW }: AppProps) {
  const db = useGritDb(dbName, schemaVersions)
  // The rest lives here, above the lock and the screens, so it runs on while the lifter looks at
  // another tab or unlocks. Only its start and end redraw: the countdown redraws inside RestBar.
  const rest = useRest(db)
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
  return <LockGateHost db={db}>{privateMode ? <PrivateModeScreen db={db} onContinue={continueAnyway} /> : <Shell db={db} rest={rest} registerSW={registerSW} />}</LockGateHost>
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
