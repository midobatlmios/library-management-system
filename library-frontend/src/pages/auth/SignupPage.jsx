import { useState } from 'react'
import { Button, Divider, Link, Stack, TextField, Typography } from '@mui/material'
import { useDispatch } from 'react-redux'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
import AuthCard from '../../components/auth/AuthCard.jsx'
import FormError from '../../components/auth/FormError.jsx'
import GoogleSignInButton from '../../components/auth/GoogleSignInButton.jsx'
import PasswordField from '../../components/auth/PasswordField.jsx'
import PasswordRules from '../../components/auth/PasswordRules.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { loginWithGoogle, signup } from '../../store/authSlice.js'
import { isPasswordValid } from '../../utils/password.js'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function SignupPage() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()
  const from = location.state?.from?.pathname || '/'

  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value })
  const emailInvalid = form.email !== '' && !EMAIL_RE.test(form.email.trim())
  const canSubmit = form.fullName.trim() && EMAIL_RE.test(form.email.trim()) && isPasswordValid(form.password)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!canSubmit) return
    setError(null)
    setBusy(true)
    try {
      await dispatch(signup({ ...form, fullName: form.fullName.trim(), email: form.email.trim() })).unwrap()
      // The account exists and we are logged in; the email still needs confirming.
      toast('Account created. Welcome to the library!')
      navigate('/check-email', { replace: true, state: { from: location.state?.from } })
    } catch (err) {
      setError(err)
      setBusy(false)
    }
  }

  // Google accounts are verified already, so go straight on.
  const handleGoogle = async (credential) => {
    setError(null)
    try {
      await dispatch(loginWithGoogle(credential)).unwrap()
      toast('Welcome to the library!')
      navigate(from, { replace: true })
    } catch (err) {
      setError(err)
    }
  }

  return (
    <AuthCard title="Create an account" subtitle="Join the library to borrow and reserve books.">
      <FormError error={error} />

      <GoogleSignInButton text="signup_with" onCredential={handleGoogle} />
      <Divider>or with your email</Divider>

      <Stack component="form" spacing={2} onSubmit={handleSubmit} noValidate>
        <TextField
          label="Full name"
          autoComplete="name"
          value={form.fullName}
          onChange={update('fullName')}
          required
          fullWidth
        />
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={update('email')}
          error={emailInvalid}
          helperText={emailInvalid ? 'Enter a valid email address' : ' '}
          required
          fullWidth
        />
        <TextField
          label="Phone (optional)"
          type="tel"
          autoComplete="tel"
          value={form.phone}
          onChange={update('phone')}
          fullWidth
        />
        <PasswordField
          label="Password"
          autoComplete="new-password"
          value={form.password}
          onChange={update('password')}
          required
          fullWidth
        />
        <PasswordRules password={form.password} />
        <Button type="submit" variant="contained" size="large" disabled={busy || !canSubmit}>
          {busy ? 'Creating account…' : 'Sign up'}
        </Button>
      </Stack>

      <Typography variant="body2" sx={{ textAlign: 'center' }}>
        Already have an account?{' '}
        <Link component={RouterLink} to="/login" state={location.state}>
          Log in
        </Link>
      </Typography>
    </AuthCard>
  )
}
