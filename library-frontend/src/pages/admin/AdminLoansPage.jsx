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
import { LoanStatusChip } from '../../components/admin/adminLabels.jsx'
import CreateFineDialog from '../../components/admin/CreateFineDialog.jsx'
import { BookPicker, UserPicker } from '../../components/admin/Pickers.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatDate } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const PAGE_SIZE = 15
const STATUS_FILTERS = { '': 'All', ACTIVE: 'Not returned', CHECKED_OUT: 'Checked out', OVERDUE: 'Overdue', RETURNED: 'Returned' }

function IssueDialog({ onDone, onClose }) {
  const [user, setUser] = useState(null)
  const [book, setBook] = useState(null)
  const [dueDays, setDueDays] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      const loan = await admin.issueBook(user.id, {
        bookId: book.id,
        dueDays: dueDays ? Number(dueDays) : undefined,
        notes: notes.trim() || undefined,
      })
      onDone(loan)
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>Issue a book</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <UserPicker value={user} onChange={setUser} required />
          <BookPicker value={book} onChange={setBook} required />
          <TextField
            label="Loan length in days (optional)"
            type="number"
            value={dueDays}
            onChange={(e) => setDueDays(e.target.value)}
            helperText="Empty: the member's plan decides"
            slotProps={{ htmlInput: { min: 1 } }}
          />
          <TextField label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} slotProps={{ htmlInput: { maxLength: 500 } }} />
          <Typography variant="caption" color="text.secondary">
            The same rules as online apply: the member needs a valid subscription, a free slot in their plan, no late
            books and no unpaid fines.
          </Typography>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy || !user || !book}>
          {busy ? 'Issuing…' : 'Issue book'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function EditLoanDialog({ loan, onDone, onClose }) {
  const [dueDate, setDueDate] = useState(loan.dueDate)
  const [maxRenewals, setMaxRenewals] = useState(loan.maxRenewals ?? '')
  const [notes, setNotes] = useState(loan.notes ?? '')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      await admin.updateLoan(loan.id, {
        dueDate,
        maxRenewals: maxRenewals === '' ? null : Number(maxRenewals),
        notes: notes.trim() || null,
      })
      onDone()
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="xs">
      <DialogTitle>Edit loan #{loan.id}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Typography variant="body2">
            {loan.userFullName} · {loan.bookTitle}
          </Typography>
          <TextField
            label="Due date"
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            label="Max renewals"
            type="number"
            value={maxRenewals}
            onChange={(e) => setMaxRenewals(e.target.value)}
            helperText={`Used ${loan.renewalCount ?? 0} so far`}
            slotProps={{ htmlInput: { min: 0 } }}
          />
          <TextField label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} multiline minRows={2} slotProps={{ htmlInput: { maxLength: 500 } }} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy || !dueDate}>
          {busy ? 'Saving…' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default function AdminLoansPage() {
  const toast = useToast()
  const [filters, setFilters] = useState({ user: null, book: null, status: 'ACTIVE', overdueOnly: false })
  const [page, setPage] = useState(0)
  const setFilter = (k, v) => {
    setFilters((f) => ({ ...f, [k]: v }))
    setPage(0)
  }

  const load = useCallback(
    () =>
      admin.searchLoans({
        userId: filters.user?.id,
        bookId: filters.book?.id,
        status: filters.status,
        overdueOnly: filters.overdueOnly || undefined,
        page,
        size: PAGE_SIZE,
        sortBy: 'dueDate',
        sortDirection: 'ASC',
      }),
    [filters, page],
  )
  const loans = useAsync(load)

  const [issuing, setIssuing] = useState(false)
  const [editing, setEditing] = useState(null)
  const [fining, setFining] = useState(null)
  const [runningJob, setRunningJob] = useState(false)

  const handleRunOverdue = async () => {
    setRunningJob(true)
    try {
      const r = await admin.runOverdueUpdate()
      toast(r.message ?? 'Done')
      loans.reload()
    } catch (e) {
      toast(e.message, 'error')
    } finally {
      setRunningJob(false)
    }
  }

  const columns = [
    { key: 'id', label: '#' },
    {
      key: 'user',
      label: 'Member',
      render: (l) => (
        <Link component={RouterLink} to={`/admin/users/${l.userId}`} underline="hover" className="whitespace-nowrap">
          {l.userFullName}
        </Link>
      ),
    },
    {
      key: 'book',
      label: 'Book',
      render: (l) => (
        <Link component={RouterLink} to={`/books/${l.bookId}`} underline="hover">
          {l.bookTitle}
        </Link>
      ),
    },
    { key: 'checkoutDate', label: 'Borrowed', render: (l) => <span className="whitespace-nowrap">{formatDate(l.checkoutDate)}</span> },
    { key: 'dueDate', label: 'Due', render: (l) => <span className="whitespace-nowrap">{formatDate(l.dueDate)}</span> },
    {
      key: 'status',
      label: 'Status',
      render: (l) => (
        <div className="flex items-center gap-1 whitespace-nowrap">
          <LoanStatusChip status={l.status} />
          {l.overdueDays > 0 && <Typography variant="caption">{l.overdueDays} d late</Typography>}
        </div>
      ),
    },
    {
      key: 'actions',
      label: '',
      align: 'right',
      render: (l) => (
        <div className="flex justify-end gap-1 whitespace-nowrap">
          {l.status !== 'RETURNED' && (
            <Button size="small" onClick={() => setEditing(l)}>
              Edit
            </Button>
          )}
          <Button size="small" color="warning" onClick={() => setFining(l)}>
            Fine
          </Button>
        </div>
      ),
    },
  ]

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PageTitle>Loans</PageTitle>
        <div className="flex flex-wrap gap-2">
          <Button variant="outlined" onClick={handleRunOverdue} disabled={runningJob}>
            {runningJob ? 'Updating…' : 'Mark overdue loans now'}
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setIssuing(true)}>
            Issue a book
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-4 items-center">
        <UserPicker size="small" label="Member" value={filters.user} onChange={(v) => setFilter('user', v)} />
        <BookPicker size="small" value={filters.book} onChange={(v) => setFilter('book', v)} />
        <TextField select size="small" label="Status" value={filters.status} onChange={(e) => setFilter('status', e.target.value)}>
          {Object.entries(STATUS_FILTERS).map(([v, l]) => (
            <MenuItem key={v} value={v}>
              {l}
            </MenuItem>
          ))}
        </TextField>
        <FormControlLabel
          control={<Switch checked={filters.overdueOnly} onChange={(e) => setFilter('overdueOnly', e.target.checked)} />}
          label="Late only"
        />
      </div>

      <AdminTable
        columns={columns}
        rows={loans.data?.content}
        loading={loans.loading}
        error={loans.error}
        empty="No loans match."
        paging={loans.data && { page, size: PAGE_SIZE, total: loans.data.totalElements, onPageChange: setPage }}
      />

      {issuing && (
        <IssueDialog
          onClose={() => setIssuing(false)}
          onDone={(loan) => {
            setIssuing(false)
            toast(`Issued. Due ${formatDate(loan.dueDate)}.`)
            loans.reload()
          }}
        />
      )}
      {editing && (
        <EditLoanDialog
          key={editing.id}
          loan={editing}
          onClose={() => setEditing(null)}
          onDone={() => {
            setEditing(null)
            toast('Loan updated')
            loans.reload()
          }}
        />
      )}
      {fining && (
        <CreateFineDialog
          key={fining.id}
          loan={fining}
          onClose={() => setFining(null)}
          onDone={() => {
            setFining(null)
            toast('Fine created', 'success', '/admin/fines')
          }}
        />
      )}
    </Stack>
  )
}
