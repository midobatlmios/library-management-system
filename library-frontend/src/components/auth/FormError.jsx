import { Alert, AlertTitle } from '@mui/material'

/**
 * Shows an error from the backend. error = { message, status } or null.
 * 429 (too many attempts) gets its own friendlier warning.
 */
export default function FormError({ error }) {
  if (!error) return null

  if (error.status === 429) {
    return (
      <Alert severity="warning">
        <AlertTitle>Too many attempts</AlertTitle>
        {error.message}
      </Alert>
    )
  }
  return <Alert severity="error">{error.message}</Alert>
}
