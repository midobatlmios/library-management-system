import { useState } from 'react'
import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material'

/**
 * "Are you sure?" dialog. onConfirm may be async: the button shows a busy
 * state, and the dialog closes when it finishes.
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Keep it',
  danger = false,
  onConfirm,
  onClose,
}) {
  const [busy, setBusy] = useState(false)

  const handleConfirm = async () => {
    setBusy(true)
    try {
      await onConfirm()
    } finally {
      setBusy(false)
      onClose()
    }
  }

  return (
    <Dialog open={open} onClose={busy ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      {message && (
        <DialogContent>
          <DialogContentText>{message}</DialogContentText>
        </DialogContent>
      )}
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          {cancelLabel}
        </Button>
        <Button
          onClick={handleConfirm}
          disabled={busy}
          variant="contained"
          color={danger ? 'error' : 'primary'}
        >
          {busy ? 'Please wait…' : confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
