package org.medcamp.feedback.config;

import org.medcamp.feedback.auth.AuthResponseHandlers;
import org.medcamp.feedback.auth.CurrentUserRefreshFilter;
import org.medcamp.feedback.common.ProblemResponseWriter;
import org.medcamp.feedback.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Session-cookie authentication for the React app served from the same origin.
 * <ul>
 * <li>{@code /api/public/**}: open to attendees (feedback form, complaint tracking).</li>
 * <li>{@code /api/**}: staff only; finer role checks use {@code @PreAuthorize} on controllers.</li>
 * <li>Everything else: the React app and its files.</li>
 * </ul>
 * CSRF uses the SPA setup: the token is sent in the {@code XSRF-TOKEN} cookie and must come back
 * in the {@code X-XSRF-TOKEN} header.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthResponseHandlers handlers, UserRepository users,
			ProblemResponseWriter writer) throws Exception {
		http.csrf(csrf -> csrf.spa())
			.authorizeHttpRequests(auth -> auth.requestMatchers("/api/public/**")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/login")
				.permitAll()
				.requestMatchers("/api/**")
				.authenticated()
				.anyRequest()
				.permitAll())
			.formLogin(form -> form.loginPage("/login")
				.loginProcessingUrl("/api/auth/login")
				.usernameParameter("email")
				.passwordParameter("password")
				.successHandler(handlers::onLoginSuccess)
				.failureHandler(handlers::onLoginFailure))
			.logout(logout -> logout.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(handlers::onUnauthenticated)
				.accessDeniedHandler(handlers::onAccessDenied))
			.httpBasic(AbstractHttpConfigurer::disable)
			// No "replay the request after sign-in", so anonymous calls never create a session.
			.requestCache(AbstractHttpConfigurer::disable)
			.addFilterBefore(new CurrentUserRefreshFilter(users, writer), AuthorizationFilter.class);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/** Publishes sign-in success/failure events, which drive account lockout. */
	@Bean
	AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
		return new DefaultAuthenticationEventPublisher(publisher);
	}

}
