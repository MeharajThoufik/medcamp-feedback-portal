import {
  Alert,
  Button,
  Container,
  Group,
  Skeleton,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconCalendarEvent,
  IconClockHour4,
  IconLinkOff,
  IconMapPin,
  IconUserHeart,
  IconWifiOff,
} from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { useMe } from '../../api/auth'
import { ApiError } from '../../api/client'
import { usePublicForm } from '../../api/publicCamps'
import type { FeedbackState } from '../../api/types'
import { AppLogo } from '../../components/AppLogo'
import { LanguageSwitch } from '../../components/HeaderControls'
import { useCampDateFormat } from '../camps/campDates'
import { FeedbackFlow } from './FeedbackFlow'

/**
 * The page a camp's QR code opens. Attendees never sign in. With `?assisted=1`, signed-in staff
 * fill the form in for an attendee who cannot use a phone.
 */
export function PublicCampPage() {
  const { token = '' } = useParams()
  return (
    <Stack mih="100vh" gap={0} bg="var(--mantine-color-body)">
      <Group
        justify="space-between"
        px="md"
        py="sm"
        style={{ borderBottom: '1px solid var(--mantine-color-default-border)' }}
      >
        <AppLogo />
        <LanguageSwitch />
      </Group>
      <Container size={560} w="100%" px="md" py="lg">
        <PublicCampContent token={token} />
      </Container>
    </Stack>
  )
}

/** The signed-in staff member's name when assisted mode was asked for and is allowed. */
function useAssistingStaff(): { pending: boolean; name: string | null } {
  const [searchParams] = useSearchParams()
  const wanted = searchParams.get('assisted') === '1'
  // Attendees never trigger the "who am I" call; only the assisted link does.
  const me = useMe(wanted)
  if (!wanted) return { pending: false, name: null }
  if (me.isPending) return { pending: true, name: null }
  const allowed = me.data && me.data.role !== 'ANALYST' && !me.data.mustChangePassword
  return { pending: false, name: allowed ? me.data!.fullName : null }
}

function PublicCampContent({ token }: { token: string }) {
  const { t } = useTranslation()
  const formatDates = useCampDateFormat()
  const form = usePublicForm(token)
  const staff = useAssistingStaff()

  if (form.isPending || staff.pending)
    return (
      <Stack>
        <Skeleton height={32} width="70%" />
        <Skeleton height={18} width="50%" />
        <Skeleton height={120} mt="md" />
      </Stack>
    )

  if (form.isError) {
    const invalidLink = form.error instanceof ApiError && form.error.status === 404
    return (
      <Stack align="center" ta="center" gap="sm" py="xl">
        <ThemeIcon size={64} radius="xl" variant="light" color={invalidLink ? 'orange' : 'gray'}>
          {invalidLink ? <IconLinkOff size={34} /> : <IconWifiOff size={34} />}
        </ThemeIcon>
        <Title order={3}>{invalidLink ? t('public.invalidTitle') : t('public.offlineTitle')}</Title>
        <Text c="dimmed">{invalidLink ? t('public.invalidBody') : t('public.offlineBody')}</Text>
        {!invalidLink && <Button onClick={() => void form.refetch()}>{t('common.retry')}</Button>}
      </Stack>
    )
  }

  const data = form.data
  return (
    <Stack gap="lg">
      <Stack gap={6}>
        <Text size="sm" c="dimmed" fw={500}>
          {t('public.welcome')}
        </Text>
        <Title order={2}>{data.name}</Title>
        <Group gap="lg">
          <Group gap={6} wrap="nowrap">
            <IconMapPin size={16} />
            <Text size="sm">{data.location}</Text>
          </Group>
          <Group gap={6} wrap="nowrap">
            <IconCalendarEvent size={16} />
            <Text size="sm">{formatDates.range(data.startDate, data.endDate)}</Text>
          </Group>
        </Group>
      </Stack>
      {staff.name && (
        <Alert color="grape" icon={<IconUserHeart size={20} />}>
          {t('feedbackForm.assisted.banner', { name: staff.name })}
        </Alert>
      )}
      {data.feedbackState === 'OPEN' ? (
        <FeedbackFlow token={token} form={data} assistedBy={staff.name} />
      ) : (
        <FormClosed state={data.feedbackState} />
      )}
    </Stack>
  )
}

function FormClosed({ state }: { state: FeedbackState }) {
  const { t } = useTranslation()
  const key = state === 'NOT_STARTED' ? 'notStarted' : state === 'ENDED' ? 'ended' : 'notActive'
  return (
    <Alert
      color="gray"
      radius="lg"
      icon={<IconClockHour4 size={20} />}
      title={t(`public.${key}Title`)}
    >
      {t(`public.${key}Body`)}
    </Alert>
  )
}
