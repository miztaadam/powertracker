import { act } from '@testing-library/react'

/**
 * Lets an exercise list finish drawing. The picker and the Exercises tab draw their first screenful at once and the
 * rest after the next frame (ExerciseBrowser's FIRST_ROWS), so a test that reads the whole list, or checks that a
 * row is not there, waits for it first. Two frames, then a timeout: the rest is drawn by then.
 */
export const wholeList = () => act(() => new Promise<void>((done) => requestAnimationFrame(() => requestAnimationFrame(() => setTimeout(done, 0)))))
