import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect } from 'vitest'
import { createYourOwn } from './wholeList'

/** What `fillCustomExercise` types into "Create your own": a variation of the bench, with two muscles. */
export const OWN_NAME = 'Tyre flip'
export const OWN_MUSCLES = 'Lats, Grip'
export const SAVE_FAILED = "Couldn't save — try again."

export const createForm = () => screen.queryByRole('dialog', { name: 'Add custom exercise' })
const form = () => within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
const name = () => form().getByRole('textbox', { name: 'Name' })
const muscles = () => form().getByRole('textbox', { name: 'Muscles (comma-separated)' })
const chip = (text: string) => form().getByRole('checkbox', { name: text })

/** Opens "Create your own" in the open picker. */
export async function openCreateForm() {
  await userEvent.click(await createYourOwn())
  expect(createForm()).not.toBeNull()
}

/** In the open create form: everything it holds, set away from its defaults. Does not tap Create. */
export async function fillCustomExercise() {
  await userEvent.clear(name())
  await userEvent.type(name(), OWN_NAME)
  await userEvent.click(chip('Variation'))
  await userEvent.click(chip('Bench'))
  await userEvent.type(muscles(), OWN_MUSCLES)
}

export const tapCreate = () => userEvent.click(form().getByRole('button', { name: 'Create' }))

/** The create form is open and still holds what `fillCustomExercise` typed: name, category, parent lift and muscles. */
export function expectCustomExerciseKept() {
  expect(createForm()).not.toBeNull()
  expect(name()).toHaveValue(OWN_NAME)
  expect(chip('Variation')).toBeChecked()
  expect(chip('Accessory')).not.toBeChecked()
  expect(chip('Bench')).toBeChecked()
  expect(muscles()).toHaveValue(OWN_MUSCLES)
}

/** The create form is open as new: `initialName` (what was searched for), an accessory, no parent lift, no muscles. */
export function expectCustomExerciseEmpty(initialName: string) {
  expect(createForm()).not.toBeNull()
  expect(name()).toHaveValue(initialName)
  expect(chip('Accessory')).toBeChecked()
  expect(chip('Variation')).not.toBeChecked()
  expect(form().queryByRole('checkbox', { name: 'Bench' })).toBeNull()
  expect(muscles()).toHaveValue('')
}

/**
 * The failed save's message is said from inside the open create form, so it is drawn over the form's scrim (and is not
 * made inert with the page behind it), and only once.
 */
export async function expectSaveFailedInTheForm() {
  expect(await within(screen.getByRole('dialog', { name: 'Add custom exercise' })).findByText(SAVE_FAILED)).toBeInTheDocument()
  expect(screen.getAllByText(SAVE_FAILED)).toHaveLength(1)
}
