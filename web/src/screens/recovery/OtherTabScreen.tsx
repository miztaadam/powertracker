import { PrimaryButton } from '../../ui/components/Button'
import { GateScreen } from '../lock/GateScreen'

/** Shown in a second tab: Grit runs in one tab at a time, so two copies never write the same log. */
export function OtherTabScreen({ onUseHere }: { onUseHere?: () => void }) {
  return (
    <GateScreen title="Grit is open in another tab" body="Grit runs in one tab at a time, so your log stays in one piece. Go back to the other tab, or use Grit here.">
      {onUseHere && <PrimaryButton text="Use Grit here" onClick={onUseHere} />}
    </GateScreen>
  )
}

/** Shown when another tab moved the database to a newer version: this page's copy of Grit is too old to go on. */
export function ReloadScreen() {
  return (
    <GateScreen title="Grit was updated" body="A newer version of Grit is open in another tab. Reload to carry on here.">
      <PrimaryButton text="Reload" onClick={() => window.location.reload()} />
    </GateScreen>
  )
}
