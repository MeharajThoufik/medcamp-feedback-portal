import {
  ActionIcon,
  Button,
  Group,
  Modal,
  Paper,
  Skeleton,
  Stack,
  Switch,
  Table,
  Text,
  TextInput,
  Tooltip,
} from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { IconPencil, IconPlus } from '@tabler/icons-react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useDepartments, useSaveDepartment } from '../api/departments'
import type { Department } from '../api/types'
import { PageHeader } from '../components/PageHeader'
import { LoadError } from '../components/PageStates'
import { errorMessage } from '../i18n/errors'

/** Rendered with a `key` per department, so its state starts fresh for each one. */
function RenameModal({
  department,
  onClose,
}: {
  department: Department | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  const save = useSaveDepartment()
  const [name, setName] = useState(department?.name ?? '')

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!department || !name.trim()) return
    save.mutate(
      { id: department.id, name: name.trim(), active: department.active },
      {
        onSuccess: () => {
          notifications.show({ color: 'teal', message: t('departments.updated') })
          onClose()
        },
      },
    )
  }

  return (
    <Modal
      opened={department !== null}
      onClose={onClose}
      title={t('departments.renameTitle')}
      centered
    >
      <form onSubmit={submit}>
        <Stack>
          <TextInput
            label={t('departments.name')}
            data-autofocus
            value={name}
            maxLength={100}
            onChange={(event) => setName(event.currentTarget.value)}
            error={save.isError ? errorMessage(t, save.error, 'departments') : undefined}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" loading={save.isPending} disabled={!name.trim()}>
              {t('common.save')}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}

export function DepartmentsPage() {
  const { t } = useTranslation()
  const [showInactive, setShowInactive] = useState(false)
  const [newName, setNewName] = useState('')
  const [renaming, setRenaming] = useState<Department | null>(null)
  const departments = useDepartments(showInactive)
  const create = useSaveDepartment()
  const toggle = useSaveDepartment()

  const addDepartment = (event: FormEvent) => {
    event.preventDefault()
    const name = newName.trim()
    if (!name) return
    create.mutate(
      { name },
      {
        onSuccess: () => {
          setNewName('')
          notifications.show({ color: 'teal', message: t('departments.created') })
        },
      },
    )
  }

  const setActive = (department: Department, active: boolean) =>
    toggle.mutate(
      { id: department.id, name: department.name, active },
      {
        onError: (error) =>
          notifications.show({ color: 'red', message: errorMessage(t, error, 'departments') }),
      },
    )

  return (
    <>
      <PageHeader title={t('departments.title')} subtitle={t('departments.subtitle')} />

      <Stack maw={720}>
        <Paper withBorder radius="md" p="md">
          <form onSubmit={addDepartment}>
            <Group align="flex-start" gap="sm">
              <TextInput
                aria-label={t('departments.name')}
                placeholder={t('departments.namePlaceholder')}
                value={newName}
                maxLength={100}
                onChange={(event) => {
                  setNewName(event.currentTarget.value)
                  if (create.isError) create.reset()
                }}
                error={create.isError ? errorMessage(t, create.error, 'departments') : undefined}
                style={{ flex: 1 }}
              />
              <Button
                type="submit"
                leftSection={<IconPlus size={18} />}
                loading={create.isPending}
                disabled={!newName.trim()}
              >
                {t('departments.add')}
              </Button>
            </Group>
          </form>
        </Paper>

        <Paper withBorder radius="md" p="md">
          <Group justify="flex-end" mb="sm">
            <Switch
              label={t('departments.showInactive')}
              checked={showInactive}
              onChange={(event) => setShowInactive(event.currentTarget.checked)}
            />
          </Group>

          {departments.isError ? (
            <LoadError
              message={errorMessage(t, departments.error)}
              onRetry={() => void departments.refetch()}
            />
          ) : departments.isPending ? (
            <Stack>
              {Array.from({ length: 3 }, (_, index) => (
                <Skeleton key={index} height={36} />
              ))}
            </Stack>
          ) : departments.data.length === 0 ? (
            <Text c="dimmed" ta="center" py="lg">
              {t('departments.empty')}
            </Text>
          ) : (
            <Table verticalSpacing="sm">
              <Table.Tbody>
                {departments.data.map((department) => (
                  <Table.Tr key={department.id}>
                    <Table.Td>
                      <Text fw={500} c={department.active ? undefined : 'dimmed'}>
                        {department.name}
                      </Text>
                    </Table.Td>
                    <Table.Td w={140}>
                      <Switch
                        size="sm"
                        label={department.active ? t('common.active') : t('common.inactive')}
                        checked={department.active}
                        onChange={(event) => setActive(department, event.currentTarget.checked)}
                      />
                    </Table.Td>
                    <Table.Td w={48}>
                      <Tooltip label={t('departments.rename')}>
                        <ActionIcon
                          variant="subtle"
                          color="gray"
                          aria-label={t('departments.rename')}
                          onClick={() => setRenaming(department)}
                        >
                          <IconPencil size={18} />
                        </ActionIcon>
                      </Tooltip>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          )}
        </Paper>
      </Stack>

      <RenameModal
        key={renaming?.id ?? 'closed'}
        department={renaming}
        onClose={() => setRenaming(null)}
      />
    </>
  )
}
