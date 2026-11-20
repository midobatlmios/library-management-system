import { useCallback, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  Divider,
  Link,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material'
import CheckIcon from '@mui/icons-material/Check'
import { useSelector } from 'react-redux'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
import { getActiveSubscription, getPlans, subscribe } from '../../api/subscriptions.js'
import { useToast } from '../../components/feedback/toastContext.js'
import { selectIsLoggedIn, selectUser } from '../../store/authSlice.js'
import { formatMoney } from '../../utils/format.js'
import { continueToPayment } from '../../utils/payment.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const CYCLE = { MONTHLY: '/ month', YEARLY: '/ year', ONE_TIME: 'one time' }

function PlanCard({ plan, current, action }) {
  const free = Number(plan.price) === 0
  return (
    <Card
      variant="outlined"
      className="flex flex-col h-full"
      sx={current ? { borderColor: 'primary.main', borderWidth: 2 } : undefined}
    >
      <CardContent className="flex-1">
        <Stack spacing={2}>
          <div className="flex items-start justify-between gap-2">
            <Typography variant="h6" component="h2" sx={{ fontWeight: 700 }}>
              {plan.title}
            </Typography>
            {current && <Chip label="Your plan" color="primary" size="small" />}
          </div>
          <div>
            <Typography variant="h4" component="p" sx={{ fontWeight: 700 }}>
              {free ? 'Free' : formatMoney(plan.price, plan.currency)}
            </Typography>
            {!free && (
              <Typography variant="body2" color="text.secondary">
                {CYCLE[plan.billingCycle] ?? ''} · {plan.durationInDays} days of access
              </Typography>
            )}
          </div>
          {plan.trialDays > 0 && (
            <Chip label={`${plan.trialDays}-day free trial`} color="success" variant="outlined" size="small" sx={{ alignSelf: 'flex-start' }} />
          )}
          {plan.description && <Typography color="text.secondary">{plan.description}</Typography>}
          <Divider />
          <List dense disablePadding>
            {[
              `Borrow up to ${plan.maxBooksAllowed} book${plan.maxBooksAllowed === 1 ? '' : 's'} at a time`,
              `Keep each book up to ${plan.maxBorrowDays} days`,
              ...(plan.features || []),
            ].map((f) => (
              <ListItem key={f} disableGutters>
                <ListItemIcon sx={{ minWidth: 32 }}>
                  <CheckIcon color="success" fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={f} />
              </ListItem>
            ))}
          </List>
        </Stack>
      </CardContent>
      <CardActions sx={{ p: 2, pt: 0 }}>{action}</CardActions>
    </Card>
  )
}

/** /plans — public. Subscribing needs to be logged in with a verified email. */
export default function PlansPage() {
  const loggedIn = useSelector(selectIsLoggedIn)
  const user = useSelector(selectUser)
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()

  const plans = useAsync(getPlans)
  // Only asked when logged in; null = no subscription.
  const loadActive = useCallback(() => (loggedIn ? getActiveSubscription() : Promise.resolve(null)), [loggedIn])
  const active = useAsync(loadActive)
  const [busyPlanId, setBusyPlanId] = useState(null)

  const activeSub = active.data
  const hasActive = Boolean(activeSub)

  const handleSubscribe = async (plan) => {
    setBusyPlanId(plan.id)
    try {
      const payment = await subscribe(plan.id)
      continueToPayment(payment, { navigate, toast })
    } catch (e) {
      toast(e.message, 'error')
      setBusyPlanId(null)
    }
  }

  const actionFor = (plan) => {
    if (!loggedIn) {
      return (
        <Button fullWidth variant="contained" component={RouterLink} to="/login" state={{ from: location }}>
          Log in to subscribe
        </Button>
      )
    }
    if (activeSub?.planId === plan.id) {
      return (
        <Button fullWidth variant="outlined" component={RouterLink} to="/subscription">
          Manage my subscription
        </Button>
      )
    }
    return (
      <Button
        fullWidth
        variant="contained"
        onClick={() => handleSubscribe(plan)}
        disabled={busyPlanId !== null || hasActive || active.loading || !user?.verified}
      >
        {busyPlanId === plan.id ? 'Starting…' : Number(plan.price) === 0 ? 'Start for free' : 'Subscribe'}
      </Button>
    )
  }

  return (
    <Stack spacing={3}>
      <div>
        <PageTitle>Membership plans</PageTitle>
        <Typography color="text.secondary">Pick a plan to borrow and reserve books. Prices in MAD.</Typography>
      </div>

      {loggedIn && user && !user.verified && (
        <Alert
          severity="warning"
          action={
            <Button color="inherit" size="small" component={RouterLink} to="/check-email">
              Verify now
            </Button>
          }
        >
          Please confirm your email address before subscribing.
        </Alert>
      )}
      {hasActive && (
        <Alert severity="info">
          You already have an active subscription ({activeSub.planName}, {activeSub.daysRemaining} days left).{' '}
          <Link component={RouterLink} to="/subscription">
            Manage it
          </Link>{' '}
          to renew or cancel.
        </Alert>
      )}
      {plans.error && <Alert severity="error">{plans.error}</Alert>}

      {!plans.data ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} variant="rounded" height={360} />
          ))}
        </div>
      ) : plans.data.length === 0 ? (
        <Typography color="text.secondary">No plans are available right now.</Typography>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {plans.data.map((plan) => (
            <PlanCard key={plan.id} plan={plan} current={activeSub?.planId === plan.id} action={actionFor(plan)} />
          ))}
        </div>
      )}

      <Typography variant="body2" color="text.secondary">
        Payments use a simulated checkout: no real money moves. Test card 4242 4242 4242 4242, any future expiry, any CVC.
      </Typography>
    </Stack>
  )
}
