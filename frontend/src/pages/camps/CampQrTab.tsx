import {
  Alert,
  Box,
  Button,
  Code,
  CopyButton,
  Group,
  Paper,
  Stack,
  Text,
  Title,
} from '@mantine/core'
import { modals } from '@mantine/modals'
import { notifications } from '@mantine/notifications'
import {
  IconCheck,
  IconCopy,
  IconDownload,
  IconExternalLink,
  IconPrinter,
  IconRefresh,
} from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { campQrUrl, useRegenerateCampLink } from '../../api/camps'
import type { Camp } from '../../api/types'
import { errorMessage } from '../../i18n/errors'
import { isLocalOnlyLink } from './links'

export function CampQrTab({ camp }: { camp: Camp }) {
  const { t } = useTranslation()
  const regenerate = useRegenerateCampLink()
  const localOnly = isLocalOnlyLink(camp.feedbackUrl)

  const confirmRegenerate = () =>
    modals.openConfirmModal({
      title: t('camps.qr.regenerateConfirm.title'),
      children: <Text size="sm">{t('camps.qr.regenerateConfirm.body')}</Text>,
      labels: { confirm: t('camps.qr.regenerate'), cancel: t('common.cancel') },
      confirmProps: { color: 'red' },
      onConfirm: () =>
        regenerate.mutate(camp.id, {
          onSuccess: () =>
            notifications.show({ color: 'teal', message: t('camps.qr.regenerated') }),
          onError: (error) =>
            notifications.show({ color: 'red', message: errorMessage(t, error, 'camps') }),
        }),
    })

  return (
    <Stack>
      {localOnly && (
        <Alert color="orange" title={t('camps.qr.localTitle')}>
          <Stack gap={6}>
            <Text size="sm">{t('camps.qr.localBody')}</Text>
            <Code block>APP_PUBLIC_BASE_URL=http://192.168.137.1:8080</Code>
          </Stack>
        </Alert>
      )}
      {camp.feedbackState !== 'OPEN' && <Alert color="gray">{t('camps.qr.worksBeforeOpen')}</Alert>}

      <Paper withBorder radius="md" p="lg">
        <Group align="flex-start" gap="xl">
          {/* The QR image is always black on white, also in dark mode, so cameras can read it. */}
          <Box bg="white" p={4} style={{ borderRadius: 8, lineHeight: 0 }}>
            <img
              src={campQrUrl(camp.id, camp.publicToken, { size: 480 })}
              width={220}
              height={220}
              alt={t('camps.qr.imageAlt', { name: camp.name })}
            />
          </Box>
          <Stack gap="sm" style={{ flex: '1 1 260px' }}>
            <Stack gap={4}>
              <Text fw={600}>{t('camps.qr.linkLabel')}</Text>
              <Text size="sm" c="dimmed">
                {t('camps.qr.linkHint')}
              </Text>
            </Stack>
            <Group gap="xs" wrap="nowrap">
              <Code fz="md" px="sm" py={6} style={{ flex: 1, wordBreak: 'break-all' }}>
                {camp.feedbackUrl}
              </Code>
              <CopyButton value={camp.feedbackUrl}>
                {({ copied, copy }) => (
                  <Button
                    variant={copied ? 'filled' : 'light'}
                    leftSection={copied ? <IconCheck size={16} /> : <IconCopy size={16} />}
                    onClick={copy}
                  >
                    {copied ? t('camps.qr.copied') : t('camps.qr.copy')}
                  </Button>
                )}
              </CopyButton>
            </Group>
            <Group gap="xs" mt="xs">
              {camp.canEdit && (
                <Button
                  component={Link}
                  to={`/camps/${camp.id}/poster`}
                  leftSection={<IconPrinter size={16} />}
                >
                  {t('camps.qr.printPoster')}
                </Button>
              )}
              <Button
                component="a"
                href={campQrUrl(camp.id, camp.publicToken, { size: 1024, download: true })}
                download
                variant="light"
                leftSection={<IconDownload size={16} />}
              >
                {t('camps.qr.downloadPng')}
              </Button>
              <Button
                component="a"
                href={camp.feedbackUrl}
                target="_blank"
                rel="noreferrer"
                variant="subtle"
                leftSection={<IconExternalLink size={16} />}
              >
                {t('camps.qr.openLink')}
              </Button>
            </Group>
          </Stack>
        </Group>
      </Paper>

      {camp.canEdit && camp.status !== 'ARCHIVED' && (
        <Paper withBorder radius="md" p="lg">
          <Group justify="space-between" align="flex-start" gap="md">
            <Stack gap={4} style={{ flex: '1 1 320px' }}>
              <Title order={5}>{t('camps.qr.regenerateTitle')}</Title>
              <Text size="sm" c="dimmed">
                {t('camps.qr.regenerateHint')}
              </Text>
            </Stack>
            <Button
              color="red"
              variant="light"
              leftSection={<IconRefresh size={16} />}
              loading={regenerate.isPending}
              onClick={confirmRegenerate}
            >
              {t('camps.qr.regenerate')}
            </Button>
          </Group>
        </Paper>
      )}
    </Stack>
  )
}
