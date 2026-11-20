import { Button, Card, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

/** "Nothing here yet" card with an optional button. */
export default function EmptyState({ title, text, actionLabel, actionTo }) {
  return (
    <Card variant="outlined" className="p-8 text-center">
      <Typography variant="h6" component="p" gutterBottom>
        {title}
      </Typography>
      {text && (
        <Typography color="text.secondary" sx={{ mb: actionLabel ? 2 : 0 }}>
          {text}
        </Typography>
      )}
      {actionLabel && (
        <Button component={RouterLink} to={actionTo} variant="contained">
          {actionLabel}
        </Button>
      )}
    </Card>
  )
}
