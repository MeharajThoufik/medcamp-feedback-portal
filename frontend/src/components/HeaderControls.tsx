import {
  ActionIcon,
  SegmentedControl,
  Tooltip,
  useComputedColorScheme,
  useMantineColorScheme,
} from '@mantine/core'
import { IconMoon, IconSun } from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'
import { LANGUAGES } from '../i18n'

export function LanguageSwitch() {
  const { t, i18n } = useTranslation()
  return (
    <SegmentedControl
      size="xs"
      aria-label={t('nav.language')}
      value={i18n.resolvedLanguage ?? 'en'}
      onChange={(language) => void i18n.changeLanguage(language)}
      data={LANGUAGES.map((language) => ({ value: language.code, label: language.short }))}
    />
  )
}

export function ColorSchemeToggle() {
  const { t } = useTranslation()
  const { setColorScheme } = useMantineColorScheme()
  const scheme = useComputedColorScheme('light')
  return (
    <Tooltip label={t('nav.colorScheme')}>
      <ActionIcon
        variant="default"
        size="lg"
        aria-label={t('nav.colorScheme')}
        onClick={() => setColorScheme(scheme === 'dark' ? 'light' : 'dark')}
      >
        {scheme === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}
      </ActionIcon>
    </Tooltip>
  )
}
