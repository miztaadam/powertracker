import type { ReactNode } from 'react'
import type { GritDb } from './db/db'
import { goBack, navigate, useRoute, type Route, type Tab } from './router'
import { CoachScreen } from './screens/coach/CoachScreen'
import { LiftHistoryScreen } from './screens/progress/LiftHistoryScreen'
import { LIFT_LABEL } from './screens/progress/lifts'
import { ProgressScreen } from './screens/progress/ProgressScreen'
import { TodayScreen } from './screens/today/TodayScreen'
import { useGritDb } from './state/useGritDb'
import { ScreenHeader } from './ui/components/ScreenHeader'
import { TabBar } from './ui/components/TabBar'
import './ui/shell.css'

const TAB_LABEL: Record<Tab, string> = { today: 'Today', coach: 'Coach', progress: 'Progress', plan: 'Plan' }

function Placeholder({ title, eyebrow }: { title: string; eyebrow: string }) {
  return (
    <section className="screen">
      <ScreenHeader title={title} eyebrow={eyebrow} />
    </section>
  )
}

function screenFor(route: Route, db: GritDb | null): ReactNode {
  if ('tab' in route && route.tab === 'today' && db) return <TodayScreen db={db} onOpenCoach={() => navigate({ tab: 'coach' })} />
  if ('tab' in route && route.tab === 'coach' && db) return <CoachScreen db={db} />
  if ('tab' in route && route.tab === 'progress' && db) return <ProgressScreen db={db} onOpenLift={(lift) => navigate({ page: 'lift', lift })} />
  if ('tab' in route) return <Placeholder title={TAB_LABEL[route.tab]} eyebrow="Grit" />
  if (route.page === 'settings') return <Placeholder title="Settings" eyebrow="Grit" />
  if (db) return <LiftHistoryScreen db={db} lift={route.lift} onBack={() => goBack({ tab: 'progress' })} />
  return <Placeholder title={`${LIFT_LABEL[route.lift]} history`} eyebrow="Grit" />
}

export function App() {
  const route = useRoute()
  const db = useGritDb()
  const activeTab = 'tab' in route ? route.tab : null
  return (
    <div className="shell">
      <main className="shell-main">{screenFor(route, db)}</main>
      <TabBar current={activeTab} />
    </div>
  )
}
