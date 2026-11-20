import { Link as RouterLink } from 'react-router-dom'

/**
 * A genre as a street-name plaque: cobalt enamel, white rim, rivets, the name in
 * caps and the number of books on a small white tile. `active` strikes it forward
 * (raised, ochre rim) while the others stay present. Renders as a link when `to`
 * is given, otherwise as a button.
 */
export default function GenreSign({ name, count, to, onClick, active = false, size = 'md' }) {
  const Tag = to ? RouterLink : 'button'
  const pad = size === 'lg' ? 'px-8 py-4 text-base' : 'px-7 py-2.5 text-[13px]'
  return (
    <Tag
      to={to}
      onClick={onClick}
      type={to ? undefined : 'button'}
      aria-pressed={to ? undefined : active}
      className={`plaque plaque-sm rivets plate-lift inline-flex items-center gap-3 no-underline cursor-pointer border-0 ${pad}`}
      style={{
        color: '#fff',
        font: 'inherit',
        ...(active
          ? { outlineColor: 'var(--enamel-ochre)', outlineWidth: 2.5, boxShadow: 'var(--plate-shadow-raised)', transform: 'translateY(-3px)' }
          : null),
      }}
    >
      <span className="sign-caps whitespace-nowrap">{name}</span>
      {count != null && (
        <span
          className="inlay sign-caps tabular rounded-[5px] px-1.5 py-1 text-[11px] leading-none"
          aria-label={`${count} book${count === 1 ? '' : 's'}`}
        >
          {count}
        </span>
      )}
    </Tag>
  )
}
