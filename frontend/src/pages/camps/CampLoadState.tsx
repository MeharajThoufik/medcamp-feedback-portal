import { Skeleton, Stack } from '@mantine/core'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../../api/client'
import { LoadError } from '../../components/PageStates'
import { errorMessage } from '../../i18n/errors'
import { ForbiddenPage, NotFoundPage } from '../ErrorPages'

export function CampPageSkeleton() {
  return (
    <Stack maw={960}>
      <Skeleton height={36} width="50%" />
      <Skeleton height={18} width="30%" />
      <Skeleton height={220} mt="md" />
    </Stack>
  )
}

/** A missing camp shows "not found", another organizer's camp shows "no access". */
export function CampLoadError({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const { t } = useTranslation()
  if (error instanceof ApiError && error.status === 404) return <NotFoundPage />
  if (error instanceof ApiError && error.status === 403) return <ForbiddenPage />
  return <LoadError message={errorMessage(t, error)} onRetry={onRetry} />
}
