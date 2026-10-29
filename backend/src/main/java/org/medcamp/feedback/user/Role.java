package org.medcamp.feedback.user;

public enum Role {

	SUPER_ADMIN,

	ORGANIZER,

	/** Linked to one department and sees only that department's data. */
	MEDICAL_STAFF,

	/** Sees aggregated, anonymized data only. */
	ANALYST;

	public String authority() {
		return "ROLE_" + name();
	}

}
