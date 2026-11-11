import type { ReactNode } from 'react'
import type { GritDb } from './db/db'
import { navigate, useRoute, type Route, type Tab } from './router'
import { CoachScreen } from './screens/coach/CoachScreen'
import { TodayScreen } from './screens/today/TodayScreen'
import { useGritDb } from './state/useGritDb'
import { ScreenHeader } from './ui/components/ScreenHeader'
import { TabBar } from './ui/components/TabBar'
import './ui/shell.css'

const TAB_LABEL: Record<Tab, string> = { today: 'Today', coach: 'Coach', progress: 'Progress', plan: 'Plan' }
const LIFT_LABEL = { SQUAT: 'Squat', BENCH: 'Bench', DEADLIFT: 'Deadlift' } as const

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
  if ('tab' in route) return <Placeholder title={TAB_LABEL[route.tab]} eyebrow="Grit" />
  if (route.page === 'settings') return <Placeholder title="Settings" eyebrow="Grit" />
  return <Placeholder title={LIFT_LABEL[route.lift]} eyebrow="Lift history" />
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
