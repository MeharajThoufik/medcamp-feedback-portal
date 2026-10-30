import {
  Anchor,
  Button,
  Group,
  Pagination,
  Paper,
  Select,
  Skeleton,
  Stack,
  Table,
  Text,
  TextInput,
} from '@mantine/core'
import { DatePickerInput } from '@mantine/dates'
import { useDebouncedValue } from '@mantine/hooks'
import { IconCalendar, IconPlus, IconSearch, IconTent } from '@tabler/icons-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { useCamps, useOrganizers } from '../../api/camps'
import { CAMP_STATUSES, type CampStatus } from '../../api/types'
import { useCurrentUser } from '../../auth/useCurrentUser'
import { PageHeader } from '../../components/PageHeader'
import { LoadError } from '../../components/PageStates'
import { errorMessage } from '../../i18n/errors'
import { CampStatusBadge, FeedbackStateBadge } from './CampBadges'
import { useCampDateFormat } from './campDates'

type DateRange = [string | null, string | null]

export function CampsPage() {
  const { t } = useTranslation()
  const me = useCurrentUser()
  const navigate = useNavigate()
  const formatDates = useCampDateFormat()
  const isAdmin = me.role === 'SUPER_ADMIN'
  const canCreate = isAdmin || me.role === 'ORGANIZER'
  // Organizers only ever see their own camps, so the column would repeat their name.
  const showOrganizer = me.role !== 'ORGANIZER'

  const [search, setSearch] = useState('')
  const [debouncedSearch] = useDebouncedValue(search, 300)
  const [status, setStatus] = useState<CampStatus | null>(null)
  const [dates, setDates] = useState<DateRange>([null, null])
  const [organizerId, setOrganizerId] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const organizers = useOrganizers(isAdmin)
  const camps = useCamps({
    search: debouncedSearch,
    status,
    // Wait for both ends while a range is being picked.
    from: dates[0] && dates[1] ? dates[0] : null,
    to: dates[0] && dates[1] ? dates[1] : null,
    organizerId: organizerId ? Number(organizerId) : null,
    page,
  })

  const changeFilter =
    <T,>(setter: (value: T) => void) =>
    (value: T) => {
      setter(value)
      setPage(0)
    }

  const hasFilters = Boolean(search.trim() || status || dates[0] || organizerId)
  const columns = showOrganizer ? 5 : 4
  const data = camps.data

  const rows = data?.items.map((camp) => (
    <Table.Tr
      key={camp.id}
      onClick={() => navigate(`/camps/${camp.id}`)}
      style={{ cursor: 'pointer' }}
      opacity={camp.status === 'ARCHIVED' ? 0.7 : 1}
    >
      <Table.Td>
        <Anchor
          component={Link}
          to={`/camps/${camp.id}`}
          fw={500}
          size="sm"
          onClick={(event) => event.stopPropagation()}
        >
          {camp.name}
        </Anchor>
        <Text size="xs" c="dimmed">
          {camp.location}
        </Text>
      </Table.Td>
      <Table.Td>
        <Text size="sm">{formatDates.range(camp.startDate, camp.endDate)}</Text>
      </Table.Td>
      <Table.Td>
        <Group gap={4}>
          <CampStatusBadge status={camp.status} />
          <FeedbackStateBadge state={camp.feedbackState} />
        </Group>
      </Table.Td>
      {showOrganizer && (
        <Table.Td>
          <Text size="sm">{camp.organizerName}</Text>
        </Table.Td>
      )}
      <Table.Td>
        <Text size="sm" c={camp.serviceCount === 0 ? 'orange' : undefined}>
          {camp.serviceCount}
        </Text>
      </Table.Td>
    </Table.Tr>
  ))

  return (
    <>
      <PageHeader
        title={t('camps.title')}
        subtitle={t(`camps.subtitle.${me.role}`)}
        action={
          canCreate && (
            <Button component={Link} to="/camps/new" leftSection={<IconPlus size={18} />}>
              {t('camps.add')}
            </Button>
          )
        }
      />

      <Paper withBorder radius="md" p="md">
        <Stack>
          <Group gap="sm" align="flex-end">
            <TextInput
              placeholder={t('camps.searchPlaceholder')}
              aria-label={t('camps.searchPlaceholder')}
              leftSection={<IconSearch size={16} />}
              value={search}
              onChange={(event) => changeFilter(setSearch)(event.currentTarget.value)}
              style={{ flex: '1 1 220px' }}
            />
            <Select
              aria-label={t('camps.statusFilter')}
              placeholder={t('camps.notArchived')}
              clearable
              data={CAMP_STATUSES.map((s) => ({ value: s, label: t(`camps.status.${s}`) }))}
              value={status}
              onChange={(value) => changeFilter(setStatus)(value as CampStatus | null)}
              w={170}
            />
            <DatePickerInput
              type="range"
              aria-label={t('camps.dateFilter')}
              placeholder={t('camps.dateFilter')}
              leftSection={<IconCalendar size={16} />}
              clearable
              allowSingleDateInRange
              valueFormat="D MMM YYYY"
              value={dates}
              onChange={(value) => changeFilter(setDates)(value as DateRange)}
              w={250}
            />
            {isAdmin && (
              <Select
                aria-label={t('camps.organizerFilter')}
                placeholder={t('camps.allOrganizers')}
                clearable
                searchable
                data={(organizers.data ?? []).map((o) => ({
                  value: String(o.id),
                  label: o.fullName,
                }))}
                value={organizerId}
                onChange={changeFilter(setOrganizerId)}
                w={200}
              />
            )}
          </Group>

          {camps.isError ? (
            <LoadError
              message={errorMessage(t, camps.error)}
              onRetry={() => void camps.refetch()}
            />
          ) : data && data.totalItems === 0 && !hasFilters ? (
            <Stack align="center" py="xl" gap="sm">
              <IconTent size={48} stroke={1.3} color="var(--mantine-color-dimmed)" />
              <Text c="dimmed" ta="center" maw={420}>
                {canCreate ? t('camps.emptyCanCreate') : t('camps.empty')}
              </Text>
              {canCreate && (
                <Button component={Link} to="/camps/new" leftSection={<IconPlus size={18} />}>
                  {t('camps.add')}
                </Button>
              )}
            </Stack>
          ) : (
            <Table.ScrollContainer minWidth={showOrganizer ? 760 : 620}>
              <Table verticalSpacing="sm" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>{t('camps.columns.camp')}</Table.Th>
                    <Table.Th>{t('camps.columns.dates')}</Table.Th>
                    <Table.Th>{t('camps.columns.status')}</Table.Th>
                    {showOrganizer && <Table.Th>{t('camps.columns.organizer')}</Table.Th>}
                    <Table.Th w={90}>{t('camps.columns.services')}</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {camps.isPending
                    ? Array.from({ length: 5 }, (_, index) => (
                        <Table.Tr key={index}>
                          <Table.Td colSpan={columns}>
                            <Skeleton height={28} />
                          </Table.Td>
                        </Table.Tr>
                      ))
                    : rows}
                  {data && data.items.length === 0 && (
                    <Table.Tr>
                      <Table.Td colSpan={columns}>
                        <Text c="dimmed" ta="center" py="lg">
                          {t('camps.noMatches')}
                        </Text>
                      </Table.Td>
                    </Table.Tr>
                  )}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}

          {data && data.totalItems > 0 && (
            <Group justify="space-between">
              <Text size="sm" c="dimmed">
                {t('camps.count', { count: data.totalItems })}
              </Text>
              {data.totalPages > 1 && (
                <Pagination
                  total={data.totalPages}
                  value={page + 1}
                  onChange={(value) => setPage(value - 1)}
                />
              )}
            </Group>
          )}
        </Stack>
      </Paper>
    </>
  )
}
