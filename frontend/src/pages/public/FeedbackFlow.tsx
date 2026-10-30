import {
  Alert,
  Button,
  Checkbox,
  Group,
  Progress,
  Radio,
  SegmentedControl,
  Stack,
  Switch,
  Text,
  TextInput,
  Textarea,
  ThemeIcon,
  Title,
} from '@mantine/core'
import { IconArrowLeft, IconArrowRight, IconCircleCheck, IconSend } from '@tabler/icons-react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../../api/client'
import { useSubmitFeedback } from '../../api/publicCamps'
import type { FormLanguage, PublicForm, RatingCategory } from '../../api/types'
import { EmojiRating } from '../../components/EmojiRating'
import {
  clearDraft,
  EMPTY_ANSWERS,
  loadDraft,
  markSentFromThisPhone,
  saveDraft,
  STEPS,
  validateStep,
  wasSentFromThisPhone,
  type Answers,
  type Step,
  type StepErrors,
} from './feedbackDraft'

type Screen = 'form' | 'already' | 'done'

/** The steps of the attendee form. In assisted mode a staff member fills it in for an attendee. */
export function FeedbackFlow({
  token,
  form,
  assistedBy,
}: {
  token: string
  form: PublicForm
  /** The signed-in staff member's name in assisted mode, otherwise null. */
  assistedBy: string | null
}) {
  const { t, i18n } = useTranslation()
  const assisted = assistedBy !== null
  const [initial] = useState(() => loadDraft(token))
  const [screen, setScreen] = useState<Screen>(() =>
    !assisted && !initial && wasSentFromThisPhone(token) ? 'already' : 'form',
  )
  const [step, setStep] = useState<Step>(initial?.step ?? 'language')
  const [answers, setAnswers] = useState<Answers>(initial?.answers ?? EMPTY_ANSWERS)
  const [errors, setErrors] = useState<StepErrors>({})
  const [restored] = useState(initial !== null && initial.step !== 'language')
  const submit = useSubmitFeedback(token)
  const headingRef = useRef<HTMLHeadingElement>(null)

  const requiredCategories = form.categories.filter((c) => c.required).map((c) => c.code)
  const language: FormLanguage = i18n.language === 'hi' ? 'hi' : 'en'

  useEffect(() => {
    if (screen === 'form') saveDraft(token, { step, answers })
  }, [token, screen, step, answers])

  // Move focus to the new question, so screen readers announce it.
  useEffect(() => {
    headingRef.current?.focus()
  }, [step, screen])

  const update = (changes: Partial<Answers>) => {
    setAnswers((current) => ({ ...current, ...changes }))
    setErrors({})
  }

  const goTo = (next: Step) => {
    setErrors({})
    submit.reset()
    setStep(next)
    window.scrollTo({ top: 0 })
  }

  const chooseLanguage = (code: FormLanguage) => {
    void i18n.changeLanguage(code)
    goTo('service')
  }

  const next = () => {
    const found = validateStep(step, answers, requiredCategories)
    if (Object.keys(found).length > 0) {
      setErrors(found)
      return
    }
    const index = STEPS.indexOf(step as (typeof STEPS)[number])
    if (index < STEPS.length - 1) goTo(STEPS[index + 1])
    else send()
  }

  const back = () => {
    const index = STEPS.indexOf(step as (typeof STEPS)[number])
    goTo(index <= 0 ? 'language' : STEPS[index - 1])
  }

  const finish = () => {
    clearDraft(token)
    if (!assisted) markSentFromThisPhone(token)
    setScreen('done')
  }

  const send = () =>
    submit.mutate(
      {
        language,
        serviceId: typeof answers.serviceId === 'number' ? answers.serviceId : null,
        ratings: answers.ratings,
        comment: answers.comment.trim() || null,
        problemReported: answers.problemReported,
        followUp: answers.wantsFollowUp
          ? {
              consent: answers.consent,
              name: answers.contactName.trim(),
              phone: answers.contactPhone.trim(),
              email: answers.contactEmail.trim(),
            }
          : null,
        assisted,
      },
      {
        onSuccess: finish,
        onError: (error) => {
          if (error instanceof ApiError && error.code === 'ALREADY_SUBMITTED') finish()
        },
      },
    )

  const startOver = () => {
    clearDraft(token)
    setAnswers(EMPTY_ANSWERS)
    setStep(assisted ? 'service' : 'language')
    setScreen('form')
    submit.reset()
  }

  const heading = (text: string) => (
    <Title order={3} ref={headingRef} tabIndex={-1} style={{ outline: 'none' }}>
      {text}
    </Title>
  )

  if (screen === 'already')
    return (
      <Stack gap="md">
        {heading(t('feedbackForm.already.title'))}
        <Text>{t('feedbackForm.already.body')}</Text>
        <Button size="lg" onClick={startOver}>
          {t('feedbackForm.already.again')}
        </Button>
      </Stack>
    )

  if (screen === 'done')
    return (
      <Stack gap="md" align="center" ta="center" py="lg">
        <ThemeIcon size={72} radius="xl" variant="light" color="teal">
          <IconCircleCheck size={44} />
        </ThemeIcon>
        {heading(t('feedbackForm.done.title'))}
        <Text>{t('feedbackForm.done.body')}</Text>
        {answers.problemReported && (
          <Text size="sm" c="dimmed">
            {t('feedbackForm.done.problemNote')}
          </Text>
        )}
        <Button size="lg" variant={assisted ? 'filled' : 'light'} onClick={startOver} mt="sm">
          {assisted ? t('feedbackForm.assisted.next') : t('feedbackForm.done.another')}
        </Button>
      </Stack>
    )

  if (step === 'language')
    return (
      <Stack gap="lg">
        {heading('Choose your language / अपनी भाषा चुनें')}
        <Stack gap="sm">
          <Button size="xl" variant="default" onClick={() => chooseLanguage('en')}>
            English
          </Button>
          <Button size="xl" variant="default" onClick={() => chooseLanguage('hi')} lang="hi">
            हिन्दी
          </Button>
        </Stack>
        <Alert color="teal" variant="light" title={t('feedbackForm.privacyTitle')}>
          {t('feedbackForm.privacy')}
        </Alert>
      </Stack>
    )

  const stepIndex = STEPS.indexOf(step)
  const isLast = stepIndex === STEPS.length - 1
  const sendError =
    submit.isError &&
    !(submit.error instanceof ApiError && submit.error.code === 'ALREADY_SUBMITTED')

  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault()
        next()
      }}
    >
      <Stack gap="lg">
        <Stack gap={6}>
          <Text size="sm" c="dimmed">
            {t('feedbackForm.stepOf', { step: stepIndex + 1, total: STEPS.length })}
          </Text>
          <Progress
            value={((stepIndex + 1) / STEPS.length) * 100}
            aria-label={t('feedbackForm.stepOf', { step: stepIndex + 1, total: STEPS.length })}
          />
        </Stack>

        {restored && stepIndex === 0 && (
          <Text size="sm" c="dimmed">
            {t('feedbackForm.draftRestored')}
          </Text>
        )}

        {step === 'service' && (
          <ServiceStep
            form={form}
            answers={answers}
            errors={errors}
            update={update}
            heading={heading}
          />
        )}
        {step === 'ratings' && (
          <Stack gap="lg">
            <Stack gap={4}>
              {heading(t('feedbackForm.ratings.title'))}
              <Text size="sm" c="dimmed">
                {t('feedbackForm.ratings.hint')}
              </Text>
            </Stack>
            {form.categories.map((category: RatingCategory) => (
              <EmojiRating
                key={category.code}
                label={language === 'hi' ? category.labelHi : category.labelEn}
                required={category.required}
                value={answers.ratings[category.code]}
                onChange={(score) =>
                  update({ ratings: { ...answers.ratings, [category.code]: score } })
                }
                error={errors[category.code] && t(errors[category.code]!)}
              />
            ))}
          </Stack>
        )}
        {step === 'comment' && (
          <Stack gap="md">
            {heading(t('feedbackForm.comment.title'))}
            <Textarea
              label={t('feedbackForm.comment.label')}
              description={t('feedbackForm.comment.hint')}
              placeholder={t('feedbackForm.comment.placeholder')}
              autosize
              minRows={4}
              maxLength={1000}
              size="md"
              value={answers.comment}
              onChange={(event) => update({ comment: event.currentTarget.value })}
              error={errors.comment && t(errors.comment)}
            />
            <Text size="xs" c="dimmed" ta="right" mt={-8}>
              {answers.comment.length} / 1000
            </Text>
            <Switch
              size="md"
              label={t('feedbackForm.problem.label')}
              description={t('feedbackForm.problem.hint')}
              checked={answers.problemReported}
              onChange={(event) => update({ problemReported: event.currentTarget.checked })}
            />
          </Stack>
        )}
        {step === 'followUp' && (
          <FollowUpStep answers={answers} errors={errors} update={update} heading={heading} />
        )}

        {sendError && (
          <Alert color="red" role="alert">
            {sendErrorMessage(t, submit.error)}
          </Alert>
        )}

        <Group grow gap="sm">
          <Button
            size="lg"
            variant="default"
            leftSection={<IconArrowLeft size={18} />}
            onClick={back}
            disabled={submit.isPending}
          >
            {t('feedbackForm.back')}
          </Button>
          <Button
            size="lg"
            type="submit"
            rightSection={isLast ? <IconSend size={18} /> : <IconArrowRight size={18} />}
            loading={submit.isPending}
          >
            {isLast ? t('feedbackForm.submit') : t('feedbackForm.next')}
          </Button>
        </Group>
      </Stack>
    </form>
  )
}

