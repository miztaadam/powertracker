import { useRef, type CSSProperties, type ReactNode } from 'react'
import type { GritDb } from './db/db'
import { goBack, navigate, useRoute, type Route, type Tab } from './router'
import { CoachScreen } from './screens/coach/CoachScreen'
import { PlanScreen } from './screens/plan/PlanScreen'
import { LiftHistoryScreen } from './screens/progress/LiftHistoryScreen'
import { LIFT_LABEL } from './screens/progress/lifts'
import { ProgressScreen } from './screens/progress/ProgressScreen'
import { SettingsScreen } from './screens/settings/SettingsScreen'
import { RestBar } from './screens/today/RestBar'
import { TodayScreen } from './screens/today/TodayScreen'
import { useGritDb } from './state/useGritDb'
import { useRest } from './state/useRest'
import { RestFlash } from './ui/components/RestFlash'
import { ScreenHeader } from './ui/components/ScreenHeader'
import { TabBar } from './ui/components/TabBar'
import { useElementHeight } from './ui/useElementHeight'
import './ui/shell.css'

const TAB_LABEL: Record<Tab, string> = { today: 'Today', coach: 'Coach', progress: 'Progress', plan: 'Plan' }

function Placeholder({ title, eyebrow }: { title: string; eyebrow: string }) {
  return (
    <section className="screen">
      <ScreenHeader title={title} eyebrow={eyebrow} />
    </section>
  )
}

function screenFor(route: Route, db: GritDb | null, onSetLogged: (isMainOrVariation: boolean) => void): ReactNode {
  if ('tab' in route && route.tab === 'today' && db) return <TodayScreen db={db} onOpenCoach={() => navigate({ tab: 'coach' })} onSetLogged={onSetLogged} onOpenSettings={() => navigate({ page: 'settings' })} />
  if ('tab' in route && route.tab === 'coach' && db) return <CoachScreen db={db} />
  if ('tab' in route && route.tab === 'progress' && db) return <ProgressScreen db={db} onOpenLift={(lift) => navigate({ page: 'lift', lift })} />
  if ('tab' in route && route.tab === 'plan' && db) return <PlanScreen db={db} onOpenSettings={() => navigate({ page: 'settings' })} />
  if ('tab' in route) return <Placeholder title={TAB_LABEL[route.tab]} eyebrow="Grit" />
  if (route.page === 'settings' && db) return <SettingsScreen db={db} onBack={() => goBack({ tab: 'plan' })} />
  if (route.page === 'settings') return <Placeholder title="Settings" eyebrow="Grit" />
  if (db) return <LiftHistoryScreen db={db} lift={route.lift} onBack={() => goBack({ tab: 'progress' })} />
  return <Placeholder title={`${LIFT_LABEL[route.lift]} history`} eyebrow="Grit" />
}

export function App() {
  const route = useRoute()
  const db = useGritDb()
  // The rest lives here, above the screens, so it runs on while the lifter looks at another tab.
  // Only its start and end redraw the shell: the countdown redraws inside RestBar.
  const rest = useRest(db)
  const activeTab = 'tab' in route ? route.tab : null
  // As on Android, the bar is part of Today; the alert at zero shows on every screen.
  const showRestBar = activeTab === 'today' && rest.running
  const restDock = useRef<HTMLDivElement>(null)
  // Messages rise by the bar's real height, so they clear its buttons at any text size.
  const restDockHeight = useElementHeight(restDock, showRestBar)
  return (
    <div className="shell" style={{ '--rest-dock-height': `${restDockHeight}px` } as CSSProperties}>
      <main className="shell-main">{screenFor(route, db, rest.onSetLogged)}</main>
      {showRestBar && (
        <div className="shell-rest" ref={restDock}>
          <RestBar store={rest.store} />
        </div>
      )}
      <TabBar current={activeTab} />
      <RestFlash showId={rest.flashId} />
    </div>
  )
}
