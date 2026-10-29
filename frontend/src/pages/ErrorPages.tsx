import { Button, Center, Stack, Text, Title } from '@mantine/core'
import { IconLock, IconMapQuestion, type Icon } from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

function ErrorState({ icon: StateIcon, title, body }: { icon: Icon; title: string; body: string }) {
  const { t } = useTranslation()
  return (
    <Center mih="60vh" p="md">
      <Stack align="center" maw={420} gap="sm">
        <StateIcon size={56} stroke={1.4} color="var(--mantine-color-dimmed)" />
        <Title order={3} ta="center">
          {title}
        </Title>
        <Text c="dimmed" ta="center">
          {body}
        </Text>
        <Button component={Link} to="/" mt="sm">
          {t('common.goHome')}
        </Button>
      </Stack>
    </Center>
  )
}

export function ForbiddenPage() {
  const { t } = useTranslation()
  return (
    <ErrorState
      icon={IconLock}
      title={t('errors.forbiddenTitle')}
      body={t('errors.forbiddenBody')}
    />
  )
}

export function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <ErrorState
      icon={IconMapQuestion}
      title={t('errors.notFoundTitle')}
      body={t('errors.notFoundBody')}
    />
  )
}
