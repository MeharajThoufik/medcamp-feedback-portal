import { Anchor, Button, Group, Paper, Stack, Text } from '@mantine/core'
import { IconCheck, IconCopy } from '@tabler/icons-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { copyText } from '../../utils/copyText'

/** The complaint reference on the thank-you screen: the attendee's only way to check on it later. */
export function ReferenceCard({ reference }: { reference: string }) {
  const { t } = useTranslation()
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    setCopied(await copyText(reference))
  }

  return (
    <Paper withBorder radius="md" p="md" w="100%" ta="left">
      <Stack gap="sm">
        <Text size="sm" fw={600}>
          {t('feedbackForm.reference.title')}
        </Text>
        <Group gap="sm" wrap="nowrap">
          <Text
            ff="monospace"
            fz={24}
            fw={700}
            style={{ flex: 1, letterSpacing: 1, userSelect: 'all' }}
            aria-label={t('feedbackForm.reference.title')}
          >
            {reference}
          </Text>
          <Button
            variant={copied ? 'filled' : 'light'}
            leftSection={copied ? <IconCheck size={16} /> : <IconCopy size={16} />}
            onClick={() => void copy()}
          >
            {copied ? t('feedbackForm.reference.copied') : t('feedbackForm.reference.copy')}
          </Button>
        </Group>
        <Text size="sm">{t('feedbackForm.reference.screenshot')}</Text>
        <Anchor component={Link} to={`/track?ref=${encodeURIComponent(reference)}`} size="sm">
          {t('feedbackForm.reference.check')}
        </Anchor>
      </Stack>
    </Paper>
  )
}
