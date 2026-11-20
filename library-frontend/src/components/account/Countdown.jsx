import { useEffect, useState } from 'react'
import { formatDuration } from '../../utils/format.js'

/** "2 d 4 h" until a date-time, updated every 30 seconds. */
export default function Countdown({ until }) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 30_000)
    return () => clearInterval(timer)
  }, [])
  return <>{formatDuration(new Date(until).getTime() - now)}</>
}
