import { useState } from 'react'
import { IconButton } from '../../ui/components/Button'
import { InfoIcon } from '../../ui/components/icons'
import { FormGuideSheet } from './FormGuideSheet'

export type InfoButtonProps = { name: string; className?: string }

/**
 * The info button beside an exercise's name: opens its form guide and does nothing else. It goes
 * wherever an exercise is shown, custom ones included, so rows line up; the sheet says when there
 * is no guide. In a row that is a button, put it beside that button, not inside it.
 */
export function InfoButton({ name, className }: InfoButtonProps) {
  const [open, setOpen] = useState(false)
  return (
    // Taps stop here: the button sits in rows that are tapped themselves, and although the sheet is
    // drawn at the end of the page, React still sends its events up through this place.
    <span className="lib-contents" onClick={(e) => e.stopPropagation()}>
      <IconButton label={`Form guide for ${name}`} icon={<InfoIcon />} onClick={() => setOpen(true)} className={className} />
      {open && <FormGuideSheet name={name} onClose={() => setOpen(false)} />}
    </span>
  )
}
