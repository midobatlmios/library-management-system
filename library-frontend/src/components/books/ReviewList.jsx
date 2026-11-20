import { useCallback, useState } from 'react'
import {
  Alert,
  Button,
  Chip,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Pagination,
  Select,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material'
import { useSelector } from 'react-redux'
import { deleteReview, getBookReviews, markReviewHelpful, updateReview } from '../../api/reviews.js'
import { selectUser } from '../../store/authSlice.js'
import { useAsync } from '../../utils/useAsync.js'
import ConfirmDialog from '../feedback/ConfirmDialog.jsx'
import { useToast } from '../feedback/toastContext.js'
import ReviewDialog from '../reviews/ReviewDialog.jsx'
import ReviewItem from '../reviews/ReviewItem.jsx'

const PAGE_SIZE = 5

// What the filter menu shows -> the backend's filter (+ rating).
const FILTERS = {
  ALL: { label: 'All reviews', filter: 'ALL' },
  VERIFIED_ONLY: { label: 'Verified readers', filter: 'VERIFIED_ONLY' },
  TOP_HELPFUL: { label: 'Most helpful', filter: 'TOP_HELPFUL' },
  5: { label: '5 stars', filter: 'BY_RATING', rating: 5 },
  4: { label: '4 stars', filter: 'BY_RATING', rating: 4 },
  3: { label: '3 stars', filter: 'BY_RATING', rating: 3 },
  2: { label: '2 stars', filter: 'BY_RATING', rating: 2 },
  1: { label: '1 star', filter: 'BY_RATING', rating: 1 },
}

/**
 * Reviews of one book with a filter menu and paging.
 * onChanged(): a review was edited or deleted (the page reloads the rating summary).
 * To reload from scratch (e.g. after a new review), the page changes this component's key.
 */
export default function ReviewList({ bookId, bookTitle, onChanged }) {
  const user = useSelector(selectUser)
  const toast = useToast()
  const [filterKey, setFilterKey] = useState('ALL')
  const [page, setPage] = useState(1)
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)

  const load = useCallback(() => {
    const f = FILTERS[filterKey]
    return getBookReviews(bookId, { filter: f.filter, rating: f.rating, page: page - 1, size: PAGE_SIZE })
  }, [bookId, filterKey, page])
  const { data, error, loading, reload } = useAsync(load)

  const handleHelpful = async (review) => {
    try {
      await markReviewHelpful(review.id)
      toast('Thanks for your feedback')
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const handleSave = async (changes) => {
    await updateReview(editing.id, changes) // errors are shown inside the dialog
    setEditing(null)
    toast('Review updated')
    reload()
    onChanged()
  }

  const handleDelete = async () => {
    try {
      await deleteReview(deleting.id)
      toast('Review deleted', 'info')
      onChanged()
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  return (
    <Stack spacing={2}>
      <FormControl size="small" sx={{ width: 220 }}>
        <InputLabel id="review-filter-label">Show</InputLabel>
        <Select
          labelId="review-filter-label"
          label="Show"
          value={filterKey}
          onChange={(e) => {
            setFilterKey(e.target.value)
            setPage(1)
          }}
        >
          {Object.entries(FILTERS).map(([key, f]) => (
            <MenuItem key={key} value={key}>
              {f.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      {error && <Alert severity="error">{error}</Alert>}

      {!data && loading ? (
        <Stack spacing={2}>
          {[0, 1].map((i) => (
            <Skeleton key={i} variant="rounded" height={90} />
          ))}
        </Stack>
      ) : data?.empty ? (
        <Typography color="text.secondary">No reviews match this filter.</Typography>
      ) : (
        data && (
          <Stack spacing={2} divider={<Divider flexItem />} sx={{ opacity: loading ? 0.5 : 1 }}>
            {data.content.map((r) => {
              const mine = user?.id === r.userId
              return (
                <ReviewItem
                  key={r.id}
                  review={r}
                  badge={mine && <Chip size="small" label="Your review" color="primary" variant="outlined" />}
                  actions={
                    mine ? (
                      <>
                        <Button size="small" onClick={() => setEditing(r)}>
                          Edit
                        </Button>
                        <Button size="small" color="error" onClick={() => setDeleting(r)}>
                          Delete
                        </Button>
                      </>
                    ) : (
                      user && (
                        <Button size="small" onClick={() => handleHelpful(r)}>
                          Helpful
                        </Button>
                      )
                    )
                  }
                />
              )
            })}
          </Stack>
        )
      )}

      {data && data.totalPages > 1 && (
        <Pagination count={data.totalPages} page={page} onChange={(_, p) => setPage(p)} size="small" />
      )}

      {editing && (
        <ReviewDialog
          key={editing.id}
          bookTitle={bookTitle}
          review={editing}
          onSubmit={handleSave}
          onClose={() => setEditing(null)}
        />
      )}
      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete your review?"
        message="This can't be undone."
        confirmLabel="Delete"
        danger
        onConfirm={handleDelete}
        onClose={() => setDeleting(null)}
      />
    </Stack>
  )
}
