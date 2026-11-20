import axios from 'axios'
import { toErrorMessage } from '../utils/errors.js'

// Same as authSlice's sessionExpired() action. Written as a plain object so this
// file does not import the slice (the slice imports the API files).
const SESSION_EXPIRED = { type: 'auth/sessionExpired' }

/**
 * The one Axios instance every API file uses.
 *  - baseURL comes from VITE_API_URL (.env.local)
 *  - adds "Authorization: Bearer <jwt>" when logged in
 *  - on 401 (token missing/expired) logs the user out
 *  - rejects with an Error whose .message is the backend's "message"
 *    (the original response stays on error.status / error.data)
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: { 'Content-Type': 'application/json' },
})

// The store is injected from main.jsx instead of imported here,
// which avoids a circular import (store -> slices -> api -> store).
let store
export function injectStore(s) {
  store = s
}

client.interceptors.request.use((config) => {
  const token = store?.getState().auth.token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    // A 401 while we hold a token means the token is no longer valid.
    if (status === 401 && store?.getState().auth.token) {
      store.dispatch(SESSION_EXPIRED)
    }
    const apiError = new Error(toErrorMessage(error))
    apiError.status = status
    apiError.data = error.response?.data
    return Promise.reject(apiError)
  },
)

export default client
