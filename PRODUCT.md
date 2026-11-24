# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

- **Members and visitors of a real local library.** People who borrow paper books from a physical library. They come to the website mainly to find a book: search the catalog, see whether a copy is on the shelf right now, and borrow or reserve it. They use it on phones and on computers.
- **Library staff (admins).** They manage the catalog, plans, loans at the desk, fines, reservations and payments from the admin area.

## Product Purpose

The website of a physical library: a public catalog with live availability of physical copies, and a member area for loans, reservations, fines, wishlist, reviews, subscription and payments. Success on the public side means a visitor finds a book quickly and knows whether they can get it.

## Positioning

A real library's own catalog: the copies, shelves, waiting lists and desk exist in the physical world, and the site shows their live state (copies available, queue position, held-until time). It is not a store and not an e-book service.

## Operating Context

- Members borrow with a paid or free subscription plan (prices in MAD, Morocco). Plans set how many books at a time and for how many days.
- Returned copies go to the next person in the reservation queue and are held for a limited time.
- Late returns create fines; unpaid fines block borrowing.
- Payments use a simulated checkout for now (no real money).
- Email notifications: verification, password reset, reminders, "your reserved book is ready".

## Capabilities and Constraints

- Stack: React 19 + Vite, MUI v9, Tailwind CSS 4 (preflight off, MUI in its own CSS layer), Redux Toolkit, React Router 7, Axios. Backend: Spring Boot.
- Interface language: English. Currency: MAD.
- Light and dark mode are both supported.
- Book data: title, author, ISBN, genre (a tree: genres and sub-genres), publisher, publication date, language, pages, description, total and available copies, optional cover image URL, ratings and reviews.
- Undecided: the library's name (the site currently says "Library"), its address, opening hours and logo.

## Evidence on Hand

- The catalog currently holds 12 books, many of them test data; none has a cover image. Designs must look complete without cover images.
- No logo, photos of the library, testimonials, opening hours or address exist. Do not invent them; leave clearly marked placeholders if a design needs them.

## Product Principles

1. Finding a book comes first: search and availability are always one step away.
2. Show the truth about the shelf: copies available, waiting lists and due dates are stated plainly.
3. One site for everyone: visitors browse freely; joining and borrowing are a clear next step, never a wall.
4. Works on a phone as well as on a desk computer.
