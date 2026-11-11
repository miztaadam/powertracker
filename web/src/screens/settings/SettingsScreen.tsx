import { useEffect, useRef, useState, type ReactNode } from 'react'
import type { AppSettings, GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { clearLock, createLock, lockSupported } from '../../platform/passkeyLock'
import { useSettings, type SettingsActions, type SettingsState } from '../../state/useSettings'
import { dayName } from '../../state/weekdayNames'
import { IconButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { formatRpe } from '../../ui/components/format'
import { ArrowBackIcon } from '../../ui/components/icons'
import { ScreenHeader, SectionHeader } from '../../ui/components/ScreenHeader'
import { ChoiceChip, SegmentedChoice } from '../../ui/components/SegmentedChoice'
import { Snackbar } from '../../ui/components/Snackbar'
import { Stepper } from '../../ui/components/StepperField'
import { Switch } from '../../ui/components/Switch'
import { LOCK_UNSUPPORTED } from '../lock/LockScreen'
import { BackupCard } from './BackupCard'
import './settings.css'

export type SettingsScreenProps = {
  db: GritDb
  onBack: () => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
  /** The real clock in milliseconds. */
  now?: () => number
}

/** How the lifter is told a rest is over. A phone browser cannot vibrate, so Silent only flashes the screen. */
const REST_ALERTS: readonly AppSettings['restAlert'][] = ['SOUND', 'SILENT', 'OFF']
const REST_ALERT_LABEL: Record<AppSettings['restAlert'], string> = { SOUND: 'Sound', SILENT: 'Silent', OFF: 'Off' }
const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7]
const SAVE_FAILED = "Couldn't save — try again."
const clamp = (value: number, low: number, high: number) => Math.min(high, Math.max(low, value))

/** Every setting, then backup and restore. */
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
      <ScreenHeader title="Settings" navigation={<IconButton label="Back" icon={<ArrowBackIcon />} onClick={onBack} />} />
      <div className="screen-body settings-body">
        {!settings.loading && (
          <>
            <SettingsContent settings={settings.settings} onUpdate={(change) => void settings.update(change).catch(() => say(SAVE_FAILED))} onAppLock={setAppLock} />
            <SectionHeader title="Backup" />
            <BackupCard db={db} lastBackupAt={settings.settings.lastBackupAt} storageProtected={settings.storageProtected} safetyCopy={settings.safetyCopy} safetyCopyDate={settings.safetyCopyDate} today={today} now={now} onMessage={say} />
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
}

/** The settings themselves, as grouped cards of rows. */
export function SettingsContent({ settings, onUpdate, onAppLock }: SettingsContentProps) {
  return (
    <>
      <SectionHeader title="Loading and targets" />
      <Group>
        <SwitchRow label="Change plates" supporting="Adds 0.5 and 0.25 kg plates, so loads round to 0.5 kg" checked={settings.changePlates} onChange={(v) => onUpdate((s) => ({ ...s, changePlates: v }))} />
        <StepperRow
          label="Default RPE"
          supporting="Planned effort for main lifts"
          value={formatRpe(settings.defaultRpe)}
          canDecrement={settings.defaultRpe > 6}
          canIncrement={settings.defaultRpe < 10}
          onStep={(by) => onUpdate((s) => ({ ...s, defaultRpe: clamp(s.defaultRpe + by * 0.5, 6, 10) }))}
        />
      </Group>

      <SectionHeader title="Training days" />
      <Card contentPadding={0} className="settings-days">
        <div className="settings-days-row" role="group" aria-label="Training days">
          {WEEKDAYS.map((day) => (
            <ChoiceChip
              key={day}
              text={dayName(day, 'short').slice(0, 3)}
              selected={settings.trainingDays.includes(day)}
              onClick={() => onUpdate((s) => ({ ...s, trainingDays: s.trainingDays.includes(day) ? s.trainingDays.filter((d) => d !== day) : [...s.trainingDays, day].sort((a, b) => a - b) }))}
            />
          ))}
        </div>
      </Card>

      <SectionHeader title="Rest timer" />
      <Group>
        <StepperRow
          label="Main lift rest"
          supporting="Squat, bench, deadlift"
          value={`${settings.restMainSec} s`}
          canDecrement={settings.restMainSec > 60}
          canIncrement={settings.restMainSec < 600}
          onStep={(by) => onUpdate((s) => ({ ...s, restMainSec: clamp(s.restMainSec + by * 30, 60, 600) }))}
        />
        <StepperRow
          label="Accessory rest"
          supporting="Everything else"
          value={`${settings.restAccessorySec} s`}
          canDecrement={settings.restAccessorySec > 30}
          canIncrement={settings.restAccessorySec < 300}
          onStep={(by) => onUpdate((s) => ({ ...s, restAccessorySec: clamp(s.restAccessorySec + by * 15, 30, 300) }))}
        />
        <div className="settings-block">
          <RowLabel label="Rest alert" supporting="How you're told the rest is over" />
          <SegmentedChoice
            label="Rest alert"
            options={REST_ALERTS.map((alert) => REST_ALERT_LABEL[alert])}
            selectedIndex={REST_ALERTS.indexOf(settings.restAlert)}
            onSelect={(i) => onUpdate((s) => ({ ...s, restAlert: REST_ALERTS[i] ?? s.restAlert }))}
          />
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

function Group({ children }: { children: ReactNode }) {
  return (
    <Card contentPadding={0} spacing={0} className="settings-group">
      {children}
    </Card>
  )
}

function RowLabel({ label, supporting }: { label: string; supporting: string }) {
  return (
    <span className="settings-label">
      <span className="title-medium">{label}</span>
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

function StepperRow({ label, supporting, value, canDecrement, canIncrement, onStep }: { label: string; supporting: string; value: string; canDecrement: boolean; canIncrement: boolean; onStep: (by: -1 | 1) => void }) {
  return (
    <div className="settings-row">
      <RowLabel label={label} supporting={supporting} />
      <Stepper label={label} valueText={value} canDecrement={canDecrement} canIncrement={canIncrement} onDecrement={() => onStep(-1)} onIncrement={() => onStep(1)} />
    </div>
  )
}
