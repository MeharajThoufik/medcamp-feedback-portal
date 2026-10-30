import {
  Alert,
  Anchor,
  Button,
  Card,
  Group,
  List,
  Paper,
  SimpleGrid,
  Stack,
  Tabs,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core'
import { modals } from '@mantine/modals'
import { notifications } from '@mantine/notifications'
import {
  IconAlertTriangle,
  IconArrowLeft,
  IconCalendarEvent,
  IconClipboardList,
  IconMapPin,
  IconMessageCircle,
  IconPencil,
  IconQrcode,
  IconStethoscope,
  IconUser,
  type Icon,
} from '@tabler/icons-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams, useSearchParams } from 'react-router'
import { useCamp, useChangeCampStatus } from '../../api/camps'
import type { Camp, CampServiceItem } from '../../api/types'
import { errorMessage } from '../../i18n/errors'
import { CampStatusBadge, FeedbackStateBadge } from './CampBadges'
import { CampFeedbackTab } from './CampFeedbackTab'
import { CampLoadError, CampPageSkeleton } from './CampLoadState'
import { CampQrTab } from './CampQrTab'
import { useCampDateFormat } from './campDates'
import { statusActions, type StatusAction } from './statusActions'

const TABS = ['overview', 'services', 'qr', 'feedback', 'complaints'] as const
type Tab = (typeof TABS)[number]

export function CampDetailsPage() {
  const id = Number(useParams().id)
  const camp = useCamp(id)

  if (camp.isPending) return <CampPageSkeleton />
  if (camp.isError) return <CampLoadError error={camp.error} onRetry={() => void camp.refetch()} />
  return <CampDetails camp={camp.data} />
}

function CampDetails({ camp }: { camp: Camp }) {
  const { t } = useTranslation()
  const formatDates = useCampDateFormat()
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('tab') as Tab | null
  const tab: Tab = requested && TABS.includes(requested) ? requested : 'overview'

  return (
    <Stack maw={960} gap="lg">
      <Stack gap={4}>
        <Anchor component={Link} to="/camps" size="sm" c="dimmed">
          <Group gap={4}>
            <IconArrowLeft size={14} />
            {t('camps.backToList')}
          </Group>
        </Anchor>
        <Group justify="space-between" align="flex-start" gap="sm">
          <Stack gap={4}>
            <Group gap="sm">
              <Title order={2}>{camp.name}</Title>
              <CampStatusBadge status={camp.status} />
              <FeedbackStateBadge state={camp.feedbackState} />
            </Group>
            <Text c="dimmed">
              {camp.location} · {formatDates.range(camp.startDate, camp.endDate)}
            </Text>
          </Stack>
          <CampActions camp={camp} />
        </Group>
      </Stack>

      <Tabs
        value={tab}
        onChange={(value) =>
          setSearchParams(value && value !== 'overview' ? { tab: value } : {}, { replace: true })
        }
        keepMounted={false}
      >
        <Tabs.List mb="md">
          <Tabs.Tab value="overview" leftSection={<IconClipboardList size={16} />}>
            {t('camps.tabs.overview')}
          </Tabs.Tab>
          <Tabs.Tab value="services" leftSection={<IconStethoscope size={16} />}>
            {t('camps.tabs.services', { count: camp.services.length })}
          </Tabs.Tab>
          <Tabs.Tab value="qr" leftSection={<IconQrcode size={16} />}>
            {t('camps.tabs.qr')}
          </Tabs.Tab>
          <Tabs.Tab value="feedback" leftSection={<IconMessageCircle size={16} />}>
            {t('camps.tabs.feedback')}
          </Tabs.Tab>
          <Tabs.Tab value="complaints" leftSection={<IconAlertTriangle size={16} />}>
            {t('camps.tabs.complaints')}
          </Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="overview">
          <OverviewTab camp={camp} />
        </Tabs.Panel>
        <Tabs.Panel value="services">
          <ServicesTab camp={camp} />
        </Tabs.Panel>
        <Tabs.Panel value="qr">
          <CampQrTab camp={camp} />
        </Tabs.Panel>
        <Tabs.Panel value="feedback">
          <CampFeedbackTab camp={camp} />
        </Tabs.Panel>
        <Tabs.Panel value="complaints">
          <ComingSoon icon={IconAlertTriangle} text={t('camps.comingSoon.complaints')} />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}

function CampActions({ camp }: { camp: Camp }) {
  const { t } = useTranslation()
  const changeStatus = useChangeCampStatus()
  if (!camp.canEdit) return null

  const run = (action: StatusAction) =>
    changeStatus.mutate(
      { id: camp.id, status: action.target },
      {
        onSuccess: () =>
          notifications.show({ color: 'teal', message: t(`camps.actions.${action.key}Done`) }),
        onError: (error) =>
          notifications.show({ color: 'red', message: errorMessage(t, error, 'camps') }),
      },
    )

  const onClick = (action: StatusAction) => {
    if (!action.confirm) {
      run(action)
      return
    }
    modals.openConfirmModal({
      title: t(`camps.actions.${action.key}Confirm.title`, { name: camp.name }),
      children: <Text size="sm">{t(`camps.actions.${action.key}Confirm.body`)}</Text>,
      labels: { confirm: t(`camps.actions.${action.key}`), cancel: t('common.cancel') },
      confirmProps: { color: action.key === 'archive' ? 'red' : undefined },
      onConfirm: () => run(action),
    })
  }

  return (
    <Group gap="xs">
      {camp.status !== 'ARCHIVED' && (
        <Button
          component={Link}
          to={`/camps/${camp.id}/edit`}
          variant="default"
          leftSection={<IconPencil size={16} />}
        >
          {t('camps.edit')}
        </Button>
      )}
      {statusActions(camp.status, camp.nextStatuses).map((action) => (
        <Button
          key={action.target}
          variant={action.primary ? 'filled' : 'light'}
          color={action.key === 'archive' ? 'gray' : undefined}
          leftSection={<action.icon size={16} />}
          loading={changeStatus.isPending && changeStatus.variables?.status === action.target}
          disabled={changeStatus.isPending}
          onClick={() => onClick(action)}
        >
          {t(`camps.actions.${action.key}`)}
        </Button>
      ))}
    </Group>
  )
}

/** Explains in plain words whether attendees can give feedback right now. */
function FeedbackNotice({ camp }: { camp: Camp }) {
  const { t } = useTranslation()
  const formatDates = useCampDateFormat()
  const values = {
    opens: formatDates.date(camp.feedbackOpensOn),
    closes: formatDates.date(camp.feedbackClosesOn),
  }
  switch (camp.feedbackState) {
    case 'OPEN':
      return (
        <Alert color="teal" title={t('camps.notice.openTitle')}>
          {t('camps.notice.open', values)}
        </Alert>
      )
    case 'NOT_STARTED':
      return (
        <Alert color="orange" title={t('camps.notice.notStartedTitle')}>
          {t('camps.notice.notStarted', values)}
        </Alert>
      )
    case 'ENDED':
      return (
        <Alert color="orange" title={t('camps.notice.endedTitle')}>
          {t('camps.notice.ended', values)}
        </Alert>
      )
    case 'CAMP_NOT_ACTIVE':
      return (
        <Alert color="gray" title={t('camps.notice.closedTitle')}>
          {camp.status === 'UPCOMING'
            ? t('camps.notice.upcoming', values)
            : t('camps.notice.closed', { status: t(`camps.status.${camp.status}`) })}
          {camp.status === 'UPCOMING' && camp.services.length === 0 && (
            <Text size="sm" mt={4} c="orange">
              {t('camps.notice.needsServices')}
            </Text>
          )}
        </Alert>
      )
  }
}

function OverviewTab({ camp }: { camp: Camp }) {
  const { t } = useTranslation()
  const formatDates = useCampDateFormat()
  return (
    <Stack>
      <FeedbackNotice camp={camp} />
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <InfoCard icon={IconCalendarEvent} label={t('camps.overview.dates')}>
          {formatDates.range(camp.startDate, camp.endDate)}
        </InfoCard>
        <InfoCard icon={IconMapPin} label={t('camps.overview.location')}>
          {camp.location}
        </InfoCard>
        <InfoCard icon={IconUser} label={t('camps.overview.organizer')}>
          {camp.organizerName}
        </InfoCard>
        <InfoCard icon={IconMessageCircle} label={t('camps.overview.feedbackWindow')}>
          {formatDates.range(camp.feedbackOpensOn, camp.feedbackClosesOn)}
        </InfoCard>
      </SimpleGrid>
    </Stack>
  )
}

function InfoCard({
  icon: CardIcon,
  label,
  children,
}: {
  icon: Icon
  label: string
  children: ReactNode
}) {
  return (
    <Card withBorder radius="md" p="md">
      <Group wrap="nowrap" align="flex-start">
        <ThemeIcon variant="light" size="lg" radius="md">
          <CardIcon size={20} />
        </ThemeIcon>
        <Stack gap={2}>
          <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
            {label}
          </Text>
          <Text fw={500}>{children}</Text>
        </Stack>
      </Group>
    </Card>
  )
}

function ServicesTab({ camp }: { camp: Camp }) {
  const { t } = useTranslation()
  if (camp.services.length === 0)
    return (
      <Paper withBorder radius="md" p="xl">
        <Stack align="center" gap="sm">
          <IconStethoscope size={40} stroke={1.3} color="var(--mantine-color-dimmed)" />
          <Text c="dimmed" ta="center">
            {t('camps.services.empty')}
          </Text>
          {camp.canEdit && camp.status !== 'ARCHIVED' && (
            <Button component={Link} to={`/camps/${camp.id}/edit`} variant="light">
              {t('camps.services.add')}
            </Button>
          )}
        </Stack>
      </Paper>
    )

  // Group by department, keeping the organizer's order.
  const groups = new Map<string, CampServiceItem[]>()
  for (const service of camp.services) {
    groups.set(service.departmentName, [...(groups.get(service.departmentName) ?? []), service])
  }
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }}>
      {[...groups].map(([department, items]) => (
        <Card key={department} withBorder radius="md" p="md">
          <Text fw={600} mb="xs">
            {department}
          </Text>
          <List size="sm" spacing={4}>
            {items.map((service) => (
              <List.Item key={service.id}>{service.name}</List.Item>
            ))}
          </List>
        </Card>
      ))}
    </SimpleGrid>
  )
}

function ComingSoon({ icon: StateIcon, text }: { icon: Icon; text: string }) {
  return (
    <Paper withBorder radius="md" p="xl">
      <Stack align="center" gap="sm">
        <StateIcon size={40} stroke={1.3} color="var(--mantine-color-dimmed)" />
        <Text c="dimmed" ta="center" maw={440}>
          {text}
        </Text>
      </Stack>
    </Paper>
  )
}
