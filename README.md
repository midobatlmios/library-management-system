# Library Al Azhar — Library Management System

The website of a physical library. Visitors search the catalog and see live
availability of paper copies. Members borrow and reserve books, manage fines,
subscriptions, wishlists and reviews. Staff run the catalog, loans, plans,
reservations and payments from an admin area.

| Part | Folder | Stack | Runs on |
|---|---|---|---|
| Backend (REST API) | [`library-management-system/`](library-management-system) | Java 17, Spring Boot, Spring Security (JWT), JPA/Hibernate, MySQL | <http://localhost:9090> |
| Frontend | [`library-frontend/`](library-frontend) | React 19, Vite, MUI, Tailwind CSS, Redux Toolkit, React Router, Axios | <http://localhost:5173> |

## Features

- **Catalog**: books with genres and sub-genres, search by title, author or ISBN, live copy counts.
- **Accounts**: sign up, email verification, login with password or Google, password reset, brute-force protection.
- **Subscriptions**: plans (in MAD) that set how many books a member can borrow and for how long.
- **Loans and fines**: checkout and return, overdue fines per late day (capped), unpaid fines block borrowing.
- **Reservations**: waiting list per book; a returned copy is held for the next member for 48 hours.
- **Payments**: a simulated checkout page (no real money).
- **Reviews and wishlist**.
- **Admin area**: statistics, catalog and plan management, desk loans, fines, reservations, payments, manual job runs.
- **Background jobs**: overdue loans, expiring subscriptions/reservations/payments, due-date reminder emails, token cleanup.

## Requirements

- Windows 10 or later (the commands below use PowerShell)
- Java 17 or later (the Maven wrapper is included; no separate Maven install needed)
- Node.js 20 or later with npm
- MySQL 8 or MariaDB (XAMPP works)

## Setup

### 1. Database

Start MySQL (for example from the XAMPP control panel) and create the database:

```sql
CREATE DATABASE library_sys;
```

Hibernate creates and updates the tables on startup.

### 2. Backend secrets

Secrets are never committed. Copy the example file and fill in your values:

```powershell
Copy-Item library-management-system\secrets.example.properties library-management-system\secrets.properties
```

| Key | Required | What it is |
|---|---|---|
| `app.jwt.secret` | Yes | Random text, at least 32 characters, used to sign login tokens |
| `app.admin.password` | Yes | Password of the first admin account (`admin@library.local`) |
| `spring.datasource.password` | Yes | MySQL password (empty for the default XAMPP `root` user) |
| `spring.mail.username` / `spring.mail.password` | No | Gmail address and app password for sending emails |
| `app.google.client-id` | No | Google OAuth client ID for "Login with Google" |

Each value can also come from an environment variable instead (for example `JWT_SECRET`, `ADMIN_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`).

### 3. Frontend environment

```powershell
Copy-Item library-frontend\.env.example library-frontend\.env.local
```

| Key | What it is |
|---|---|
| `VITE_API_URL` | Backend URL, normally `http://localhost:9090` |
| `VITE_GOOGLE_CLIENT_ID` | Same value as `app.google.client-id` (optional) |

### 4. Install frontend packages

```powershell
cd library-frontend
npm install
```

## Run

Start the backend in one PowerShell window:

```powershell
cd library-management-system
.\mvnw.cmd spring-boot:run
```

The first run downloads dependencies and may take a few minutes. When it is up,
<http://localhost:9090/> shows a welcome message.

Start the frontend in a second window:

```powershell
cd library-frontend
npm run dev
```

Open <http://localhost:5173>. Keep the frontend on port 5173: the backend's
CORS settings allow exactly that origin.

Press `Ctrl+C` in either window to stop that server.

## Tests and build

Backend tests use a separate database, `library_sys_test`, which is created
automatically and rebuilt empty on each run. MySQL must be running.

```powershell
cd library-management-system
.\mvnw.cmd test
```

Build a backend JAR without running tests:

```powershell
.\mvnw.cmd -DskipTests package
```

Frontend lint and production build:

```powershell
cd library-frontend
npm run lint
npm run build
```

## API overview

All routes are served from <http://localhost:9090>.

| Path | Purpose |
|---|---|
| `/auth/**` | Sign up, login (password or Google), email verification, password reset |
| `/api/books`, `/api/genres` | Catalog (reading is public) |
| `/api/users` | Current user's profile |
| `/api/subscription-plans`, `/api/subscriptions` | Plans and member subscriptions |
| `/api/book-loans` | Loans: borrow, return, history |
| `/api/reservations` | Waiting lists and held copies |
| `/api/fines` | Overdue and other fines |
| `/api/payments`, `/pay/**` | Payments and the simulated checkout page |
| `/api/reviews`, `/api/wishlist` | Book reviews and wishlist |
| `/api/admin/**` | Admin tools, statistics and background jobs (admin only) |

Protected routes expect an `Authorization: Bearer <token>` header with the
token returned by login. A Postman workspace lives in
[`library-management-system/postman/`](library-management-system/postman).

## Project layout

```
library-management-system/        Spring Boot API
  src/main/java/com/library/
    controller/                   REST endpoints
    service/, service/impl/       Business logic
    repository/                   JPA repositories
    model/, domain/               Entities and enums
    payload/                      Request and response DTOs
    payment/                      Simulated payment gateway
    jobs/                         Scheduled background jobs
  src/main/resources/application.properties   Non-secret settings
  secrets.example.properties      Template for secrets.properties
library-frontend/                 React app (see its own README for src/ folders)
data/                             Sample catalog data (books, genres, plans) as JSON
```

## Development notes

- `app.dev.google-test-page=true` enables a Google login test page at
  `/dev/google-login`. Turn it off outside development.
- Emails are not sent to the reserved `example.com/.org/.net` domains, so test
  users with those addresses never cause bounces.
- Fine amounts, reservation hold time, payment expiry and job schedules are all
  set in `application.properties`.
