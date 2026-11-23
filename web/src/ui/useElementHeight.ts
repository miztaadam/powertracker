import { useLayoutEffect, useState, type RefObject } from 'react'

/**
 * The height in px of the element in `ref`, followed as it changes (larger text, rotation); 0 while
 * there is none. `present` says whether the element is rendered, so the hook looks again when it
 * comes and goes.
 */
export function useElementHeight(ref: RefObject<HTMLElement>, present: boolean): number {
  const [height, setHeight] = useState(0)
  useLayoutEffect(() => {
    const element = ref.current
    if (!present || element == null) {
      setHeight(0)
      return
    }
    const measure = () => setHeight(element.offsetHeight)
    measure()
    if (typeof ResizeObserver === 'undefined') return
    const observer = new ResizeObserver(measure)
    observer.observe(element)
    return () => observer.disconnect()
  }, [ref, present])
  return height
}
