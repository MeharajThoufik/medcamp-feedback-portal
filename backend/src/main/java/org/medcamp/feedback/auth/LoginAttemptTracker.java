package org.medcamp.feedback.auth;

import java.time.Clock;

import org.medcamp.feedback.common.Emails;
import org.medcamp.feedback.config.AppProperties;
import org.medcamp.feedback.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Locks an account for a while after too many wrong passwords. Attempts on a locked account are
 * rejected before the password is checked, so they do not extend the lock.
 */
@Component
public class LoginAttemptTracker {

	private static final Logger log = LoggerFactory.getLogger(LoginAttemptTracker.class);

	private final UserRepository users;
	private final AppProperties.Security settings;
	private final Clock clock;

	public LoginAttemptTracker(UserRepository users, AppProperties properties, Clock clock) {
		this.users = users;
		this.settings = properties.security();
		this.clock = clock;
	}

	@EventListener
	@Transactional
	public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
		try {
			users.findByEmail(Emails.normalize(event.getAuthentication().getName()))
				.ifPresent(user -> user.registerFailedLogin(clock.instant(), settings.maxFailedLogins(),
						settings.lockoutDuration()));
		}
		catch (OptimisticLockingFailureException ex) {
			log.warn("Could not record a failed sign-in because the user was being updated", ex);
		}
	}

	@EventListener
	@Transactional
	public void onSuccess(AuthenticationSuccessEvent event) {
		if (event.getAuthentication().getPrincipal() instanceof AppUserDetails details) {
			users.findById(details.getId()).ifPresent(user -> user.registerSuccessfulLogin(clock.instant()));
		}
	}

}
