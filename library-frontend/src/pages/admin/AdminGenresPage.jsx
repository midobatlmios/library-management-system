import { useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteForeverIcon from '@mui/icons-material/DeleteForever'
import EditIcon from '@mui/icons-material/Edit'
import RestoreIcon from '@mui/icons-material/Restore'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'
import * as admin from '../../api/admin.js'
import AdminTable from '../../components/admin/AdminTable.jsx'
import BulkCreateDialog from '../../components/admin/BulkCreateDialog.jsx'
import { descendantIds, flattenGenreTree } from '../../components/admin/genreTree.js'
import ConfirmDialog from '../../components/feedback/ConfirmDialog.jsx'
import { useToast } from '../../components/feedback/toastContext.js'
import { useAsync } from '../../utils/useAsync.js'
import PageTitle from '../../components/enamel/PageTitle.jsx'

const BULK_EXAMPLE = `[
  { "code": "SCIENCE_FICTION", "name": "Science fiction", "displayOrder": 1 },
  { "code": "SPACE_OPERA", "name": "Space opera", "parentGenreId": 1 }
]`

/** "Science fiction" -> "SCIENCE_FICTION" (the backend allows only A-Z and _). */
const toCode = (name) =>
  name
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '') // é -> e
    .toUpperCase()
    .replace(/[^A-Z]+/g, '_')
    .replace(/^_+|_+$/g, '')

