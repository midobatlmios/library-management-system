import { Card, CardContent, Stack, Typography } from '@mui/material'

/** The centered card every auth page sits in. */
export default function AuthCard({ title, subtitle, children }) {
  return (
    <div className="mx-auto w-full max-w-md">
      <Card variant="outlined">
        <CardContent className="p-6 sm:p-8">
          <Stack spacing={3}>
            <div>
              <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
                {title}
              </Typography>
              {subtitle && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                  {subtitle}
                </Typography>
              )}
            </div>
            {children}
          </Stack>
        </CardContent>
      </Card>
    </div>
  )
}
