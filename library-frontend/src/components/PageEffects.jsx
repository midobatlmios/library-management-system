import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

// The browser tab title for each page (first match wins).
const TITLES = [
  [/^\/$/, 'Home'],
  [/^\/books\/\d+/, 'Book'],
  [/^\/books/, 'Catalog'],
  [/^\/plans/, 'Plans'],
  [/^\/login/, 'Log in'],
  [/^\/signup/, 'Sign up'],
  [/^\/forgot-password/, 'Forgot password'],
  [/^\/reset-password/, 'Reset password'],
  [/^\/check-email/, 'Check your email'],
  [/^\/payment\/result/, 'Payment'],
  [/^\/loans/, 'My loans'],
  [/^\/reservations/, 'My reservations'],
  [/^\/fines/, 'My fines'],
  [/^\/wishlist/, 'My wishlist'],
  [/^\/my-reviews/, 'My reviews'],
  [/^\/subscription/, 'My subscription'],
  [/^\/payments/, 'My payments'],
  [/^\/profile/, 'Profile'],
  [/^\/admin\/?$/, 'Admin'],
  [/^\/admin\/(\w+)/, (m) => `Admin · ${m[1][0].toUpperCase()}${m[1].slice(1)}`],
]

/**
 * Small things that happen on every page change:
 *  - scroll back to the top (a SPA keeps the old scroll position otherwise)
 *  - set the browser tab title
 * Filter changes on the same page (?page=2) do not scroll, only a new path does.
 */
export default function PageEffects() {
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  useEffect(() => {
    for (const [re, title] of TITLES) {
      const m = pathname.match(re)
      if (m) {
        document.title = `${typeof title === 'function' ? title(m) : title} · Library`
        return
      }
    }
    document.title = 'Library'
  }, [pathname])

  return null
}
