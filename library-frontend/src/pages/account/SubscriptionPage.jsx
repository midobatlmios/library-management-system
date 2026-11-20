import { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  LinearProgress,
  Skeleton,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useNavigate } from 'react-router-dom'
import {
  cancelSubscription,
  getActiveSubscription,
  getSubscriptionHistory,
  renewSubscription,
} from '../../api/subscriptions.js'
import EmptyState from '../../components/account/EmptyState.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatDate, formatMoney } from '../../utils/format.js'
import { continueToPayment } from '../../utils/payment.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const STATUS = {
  ACTIVE: { label: 'Active', color: 'success' },
  PENDING: { label: 'Waiting for payment', color: 'warning' },
  EXPIRED: { label: 'Expired', color: 'default' },
  CANCELLED: { label: 'Cancelled', color: 'default' },
}

function StatusChip({ sub }) {
  const s = STATUS[sub.status] ?? { label: sub.status, color: 'default' }
  return <Chip size="small" color={s.color} label={s.label} />
}

function Fact({ label, value }) {
  return (
    <div>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography sx={{ fontWeight: 600 }}>{value}</Typography>
    </div>
  )
}

function CancelDialog({ sub, onClose, onDone }) {
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  const handleCancel = async () => {
    setBusy(true)
    try {
      await cancelSubscription(sub.id, reason.trim() || undefined)
      toast('Subscription cancelled', 'info')
      onDone()
    } catch (e) {
      toast(e.message, 'error')
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Cancel your subscription?</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          You will no longer be able to borrow or reserve books with the {sub.planName} plan.
        </DialogContentText>
        <TextField
          label="Why are you leaving? (optional)"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          fullWidth
          multiline
          minRows={2}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Keep it
        </Button>
        <Button color="error" variant="contained" onClick={handleCancel} disabled={busy}>
          {busy ? 'Cancelling…' : 'Cancel subscription'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

/** /subscription — the current plan with Renew / Cancel, and the history. */
export default function SubscriptionPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const active = useAsync(getActiveSubscription)
  const history = useAsync(getSubscriptionHistory)
  const [renewing, setRenewing] = useState(false)
  const [cancelOpen, setCancelOpen] = useState(false)

  const sub = active.data

  const handleRenew = async () => {
    setRenewing(true)
    try {
      const payment = await renewSubscription(sub.id)
      continueToPayment(payment, { navigate, toast })
    } catch (e) {
      toast(e.message, 'error')
      setRenewing(false)
    }
  }

  const reloadAll = () => {
    setCancelOpen(false)
    active.reload()
    history.reload()
  }

  // Share of the period already used, for the progress bar.
  const used =
    sub && sub.durationDays ? Math.min(100, Math.max(0, ((sub.durationDays - sub.daysRemaining) / sub.durationDays) * 100)) : 0

  return (
    <Stack spacing={3}>
      <PageTitle>My subscription</PageTitle>

      {active.error && <Alert severity="error">{active.error}</Alert>}

      {active.loading && !sub ? (
        <Skeleton variant="rounded" height={200} />
      ) : !sub ? (
        <EmptyState
          title="No active subscription"
          text="Subscribe to a plan to borrow and reserve books."
          actionLabel="See the plans"
          actionTo="/plans"
        />
      ) : (
        <Card variant="outlined">
          <CardContent>
            <Stack spacing={3}>
              <div className="flex flex-wrap items-center gap-2">
                <Typography variant="h5" component="h2" sx={{ fontWeight: 700 }}>
                  {sub.planName}
                </Typography>
                <StatusChip sub={sub} />
                {sub.autoRenew && <Chip size="small" variant="outlined" label="Auto-renew" />}
              </div>

              <div>
                <div className="flex justify-between">
                  <Typography variant="body2">{formatDate(sub.startDate)}</Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {sub.daysRemaining} day{sub.daysRemaining === 1 ? '' : 's'} left
                  </Typography>
                  <Typography variant="body2">{formatDate(sub.endDate)}</Typography>
                </div>
                <LinearProgress
                  variant="determinate"
                  value={used}
                  color={sub.daysRemaining <= 5 ? 'warning' : 'primary'}
                  sx={{ height: 8, borderRadius: 4, mt: 1 }}
                  aria-label="Part of the subscription period used"
                />
              </div>

              <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
                <Fact label="Books at a time" value={sub.maxBooksAllowed} />
                <Fact label="Days per loan" value={sub.maxBorrowDays} />
                <Fact label="Price" value={formatMoney(sub.priceInMajorUnits, sub.currency)} />
                <Fact label="Period" value={`${sub.durationDays} days`} />
              </div>

              {sub.daysRemaining <= 5 && (
                <Alert severity="warning">Your subscription ends soon. Renew it to keep borrowing.</Alert>
              )}

              <div className="flex flex-wrap gap-2">
                <Button variant="contained" onClick={handleRenew} disabled={renewing}>
                  {renewing ? 'Starting…' : 'Renew'}
                </Button>
                <Button color="error" onClick={() => setCancelOpen(true)}>
                  Cancel subscription
                </Button>
              </div>
              <Typography variant="caption" color="text.secondary">
                Renewing adds a new period that starts when this one ends.
              </Typography>
            </Stack>
          </CardContent>
        </Card>
      )}

      <Typography variant="h6" component="h2" sx={{ fontWeight: 700 }}>
        History
      </Typography>
      {history.error && <Alert severity="error">{history.error}</Alert>}
      {!history.data ? (
        <Skeleton variant="rounded" height={120} />
      ) : history.data.length === 0 ? (
        <Typography color="text.secondary">No subscriptions yet.</Typography>
      ) : (
        <Stack spacing={1}>
          {history.data.map((h) => (
            <Card key={h.id} variant="outlined">
              <CardContent className="flex flex-wrap items-center gap-x-4 gap-y-1" sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Typography sx={{ fontWeight: 600 }} className="flex-1 min-w-40">
                  {h.planName}
                  {h.renewedFromId && (
                    <Typography component="span" variant="body2" color="text.secondary">
                      {' '}
                      (renewal)
                    </Typography>
                  )}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {h.startDate ? `${formatDate(h.startDate)} – ${formatDate(h.endDate)}` : formatDate(h.createdAt)}
                </Typography>
                <Typography variant="body2">{formatMoney(h.priceInMajorUnits, h.currency)}</Typography>
                <StatusChip sub={h} />
              </CardContent>
            </Card>
          ))}
        </Stack>
      )}

      {cancelOpen && sub && <CancelDialog sub={sub} onClose={() => setCancelOpen(false)} onDone={reloadAll} />}
    </Stack>
  )
}
