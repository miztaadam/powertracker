import { useState } from 'react'
import { grit } from '../../domain'
import { IconButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { cx } from '../../ui/components/format'
import { CloseIcon, InfoIcon } from '../../ui/components/icons'
import './library.css'

/** What the info button is called, for a screen reader. */
export const RPE_INFO_LABEL = 'What RPE means'

export type RpeInfoButtonProps = {
  /**
   * Drawn small, taking only the room of its icon and no height, so it can sit in a caption or a row
   * title without moving anything. Its 48 px target is centred on the line it is in and reaches over
   * what is around it.
   */
  inline?: boolean
  className?: string
}

/** The info button beside an RPE label: opens `RpeSheet` and does nothing else. A full 48 px target wherever it is. */
export function RpeInfoButton({ inline = false, className }: RpeInfoButtonProps) {
  const [open, setOpen] = useState(false)
  const button = <IconButton label={RPE_INFO_LABEL} icon={<InfoIcon size={inline ? 18 : 24} />} onClick={() => setOpen(true)} className={inline ? undefined : className} />
  return (
    // Taps stop here: the button sits in rows that are tapped themselves, and although the sheet is
    // drawn at the end of the page, React still sends its events up through this place.
    <span className="lib-contents" onClick={(e) => e.stopPropagation()}>
      {inline ? <span className={cx('lib-rpe-inline', className)}>{button}</span> : button}
      {open && <RpeSheet onClose={() => setOpen(false)} />}
    </span>
  )
}

export type RpeSheetProps = { onClose: () => void }

const isValue = (line: string) => /^\d/.test(line)

/**
 * What RPE means, as a sheet in the style of the form guides: the title, then the domain's lines in
 * order, the scale itself (the lines that start with an RPE value) as a list. There is no safety
 * line: this explains a scale, not an exercise.
 */
export function RpeSheet({ onClose }: RpeSheetProps) {
  const guide = grit.rpeGuide()
  const lines = [...guide.lines]
  const first = lines.findIndex(isValue)
  const scaleStart = first >= 0 ? first : lines.length
  const scaleEnd = lines.reduce((end, line, i) => (isValue(line) ? i + 1 : end), scaleStart)
  return (
    <Dialog
      open
      variant="sheet"
      portal
      title={guide.title}
      onDismiss={onClose}
      header={
        <div className="lib-guide-head">
          <div className="lib-grow">
            <h2 className="headline-medium">{guide.title}</h2>
          </div>
          <IconButton label="Close" icon={<CloseIcon />} onClick={onClose} />
        </div>
      }
    >
      <div className="lib-guide lib-rpe">
        {lines.slice(0, scaleStart).map((line) => (
          <p key={line} className="lib-guide-purpose body-large">
            {line}
          </p>
        ))}
        {scaleEnd > scaleStart && (
          // The markers are drawn in CSS; the role keeps it a list for VoiceOver without them.
          <ul role="list" className="lib-guide-lines">
            {lines.slice(scaleStart, scaleEnd).map((line) => (
              <li key={line} className="body-large">
                {line}
              </li>
            ))}
          </ul>
        )}
        {lines.slice(scaleEnd).map((line) => (
          <p key={line} className="lib-guide-purpose body-large">
            {line}
          </p>
        ))}
      </div>
    </Dialog>
  )
}
