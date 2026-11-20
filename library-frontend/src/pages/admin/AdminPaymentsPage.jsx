import { useCallback, useState } from 'react'
import { Link, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import * as admin from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import StatCard from '../../components/admin/StatCard.jsx'
import { PAYMENT_STATUSES, PaymentStatusChip, statusLabel } from '../../components/admin/adminLabels.jsx'
import { UserPicker } from '../../components/admin/Pickers.jsx'
import { formatDateTime, formatMoney } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const PAGE_SIZE = 15
const TYPES = { SUBSCRIPTION: 'Subscription', FINE: 'Fine' }

export default function AdminPaymentsPage() {
  const [filters, setFilters] = useState({ user: null, status: '', type: '' })
  const [page, setPage] = useState(0)
  const setFilter = (k, v) => {
    setFilters((f) => ({ ...f, [k]: v }))
    setPage(0)
  }

  const load = useCallback(
    () => admin.searchPayments({ userId: filters.user?.id, status: filters.status, type: filters.type, page, size: PAGE_SIZE }),
    [filters, page],
  )
  const payments = useAsync(load)
  const stats = useAsync(admin.getPaymentStats)
  const s = stats.data
  const byStatus = s?.paymentsByStatus || {}

  const columns = [
    {
      key: 'paymentId',
      label: 'Payment',
      render: (p) => (
        <div className="min-w-48">
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {p.description}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {p.paymentId}
          </Typography>
        </div>
      ),
    },
    {
      key: 'user',
      label: 'Member',
      render: (p) => (
        <Link component={RouterLink} to={`/admin/users/${p.userId}`} underline="hover">
          User #{p.userId}
        </Link>
      ),
    },
    { key: 'amount', label: 'Amount', align: 'right', render: (p) => <span className="whitespace-nowrap">{formatMoney(p.amountInMajorUnits, p.currency)}</span> },
    {
      key: 'status',
      label: 'Status',
      render: (p) => (
        <div className="whitespace-nowrap">
          <PaymentStatusChip status={p.status} />
          {p.lastError && p.status !== 'SUCCESS' && (
            <Typography variant="caption" component="p" color="error">
              {p.lastError}
            </Typography>
          )}
        </div>
      ),
    },
    { key: 'card', label: 'Card', render: (p) => (p.cardLast4 ? `•••• ${p.cardLast4}` : '') },
    { key: 'date', label: 'Date', render: (p) => <span className="whitespace-nowrap">{formatDateTime(p.completedAt || p.createdAt)}</span> },
  ]

  return (
    <Stack spacing={3}>
      <PageTitle>Payments</PageTitle>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard label="Collected in total" loading={!s} value={s && formatMoney(s.collectedTotal)} />
        <StatCard label="From subscriptions" loading={!s} value={s && formatMoney(s.collectedSubscriptions)} />
        <StatCard label="From fines (online)" loading={!s} value={s && formatMoney(s.collectedFines)} />
      </div>
      {s && (
        <Typography variant="body2" color="text.secondary">
          {PAYMENT_STATUSES.map((st) => `${byStatus[st] ?? 0} ${statusLabel.PAYMENT[st].label.toLowerCase()}`).join(' · ')}
        </Typography>
      )}

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
        <UserPicker size="small" label="Member" value={filters.user} onChange={(v) => setFilter('user', v)} />
        <TextField select size="small" label="Status" value={filters.status} onChange={(e) => setFilter('status', e.target.value)}>
          <MenuItem value="">All</MenuItem>
          {PAYMENT_STATUSES.map((st) => (
            <MenuItem key={st} value={st}>
              {statusLabel.PAYMENT[st].label}
            </MenuItem>
          ))}
        </TextField>
        <TextField select size="small" label="For" value={filters.type} onChange={(e) => setFilter('type', e.target.value)}>
          <MenuItem value="">All</MenuItem>
          {Object.entries(TYPES).map(([v, l]) => (
            <MenuItem key={v} value={v}>
              {l}
            </MenuItem>
          ))}
        </TextField>
      </div>

      <AdminTable
        columns={columns}
        rows={payments.data?.content}
        rowKey="paymentId"
        loading={payments.loading}
        error={payments.error}
        empty="No payments match."
        paging={payments.data && { page, size: PAGE_SIZE, total: payments.data.totalElements, onPageChange: setPage }}
      />
    </Stack>
  )
}