function sendErrorMessage(t: (key: string) => string, error: unknown) {
  if (error instanceof ApiError) {
    if (error.code === 'TOO_MANY_SUBMISSIONS') return t('feedbackForm.errors.TOO_MANY_SUBMISSIONS')
    if (error.code === 'FORM_CLOSED') return t('feedbackForm.errors.FORM_CLOSED')
    if (error.code === 'NETWORK_ERROR' || error.status >= 500)
      return t('feedbackForm.errors.network')
  }
  return t('feedbackForm.errors.other')
}

interface StepProps {
  answers: Answers
  errors: StepErrors
  update: (changes: Partial<Answers>) => void
  heading: (text: string) => React.ReactNode
}

function ServiceStep({ form, answers, errors, update, heading }: StepProps & { form: PublicForm }) {
  const { t } = useTranslation()
  const value = answers.serviceId === null ? null : String(answers.serviceId)
  return (
    <Radio.Group
      label={heading(t('feedbackForm.service.title'))}
      value={value}
      onChange={(choice) =>
        update({ serviceId: choice === 'general' ? 'general' : Number(choice) })
      }
      error={errors.service && t(errors.service)}
    >
      <Stack gap="sm" mt="md">
        {form.services.map((service) => (
          <Radio.Card key={service.id} value={String(service.id)} p="md" radius="md">
            <Group wrap="nowrap" align="flex-start">
              <Radio.Indicator />
              <Stack gap={0}>
                <Text fw={600}>{service.name}</Text>
                <Text size="sm" c="dimmed">
                  {service.departmentName}
                </Text>
              </Stack>
            </Group>
          </Radio.Card>
        ))}
        <Radio.Card value="general" p="md" radius="md">
          <Group wrap="nowrap">
            <Radio.Indicator />
            <Text fw={600}>{t('feedbackForm.service.general')}</Text>
          </Group>
        </Radio.Card>
      </Stack>
    </Radio.Group>
  )
}

