import { createBrowserRouter } from 'react-router'
import { RequireAuth, RequireRole } from './auth/guards'
import { AppLayout } from './layout/AppLayout'
import { CampDetailsPage } from './pages/camps/CampDetailsPage'
import { EditCampPage, NewCampPage } from './pages/camps/CampFormPage'
import { CampsPage } from './pages/camps/CampsPage'
import { ChangePasswordPage, ForcedChangePasswordPage } from './pages/ChangePasswordPage'
import { DepartmentsPage } from './pages/DepartmentsPage'
import { NotFoundPage } from './pages/ErrorPages'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { UsersPage } from './pages/users/UsersPage'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
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
      { path: 'account/password', element: <ChangePasswordPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
