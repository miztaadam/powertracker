import { Component, type ReactNode } from 'react'

export type ErrorBoundaryProps = {
  /** What to show in place of the app once something in it has thrown while rendering. */
  fallback: (error: unknown) => ReactNode
  children: ReactNode
}

/**
 * Catches what the screens throw when the database cannot be opened or read, so the lifter gets the
 * recovery screen and not a blank page.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, { failure: { error: unknown } | null }> {
  state: { failure: { error: unknown } | null } = { failure: null }

  static getDerivedStateFromError(error: unknown) {
    return { failure: { error } }
  }

  render() {
    return this.state.failure != null ? this.props.fallback(this.state.failure.error) : this.props.children
  }
}
