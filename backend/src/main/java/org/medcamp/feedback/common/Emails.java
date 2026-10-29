package org.medcamp.feedback.common;

import java.util.Locale;

public final class Emails {

	private Emails() {
	}

	/** Emails are stored lower-case so uniqueness and sign-in are case-insensitive. */
	public static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

}
