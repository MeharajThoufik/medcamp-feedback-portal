package org.medcamp.feedback.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app")
public record AppProperties(String publicBaseUrl, Bootstrap bootstrap, Security security) {

	public record Bootstrap(Admin admin) {
	}

	public record Admin(String email, String fullName, String password) {
	}

	public record Security(@DefaultValue("5") int maxFailedLogins, @DefaultValue("15m") Duration lockoutDuration) {
	}

}
