import {
  Alert,
  Anchor,
  Badge,
  Button,
  Card,
  Group,
  Pagination,
  Paper,
  Progress,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { IconMessageCircle, IconUserHeart } from '@tabler/icons-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { useCampFeedback, useFeedbackSummary } from '../../api/feedback'
import type { Camp, FeedbackItem, FeedbackSummary } from '../../api/types'
import { useCurrentUser } from '../../auth/useCurrentUser'
import { SCORE_EMOJI } from '../../components/ratingScale'
import { LoadError } from '../../components/PageStates'
import { errorMessage } from '../../i18n/errors'

type Labels = Record<string, string>

export function CampFeedbackTab({ camp }: { camp: Camp }) {
  const { t, i18n } = useTranslation()
  const me = useCurrentUser()
  const isAnalyst = me.role === 'ANALYST'
  const [page, setPage] = useState(0)
  const summary = useFeedbackSummary(camp.id, !isAnalyst)
  const list = useCampFeedback(camp.id, page, !isAnalyst)

  if (isAnalyst) return <Alert color="gray">{t('camps.feedback.analystNote')}</Alert>

  const labels: Labels = Object.fromEntries(
    (summary.data?.categories ?? []).map((c) => [
      c.code,
      i18n.language === 'hi' ? c.labelHi : c.labelEn,
    ]),
  )
  const error = summary.error ?? list.error
  const summaryData = summary.data
  const listData = list.data

  return (
    <Stack>
      {camp.feedbackState === 'OPEN' && (
        <Paper withBorder radius="md" p="md">
          <Group justify="space-between" gap="sm">
            <Text size="sm" style={{ flex: '1 1 280px' }}>
              {t('camps.feedback.fillInHint')}
            </Text>
            <Button
              component={Link}
              to={`/f/${camp.publicToken}?assisted=1`}
              variant="light"
              leftSection={<IconUserHeart size={16} />}
            >
              {t('camps.feedback.fillIn')}
            </Button>
          </Group>
        </Paper>
      )}
      {me.role === 'MEDICAL_STAFF' && (
        <Text size="sm" c="dimmed">
          {t('camps.feedback.departmentOnly')}
        </Text>
      )}

      {error ? (
        <LoadError
          message={errorMessage(t, error)}
          onRetry={() => {
            void summary.refetch()
            void list.refetch()
          }}
        />
      ) : !summaryData || !listData ? (
        <Stack>
          <Skeleton height={140} />
          <Skeleton height={120} />
        </Stack>
      ) : summaryData.total === 0 ? (
        <Paper withBorder radius="md" p="xl">
          <Stack align="center" gap="sm">
            <IconMessageCircle size={40} stroke={1.3} color="var(--mantine-color-dimmed)" />
            <Text c="dimmed" ta="center" maw={440}>
              {t('camps.feedback.empty')}
            </Text>
          </Stack>
        </Paper>
      ) : (
        <>
          <SummaryCard summary={summaryData} labels={labels} />
          {listData.items.map((item) => (
            <FeedbackCard key={item.id} item={item} labels={labels} />
          ))}
          {listData.totalPages > 1 && (
            <Group justify="center">
              <Pagination
                total={listData.totalPages}
                value={page + 1}
                onChange={(value) => setPage(value - 1)}
              />
            </Group>
          )}
        </>
      )}
    </Stack>
  )
}

function SummaryCard({ summary, labels }: { summary: FeedbackSummary; labels: Labels }) {
  const { t } = useTranslation()
  return (
    <Paper withBorder radius="md" p="lg">
      <Stack gap="md">
        <Title order={4}>{t('camps.feedback.responses', { count: summary.total })}</Title>
        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" verticalSpacing="sm">
          {summary.categories.map((category) => (
            <Stack key={category.code} gap={4}>
              <Group justify="space-between" gap="xs" wrap="nowrap">
                <Text size="sm" lineClamp={1}>
                  {labels[category.code]}
                </Text>
                <Text size="sm" fw={600} style={{ whiteSpace: 'nowrap' }}>
                  {category.average === null
                    ? '—'
                    : t('camps.feedback.average', { average: category.average.toFixed(1) })}
                </Text>
              </Group>
              <Progress
                value={category.average === null ? 0 : (category.average / 5) * 100}
                aria-label={labels[category.code]}
              />
              <Text size="xs" c="dimmed">
                {t('camps.feedback.answers', { count: category.count })}
              </Text>
            </Stack>
          ))}
        </SimpleGrid>
      </Stack>
    </Paper>
  )
}

function FeedbackCard({ item, labels }: { item: FeedbackItem; labels: Labels }) {
  const { t, i18n } = useTranslation()
  const when = new Intl.DateTimeFormat(i18n.language === 'hi' ? 'hi-IN' : 'en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(item.submittedAt))
  const overall = item.ratings.OVERALL
  const others = Object.entries(item.ratings).filter(([code]) => code !== 'OVERALL')

  return (
    <Card withBorder radius="md" p="md">
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" gap="sm">
          <Stack gap={2}>
            <Text fw={600}>
              {item.serviceName ?? t('camps.feedback.generalVisit')}
              {item.departmentName && (
                <Text span c="dimmed" fw={400}>
                  {' '}
                  · {item.departmentName}
                </Text>
              )}
            </Text>
            <Text size="xs" c="dimmed">
              {when}
            </Text>
          </Stack>
          {overall !== undefined && (
            <Group gap={6} wrap="nowrap" aria-label={`${labels.OVERALL}: ${overall} / 5`}>
              <Text fz={26} lh={1} aria-hidden>
                {SCORE_EMOJI[overall]}
              </Text>
              <Text fw={700}>{overall}/5</Text>
            </Group>
          )}
        </Group>

        {others.length > 0 && (
          <Group gap={6}>
            {others.map(([code, score]) => (
              <Badge
                key={code}
                variant="light"
                color={score <= 2 ? 'red' : score === 3 ? 'yellow' : 'teal'}
                tt="none"
              >
                {labels[code] ?? code}: {score}/5
              </Badge>
            ))}
          </Group>
        )}

        {item.comment && <Text style={{ whiteSpace: 'pre-wrap' }}>{item.comment}</Text>}

        <Group gap={6}>
          {item.problemReported && (
            <Badge color="red" variant="filled">
              {t('camps.feedback.problem')}
            </Badge>
          )}
          <Badge variant="outline" color="gray">
            {item.anonymous ? t('camps.feedback.anonymous') : t('camps.feedback.wantsReply')}
          </Badge>
          {item.assisted && (
            <Badge variant="light" color="grape">
              {t('camps.feedback.assistedBy', { name: item.assistedByName })}
            </Badge>
          )}
          <Badge variant="outline" color="gray">
            {item.language === 'hi' ? 'हिन्दी' : 'English'}
          </Badge>
        </Group>

        {item.contact && (
          <Paper bg="var(--mantine-color-default-hover)" p="sm" radius="sm">
            <Text size="xs" c="dimmed" fw={600} tt="uppercase">
              {t('camps.feedback.contactTitle')}
            </Text>
            {item.contact.name && <Text size="sm">{item.contact.name}</Text>}
            {item.contact.phone && (
              <Anchor size="sm" href={`tel:${item.contact.phone}`}>
                {item.contact.phone}
              </Anchor>
            )}
            {item.contact.email && (
              <Anchor size="sm" href={`mailto:${item.contact.email}`} display="block">
                {item.contact.email}
              </Anchor>
            )}
          </Paper>
        )}
      </Stack>
    </Card>
  )
}
