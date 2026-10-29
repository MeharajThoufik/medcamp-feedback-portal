import {
  ActionIcon,
  Badge,
  Button,
  Group,
  Menu,
  Pagination,
  Paper,
  SegmentedControl,
  Select,
  Skeleton,
  Stack,
  Table,
  Text,
  TextInput,
  VisuallyHidden,
} from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { modals } from '@mantine/modals'
import { notifications } from '@mantine/notifications'
import {
  IconDots,
  IconKey,
  IconPencil,
  IconPlus,
  IconSearch,
  IconUserCheck,
  IconUserOff,
} from '@tabler/icons-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ROLES, type Role, type User } from '../../api/types'
import { useResetPassword, useSetUserEnabled, useUsers } from '../../api/users'
import { useCurrentUser } from '../../auth/useCurrentUser'
import { PageHeader } from '../../components/PageHeader'
import { LoadError } from '../../components/PageStates'
import { RoleBadge } from '../../components/RoleBadge'
import { errorMessage } from '../../i18n/errors'
import { TemporaryPasswordModal, type TemporaryPassword } from './TemporaryPasswordModal'
import { UserFormModal } from './UserFormModal'

type StatusFilter = 'all' | 'active' | 'disabled'

function useDateTime() {
  const { i18n } = useTranslation()
  const format = new Intl.DateTimeFormat(i18n.language === 'hi' ? 'hi-IN' : 'en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
  return (iso: string) => format.format(new Date(iso))
}

export function UsersPage() {
  const { t } = useTranslation()
  const me = useCurrentUser()
  const formatDateTime = useDateTime()

  const [search, setSearch] = useState('')
  const [debouncedSearch] = useDebouncedValue(search, 300)
  const [role, setRole] = useState<Role | null>(null)
  const [status, setStatus] = useState<StatusFilter>('all')
  const [page, setPage] = useState(0)

  const [formOpened, setFormOpened] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [temporaryPassword, setTemporaryPassword] = useState<TemporaryPassword | null>(null)

  const users = useUsers({
    search: debouncedSearch,
    role,
    enabled: status === 'all' ? null : status === 'active',
    page,
  })
  const setEnabled = useSetUserEnabled()
  const resetPassword = useResetPassword()

  const changeFilter =
    <T,>(setter: (value: T) => void) =>
    (value: T) => {
      setter(value)
      setPage(0)
    }

  const openCreate = () => {
    setEditing(null)
    setFormOpened(true)
  }

  const openEdit = (user: User) => {
    setEditing(user)
    setFormOpened(true)
  }

  const showError = (error: unknown) =>
    notifications.show({ color: 'red', message: errorMessage(t, error, 'users') })

  const confirmToggleEnabled = (user: User) => {
    const enable = !user.enabled
    const run = () =>
      setEnabled.mutate(
        { id: user.id, enabled: enable },
        {
          onSuccess: () =>
            notifications.show({
              color: 'teal',
              message: enable ? t('users.enabled') : t('users.disabled'),
            }),
          onError: showError,
        },
      )
    if (enable) {
      run()
      return
    }
    modals.openConfirmModal({
      title: t('users.confirmDisable.title', { name: user.fullName }),
      children: <Text size="sm">{t('users.confirmDisable.body')}</Text>,
      labels: { confirm: t('users.confirmDisable.confirm'), cancel: t('common.cancel') },
      confirmProps: { color: 'red' },
      onConfirm: run,
    })
  }

  const confirmResetPassword = (user: User) =>
    modals.openConfirmModal({
      title: t('users.confirmReset.title', { name: user.fullName }),
      children: <Text size="sm">{t('users.confirmReset.body')}</Text>,
      labels: { confirm: t('users.confirmReset.confirm'), cancel: t('common.cancel') },
      onConfirm: () =>
        resetPassword.mutate(user.id, {
          onSuccess: (result) =>
            setTemporaryPassword({ name: user.fullName, password: result.temporaryPassword }),
          onError: showError,
        }),
    })

  const data = users.data
  const rows = data?.items.map((user) => {
    const isSelf = user.id === me.id
    return (
      <Table.Tr key={user.id} opacity={user.enabled ? 1 : 0.6}>
        <Table.Td>
          <Group gap={6} wrap="nowrap">
            <Text fw={500} size="sm">
              {user.fullName}
            </Text>
            {isSelf && (
              <Badge size="xs" variant="outline">
                {t('users.badges.you')}
              </Badge>
            )}
          </Group>
          <Text size="xs" c="dimmed">
            {user.email}
          </Text>
        </Table.Td>
        <Table.Td>
          <RoleBadge role={user.role} />
        </Table.Td>
        <Table.Td>
          <Text size="sm">{user.departmentName ?? '—'}</Text>
        </Table.Td>
        <Table.Td>
          <Group gap={4}>
            <Badge variant="dot" color={user.enabled ? 'teal' : 'gray'}>
              {user.enabled ? t('users.statusActive') : t('users.statusDisabled')}
            </Badge>
            {user.locked && (
              <Badge variant="light" color="red">
                {t('users.badges.locked')}
              </Badge>
            )}
            {user.mustChangePassword && (
              <Badge variant="light" color="yellow">
                {t('users.badges.mustChange')}
              </Badge>
            )}
          </Group>
        </Table.Td>
        <Table.Td>
          <Text size="sm" c={user.lastLoginAt ? undefined : 'dimmed'}>
            {user.lastLoginAt ? formatDateTime(user.lastLoginAt) : t('common.never')}
          </Text>
        </Table.Td>
        <Table.Td>
          <Menu position="bottom-end" withinPortal>
            <Menu.Target>
              <ActionIcon variant="subtle" color="gray" aria-label={t('common.actions')}>
                <IconDots size={18} />
              </ActionIcon>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Item leftSection={<IconPencil size={16} />} onClick={() => openEdit(user)}>
                {t('users.menu.edit')}
              </Menu.Item>
              <Menu.Item
                leftSection={<IconKey size={16} />}
                onClick={() => confirmResetPassword(user)}
              >
                {t('users.menu.resetPassword')}
              </Menu.Item>
              {!isSelf && (
                <Menu.Item
                  color={user.enabled ? 'red' : 'teal'}
                  leftSection={
                    user.enabled ? <IconUserOff size={16} /> : <IconUserCheck size={16} />
                  }
                  onClick={() => confirmToggleEnabled(user)}
                >
                  {user.enabled ? t('users.menu.disable') : t('users.menu.enable')}
                </Menu.Item>
              )}
            </Menu.Dropdown>
          </Menu>
        </Table.Td>
      </Table.Tr>
    )
  })

  return (
    <>
      <PageHeader
        title={t('users.title')}
        subtitle={t('users.subtitle')}
        action={
          <Button leftSection={<IconPlus size={18} />} onClick={openCreate}>
            {t('users.add')}
          </Button>
        }
      />

      <Paper withBorder radius="md" p="md">
        <Stack>
          <Group gap="sm" align="flex-end">
            <TextInput
              placeholder={t('users.searchPlaceholder')}
              aria-label={t('users.searchPlaceholder')}
              leftSection={<IconSearch size={16} />}
              value={search}
              onChange={(event) => changeFilter(setSearch)(event.currentTarget.value)}
              style={{ flex: '1 1 220px' }}
            />
            <Select
              aria-label={t('users.role')}
              placeholder={t('users.allRoles')}
              clearable
              data={ROLES.map((r) => ({ value: r, label: t(`roles.${r}`) }))}
              value={role}
              onChange={(value) => changeFilter(setRole)(value as Role | null)}
              w={180}
            />
            <SegmentedControl
              aria-label={t('users.status')}
              value={status}
              onChange={(value) => changeFilter(setStatus)(value as StatusFilter)}
              data={[
                { value: 'all', label: t('common.all') },
                { value: 'active', label: t('users.statusActive') },
                { value: 'disabled', label: t('users.statusDisabled') },
              ]}
            />
          </Group>

          {users.isError ? (
            <LoadError
              message={errorMessage(t, users.error)}
              onRetry={() => void users.refetch()}
            />
          ) : (
            <Table.ScrollContainer minWidth={760}>
              <Table verticalSpacing="sm" highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>{t('users.columns.name')}</Table.Th>
                    <Table.Th>{t('users.columns.role')}</Table.Th>
                    <Table.Th>{t('users.columns.department')}</Table.Th>
                    <Table.Th>{t('users.columns.status')}</Table.Th>
                    <Table.Th>{t('users.columns.lastLogin')}</Table.Th>
                    <Table.Th w={48}>
                      <VisuallyHidden>{t('common.actions')}</VisuallyHidden>
                    </Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {users.isPending
                    ? Array.from({ length: 5 }, (_, index) => (
                        <Table.Tr key={index}>
                          <Table.Td colSpan={6}>
                            <Skeleton height={28} />
                          </Table.Td>
                        </Table.Tr>
                      ))
                    : rows}
                  {data && data.items.length === 0 && (
                    <Table.Tr>
                      <Table.Td colSpan={6}>
                        <Text c="dimmed" ta="center" py="lg">
                          {t('users.empty')}
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
                {t('users.count', { count: data.totalItems })}
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

      <UserFormModal
        opened={formOpened}
        user={editing}
        currentUserId={me.id}
        onClose={() => setFormOpened(false)}
        onCreated={(user, password) => {
          setFormOpened(false)
          notifications.show({ color: 'teal', message: t('users.created') })
          setTemporaryPassword({ name: user.fullName, password })
        }}
        onUpdated={() => {
          setFormOpened(false)
          notifications.show({ color: 'teal', message: t('users.updated') })
        }}
      />
      <TemporaryPasswordModal
        value={temporaryPassword}
        onClose={() => setTemporaryPassword(null)}
      />
    </>
  )
}
