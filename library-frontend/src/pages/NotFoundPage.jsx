import { Button, Stack, Typography } from '@mui/material'
import SearchOffIcon from '@mui/icons-material/SearchOff'
import { Link as RouterLink, useLocation } from 'react-router-dom'

export default function NotFoundPage() {
  const { pathname } = useLocation()
  return (
    <Stack spacing={2} sx={{ alignItems: 'center', textAlign: 'center', py: { xs: 4, sm: 8 } }}>
      <SearchOffIcon sx={{ fontSize: 72, color: 'text.disabled' }} />
      <Typography variant="h4" component="h1" sx={{ fontWeight: 700 }}>
        Page not found
      </Typography>
      <Typography color="text.secondary">
        There is no page at <code>{pathname}</code>. The link may be old or mistyped.
      </Typography>
      <div className="flex flex-wrap justify-center gap-2 pt-2">
        <Button component={RouterLink} to="/" variant="contained">
          Back to home
        </Button>
        <Button component={RouterLink} to="/books">
          Browse the catalog
        </Button>
      </div>
    </Stack>
  )
}
