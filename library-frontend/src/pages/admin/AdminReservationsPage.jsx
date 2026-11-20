import { useCallback, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Link,
  MenuItem,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import { Link as RouterLink } from 'react-router-dom'
import * as admin from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import { RESERVATION_STATUSES, ReservationStatusChip, statusLabel } from '../../components/admin/adminLabels.jsx'
import { BookPicker, UserPicker } from '../../components/admin/Pickers.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatDate, formatDateTime } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const PAGE_SIZE = 15

function HoldDialog({ onDone, onClose }) {
  const [user, setUser] = useState(null)
  const [book, setBook] = useState(null)
  const [notes, setNotes] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      await admin.reserveForUser(user.id, { bookId: book.id, notes: notes.trim() || undefined })
      onDone()
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>Place a hold for a member</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <UserPicker value={user} onChange={setUser} required />
          <BookPicker value={book} onChange={setBook} required />
          <TextField label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} slotProps={{ htmlInput: { maxLength: 500 } }} />
          <Typography variant="caption" color="text.secondary">
            The member joins the waiting list for this book and gets an email when a copy is ready.
          </Typography>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy || !user || !book}>
          {busy ? 'Saving…' : 'Place hold'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default function AdminReservationsPage() {
  const toast = useToast()
  const [filters, setFilters] = useState({ user: null, book: null, status: '', activeOnly: true })
  const [page, setPage] = useState(0)
  const setFilter = (k, v) => {
    setFilters((f) => ({ ...f, [k]: v }))
    setPage(0)
  }

  const load = useCallback(
    () =>
      admin.searchReservations({
        userId: filters.user?.id,
        bookId: filters.book?.id,
        status: filters.status,
        activeOnly: filters.activeOnly || undefined,
        page,
        size: PAGE_SIZE,
      }),
    [filters, page],
  )
  const reservations = useAsync(load)
  const [holding, setHolding] = useState(false)

  const columns = [
    { key: 'id', label: '#' },
    {
      key: 'user',
      label: 'Member',
      render: (r) => (
        <Link component={RouterLink} to={`/admin/users/${r.userId}`} underline="hover" className="whitespace-nowrap">
          {r.userFullName}
        </Link>
      ),
    },
    {
      key: 'book',
      label: 'Book',
      render: (r) => (
        <Link component={RouterLink} to={`/books/${r.bookId}`} underline="hover">
          {r.bookTitle}
        </Link>
      ),
    },
    { key: 'reservedAt', label: 'Reserved', render: (r) => <span className="whitespace-nowrap">{formatDate(r.reservedAt)}</span> },
    { key: 'status', label: 'Status', render: (r) => <ReservationStatusChip status={r.status} /> },
    {
      key: 'queue',
      label: 'Queue / held until',
      render: (r) =>
        r.status === 'AVAILABLE' && r.availableUntil
          ? `Held until ${formatDateTime(r.availableUntil)}`
          : r.status === 'PENDING' && r.queuePosition != null
            ? `#${r.queuePosition} in line`
            : '',
    },
  ]

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PageTitle>Reservations</PageTitle>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setHolding(true)}>
          Place a hold
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-4 items-center">
        <UserPicker size="small" label="Member" value={filters.user} onChange={(v) => setFilter('user', v)} />
        <BookPicker size="small" value={filters.book} onChange={(v) => setFilter('book', v)} />
        <TextField select size="small" label="Status" value={filters.status} onChange={(e) => setFilter('status', e.target.value)}>
          <MenuItem value="">All</MenuItem>
          {RESERVATION_STATUSES.map((s) => (
            <MenuItem key={s} value={s}>
              {statusLabel.RESERVATION?.[s]?.label ?? s}
            </MenuItem>
          ))}
        </TextField>
        <FormControlLabel
          control={<Switch checked={filters.activeOnly} onChange={(e) => setFilter('activeOnly', e.target.checked)} />}
          label="Waiting or ready only"
        />
      </div>

      <AdminTable
        columns={columns}
        rows={reservations.data?.content}
        loading={reservations.loading}
        error={reservations.error}
        empty="No reservations match."
        paging={reservations.data && { page, size: PAGE_SIZE, total: reservations.data.totalElements, onPageChange: setPage }}
      />

      {holding && (
        <HoldDialog
          onClose={() => setHolding(false)}
          onDone={() => {
            setHolding(false)
            toast('Hold placed')
            reservations.reload()
          }}
        />
      )}
    </Stack>
  )
}
