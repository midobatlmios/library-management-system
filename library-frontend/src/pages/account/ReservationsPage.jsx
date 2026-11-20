import { useCallback, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  CardContent,
  Chip,
  Link,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import { cancelReservation, fulfillReservation, getMyReservations } from '../../api/reservations.js'
import Countdown from '../../components/account/Countdown.jsx'
import EmptyState from '../../components/account/EmptyState.jsx'
import PagedList from '../../components/account/PagedList.jsx'
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatDate, formatDateTime } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const STATUS = {
  PENDING: { label: 'Waiting', color: 'info' },
  AVAILABLE: { label: 'Ready to pick up', color: 'success' },
  FULFILLED: { label: 'Picked up', color: 'default' },
  EXPIRED: { label: 'Expired', color: 'default' },
  CANCELLED: { label: 'Cancelled', color: 'default' },
}

function ReservationCard({ reservation: r, onPickUp, onCancel }) {
  const status = STATUS[r.status] ?? { label: r.status, color: 'default' }
  const open = r.status === 'PENDING' || r.status === 'AVAILABLE'

  return (
    <Card variant="outlined">
      <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <Stack spacing={0.75} className="flex-1 min-w-0">
          <Link component={RouterLink} to={`/books/${r.bookId}`} underline="hover" sx={{ fontWeight: 600 }}>
            {r.bookTitle}
          </Link>
          <Typography variant="body2" color="text.secondary">
            Reserved {formatDate(r.reservedAt)}
            {r.fulfilledAt && ` · Picked up ${formatDate(r.fulfilledAt)}`}
            {r.cancelledAt && ` · Cancelled ${formatDate(r.cancelledAt)}`}
          </Typography>
          <div className="flex flex-wrap items-center gap-2">
            <Chip size="small" color={status.color} label={status.label} />
            {r.status === 'PENDING' && r.queuePosition != null && (
              <Typography variant="body2">
                {r.queuePosition === 1 ? "You're next in line" : `Position ${r.queuePosition} in the queue`}
              </Typography>
            )}
          </div>
          {r.status === 'AVAILABLE' && r.availableUntil && (
            <Alert severity="success" sx={{ py: 0 }}>
              A copy is held for you. Pick it up within <strong><Countdown until={r.availableUntil} /></strong>{' '}
              (until {formatDateTime(r.availableUntil)}).
            </Alert>
          )}
        </Stack>
        {open && (
          <div className="flex gap-2">
            <Button variant="outlined" color="error" onClick={() => onCancel(r)}>
              Cancel
            </Button>
            {r.status === 'AVAILABLE' && (
              <Button variant="contained" onClick={() => onPickUp(r)}>
                Pick up
              </Button>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

/** /reservations — waiting list entries. Linked from the "your book is ready" email. */
export default function ReservationsPage() {
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const view = params.get('view') === 'all' ? 'all' : 'active'
  const page = Math.max(1, Number(params.get('page')) || 1)

  const load = useCallback(
    () => getMyReservations({ activeOnly: view === 'active' ? true : undefined, page: page - 1 }),
    [view, page],
  )
  const { data, error, loading, reload } = useAsync(load)
  const [cancelling, setCancelling] = useState(null)

  const handlePickUp = async (r) => {
    try {
      await fulfillReservation(r.id)
      toast(`"${r.bookTitle}" is now on loan to you.`, 'success', '/loans')
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const handleCancel = async () => {
    try {
      await cancelReservation(cancelling.id)
      toast('Reservation cancelled', 'info')
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PageTitle>My reservations</PageTitle>
        <ToggleButtonGroup
          size="small"
          exclusive
          value={view}
          onChange={(_, v) => v && setParams(v === 'all' ? { view: 'all' } : {})}
        >
          <ToggleButton value="active">Active</ToggleButton>
          <ToggleButton value="all">All</ToggleButton>
        </ToggleButtonGroup>
      </div>

      <PagedList
        data={data}
        error={error}
        loading={loading}
        page={page}
        onPageChange={(p) => setParams(view === 'all' ? { view, page: p } : { page: p })}
        renderItem={(r) => (
          <ReservationCard key={r.id} reservation={r} onPickUp={handlePickUp} onCancel={setCancelling} />
        )}
        empty={
          <EmptyState
            title={view === 'active' ? 'No active reservations' : 'No reservations yet'}
            text="When all copies of a book are out, reserve it from its page to join the waiting list."
            actionLabel="Browse the catalog"
            actionTo="/books"
          />
        }
      />

      <ConfirmDialog
        open={Boolean(cancelling)}
        title="Cancel this reservation?"
        message={
          cancelling &&
          (cancelling.status === 'AVAILABLE'
            ? `The copy held for you will go to the next person waiting for "${cancelling.bookTitle}".`
            : `You will leave the waiting list for "${cancelling.bookTitle}".`)
        }
        confirmLabel="Cancel reservation"
        danger
        onConfirm={handleCancel}
        onClose={() => setCancelling(null)}
      />
    </Stack>
  )
}
