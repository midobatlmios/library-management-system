import { useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField,
} from '@mui/material'

/**
 * Paste a JSON array and send it to a bulk endpoint. The backend creates
 * all of them or none (one transaction), and says which item was wrong.
 */
export default function BulkCreateDialog({ title, example, onSubmit, onClose }) {
  const [text, setText] = useState(example)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    let items
    try {
      items = JSON.parse(text)
    } catch (e) {
      setError(`This is not valid JSON: ${e.message}`)
      return
    }
    if (!Array.isArray(items) || items.length === 0) {
      setError('Paste a JSON array with at least one item: [ {...}, {...} ]')
      return
    }
    setBusy(true)
    try {
      await onSubmit(items)
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="md">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          Paste a JSON array. Either all items are created or none, so fix any error and send again.
        </DialogContentText>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <TextField
          value={text}
          onChange={(e) => setText(e.target.value)}
          multiline
          minRows={12}
          fullWidth
          slotProps={{ htmlInput: { spellCheck: false, style: { fontFamily: 'monospace', fontSize: 13 } } }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={busy}>
          {busy ? 'Creating…' : 'Create all'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
