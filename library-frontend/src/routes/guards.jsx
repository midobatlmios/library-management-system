import { useSelector } from 'react-redux'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { selectIsAdmin, selectIsLoggedIn } from '../store/authSlice.js'

/**
 * Route guards. Each one wraps a group of routes in AppRoutes and either
 * renders them (<Outlet />) or redirects.
 */

/** Logged-in users only. Otherwise go to /login and remember where we were going. */
export function PrivateRoute() {
  const loggedIn = useSelector(selectIsLoggedIn)
  const location = useLocation()
  if (!loggedIn) return <Navigate to="/login" replace state={{ from: location }} />
  return <Outlet />
}

/** ROLE_ADMIN only. A logged-in member is sent home; a visitor goes to log in. */
export function AdminRoute() {
  const loggedIn = useSelector(selectIsLoggedIn)
  const isAdmin = useSelector(selectIsAdmin)
  const location = useLocation()
  if (!loggedIn) return <Navigate to="/login" replace state={{ from: location }} />
  if (!isAdmin) return <Navigate to="/" replace />
  return <Outlet />
}

/** Login/signup pages: someone already logged in has nothing to do there. */
export function GuestRoute() {
  const loggedIn = useSelector(selectIsLoggedIn)
  if (loggedIn) return <Navigate to="/" replace />
  return <Outlet />
}
