/**
 * The library's own details, in one place. Change them here and the navbar,
 * footer, home page and tab titles all follow.
 */
export const LIBRARY = {
  name: 'Library Al Azhar',
  shortName: 'Al Azhar',
  address: 'N° 23, Al Azhar, Casablanca',
  timeZone: 'Africa/Casablanca',
}

/**
 * Opening hours per weekday (0 = Sunday … 6 = Saturday), as [open, close] in
 * 24-hour "HH:MM", or null when closed.
 */
export const OPENING_HOURS = {
  1: ['09:00', '17:00'], // Monday
  2: ['09:00', '17:00'],
  3: ['09:00', '17:00'],
  4: ['09:00', '17:00'],
  5: ['09:00', '12:00'], // Friday
  6: ['09:00', '17:00'], // Saturday
  0: null, // Sunday: closed
}

export const DAY_NAMES = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']

/** Rows for an hours table, Monday first: [{ day, dayIndex, hours: [open, close] | null }]. */
export const HOURS_ROWS = [1, 2, 3, 4, 5, 6, 0].map((d) => ({ day: DAY_NAMES[d], dayIndex: d, hours: OPENING_HOURS[d] }))

const toMinutes = (hhmm) => {
  const [h, m] = hhmm.split(':').map(Number)
  return h * 60 + m
}

/** "17:00" -> "17:00"; kept as 24-hour time, the way it is read in Morocco. */
export const formatTime = (hhmm) => hhmm

/** The weekday and minutes since midnight in Casablanca, whatever the visitor's own timezone. */
function casablancaNow(date = new Date()) {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: LIBRARY.timeZone,
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date)
  const get = (t) => parts.find((p) => p.type === t)?.value
  const day = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].indexOf(get('weekday'))
  return { day, minutes: Number(get('hour')) * 60 + Number(get('minute')) }
}

/**
 * Is the library open right now?
 *   { open: true,  today, label: 'Open now · closes at 17:00' }
 *   { open: false, today, label: 'Closed · opens Monday at 09:00' }
 */
export function openStatus(date = new Date()) {
  const { day, minutes } = casablancaNow(date)
  const today = OPENING_HOURS[day]
  if (today && minutes >= toMinutes(today[0]) && minutes < toMinutes(today[1])) {
    return { open: true, today: day, label: `Open now · closes at ${formatTime(today[1])}` }
  }
  // Later today?
  if (today && minutes < toMinutes(today[0])) {
    return { open: false, today: day, label: `Closed · opens today at ${formatTime(today[0])}` }
  }
  // The next day that opens.
  for (let i = 1; i <= 7; i++) {
    const d = (day + i) % 7
    if (OPENING_HOURS[d]) {
      const when = i === 1 ? 'tomorrow' : DAY_NAMES[d]
      return { open: false, today: day, label: `Closed · opens ${when} at ${formatTime(OPENING_HOURS[d][0])}` }
    }
  }
  return { open: false, today: day, label: 'Closed' }
}

/**
 * The hours as short lines, days with the same hours grouped:
 * ["Mon–Thu, Sat 09:00–17:00", "Fri 09:00–12:00", "Sun closed"]
 */
export function hoursSummary() {
  const short = (i) => DAY_NAMES[i].slice(0, 3)
  const order = [1, 2, 3, 4, 5, 6, 0] // Monday first
  const groups = new Map() // "09:00–17:00" or "closed" -> [day indexes]
  for (const d of order) {
    const h = OPENING_HOURS[d]
    const key = h ? `${h[0]}–${h[1]}` : 'closed'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(d)
  }
  return [...groups].map(([key, days]) => {
    // Runs of consecutive days become "Mon–Thu"; single days stay "Sat".
    const positions = days.map((d) => order.indexOf(d))
    const runs = []
    positions.forEach((p, i) => {
      if (i && p === positions[i - 1] + 1) runs[runs.length - 1].push(p)
      else runs.push([p])
    })
    const label = runs
      .map((r) => (r.length > 2 ? `${short(order[r[0]])}–${short(order[r[r.length - 1]])}` : r.map((p) => short(order[p])).join(', ')))
      .join(', ')
    return `${label} ${key}`
  })
}

/** A Google Maps search link for the address. */
export const MAPS_URL = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${LIBRARY.address}, Morocco`)}`
