/** True when the browser has promised not to clear Grit's storage on its own. False when it cannot say. */
export async function storageProtected(): Promise<boolean> {
  try {
    return (await navigator.storage?.persisted?.()) === true
  } catch {
    return false
  }
}
