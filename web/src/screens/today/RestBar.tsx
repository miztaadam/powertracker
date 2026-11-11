import { useRestCountdown, type RestTimerStore } from '../../platform/restTimer'
import { RestTimerBar } from '../../ui/components/RestTimerBar'

/**
 * The rest bar on its store. The countdown lives here, so the passing seconds redraw this bar and
 * nothing around it.
 */
export function RestBar({ store }: { store: RestTimerStore }) {
  const { remainingSec, totalSec } = useRestCountdown(store)
  return <RestTimerBar remainingSec={remainingSec} totalSec={totalSec} onSubtractThirty={() => store.adjust(-30)} onAddThirty={() => store.adjust(30)} onSkip={() => store.skip()} />
}
