package org.medcamp.feedback.user;

import jakarta.validation.Valid;
import org.medcamp.feedback.auth.AppUserDetails;
import org.medcamp.feedback.common.PageResponse;
import org.medcamp.feedback.user.UserDtos.CreatedUserResponse;
import org.medcamp.feedback.user.UserDtos.SetEnabledRequest;
import org.medcamp.feedback.user.UserDtos.TemporaryPasswordResponse;
import org.medcamp.feedback.user.UserDtos.UserRequest;
import org.medcamp.feedback.user.UserDtos.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public PageResponse<UserResponse> list(@RequestParam(required = false) String search,
			@RequestParam(required = false) Role role, @RequestParam(required = false) Boolean enabled,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return userService.list(search, role, enabled, page, size);
	}

	@GetMapping("/{id}")
	public UserResponse get(@PathVariable long id) {
		return userService.get(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreatedUserResponse create(@Valid @RequestBody UserRequest request) {
		return userService.create(request);
	}

	@PutMapping("/{id}")
	public UserResponse update(@PathVariable long id, @Valid @RequestBody UserRequest request,
			@AuthenticationPrincipal AppUserDetails currentUser) {
		return userService.update(id, request, currentUser.getId());
	}

	@PutMapping("/{id}/enabled")
	public UserResponse setEnabled(@PathVariable long id, @Valid @RequestBody SetEnabledRequest request,
			@AuthenticationPrincipal AppUserDetails currentUser) {
		return userService.setEnabled(id, request.enabled(), currentUser.getId());
	}

	@PostMapping("/{id}/reset-password")
	public TemporaryPasswordResponse resetPassword(@PathVariable long id) {
		return userService.resetPassword(id);
	}

}
