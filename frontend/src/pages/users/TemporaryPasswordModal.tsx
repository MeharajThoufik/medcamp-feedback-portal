import { Button, Code, CopyButton, Group, Modal, Stack, Text } from '@mantine/core'
import { IconCheck, IconCopy } from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'

export interface TemporaryPassword {
  name: string
  password: string
}

/** Shows a one-time password. It cannot be closed by clicking outside, so it is not lost by accident. */
export function TemporaryPasswordModal({
  value,
  onClose,
}: {
  value: TemporaryPassword | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  return (
    <Modal
      opened={value !== null}
      onClose={onClose}
      title={t('users.tempPassword.title')}
      centered
      closeOnClickOutside={false}
    >
      {value && (
        <Stack>
          <Text size="sm">{t('users.tempPassword.body', { name: value.name })}</Text>
          <Group gap="sm" wrap="nowrap">
            <Code fz="lg" px="md" py="sm" style={{ flex: 1, letterSpacing: 1 }}>
              {value.password}
            </Code>
            <CopyButton value={value.password}>
              {({ copied, copy }) => (
                <Button
                  variant={copied ? 'filled' : 'light'}
                  color={copied ? 'teal' : undefined}
                  leftSection={copied ? <IconCheck size={16} /> : <IconCopy size={16} />}
                  onClick={copy}
                >
                  {copied ? t('users.tempPassword.copied') : t('users.tempPassword.copy')}
                </Button>
              )}
            </CopyButton>
          </Group>
          <Group justify="flex-end">
            <Button onClick={onClose}>{t('users.tempPassword.done')}</Button>
          </Group>
        </Stack>
      )}
    </Modal>
  )
}
