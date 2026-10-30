package org.medcamp.feedback.qr;

import org.medcamp.feedback.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Builds the address attendees open, such as {@code http://192.168.137.1:8080/f/7Kq2xPm4}. The
 * base comes from {@code app.public-base-url}, because the server cannot tell which of the
 * laptop's addresses phones can reach.
 */
@Component
public class FeedbackLinks {

	private final String baseUrl;

	public FeedbackLinks(AppProperties properties) {
		this.baseUrl = properties.publicBaseUrl().replaceAll("/+$", "");
	}

	public String forToken(String publicToken) {
		return baseUrl + "/f/" + publicToken;
	}

	public String baseUrl() {
		return baseUrl;
	}

}
