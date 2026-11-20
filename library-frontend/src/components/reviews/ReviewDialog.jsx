import { useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Rating,
  Stack,
  TextField,
  Typography,
} from '@mui/material'

// Same limits as the backend (CreateReviewRequest / UpdateReviewRequest).
const TITLE_MAX = 200
const TEXT_MIN = 10
const TEXT_MAX = 2000

/**
 * Write or edit a review. review = null for a new one.
 * onSubmit({ rating, title, reviewText }) may throw: its message is shown in the dialog.
 */
export default function ReviewDialog({ bookTitle, review, onSubmit, onClose }) {
  const [rating, setRating] = useState(review?.rating ?? 0)
  const [title, setTitle] = useState(review?.title ?? '')
  const [text, setText] = useState(review?.reviewText ?? '')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const textLength = text.trim().length
  const textError = text !== '' && textLength < TEXT_MIN
  const valid = rating >= 1 && textLength >= TEXT_MIN && textLength <= TEXT_MAX && title.length <= TITLE_MAX

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      await onSubmit({ rating, title: title.trim() || null, reviewText: text.trim() })
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>{review ? 'Edit your review' : 'Write a review'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {bookTitle && <Typography color="text.secondary">{bookTitle}</Typography>}
          {error && <Alert severity="error">{error}</Alert>}
          <div>
            <Typography component="legend" variant="body2" sx={{ mb: 0.5 }}>
              Your rating *
            </Typography>
            <Rating value={rating} onChange={(_, v) => setRating(v ?? 0)} size="large" />
          </div>
          <TextField
            label="Title (optional)"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            slotProps={{ htmlInput: { maxLength: TITLE_MAX } }}
            fullWidth
          />
          <TextField
            label="Your review"
            value={text}
            onChange={(e) => setText(e.target.value)}
            multiline
            minRows={4}
            required
            fullWidth
            error={textError}
            helperText={
              textError ? `At least ${TEXT_MIN} characters` : `${textLength} / ${TEXT_MAX}`
            }
            slotProps={{ htmlInput: { maxLength: TEXT_MAX } }}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy || !valid}>
          {busy ? 'Saving…' : review ? 'Save changes' : 'Publish review'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
