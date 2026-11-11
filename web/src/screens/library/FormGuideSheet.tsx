import { grit } from '../../domain'
import { IconButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { CloseIcon, HealthAndSafetyIcon } from '../../ui/components/icons'
import { whereAndWith } from './labels'
import './library.css'

/** What the sheet says for an exercise the catalogue does not know. */
export const NO_GUIDE = 'No form guide for your own exercises.'

export type FormGuideSheetProps = { name: string; onClose: () => void }

/**
 * The form guide for the exercise `name`, as a sheet to read between sets. The guide follows the
 * name: a catalogue name or alias shows the catalogue guide, whoever made the row; any other name
 * shows `NO_GUIDE`. Every sheet ends with the safety line.
 */
export function FormGuideSheet({ name, onClose }: FormGuideSheetProps) {
  const entry = grit.findInLibrary(name)
  const guide = entry?.guide
  return (
    <Dialog
      open
      variant="sheet"
      portal
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
            <p className="lib-guide-purpose body-large">{guide.purpose}</p>
            <GuideSection title="Setup" lines={guide.setup} />
            <GuideSection title="How to do it" lines={guide.steps} numbered />
            <GuideSection title="Cues" lines={guide.cues} />
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
