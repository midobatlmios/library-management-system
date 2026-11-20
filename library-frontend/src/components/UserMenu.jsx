import { useEffect, useRef, useState } from 'react'
import { Avatar, Chip, Divider, IconButton, ListItemIcon, Menu, MenuItem, Typography } from '@mui/material'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
import LogoutIcon from '@mui/icons-material/Logout'
import MarkEmailUnreadIcon from '@mui/icons-material/MarkEmailUnread'
import MenuBookIcon from '@mui/icons-material/MenuBook'
import BookmarkIcon from '@mui/icons-material/Bookmark'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong'
import FavoriteIcon from '@mui/icons-material/Favorite'
import RateReviewIcon from '@mui/icons-material/RateReview'
import PersonIcon from '@mui/icons-material/Person'
import CardMembershipIcon from '@mui/icons-material/CardMembership'
import PaymentsIcon from '@mui/icons-material/Payments'
import { useDispatch, useSelector } from 'react-redux'
import { useLocation, useNavigate } from 'react-router-dom'
import { loggedOut, selectIsAdmin, selectUser } from '../store/authSlice.js'
import { useToast } from './feedback/toastContext.js'

const ACCOUNT_LINKS = [
  { to: '/loans', label: 'My loans', Icon: MenuBookIcon },
  { to: '/reservations', label: 'My reservations', Icon: BookmarkIcon },
  { to: '/fines', label: 'My fines', Icon: ReceiptLongIcon },
  { to: '/wishlist', label: 'My wishlist', Icon: FavoriteIcon },
  { to: '/my-reviews', label: 'My reviews', Icon: RateReviewIcon },
  { to: '/subscription', label: 'My subscription', Icon: CardMembershipIcon },
  { to: '/payments', label: 'My payments', Icon: PaymentsIcon },
  { to: '/profile', label: 'Profile', Icon: PersonIcon },
]

function initials(name = '') {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('')
}

/** Avatar button in the navbar with the account menu. */
export default function UserMenu() {
  const user = useSelector(selectUser)
  const isAdmin = useSelector(selectIsAdmin)
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const toast = useToast()
  const { pathname } = useLocation()
  const [anchor, setAnchor] = useState(null)

  const go = (path) => {
    setAnchor(null)
    navigate(path)
  }

  // Logging out = go home, THEN clear the session. If we cleared it first, the
  // guard of a private page would see "logged out" and send us to /login.
  // React Router navigates in a transition (later than the Redux update), so
  // wait until the URL really is "/" before clearing.
  const loggingOut = useRef(false)
  useEffect(() => {
    if (loggingOut.current && pathname === '/') {
      loggingOut.current = false
      dispatch(loggedOut())
      toast('You are logged out', 'info')
    }
  }, [pathname, dispatch, toast])

  const handleLogout = () => {
    setAnchor(null)
    if (pathname === '/') {
      dispatch(loggedOut()) // already home: nothing to wait for
      toast('You are logged out', 'info')
    } else {
      loggingOut.current = true
      navigate('/')
    }
  }

  return (
    <>
      <IconButton onClick={(e) => setAnchor(e.currentTarget)} aria-label="Account menu" sx={{ p: 0.5 }}>
        <Avatar src={user?.profileImage || undefined} sx={{ width: 34, height: 34, bgcolor: 'secondary.main' }}>
          {initials(user?.fullName)}
        </Avatar>
      </IconButton>
      <Menu
        anchorEl={anchor}
        open={Boolean(anchor)}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <div className="px-4 py-2">
          <Typography sx={{ fontWeight: 600 }}>{user?.fullName}</Typography>
          <Typography variant="body2" color="text.secondary">
            {user?.email}
          </Typography>
          {isAdmin && <Chip label="Admin" size="small" color="secondary" sx={{ mt: 1 }} />}
        </div>
        <Divider />
        {ACCOUNT_LINKS.map(({ to, label, Icon }) => (
          <MenuItem key={to} onClick={() => go(to)}>
            <ListItemIcon>
              <Icon fontSize="small" />
            </ListItemIcon>
            {label}
          </MenuItem>
        ))}
        <Divider />
        {user && !user.verified && (
          <MenuItem onClick={() => go('/check-email')}>
            <ListItemIcon>
              <MarkEmailUnreadIcon fontSize="small" />
            </ListItemIcon>
            Verify your email
          </MenuItem>
        )}
        {isAdmin && (
          <MenuItem onClick={() => go('/admin')}>
            <ListItemIcon>
              <AdminPanelSettingsIcon fontSize="small" />
            </ListItemIcon>
            Admin
          </MenuItem>
        )}
        <MenuItem onClick={handleLogout}>
          <ListItemIcon>
            <LogoutIcon fontSize="small" />
          </ListItemIcon>
          Log out
        </MenuItem>
      </Menu>
    </>
  )
}
