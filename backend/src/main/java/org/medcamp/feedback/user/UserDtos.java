package org.medcamp.feedback.user;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserDtos {

	private UserDtos() {
	}

	/** Used for both create and update. {@code departmentId} is required only for MEDICAL_STAFF. */
	public record UserRequest(@NotBlank @Email @Size(max = 255) String email,
			@NotBlank @Size(max = 150) String fullName, @NotNull Role role, Long departmentId) {
	}

	public record SetEnabledRequest(@NotNull Boolean enabled) {
	}

	public record UserResponse(Long id, String email, String fullName, Role role, Long departmentId,
			String departmentName, boolean enabled, boolean locked, boolean mustChangePassword, Instant lastLoginAt,
			Instant createdAt) {

		static UserResponse from(User user, Instant now) {
			return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
					user.getDepartmentId(), user.getDepartment() == null ? null : user.getDepartment().getName(),
					user.isEnabled(), user.isLocked(now), user.isMustChangePassword(), user.getLastLoginAt(),
					user.getCreatedAt());
		}

	}

	/** The temporary password is returned once and never stored in plain text. */
	public record CreatedUserResponse(UserResponse user, String temporaryPassword) {
	}

	public record TemporaryPasswordResponse(String temporaryPassword) {
	}

	/** An organizer a Super Admin can assign to a camp. */
	public record OrganizerOption(Long id, String fullName, String email) {

		static OrganizerOption from(User user) {
			return new OrganizerOption(user.getId(), user.getFullName(), user.getEmail());
		}

	}

}
