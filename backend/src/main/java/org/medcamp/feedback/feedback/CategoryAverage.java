package org.medcamp.feedback.feedback;

/** Average score and number of answers for one rating category. */
public record CategoryAverage(String code, Double average, Long count) {
}
