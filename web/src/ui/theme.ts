const BACKGROUND = { dark: '#0B0C0E', light: '#F4F4F0' } as const

export type Scheme = keyof typeof BACKGROUND

const query = (): MediaQueryList | null =>
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-color-scheme: light)')
    : null

/** The system colour scheme; dark unless the system asks for light. */
export function currentScheme(): Scheme {
  return query()?.matches ? 'light' : 'dark'
}

/** Keeps the browser chrome (`theme-color`) in step with the system scheme. Returns a stop function. */
export function syncThemeColor(): () => void {
  const apply = () => {
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', BACKGROUND[currentScheme()])
  }
  apply()
  const mq = query()
  mq?.addEventListener('change', apply)
  return () => mq?.removeEventListener('change', apply)
}
