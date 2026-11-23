/** The meaning a coloured element carries. Pick by meaning, never by looks. */
export type Tone = 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'info'

/** Sets `--tone` (text, icon, stroke) and `--tone-container` (soft background) on an element. */
export function toneClass(tone: Tone): string {
  return `gr-tone-${tone}`
}
