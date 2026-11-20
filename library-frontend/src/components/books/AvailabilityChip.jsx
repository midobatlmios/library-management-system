import { Chip } from '@mui/material'

/** "3 of 5 available" in green, or "All copies out" in orange. */
export default function AvailabilityChip({ book, size = 'small' }) {
  const available = book.availableCopies ?? 0
  if (available > 0) {
    return (
      <Chip
        size={size}
        color="success"
        variant="outlined"
        label={book.totalCopies == null ? `${available} available` : `${available} of ${book.totalCopies} available`}
      />
    )
  }
  return <Chip size={size} color="warning" variant="outlined" label="All copies out" />
}
