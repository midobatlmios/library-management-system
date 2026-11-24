# Frontend plan (React)

The backend (Spring Boot, `library-management-system/`) is finished: steps 1–13 plus 54 tests.
This file is the plan for the frontend, to be built **one step per message**, testing each step in the browser before the next.
Developer guide for the backend: see the "Library Backend Guide" page, or read the code.

## Working rules
- One step per message. After each step: run it, test it in the browser, then the user commits **themselves** (never commit for them).
- The backend must be running while working on the frontend: start MySQL in XAMPP, then `.\mvnw.cmd spring-boot:run` in `library-management-system/`.
- Never commit secrets: environment files with real values (`.env.local`) are git-ignored. The repo is public.
- If the frontend needs a backend change, make it small, run `.\mvnw.cmd test` afterwards, and say so.
- Explain what each step builds, so the user learns, not only gets code.

## Stack (from the project document)
React + Vite, MUI, Tailwind CSS, Redux Toolkit, React Router, Axios.
Frontend folder: `library-frontend/` at the repository root, next to `library-management-system/`. Dev server: `http://localhost:5173` (already allowed by the backend's CORS).

## What the frontend must know about the backend

| Topic | Details |
|---|---|
| Base URL | `http://localhost:9090` (put it in `VITE_API_URL`) |
| Auth | `POST /auth/login` or `/auth/signup` or `/auth/google` returns `{ jwt, message, title, user }`. Send `Authorization: Bearer <jwt>` on every `/api/**` call. Token lasts 24 h; 401 means log out and go to login. |
| Roles | `user.role` is `ROLE_USER` or `ROLE_ADMIN`. Admin pages only for `ROLE_ADMIN`. |
| Errors | Always `{ "message": "...", "success": false }`. Show `message` to the user. 400 bad input/rule, 401 not logged in, 403 not allowed or email not verified, 404, 409 conflict, 429 too many attempts. |
| Lists | Paged lists return `{ content, pageNumber, pageSize, totalElements, totalPages, first, last, empty }`. Query params `page` (from 0) and `size`. |
| Money | Stored in centimes (`19900`). Show the `...InMajorUnits` fields (`199.00`) with the `currency` (MAD). Plan `price` is already in MAD. |
| Dates | `YYYY-MM-DD` for dates, `YYYY-MM-DDTHH:mm:ss` for date-times. |
| Google login | Show Google's "Sign in with Google" button (Google Identity Services) with the client id in `VITE_GOOGLE_CLIENT_ID` (same value as `app.google.client-id` in `secrets.properties`). Send the returned `credential` to `POST /auth/google` as `{ "idToken": credential }`. `http://localhost:5173` is already an authorized origin. |
| Email links | Verification links open the backend directly (`/auth/verify-email`), nothing to build. Password-reset emails link to **`/reset-password?token=...`** on the frontend: that page must exist. Other emails link to `/loans` and `/reservations`. |
| Payments | Subscribing (`POST /api/subscriptions/subscribe`), renewing, or paying a fine (`POST /api/fines/{id}/pay`) returns `{ paymentId, checkoutUrl, amountInMajorUnits, currency, status }`. Send the browser to `checkoutUrl` (the backend's simulated payment page). Afterwards check `GET /api/payments/{paymentId}` for `SUCCESS`. Free plans return `status: "SUCCESS"` with no checkoutUrl. Test card: `4242 4242 4242 4242`. |
| Email verification | Users must verify their email before subscribing (403 otherwise). `POST /auth/resend-verification` sends a new link. Google accounts are verified automatically. |

## Steps

### Step 14 – Project setup
- Create `library-frontend` with Vite + React (JavaScript unless the user asks for TypeScript).
- Install MUI, Tailwind (with Tailwind's base reset turned off or adjusted so it doesn't fight MUI), Redux Toolkit, React Redux, React Router, Axios.
- Folder structure: `api/` (Axios instance + one file per module), `store/` (slices), `pages/`, `components/`, `layouts/`, `routes/`, `utils/`.
- Axios instance: base URL from `VITE_API_URL`, adds the Bearer token, on 401 logs out, turns errors into the backend's `message`.
- `.env.example` (committed) and `.env.local` (git-ignored) with `VITE_API_URL` and `VITE_GOOGLE_CLIENT_ID`.
- MUI theme (light/dark), app shell with navbar, a home page that calls `GET /api/books/stats` to prove the connection works.

### Step 15 – Authentication
- Auth slice in Redux: token + user, saved in localStorage, restored on reload, `GET /api/users/profile` to refresh the user.
- Pages: Login, Signup (show the password rules: 8+ chars, a letter and a digit), Forgot password, **Reset password** (`/reset-password?token=`), "Check your email" notice with a Resend button.
- Google sign-in button on Login and Signup.
- Route guards: `PrivateRoute` (logged in), `AdminRoute` (`ROLE_ADMIN`), redirect after login.
- Logout. Show 429 ("too many attempts") nicely.

### Step 16 – Catalog
- Book list: search (`?search=`), genre filter, "available only", sort, pagination (`GET /api/books`).
- Genre tree sidebar (`GET /api/genres/active/hierarchy`).
- Book details page: info, available copies, rating statistics and reviews (`/api/reviews/book/{id}` with filters), wishlist toggle.
- Action button that adapts: Borrow (copies available), Reserve (none available), or "Subscribe to borrow" (no subscription).

### Step 17 – Member area
- My loans (`/loans`): current and past, due date, late days, Renew, Return.
- My reservations (`/reservations`): queue position, "ready until" countdown, Pick up (fulfill), Cancel.
- My fines: list, total unpaid, Pay (goes to checkout).
- My wishlist: list, notes, remove.
- My reviews: write (only if `GET /api/reviews/can-review/{bookId}` allows), edit, delete; mark others' reviews helpful.
- Profile page: details, email verification status + resend.

### Step 18 – Subscriptions and payments
- Plans page (public): `GET /api/subscription-plans`, prices in MAD, features, trial days.
- Subscribe → redirect to `checkoutUrl` → result page that checks `GET /api/payments/{paymentId}` and shows success or failure.
- My subscription: active plan, days remaining, limits, Renew, Cancel, history.
- Payment history: `GET /api/payments/my`.
- Small backend change if wanted: let the checkout page return the browser to a frontend URL like `/payment/result?paymentId=...` instead of the home page.

### Step 19 – Admin dashboard
- Dashboard: statistics (books, loans, fines collected/outstanding, payments, plans, users, reviews).
- Books and genres: create, bulk create, edit, soft delete / restore, permanent delete (with confirmation).
- Plans: create, edit, deactivate.
- Users: list, view a user's loans, subscriptions, fines, wishlist.
- Loans: issue a book to a user, search, edit due date, run the overdue job.
- Fines: create (damage/lost/other), record a desk payment, waive.
- Reservations: search, place a hold for a user.
- Payments: search and statistics.
- Jobs: status table with "Run now" buttons (`/api/admin/jobs`).
- Email test button (`POST /api/admin/email/test?to=`).

### Step 20 – Polish
- Loading skeletons, empty states, toast messages for every action, confirmation dialogs for destructive actions.
- Form validation that matches the backend rules.
- Responsive layout (phone width), dark mode, 404 page.

### Step 21 – Tests and build (optional)
- A few component and flow tests (Vitest + React Testing Library) for login, borrowing and checkout.
- Production build (`npm run build`) and notes for deploying frontend and backend together (HTTPS, real domain in Google Cloud origins, turn off the backend's dev flags).
