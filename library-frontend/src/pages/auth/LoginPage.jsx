import { useState } from 'react'
import { Alert, Button, Divider, Link, Stack, TextField, Typography } from '@mui/material'
import { useDispatch, useSelector } from 'react-redux'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
import AuthCard from '../../components/auth/AuthCard.jsx'
import FormError from '../../components/auth/FormError.jsx'
import GoogleSignInButton from '../../components/auth/GoogleSignInButton.jsx'
import PasswordField from '../../components/auth/PasswordField.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { login, loginWithGoogle, selectSessionExpired } from '../../store/authSlice.js'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function LoginPage() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()
  const expired = useSelector(selectSessionExpired)
  // PrivateRoute sends us here with state.from = the page the user wanted (keep its ?filters too).
  const fromLoc = location.state?.from
  const from = fromLoc ? `${fromLoc.pathname}${fromLoc.search ?? ''}` : '/'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const run = async (action) => {
    setError(null)
    setBusy(true)
    try {
      const res = await dispatch(action).unwrap()
      toast(`Welcome back, ${res.user?.fullName?.split(' ')[0] || 'reader'}!`)
      navigate(from, { replace: true })
    } catch (e) {
      setError(e)
      setBusy(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    run(login({ email: email.trim(), password }))
  }

  return (
    <AuthCard title="Log in" subtitle="Welcome back to the library.">
      {expired && !error && (
        <Alert severity="info">Your session has expired. Please log in again to continue.</Alert>
      )}
      <FormError error={error} />

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
        <PasswordField
          label="Password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          fullWidth
        />
        <Link component={RouterLink} to="/forgot-password" variant="body2" sx={{ alignSelf: 'flex-end' }}>
          Forgot your password?
        </Link>
        <Button type="submit" variant="contained" size="large" disabled={busy || !EMAIL_RE.test(email.trim()) || !password}>
          {busy ? 'Logging in…' : 'Log in'}
        </Button>
      </Stack>

      <Divider>or</Divider>
      <GoogleSignInButton onCredential={(credential) => run(loginWithGoogle(credential))} />

      <Typography variant="body2" sx={{ textAlign: 'center' }}>
        No account yet?{' '}
        <Link component={RouterLink} to="/signup" state={location.state}>
          Sign up
        </Link>
      </Typography>
    </AuthCard>
  )
}
