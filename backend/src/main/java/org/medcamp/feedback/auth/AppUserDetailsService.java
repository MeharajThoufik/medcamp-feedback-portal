package org.medcamp.feedback.auth;

import java.time.Clock;

import org.medcamp.feedback.common.Emails;
import org.medcamp.feedback.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository users;
	private final Clock clock;

	public AppUserDetailsService(UserRepository users, Clock clock) {
		this.users = users;
		this.clock = clock;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		return users.findByEmail(Emails.normalize(email))
			.map(user -> AppUserDetails.forAuthentication(user, user.isLocked(clock.instant())))
			.orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
	}

}
