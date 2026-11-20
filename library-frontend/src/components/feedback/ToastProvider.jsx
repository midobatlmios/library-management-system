import { useCallback, useState } from 'react'
import { Alert, Button, Snackbar } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { ToastContext } from './toastContext.js'

/** One toast (snackbar) for the whole app. Components show one with useToast() (toastContext.js). */
export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null)

  const show = useCallback((message, severity = 'success', link) => {
    // A new key restarts the auto-hide timer even when the text is the same.
    setToast({ message, severity, link, key: Date.now() })
  }, [])

  const close = (_, reason) => {
    if (reason !== 'clickaway') setToast(null)
  }

  return (
    <ToastContext.Provider value={show}>
      {children}
      <Snackbar
        key={toast?.key}
        open={Boolean(toast)}
        autoHideDuration={toast?.severity === 'error' ? 8000 : 5000}
        onClose={close}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        {toast ? (
          <Alert
            severity={toast.severity}
            variant="filled"
            onClose={close}
            action={
              toast.link && (
                <Button color="inherit" size="small" component={RouterLink} to={toast.link} onClick={close}>
                  View
                </Button>
              )
            }
          >
            {toast.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </ToastContext.Provider>
  )
}
