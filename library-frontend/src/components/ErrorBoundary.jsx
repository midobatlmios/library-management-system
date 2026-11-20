import { Component } from 'react'
import { Alert, Button, Stack, Typography } from '@mui/material'

/**
 * If a page crashes while rendering (a bug), React would show a blank screen.
 * This catches the error and shows a friendly message instead.
 * Error boundaries must still be class components in React.
 * resetKey: when it changes (e.g. the URL), the boundary tries again.
 */
export default class ErrorBoundary extends Component {
  state = { error: null, resetKey: this.props.resetKey }

  static getDerivedStateFromError(error) {
    return { error }
  }

  static getDerivedStateFromProps(props, state) {
    // Navigating to another page clears the error.
    if (props.resetKey !== state.resetKey) return { error: null, resetKey: props.resetKey }
    return null
  }

  componentDidCatch(error, info) {
    console.error('Page crashed:', error, info.componentStack)
  }

  render() {
    if (!this.state.error) return this.props.children
    return (
      <Stack spacing={2} sx={{ alignItems: 'flex-start', py: 4 }}>
        <Typography variant="h4" component="h1" sx={{ fontWeight: 700 }}>
          Something went wrong
        </Typography>
        <Typography color="text.secondary">This page ran into a problem. Reloading usually fixes it.</Typography>
        {import.meta.env.DEV && (
          <Alert severity="error" sx={{ whiteSpace: 'pre-wrap', maxWidth: '100%', overflow: 'auto' }}>
            {String(this.state.error?.message || this.state.error)}
          </Alert>
        )}
        <div className="flex gap-2">
          <Button variant="contained" onClick={() => window.location.reload()}>
            Reload the page
          </Button>
          <Button href="/">Go home</Button>
        </div>
      </Stack>
    )
  }
}
