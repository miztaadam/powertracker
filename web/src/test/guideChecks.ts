import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect } from 'vitest'
import { grit } from '../domain'

export const NO_GUIDE = 'No form guide for your own exercises.'

export const guideIsOpen = () => screen.queryAllByRole('dialog', { name: /^Form guide for / }).length > 0

/**
 * Taps the info button for `name` (the `index`-th, when the exercise is on screen more than once),
 * checks that the sheet opens on that exercise's guide, then closes the sheet.
 */
export async function checkInfoButtonOpensGuide(name: string, index = 0) {
  const buttons = await screen.findAllByRole('button', { name: `Form guide for ${name}` })
  await userEvent.click(buttons[index]!)
  const sheet = within(await screen.findByRole('dialog', { name: `Form guide for ${name}` }))

  expect(sheet.getByRole('heading', { level: 2, name })).toBeInTheDocument()
  const entry = grit.findInLibrary(name)
  if (entry) {
    expect(sheet.getByText(entry.guide.purpose)).toBeInTheDocument()
    expect(sheet.getByText(entry.guide.steps[0]!)).toBeInTheDocument()
  } else {
    expect(sheet.getByText(NO_GUIDE)).toBeInTheDocument()
  }
  expect(sheet.getByText(grit.safetyLine())).toBeInTheDocument()

  await userEvent.click(sheet.getByRole('button', { name: 'Close guide' }))
  await waitFor(() => expect(guideIsOpen()).toBe(false))
}
