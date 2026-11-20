import { useState } from 'react'
import { genreEnamel } from '../enamel/enamel.js'

/** "On the shelf" strip at the foot of a plate. Hidden when availability is unknown. */
function ShelfStrip({ book }) {
  if (book.availableCopies == null) return null
  const available = book.availableCopies
  const label =
    available > 0
      ? book.totalCopies != null
        ? `${available} of ${book.totalCopies} on the shelf`
        : `${available} on the shelf`
      : 'All out · reserve'
  return (
    <div
      className="inlay absolute inset-x-[10px] bottom-[10px] rounded-[6px] px-2 py-1 text-center sign-caps tabular"
      style={{
        // Fixed light-mode inks: the inlay is always white enamel.
        color: available > 0 ? '#16673e' : '#a8261f',
        fontSize: 'clamp(9px, 6.2cqw, 12px)',
        letterSpacing: '0.05em',
      }}
    >
      {label}
    </div>
  )
}

/**
 * A book as an enamel plate in its genre's colour: genre (its plate of origin),
 * title and author in signage lettering, and the live shelf count at the foot.
 * When the book has a cover image it sits inside the same riveted frame.
 * Always a 2:3 book shape. Sizes follow the plate's own width (container units),
 * so the same plate works small on a shelf and large on the book page.
 */
export default function BookCover({ book, className = '', showShelf = true }) {
  const [failed, setFailed] = useState(false)
  const [loaded, setLoaded] = useState(false)
  const enamel = genreEnamel(book.genreId, book.genreName)
  const showImage = book.coverImageUrl && !failed
  const title = book.title || ''
  // Long titles, and titles with a long word ("Architecture"), step down so they fit the plate.
  const longestWord = title.split(/\s+/).reduce((m, w) => Math.max(m, w.length), 0)
  const titleSize = title.length > 40 || longestWord > 11 ? 9.5 : title.length > 22 || longestWord > 9 ? 11.5 : 13.5

  return (
    <div
      className={`plaque rivets-v relative aspect-[2/3] w-full overflow-hidden ${className}`}
      style={{ '--plate': enamel, containerType: 'inline-size' }}
      role="img"
      aria-label={`${title}${book.author ? ` by ${book.author}` : ''}`}
    >
      {/* The lettered plate is always there; a cover image fades in over it once it has
          loaded, so a slow or missing cover never leaves an empty plate. */}
      <div className="absolute inset-0 flex flex-col px-[11%] pb-[22%] pt-[16%]">
          {book.genreName && (
            <div className="sign-caps opacity-85 line-clamp-1" style={{ fontSize: 'clamp(8px, 6cqw, 12px)' }}>
              {book.genreName}
            </div>
          )}
          <div className="mt-[8%] h-[2px] w-[28%] rounded bg-white/70" />
          <div
            className="mt-[10%] line-clamp-5 font-black leading-[1.02]"
            style={{
              fontSize: `${titleSize}cqw`,
              textWrap: 'balance',
              letterSpacing: '-0.01em',
              overflowWrap: 'anywhere', // a word longer than the plate still breaks instead of being cut
              hyphens: 'auto',
            }}
            lang="en"
          >
            {title}
          </div>
          {book.author && (
            <div className="mt-auto line-clamp-2 font-semibold opacity-90" style={{ fontSize: 'clamp(9px, 7cqw, 15px)' }}>
              {book.author}
            </div>
          )}
      </div>
      {showImage && (
        <img
          src={book.coverImageUrl}
          alt=""
          loading="lazy"
          onLoad={(e) => {
            // Some hosts answer a missing cover with a tiny placeholder image: keep the plate then.
            if (e.currentTarget.naturalWidth > 10) setLoaded(true)
            else setFailed(true)
          }}
          onError={() => setFailed(true)}
          className="absolute inset-[9px] h-[calc(100%-18px)] w-[calc(100%-18px)] rounded-[8px] object-cover transition-opacity duration-300"
          style={{ opacity: loaded ? 1 : 0 }}
        />
      )}
      {showShelf && <ShelfStrip book={book} />}
    </div>
  )
}
