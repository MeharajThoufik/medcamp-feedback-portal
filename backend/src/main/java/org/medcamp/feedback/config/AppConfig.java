package org.medcamp.feedback.config;

import java.time.Clock;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Medical Camp Feedback Portal API", version = "v1"))
public class AppConfig {

	/** Injected wherever "now" matters, so tests can control time. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
