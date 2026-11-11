import type { ReactNode } from 'react'
import { TABS, routeToHash, useRoute, type Route, type Tab } from './router'
import './ui/shell.css'

const TAB_LABEL: Record<Tab, string> = { today: 'Today', coach: 'Coach', progress: 'Progress', plan: 'Plan' }
const LIFT_LABEL = { SQUAT: 'Squat', BENCH: 'Bench', DEADLIFT: 'Deadlift' } as const

function Placeholder({ title, eyebrow }: { title: string; eyebrow: string }) {
  return (
    <section className="screen">
      <p className="eyebrow">{eyebrow}</p>
      <h1 className="headline-large">{title}</h1>
    </section>
  )
}

function screenFor(route: Route): ReactNode {
  if ('tab' in route) return <Placeholder title={TAB_LABEL[route.tab]} eyebrow="Grit" />
  if (route.page === 'settings') return <Placeholder title="Settings" eyebrow="Grit" />
  return <Placeholder title={LIFT_LABEL[route.lift]} eyebrow="Lift history" />
}

export function App() {
  const route = useRoute()
  const activeTab = 'tab' in route ? route.tab : null
  return (
    <div className="shell">
      <main className="shell-main">{screenFor(route)}</main>
      <nav className="tabbar" aria-label="Main">
        {TABS.map((tab) => (
          <a
            key={tab}
            href={routeToHash({ tab })}
            className="tab label-medium"
            aria-current={activeTab === tab ? 'page' : undefined}
          >
            {TAB_LABEL[tab]}
          </a>
        ))}
      </nav>
    </div>
  )
}
