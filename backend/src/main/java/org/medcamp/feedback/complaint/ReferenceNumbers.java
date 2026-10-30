package org.medcamp.feedback.complaint;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

import org.medcamp.feedback.camp.CampCalendar;
import org.springframework.stereotype.Component;

/**
 * Complaint references such as {@code MC-26-K7Q2XP}: the year, then 6 random characters. The
 * random part (about 890 million combinations) stops anyone from guessing other people's
 * references, since a reference is the only key to the public status page. Letters and digits
 * that look alike (I/1/L, O/0) are left out, so it can be read aloud or copied from a screenshot.
 */
@Component
public class ReferenceNumbers {

	static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
	private static final int RANDOM_LENGTH = 6;

	private final SecureRandom random = new SecureRandom();
	private final CampCalendar calendar;

	public ReferenceNumbers(CampCalendar calendar) {
		this.calendar = calendar;
	}

	public String generate() {
		StringBuilder reference = new StringBuilder("MC-");
		reference.append(String.format("%02d", calendar.today().getYear() % 100)).append('-');
		for (int i = 0; i < RANDOM_LENGTH; i++) {
			reference.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return reference.toString();
	}

	/**
	 * Accepts what people actually type, such as {@code mc26k7q2xp} or {@code MC 26 K7Q2XP}, and
	 * returns the stored form, or empty when it cannot be a reference.
	 */
	public static Optional<String> normalize(String input) {
		if (input == null) {
			return Optional.empty();
		}
		String compact = input.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
		if (!compact.matches("MC[0-9]{2}[" + ALPHABET + "]{" + RANDOM_LENGTH + "}")) {
			return Optional.empty();
		}
		return Optional.of("MC-" + compact.substring(2, 4) + "-" + compact.substring(4));
	}

}
