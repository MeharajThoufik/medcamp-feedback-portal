import {
  Alert,
  Button,
  Card,
  Group,
  Skeleton,
  Stack,
  Text,
  TextInput,
  Timeline,
  Title,
} from '@mantine/core'
import {
  IconCircleCheck,
  IconInfoCircle,
  IconSearch,
  IconTool,
  IconInbox,
  IconLock,
} from '@tabler/icons-react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { ApiError } from '../../api/client'
import { useComplaintStatus } from '../../api/publicCamps'
import { COMPLAINT_STATUSES, type PublicComplaintStatus } from '../../api/types'
import { PublicLayout } from './PublicLayout'

const STEP_ICONS = {
  OPEN: IconInbox,
  IN_PROGRESS: IconTool,
  RESOLVED: IconCircleCheck,
  CLOSED: IconLock,
}

/** "Check my complaint": anyone with a reference number sees its status, nothing more. */
export function TrackComplaintPage() {
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const reference = searchParams.get('ref')
  const [input, setInput] = useState(reference ?? '')
  const lookup = useComplaintStatus(reference && reference.trim() ? reference.trim() : null)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    const value = input.trim()
    if (value) setSearchParams({ ref: value }, { replace: true })
  }

  return (
    <PublicLayout>
      <Stack gap="lg">
        <Stack gap={4}>
          <Title order={2}>{t('track.title')}</Title>
          <Text c="dimmed">{t('track.subtitle')}</Text>
        </Stack>

        <form onSubmit={submit} noValidate>
          <Group align="flex-end" gap="sm" wrap="nowrap">
            <TextInput
              label={t('track.label')}
              placeholder="MC-26-K7Q2XP"
              size="md"
              autoCapitalize="characters"
              autoComplete="off"
              spellCheck={false}
              maxLength={20}
              value={input}
              onChange={(event) => setInput(event.currentTarget.value)}
              styles={{ input: { fontFamily: 'monospace', textTransform: 'uppercase' } }}
              style={{ flex: 1 }}
            />
            <Button
              type="submit"
              size="md"
              leftSection={<IconSearch size={18} />}
              loading={lookup.isFetching}
            >
              {t('track.check')}
            </Button>
          </Group>
        </form>

        {lookup.isPending && lookup.fetchStatus === 'fetching' && <Skeleton height={220} />}
        {lookup.isError && <LookupError error={lookup.error} />}
        {lookup.data && <StatusCard status={lookup.data} />}

        <Text size="sm" c="dimmed">
          {t('track.privacy')}
        </Text>
      </Stack>
    </PublicLayout>
  )
}

function LookupError({ error }: { error: Error }) {
  const { t } = useTranslation()
  if (error instanceof ApiError && error.code === 'COMPLAINT_NOT_FOUND')
    return (
      <Alert color="orange" icon={<IconInfoCircle size={20} />} title={t('track.notFoundTitle')}>
        {t('track.notFoundBody')}
      </Alert>
    )
  if (error instanceof ApiError && error.code === 'TOO_MANY_LOOKUPS')
    return <Alert color="orange">{t('track.tooMany')}</Alert>
  return <Alert color="red">{t('track.offline')}</Alert>
}

function StatusCard({ status }: { status: PublicComplaintStatus }) {
  const { t, i18n } = useTranslation()
  const format = new Intl.DateTimeFormat(i18n.language === 'hi' ? 'hi-IN' : 'en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
  const reached = COMPLAINT_STATUSES.indexOf(status.status)

  return (
    <Card withBorder radius="lg" p="lg">
      <Stack gap="md">
        <Group justify="space-between" gap="xs">
          <Text ff="monospace" fw={700} fz="lg">
            {status.referenceNo}
          </Text>
          <Text size="sm" c="dimmed">
            {t('track.updated', { when: format.format(new Date(status.lastUpdatedAt)) })}
          </Text>
        </Group>
        <Timeline active={reached} bulletSize={28} lineWidth={2} aria-label={t('track.timeline')}>
          {COMPLAINT_STATUSES.map((step, index) => {
            const StepIcon = STEP_ICONS[step]
            return (
              <Timeline.Item
                key={step}
                bullet={<StepIcon size={16} />}
                title={t(`track.status.${step}.title`)}
                aria-current={index === reached ? 'step' : undefined}
              >
                <Text size="sm" c={index <= reached ? undefined : 'dimmed'}>
                  {index === 0
                    ? t('track.received', { when: format.format(new Date(status.submittedAt)) })
                    : t(`track.status.${step}.body`)}
                </Text>
              </Timeline.Item>
            )
          })}
        </Timeline>
        {status.publicNote && (
          <Alert color="teal" title={t('track.noteTitle')}>
            <Text size="sm" style={{ whiteSpace: 'pre-wrap' }}>
              {status.publicNote}
            </Text>
          </Alert>
        )}
      </Stack>
    </Card>
  )
}
