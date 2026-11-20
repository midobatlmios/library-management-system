import { useCallback, useState } from 'react'
import { Alert, Button, Card, CardContent, Chip, Link, Skeleton, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom'
import { getMyFines, getMyUnpaidTotal, payFine } from '../../api/fines.js'
import { continueToPayment } from '../../utils/payment.js'
import EmptyState from '../../components/account/EmptyState.jsx'
import PagedList from '../../components/account/PagedList.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatDate, formatMoney } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const TYPE_LABEL = { OVERDUE: 'Late return', DAMAGE: 'Damaged book', LOST: 'Lost book', OTHER: 'Other fee' }
const STATUS = {
  PENDING: { label: 'Unpaid', color: 'error' },
  PARTIALLY_PAID: { label: 'Partly paid', color: 'warning' },
  PAID: { label: 'Paid', color: 'success' },
  WAIVED: { label: 'Waived', color: 'default' },
}
const isUnpaid = (f) => f.status === 'PENDING' || f.status === 'PARTIALLY_PAID'

function FineCard({ fine, paying, onPay }) {
  const status = STATUS[fine.status] ?? { label: fine.status, color: 'default' }
  return (
    <Card variant="outlined">
      <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <Stack spacing={0.5} className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <Typography sx={{ fontWeight: 600 }}>{TYPE_LABEL[fine.type] ?? fine.type}</Typography>
            <Chip size="small" color={status.color} label={status.label} />
          </div>
          {fine.bookTitle && (
            <Link component={RouterLink} to={`/books/${fine.bookId}`} underline="hover" variant="body2">
              {fine.bookTitle}
            </Link>
          )}
          {fine.reason && (
            <Typography variant="body2" color="text.secondary">
              {fine.reason}
            </Typography>
          )}
          <Typography variant="caption" color="text.secondary">
            {formatDate(fine.createdAt)}
            {fine.paidAt && ` · Paid ${formatDate(fine.paidAt)}`}
            {fine.waiverReason && ` · Waived: ${fine.waiverReason}`}
          </Typography>
        </Stack>
        <Stack spacing={1} sx={{ alignItems: { sm: 'flex-end' } }}>
          <Typography variant="h6" component="p">
            {formatMoney(fine.amountInMajorUnits, fine.currency)}
          </Typography>
          {fine.status === 'PARTIALLY_PAID' && (
            <Typography variant="caption" color="text.secondary">
              {formatMoney(fine.amountOutstandingInMajorUnits, fine.currency)} left to pay
            </Typography>
          )}
          {isUnpaid(fine) && (
            <Button variant="contained" onClick={() => onPay(fine)} disabled={paying}>
              {paying ? 'Opening payment…' : 'Pay'}
            </Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  )
}

/** /fines — the total still to pay, and every fine with a Pay button. */
export default function FinesPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const page = Math.max(1, Number(params.get('page')) || 1)

  const loadFines = useCallback(() => getMyFines({ page: page - 1 }), [page])
  const fines = useAsync(loadFines)
  const total = useAsync(getMyUnpaidTotal) // a module function never changes, so it loads once
  const [payingId, setPayingId] = useState(null)

  const handlePay = async (fine) => {
    setPayingId(fine.id)
    try {
      const payment = await payFine(fine.id)
      // Off to the payment page; it comes back to /payment/result.
      continueToPayment(payment, { navigate, toast, fallback: '/fines' })
      return
    } catch (e) {
      toast(e.message, 'error')
    }
    setPayingId(null)
  }

  const unpaid = total.data ? Number(total.data.totalInMajorUnits) : 0

  return (
    <Stack spacing={3}>
      <PageTitle>My fines</PageTitle>

      <Card variant="outlined">
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            Total unpaid
          </Typography>
          {total.data ? (
            <Typography variant="h4" component="p" color={unpaid > 0 ? 'error' : 'text.primary'}>
              {formatMoney(total.data.totalInMajorUnits, total.data.currency)}
            </Typography>
          ) : (
            <Skeleton width={160} height={48} />
          )}
          {unpaid > 0 && (
            <Alert severity="warning" sx={{ mt: 1 }}>
              You can't borrow books while you have unpaid fines.
            </Alert>
          )}
        </CardContent>
      </Card>

      <PagedList
        data={fines.data}
        error={fines.error}
        loading={fines.loading}
        page={page}
        onPageChange={(p) => setParams({ page: p })}
        renderItem={(f) => <FineCard key={f.id} fine={f} paying={payingId === f.id} onPay={handlePay} />}
        empty={<EmptyState title="No fines" text="Return your books on time and it stays that way." />}
      />
    </Stack>
  )
}
