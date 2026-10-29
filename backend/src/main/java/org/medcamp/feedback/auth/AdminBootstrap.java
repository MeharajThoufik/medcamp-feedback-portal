package org.medcamp.feedback.auth;

import org.medcamp.feedback.common.Emails;
import org.medcamp.feedback.config.AppProperties;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;
import org.medcamp.feedback.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first Super Admin when the database has no users yet. */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AppProperties.Admin admin;

	public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder, AppProperties properties) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.admin = properties.bootstrap().admin();
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (users.count() > 0) {
			return;
		}
		if (admin.password() == null || admin.password().isBlank()) {
			throw new IllegalStateException(
					"No users exist yet. Set the APP_ADMIN_PASSWORD environment variable to create the first Super Admin.");
		}
		User user = new User(Emails.normalize(admin.email()), admin.fullName(), passwordEncoder.encode(admin.password()),
				Role.SUPER_ADMIN, null);
		user.requirePasswordChange();
		users.save(user);
		log.info("Created the first Super Admin '{}'. The password must be changed at first sign-in.", user.getEmail());
	}

}
