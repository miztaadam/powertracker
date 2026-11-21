import { useContext, type ReactNode } from 'react'
import { grit, muscleFocus } from '../../domain'
import { IconButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { CloseIcon, HealthAndSafetyIcon } from '../../ui/components/icons'
import { TrainingModeContext } from '../../ui/components/ModeLabel'
import { ExercisePicture } from './ExercisePicture'
import { whereAndWith } from './labels'
import './library.css'

/** What the sheet says for an exercise the catalogue does not know. */
export const NO_GUIDE = 'No form guide for your own exercises.'

export type FormGuideSheetProps = {
  name: string
  onClose: () => void
  /** Buttons for what can be done with the exercise (the Exercises tab's "Add to plan"). They stay in view under the guide. */
  actions?: ReactNode
}

/**
 * The form guide for the exercise `name`, as a sheet to read between sets. The guide follows the
 * name: a catalogue name or alias shows the catalogue guide, whoever made the row; any other name
 * shows `NO_GUIDE`. A guide whose exercise has a figure opens with its picture, above the purpose.
 * While bodybuilding mode is on, a "Stretch and squeeze" section follows Cues (stage 4): the two lines, or the one
 * line saying there is nothing to aim for; with the mode off the sheet is as it always was. Every guide ends with the
 * safety line; `actions` sit below it.
 */
export function FormGuideSheet({ name, onClose, actions }: FormGuideSheetProps) {
  const entry = grit.findInLibrary(name)
  const guide = entry?.guide
  const focus = useContext(TrainingModeContext) === 'BODYBUILDING' ? muscleFocus(name) : null
  return (
    <Dialog
      open
      variant="sheet"
      portal
      className="lib-guide-sheet"
      title={`Form guide for ${name}`}
      onDismiss={onClose}
      header={
        <div className="lib-guide-head">
          <div className="lib-grow">
            <h2 className="headline-medium">{name}</h2>
            {entry && <p className="lib-guide-where eyebrow">{whereAndWith(entry)}</p>}
          </div>
          <IconButton label="Close guide" icon={<CloseIcon />} onClick={onClose} />
        </div>
      }
    >
      <div className="lib-guide">
        {guide ? (
          <>
            <ExercisePicture name={name} />
            <p className="lib-guide-purpose body-large">{guide.purpose}</p>
            <GuideSection title="Setup" lines={guide.setup} />
            <GuideSection title="How to do it" lines={guide.steps} numbered />
            <GuideSection title="Cues" lines={guide.cues} />
            {focus && <GuideSection title="Stretch and squeeze" lines={focus.kind === 'lines' ? [focus.stretch, focus.squeeze] : [focus.line]} />}
            <GuideSection title="Common mistakes" lines={guide.mistakes} />
          </>
        ) : (
          <p className="lib-guide-purpose body-large">{NO_GUIDE}</p>
        )}
        <div className="lib-guide-safety">
          <HealthAndSafetyIcon />
          <p className="lib-grow body-large">{grit.safetyLine()}</p>
        </div>
      </div>
      {actions != null && <div className="lib-guide-actions">{actions}</div>}
    </Dialog>
  )
}

function GuideSection({ title, lines, numbered = false }: { title: string; lines: readonly string[]; numbered?: boolean }) {
  const List = numbered ? 'ol' : 'ul'
  return (
    <section>
      <h3 className="lib-guide-title title-large">{title}</h3>
      {/* The markers are drawn in CSS; the role keeps it a list for VoiceOver without them. */}
      <List role="list" className={numbered ? 'lib-guide-lines lib-guide-steps' : 'lib-guide-lines'}>
        {lines.map((line) => (
          <li key={line} className="body-large">
            {line}
          </li>
        ))}
      </List>
    </section>
  )
}
