import { useMe } from '../api/auth'
import type { Me } from '../api/types'

/** The signed-in user. Only use inside `RequireAuth`. */
export function useCurrentUser(): Me {
  const { data } = useMe()
  if (!data) throw new Error('useCurrentUser must be used inside RequireAuth')
  return data
}
