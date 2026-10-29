import { Group, Text, ThemeIcon } from '@mantine/core'
import { IconFirstAidKit } from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'

export function AppLogo({ size = 'md' }: { size?: 'md' | 'lg' }) {
  const { t } = useTranslation()
  return (
    <Group gap="xs" wrap="nowrap">
      <ThemeIcon size={size === 'lg' ? 44 : 32} radius="md">
        <IconFirstAidKit size={size === 'lg' ? 26 : 20} />
      </ThemeIcon>
      <Text fw={700} size={size === 'lg' ? 'xl' : 'md'} lineClamp={1}>
        {t('app.shortName')}
      </Text>
    </Group>
  )
}
