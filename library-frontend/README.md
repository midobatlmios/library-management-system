# Library frontend

React + Vite, MUI, Tailwind CSS, Redux Toolkit, React Router, Axios.

## Run

1. Start MySQL in XAMPP and the backend (`.\mvnw.cmd spring-boot:run` in `library-management-system/`).
2. Copy `.env.example` to `.env.local` and fill in the values (`.env.local` is git-ignored).
3. `npm install`, then `npm run dev` and open http://localhost:5173.

## Folders (`src/`)

| Folder | What goes there |
|---|---|
| `api/` | `client.js` (the Axios instance) and one file per backend module |
| `store/` | Redux store and slices |
| `pages/` | One component per route |
| `components/` | Reusable pieces (navbar, buttons, cards) |
| `layouts/` | Page shells (navbar + `<Outlet />`) |
| `routes/` | All routes and route guards |
| `utils/` | Helpers (localStorage, error messages) |
