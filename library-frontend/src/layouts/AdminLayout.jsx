import { Suspense } from 'react'
import { Box, Card, List, ListItemButton, ListItemIcon, ListItemText, Tab, Tabs, Typography } from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import MenuBookIcon from '@mui/icons-material/MenuBook'
import CategoryIcon from '@mui/icons-material/Category'
import CardMembershipIcon from '@mui/icons-material/CardMembership'
import PeopleIcon from '@mui/icons-material/People'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong'
import BookmarkIcon from '@mui/icons-material/Bookmark'
import PaymentsIcon from '@mui/icons-material/Payments'
import ScheduleIcon from '@mui/icons-material/Schedule'
import { Link as RouterLink, Outlet, useLocation } from 'react-router-dom'
import PageSkeleton from '../components/PageSkeleton.jsx'

const LINKS = [
  { to: '/admin', label: 'Dashboard', Icon: DashboardIcon, exact: true },
  { to: '/admin/books', label: 'Books', Icon: MenuBookIcon },
  { to: '/admin/genres', label: 'Genres', Icon: CategoryIcon },
  { to: '/admin/plans', label: 'Plans', Icon: CardMembershipIcon },
  { to: '/admin/users', label: 'Users', Icon: PeopleIcon },
  { to: '/admin/loans', label: 'Loans', Icon: SwapHorizIcon },
  { to: '/admin/fines', label: 'Fines', Icon: ReceiptLongIcon },
  { to: '/admin/reservations', label: 'Reservations', Icon: BookmarkIcon },
  { to: '/admin/payments', label: 'Payments', Icon: PaymentsIcon },
  { to: '/admin/jobs', label: 'Jobs & tools', Icon: ScheduleIcon },
]

/** Admin area: a side menu on wide screens, scrollable tabs on phones. */
export default function AdminLayout() {
  const { pathname } = useLocation()
  const current = LINKS.find((l) => (l.exact ? pathname === l.to : pathname.startsWith(l.to)))?.to ?? false

  return (
    <div className="flex flex-col gap-6 md:flex-row">
      <Card variant="outlined" className="hidden md:block w-56 shrink-0 self-start p-2">
        <Typography variant="overline" color="text.secondary" sx={{ px: 1.5 }}>
          Admin
        </Typography>
        <List dense disablePadding>
          {LINKS.map(({ to, label, Icon }) => (
            <ListItemButton key={to} component={RouterLink} to={to} selected={current === to} sx={{ borderRadius: 1 }}>
              <ListItemIcon sx={{ minWidth: 36 }}>
                <Icon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary={label} />
            </ListItemButton>
          ))}
        </List>
      </Card>

      <Box className="md:hidden" sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={current} variant="scrollable" scrollButtons="auto" allowScrollButtonsMobile aria-label="Admin">
          {LINKS.map(({ to, label }) => (
            <Tab key={to} value={to} label={label} component={RouterLink} to={to} />
          ))}
        </Tabs>
      </Box>

      <div className="flex-1 min-w-0">
        <Suspense fallback={<PageSkeleton />}>
          <Outlet />
        </Suspense>
      </div>
    </div>
  )
}
