import { useState } from 'react'
import {
  AppBar,
  Box,
  Button,
  Container,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material'
import LocalLibraryIcon from '@mui/icons-material/LocalLibrary'
import MenuIcon from '@mui/icons-material/Menu'
import MenuBookIcon from '@mui/icons-material/MenuBook'
import CardMembershipIcon from '@mui/icons-material/CardMembership'
import HomeIcon from '@mui/icons-material/Home'
import LoginIcon from '@mui/icons-material/Login'
import PersonAddIcon from '@mui/icons-material/PersonAdd'
import { useSelector } from 'react-redux'
import { Link as RouterLink, useLocation } from 'react-router-dom'
import { selectIsLoggedIn } from '../store/authSlice.js'
import ThemeToggle from './ThemeToggle.jsx'
import UserMenu from './UserMenu.jsx'
import { LIBRARY } from '../library.js'

const NAV = [
  { to: '/books', label: 'Catalog', Icon: MenuBookIcon },
  { to: '/plans', label: 'Plans', Icon: CardMembershipIcon },
]

/**
 * Top bar. Wide screens: links in the bar. Phones (below sm): a menu button
 * opens a side drawer with the same links, so nothing gets squeezed.
 */
export default function Navbar() {
  const loggedIn = useSelector(selectIsLoggedIn)
  const location = useLocation()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const isCurrent = (to) => location.pathname === to || location.pathname.startsWith(`${to}/`)

  return (
    <AppBar position="sticky" elevation={0} color="primary">
      <Container maxWidth="lg">
        <Toolbar disableGutters className="gap-1 sm:gap-2">
          <IconButton
            color="inherit"
            edge="start"
            aria-label="Open the menu"
            onClick={() => setDrawerOpen(true)}
            className="sm:hidden"
          >
            <MenuIcon />
          </IconButton>
          {/* The name sits on a small white enamel sign with a cobalt rim. */}
          <RouterLink
            to="/"
            aria-label={`${LIBRARY.name} home`}
            className="plaque-white plaque-sm sign-caps inline-flex items-center gap-1.5 px-3.5 py-1.5 text-[15px] no-underline"
            style={{ color: 'var(--enamel-cobalt)', background: '#f6f7f3', outlineWidth: 1.5, outlineOffset: -4 }}
          >
            <LocalLibraryIcon sx={{ fontSize: 19 }} />
            {/* Full name from 600px up; the short name on phones */}
            <span className="hidden sm:inline">{LIBRARY.name}</span>
            <span className="sm:hidden">{LIBRARY.shortName}</span>
          </RouterLink>

          <div className="hidden sm:flex gap-1 ml-4">
            {NAV.map(({ to, label }) => (
              <Button
                key={to}
                color="inherit"
                component={RouterLink}
                to={to}
                sx={{ fontWeight: isCurrent(to) ? 700 : 500, opacity: isCurrent(to) ? 1 : 0.9 }}
              >
                {label}
              </Button>
            ))}
          </div>

          <Box className="flex-1" />

          <ThemeToggle />
          {loggedIn ? (
            <UserMenu />
          ) : (
            <>
              {/* state.from: come back to this page after logging in */}
              <Button color="inherit" component={RouterLink} to="/login" state={{ from: location }}>
                Log in
              </Button>
              <Button
                variant="outlined"
                color="inherit"
                component={RouterLink}
                to="/signup"
                state={{ from: location }}
                className="hidden sm:inline-flex"
              >
                Sign up
              </Button>
            </>
          )}
        </Toolbar>
      </Container>

      <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)}>
        <Box className="w-64" role="navigation" onClick={() => setDrawerOpen(false)}>
          <div className="flex items-center gap-2 px-4 py-4">
            <LocalLibraryIcon color="primary" />
            <Typography variant="h6" sx={{ fontWeight: 700 }}>
              {LIBRARY.name}
            </Typography>
          </div>
          <Divider />
          <List>
            {[{ to: '/', label: 'Home', Icon: HomeIcon }, ...NAV].map(({ to, label, Icon }) => (
              <ListItemButton key={to} component={RouterLink} to={to} selected={to === '/' ? location.pathname === '/' : isCurrent(to)}>
                <ListItemIcon>
                  <Icon />
                </ListItemIcon>
                <ListItemText primary={label} />
              </ListItemButton>
            ))}
          </List>
          {!loggedIn && (
            <>
              <Divider />
              <List>
                <ListItemButton component={RouterLink} to="/login" state={{ from: location }}>
                  <ListItemIcon>
                    <LoginIcon />
                  </ListItemIcon>
                  <ListItemText primary="Log in" />
                </ListItemButton>
                <ListItemButton component={RouterLink} to="/signup" state={{ from: location }}>
                  <ListItemIcon>
                    <PersonAddIcon />
                  </ListItemIcon>
                  <ListItemText primary="Sign up" />
                </ListItemButton>
              </List>
            </>
          )}
        </Box>
      </Drawer>
    </AppBar>
  )
}
