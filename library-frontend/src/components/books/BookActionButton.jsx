import { useEffect, useState } from 'react'
import { Button, Skeleton, Stack, Typography } from '@mui/material'
import { useSelector } from 'react-redux'
import { Link as RouterLink, useLocation } from 'react-router-dom'
import { checkoutBook } from '../../api/loans.js'
import { reserveBook } from '../../api/reservations.js'
import { checkSubscription } from '../../api/subscriptions.js'
import { selectIsLoggedIn } from '../../store/authSlice.js'
import { formatDate } from '../../utils/format.js'
import { useToast } from '../feedback/toastContext.js'

/**
 * The main button on a book page. It adapts:
 *   not logged in            -> "Log in to borrow"
 *   no valid subscription    -> "Subscribe to borrow" (plans page, step 18)
 *   copies available         -> "Borrow"
 *   no copy available        -> "Reserve" (join the waiting list)
 *
 * Messages go to the app's toast; onChanged() reloads the book
 * (available copies change after borrowing).
 */
export default function BookActionButton({ book, onChanged }) {
  const onDone = useToast()
  const loggedIn = useSelector(selectIsLoggedIn)
  const location = useLocation()
  const [hasSubscription, setHasSubscription] = useState(null) // null = still checking
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!loggedIn) return
    let cancelled = false
    checkSubscription()
      .then((r) => !cancelled && setHasSubscription(Boolean(r.success)))
      .catch(() => !cancelled && setHasSubscription(false))
    return () => {
      cancelled = true
    }
  }, [loggedIn])

  if (!loggedIn) {
    return (
      <Button component={RouterLink} to="/login" state={{ from: location }} variant="contained" size="large">
        Log in to borrow
      </Button>
    )
  }

  if (hasSubscription === null) return <Skeleton variant="rounded" width={180} height={42} />

  if (!hasSubscription) {
    return (
      <Stack spacing={1} sx={{ alignItems: 'flex-start' }}>
        <Button component={RouterLink} to="/plans" variant="contained" size="large">
          Subscribe to borrow
        </Button>
        <Typography variant="body2" color="text.secondary">
          You need an active subscription to borrow or reserve books.
        </Typography>
      </Stack>
    )
  }

  const run = async (fn) => {
    setBusy(true)
    try {
      await fn()
      onChanged?.()
    } catch (e) {
      onDone(e.message, 'error')
    } finally {
      setBusy(false)
    }
  }

  if (book.availableCopies > 0) {
    return (
      <Button
        variant="contained"
        size="large"
        disabled={busy}
        onClick={() =>
          run(async () => {
            const loan = await checkoutBook(book.id)
            onDone(`Borrowed! Please return it by ${formatDate(loan.dueDate)}.`, 'success', '/loans')
          })
        }
      >
        {busy ? 'Borrowing…' : 'Borrow'}
      </Button>
    )
  }

  return (
    <Stack spacing={1} sx={{ alignItems: 'flex-start' }}>
      <Button
        variant="contained"
        color="secondary"
        size="large"
        disabled={busy}
        onClick={() =>
          run(async () => {
            await reserveBook(book.id)
            onDone("Reserved! We'll email you when a copy is ready for you.", 'success', '/reservations')
          })
        }
      >
        {busy ? 'Reserving…' : 'Reserve'}
      </Button>
      <Typography variant="body2" color="text.secondary">
        All copies are out. Reserve to join the waiting list.
      </Typography>
    </Stack>
  )
}
