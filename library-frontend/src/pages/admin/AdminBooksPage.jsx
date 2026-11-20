import { useCallback, useState } from 'react'
import {
  Button,
  Chip,
  FormControlLabel,
  IconButton,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteForeverIcon from '@mui/icons-material/DeleteForever'
import EditIcon from '@mui/icons-material/Edit'
import RestoreIcon from '@mui/icons-material/Restore'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'
import { Link as RouterLink } from 'react-router-dom'
import * as admin from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import BookFormDialog from '../../components/admin/BookFormDialog.jsx'
import BulkCreateDialog from '../../components/admin/BulkCreateDialog.jsx'
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const PAGE_SIZE = 15
const BULK_EXAMPLE = `[
  {
    "isbn": "978-0-13-468599-1",
    "title": "Effective Java",
    "author": "Joshua Bloch",
    "genreId": 1,
    "totalCopies": 3,
    "language": "English",
    "pages": 412
  }
]`

export default function AdminBooksPage() {
  const toast = useToast()
  const [search, setSearch] = useState('')
  const [query, setQuery] = useState('') // what was last searched
  const [showHidden, setShowHidden] = useState(true)
  const [page, setPage] = useState(0)

  const load = useCallback(
    () =>
      admin.searchBooksAdmin({
        search: query,
        includeInactive: showHidden,
        page,
        size: PAGE_SIZE,
        sortBy: 'title',
        sortDirection: 'ASC',
      }),
    [query, showHidden, page],
  )
  const books = useAsync(load)
  const genres = useAsync(admin.getAllGenres)

  const [editing, setEditing] = useState(null) // a book, 'new', or null
  const [bulkOpen, setBulkOpen] = useState(false)
  const [deleting, setDeleting] = useState(null)
  const [hiding, setHiding] = useState(null)

  const act = async (fn, message) => {
    try {
      await fn()
      toast(message)
      books.reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const handleSave = async (dto) => {
    if (editing === 'new') await admin.createBook(dto)
    else await admin.updateBook(editing.id, dto)
    toast(editing === 'new' ? 'Book created' : 'Book saved')
    setEditing(null)
    books.reload()
  }

  const handleBulk = async (items) => {
    const created = await admin.createBooksBulk(items)
    toast(`${created.length} book${created.length === 1 ? '' : 's'} created`)
    setBulkOpen(false)
    books.reload()
  }

  const columns = [
    {
      key: 'title',
      label: 'Book',
      render: (b) => (
        <div className="min-w-48">
          <Typography
            component={RouterLink}
            to={`/books/${b.id}`}
            variant="body2"
            sx={{ fontWeight: 600, color: 'inherit', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
          >
            {b.title}
          </Typography>
          <Typography variant="caption" color="text.secondary" component="p">
            {b.author}
          </Typography>
        </div>
      ),
    },
    { key: 'isbn', label: 'ISBN', render: (b) => <span className="whitespace-nowrap">{b.isbn}</span> },
    { key: 'genreName', label: 'Genre' },
    { key: 'copies', label: 'Copies', align: 'right', render: (b) => `${b.availableCopies} / ${b.totalCopies}` },
    {
      key: 'active',
      label: 'Status',
      render: (b) => (b.active === false ? <Chip size="small" label="Hidden" /> : <Chip size="small" color="success" variant="outlined" label="Active" />),
    },
    {
      key: 'actions',
      label: '',
      align: 'right',
      render: (b) => (
        <div className="flex justify-end whitespace-nowrap">
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => setEditing(b)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {b.active === false ? (
            <Tooltip title="Restore (show in the catalog again)">
              <IconButton size="small" onClick={() => act(() => admin.updateBook(b.id, { ...b, active: true }), 'Book restored')}>
                <RestoreIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <Tooltip title="Hide from the catalog (soft delete)">
              <IconButton size="small" onClick={() => setHiding(b)}>
                <VisibilityOffIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          <Tooltip title="Delete permanently">
            <IconButton size="small" color="error" onClick={() => setDeleting(b)}>
              <DeleteForeverIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </div>
      ),
    },
  ]

  return (
    <Stack spacing={3}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <PageTitle>Books</PageTitle>
        <div className="flex gap-2">
          <Button variant="outlined" onClick={() => setBulkOpen(true)}>
            Bulk add
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditing('new')}>
            New book
          </Button>
        </div>
      </div>

      <form
        className="flex flex-wrap items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          setPage(0)
          setQuery(search.trim())
        }}
      >
        <TextField
          size="small"
          label="Search title, author or ISBN"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="flex-1 min-w-56"
        />
        <Button type="submit" variant="outlined">
          Search
        </Button>
        <FormControlLabel
          control={
            <Switch
              checked={showHidden}
              onChange={(e) => {
                setShowHidden(e.target.checked)
                setPage(0)
              }}
            />
          }
          label="Show hidden books"
        />
      </form>

      <AdminTable
        columns={columns}
        rows={books.data?.content}
        loading={books.loading}
        error={books.error}
        empty="No books match."
        paging={books.data && { page, size: PAGE_SIZE, total: books.data.totalElements, onPageChange: setPage }}
      />

      {editing && (
        <BookFormDialog
          key={editing === 'new' ? 'new' : editing.id}
          book={editing === 'new' ? null : editing}
          genres={genres.data || []}
          onSubmit={handleSave}
          onClose={() => setEditing(null)}
        />
      )}
      {bulkOpen && (
        <BulkCreateDialog title="Add several books" example={BULK_EXAMPLE} onSubmit={handleBulk} onClose={() => setBulkOpen(false)} />
      )}
      <ConfirmDialog
        open={Boolean(hiding)}
        title="Hide this book?"
        message={hiding && `"${hiding.title}" disappears from the catalog. Current loans and history stay. You can restore it any time.`}
        confirmLabel="Hide book"
        cancelLabel="Cancel"
        onConfirm={() => act(() => admin.softDeleteBook(hiding.id), 'Book hidden')}
        onClose={() => setHiding(null)}
      />
      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete this book permanently?"
        message={
          deleting &&
          `"${deleting.title}" will be removed from the database. This can't be undone. A book with history (loans, reservations, reviews or wishlists) can't be deleted: hide it instead.`
        }
        confirmLabel="Delete permanently"
        danger
        onConfirm={() => act(() => admin.hardDeleteBook(deleting.id), 'Book deleted')}
        onClose={() => setDeleting(null)}
      />
    </Stack>
  )
}
