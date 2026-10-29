package org.medcamp.feedback.auth;

import java.nio.charset.StandardCharsets;

import org.medcamp.feedback.auth.AuthDtos.MeResponse;
import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.user.User;
import org.medcamp.feedback.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public MeResponse me(long userId) {
		return MeResponse.from(getUser(userId));
	}

	/** The session picks up the cleared "must change password" flag on the next request. */
	public void changePassword(long userId, String currentPassword, String newPassword) {
		User user = getUser(userId);
		if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw ApiException.badRequest("WRONG_CURRENT_PASSWORD", "The current password is incorrect.");
		}
		if (newPassword.getBytes(StandardCharsets.UTF_8).length > AuthDtos.PASSWORD_MAX_BYTES) {
			throw ApiException.badRequest("PASSWORD_TOO_LONG", "The new password is too long.");
		}
		if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
			throw ApiException.badRequest("PASSWORD_UNCHANGED",
					"The new password must be different from the current one.");
		}
		user.changePassword(passwordEncoder.encode(newPassword), false);
	}

	private User getUser(long userId) {
		return users.findWithDepartmentById(userId)
			.orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
	}

}
