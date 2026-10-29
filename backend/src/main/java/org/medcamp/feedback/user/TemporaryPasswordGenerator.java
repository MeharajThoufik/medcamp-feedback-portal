package org.medcamp.feedback.user;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/** Generates readable one-time passwords (no 0/O or 1/l/I) that meet the password rules. */
@Component
public class TemporaryPasswordGenerator {

	private static final String LETTERS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
	private static final String DIGITS = "23456789";
	private static final String ALL = LETTERS + DIGITS;
	private static final int LENGTH = 12;

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		char[] password = new char[LENGTH];
		password[0] = LETTERS.charAt(random.nextInt(LETTERS.length()));
		password[1] = DIGITS.charAt(random.nextInt(DIGITS.length()));
		for (int i = 2; i < LENGTH; i++) {
			password[i] = ALL.charAt(random.nextInt(ALL.length()));
		}
		// Shuffle so the guaranteed letter and digit are not always first.
		for (int i = LENGTH - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			char swap = password[i];
			password[i] = password[j];
			password[j] = swap;
		}
		return new String(password);
	}

}
