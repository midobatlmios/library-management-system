import { useEffect, useState } from 'react'
import { openStatus } from '../../library.js'

/**
 * "Open now · closes at 17:00" or "Closed · opens Monday at 09:00", worked out in
 * Casablanca time and refreshed every minute. A green or red dot is paired with
 * the words, so the state never depends on colour alone.
 */
export default function OpenNow({ className = '', style }) {
  const [status, setStatus] = useState(() => openStatus())
  useEffect(() => {
    const timer = setInterval(() => setStatus(openStatus()), 60_000)
    return () => clearInterval(timer)
  }, [])

  return (
    <span className={`inline-flex items-center gap-2 font-semibold ${className}`} style={style} role="status">
      <span
        aria-hidden="true"
        className="inline-block h-2.5 w-2.5 shrink-0 rounded-full"
        style={{
          background: status.open ? '#2fbf71' : '#d9534a',
          boxShadow: `0 0 0 3px ${status.open ? 'rgba(47,191,113,0.25)' : 'rgba(217,83,74,0.25)'}`,
        }}
      />
      {status.label}
    </span>
  )
}
