import {
  Alert,
  Button,
  Card,
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
  IconMessageCircle,
  IconWifiOff,
} from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router'
import { ApiError } from '../../api/client'
import { usePublicCamp } from '../../api/publicCamps'
import type { FeedbackState } from '../../api/types'
import { AppLogo } from '../../components/AppLogo'
import { LanguageSwitch } from '../../components/HeaderControls'
import { useCampDateFormat } from '../camps/campDates'

/**
 * The page a camp's QR code opens. Attendees never sign in. Phase 4 puts the feedback form here;
 * for now it confirms the link works and says whether the camp is taking feedback.
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
      <Container size={520} w="100%" px="md" py="xl">
        <PublicCampContent token={token} />
      </Container>
    </Stack>
  )
}

function PublicCampContent({ token }: { token: string }) {
  const { t } = useTranslation()
  const formatDates = useCampDateFormat()
  const camp = usePublicCamp(token)

  if (camp.isPending)
    return (
      <Stack>
        <Skeleton height={32} width="70%" />
        <Skeleton height={18} width="50%" />
        <Skeleton height={120} mt="md" />
      </Stack>
    )

  if (camp.isError) {
    const invalidLink = camp.error instanceof ApiError && camp.error.status === 404
    return (
      <Stack align="center" ta="center" gap="sm" py="xl">
        <ThemeIcon size={64} radius="xl" variant="light" color={invalidLink ? 'orange' : 'gray'}>
          {invalidLink ? <IconLinkOff size={34} /> : <IconWifiOff size={34} />}
        </ThemeIcon>
        <Title order={3}>{invalidLink ? t('public.invalidTitle') : t('public.offlineTitle')}</Title>
        <Text c="dimmed">{invalidLink ? t('public.invalidBody') : t('public.offlineBody')}</Text>
        {!invalidLink && <Button onClick={() => void camp.refetch()}>{t('common.retry')}</Button>}
      </Stack>
    )
  }

  const data = camp.data
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
      <FeedbackStatus state={data.feedbackState} />
    </Stack>
  )
}

function FeedbackStatus({ state }: { state: FeedbackState }) {
  const { t } = useTranslation()
  if (state === 'OPEN')
    return (
      <Card withBorder radius="lg" p="lg">
        <Group wrap="nowrap" align="flex-start">
          <ThemeIcon size={44} radius="xl" variant="light">
            <IconMessageCircle size={24} />
          </ThemeIcon>
          <Stack gap={4}>
            <Text fw={600}>{t('public.openTitle')}</Text>
            <Text size="sm" c="dimmed">
              {t('public.openBody')}
            </Text>
          </Stack>
        </Group>
      </Card>
    )
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
