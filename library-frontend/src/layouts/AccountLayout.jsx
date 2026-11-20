import { Suspense } from 'react'
import { Box, Tab, Tabs } from '@mui/material'
import { Link as RouterLink, Outlet, useLocation } from 'react-router-dom'
import PageSkeleton from '../components/PageSkeleton.jsx'

// The member area. Each tab is a page with its own URL.
const TABS = [
  { to: '/loans', label: 'Loans' },
  { to: '/reservations', label: 'Reservations' },
  { to: '/fines', label: 'Fines' },
  { to: '/wishlist', label: 'Wishlist' },
  { to: '/my-reviews', label: 'Reviews' },
  { to: '/subscription', label: 'Subscription' },
  { to: '/payments', label: 'Payments' },
  { to: '/profile', label: 'Profile' },
]

/** Tabs on top of every "My ..." page. Scrollable on phones. */
export default function AccountLayout() {
  const { pathname } = useLocation()
  const current = TABS.find((t) => pathname.startsWith(t.to))?.to ?? false

  return (
    <div className="flex flex-col gap-6">
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={current} variant="scrollable" scrollButtons="auto" allowScrollButtonsMobile aria-label="My account">
          {TABS.map((t) => (
            <Tab key={t.to} value={t.to} label={t.label} component={RouterLink} to={t.to} />
          ))}
        </Tabs>
      </Box>
      <Suspense fallback={<PageSkeleton />}>
        <Outlet />
      </Suspense>
    </div>
  )
}
