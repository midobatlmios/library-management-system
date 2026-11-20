import { useCallback, useState } from 'react'
import { Alert, Skeleton } from '@mui/material'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import EastRoundedIcon from '@mui/icons-material/EastRounded'
import TravelExploreRoundedIcon from '@mui/icons-material/TravelExploreRounded'
import BookmarkAddRoundedIcon from '@mui/icons-material/BookmarkAddRounded'
import MarkEmailReadRoundedIcon from '@mui/icons-material/MarkEmailReadRounded'
import AssignmentReturnRoundedIcon from '@mui/icons-material/AssignmentReturnRounded'
import { useSelector } from 'react-redux'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { getBookStats, searchBooks } from '../api/books.js'
import { getGenreHierarchy } from '../api/genres.js'
import { getPlans } from '../api/subscriptions.js'
import BookCard from '../components/books/BookCard.jsx'
import GenreSign from '../components/enamel/GenreSign.jsx'
import SignHeading from '../components/enamel/SignHeading.jsx'
import OpenNow from '../components/enamel/OpenNow.jsx'
import VisitUs from '../components/enamel/VisitUs.jsx'
import { selectUser } from '../store/authSlice.js'
import { formatMoney } from '../utils/format.js'
import { useAsync } from '../utils/useAsync.js'

// ---------------------------------------------------------------- the search plaque

function SearchPlaque({ stats, genres }) {
  const navigate = useNavigate()
  const [q, setQ] = useState('')

  // Once the catalog has books, the sign shows the genres that have books, biggest first
  // (at most 12) plus "All genres". With an empty catalog every genre is shown.
  const MAX_GENRES = 12
  const all = genres ?? []
  const withBooks = all.filter((g) => g.bookCount > 0).sort((a, b) => b.bookCount - a.bookCount)
  const shownGenres = withBooks.length ? withBooks.slice(0, MAX_GENRES) : all
  const hiddenCount = all.length - shownGenres.length

  const submit = (e) => {
    e.preventDefault()
    const term = q.trim()
    navigate(term ? `/books?q=${encodeURIComponent(term)}` : '/books')
  }

  return (
    <section aria-labelledby="find-heading" className="plaque relative px-6 pb-9 pt-10 sm:px-12 sm:pb-12 sm:pt-14">
      {/* four corner rivets hold the sign to the wall */}
      <span className="rivet left-4 top-4" />
      <span className="rivet right-4 top-4" />
      <span className="rivet bottom-4 left-4" />
      <span className="rivet bottom-4 right-4" />

      <h1
        id="find-heading"
        className="sign-caps m-0 leading-[0.95]"
        style={{ fontSize: 'clamp(2.1rem, 6.4vw, 4.6rem)', letterSpacing: '0.01em', textWrap: 'balance' }}
      >
        Find your next book
      </h1>

      {/* The search is set into the sign: a white enamel field and a plate button. */}
      <form onSubmit={submit} role="search" className="mt-7 flex max-w-3xl flex-col gap-3 sm:flex-row">
        <label className="relative flex-1">
          <span className="sr-only">Search title, author or ISBN</span>
          <SearchRoundedIcon
            className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2"
            sx={{ color: 'var(--enamel-cobalt)', fontSize: 26 }}
          />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Title, author or ISBN"
            className="w-full rounded-[10px] border-0 py-4 pl-13 pr-4 text-lg font-semibold"
            style={{
              background: '#f6f7f3',
              color: '#12151c',
              boxShadow: 'inset 0 2px 4px rgba(10,18,40,0.25)',
              font: 'inherit',
              fontSize: '1.125rem',
            }}
          />
        </label>
        <button
          type="submit"
          className="sign-caps plate-lift cursor-pointer rounded-[10px] border-0 px-8 py-4 text-base"
          style={{
            background: 'var(--enamel-ochre)',
            color: '#12151c',
            outline: '1.5px solid rgba(255,255,255,0.85)',
            outlineOffset: -5,
            boxShadow: 'var(--plate-shadow)',
          }}
        >
          Search
        </button>
      </form>

      {/* One figure, one truth. */}
      <p className="mb-0 mt-4 text-[15px] font-semibold text-white/90 tabular">
        {stats ? (
          <>
            <span
              className="mr-2 inline-block h-2.5 w-2.5 rounded-full align-middle"
              style={{ background: '#5fe3a0', boxShadow: '0 0 0 3px rgba(95,227,160,0.25)' }}
            />
            {stats.totalActiveBooks === 0
              ? 'No books in the catalog yet'
              : `${stats.totalAvailableBooks} of ${stats.totalActiveBooks} titles have a copy on the shelf right now`}
          </>
        ) : (
          ' '
        )}
      </p>
      <OpenNow className="mt-2 text-[15px] text-white/90" />

      {shownGenres.length > 0 && (
        <nav aria-label="Genres" className="mt-8 border-t-2 border-white/25 pt-7">
          {/* Phones: one row that scrolls sideways. Wider screens: the signs wrap. */}
          <ul className="-mx-2 m-0 flex list-none gap-3 overflow-x-auto px-2 pb-3 pt-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0">
            {shownGenres.map((g) => (
              <li key={g.id} className="shrink-0">
                <GenreSign name={g.name} count={g.bookCount} to={`/books?genre=${g.id}`} />
              </li>
            ))}
            {hiddenCount > 0 && (
              <li className="shrink-0">
                <GenreSign name={`All ${genres.length} genres`} to="/books" />
              </li>
            )}
          </ul>
        </nav>
      )}
    </section>
  )
}

