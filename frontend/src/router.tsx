import { createBrowserRouter } from 'react-router'
import { RequireAuth, RequireRole } from './auth/guards'
import { AppLayout } from './layout/AppLayout'
import { CampDetailsPage } from './pages/camps/CampDetailsPage'
import { EditCampPage, NewCampPage } from './pages/camps/CampFormPage'
import { CampPosterPage } from './pages/camps/CampPosterPage'
import { CampsPage } from './pages/camps/CampsPage'
import { ChangePasswordPage, ForcedChangePasswordPage } from './pages/ChangePasswordPage'
import { DepartmentsPage } from './pages/DepartmentsPage'
import { NotFoundPage } from './pages/ErrorPages'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { PublicCampPage } from './pages/public/PublicCampPage'
import { TrackComplaintPage } from './pages/public/TrackComplaintPage'
import { SettingsPage } from './pages/SettingsPage'
import { UsersPage } from './pages/users/UsersPage'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  // Opened from a camp's QR code by attendees, who never sign in.
  { path: '/f/:token', element: <PublicCampPage /> },
  // Attendees check a reported problem with its reference number.
  { path: '/track', element: <TrackComplaintPage /> },
  {
    // Outside the app layout, so only the poster prints.
    path: '/camps/:id/poster',
    element: (
      <RequireAuth>
        <RequireRole roles={['SUPER_ADMIN', 'ORGANIZER']}>
          <CampPosterPage />
        </RequireRole>
      </RequireAuth>
    ),
  },
  {
    path: '/change-password',
    element: (
      <RequireAuth>
        <ForcedChangePasswordPage />
      </RequireAuth>
    ),
  },
  {
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <HomePage /> },
      { path: 'camps', element: <CampsPage /> },
      {
        path: 'camps/new',
        element: (
          <RequireRole roles={['SUPER_ADMIN', 'ORGANIZER']}>
            <NewCampPage />
          </RequireRole>
        ),
      },
      { path: 'camps/:id', element: <CampDetailsPage /> },
      {
        path: 'camps/:id/edit',
        element: (
          <RequireRole roles={['SUPER_ADMIN', 'ORGANIZER']}>
            <EditCampPage />
          </RequireRole>
        ),
      },
      {
        path: 'users',
        element: (
          <RequireRole roles={['SUPER_ADMIN']}>
            <UsersPage />
          </RequireRole>
        ),
      },
      {
        path: 'departments',
        element: (
          <RequireRole roles={['SUPER_ADMIN']}>
            <DepartmentsPage />
          </RequireRole>
        ),
      },
      {
        path: 'settings',
        element: (
          <RequireRole roles={['SUPER_ADMIN']}>
            <SettingsPage />
          </RequireRole>
        ),
      },
      { path: 'account/password', element: <ChangePasswordPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
