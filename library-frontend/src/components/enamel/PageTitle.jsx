/**
 * A page title set on a cobalt street plaque, with optional text beside it
 * (a count, a short status) and actions on the right.
 */
export default function PageTitle({ children, aside, actions }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div className="flex flex-wrap items-center gap-x-5 gap-y-2">
        <h1 className="plaque plaque-sm rivets sign-caps m-0 inline-block px-9 py-2.5 text-lg sm:text-xl">{children}</h1>
        {aside && (
          <span className="text-[15px] font-semibold" style={{ color: 'var(--mui-palette-text-secondary)' }}>
            {aside}
          </span>
        )}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}
