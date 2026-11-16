import { useEffect, useRef, useState, type ReactNode } from 'react'
import { activeTrainingDays, setTrainingMode, withTrainingDayToggled, type AppSettings, type GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { canVibrate, isIos } from '../../platform/device'
import { clearLock, createLock, lockSupported } from '../../platform/passkeyLock'
import { useSettings, type SettingsActions, type SettingsState } from '../../state/useSettings'
import { dayName } from '../../state/weekdayNames'
import { IconButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { formatRpe } from '../../ui/components/format'
import { ArrowBackIcon } from '../../ui/components/icons'
import { ScreenHeader, SectionHeader } from '../../ui/components/ScreenHeader'
import { ModeLabel } from '../../ui/components/ModeLabel'
import { ChoiceChip, SegmentedChoice } from '../../ui/components/SegmentedChoice'
import { Snackbar } from '../../ui/components/Snackbar'
import { Stepper } from '../../ui/components/StepperField'
import { Switch } from '../../ui/components/Switch'
import { RpeInfoButton } from '../library/RpeSheet'
import { LOCK_UNSUPPORTED } from '../lock/LockScreen'
import { appVersionLine, appVersionSpoken } from './appVersion'
import { BackupCard } from './BackupCard'
import { APP_VERSION, BUILD_ID } from './build'
import './settings.css'

export type SettingsScreenProps = {
  db: GritDb
  onBack: () => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
  /** The real clock in milliseconds. */
  now?: () => number
}

/**
 * How the lifter is told a rest is over. Android browsers can vibrate, so there Sound + vibrate is offered too;
 * iPhones give web apps no vibration at all, so there it is not, and Silent only flashes the screen everywhere.
 */
const REST_ALERTS: readonly AppSettings['restAlert'][] = ['SOUND', 'SILENT', 'OFF']
const REST_ALERTS_WITH_VIBRATION: readonly AppSettings['restAlert'][] = ['SOUND', 'SOUND_AND_VIBRATE', 'SILENT', 'OFF']
const REST_ALERT_LABEL: Record<AppSettings['restAlert'], string> = { SOUND: 'Sound', SOUND_AND_VIBRATE: 'Sound + vibrate', SILENT: 'Silent', OFF: 'Off' }
export const NO_VIBRATION_ON_IPHONE = "iPhones don't let web apps vibrate."
const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7]
const SAVE_FAILED = "Couldn't save — try again."
const clamp = (value: number, low: number, high: number) => Math.min(high, Math.max(low, value))

/** Every setting, then backup and restore, then the version line. */
export function SettingsScreen({ db, onBack, today = todayIso, now = Date.now }: SettingsScreenProps) {
  const settings = useSettings(db)
  // The newest message for the snackbar; `id` changes with each one, also when the text repeats.
  const [message, setMessage] = useState<{ id: number; text: string } | null>(null)
  const say = (text: string) => setMessage((last) => ({ id: (last?.id ?? 0) + 1, text }))
  // Turning App lock on makes this phone's passkey first, so the setting is never on without one;
  // closing the Face ID sheet, or a browser that has none, leaves it off. One change at a time.
  const lockBusy = useRef(false)
  // Asked ahead of time, so that the tap itself goes straight to the Face ID sheet (Safari shows it only then).
  const lockSupport = useRef<boolean | null>(null)
  useEffect(() => {
    lockSupported()
      .then((value) => {
        lockSupport.current = value
      })
      .catch(() => {})
  }, [])
  const setAppLock = (on: boolean) => {
    if (lockBusy.current) return
    lockBusy.current = true
    void (async () => {
      if (!on) {
        await settings.update((s) => ({ ...s, appLock: false }))
        return clearLock(db)
      }
      const result = await createLock(db, lockSupport.current)
      if (result === 'ok') await settings.update((s) => ({ ...s, appLock: true }))
      else say(result === 'unsupported' ? `${LOCK_UNSUPPORTED}, so App lock stays off.` : 'App lock stays off.')
    })()
      .catch(() => say(SAVE_FAILED))
      .finally(() => {
        lockBusy.current = false
      })
  }
  return (
    <section className="screen">
      <ScreenHeader title="Settings" label={<ModeLabel />} navigation={<IconButton label="Back" icon={<ArrowBackIcon />} onClick={onBack} />} />
      <div className="screen-body settings-body">
        {!settings.loading && (
          <>
            <SettingsContent
              settings={settings.settings}
              onUpdate={(change) => void settings.update(change).catch(() => say(SAVE_FAILED))}
              onAppLock={setAppLock}
              onBodybuilding={(on) => void setTrainingMode(db, on ? 'BODYBUILDING' : 'POWERLIFTING', today()).catch(() => say(SAVE_FAILED))}
            />
            <SectionHeader title="Backup" />
            <BackupCard db={db} lastBackupAt={settings.settings.lastBackupAt} storageProtected={settings.storageProtected} safetyCopy={settings.safetyCopy} safetyCopyDate={settings.safetyCopyDate} today={today} now={now} onMessage={say} />
            <VersionLine />
          </>
        )}
      </div>
      <Snackbar message={message?.text ?? null} showId={message?.id} onDismiss={() => setMessage(null)} />
    </section>
  )
}

export type SettingsContentProps = {
  settings: SettingsState['settings']
  onUpdate: (change: Parameters<SettingsActions['update']>[0]) => void
  /** App lock is not just stored: turning it on makes a passkey. */
  onAppLock: (on: boolean) => void
  /** Bodybuilding mode is not just stored: switching sides dates the side left (`setTrainingMode`). */
  onBodybuilding: (on: boolean) => void
}

/** The settings themselves, as grouped cards of rows. */
export function SettingsContent({ settings, onUpdate, onAppLock, onBodybuilding }: SettingsContentProps) {
  // The bodybuilding side has no main lifts: it hides the powerlifting rows (planned effort for main lifts, strict
  // press on Progress, main lift rest) and its one rest is every exercise's. Their stored values are kept as they are.
  const powerlifting = settings.trainingMode !== 'BODYBUILDING'
  const vibrates = canVibrate()
  const restAlerts = vibrates ? REST_ALERTS_WITH_VIBRATION : REST_ALERTS
  // Sound + vibrate restored onto a phone that cannot vibrate is the sound alone.
  const restAlert = settings.restAlert === 'SOUND_AND_VIBRATE' && !vibrates ? 'SOUND' : settings.restAlert
  return (
    <>
      <SectionHeader title="Training style" />
      <Group>
        <SwitchRow label="Bodybuilding mode" supporting="Rep ranges, a physique coach and a blue look. Your powerlifting plan is kept." checked={settings.trainingMode === 'BODYBUILDING'} onChange={onBodybuilding} />
      </Group>

      <SectionHeader title="Loading and targets" />
      <Group>
        <SwitchRow label="Change plates" supporting="Adds 0.5 and 0.25 kg plates, so loads round to 0.5 kg" checked={settings.changePlates} onChange={(v) => onUpdate((s) => ({ ...s, changePlates: v }))} />
        {powerlifting && (
          <StepperRow
            label="Default RPE"
            supporting="Planned effort for main lifts"
            value={formatRpe(settings.defaultRpe)}
            canDecrement={settings.defaultRpe > 6}
            canIncrement={settings.defaultRpe < 10}
            onStep={(by) => onUpdate((s) => ({ ...s, defaultRpe: clamp(s.defaultRpe + by * 0.5, 6, 10) }))}
            labelAction={<RpeInfoButton inline />}
          />
        )}
      </Group>

      {powerlifting && (
        <>
          <SectionHeader title="Progress" />
          <Group>
            <SwitchRow label="Strict press" supporting="Show it on Progress and add it to your total." checked={settings.showStrictPress} onChange={(v) => onUpdate((s) => ({ ...s, showStrictPress: v }))} />
          </Group>
        </>
      )}

      <SectionHeader title="Training days" />
      <Card contentPadding={0} className="settings-days">
        <div className="settings-days-row" role="group" aria-label="Training days">
          {WEEKDAYS.map((day) => (
            <ChoiceChip
              key={day}
              text={dayName(day, 'short').slice(0, 3)}
              selected={activeTrainingDays(settings).includes(day)}
              onClick={() => onUpdate((s) => withTrainingDayToggled(s, day))}
            />
          ))}
        </div>
      </Card>

      <SectionHeader title="Rest timer" />
      <Group>
        {powerlifting && (
          <StepperRow
            label="Main lift rest"
            supporting="Squat, bench, deadlift"
            value={`${settings.restMainSec} s`}
            canDecrement={settings.restMainSec > 60}
            canIncrement={settings.restMainSec < 600}
            onStep={(by) => onUpdate((s) => ({ ...s, restMainSec: clamp(s.restMainSec + by * 30, 60, 600) }))}
          />
        )}
        {/* Every exercise on the bodybuilding side rests as an accessory does (spec §5.3). */}
        <StepperRow
          label={powerlifting ? 'Accessory rest' : 'Rest'}
          supporting={powerlifting ? 'Everything else' : 'Every exercise'}
          value={`${settings.restAccessorySec} s`}
          canDecrement={settings.restAccessorySec > 30}
          canIncrement={settings.restAccessorySec < 300}
          onStep={(by) => onUpdate((s) => ({ ...s, restAccessorySec: clamp(s.restAccessorySec + by * 15, 30, 300) }))}
        />
        <div className="settings-block">
          <RowLabel label="Rest alert" supporting="How you're told the rest is over" />
          <SegmentedChoice
            label="Rest alert"
            className="settings-rest-alert"
            options={restAlerts.map((alert) => REST_ALERT_LABEL[alert])}
            selectedIndex={restAlerts.indexOf(restAlert)}
            onSelect={(i) => onUpdate((s) => ({ ...s, restAlert: restAlerts[i] ?? s.restAlert }))}
          />
          {!vibrates && isIos() && <p className="settings-supporting settings-note body-small">{NO_VIBRATION_ON_IPHONE}</p>}
        </div>
        <SwitchRow label="Keep screen awake during rests" supporting="The screen stays on while the rest timer runs" checked={settings.keepAwake} onChange={(v) => onUpdate((s) => ({ ...s, keepAwake: v }))} />
      </Group>

      <SectionHeader title="Privacy" />
      <Group>
        <SwitchRow label="App lock" supporting="Ask for Face ID, Touch ID or your passcode when Grit opens, and after a minute away" checked={settings.appLock} onChange={onAppLock} />
      </Group>
    </>
  )
}

/**
 * Last of all: which version this is, which build, and who made it. Plain text; a screen reader hears
 * "Grit version 1.1, build a1b2c3d, by AppsbyAdam" once instead of the middle dots.
 */
function VersionLine() {
  return (
    <p className="settings-version body-small">
      <span aria-hidden="true">{appVersionLine(APP_VERSION, BUILD_ID)}</span>
      <span className="sr-only">{appVersionSpoken(APP_VERSION, BUILD_ID)}</span>
    </p>
  )
}

function Group({ children }: { children: ReactNode }) {
  return (
    <Card contentPadding={0} spacing={0} className="settings-group">
      {children}
    </Card>
  )
}

/** `labelAction` goes right of the label, on its line (the RPE info button). */
function RowLabel({ label, supporting, labelAction }: { label: string; supporting: string; labelAction?: ReactNode }) {
  return (
    <span className="settings-label">
      {labelAction != null ? (
        <span className="settings-title">
          <span className="title-medium">{label}</span>
          {labelAction}
        </span>
      ) : (
        <span className="title-medium">{label}</span>
      )}
      <span className="settings-supporting body-small">{supporting}</span>
    </span>
  )
}

/** The whole row toggles the switch, as on Android. */
function SwitchRow({ label, supporting, checked, onChange }: { label: string; supporting: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="settings-row">
      <RowLabel label={label} supporting={supporting} />
      <Switch label={label} checked={checked} onChange={onChange} />
    </label>
  )
}

function StepperRow({
  label,
  supporting,
  value,
  canDecrement,
  canIncrement,
  onStep,
  labelAction,
}: {
  label: string
  supporting: string
  value: string
  canDecrement: boolean
  canIncrement: boolean
  onStep: (by: -1 | 1) => void
  labelAction?: ReactNode
}) {
  return (
    <div className="settings-row">
      <RowLabel label={label} supporting={supporting} labelAction={labelAction} />
      <Stepper label={label} valueText={value} canDecrement={canDecrement} canIncrement={canIncrement} onDecrement={() => onStep(-1)} onIncrement={() => onStep(1)} />
    </div>
  )
}