function FollowUpStep({ answers, errors, update, heading }: StepProps) {
  const { t } = useTranslation()
  return (
    <Stack gap="md">
      {heading(t('feedbackForm.followUp.title'))}
      <SegmentedControl
        fullWidth
        size="md"
        orientation="vertical"
        aria-label={t('feedbackForm.followUp.title')}
        value={answers.wantsFollowUp ? 'yes' : 'no'}
        onChange={(choice) => update({ wantsFollowUp: choice === 'yes' })}
        data={[
          { value: 'no', label: t('feedbackForm.followUp.no') },
          { value: 'yes', label: t('feedbackForm.followUp.yes') },
        ]}
      />
      {answers.wantsFollowUp ? (
        <Stack gap="sm">
          <TextInput
            size="md"
            label={t('feedbackForm.followUp.name')}
            autoComplete="name"
            maxLength={100}
            value={answers.contactName}
            onChange={(event) => update({ contactName: event.currentTarget.value })}
          />
          <TextInput
            size="md"
            type="tel"
            inputMode="tel"
            autoComplete="tel"
            label={t('feedbackForm.followUp.phone')}
            maxLength={20}
            value={answers.contactPhone}
            onChange={(event) => update({ contactPhone: event.currentTarget.value })}
            error={errors.contactPhone && t(errors.contactPhone)}
          />
          <TextInput
            size="md"
            type="email"
            autoComplete="email"
            label={t('feedbackForm.followUp.email')}
            maxLength={255}
            value={answers.contactEmail}
            onChange={(event) => update({ contactEmail: event.currentTarget.value })}
            error={errors.contactEmail && t(errors.contactEmail)}
          />
          {errors.contact && (
            <Text c="red" size="sm" role="alert">
              {t(errors.contact)}
            </Text>
          )}
          <Checkbox
            size="md"
            label={t('feedbackForm.followUp.consent')}
            checked={answers.consent}
            onChange={(event) => update({ consent: event.currentTarget.checked })}
            error={errors.consent && t(errors.consent)}
          />
        </Stack>
      ) : (
        <Text size="sm" c="dimmed">
          {t('feedbackForm.followUp.anonymousNote')}
        </Text>
      )}
    </Stack>
  )
}
