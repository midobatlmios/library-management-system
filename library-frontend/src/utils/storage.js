// Small wrappers around localStorage. They never throw (private mode, blocked storage...).

const TOKEN_KEY = 'library.jwt'
const USER_KEY = 'library.user'

export function loadAuth() {
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    const user = JSON.parse(localStorage.getItem(USER_KEY) || 'null')
    return { token, user }
  } catch {
    return { token: null, user: null }
  }
}

export function saveAuth(token, user) {
  try {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } catch {
    /* ignore */
  }
}

export function clearAuth() {
  try {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  } catch {
    /* ignore */
  }
}
