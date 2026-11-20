import { useCallback, useState } from 'react'
import { Button, Card, CardContent, Link, Stack } from '@mui/material'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import { deleteReview, getMyReviews, updateReview } from '../../api/reviews.js'
import EmptyState from '../../components/account/EmptyState.jsx'
import PagedList from '../../components/account/PagedList.jsx'
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import ReviewDialog from '../../components/reviews/ReviewDialog.jsx'
import ReviewItem from '../../components/reviews/ReviewItem.jsx'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

/** /my-reviews — every review you wrote, with Edit and Delete. New reviews are written on a book's page. */
export default function MyReviewsPage() {
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const page = Math.max(1, Number(params.get('page')) || 1)
  const load = useCallback(() => getMyReviews({ page: page - 1 }), [page])
  const { data, error, loading, reload } = useAsync(load)
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)

  const handleSave = async (changes) => {
    await updateReview(editing.id, changes) // errors are shown inside the dialog
    setEditing(null)
    toast('Review updated')
    reload()
  }

  const handleDelete = async () => {
    try {
      await deleteReview(deleting.id)
      toast('Review deleted', 'info')
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  return (
    <Stack spacing={3}>
      <PageTitle>My reviews</PageTitle>

      <PagedList
        data={data}
        error={error}
        loading={loading}
        page={page}
        onPageChange={(p) => setParams({ page: p })}
        renderItem={(r) => (
          <Card key={r.id} variant="outlined">
            <CardContent>
              <ReviewItem
                review={r}
                heading={
                  <Link component={RouterLink} to={`/books/${r.bookId}`} underline="hover" sx={{ fontWeight: 600 }}>
                    {r.bookTitle}
                  </Link>
                }
                actions={
                  <>
                    <Button size="small" onClick={() => setEditing(r)}>
                      Edit
                    </Button>
                    <Button size="small" color="error" onClick={() => setDeleting(r)}>
                      Delete
                    </Button>
                  </>
                }
              />
            </CardContent>
          </Card>
        )}
        empty={
          <EmptyState
            title="You haven't written any reviews yet"
            text="Open a book you've borrowed and click “Write a review”."
            actionLabel="My loans"
            actionTo="/loans?view=history"
          />
        }
      />

      {editing && (
        <ReviewDialog
          key={editing.id}
          bookTitle={editing.bookTitle}
          review={editing}
          onSubmit={handleSave}
          onClose={() => setEditing(null)}
        />
      )}
      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete your review?"
        message={deleting && `Your review of "${deleting.bookTitle}" will be removed. This can't be undone.`}
        confirmLabel="Delete"
        danger
        onConfirm={handleDelete}
        onClose={() => setDeleting(null)}
      />
    </Stack>
  )
}
