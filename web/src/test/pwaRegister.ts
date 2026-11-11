// Stands in for vite-plugin-pwa's `virtual:pwa-register` in unit tests, where no service worker exists.
export type RegisterSWOptions = { onNeedRefresh?: () => void }

export function registerSW(_options?: RegisterSWOptions): (reloadPage?: boolean) => Promise<void> {
  return async () => {}
}
