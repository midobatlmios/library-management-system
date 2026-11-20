import { useCallback, useState } from 'react'
import { Alert, Button, Card, CardContent, CircularProgress, Stack, Typography } from '@mui/material'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import HourglassTopIcon from '@mui/icons-material/HourglassTop'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import { cancelPayment, getPayment } from '../../api/payments.js'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatDateTime, formatMoney } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'

// Where to go next, by what was paid for.
const NEXT = {
  SUBSCRIPTION: { to: '/subscription', label: 'See my subscription', retry: '/plans' },
  FINE: { to: '/fines', label: 'Back to my fines', retry: '/fines' },
}

const FAILED_TEXT = {
  FAILED: 'The payment failed.',
  EXPIRED: 'The payment link expired before it was paid.',
  CANCELLED: 'The payment was cancelled.',
}

/**
 * /payment/result?paymentId=... — the simulated checkout page sends the browser here.
 * It asks the backend for the payment's real status (GET /api/payments/{id}):
 * never trust a "success" in the URL, always check with the server.
 */
export default function PaymentResultPage() {
  const [params] = useSearchParams()
  const paymentId = params.get('paymentId')
  const toast = useToast()

  const load = useCallback(() => (paymentId ? getPayment(paymentId) : Promise.resolve(null)), [paymentId])
  const { data: payment, error, loading, reload } = useAsync(load)
  const [cancelling, setCancelling] = useState(false)

  if (!paymentId) {
    return <Alert severity="error">This link is missing the payment id.</Alert>
  }
  if (error) {
    return (
      <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
        <Alert severity="error">{error}</Alert>
        <Button component={RouterLink} to="/payments" variant="contained">
          My payments
        </Button>
      </Stack>
    )
  }
  if (loading || !payment) {
    return (
      <Stack sx={{ alignItems: 'center', py: 8 }} spacing={2}>
        <CircularProgress />
        <Typography color="text.secondary">Checking your payment…</Typography>
      </Stack>
    )
  }

  const next = NEXT[payment.referenceType] ?? { to: '/payments', label: 'My payments', retry: '/' }
  const amount = formatMoney(payment.amountInMajorUnits, payment.currency)

  // The checkout page belongs to the backend's simulated gateway: <API>/pay/<paymentId>.
  const checkoutUrl = `${import.meta.env.VITE_API_URL}/pay/${encodeURIComponent(payment.paymentId)}`

  const handleCancel = async () => {
    setCancelling(true)
    try {
      await cancelPayment(payment.paymentId)
      toast('Payment cancelled', 'info')
      reload()
    } catch (e) {
      toast(e.message, 'error')
    } finally {
      setCancelling(false)
    }
  }

  let icon, title, body, actions
  if (payment.status === 'SUCCESS') {
    icon = <CheckCircleIcon color="success" sx={{ fontSize: 64 }} />
    title = 'Payment successful'
    body = (
      <>
        <Typography>
          {payment.description}: <strong>{amount}</strong> paid
          {payment.cardLast4 && ` with the card ending ${payment.cardLast4}`}.
        </Typography>
        {payment.referenceType === 'SUBSCRIPTION' && (
          <Typography color="text.secondary">Your subscription is active. You can borrow books now.</Typography>
        )}
        {payment.completedAt && (
          <Typography variant="body2" color="text.secondary">
            {formatDateTime(payment.completedAt)}
          </Typography>
        )}
      </>
    )
    actions = (
      <>
        <Button component={RouterLink} to={next.to} variant="contained">
          {next.label}
        </Button>
        <Button component={RouterLink} to="/books">
          Browse the catalog
        </Button>
      </>
    )
  } else if (payment.status === 'PENDING') {
    icon = <HourglassTopIcon color="warning" sx={{ fontSize: 64 }} />
    title = 'Payment not finished'
    body = (
      <Typography>
        {payment.description}: {amount} is still waiting to be paid
        {payment.expiresAt && ` (the link works until ${formatDateTime(payment.expiresAt)})`}.
      </Typography>
    )
    actions = (
      <>
        <Button href={checkoutUrl} variant="contained">
          Continue to payment
        </Button>
        <Button color="error" onClick={handleCancel} disabled={cancelling}>
          {cancelling ? 'Cancelling…' : 'Cancel this payment'}
        </Button>
      </>
    )
  } else {
    icon = <ErrorIcon color="error" sx={{ fontSize: 64 }} />
    title = 'Payment not completed'
    body = (
      <>
        <Typography>
          {FAILED_TEXT[payment.status] ?? `Status: ${payment.status}.`} Nothing was charged.
        </Typography>
        {payment.lastError && <Alert severity="error">{payment.lastError}</Alert>}
      </>
    )
    actions = (
      <Button component={RouterLink} to={next.retry} variant="contained">
        Try again
      </Button>
    )
  }

  return (
    <div className="mx-auto w-full max-w-lg">
      <Card variant="outlined">
        <CardContent className="p-6 sm:p-8">
          <Stack spacing={2} sx={{ alignItems: 'center', textAlign: 'center' }}>
            {icon}
            <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
              {title}
            </Typography>
            {body}
            <Typography variant="caption" color="text.secondary">
              Reference {payment.paymentId}
            </Typography>
            <div className="flex flex-wrap justify-center gap-2 pt-2">{actions}</div>
          </Stack>
        </CardContent>
      </Card>
    </div>
  )
}
