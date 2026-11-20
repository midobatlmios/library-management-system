import { useCallback, useState } from 'react'
import { Button, Card, CardContent, Chip, Link, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import { getMyLoans, renewLoan, returnLoan } from '../../api/loans.js'
import EmptyState from '../../components/account/EmptyState.jsx'
import PagedList from '../../components/account/PagedList.jsx'
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { daysFromToday, formatDate } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

/** Green "Due in 5 days", orange "Due today", red "3 days late", grey "Returned". */
function LoanStatusChip({ loan }) {
  if (loan.status === 'RETURNED') {
    return loan.overdueDays > 0 ? (
      <Chip size="small" label={`Returned ${loan.overdueDays} day${loan.overdueDays === 1 ? '' : 's'} late`} />
    ) : (
      <Chip size="small" label="Returned" />
    )
  }
  if (loan.isOverdue || loan.status === 'OVERDUE') {
    const late = loan.overdueDays || -daysFromToday(loan.dueDate)
    return <Chip size="small" color="error" label={`${late} day${late === 1 ? '' : 's'} late`} />
  }
  const days = daysFromToday(loan.dueDate)
  if (days === 0) return <Chip size="small" color="warning" label="Due today" />
  return (
    <Chip
      size="small"
      color={days <= 3 ? 'warning' : 'success'}
      variant="outlined"
      label={`Due in ${days} day${days === 1 ? '' : 's'}`}
    />
  )
}

function LoanCard({ loan, onRenew, onReturn }) {
  const active = loan.status !== 'RETURNED'
  const late = loan.isOverdue || loan.status === 'OVERDUE'
  return (
    <Card variant="outlined">
      <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <Stack spacing={0.5} className="flex-1 min-w-0">
          <Link component={RouterLink} to={`/books/${loan.bookId}`} underline="hover" sx={{ fontWeight: 600 }}>
            {loan.bookTitle}
          </Link>
          <Typography variant="body2" color="text.secondary">
            {loan.bookAuthor}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Borrowed {formatDate(loan.checkoutDate)} · {active ? 'Due' : 'Was due'} {formatDate(loan.dueDate)}
            {loan.returnDate && ` · Returned ${formatDate(loan.returnDate)}`}
          </Typography>
          <div className="flex flex-wrap items-center gap-2 pt-1">
            <LoanStatusChip loan={loan} />
            {active && (
              <Typography variant="caption" color="text.secondary">
                {loan.renewalsLeft > 0
                  ? `${loan.renewalsLeft} renewal${loan.renewalsLeft === 1 ? '' : 's'} left`
                  : 'No renewals left'}
              </Typography>
            )}
          </div>
          {late && (
            <Typography variant="body2" color="error">
              Please return it soon: a late fee is added when you return it.
            </Typography>
          )}
        </Stack>
        {active && (
          <div className="flex gap-2">
            <Button variant="outlined" onClick={() => onRenew(loan)} disabled={loan.renewalsLeft === 0}>
              Renew
            </Button>
            <Button variant="contained" onClick={() => onReturn(loan)}>
              Return
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

/** /loans — current loans (default) and history. Linked from the reminder emails. */
export default function LoansPage() {
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const view = params.get('view') === 'history' ? 'history' : 'current'
  const page = Math.max(1, Number(params.get('page')) || 1)

  const load = useCallback(
    () => getMyLoans({ status: view === 'current' ? 'ACTIVE' : 'RETURNED', page: page - 1 }),
    [view, page],
  )
  const { data, error, loading, reload } = useAsync(load)
  const [returning, setReturning] = useState(null) // the loan in the confirm dialog

  const handleRenew = async (loan) => {
    try {
      const updated = await renewLoan(loan.id)
      toast(`Renewed. New due date: ${formatDate(updated.dueDate)}`)
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const handleReturn = async () => {
    try {
      const returned = await returnLoan(returning.id)
      if (returned.overdueDays > 0) {
        toast(
          `Returned ${returned.overdueDays} day${returned.overdueDays === 1 ? '' : 's'} late. A late fee was added.`,
          'warning',
          '/fines',
        )
      } else {
        toast('Returned. Thank you!')
      }
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PageTitle>My loans</PageTitle>
        <ToggleButtonGroup
          size="small"
          exclusive
          value={view}
          onChange={(_, v) => v && setParams(v === 'history' ? { view: 'history' } : {})}
        >
          <ToggleButton value="current">Current</ToggleButton>
          <ToggleButton value="history">History</ToggleButton>
        </ToggleButtonGroup>
      </div>

      <PagedList
        data={data}
        error={error}
        loading={loading}
        page={page}
        onPageChange={(p) => setParams(view === 'history' ? { view, page: p } : { page: p })}
        renderItem={(loan) => <LoanCard key={loan.id} loan={loan} onRenew={handleRenew} onReturn={setReturning} />}
        empty={
          view === 'current' ? (
            <EmptyState
              title="No books borrowed right now"
              text="Find something to read in the catalog."
              actionLabel="Browse the catalog"
              actionTo="/books"
            />
          ) : (
            <EmptyState title="No returned books yet" />
          )
        }
      />

      <ConfirmDialog
        open={Boolean(returning)}
        title="Return this book?"
        message={returning && `"${returning.bookTitle}" will be marked as returned today.`}
        confirmLabel="Return"
        cancelLabel="Cancel"
        onConfirm={handleReturn}
        onClose={() => setReturning(null)}
      />
    </Stack>
  )
}
