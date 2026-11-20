import { useState } from 'react'
import { Alert, Button, Link, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../../api/auth.js'
import AuthCard from '../../components/auth/AuthCard.jsx'
import FormError from '../../components/auth/FormError.jsx'
import PasswordField from '../../components/auth/PasswordField.jsx'
import PasswordRules from '../../components/auth/PasswordRules.jsx'
import { isPasswordValid } from '../../utils/password.js'

/** Opened from the reset email: /reset-password?token=... */
export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token')

  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const mismatch = confirm !== '' && confirm !== password
  const canSubmit = isPasswordValid(password) && password === confirm

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!canSubmit) return
    setError(null)
    setBusy(true)
    try {
      await resetPassword(token, password)
      setDone(true)
    } catch (err) {
      setError({ message: err.message, status: err.status })
    } finally {
      setBusy(false)
    }
  }

  if (!token) {
    return (
      <AuthCard title="Reset your password">
        <Alert severity="error">This reset link is incomplete. Open the link from your email again.</Alert>
        <Button component={RouterLink} to="/forgot-password" variant="contained">
          Ask for a new link
        </Button>
      </AuthCard>
    )
  }

  if (done) {
    return (
      <AuthCard title="Password changed">
        <Alert severity="success">Your password was reset. You can log in with the new one.</Alert>
        <Button component={RouterLink} to="/login" variant="contained" size="large">
          Log in
        </Button>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Choose a new password">
      <FormError error={error} />
      {error && (
        <Typography variant="body2">
          Link expired?{' '}
          <Link component={RouterLink} to="/forgot-password">
            Ask for a new one
          </Link>
        </Typography>
      )}

      <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
        <PasswordField
          label="New password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          autoFocus
          fullWidth
        />
        <PasswordRules password={password} />
        <PasswordField
          label="Repeat the new password"
          autoComplete="new-password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          error={mismatch}
          helperText={mismatch ? 'The passwords do not match' : ' '}
          required
          fullWidth
        />
        <Button type="submit" variant="contained" size="large" disabled={busy || !canSubmit}>
          {busy ? 'Saving…' : 'Save new password'}
        </Button>
      </Stack>
    </AuthCard>
  )
}
