import { useEffect, useState } from 'react'
import PlaceRoundedIcon from '@mui/icons-material/PlaceRounded'
import { HOURS_ROWS, LIBRARY, MAPS_URL, openStatus } from '../../library.js'
import OpenNow from './OpenNow.jsx'
import SignHeading from './SignHeading.jsx'

/**
 * "Visit us": the library's door plate. Name, address with a map link, the live
 * open/closed status, and the week's hours with today (in Casablanca) marked.
 */
export default function VisitUs() {
  const [today, setToday] = useState(() => openStatus().today)
  useEffect(() => {
    const timer = setInterval(() => setToday(openStatus().today), 60_000)
    return () => clearInterval(timer)
  }, [])

  return (
    <section aria-labelledby="visit-heading" className="flex flex-col gap-5">
      <SignHeading id="visit-heading">Visit us</SignHeading>
      <div className="plaque-white grid grid-cols-1 gap-8 px-7 py-8 sm:px-10 md:grid-cols-2">
        <div className="flex flex-col gap-3">
          <p className="sign-caps m-0 text-[clamp(1.4rem,3vw,2rem)] leading-none" style={{ color: 'var(--mui-palette-primary-main)' }}>
            {LIBRARY.name}
          </p>
          <a
            href={MAPS_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 text-[16px] font-semibold no-underline underline-offset-4 hover:underline"
            style={{ color: 'var(--mui-palette-text-primary)' }}
          >
            <PlaceRoundedIcon sx={{ color: 'var(--mui-palette-primary-main)' }} />
            {LIBRARY.address}
          </a>
          <OpenNow className="text-[15px]" style={{ color: 'var(--mui-palette-text-primary)' }} />
          <p className="m-0 mt-2 text-[15px] leading-relaxed" style={{ color: 'var(--mui-palette-text-secondary)' }}>
            Borrow and return books at the desk during opening hours. Reservations and renewals work online at any time.
          </p>
        </div>

        <table className="w-full border-collapse self-start text-[15px] tabular">
          <caption className="sign-caps pb-2 text-left text-[12px]" style={{ color: 'var(--mui-palette-text-secondary)' }}>
            Opening hours
          </caption>
          <tbody>
            {HOURS_ROWS.map(({ day, dayIndex, hours }) => {
              const isToday = dayIndex === today
              return (
                <tr
                  key={day}
                  aria-current={isToday ? 'date' : undefined}
                  style={
                    isToday
                      ? { background: 'var(--enamel-cobalt)', color: '#fff' }
                      : { borderBottom: '1px solid var(--mui-palette-divider)' }
                  }
                >
                  <th scope="row" className={`px-3 py-2 text-left font-bold ${isToday ? 'rounded-l-[8px]' : ''}`}>
                    {day}
                    {isToday && <span className="sign-caps ml-2 text-[10px] opacity-90">Today</span>}
                  </th>
                  <td className={`px-3 py-2 text-right ${isToday ? 'rounded-r-[8px] font-bold' : ''}`}>
                    {hours ? `${hours[0]} – ${hours[1]}` : 'Closed'}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}
