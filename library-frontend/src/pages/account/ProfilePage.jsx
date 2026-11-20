import { useEffect, useState } from 'react'
import { Alert, Avatar, Button, Card, CardContent, Chip, Divider, Stack, Typography } from '@mui/material'
import VerifiedIcon from '@mui/icons-material/Verified'
import { useDispatch, useSelector } from 'react-redux'
import { resendVerification } from '../../api/auth.js'
import { useToast } from '../../components/feedback/toastContext.js'
import { refreshProfile, selectUser } from '../../store/authSlice.js'
import { formatDate, formatDateTime } from '../../utils/format.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

function Row({ label, children }) {
  return (
    <div className="flex flex-col gap-0.5 sm:flex-row sm:gap-4 py-2">
      <Typography variant="body2" color="text.secondary" className="sm:w-40 shrink-0">
        {label}
      </Typography>
      <div className="min-w-0 break-words">{children}</div>
    </div>
  )
}

/** /profile — account details and email verification. (The backend has no "edit profile" endpoint yet.) */
export default function ProfilePage() {
  const user = useSelector(selectUser)
  const dispatch = useDispatch()
  const toast = useToast()
  const [sending, setSending] = useState(false)

  // Fresh data from the backend each time the page opens (e.g. verified in another tab).
  useEffect(() => {
    dispatch(refreshProfile())
  }, [dispatch])

  if (!user) return null

  const handleResend = async () => {
    setSending(true)
    try {
      const res = await resendVerification()
      toast(res.message)
    } catch (e) {
      toast(e.message, e.status === 429 ? 'warning' : 'error')
    } finally {
      setSending(false)
    }
  }

  const initials = (user.fullName || '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('')

  return (
    <Stack spacing={3}>
      <PageTitle>My profile</PageTitle>

      <Card variant="outlined">
        <CardContent>
          <div className="flex items-center gap-4 pb-4">
            <Avatar src={user.profileImage || undefined} sx={{ width: 64, height: 64, bgcolor: 'secondary.main', fontSize: 24 }}>
              {initials}
            </Avatar>
            <div className="min-w-0">
              <Typography variant="h6" component="p" sx={{ fontWeight: 600 }}>
                {user.fullName}
              </Typography>
              <Typography color="text.secondary" className="break-words">
                {user.email}
              </Typography>
            </div>
          </div>
          <Divider />

          <Row label="Email">
            <div className="flex flex-wrap items-center gap-2">
              <Typography>{user.email}</Typography>
              {user.verified ? (
                <Chip icon={<VerifiedIcon />} label="Verified" size="small" color="success" variant="outlined" />
              ) : (
                <Chip label="Not verified" size="small" color="warning" />
              )}
            </div>
          </Row>
          <Row label="Phone">
            <Typography color={user.phone ? 'text.primary' : 'text.secondary'}>{user.phone || 'Not set'}</Typography>
          </Row>
          <Row label="Account type">
            <Typography>{user.role === 'ROLE_ADMIN' ? 'Administrator' : 'Member'}</Typography>
          </Row>
          <Row label="Sign-in method">
            <Typography>{user.authProvider === 'GOOGLE' ? 'Google' : 'Email and password'}</Typography>
          </Row>
          {user.createdAt && (
            <Row label="Member since">
              <Typography>{formatDate(user.createdAt)}</Typography>
            </Row>
          )}
          {user.lastLogin && (
            <Row label="Last login">
              <Typography>{formatDateTime(user.lastLogin)}</Typography>
            </Row>
          )}
        </CardContent>
      </Card>

      {!user.verified && (
        <Alert
          severity="warning"
          action={
            <Button color="inherit" size="small" onClick={handleResend} disabled={sending}>
              {sending ? 'Sending…' : 'Resend email'}
            </Button>
          }
        >
          Confirm your email to subscribe and borrow books. Check your inbox for the link.
        </Alert>
      )}
    </Stack>
  )
}
