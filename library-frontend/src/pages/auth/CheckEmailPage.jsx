import { useState } from 'react'
import { Alert, Button, Stack, Typography } from '@mui/material'
import MarkEmailReadIcon from '@mui/icons-material/MarkEmailRead'
import { useDispatch, useSelector } from 'react-redux'
import { Link as RouterLink, useLocation } from 'react-router-dom'
import { resendVerification } from '../../api/auth.js'
import AuthCard from '../../components/auth/AuthCard.jsx'
import FormError from '../../components/auth/FormError.jsx'
import { refreshProfile, selectUser } from '../../store/authSlice.js'

/**
 * "Check your email" notice. Shown after signup and from the banner for
 * unverified users. The link in the email opens the backend directly.
 */
export default function CheckEmailPage() {
  const user = useSelector(selectUser)
  const dispatch = useDispatch()
  const location = useLocation()
  const next = location.state?.from?.pathname || '/'

  const [info, setInfo] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const wrap = async (fn) => {
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      await fn()
    } catch (err) {
      setError({ message: err.message, status: err.status })
    } finally {
      setBusy(false)
    }
  }

  const handleResend = () =>
    wrap(async () => {
      const res = await resendVerification()
      setInfo(res.message)
    })

  // After clicking the link in the email, reload the user to see verified = true.
  const handleCheck = () =>
    wrap(async () => {
      const fresh = await dispatch(refreshProfile()).unwrap()
      if (!fresh.verified) setInfo('Not confirmed yet. Click the link in the email first.')
    })

  if (user?.verified) {
    return (
      <AuthCard title="Email confirmed">
        <Alert severity="success">{user.email} is verified. You have full access.</Alert>
        <Button component={RouterLink} to={next} variant="contained" size="large">
          Continue
        </Button>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Check your email">
      <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start' }}>
        <MarkEmailReadIcon color="primary" fontSize="large" />
        <Typography>
          We sent a confirmation link to <strong>{user?.email}</strong>. Click it to verify your address.
          You need a verified email to subscribe and borrow books.
        </Typography>
      </Stack>

      {info && <Alert severity="info">{info}</Alert>}
      <FormError error={error} />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
        <Button variant="contained" onClick={handleCheck} disabled={busy}>
          I've confirmed it
        </Button>
        <Button variant="outlined" onClick={handleResend} disabled={busy}>
          Resend the email
        </Button>
      </Stack>
      <Button component={RouterLink} to={next} sx={{ alignSelf: 'flex-start' }}>
        Do it later
      </Button>
    </AuthCard>
  )
}
