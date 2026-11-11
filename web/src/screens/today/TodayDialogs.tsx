import { useState } from 'react'
import type { PendingLog } from '../../state/useToday'
import { PrimaryButton, QuietButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { formatKg } from '../../ui/components/format'
import { TextField } from '../../ui/components/TextField'
import './today.css'

export type NumberEntryDialogProps = {
  title: string
  /** Names the field: what the number is ("Kilograms"). */
  label: string
  unit?: string
  initial: number | null
  min?: number
  max: number
  /** Whole numbers only (reps). */
  whole: boolean
  onDismiss: () => void
  onSet: (value: number) => void
}

/** Type a number instead of stepping to it. Opens with the current value selected, so typing replaces it. */
export function NumberEntryDialog({ title, label, unit, initial, min = 0, max, whole, onDismiss, onSet }: NumberEntryDialogProps) {
  const [text, setText] = useState(initial != null ? formatKg(initial) : '')
  const typed = text.trim().replace(',', '.')
  const parsed = /^\d+(\.\d+)?$/.test(typed) ? Number(typed) : null
  const value = parsed != null && parsed >= min && parsed <= max && (!whole || Number.isInteger(parsed)) ? parsed : null
  const range = `${formatKg(min)} to ${formatKg(max)}`
  const error = text.trim() !== '' && value == null ? (whole ? `Enter a whole number from ${range}.` : `Enter a number from ${range}.`) : undefined

  return (
    <Dialog
      open
      title={title}
      onDismiss={onDismiss}
      actions={
        <>
          <QuietButton text="Cancel" onClick={onDismiss} />
          <PrimaryButton text="Set" onClick={() => value != null && onSet(value)} enabled={value != null} compact />
        </>
      }
    >
      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          if (value != null) onSet(value)
        }}
        onFocus={(e) => e.target instanceof HTMLInputElement && e.target.select()}
      >
        <TextField label={label} value={text} onChange={setText} numeric unit={unit} error={error} autoFocus />
      </form>
    </Dialog>
  )
}

export type TypoDialogProps = { pending: PendingLog; onConfirm: () => void; onEdit: () => void }

/** "Did you mean…?" — shown before a set that looks like a typo is saved. */
export function TypoDialog({ pending, onConfirm, onEdit }: TypoDialogProps) {
  const { set, warnings, lastWeightKg } = pending
  return (
    <Dialog
      open
      title={`Did you mean ${formatKg(set.weightKg)} kg × ${set.reps}?`}
      onDismiss={onEdit}
      actions={
        <>
          <QuietButton text="Edit" onClick={onEdit} />
          <PrimaryButton text="Confirm" onClick={onConfirm} compact />
        </>
      }
    >
      <div className="today-dialog-lines">
        {warnings.map((warning) => (
          <p key={warning}>
            {warning === 'HIGH_REPS' ? `${set.reps} reps is a lot for one set.` : lastWeightKg != null ? `That is a big jump from ${formatKg(lastWeightKg)} kg.` : 'That is a big jump.'}
          </p>
        ))}
        <p className="today-muted">Nothing is saved yet.</p>
      </div>
    </Dialog>
  )
}
