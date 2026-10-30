import { Alert, Anchor, Button, Group, Stack, Text } from '@mantine/core'
import { IconArrowLeft, IconPrinter } from '@tabler/icons-react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { campQrUrl, useCampPoster } from '../../api/camps'
import type { Poster } from '../../api/types'
import { wifiQrUrl } from '../../api/settings'
import i18n from '../../i18n'
import { CampLoadError, CampPageSkeleton } from './CampLoadState'
import { isLocalOnlyLink } from './links'
import './poster.css'

/** Poster text is always printed in both English and Hindi, whatever language the screen uses. */
const en = i18n.getFixedT('en', undefined, 'poster')
const hi = i18n.getFixedT('hi', undefined, 'poster')

function formatRange(from: string, to: string) {
  const toDate = (date: string) => {
    const [year, month, day] = date.split('-').map(Number)
    return new Date(year, month - 1, day)
  }
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).formatRange(
    toDate(from),
    toDate(to),
  )
}

export function CampPosterPage() {
  const { t } = useTranslation()
  const id = Number(useParams().id)
  const poster = useCampPoster(id)

  if (poster.isPending)
    return (
      <Stack p="md">
        <CampPageSkeleton />
      </Stack>
    )
  if (poster.isError)
    return (
      <Stack p="md">
        <CampLoadError error={poster.error} onRetry={() => void poster.refetch()} />
      </Stack>
    )

  return (
    <div className="poster-page">
      <Stack className="poster-toolbar" maw="210mm" mx="auto" mb="md" gap="sm">
        <Group justify="space-between">
          <Anchor component={Link} to={`/camps/${id}?tab=qr`} size="sm">
            <Group gap={4}>
              <IconArrowLeft size={14} />
              {t('camps.backToCamp')}
            </Group>
          </Anchor>
          <Button leftSection={<IconPrinter size={16} />} onClick={() => window.print()}>
            {t('camps.poster.print')}
          </Button>
        </Group>
        <Text size="sm" c="dark.6">
          {t('camps.poster.printHint')}
        </Text>
        {isLocalOnlyLink(poster.data.feedbackUrl) && (
          <Alert color="orange" title={t('camps.qr.localTitle')}>
            {t('camps.qr.localBody')}
          </Alert>
        )}
        {!poster.data.wifi && (
          <Text size="sm" c="dark.6">
            {t('camps.poster.noWifi')}
          </Text>
        )}
      </Stack>
      <PosterSheet poster={poster.data} />
    </div>
  )
}

function Bilingual({ k, className }: { k: string; className: string }) {
  return (
    <>
      <p className={className}>{en(k)}</p>
      <p className={`${className}-hi`}>{hi(k)}</p>
    </>
  )
}

export function PosterSheet({ poster }: { poster: Poster }) {
  const qr = campQrUrl(poster.campId, poster.feedbackUrl, { size: 1024 })
  const wifi = poster.wifi
  return (
    <article className="poster-sheet" aria-label={en('title')}>
      <h1 className="poster-title">{en('title')}</h1>
      <p className="poster-title-hi">{hi('title')}</p>
      <p className="poster-subtitle">
        {en('subtitle')} · {hi('subtitle')}
      </p>

      <div className="poster-camp">
        <p className="poster-camp-name">{poster.name}</p>
        <p className="poster-camp-meta">
          {poster.location} · {formatRange(poster.startDate, poster.endDate)}
        </p>
      </div>

      <div className="poster-steps">
        {wifi && (
          <section className="poster-step poster-step-wifi">
            <span className="poster-step-number">1</span>
            <Bilingual k="wifiStep" className="poster-step-title" />
            <img className="poster-qr poster-qr-wifi" src={wifiQrUrl(600)} alt={en('wifiStep')} />
            <p className="poster-wifi-details">
              {en('network')} / {hi('network')}: <strong>{wifi.ssid}</strong>
              <br />
              {en('password')} / {hi('password')}:{' '}
              <strong>{wifi.password ?? `${en('noPassword')} / ${hi('noPassword')}`}</strong>
            </p>
          </section>
        )}
        <section className="poster-step poster-step-feedback">
          {wifi && <span className="poster-step-number">2</span>}
          <Bilingual k="scanStep" className="poster-step-title" />
          <img className="poster-qr poster-qr-feedback" src={qr} alt={en('scanStep')} />
        </section>
      </div>

      <p className="poster-link-label">
        {en('typeLink')} / {hi('typeLink')}
      </p>
      <p className="poster-link">{poster.feedbackUrl.replace(/^https?:\/\//, '')}</p>

      <ul className="poster-how">
        <li>1. {en('how1')}</li>
        <li>1. {hi('how1')}</li>
        <li>2. {en('how2')}</li>
        <li>2. {hi('how2')}</li>
        <li>3. {en('how3')}</li>
        <li>3. {hi('how3')}</li>
      </ul>
      <p className="poster-footer">
        {en('help')} · {hi('help')}
      </p>
    </article>
  )
}