function GenreFormDialog({ genre, genres, onSubmit, onClose }) {
  const [form, setForm] = useState({
    name: genre?.name ?? '',
    code: genre?.code ?? '',
    description: genre?.description ?? '',
    displayOrder: genre?.displayOrder ?? 0,
    parentGenreId: genre?.parentGenreId ?? '',
  })
  const [codeTouched, setCodeTouched] = useState(Boolean(genre))
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  // A genre can't go under itself or one of its own sub-genres.
  const blocked = genre ? descendantIds(genres, genre.id) : new Set()
  const parents = flattenGenreTree(genres).filter((g) => !blocked.has(g.id))

  const codeError = form.code !== '' && !/^[A-Z_]{2,50}$/.test(form.code)
  const valid = form.name.trim().length >= 2 && /^[A-Z_]{2,50}$/.test(form.code)

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      await onSubmit({
        ...(genre || {}),
        name: form.name.trim(),
        code: form.code,
        description: form.description.trim() || null,
        displayOrder: Number(form.displayOrder) || 0,
        parentGenreId: form.parentGenreId === '' ? null : Number(form.parentGenreId),
        subGenres: undefined,
        depth: undefined, // only used for indenting the table
      })
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>{genre ? `Edit "${genre.name}"` : 'New genre'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="Name"
            value={form.name}
            required
            onChange={(e) =>
              setForm({ ...form, name: e.target.value, code: codeTouched ? form.code : toCode(e.target.value) })
            }
          />
          <TextField
            label="Code"
            value={form.code}
            required
            onChange={(e) => {
              setCodeTouched(true)
              setForm({ ...form, code: e.target.value.toUpperCase() })
            }}
            error={codeError}
            helperText={codeError ? 'Only A-Z and _, 2 to 50 characters' : 'Made from the name; you can change it'}
          />
          <TextField
            select
            label="Parent genre"
            value={form.parentGenreId}
            onChange={(e) => setForm({ ...form, parentGenreId: e.target.value })}
          >
            <MenuItem value="">None (top level)</MenuItem>
            {parents.map((g) => (
              <MenuItem key={g.id} value={g.id}>
                <span style={{ paddingLeft: g.depth * 16 }}>{g.name}</span>
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Display order"
            type="number"
            value={form.displayOrder}
            onChange={(e) => setForm({ ...form, displayOrder: e.target.value })}
            slotProps={{ htmlInput: { min: 0 } }}
            helperText="Smaller numbers come first"
          />
          <TextField
            label="Description"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            multiline
            minRows={2}
            slotProps={{ htmlInput: { maxLength: 500 } }}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy || !valid}>
          {busy ? 'Saving…' : genre ? 'Save changes' : 'Create genre'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default function AdminGenresPage() {
  const toast = useToast()
  const genres = useAsync(admin.getAllGenres)
  const rows = useMemo(() => flattenGenreTree(genres.data || []), [genres.data])
  const [editing, setEditing] = useState(null) // a genre, 'new', or null
  const [bulkOpen, setBulkOpen] = useState(false)
  const [deleting, setDeleting] = useState(null)
  const [hiding, setHiding] = useState(null)

  const act = async (fn, message) => {
    try {
      await fn()
      toast(message)
      genres.reload()
    } catch (e) {
      toast(e.message, 'error')
    }
  }

  const handleSave = async (dto) => {
    if (editing === 'new') await admin.createGenre(dto)
    else await admin.updateGenre(editing.id, dto)
    toast(editing === 'new' ? 'Genre created' : 'Genre saved')
    setEditing(null)
    genres.reload()
  }

  const columns = [
    {
      key: 'name',
      label: 'Genre',
      render: (g) => (
        <div style={{ paddingLeft: g.depth * 20 }} className="min-w-40">
          <Typography variant="body2" sx={{ fontWeight: g.depth === 0 ? 600 : 400 }}>
            {g.depth > 0 && '└ '}
            {g.name}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {g.code}
          </Typography>
        </div>
      ),
    },
    { key: 'bookCount', label: 'Books', align: 'right', render: (g) => g.bookCount ?? '—' },
    { key: 'displayOrder', label: 'Order', align: 'right' },
    {
      key: 'active',
      label: 'Status',
      render: (g) => (g.active === false ? <Chip size="small" label="Hidden" /> : <Chip size="small" color="success" variant="outlined" label="Active" />),
    },
    {
      key: 'actions',
      label: '',
      align: 'right',
      render: (g) => (
        <div className="flex justify-end whitespace-nowrap">
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => setEditing(g)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {g.active === false ? (
            <Tooltip title="Restore">
              <IconButton
                size="small"
                onClick={() => act(() => admin.updateGenre(g.id, { ...g, depth: undefined, subGenres: undefined, active: true }), 'Genre restored')}
              >
                <RestoreIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <Tooltip title="Hide (soft delete). Its books keep this genre.">
              <IconButton size="small" onClick={() => setHiding(g)}>
                <VisibilityOffIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
          <Tooltip title="Delete permanently">
            <IconButton size="small" color="error" onClick={() => setDeleting(g)}>
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
        <PageTitle>Genres</PageTitle>
        <div className="flex gap-2">
          <Button variant="outlined" onClick={() => setBulkOpen(true)}>
            Bulk add
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditing('new')}>
            New genre
          </Button>
        </div>
      </div>

      <AdminTable columns={columns} rows={genres.data ? rows : undefined} loading={genres.loading} error={genres.error} empty="No genres yet." />

      {editing && (
        <GenreFormDialog
          key={editing === 'new' ? 'new' : editing.id}
          genre={editing === 'new' ? null : editing}
          genres={genres.data || []}
          onSubmit={handleSave}
          onClose={() => setEditing(null)}
        />
      )}
      {bulkOpen && (
        <BulkCreateDialog
          title="Add several genres"
          example={BULK_EXAMPLE}
          onSubmit={async (items) => {
            const created = await admin.createGenresBulk(items)
            toast(`${created.length} genre${created.length === 1 ? '' : 's'} created`)
            setBulkOpen(false)
            genres.reload()
          }}
          onClose={() => setBulkOpen(false)}
        />
      )}
      <ConfirmDialog
        open={Boolean(hiding)}
        title="Hide this genre?"
        message={hiding && `"${hiding.name}" disappears from the catalog's genre list. Its books keep this genre and stay in the catalog. You can restore it any time.`}
        confirmLabel="Hide genre"
        cancelLabel="Cancel"
        onConfirm={() => act(() => admin.softDeleteGenre(hiding.id), 'Genre hidden')}
        onClose={() => setHiding(null)}
      />
      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete this genre permanently?"
        message={
          deleting &&
          `"${deleting.name}" will be removed. This can't be undone. A genre that still has books or sub-genres can't be deleted.`
        }
        confirmLabel="Delete permanently"
        danger
        onConfirm={() => act(() => admin.hardDeleteGenre(deleting.id), 'Genre deleted')}
        onClose={() => setDeleting(null)}
      />
    </Stack>
  )
}
