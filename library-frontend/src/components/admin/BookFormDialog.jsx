import { useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material'
import { flattenGenreTree } from './genreTree.js'

const EMPTY = {
  isbn: '',
  title: '',
  author: '',
  genreId: '',
  publisher: '',
  publicationDate: '',
  language: '',
  pages: '',
  description: '',
  totalCopies: 1,
  price: '',
  coverImageUrl: '',
}

// ISBN-10 (last may be X) or ISBN-13, hyphens and spaces allowed — same check as the backend.
const isbnOk = (v) => /^\d{13}$|^\d{9}[\dXx]$/.test(v.replace(/[\s-]/g, ''))

/**
 * Create or edit a book. book = null for a new one.
 * onSubmit(dto) may throw: the backend's message is shown in the dialog.
 */
export default function BookFormDialog({ book, genres, onSubmit, onClose }) {
  const [form, setForm] = useState(() =>
    book
      ? Object.fromEntries(Object.keys(EMPTY).map((k) => [k, book[k] ?? '']))
      : EMPTY,
  )
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const isbnError = form.isbn !== '' && !isbnOk(form.isbn)
  const valid =
    isbnOk(form.isbn) && form.title.trim() && form.author.trim() && form.genreId !== '' && Number(form.totalCopies) >= 0

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    // Empty optional fields go as null; numbers as numbers.
    const opt = (v) => (v === '' || v == null ? null : v)
    const dto = {
      ...(book || {}), // keeps active, availableCopies... when editing
      isbn: form.isbn.trim(),
      title: form.title.trim(),
      author: form.author.trim(),
      genreId: Number(form.genreId),
      publisher: opt(form.publisher.trim()),
      publicationDate: opt(form.publicationDate),
      language: opt(form.language.trim()),
      pages: opt(form.pages) === null ? null : Number(form.pages),
      description: opt(form.description.trim()),
      totalCopies: Number(form.totalCopies),
      price: opt(form.price) === null ? null : Number(form.price),
      coverImageUrl: opt(form.coverImageUrl.trim()),
    }
    try {
      await onSubmit(dto)
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  const genreOptions = flattenGenreTree(genres)

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="md">
      <DialogTitle>{book ? `Edit "${book.title}"` : 'New book'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <TextField label="Title" value={form.title} onChange={set('title')} required className="sm:col-span-2" />
            <TextField label="Author" value={form.author} onChange={set('author')} required />
            <TextField
              label="ISBN"
              value={form.isbn}
              onChange={set('isbn')}
              required
              error={isbnError}
              helperText={isbnError ? 'ISBN-10 or ISBN-13, e.g. 978-0-13-468599-1' : ' '}
            />
            <TextField select label="Genre" value={form.genreId} onChange={set('genreId')} required>
              {genreOptions.map((g) => (
                <MenuItem key={g.id} value={g.id} disabled={g.active === false}>
                  <span style={{ paddingLeft: g.depth * 16 }}>
                    {g.name}
                    {g.active === false && ' (hidden)'}
                  </span>
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Total copies"
              type="number"
              value={form.totalCopies}
              onChange={set('totalCopies')}
              required
              slotProps={{ htmlInput: { min: 0 } }}
              helperText={book ? `${book.availableCopies} on the shelf now` : 'All copies start on the shelf'}
            />
            <TextField label="Publisher" value={form.publisher} onChange={set('publisher')} />
            <TextField
              label="Publication date"
              type="date"
              value={form.publicationDate}
              onChange={set('publicationDate')}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField label="Language" value={form.language} onChange={set('language')} />
            <TextField label="Pages" type="number" value={form.pages} onChange={set('pages')} slotProps={{ htmlInput: { min: 1 } }} />
            <TextField
              label="Price (MAD)"
              type="number"
              value={form.price}
              onChange={set('price')}
              slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
            />
            <TextField label="Cover image URL" value={form.coverImageUrl} onChange={set('coverImageUrl')} />
            <TextField
              label="Description"
              value={form.description}
              onChange={set('description')}
              multiline
              minRows={3}
              className="sm:col-span-2"
              slotProps={{ htmlInput: { maxLength: 2000 } }}
            />
          </div>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy || !valid}>
          {busy ? 'Saving…' : book ? 'Save changes' : 'Create book'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
