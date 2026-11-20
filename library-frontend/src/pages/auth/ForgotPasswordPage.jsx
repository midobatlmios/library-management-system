import { useState } from 'react'
import { Alert, Button, Link, Stack, TextField, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { forgotPassword } from '../../api/auth.js'
import AuthCard from '../../components/auth/AuthCard.jsx'
import FormError from '../../components/auth/FormError.jsx'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sentMessage, setSentMessage] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      // The backend answers the same way whether or not the email exists,
      // so nobody can use this form to find out who has an account.
      const res = await forgotPassword(email.trim())
      setSentMessage(res.message)
    } catch (err) {
      setError({ message: err.message, status: err.status })
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthCard
      title="Forgot your password?"
      subtitle="Enter your email and we will send you a link to choose a new one."
    >
      <FormError error={error} />

      {sentMessage ? (
        <Alert severity="success">
          {sentMessage} The link is valid for 15 minutes. Check your spam folder if you don't see it.
        </Alert>
      ) : (
        <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
          <TextField
            label="Email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
            fullWidth
          />
          <Button type="submit" variant="contained" size="large" disabled={busy || !email.trim()}>
            {busy ? 'Sending…' : 'Send reset link'}
          </Button>
        </Stack>
      )}

      <Typography variant="body2" sx={{ textAlign: 'center' }}>
        <Link component={RouterLink} to="/login">
          Back to log in
        </Link>
      </Typography>
    </AuthCard>
  )
}
