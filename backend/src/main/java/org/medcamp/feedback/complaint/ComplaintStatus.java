package org.medcamp.feedback.complaint;

/** Every complaint starts {@link #OPEN}; Phase 6 adds the rules for moving between statuses. */
public enum ComplaintStatus {

	OPEN, IN_PROGRESS, RESOLVED, CLOSED

}
