import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useMe } from '../api/auth'
import type { Role } from '../api/types'
import { FullPageLoader, ServerUnreachable } from '../components/PageStates'
import { ForbiddenPage } from '../pages/ErrorPages'
import { useCurrentUser } from './useCurrentUser'

/** Shows children only to a signed-in user; sends anyone else to the sign-in page. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { data: me, isPending, isError, refetch } = useMe()
  const location = useLocation()

  if (isPending) return <FullPageLoader />
  if (isError) return <ServerUnreachable onRetry={() => void refetch()} />
  if (!me) return <Navigate to="/login" replace state={{ from: location.pathname }} />

  const onForcedPage = location.pathname === '/change-password'
  if (me.mustChangePassword && !onForcedPage) return <Navigate to="/change-password" replace />
  if (!me.mustChangePassword && onForcedPage) return <Navigate to="/account/password" replace />
  return children
}

/** UI-only check for nicer navigation; the server enforces the same rule on every API call. */
export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const me = useCurrentUser()
  return roles.includes(me.role) ? children : <ForbiddenPage />
}
