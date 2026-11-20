/** "2026-11-20" or "2026-11-20T10:30:00" -> "20 Nov 2026". Parsed as a local date (no timezone shift). */
export function formatDate(value) {
  if (!value) return ''
  const [y, m, d] = value.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

/**
 * A publication date. Books often only have a year, stored as YYYY-01-01:
 * those show as "2012" instead of claiming "1 Jan 2012".
 */
export function formatPublished(value) {
  if (!value) return ''
  return value.slice(5, 10) === '01-01' ? value.slice(0, 4) : formatDate(value)
}

/** "2026-11-20T10:30:00" -> "20 Nov 2026, 10:30". The backend sends local times without a zone. */
export function formatDateTime(value) {
  if (!value) return ''
  return new Date(value).toLocaleString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** 199 or "199.00" with "MAD" -> "199.00 MAD". Use the backend's ...InMajorUnits fields. */
export function formatMoney(major, currency = 'MAD') {
  const n = Number(major ?? 0)
  // 21382 -> "21,382.00 MAD"
  return `${n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`
}

/** Whole days from today to a "YYYY-MM-DD" date (negative = in the past). */
export function daysFromToday(value) {
  if (!value) return null
  const [y, m, d] = value.slice(0, 10).split('-').map(Number)
  const target = new Date(y, m - 1, d)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((target - today) / 86_400_000)
}

/** Milliseconds left -> "2 d 4 h", "3 h 12 min", "8 min". */
export function formatDuration(ms) {
  if (ms <= 0) return 'now'
  const minutes = Math.floor(ms / 60_000)
  const days = Math.floor(minutes / 1440)
  const hours = Math.floor((minutes % 1440) / 60)
  const mins = minutes % 60
  if (days > 0) return `${days} d ${hours} h`
  if (hours > 0) return `${hours} h ${mins} min`
  return `${Math.max(mins, 1)} min`
}
