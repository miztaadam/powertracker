import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { PRESS_HOLD_MS, PRESS_TARGETS, PRESSING, installPressHold } from './pressHold'

const pointer = (type: string, target: EventTarget) => target.dispatchEvent(new Event(type, { bubbles: true, cancelable: true }))

let uninstall: () => void
let button: HTMLButtonElement

beforeEach(() => {
  vi.useFakeTimers()
  document.body.innerHTML = '<div class="row"><button type="button" class="gr-btn"><span class="face">Log set</span></button><p class="plain">Text</p></div>'
  button = document.querySelector('button')!
  uninstall = installPressHold(document)
})
afterEach(() => {
  uninstall()
  vi.useRealTimers()
  document.documentElement.removeAttribute('data-motion')
  document.body.innerHTML = ''
})

describe('press hold', () => {
  it('keeps a quick tap pressed for 90 ms from the finger going down', () => {
    pointer('pointerdown', button.querySelector('.face')!)
    expect(button).toHaveClass(PRESSING)
    pointer('pointerup', button)
    vi.advanceTimersByTime(PRESS_HOLD_MS - 1)
    expect(button).toHaveClass(PRESSING)
    vi.advanceTimersByTime(1)
    expect(button).not.toHaveClass(PRESSING)
  })

  it('lets go at once after a longer press', () => {
    pointer('pointerdown', button)
    vi.advanceTimersByTime(300)
    pointer('pointerup', button)
    vi.advanceTimersByTime(0)
    expect(button).not.toHaveClass(PRESSING)
  })

  it('springs straight back when the press turns into a scroll', () => {
    pointer('pointerdown', button)
    pointer('pointercancel', button)
    expect(button).not.toHaveClass(PRESSING)
  })

  it('never marks a disabled control, something not tappable, or anything with motion off', () => {
    button.disabled = true
    pointer('pointerdown', button)
    expect(button).not.toHaveClass(PRESSING)
    button.disabled = false
    button.setAttribute('aria-disabled', 'true')
    pointer('pointerdown', button)
    expect(button).not.toHaveClass(PRESSING)
    button.removeAttribute('aria-disabled')
    pointer('pointerdown', document.querySelector('.plain')!)
    expect(document.querySelectorAll(`.${PRESSING}`)).toHaveLength(0)
    document.documentElement.setAttribute('data-motion', 'off')
    pointer('pointerdown', button)
    expect(button).not.toHaveClass(PRESSING)
  })

  it('stops listening once uninstalled', () => {
    uninstall()
    pointer('pointerdown', button)
    expect(button).not.toHaveClass(PRESSING)
    uninstall = () => {}
  })

  it('names exactly the targets press.css sinks', () => {
    const css = readFileSync(resolve(process.cwd(), 'src/ui/press.css'), 'utf8')
    for (const selector of PRESS_TARGETS.split(',').map((s) => s.trim())) expect(css, selector).toContain(`${selector}`)
  })
})
