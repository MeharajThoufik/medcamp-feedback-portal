package org.medcamp.feedback.camp;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Generates the random part of a camp's public link, such as {@code /f/7Kq2xPm4}. It avoids
 * look-alike characters (0/O, 1/l/I) so the link can also be typed from a poster.
 */
@Component
public class PublicTokenGenerator {

	private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int LENGTH = 8;

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		char[] token = new char[LENGTH];
		for (int i = 0; i < LENGTH; i++) {
			token[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
		}
		return new String(token);
	}

}
