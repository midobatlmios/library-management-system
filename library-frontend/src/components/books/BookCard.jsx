import { Link as RouterLink } from 'react-router-dom'
import BookCover from './BookCover.jsx'

/**
 * One book in a grid or on a shelf: the enamel plate itself is the link.
 * The plate already carries genre, title, author and the shelf count, so
 * nothing is repeated under it.
 */
export default function BookCard({ book }) {
  return (
    <RouterLink
      to={`/books/${book.id}`}
      className="plate-lift block rounded-[14px] no-underline"
      aria-label={`${book.title} by ${book.author}${
        book.availableCopies != null ? `, ${book.availableCopies} of ${book.totalCopies} on the shelf` : ''
      }`}
    >
      <BookCover book={book} />
    </RouterLink>
  )
}
