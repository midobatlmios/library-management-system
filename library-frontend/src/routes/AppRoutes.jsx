import { lazy } from 'react'
import { Route, Routes } from 'react-router-dom'
import AccountLayout from '../layouts/AccountLayout.jsx'
import AdminLayout from '../layouts/AdminLayout.jsx'
import MainLayout from '../layouts/MainLayout.jsx'
import CheckEmailPage from '../pages/auth/CheckEmailPage.jsx'
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage.jsx'
import LoginPage from '../pages/auth/LoginPage.jsx'
import ResetPasswordPage from '../pages/auth/ResetPasswordPage.jsx'
import SignupPage from '../pages/auth/SignupPage.jsx'
import BookDetailsPage from '../pages/books/BookDetailsPage.jsx'
import CatalogPage from '../pages/books/CatalogPage.jsx'
import HomePage from '../pages/HomePage.jsx'
import NotFoundPage from '../pages/NotFoundPage.jsx'
import { AdminRoute, GuestRoute, PrivateRoute } from './guards.jsx'

/*
 * Code splitting: the pages below are downloaded only when someone opens them.
 * A visitor browsing the catalog never downloads the admin area or the member
 * pages, so the first page loads faster. While a page downloads, the
 * <Suspense> in the layouts shows a skeleton.
 */
const PlansPage = lazy(() => import('../pages/subscriptions/PlansPage.jsx'))
const PaymentResultPage = lazy(() => import('../pages/subscriptions/PaymentResultPage.jsx'))

const LoansPage = lazy(() => import('../pages/account/LoansPage.jsx'))
const ReservationsPage = lazy(() => import('../pages/account/ReservationsPage.jsx'))
const FinesPage = lazy(() => import('../pages/account/FinesPage.jsx'))
const WishlistPage = lazy(() => import('../pages/account/WishlistPage.jsx'))
const MyReviewsPage = lazy(() => import('../pages/account/MyReviewsPage.jsx'))
const SubscriptionPage = lazy(() => import('../pages/account/SubscriptionPage.jsx'))
const PaymentsPage = lazy(() => import('../pages/account/PaymentsPage.jsx'))
const ProfilePage = lazy(() => import('../pages/account/ProfilePage.jsx'))

const AdminHomePage = lazy(() => import('../pages/admin/AdminHomePage.jsx'))
const AdminBooksPage = lazy(() => import('../pages/admin/AdminBooksPage.jsx'))
const AdminGenresPage = lazy(() => import('../pages/admin/AdminGenresPage.jsx'))
const AdminPlansPage = lazy(() => import('../pages/admin/AdminPlansPage.jsx'))
const AdminUsersPage = lazy(() => import('../pages/admin/AdminUsersPage.jsx'))
const AdminUserDetailPage = lazy(() => import('../pages/admin/AdminUserDetailPage.jsx'))
const AdminLoansPage = lazy(() => import('../pages/admin/AdminLoansPage.jsx'))
const AdminFinesPage = lazy(() => import('../pages/admin/AdminFinesPage.jsx'))
const AdminReservationsPage = lazy(() => import('../pages/admin/AdminReservationsPage.jsx'))
const AdminPaymentsPage = lazy(() => import('../pages/admin/AdminPaymentsPage.jsx'))
const AdminJobsPage = lazy(() => import('../pages/admin/AdminJobsPage.jsx'))

/** All routes in one place. Pages inside MainLayout get the navbar. */
export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        {/* Public */}
        <Route index element={<HomePage />} />
        <Route path="books" element={<CatalogPage />} />
        <Route path="books/:id" element={<BookDetailsPage />} />
        <Route path="plans" element={<PlansPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        {/* Linked from the reset email; works logged in or not. */}
        <Route path="reset-password" element={<ResetPasswordPage />} />

        {/* Only when logged out */}
        <Route element={<GuestRoute />}>
          <Route path="login" element={<LoginPage />} />
          <Route path="signup" element={<SignupPage />} />
        </Route>

        {/* Logged in */}
        <Route element={<PrivateRoute />}>
          <Route path="check-email" element={<CheckEmailPage />} />
          {/* The simulated checkout page sends the browser back here. */}
          <Route path="payment/result" element={<PaymentResultPage />} />
          {/* Member area: tabs on top (AccountLayout) */}
          <Route element={<AccountLayout />}>
            <Route path="loans" element={<LoansPage />} />
            <Route path="reservations" element={<ReservationsPage />} />
            <Route path="fines" element={<FinesPage />} />
            <Route path="wishlist" element={<WishlistPage />} />
            <Route path="my-reviews" element={<MyReviewsPage />} />
            <Route path="subscription" element={<SubscriptionPage />} />
            <Route path="payments" element={<PaymentsPage />} />
            <Route path="profile" element={<ProfilePage />} />
          </Route>
        </Route>

        {/* ROLE_ADMIN */}
        <Route element={<AdminRoute />}>
          <Route path="admin" element={<AdminLayout />}>
            <Route index element={<AdminHomePage />} />
            <Route path="books" element={<AdminBooksPage />} />
            <Route path="genres" element={<AdminGenresPage />} />
            <Route path="plans" element={<AdminPlansPage />} />
            <Route path="users" element={<AdminUsersPage />} />
            <Route path="users/:id" element={<AdminUserDetailPage />} />
            <Route path="loans" element={<AdminLoansPage />} />
            <Route path="fines" element={<AdminFinesPage />} />
            <Route path="reservations" element={<AdminReservationsPage />} />
            <Route path="payments" element={<AdminPaymentsPage />} />
            <Route path="jobs" element={<AdminJobsPage />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
