import { useCallback, useState } from 'react'
import {
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Link,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlined'
import EditNoteIcon from '@mui/icons-material/EditNote'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import { getMyWishlist, removeFromWishlist, updateWishlistNotes } from '../../api/wishlist.js'
import EmptyState from '../../components/account/EmptyState.jsx'
import PagedList from '../../components/account/PagedList.jsx'
import AvailabilityChip from '../../components/books/AvailabilityChip.jsx'
import BookCover from '../../components/books/BookCover.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { formatDate } from '../../utils/format.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

function NotesDialog({ item, onClose, onSave }) {
  const [notes, setNotes] = useState(item?.notes ?? '')
  const [busy, setBusy] = useState(false)
  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>Notes for "{item.bookTitle}"</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          multiline
          minRows={3}
          fullWidth
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Why you want to read it, who recommended it…"
          sx={{ mt: 1 }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button
          variant="contained"
          disabled={busy}
          onClick={async () => {
            setBusy(true)
            await onSave(notes.trim())
            setBusy(false)
          }}
        >
          Save
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function WishlistCard({ item, onEditNotes, onRemove }) {
  return (
    <Card variant="outlined">
      <CardContent className="flex gap-4">
        <div className="w-16 shrink-0">
          <BookCover book={{ title: item.bookTitle, coverImageUrl: item.coverImageUrl }} />
        </div>
        <Stack spacing={0.5} className="flex-1 min-w-0">
          <Link component={RouterLink} to={`/books/${item.bookId}`} underline="hover" sx={{ fontWeight: 600 }}>
            {item.bookTitle}
          </Link>
          <Typography variant="body2" color="text.secondary">
            {item.bookAuthor}
          </Typography>
          <div>
            {item.bookActive === false ? (
              <Typography variant="caption" color="text.secondary">
                No longer in the catalog
              </Typography>
            ) : (
              // The wishlist knows availableCopies but not the total, so the chip says "3 available".
              <AvailabilityChip book={{ availableCopies: item.availableCopies ?? 0 }} />
            )}
          </div>
          {item.notes && (
            <Typography variant="body2" sx={{ fontStyle: 'italic', whiteSpace: 'pre-line' }}>
              {item.notes}
            </Typography>
          )}
          <Typography variant="caption" color="text.secondary">
            Added {formatDate(item.addedAt)}
          </Typography>
        </Stack>
        <Stack>
          <Tooltip title="Edit notes">
            <IconButton onClick={() => onEditNotes(item)} aria-label="Edit notes">
              <EditNoteIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Remove from wishlist">
            <IconButton onClick={() => onRemove(item)} aria-label="Remove from wishlist">
              <DeleteOutlineIcon />
            </IconButton>
          </Tooltip>
        </Stack>
      </CardContent>
    </Card>
  )
}

/** /wishlist — books saved for later, with personal notes. */
export default function WishlistPage() {
  const toast = useToast()
  const [params, setParams] = useSearchParams()
  const page = Math.max(1, Number(params.get('page')) || 1)
  const load = useCallback(() => getMyWishlist({ page: page - 1 }), [page])
  const { data, error, loading, reload } = useAsync(load)
  const [editing, setEditing] = useState(null)

  const handleRemove = async (item) => {
    try {
      await removeFromWishlist(item.bookId)
      toast(`Removed "${item.bookTitle}"`, 'info')
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const handleSaveNotes = async (notes) => {
    try {
      await updateWishlistNotes(editing.bookId, notes)
      toast('Notes saved')
      setEditing(null)
      reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  return (
    <Stack spacing={3}>
      <PageTitle>My wishlist</PageTitle>
      <PagedList
        data={data}
        error={error}
        loading={loading}
        page={page}
        onPageChange={(p) => setParams({ page: p })}
        renderItem={(item) => (
          <WishlistCard key={item.id} item={item} onEditNotes={setEditing} onRemove={handleRemove} />
        )}
        empty={
          <EmptyState
            title="Your wishlist is empty"
            text="Tap the heart on a book's page to save it here."
            actionLabel="Browse the catalog"
            actionTo="/books"
          />
        }
      />
      {/* key: a fresh dialog (and fresh text) for each book */}
      {editing && (
        <NotesDialog key={editing.id} item={editing} onClose={() => setEditing(null)} onSave={handleSaveNotes} />
      )}
    </Stack>
  )
}
