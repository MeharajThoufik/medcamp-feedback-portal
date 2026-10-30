package org.medcamp.feedback.config;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param timeZone where the camps run; decides what "today" means for feedback windows
 */
@ConfigurationProperties("app")
public record AppProperties(String publicBaseUrl, @DefaultValue("Asia/Kolkata") ZoneId timeZone, Bootstrap bootstrap,
		Security security) {

	public record Bootstrap(Admin admin) {
	}

	public record Admin(String email, String fullName, String password) {
	}

	public record Security(@DefaultValue("5") int maxFailedLogins, @DefaultValue("15m") Duration lockoutDuration) {
	}

}
