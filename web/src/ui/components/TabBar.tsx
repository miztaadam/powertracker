import { TABS, routeToHash, type Tab } from '../../router'
import { BoltIcon, CalendarMonthIcon, FitnessCenterIcon, InsightsIcon, type IconComponent } from './icons'
import './TabBar.css'

export type TabBarProps = {
  /** The tab on screen, or null on a page outside the tabs (settings, lift history). */
  current: Tab | null
}

const TAB: Record<Tab, { label: string; Icon: IconComponent }> = {
  today: { label: 'Today', Icon: FitnessCenterIcon },
  coach: { label: 'Coach', Icon: BoltIcon },
  progress: { label: 'Progress', Icon: InsightsIcon },
  plan: { label: 'Plan', Icon: CalendarMonthIcon },
}

/** The four-tab bottom bar. The current tab gets the volt indicator; everything else stays quiet. */
export function TabBar({ current }: TabBarProps) {
  return (
    <nav className="gr-tabbar" aria-label="Main">
      {TABS.map((tab) => {
        const { label, Icon } = TAB[tab]
        return (
          <a key={tab} href={routeToHash({ tab })} className="gr-tab label-medium" aria-current={current === tab ? 'page' : undefined}>
            <span className="gr-tab-indicator">
              <Icon />
            </span>
            {label}
          </a>
        )
      })}
    </nav>
  )
}
