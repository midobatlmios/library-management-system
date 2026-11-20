import { useEffect, useState } from 'react'
import { Button, Typography } from '@mui/material'
import RateReviewIcon from '@mui/icons-material/RateReview'
import { useSelector } from 'react-redux'
import { canReviewBook, createReview } from '../../api/reviews.js'
import { selectIsLoggedIn } from '../../store/authSlice.js'
import { useToast } from '../feedback/toastContext.js'
import ReviewDialog from './ReviewDialog.jsx'

/**
 * "Write a review" on a book page. Only shown when the backend says this
 * user may review this book (GET /api/reviews/can-review/{bookId});
 * otherwise its reason is shown in small text. onCreated() reloads the reviews.
 */
export default function WriteReview({ book, onCreated }) {
  const loggedIn = useSelector(selectIsLoggedIn)
  const toast = useToast()
  const [eligibility, setEligibility] = useState(null)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!loggedIn) return
    let cancelled = false
    canReviewBook(book.id)
      .then((r) => !cancelled && setEligibility(r))
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [book.id, loggedIn])

  if (!loggedIn || !eligibility) return null

  if (!eligibility.canReview) {
    return eligibility.reason ? (
      <Typography variant="body2" color="text.secondary">
        {eligibility.reason}
      </Typography>
    ) : null
  }

  const handleSubmit = async (review) => {
    await createReview(book.id, review) // errors are shown inside the dialog
    setOpen(false)
    setEligibility({ canReview: false, reason: null }) // one review per book
    toast('Thanks! Your review is published.')
    onCreated()
  }

  return (
    <>
      <Button variant="outlined" startIcon={<RateReviewIcon />} onClick={() => setOpen(true)} sx={{ alignSelf: 'flex-start' }}>
        Write a review
      </Button>
      {open && <ReviewDialog bookTitle={book.title} onSubmit={handleSubmit} onClose={() => setOpen(false)} />}
    </>
  )
}
