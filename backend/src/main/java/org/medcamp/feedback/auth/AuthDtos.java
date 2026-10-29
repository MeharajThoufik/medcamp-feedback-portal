package org.medcamp.feedback.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.medcamp.feedback.user.Role;
import org.medcamp.feedback.user.User;

public final class AuthDtos {

	static final int PASSWORD_MIN_LENGTH = 8;

	/** BCrypt only uses the first 72 bytes of a password. */
	static final int PASSWORD_MAX_BYTES = 72;

	private AuthDtos() {
	}

	public record MeResponse(Long id, String email, String fullName, Role role, Long departmentId,
			String departmentName, boolean mustChangePassword) {

		static MeResponse from(User user) {
			return new MeResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
					user.getDepartmentId(), user.getDepartment() == null ? null : user.getDepartment().getName(),
					user.isMustChangePassword());
		}

	}

	public record ChangePasswordRequest(@NotBlank String currentPassword,
			@NotBlank @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_BYTES,
					message = "Password must be 8 to 72 characters long.") @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
							message = "Password must contain at least one letter and one number.") String newPassword) {
	}

}
