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
  Typography,
} from '@mui/material'
import { createFine } from '../../api/admin.js'

// Overdue fines are created automatically when a late book comes back.
const TYPES = { DAMAGE: 'Damaged book', LOST: 'Lost book', OTHER: 'Other fee' }

/**
 * Charge a fine for a loan. loan = the loan row (from Loans), or null to type a loan id.
 * The amount is typed in MAD and sent in centimes.
 */
export default function CreateFineDialog({ loan, onDone, onClose }) {
  const [loanId, setLoanId] = useState(loan?.id ?? '')
  const [type, setType] = useState('DAMAGE')
  const [amount, setAmount] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const centimes = Math.round(Number(amount) * 100)
  const valid = Number(loanId) > 0 && centimes >= 1

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      await createFine({ bookLoanId: Number(loanId), type, amount: centimes, reason: reason.trim() || null })
      onDone()
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>New fine</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {loan ? (
            <Typography>
              {loan.userFullName} · <strong>{loan.bookTitle}</strong> (loan #{loan.id})
            </Typography>
          ) : (
            <TextField
              label="Loan id"
              type="number"
              value={loanId}
              onChange={(e) => setLoanId(e.target.value)}
              required
              helperText="Tip: use the Fine button on the Loans page to fill this in"
            />
          )}
          <TextField select label="Type" value={type} onChange={(e) => setType(e.target.value)}>
            {Object.entries(TYPES).map(([v, l]) => (
              <MenuItem key={v} value={v}>
                {l}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Amount (MAD)"
            type="number"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
            slotProps={{ htmlInput: { min: 0.01, step: '0.01' } }}
          />
          <TextField
            label="Reason (the member sees it)"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
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
          {busy ? 'Saving…' : 'Create fine'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
