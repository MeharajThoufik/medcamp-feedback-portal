package org.medcamp.feedback.user;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import org.medcamp.feedback.common.ApiException;
import org.medcamp.feedback.common.Emails;
import org.medcamp.feedback.common.PageResponse;
import org.medcamp.feedback.department.Department;
import org.medcamp.feedback.department.DepartmentService;
import org.medcamp.feedback.user.UserDtos.CreatedUserResponse;
import org.medcamp.feedback.user.UserDtos.TemporaryPasswordResponse;
import org.medcamp.feedback.user.UserDtos.UserRequest;
import org.medcamp.feedback.user.UserDtos.UserResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User management for Super Admins. */
@Service
@Transactional
public class UserService {

	private static final int MAX_PAGE_SIZE = 100;

	private final UserRepository users;
	private final DepartmentService departmentService;
	private final PasswordEncoder passwordEncoder;
	private final TemporaryPasswordGenerator passwordGenerator;
	private final Clock clock;

	public UserService(UserRepository users, DepartmentService departmentService, PasswordEncoder passwordEncoder,
			TemporaryPasswordGenerator passwordGenerator, Clock clock) {
		this.users = users;
		this.departmentService = departmentService;
		this.passwordEncoder = passwordEncoder;
		this.passwordGenerator = passwordGenerator;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PageResponse<UserResponse> list(String search, Role role, Boolean enabled, int page, int size) {
		PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by("fullName").and(Sort.by("id")));
		return PageResponse.of(users.findAll(filter(search, role, enabled), pageRequest).map(this::toResponse));
	}

	@Transactional(readOnly = true)
	public UserResponse get(long id) {
		return toResponse(getEntity(id));
	}

	public CreatedUserResponse create(UserRequest request) {
		String email = Emails.normalize(request.email());
		if (users.existsByEmail(email)) {
			throw emailTaken();
		}
		String temporaryPassword = passwordGenerator.generate();
		User user = new User(email, request.fullName().trim(), passwordEncoder.encode(temporaryPassword),
				request.role(), resolveDepartment(request.role(), request.departmentId(), null));
		user.requirePasswordChange();
		return new CreatedUserResponse(toResponse(users.save(user)), temporaryPassword);
	}

	public UserResponse update(long id, UserRequest request, long actingUserId) {
		User user = getEntity(id);
		if (id == actingUserId && request.role() != user.getRole()) {
			throw ApiException.badRequest("CANNOT_CHANGE_OWN_ROLE", "You cannot change your own role.");
		}
		String email = Emails.normalize(request.email());
		if (users.existsByEmailAndIdNot(email, id)) {
			throw emailTaken();
		}
		user.setEmail(email);
		user.setFullName(request.fullName().trim());
		user.setDepartment(resolveDepartment(request.role(), request.departmentId(), user.getDepartment()));
		user.setRole(request.role());
		return toResponse(user);
	}

	public UserResponse setEnabled(long id, boolean enabled, long actingUserId) {
		if (id == actingUserId && !enabled) {
			throw ApiException.badRequest("CANNOT_DISABLE_SELF", "You cannot disable your own account.");
		}
		User user = getEntity(id);
		user.setEnabled(enabled);
		return toResponse(user);
	}

	/** Issues a new one-time password, which also unlocks the account. */
	public TemporaryPasswordResponse resetPassword(long id) {
		User user = getEntity(id);
		String temporaryPassword = passwordGenerator.generate();
		user.changePassword(passwordEncoder.encode(temporaryPassword), true);
		return new TemporaryPasswordResponse(temporaryPassword);
	}

	private User getEntity(long id) {
		return users.findWithDepartmentById(id)
			.orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found."));
	}

	/**
	 * Medical staff must belong to a department; other roles never do. A newly chosen department must
	 * be active, but a user may keep a department that was deactivated later.
	 */
	private Department resolveDepartment(Role role, Long departmentId, Department current) {
		if (role != Role.MEDICAL_STAFF) {
			return null;
		}
		if (departmentId == null) {
			throw ApiException.badRequest("DEPARTMENT_REQUIRED", "Medical staff must be linked to a department.");
		}
		if (current != null && departmentId.equals(current.getId())) {
			return current;
		}
		Department department = departmentService.getEntity(departmentId);
		if (!department.isActive()) {
			throw ApiException.badRequest("DEPARTMENT_INACTIVE", "This department is inactive.");
		}
		return department;
	}

	private UserResponse toResponse(User user) {
		return UserResponse.from(user, clock.instant());
	}

	private static Specification<User> filter(String search, Role role, Boolean enabled) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (search != null && !search.isBlank()) {
				String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
						cb.like(cb.lower(root.get("email")), pattern, '\\')));
			}
			if (role != null) {
				predicates.add(cb.equal(root.get("role"), role));
			}
			if (enabled != null) {
				predicates.add(cb.equal(root.get("enabled"), enabled));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static ApiException emailTaken() {
		return ApiException.conflict("EMAIL_TAKEN", "A user with this email already exists.");
	}

}
