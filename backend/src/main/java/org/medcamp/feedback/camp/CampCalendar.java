package org.medcamp.feedback.camp;

import java.time.Clock;
import java.time.LocalDate;

import org.medcamp.feedback.config.AppProperties;
import org.springframework.stereotype.Component;

/** Camp dates are calendar days where the camps run ({@code app.time-zone}), not in UTC. */
@Component
public class CampCalendar {

	private final Clock clock;

	public CampCalendar(Clock clock, AppProperties properties) {
		this.clock = clock.withZone(properties.timeZone());
	}

	public LocalDate today() {
		return LocalDate.now(clock);
	}

}
