import { useState } from 'react'
import { AdviceCard } from '../components/AdviceCard'
import { BarStrip } from '../components/BarStrip'
import { IconButton, PrimaryButton, QuietButton, SecondaryButton } from '../components/Button'
import { Card } from '../components/Card'
import { CoachSuggestion } from '../components/CoachSuggestion'
import { Dialog } from '../components/Dialog'
import { EmptyState } from '../components/EmptyState'
import * as icons from '../components/icons'
import { LineChart } from '../components/LineChart'
import { Eyebrow, NumberDisplay } from '../components/NumberDisplay'
import { Pill } from '../components/Pill'
import { PlateStrip, PlateStripNote } from '../components/PlateStrip'
import { RatioBar } from '../components/RatioBar'
import { RestTimerBar } from '../components/RestTimerBar'
import { ScreenHeader, SectionHeader } from '../components/ScreenHeader'
import { ChoiceChip, SegmentedChoice } from '../components/SegmentedChoice'
import { SetRow } from '../components/SetRow'
import { Snackbar } from '../components/Snackbar'
import { Sparkline } from '../components/Sparkline'
import { StatTile } from '../components/StatTile'
import { Stepper, StepperField } from '../components/StepperField'
import { Switch } from '../components/Switch'
import { TabBar } from '../components/TabBar'
import { TextField } from '../components/TextField'
import '../shell.css'
import './showcase.css'

// Dev-only: every component in its states, with the same sample story as the Android mockups
// (squat e1RM 190.5, bench 122.5 and stalled, deadlift 250, on Wednesday 11 November).

const SQUAT = [172.5, 175, 175, 177.5, 180, 180, 182.5, 185, 185, 187.5, 187.5, 190.5]
const BENCH = [115, 117.5, 119, 120, 121, 122.5, 122, 122.5, 122.5, 122, 122.5, 122.5]
const DEADLIFT = [232.5, 235, 237.5, 237.5, 240, 242.5, 242.5, 245, 247.5, 247.5, 250, 250]
const TOTAL = SQUAT.map((s, i) => s + BENCH[i]! + DEADLIFT[i]!)
const X_LABELS = ['24 Aug', '', '', '', '', '28 Sep', '', '', '', '', '', '9 Nov']
const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const noop = () => {}

function Accessories({ rows }: { rows: [string, string][] }) {
  return (
    <div className="sc-list">
      {rows.map(([name, dose]) => (
        <div key={name} className="sc-list-row">
          <span className="title-small">{name}</span>
          <span className="headline-small">{dose}</span>
        </div>
      ))}
    </div>
  )
}

function SettingRow({ title, note, control }: { title: string; note: string; control: React.ReactNode }) {
  return (
    <div className="sc-setting">
      <div className="sc-setting-text">
        <span className="title-medium">{title}</span>
        <span className="body-small sc-muted">{note}</span>
      </div>
      {control}
    </div>
  )
}

