package org.medcamp.feedback.camp;

import java.util.Set;

/**
 * Set by the organizer. The allowed moves are:
 * <pre>
 * UPCOMING → ACTIVE or ARCHIVED (cancelled)
 * ACTIVE   → CLOSED
 * CLOSED   → ACTIVE (reopen) or ARCHIVED
 * ARCHIVED → CLOSED (restore)
 * </pre>
 */
public enum CampStatus {

	UPCOMING, ACTIVE, CLOSED, ARCHIVED;

	public Set<CampStatus> nextStatuses() {
		return switch (this) {
			case UPCOMING -> Set.of(ACTIVE, ARCHIVED);
			case ACTIVE -> Set.of(CLOSED);
			case CLOSED -> Set.of(ACTIVE, ARCHIVED);
			case ARCHIVED -> Set.of(CLOSED);
		};
	}

	public boolean canMoveTo(CampStatus target) {
		return nextStatuses().contains(target);
	}

}
