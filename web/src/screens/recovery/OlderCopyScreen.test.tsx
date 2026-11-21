import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { OlderCopyScreen } from './OlderCopyScreen'

// 1.17: shown instead of the app when this copy of Grit is older than the stored log. It never opens the database,
// and offers only to update: no export (an older copy's backup would drop the plate markers), nothing else.

/** An update the test settles: `finish('failed')` is "couldn't update". */
function controlledUpdate() {
  let finish: (result: 'reloading' | 'failed') => void = () => {}
  const update = vi.fn(() => new Promise<'reloading' | 'failed'>((done) => (finish = done)))
  return { update, finish: (result: 'reloading' | 'failed') => finish(result) }
}

describe('OlderCopyScreen', () => {
  it('says why, in these words, with one button and nothing else', () => {
    render(<OlderCopyScreen update={vi.fn()} />)
    expect(screen.getByRole('heading', { level: 1, name: 'Update Grit to carry on' })).toBeInTheDocument()
    expect(screen.getByText("This copy of Grit is older than your log, so it can't open it safely. Nothing has been changed.")).toBeInTheDocument()
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual(['Update and reload'])
    expect(screen.queryByRole('button', { name: /export/i })).toBeNull()
    expect(screen.queryByText(/export/i)).toBeNull()
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })

  it('while updating: the button is disabled and reads Updating…', async () => {
    const { update } = controlledUpdate()
    render(<OlderCopyScreen update={update} />)
    await userEvent.click(screen.getByRole('button', { name: 'Update and reload' }))
    const button = screen.getByRole('button', { name: 'Updating…' })
    expect(button).toBeDisabled()
    await userEvent.click(button)
    expect(update).toHaveBeenCalledTimes(1)
  })

  it("couldn't update: says so politely, and the button is enabled again", async () => {
    const { update, finish } = controlledUpdate()
    render(<OlderCopyScreen update={update} />)
    await userEvent.click(screen.getByRole('button', { name: 'Update and reload' }))
    finish('failed')
    expect(await screen.findByText("Couldn't update. Check your connection and try again.")).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent("Couldn't update. Check your connection and try again.")
    const button = screen.getByRole('button', { name: 'Update and reload' })
    expect(button).toBeEnabled()

    // Tried again: the line goes while it works.
    await userEvent.click(button)
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
    expect(update).toHaveBeenCalledTimes(2)
  })

  it('reloading: stays on Updating… until the page goes', async () => {
    const { update, finish } = controlledUpdate()
    render(<OlderCopyScreen update={update} />)
    await userEvent.click(screen.getByRole('button', { name: 'Update and reload' }))
    finish('reloading')
    await Promise.resolve()
    expect(screen.getByRole('button', { name: 'Updating…' })).toBeDisabled()
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })
})
