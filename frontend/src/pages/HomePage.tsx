import { Button, Card, Group, SimpleGrid, Stack, Text, ThemeIcon, Title } from '@mantine/core'
import {
  IconBuildingHospital,
  IconHourglassHigh,
  IconPlus,
  IconTent,
  IconUsers,
} from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { useCurrentUser } from '../auth/useCurrentUser'
import { RoleBadge } from '../components/RoleBadge'

export function HomePage() {
  const { t } = useTranslation()
  const me = useCurrentUser()

  return (
    <Stack gap="lg" maw={960}>
      <Card withBorder radius="lg" p="xl">
        <Stack gap="xs">
          <Group gap="sm">
            <Title order={2}>{t('home.greeting', { name: me.fullName })}</Title>
            <RoleBadge role={me.role} />
          </Group>
          <Text c="dimmed">{t(`home.roleInfo.${me.role}`)}</Text>
          {me.departmentName && (
            <Text size="sm">{t('home.department', { name: me.departmentName })}</Text>
          )}
          {me.role === 'ORGANIZER' && (
            <Group mt="sm">
              <Button component={Link} to="/camps/new" leftSection={<IconPlus size={18} />}>
                {t('home.createCamp')}
              </Button>
              <Button
                component={Link}
                to="/camps"
                variant="light"
                leftSection={<IconTent size={18} />}
              >
                {t('home.viewCamps')}
              </Button>
            </Group>
          )}
          {me.role === 'SUPER_ADMIN' && (
            <Group mt="sm">
              <Button component={Link} to="/users" leftSection={<IconUsers size={18} />}>
                {t('home.manageUsers')}
              </Button>
              <Button
                component={Link}
                to="/camps"
                variant="light"
                leftSection={<IconTent size={18} />}
              >
                {t('home.viewCamps')}
              </Button>
              <Button
                component={Link}
                to="/departments"
                variant="light"
                leftSection={<IconBuildingHospital size={18} />}
              >
                {t('home.manageDepartments')}
              </Button>
            </Group>
          )}
        </Stack>
      </Card>

      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <Card withBorder radius="lg" p="lg">
          <Group wrap="nowrap" align="flex-start">
            <ThemeIcon variant="light" size="lg" radius="md">
              <IconHourglassHigh size={20} />
            </ThemeIcon>
            <Stack gap={4}>
              <Text fw={600}>{t('home.nextTitle')}</Text>
              <Text size="sm" c="dimmed">
                {t('home.next')}
              </Text>
            </Stack>
          </Group>
        </Card>
      </SimpleGrid>
    </Stack>
  )
}