export default function Showcase() {
  const [miss, setMiss] = useState<number | null>(0)
  const [range, setRange] = useState<number | null>(1)
  const [alert, setAlert] = useState<number | null>(0)
  const [days, setDays] = useState([true, false, true, false, true, false, false])
  const [plates, setPlates] = useState(true)
  const [lock, setLock] = useState(false)
  const [weight, setWeight] = useState(155)
  const [typed, setTyped] = useState('152.5')

  return (
    <div className="sc">
      <ScreenHeader title="Today" eyebrow="Wednesday 11 November" actions={<SecondaryButton text="Finish" onClick={noop} compact />} />
      <div className="screen-body">
        <Card>
          <div className="sc-row">
            <div className="sc-grow">
              <Eyebrow text="Exercise 1 of 4" />
              <h2 className="headline-medium">Squat</h2>
            </div>
            <Pill text="Set 4 of 5" />
          </div>
          <CoachSuggestion headline="155 × 5 @ 8" reason="Last time: 150 × 5 @ 7." onApply={noop} onDismiss={noop} />
          <div>
            <SetRow index={1} weightKg={150} reps={5} rpe={7.5} state="done" />
            <SetRow index={2} weightKg={155} reps={5} rpe={8.5} state="done" isTopSet onClick={noop} />
            <SetRow index={3} weightKg={155} reps={3} rpe={10} state="failed" />
            <div className="sc-miss">
              <span className="body-small sc-muted">Where did it stall?</span>
              <SegmentedChoice label="Where did it stall?" options={['Floor', 'Mid', 'Lockout']} selectedIndex={miss} onSelect={setMiss} />
            </div>
          </div>
          <hr className="sc-hairline" />
          <div className="sc-steppers">
            <StepperField
              className="sc-weight"
              label="Weight"
              valueText={String(weight)}
              unit="kg"
              canDecrement={weight > 0}
              canIncrement
              onDecrement={() => setWeight(weight - 2.5)}
              onIncrement={() => setWeight(weight + 2.5)}
              onValueClick={noop}
            />
            <StepperField label="Reps" valueText="5" canDecrement canIncrement onDecrement={noop} onIncrement={noop} />
            <StepperField label="RPE" valueText="8" canDecrement canIncrement onDecrement={noop} onIncrement={noop} />
          </div>
          <PlateStrip perSide={[25, 25, 15, 2.5]} totalKg={155} />
          <div className="sc-row sc-gap">
            <SecondaryButton text="Missed" onClick={noop} icon={<icons.CloseIcon />} />
            <PrimaryButton className="sc-grow" text="Log set 4" onClick={noop} icon={<icons.CheckIcon />} />
          </div>
        </Card>
        <RestTimerBar remainingSec={108} totalSec={180} onSubtractThirty={noop} onAddThirty={noop} onSkip={noop} />

        <SectionHeader title="Set rows" />
        <Card spacing={0}>
          <SetRow index={1} weightKg={170} reps={3} rpe={9} state="done" isTopSet isPr />
          <SetRow index={2} weightKg={142.5} reps={3} rpe={7.5} state="done" />
          <SetRow index={3} weightKg={155} reps={3} rpe={10} state="failed" />
          <SetRow index={4} weightKg={150} reps={5} rpe={8} state="planned" />
          <SetRow index={5} weightKg={150} reps={5} rpe={null} state="planned" />
        </Card>

        <SectionHeader title="Plates" />
        <PlateStrip perSide={[25, 25, 25, 5, 1.25]} totalKg={182.5} />
        <PlateStrip perSide={[20, 10, 5, 2.5, 1.25, 0.5, 0.25]} totalKg={98.5} />
        <PlateStrip perSide={[]} totalKg={20} />
        <PlateStrip perSide={null} totalKg={181} nearestKg={180} />
        <PlateStripNote title="No weight yet" caption="Tap the number to type it in." />
      </div>

      <ScreenHeader title="Coach" eyebrow="3 open suggestions" />
      <div className="screen-body">
        <AdviceCard
          tone="danger"
          icon={<icons.BatteryAlertIcon />}
          tag="Deload · All lifts"
          title="Take a deload week"
          body="Signs of fatigue: RPE rising at the same load on squat and deadlift; 3 failed reps. Take a deload week: about 60% of the volume at about 90% of the load."
          primaryAction="Apply deload"
          secondaryAction="Dismiss"
        >
          <div className="sc-row sc-gap">
            <StatTile label="Volume" value="60" unit="%" note="of your usual sets" />
            <StatTile label="Load" value="90" unit="%" note="of your last top sets" />
          </div>
        </AdviceCard>
        <AdviceCard
          tone="warning"
          icon={<icons.TrendingFlatIcon />}
          tag="Stall · Bench"
          title="Bench has stalled"
          body="Bench e1RM hasn't improved for 3 weeks. You've mostly missed at lockout — this works the triceps."
          primaryAction="Add to plan"
          secondaryAction="Dismiss"
        >
          <div className="sc-row sc-gap sc-end">
            <NumberDisplay value="122.5" unit="kg" label="Bench e1RM" size="medium" />
            <Sparkline className="sc-spark" values={BENCH.slice(-6)} color="var(--chart-bench)" minSpan={122.5 * 0.05} />
          </div>
          <Accessories
            rows={[
              ['Close-grip bench', '3 × 3–5'],
              ['JM press', '3 × 8–10'],
              ['Board press', '3 × 3–5'],
            ]}
          />
        </AdviceCard>
        <AdviceCard
          tone="info"
          icon={<icons.BalanceIcon />}
          tag="Weak point · Squat"
          title="Squat is lagging your deadlift"
          body="Your squat is low relative to your deadlift (76% vs a typical 80–90%). Prioritise squat."
          primaryAction="Prioritise squat"
          secondaryAction="Dismiss"
        >
          <RatioBar label="Squat ÷ Deadlift" valuePct={76} lowPct={80} highPct={90} />
        </AdviceCard>
        <AdviceCard tone="accent" icon={<icons.BoltIcon />} tag="Target · Squat" title="155 × 5 @ 8 next time" body="Last time you hit 150 × 5 @ 7." />
        <EmptyState icon={<icons.BoltIcon />} title="No advice right now. Keep logging." body="The coach looks at your log after each session." action={<PrimaryButton text="Start a quick session" onClick={noop} />} />
      </div>

      <ScreenHeader title="Progress" eyebrow="Estimated 1RM" />
      <div className="screen-body">
        <Card>
          <div className="sc-row sc-gap sc-end">
            <div>
              <NumberDisplay value="563" unit="kg" label="Total" size="hero" />
              <Pill text="+13 kg in 4 weeks" tone="success" icon={<icons.TrendingUpIcon />} />
            </div>
            <Sparkline className="sc-spark sc-spark-tall" values={TOTAL} color="var(--accent-text)" />
          </div>
          <div className="sc-row sc-gap-s">
            <StatTile label="Squat" value="190.5" unit="kg" note="+5.5 kg" noteTone="success" onClick={noop} onClickLabel="Open squat history" />
            <StatTile label="Bench" value="122.5" unit="kg" note="No change" onClick={noop} onClickLabel="Open bench history" />
            <StatTile label="Deadlift" value="250" unit="kg" note="+7.5 kg" noteTone="success" onClick={noop} onClickLabel="Open deadlift history" />
          </div>
        </Card>
        <Card>
          <SegmentedChoice label="Chart range" options={['8 wk', '6 mo', '1 yr', 'All']} selectedIndex={range} onSelect={setRange} />
          <Eyebrow text="Weekly best e1RM" />
          <LineChart
            series={[
              { name: 'Squat', color: 'var(--chart-squat)', values: SQUAT },
              { name: 'Bench', color: 'var(--chart-bench)', values: BENCH },
              { name: 'Deadlift', color: 'var(--chart-deadlift)', values: DEADLIFT },
            ]}
            xLabels={X_LABELS}
          />
        </Card>
        <SectionHeader title="Weekly volume" />
        <Card>
          <div className="sc-row sc-gap sc-end">
            <NumberDisplay className="sc-volume" value="3,310" unit="kg" label="Squat · this week" size="medium" />
            <BarStrip className="sc-bars" values={[2400, 3000, 2500, 3300, 2600, 3200, 2600, 3310]} color="var(--chart-squat)" />
          </div>
          <div className="sc-row sc-gap sc-end">
            <NumberDisplay className="sc-volume" value="0" unit="kg" label="Deadlift · this week" size="medium" />
            <BarStrip className="sc-bars" values={[2300, 3100, 2300, 3100, 2500, 3200, 2300, 0]} color="var(--chart-deadlift)" />
          </div>
        </Card>
        <SectionHeader title="Lift ratios" />
        <Card>
          <RatioBar label="Squat ÷ Deadlift" valuePct={76} lowPct={80} highPct={90} />
          <RatioBar label="Bench ÷ Squat" valuePct={64} lowPct={60} highPct={70} />
          <RatioBar label="Bench ÷ Deadlift" valuePct={71} lowPct={45} highPct={55} />
        </Card>
      </div>

      <ScreenHeader title="Settings" navigation={<IconButton label="Back" icon={<icons.ArrowBackIcon />} onClick={noop} />} actions={<IconButton label="Settings" icon={<icons.SettingsIcon />} onClick={noop} />} />
      <div className="screen-body">
        <SectionHeader title="Loading and targets" action={<QuietButton text="Reset" onClick={noop} />} />
        <Card>
          <SettingRow title="Change plates" note="Adds 0.5 and 0.25 kg plates, so loads round to 0.5 kg" control={<Switch label="Change plates" checked={plates} onChange={setPlates} />} />
          <hr className="sc-hairline" />
          <SettingRow title="Default RPE" note="Planned effort for main lifts" control={<Stepper label="Default RPE" valueText="8" canDecrement canIncrement onDecrement={noop} onIncrement={noop} />} />
          <hr className="sc-hairline" />
          <SettingRow title="Sets" note="At its lowest value" control={<Stepper label="Sets" valueText="1" canDecrement={false} canIncrement onDecrement={noop} onIncrement={noop} />} />
          <hr className="sc-hairline" />
          <SettingRow title="App lock" note="Ask for Face ID when Grit opens" control={<Switch label="App lock" checked={lock} onChange={setLock} />} />
        </Card>
        <Card contentPadding={8}>
          <div className="sc-chips">
            {DAYS.map((d, i) => (
              <ChoiceChip key={d} text={d} selected={days[i]!} onClick={() => setDays(days.map((on, j) => (j === i ? !on : on)))} />
            ))}
          </div>
        </Card>
        <Card>
          <SegmentedChoice label="Rest alert" options={['Vibrate only', 'Sound', 'Off']} selectedIndex={alert} onSelect={setAlert} />
          <TextField label="Weight" value={typed} onChange={setTyped} numeric unit="kg" />
          <TextField label="Squat best" value="900" onChange={noop} numeric unit="kg" error="Enter a weight up to 500 kg." />
          <TextField label="Exercise name" value="" onChange={noop} placeholder="e.g. Pause squat" />
          <SecondaryButton text="Export backup" onClick={noop} icon={<icons.UploadIcon />} />
          <SecondaryButton text="Restore backup" onClick={noop} icon={<icons.DownloadIcon />} />
        </Card>

        <SectionHeader title="Buttons" />
        <Card>
          <PrimaryButton text="Start session" onClick={noop} icon={<icons.PlayArrowIcon />} />
          <PrimaryButton text="Log set 4" onClick={noop} enabled={false} icon={<icons.CheckIcon />} />
          <div className="sc-row sc-gap-s sc-wrap">
            <PrimaryButton text="Apply" onClick={noop} compact />
            <SecondaryButton text="Add" onClick={noop} compact icon={<icons.AddIcon />} />
            <SecondaryButton text="Missed" onClick={noop} compact enabled={false} />
            <QuietButton text="Dismiss" onClick={noop} />
            <QuietButton text="Skip" onClick={noop} enabled={false} />
          </div>
          <div className="sc-row sc-gap-s sc-wrap">
            <Pill text="PR" tone="success" icon={<icons.EmojiEventsIcon />} />
            <Pill text="Stall" tone="warning" />
            <Pill text="Failed" tone="danger" />
            <Pill text="Weak point" tone="info" />
            <Pill text="Target" tone="accent" />
            <Pill text="Set 4 of 5" />
          </div>
        </Card>

        <SectionHeader title="Dialog and snackbar" />
        <div className="sc-static">
          <Dialog
            open
            title="Replace all data?"
            onDismiss={noop}
            isolate={false}
            actions={
              <>
                <QuietButton text="Cancel" onClick={noop} />
                <PrimaryButton text="Replace" onClick={noop} compact />
              </>
            }
          >
            Restoring replaces everything in Grit with the contents of this backup.
          </Dialog>
          <Snackbar message="Backup saved." />
          <Snackbar message="Update ready — tap to refresh" actionLabel="Refresh" onAction={noop} />
        </div>

        <SectionHeader title="Icons" />
        <Card>
          <div className="sc-icons">
            {Object.entries(icons).map(([name, Icon]) => (
              <span key={name} title={name}>
                <Icon />
              </span>
            ))}
          </div>
        </Card>
      </div>
      <TabBar current="today" />
    </div>
  )
}
