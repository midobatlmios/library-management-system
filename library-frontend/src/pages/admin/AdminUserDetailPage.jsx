import { useCallback, useState } from 'react'
import { Alert, Box, Breadcrumbs, Card, CardContent, Chip, Link, Skeleton, Stack, Tab, Tabs, Typography } from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'
import * as admin from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import {
  FINE_TYPES,
  FineStatusChip,
  LoanStatusChip,
  SubscriptionStatusChip,
} from '../../components/admin/adminLabels.jsx'
import { formatDate, formatDateTime, formatMoney } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'

const PAGE_SIZE = 10
const bookLink = (id, title) => (
  <Link component={RouterLink} to={`/books/${id}`} underline="hover">
    {title}
  </Link>
)

function LoansTab({ userId }) {
  const [page, setPage] = useState(0)
  const load = useCallback(() => admin.getUserLoans(userId, { page, size: PAGE_SIZE }), [userId, page])
  const { data, error, loading } = useAsync(load)
  return (
    <AdminTable
      rows={data?.content}
      loading={loading}
      error={error}
      empty="No loans."
      paging={data && { page, size: PAGE_SIZE, total: data.totalElements, onPageChange: setPage }}
      columns={[
        { key: 'book', label: 'Book', render: (l) => bookLink(l.bookId, l.bookTitle) },
        { key: 'checkoutDate', label: 'Borrowed', render: (l) => formatDate(l.checkoutDate) },
        { key: 'dueDate', label: 'Due', render: (l) => formatDate(l.dueDate) },
        { key: 'returnDate', label: 'Returned', render: (l) => formatDate(l.returnDate) || '—' },
        { key: 'status', label: 'Status', render: (l) => <LoanStatusChip status={l.status} /> },
        { key: 'late', label: 'Days late', align: 'right', render: (l) => l.overdueDays || '' },
      ]}
    />
  )
}

function SubscriptionsTab({ userId }) {
  const load = useCallback(() => admin.getUserSubscriptions(userId), [userId])
  const { data, error, loading } = useAsync(load)
  return (
    <AdminTable
      rows={data}
      loading={loading}
      error={error}
      empty="No subscriptions."
      columns={[
        { key: 'planName', label: 'Plan' },
        { key: 'period', label: 'Period', render: (s) => (s.startDate ? `${formatDate(s.startDate)} – ${formatDate(s.endDate)}` : '—') },
        { key: 'price', label: 'Price', render: (s) => formatMoney(s.priceInMajorUnits, s.currency) },
        { key: 'status', label: 'Status', render: (s) => <SubscriptionStatusChip status={s.status} /> },
        { key: 'days', label: 'Days left', align: 'right', render: (s) => (s.status === 'ACTIVE' ? s.daysRemaining : '') },
      ]}
    />
  )
}

function FinesTab({ userId }) {
  const [page, setPage] = useState(0)
  const load = useCallback(() => admin.searchFines({ userId, page, size: PAGE_SIZE }), [userId, page])
  const { data, error, loading } = useAsync(load)
  return (
    <AdminTable
      rows={data?.content}
      loading={loading}
      error={error}
      empty="No fines."
      paging={data && { page, size: PAGE_SIZE, total: data.totalElements, onPageChange: setPage }}
      columns={[
        { key: 'type', label: 'Type', render: (f) => FINE_TYPES[f.type] ?? f.type },
        { key: 'book', label: 'Book', render: (f) => (f.bookId ? bookLink(f.bookId, f.bookTitle) : '—') },
        { key: 'amount', label: 'Amount', render: (f) => formatMoney(f.amountInMajorUnits, f.currency) },
        { key: 'left', label: 'Left to pay', render: (f) => formatMoney(f.amountOutstandingInMajorUnits, f.currency) },
        { key: 'status', label: 'Status', render: (f) => <FineStatusChip status={f.status} /> },
        { key: 'createdAt', label: 'Date', render: (f) => formatDate(f.createdAt) },
      ]}
    />
  )
}

function WishlistTab({ userId }) {
  const [page, setPage] = useState(0)
  const load = useCallback(() => admin.getUserWishlist(userId, { page, size: PAGE_SIZE }), [userId, page])
  const { data, error, loading } = useAsync(load)
  return (
    <AdminTable
      rows={data?.content}
      loading={loading}
      error={error}
      empty="Wishlist is empty."
      paging={data && { page, size: PAGE_SIZE, total: data.totalElements, onPageChange: setPage }}
      columns={[
        { key: 'book', label: 'Book', render: (w) => bookLink(w.bookId, w.bookTitle) },
        { key: 'bookAuthor', label: 'Author' },
        { key: 'notes', label: 'Notes', render: (w) => w.notes || '' },
        { key: 'addedAt', label: 'Added', render: (w) => formatDate(w.addedAt) },
      ]}
    />
  )
}

const TABS = { loans: LoansTab, subscriptions: SubscriptionsTab, fines: FinesTab, wishlist: WishlistTab }

/** /admin/users/:id — one user's details with their loans, subscriptions, fines and wishlist. */
export default function AdminUserDetailPage() {
  const { id } = useParams()
  const userId = Number(id)
  const load = useCallback(() => admin.getUser(userId), [userId])
  const { data: user, error } = useAsync(load)
  const [tab, setTab] = useState('loans')
  const TabContent = TABS[tab]

  return (
    <Stack spacing={3}>
      <Breadcrumbs>
        <Link component={RouterLink} to="/admin/users" underline="hover" color="inherit">
          Users
        </Link>
        <Typography color="text.primary">{user?.fullName ?? `User ${id}`}</Typography>
      </Breadcrumbs>

      {error && <Alert severity="error">{error}</Alert>}
      {!user && !error ? (
        <Skeleton variant="rounded" height={120} />
      ) : (
        user && (
          <Card variant="outlined">
            <CardContent>
              <div className="flex flex-wrap items-center gap-2">
                <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
                  {user.fullName}
                </Typography>
                {user.role === 'ROLE_ADMIN' && <Chip size="small" color="secondary" label="Admin" />}
                {user.verified ? (
                  <Chip size="small" color="success" variant="outlined" label="Email verified" />
                ) : (
                  <Chip size="small" color="warning" label="Email not verified" />
                )}
              </div>
              <Typography color="text.secondary">{user.email}</Typography>
              <Typography variant="body2" color="text.secondary">
                {user.phone ? `${user.phone} · ` : ''}
                {user.authProvider === 'GOOGLE' ? 'Google sign-in' : 'Email and password'} · Joined {formatDate(user.createdAt)}
                {user.lastLogin && ` · Last login ${formatDateTime(user.lastLogin)}`}
              </Typography>
            </CardContent>
          </Card>
        )
      )}

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" allowScrollButtonsMobile>
          <Tab value="loans" label="Loans" />
          <Tab value="subscriptions" label="Subscriptions" />
          <Tab value="fines" label="Fines" />
          <Tab value="wishlist" label="Wishlist" />
        </Tabs>
      </Box>
      <TabContent userId={userId} />
    </Stack>
  )
}
