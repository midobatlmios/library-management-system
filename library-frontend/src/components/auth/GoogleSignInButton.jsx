import { useEffect, useRef, useState } from 'react'
import { Typography } from '@mui/material'
import { useColorScheme } from '@mui/material/styles'

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID
const SCRIPT_URL = 'https://accounts.google.com/gsi/client'

// Load Google's script once for the whole app, however many buttons are shown.
let scriptPromise
function loadGoogleScript() {
  if (window.google?.accounts?.id) return Promise.resolve()
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = SCRIPT_URL
      script.async = true
      script.onload = resolve
      script.onerror = () => {
        scriptPromise = null // allow a retry on the next mount
        reject(new Error('Could not load Google sign-in'))
      }
      document.head.appendChild(script)
    })
  }
  return scriptPromise
}

/**
 * Google's own "Sign in with Google" button (Google Identity Services).
 * When the user picks an account, Google gives us a "credential" (an ID token);
 * onCredential sends it to the backend (POST /auth/google).
 *
 * text: 'signin_with' or 'signup_with'
 */
export default function GoogleSignInButton({ onCredential, text = 'signin_with' }) {
  const containerRef = useRef(null)
  const [loadError, setLoadError] = useState(null)
  const { mode, systemMode } = useColorScheme()
  const dark = (mode === 'system' ? systemMode : mode) === 'dark'

  // Google keeps the callback from initialize(); a ref lets it always call the latest prop.
  const callbackRef = useRef(onCredential)
  useEffect(() => {
    callbackRef.current = onCredential
  }, [onCredential])

  useEffect(() => {
    if (!CLIENT_ID) return
    let cancelled = false
    loadGoogleScript()
      .then(() => {
        if (cancelled || !containerRef.current) return
        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: (response) => callbackRef.current(response.credential),
        })
        containerRef.current.innerHTML = ''
        window.google.accounts.id.renderButton(containerRef.current, {
          type: 'standard',
          theme: dark ? 'filled_black' : 'outline',
          size: 'large',
          text,
          shape: 'pill',
          locale: 'en', // otherwise Google follows the browser language

          width: Math.min(containerRef.current.offsetWidth || 320, 400),
        })
      })
      .catch((e) => !cancelled && setLoadError(e.message))
    return () => {
      cancelled = true
    }
  }, [dark, text])

  if (!CLIENT_ID) {
    return (
      <Typography variant="body2" color="text.secondary">
        Google sign-in is off: set VITE_GOOGLE_CLIENT_ID in .env.local.
      </Typography>
    )
  }
  if (loadError) {
    return (
      <Typography variant="body2" color="error">
        {loadError}
      </Typography>
    )
  }
  // Google draws its button inside this div. min-h keeps the layout from jumping.
  return <div ref={containerRef} className="flex min-h-[44px] w-full justify-center" />
}