// ---------------------------------------------------------------- shelves

function Shelf({ id, title, linkTo, data, error, emptyText }) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-5">
      <SignHeading id={id} linkTo={linkTo}>
        {title}
      </SignHeading>
      {error ? (
        <Alert severity="error">{error}</Alert>
      ) : !data ? (
        <div className="shelf">
          {[0, 1, 2, 3, 4].map((i) => (
            <Skeleton key={i} variant="rounded" className="aspect-[2/3] !h-auto !rounded-[14px]" />
          ))}
        </div>
      ) : data.empty ? (
        <p className="m-0 text-[15px]" style={{ color: 'var(--mui-palette-text-secondary)' }}>
          {emptyText}
        </p>
      ) : (
        <ul className="shelf m-0 list-none" aria-label={title}>
          {data.content.map((b) => (
            <li key={b.id}>
              <BookCard book={b} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

// ---------------------------------------------------------------- how borrowing works

const STEPS = [
  {
    Icon: TravelExploreRoundedIcon,
    verb: 'Find',
    text: 'Search the catalog. Every book shows how many copies are on the shelf right now.',
  },
  {
    Icon: BookmarkAddRoundedIcon,
    verb: 'Borrow or reserve',
    text: 'Borrow a copy that is on the shelf, or reserve the book to join its waiting list.',
  },
  {
    Icon: MarkEmailReadRoundedIcon,
    verb: 'Pick up',
    text: 'When a reserved copy comes back, we email you and hold it for you for a short time.',
  },
  {
    Icon: AssignmentReturnRoundedIcon,
    verb: 'Return',
    text: 'Bring it back by the due date, or renew it online if you need longer.',
  },
]

function HowItWorks() {
  return (
    <section aria-labelledby="how-heading" className="flex flex-col gap-5">
      <SignHeading id="how-heading">How borrowing works</SignHeading>
      {/* The order is the information here, so the steps carry numbers. */}
      <ol className="m-0 grid list-none grid-cols-1 gap-4 p-0 sm:grid-cols-2 lg:grid-cols-4">
        {STEPS.map(({ Icon, verb, text }, i) => (
          <li key={verb} className="plaque-white relative flex flex-col gap-3 px-6 pb-6 pt-7">
            <div className="flex items-center justify-between">
              <span
                className="plaque plaque-sm sign-caps tabular inline-flex h-10 w-10 items-center justify-center text-lg"
                aria-hidden="true"
              >
                {i + 1}
              </span>
              {i < STEPS.length - 1 ? (
                <EastRoundedIcon aria-hidden="true" sx={{ color: 'var(--enamel-cobalt)', fontSize: 30 }} className="hidden lg:block" />
              ) : null}
            </div>
            <div className="flex items-center gap-2">
              <Icon aria-hidden="true" sx={{ color: 'var(--enamel-cobalt)' }} />
              <h3 className="sign-caps m-0 text-[17px]">{verb}</h3>
            </div>
            <p className="m-0 text-[15px] leading-relaxed" style={{ color: 'var(--mui-palette-text-secondary)' }}>
              {text}
            </p>
          </li>
        ))}
      </ol>
    </section>
  )
}

// ---------------------------------------------------------------- membership

function MembershipPlaque({ plans, user }) {
  const paid = (plans || []).filter((p) => Number(p.price) > 0)
  const free = (plans || []).some((p) => Number(p.price) === 0)
  const cheapest = paid.length ? paid.reduce((a, b) => (Number(a.price) <= Number(b.price) ? a : b)) : null
  const mostBooks = (plans || []).reduce((m, p) => Math.max(m, p.maxBooksAllowed || 0), 0)

  return (
    <section
      aria-labelledby="member-heading"
      className="plaque relative flex flex-col gap-6 px-6 py-10 sm:px-12 md:flex-row md:items-center md:justify-between"
      style={{ '--plate': 'var(--enamel-green)' }}
    >
      <span className="rivet left-4 top-4" />
      <span className="rivet right-4 top-4" />
      <span className="rivet bottom-4 left-4" />
      <span className="rivet bottom-4 right-4" />
      <div className="max-w-2xl">
        <h2 id="member-heading" className="sign-caps m-0 text-[clamp(1.6rem,3.6vw,2.4rem)] leading-none">
          {user ? 'Your membership' : 'Become a member'}
        </h2>
        <p className="mb-0 mt-3 text-[16px] leading-relaxed text-white/90">
          {plans == null
            ? ' '
            : plans.length === 0
              ? 'Membership plans will be listed here soon.'
              : [
                  free ? 'Start with a free plan' : cheapest && `Plans from ${formatMoney(cheapest.price, cheapest.currency)}`,
                  mostBooks > 0 && `borrow up to ${mostBooks} books at a time`,
                  'reserve books that are out',
                ]
                  .filter(Boolean)
                  .join(', ') + '.'}
        </p>
      </div>
      <div className="flex flex-wrap gap-3">
        <RouterLink
          to={user ? '/subscription' : '/plans'}
          className="sign-caps plate-lift rounded-[10px] px-7 py-3.5 text-[15px] no-underline"
          style={{ background: '#f6f7f3', color: 'var(--enamel-green)', boxShadow: 'var(--plate-shadow)' }}
        >
          {user ? 'My subscription' : 'See the plans'}
        </RouterLink>
        {!user && (
          <RouterLink
            to="/signup"
            className="sign-caps rounded-[10px] px-7 py-3.5 text-[15px] text-white no-underline"
            style={{ outline: '2px solid rgba(255,255,255,0.85)', outlineOffset: -2 }}
          >
            Create an account
          </RouterLink>
        )}
      </div>
    </section>
  )
}

// ---------------------------------------------------------------- page

export default function HomePage() {
  const user = useSelector(selectUser)
  const stats = useAsync(getBookStats)
  const genres = useAsync(getGenreHierarchy)
  const plans = useAsync(getPlans)
  const loadOnShelf = useCallback(
    () => searchBooks({ availableOnly: true, size: 12, sortBy: 'availableCopies', sortDirection: 'DESC' }),
    [],
  )
  const loadNew = useCallback(() => searchBooks({ size: 12, sortBy: 'createdAt', sortDirection: 'DESC' }), [])
  const onShelf = useAsync(loadOnShelf)
  const newest = useAsync(loadNew)

  return (
    <div className="flex flex-col gap-14 sm:gap-16">
      <SearchPlaque stats={stats.data} genres={genres.data} />

      {user && (
        <p className="-mt-8 mb-0 text-[15px] sm:-mt-10" style={{ color: 'var(--mui-palette-text-secondary)' }}>
          Signed in as <strong style={{ color: 'var(--mui-palette-text-primary)' }}>{user.fullName}</strong>.{' '}
          <RouterLink to="/loans" style={{ color: 'var(--mui-palette-primary-main)' }} className="font-bold">
            My loans
          </RouterLink>
          {' · '}
          <RouterLink to="/reservations" style={{ color: 'var(--mui-palette-primary-main)' }} className="font-bold">
            My reservations
          </RouterLink>
        </p>
      )}

      <Shelf
        id="on-shelf-heading"
        title="On the shelf now"
        linkTo="/books?available=1&sort=available"
        data={onShelf.data}
        error={onShelf.error}
        emptyText="Every copy is out on loan right now. Reserve a book to join its waiting list."
      />

      <Shelf
        id="new-heading"
        title="New in the library"
        linkTo="/books"
        data={newest.data}
        error={newest.error}
        emptyText="No books in the catalog yet."
      />

      <HowItWorks />

      <VisitUs />

      <MembershipPlaque plans={plans.data} user={user} />
    </div>
  )
}
