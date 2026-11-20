/**
 * Turns any Axios error into a readable message.
 * The backend always answers errors as { message, success: false }.
 */
export function toErrorMessage(error) {
  if (error?.response) {
    const data = error.response.data
    if (data && typeof data.message === 'string' && data.message) return data.message
    if (error.response.status === 429) return 'Too many attempts. Please wait a moment and try again.'
    return `Request failed (${error.response.status})`
  }
  if (error?.request) return 'Cannot reach the server. Is the backend running?'
  return error?.message || 'Something went wrong'
}
