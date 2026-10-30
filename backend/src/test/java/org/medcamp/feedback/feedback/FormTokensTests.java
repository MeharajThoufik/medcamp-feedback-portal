package org.medcamp.feedback.feedback;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.feedback.FormTokens.Check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormTokensTests {

	private static final Instant NOW = Instant.parse("2026-11-02T04:30:00Z");

	@Test
	void aTokenIsValidForItsOwnCampUntilItExpires() {
		String token = tokens(NOW).issue("campA123");

		// After a restart the key is new, so older tokens no longer check out.
		assertThat(tokens(NOW).check(token, "campA123")).isEqualTo(Check.INVALID);
		FormTokens same = tokens(NOW);
		String fresh = same.issue("campA123");
		assertThat(same.check(fresh, "campA123")).isEqualTo(Check.VALID);
		assertThat(same.check(fresh, "campB456")).isEqualTo(Check.INVALID);
		assertThat(same.check(fresh.replace('.', ':'), "campA123")).isEqualTo(Check.INVALID);
		assertThat(same.check(null, "campA123")).isEqualTo(Check.INVALID);
	}

	@Test
	void tokensExpireAfterTwelveHours() {
		MutableClock clock = new MutableClock(NOW);
		FormTokens tokens = new FormTokens(clock);
		String token = tokens.issue("campA123");

		clock.now = NOW.plus(FormTokens.MAX_AGE);
		assertThat(tokens.check(token, "campA123")).isEqualTo(Check.VALID);
		clock.now = NOW.plus(FormTokens.MAX_AGE).plus(Duration.ofSeconds(1));
		assertThat(tokens.check(token, "campA123")).isEqualTo(Check.EXPIRED);
	}

	@Test
	void phoneNumbersAreCleanedUp() {
		assertThat(PublicFeedbackService.normalizePhone(" +91 98765-43210 ")).isEqualTo("+919876543210");
		assertThat(PublicFeedbackService.normalizePhone("(044) 2345 6789")).isEqualTo("04423456789");
		assertThat(PublicFeedbackService.normalizePhone("  ")).isNull();
		assertThatThrownBy(() -> PublicFeedbackService.normalizePhone("12345")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> PublicFeedbackService.normalizePhone("call me")).isInstanceOf(ApiException.class);
	}

	private static FormTokens tokens(Instant now) {
		return new FormTokens(Clock.fixed(now, ZoneOffset.UTC));
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

	}

}
