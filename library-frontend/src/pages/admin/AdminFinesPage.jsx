import { useCallback, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Link,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import { Link as RouterLink } from 'react-router-dom'
import * as admin from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import { FINE_STATUSES, FINE_TYPES, FineStatusChip, statusLabel } from '../../components/admin/adminLabels.jsx'
import CreateFineDialog from '../../components/admin/CreateFineDialog.jsx'
import { UserPicker } from '../../components/admin/Pickers.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatDate, formatMoney } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const PAGE_SIZE = 15
const isOpen = (f) => f.status === 'PENDING' || f.status === 'PARTIALLY_PAID'

/** Cash or card at the desk. Amount typed in MAD, sent in centimes; can be partial. */
function RecordPaymentDialog({ fine, onDone, onClose }) {
  const [amount, setAmount] = useState(String(fine.amountOutstandingInMajorUnits ?? ''))
  const [reference, setReference] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const centimes = Math.round(Number(amount) * 100)

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      await admin.recordFinePayment(fine.id, centimes, reference.trim() || undefined)
      onDone()
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="xs">
      <DialogTitle>Record a desk payment</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Typography variant="body2">
            {fine.userEmail} · {FINE_TYPES[fine.type]} · {formatMoney(fine.amountOutstandingInMajorUnits, fine.currency)} left to pay
          </Typography>
          <TextField
            label="Amount received (MAD)"
            type="number"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            slotProps={{ htmlInput: { min: 0.01, step: '0.01' } }}
            helperText="Less than the total records a partial payment"
          />
          <TextField label="Receipt / reference (optional)" value={reference} onChange={(e) => setReference(e.target.value)} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy || centimes < 1}>
          {busy ? 'Saving…' : 'Record payment'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function WaiveDialog({ fine, onDone, onClose }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      await admin.waiveFine(fine.id, reason.trim() || undefined)
      onDone()
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="xs">
      <DialogTitle>Waive this fine?</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Typography variant="body2">
            The member will no longer owe {formatMoney(fine.amountOutstandingInMajorUnits, fine.currency)}.
          </Typography>
          <TextField
            label="Reason (the member sees it)"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            multiline
            minRows={2}
            slotProps={{ htmlInput: { maxLength: 500 } }}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" color="warning" onClick={handleSubmit} disabled={busy}>
          {busy ? 'Saving…' : 'Waive fine'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default function AdminFinesPage() {
  const toast = useToast()
  const [filters, setFilters] = useState({ user: null, status: '', type: '' })
  const [page, setPage] = useState(0)
  const setFilter = (k, v) => {
    setFilters((f) => ({ ...f, [k]: v }))
    setPage(0)
  }

  const load = useCallback(
    () => admin.searchFines({ userId: filters.user?.id, status: filters.status, type: filters.type, page, size: PAGE_SIZE }),
    [filters, page],
  )
  const fines = useAsync(load)
  const outstanding = useAsync(admin.getFinesOutstanding)
  const collected = useAsync(admin.getFinesCollected)

  const [creating, setCreating] = useState(false)
  const [paying, setPaying] = useState(null)
  const [waiving, setWaiving] = useState(null)

  const done = (message) => () => {
    setCreating(false)
    setPaying(null)
    setWaiving(null)
    toast(message)
    fines.reload()
    outstanding.reload()
    collected.reload()
  }

  const columns = [
    { key: 'id', label: '#' },
    {
      key: 'user',
      label: 'Member',
      render: (f) => (
        <Link component={RouterLink} to={`/admin/users/${f.userId}`} underline="hover">
          {f.userEmail}
        </Link>
      ),
    },
    { key: 'type', label: 'Type', render: (f) => FINE_TYPES[f.type] ?? f.type },
    { key: 'book', label: 'Book', render: (f) => f.bookTitle ?? '—' },
    {
      key: 'amount',
      label: 'Amount',
      render: (f) => (
        <div className="whitespace-nowrap">
          {formatMoney(f.amountInMajorUnits, f.currency)}
          {f.status === 'PARTIALLY_PAID' && (
            <Typography variant="caption" component="p" color="text.secondary">
              {formatMoney(f.amountOutstandingInMajorUnits, f.currency)} left
            </Typography>
          )}
        </div>
      ),
    },
    { key: 'status', label: 'Status', render: (f) => <FineStatusChip status={f.status} /> },
    { key: 'createdAt', label: 'Date', render: (f) => <span className="whitespace-nowrap">{formatDate(f.createdAt)}</span> },
    {
      key: 'actions',
      label: '',
      align: 'right',
      render: (f) =>
        isOpen(f) && (
          <div className="flex justify-end gap-1 whitespace-nowrap">
            <Button size="small" onClick={() => setPaying(f)}>
              Record payment
            </Button>
            <Button size="small" color="warning" onClick={() => setWaiving(f)}>
              Waive
            </Button>
          </div>
        ),
    },
  ]

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PageTitle>Fines</PageTitle>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreating(true)}>
          New fine
        </Button>
      </div>
      {outstanding.data && collected.data && (
        <Typography color="text.secondary">
          {formatMoney(outstanding.data.totalInMajorUnits, outstanding.data.currency)} outstanding ·{' '}
          {formatMoney(collected.data.totalInMajorUnits, collected.data.currency)} collected
        </Typography>
      )}

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
        <UserPicker size="small" label="Member" value={filters.user} onChange={(v) => setFilter('user', v)} />
        <TextField select size="small" label="Status" value={filters.status} onChange={(e) => setFilter('status', e.target.value)}>
          <MenuItem value="">All</MenuItem>
          {FINE_STATUSES.map((s) => (
            <MenuItem key={s} value={s}>
              {statusLabel.FINE[s].label}
            </MenuItem>
          ))}
        </TextField>
        <TextField select size="small" label="Type" value={filters.type} onChange={(e) => setFilter('type', e.target.value)}>
          <MenuItem value="">All</MenuItem>
          {Object.entries(FINE_TYPES).map(([v, l]) => (
            <MenuItem key={v} value={v}>
              {l}
            </MenuItem>
          ))}
        </TextField>
      </div>

      <AdminTable
        columns={columns}
        rows={fines.data?.content}
        loading={fines.loading}
        error={fines.error}
        empty="No fines match."
        paging={fines.data && { page, size: PAGE_SIZE, total: fines.data.totalElements, onPageChange: setPage }}
      />

      {creating && <CreateFineDialog loan={null} onClose={() => setCreating(false)} onDone={done('Fine created')} />}
      {paying && <RecordPaymentDialog key={paying.id} fine={paying} onClose={() => setPaying(null)} onDone={done('Payment recorded')} />}
      {waiving && <WaiveDialog key={waiving.id} fine={waiving} onClose={() => setWaiving(null)} onDone={done('Fine waived')} />}
    </Stack>
  )
}
