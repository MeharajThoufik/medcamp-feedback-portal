import { Text } from '@mantine/core'
import { useId } from 'react'
import { useTranslation } from 'react-i18next'
import classes from './EmojiRating.module.css'
import { SCORE_EMOJI, SCORES } from './ratingScale'

/**
 * A 1–5 rating with large emoji faces. Built on real radio buttons, so arrow keys, screen readers
 * and form labels work without extra code.
 */
export function EmojiRating({
  label,
  required,
  value,
  onChange,
  error,
}: {
  label: string
  required?: boolean
  value: number | undefined
  onChange: (score: number) => void
  error?: string
}) {
  const { t } = useTranslation()
  const name = useId()
  const errorId = `${name}-error`
  return (
    <fieldset
      className={classes.fieldset}
      aria-invalid={error ? true : undefined}
      aria-describedby={error ? errorId : undefined}
    >
      <legend className={classes.legend}>
        {label}
        {required && (
          <Text span c="red" ml={4} aria-hidden>
            *
          </Text>
        )}
      </legend>
      <div className={classes.options}>
        {SCORES.map((score) => (
          <label key={score} className={classes.option}>
            <input
              type="radio"
              className={classes.input}
              name={name}
              value={score}
              checked={value === score}
              onChange={() => onChange(score)}
            />
            <span className={classes.emoji} aria-hidden>
              {SCORE_EMOJI[score]}
            </span>
            <span className={classes.caption}>{t(`feedbackForm.scale.${score}`)}</span>
          </label>
        ))}
      </div>
      {error && (
        <Text id={errorId} c="red" size="sm" mt={6}>
          {error}
        </Text>
      )}
    </fieldset>
  )
}
