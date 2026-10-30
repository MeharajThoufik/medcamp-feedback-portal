package org.medcamp.feedback.feedback;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * One-time submission tokens, handed out when a form is opened and spent when it is submitted.
 * A token is signed for one camp link, so it cannot be made up or used for another camp, and it
 * expires after {@link #MAX_AGE}. "Used once" is enforced by the unique column on feedback.
 * <p>
 * The signing key is created at startup. After a restart, open forms simply fetch a new token
 * when they submit.
 */
@Component
public class FormTokens {

	static final Duration MAX_AGE = Duration.ofHours(12);
	private static final Duration CLOCK_SKEW = Duration.ofMinutes(5);
	private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();

	public enum Check {

		VALID, EXPIRED, INVALID

	}

	private final SecureRandom random = new SecureRandom();
	private final byte[] key = new byte[32];
	private final Clock clock;

	public FormTokens(Clock clock) {
		this.clock = clock;
		random.nextBytes(key);
	}

	/** Format: {@code <issued epoch seconds>.<random>.<signature>}. */
	public String issue(String campPublicToken) {
		byte[] nonce = new byte[12];
		random.nextBytes(nonce);
		String payload = clock.instant().getEpochSecond() + "." + BASE64.encodeToString(nonce);
		return payload + "." + sign(campPublicToken, payload);
	}

	public Check check(String token, String campPublicToken) {
		String[] parts = token == null ? new String[0] : token.split("[.]");
		if (parts.length != 3) {
			return Check.INVALID;
		}
		String payload = parts[0] + "." + parts[1];
		byte[] expected = sign(campPublicToken, payload).getBytes(StandardCharsets.US_ASCII);
		if (!MessageDigest.isEqual(expected, parts[2].getBytes(StandardCharsets.US_ASCII))) {
			return Check.INVALID;
		}
		long issued;
		try {
			issued = Long.parseLong(parts[0]);
		}
		catch (NumberFormatException ex) {
			return Check.INVALID;
		}
		long now = clock.instant().getEpochSecond();
		if (issued > now + CLOCK_SKEW.toSeconds() || now - issued > MAX_AGE.toSeconds()) {
			return Check.EXPIRED;
		}
		return Check.VALID;
	}

	private String sign(String campPublicToken, String payload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return BASE64.encodeToString(mac.doFinal((campPublicToken + "|" + payload).getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("HmacSHA256 is not available", ex);
		}
	}

}
