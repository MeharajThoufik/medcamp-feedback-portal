package org.medcamp.feedback.camp;

/** Whether attendees can submit feedback for a camp today, and if not, why. */
public enum FeedbackState {

	OPEN,

	/** The camp is not Active (Upcoming, Closed or Archived). */
	CAMP_NOT_ACTIVE,

	/** The camp is Active but the feedback window has not started yet. */
	NOT_STARTED,

	/** The camp is Active but the feedback window has passed. */
	ENDED

}
