import { useCallback } from 'react'
import { Card, CardContent, Chip, Link, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import { getMyPayments } from '../../api/payments.js'
import EmptyState from '../../components/account/EmptyState.jsx'
import PagedList from '../../components/account/PagedList.jsx'
import { formatDateTime, formatMoney } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const STATUS = {
  SUCCESS: { label: 'Paid', color: 'success' },
  PENDING: { label: 'Waiting', color: 'warning' },
  FAILED: { label: 'Failed', color: 'error' },
  EXPIRED: { label: 'Expired', color: 'default' },
  CANCELLED: { label: 'Cancelled', color: 'default' },
}

/** /payments — every payment (subscriptions and fines), newest first. */
export default function PaymentsPage() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(1, Number(params.get('page')) || 1)
  const load = useCallback(() => getMyPayments({ page: page - 1 }), [page])
  const { data, error, loading } = useAsync(load)

  return (
    <Stack spacing={3}>
      <PageTitle>My payments</PageTitle>
      <PagedList
        data={data}
        error={error}
        loading={loading}
        page={page}
        onPageChange={(p) => setParams({ page: p })}
        skeletonHeight={72}
        renderItem={(p) => {
          const s = STATUS[p.status] ?? { label: p.status, color: 'default' }
          return (
            <Card key={p.paymentId} variant="outlined">
              <CardContent className="flex flex-wrap items-center gap-x-4 gap-y-1" sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
                <Stack className="flex-1 min-w-48">
                  {/* The result page shows the details and, if still waiting, lets you pay or cancel. */}
                  <Link
                    component={RouterLink}
                    to={`/payment/result?paymentId=${encodeURIComponent(p.paymentId)}`}
                    underline="hover"
                    sx={{ fontWeight: 600 }}
                  >
                    {p.description}
                  </Link>
                  <Typography variant="caption" color="text.secondary">
                    {formatDateTime(p.completedAt || p.createdAt)}
                    {p.cardLast4 && ` · Card •••• ${p.cardLast4}`}
                  </Typography>
                </Stack>
                <Typography sx={{ fontWeight: 600 }}>{formatMoney(p.amountInMajorUnits, p.currency)}</Typography>
                <Chip size="small" color={s.color} label={s.label} />
              </CardContent>
            </Card>
          )
        }}
        empty={<EmptyState title="No payments yet" />}
      />
    </Stack>
  )
}
