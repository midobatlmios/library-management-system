import { createContext, useContext } from 'react'

/** Filled by <ToastProvider>. Kept in its own file so the provider file only exports a component. */
export const ToastContext = createContext(null)

/**
 *   const toast = useToast()
 *   toast('Saved')                          // success
 *   toast(e.message, 'error')
 *   toast('Borrowed!', 'success', '/loans') // with a "View" link
 */
export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>')
  return ctx
}
