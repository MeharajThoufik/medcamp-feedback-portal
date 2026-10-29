import {
  ActionIcon,
  AppShell,
  Avatar,
  Burger,
  Group,
  Indicator,
  Menu,
  NavLink,
  ScrollArea,
  Stack,
  Text,
  UnstyledButton,
} from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconBell, IconChevronDown, IconKey, IconLogout } from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import { useLogout } from '../api/auth'
import { useCurrentUser } from '../auth/useCurrentUser'
import { AppLogo } from '../components/AppLogo'
import { ColorSchemeToggle, LanguageSwitch } from '../components/HeaderControls'
import { RoleBadge } from '../components/RoleBadge'
import { navigationFor } from './navigation'

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]!.toUpperCase())
    .join('')
}

export function AppLayout() {
  const { t } = useTranslation()
  const me = useCurrentUser()
  const location = useLocation()
  const navigate = useNavigate()
  const logout = useLogout()
  const [navbarOpened, { toggle: toggleNavbar, close: closeNavbar }] = useDisclosure()

  const isActive = (to: string) =>
    to === '/' ? location.pathname === '/' : location.pathname.startsWith(to)

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 250, breakpoint: 'sm', collapsed: { mobile: !navbarOpened } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger
              opened={navbarOpened}
              onClick={toggleNavbar}
              hiddenFrom="sm"
              size="sm"
              aria-label={t('nav.toggleMenu')}
            />
            <UnstyledButton component={Link} to="/">
              <AppLogo />
            </UnstyledButton>
          </Group>

          <Group gap="xs" wrap="nowrap">
            <Group gap="xs" visibleFrom="xs" wrap="nowrap">
              <LanguageSwitch />
              <ColorSchemeToggle />
            </Group>

            {/* Placeholder until live notifications arrive in Phase 6. */}
            <Menu position="bottom-end" width={260}>
              <Menu.Target>
                <ActionIcon variant="default" size="lg" aria-label={t('nav.notifications')}>
                  <Indicator disabled size={8}>
                    <IconBell size={18} />
                  </Indicator>
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                <Menu.Label>{t('nav.notifications')}</Menu.Label>
                <Text size="sm" c="dimmed" px="sm" py="xs">
                  {t('nav.noNotifications')}
                </Text>
              </Menu.Dropdown>
            </Menu>

            <Menu position="bottom-end" width={240}>
              <Menu.Target>
                <UnstyledButton aria-label={me.fullName}>
                  <Group gap={6} wrap="nowrap">
                    <Avatar color="teal" radius="xl" size={34}>
                      {initials(me.fullName)}
                    </Avatar>
                    <IconChevronDown size={14} />
                  </Group>
                </UnstyledButton>
              </Menu.Target>
              <Menu.Dropdown>
                <Stack gap={4} px="sm" py="xs">
                  <Text fw={600} size="sm" lineClamp={1}>
                    {me.fullName}
                  </Text>
                  <Text size="xs" c="dimmed" lineClamp={1}>
                    {me.email}
                  </Text>
                  <div>
                    <RoleBadge role={me.role} />
                  </div>
                </Stack>
                <Group gap="xs" px="sm" pb="xs" hiddenFrom="xs">
                  <LanguageSwitch />
                  <ColorSchemeToggle />
                </Group>
                <Menu.Divider />
                <Menu.Item
                  leftSection={<IconKey size={16} />}
                  onClick={() => navigate('/account/password')}
                >
                  {t('nav.changePassword')}
                </Menu.Item>
                <Menu.Item
                  color="red"
                  leftSection={<IconLogout size={16} />}
                  onClick={() => logout.mutate()}
                >
                  {t('nav.signOut')}
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="sm">
        <AppShell.Section grow component={ScrollArea}>
          {navigationFor(me.role).map((section, index) => (
            <Stack key={section.titleKey ?? index} gap={2} mb="md">
              {section.titleKey && (
                <Text size="xs" fw={600} c="dimmed" tt="uppercase" px="sm" mb={4}>
                  {t(section.titleKey)}
                </Text>
              )}
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  component={Link}
                  to={item.to}
                  label={t(item.labelKey)}
                  leftSection={<item.icon size={18} stroke={1.6} />}
                  active={isActive(item.to)}
                  onClick={closeNavbar}
                  style={{ borderRadius: 'var(--mantine-radius-md)' }}
                />
              ))}
            </Stack>
          ))}
        </AppShell.Section>
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
