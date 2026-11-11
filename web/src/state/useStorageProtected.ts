import { useEffect, useState } from 'react'
import { onStorageProtectionChange, storageProtected } from '../platform/storage'

/** Whether the browser keeps Grit's storage; null until it has answered. Read again after each request to keep it. */
export function useStorageProtected(): boolean | null {
  const [isProtected, setProtected] = useState<boolean | null>(null)
  useEffect(() => {
    let stale = false
    const read = () => {
      storageProtected()
        .then((value) => {
          if (!stale) setProtected(value)
        })
        .catch(() => {})
    }
    read()
    const stop = onStorageProtectionChange(read)
    return () => {
      stale = true
      stop()
    }
  }, [])
  return isProtected
}
