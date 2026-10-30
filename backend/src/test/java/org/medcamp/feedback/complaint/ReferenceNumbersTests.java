package org.medcamp.feedback.complaint;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.medcamp.feedback.camp.CampCalendar;
import org.medcamp.feedback.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceNumbersTests {

	@Test
	void referencesHaveTheYearAndSixRandomCharactersThatDoNotLookAlike() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-02T04:30:00Z"), ZoneOffset.UTC);
		AppProperties properties = new AppProperties("http://localhost:8080", ZoneId.of("Asia/Kolkata"),
				null, null);
		ReferenceNumbers numbers = new ReferenceNumbers(new CampCalendar(clock, properties));

		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			String reference = numbers.generate();
			assertThat(reference).matches("MC-26-[A-HJKMNP-Z2-9]{6}");
			seen.add(reference);
		}
		assertThat(seen).hasSizeGreaterThan(990);
	}

	@Test
	void typedReferencesAreTidiedUp() {
		assertThat(ReferenceNumbers.normalize("MC-26-K7Q2XP")).contains("MC-26-K7Q2XP");
		assertThat(ReferenceNumbers.normalize(" mc 26 k7q2xp ")).contains("MC-26-K7Q2XP");
		assertThat(ReferenceNumbers.normalize("mc26k7q2xp")).contains("MC-26-K7Q2XP");
	}

	@Test
	void thingsThatCannotBeReferencesAreRejected() {
		assertThat(ReferenceNumbers.normalize(null)).isEqualTo(Optional.empty());
		assertThat(ReferenceNumbers.normalize("MC-26-K7Q2X")).isEmpty();
		assertThat(ReferenceNumbers.normalize("MC-26-K7Q2X0")).as("0 is never used").isEmpty();
		assertThat(ReferenceNumbers.normalize("XX-26-K7Q2XP")).isEmpty();
		assertThat(ReferenceNumbers.normalize("MC-26-K7Q2XP-1")).isEmpty();
	}

}
