import { Alert, Button, Center, Loader, Stack, Text } from '@mantine/core'
import { IconPlugConnectedX } from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'

export function FullPageLoader() {
  return (
    <Center h="100vh">
      <Loader />
    </Center>
  )
}

export function ServerUnreachable({ onRetry }: { onRetry: () => void }) {
  const { t } = useTranslation()
  return (
    <Center h="100vh" p="md">
      <Stack align="center" maw={420}>
        <IconPlugConnectedX size={48} stroke={1.5} />
        <Text ta="center">{t('common.serverUnreachable')}</Text>
        <Button onClick={onRetry}>{t('common.retry')}</Button>
      </Stack>
    </Center>
  )
}

/** Inline error block for a failed list or page load. */
export function LoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  const { t } = useTranslation()
  return (
    <Alert color="red" title={message}>
      <Button size="xs" variant="light" color="red" onClick={onRetry}>
        {t('common.retry')}
      </Button>
    </Alert>
  )
}
