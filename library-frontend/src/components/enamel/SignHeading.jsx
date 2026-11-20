import EastRoundedIcon from '@mui/icons-material/EastRounded'
import { Link as RouterLink } from 'react-router-dom'

/**
 * A section heading set on a small cobalt street plaque (the heading itself,
 * not a label above one), with an optional "see all" link beside it.
 */
export default function SignHeading({ children, id, linkTo, linkLabel = 'See all' }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h2 id={id} className="plaque plaque-sm rivets sign-caps m-0 inline-block px-8 py-2 text-[15px] sm:text-base">
        {children}
      </h2>
      {linkTo && (
        <RouterLink
          to={linkTo}
          className="sign-caps inline-flex items-center gap-1 text-[13px] no-underline underline-offset-4 hover:underline"
          // The theme's primary: cobalt in light mode, a lighter blue that reads on the night ground in dark mode.
          style={{ color: 'var(--mui-palette-primary-main)' }}
        >
          {linkLabel}
          <EastRoundedIcon sx={{ fontSize: 16 }} />
        </RouterLink>
      )}
    </div>
  )
}
