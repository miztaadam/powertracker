export type SaveResult = 'saved' | 'cancelled'

/**
 * Hands a JSON file to the lifter: through the share sheet where the browser can share files (iPhone:
 * "Save to Files"), otherwise as a download. 'cancelled' means the share sheet was closed without a choice.
 */
export async function saveJsonFile(name: string, json: string): Promise<SaveResult> {
  const file = new File([json], name, { type: 'application/json' })
  if (typeof navigator.share === 'function' && navigator.canShare?.({ files: [file] }) === true) {
    try {
      await navigator.share({ files: [file] })
      return 'saved'
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') return 'cancelled'
      // Sharing was refused (for example the tap was too long ago): download instead.
    }
  }
  const url = URL.createObjectURL(file)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.click()
  // The download reads the URL after the click returns.
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
  return 'saved'
}
