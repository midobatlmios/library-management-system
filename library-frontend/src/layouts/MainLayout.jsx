import { Suspense } from 'react'
import { Alert, Box, Button, Container, Link, Typography } from '@mui/material'
import { useSelector } from 'react-redux'
import { Link as RouterLink, Outlet, useLocation } from 'react-router-dom'
import ErrorBoundary from '../components/ErrorBoundary.jsx'
import Navbar from '../components/Navbar.jsx'
import PageEffects from '../components/PageEffects.jsx'
import PageSkeleton from '../components/PageSkeleton.jsx'
import OpenNow from '../components/enamel/OpenNow.jsx'
import { hoursSummary, LIBRARY, MAPS_URL } from '../library.js'
import { selectUser } from '../store/authSlice.js'

// Read once when the app loads (reading the clock during render is "impure" for React).
const YEAR = new Date().getFullYear()

/** The app shell: navbar, the current page (Outlet), footer. */
export default function MainLayout() {
  const user = useSelector(selectUser)
  const { pathname } = useLocation()
  const showVerifyBanner = user && !user.verified && pathname !== '/check-email'

  return (
    <Box className="min-h-screen flex flex-col">
      <PageEffects />
      <Navbar />
      {showVerifyBanner && (
        <Alert
          severity="warning"
          square
          action={
            <Button color="inherit" size="small" component={RouterLink} to="/check-email">
              Verify now
            </Button>
          }
        >
          Please confirm your email address to subscribe and borrow books.
        </Alert>
      )}
      <Container component="main" maxWidth="lg" className="flex-1 py-6 sm:py-8">
        {/* resetKey: an error on one page doesn't follow you to the next */}
        <ErrorBoundary resetKey={pathname}>
          <Suspense fallback={<PageSkeleton />}>
            <Outlet />
          </Suspense>
        </ErrorBoundary>
      </Container>
      <Box component="footer" sx={{ borderTop: 1, borderColor: 'divider', py: 4 }}>
        <Container maxWidth="lg" className="grid grid-cols-1 gap-6 sm:grid-cols-3">
          <div className="flex flex-col gap-1">
            <span className="sign-caps text-[13px]">{LIBRARY.name}</span>
            <Link href={MAPS_URL} target="_blank" rel="noopener noreferrer" variant="body2" color="text.secondary" underline="hover">
              {LIBRARY.address}
            </Link>
            <Typography variant="body2" color="text.secondary">
              © {YEAR} {LIBRARY.name}
            </Typography>
          </div>
          <div className="flex flex-col gap-1">
            <span className="sign-caps text-[13px]">Opening hours</span>
            {hoursSummary().map((line) => (
              <Typography key={line} variant="body2" color="text.secondary">
                {line}
              </Typography>
            ))}
            <OpenNow className="text-[14px]" />
          </div>
          <div className="flex flex-col gap-1 sm:items-end">
            <Link component={RouterLink} to="/books" variant="body2" color="text.secondary" underline="hover">
              Catalog
            </Link>
            <Link component={RouterLink} to="/plans" variant="body2" color="text.secondary" underline="hover">
              Membership plans
            </Link>
          </div>
        </Container>
      </Box>
    </Box>
  )
}
