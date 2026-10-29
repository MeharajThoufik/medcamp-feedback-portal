package org.medcamp.feedback.auth;

import jakarta.validation.Valid;
import org.medcamp.feedback.auth.AuthDtos.ChangePasswordRequest;
import org.medcamp.feedback.auth.AuthDtos.MeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-in ({@code POST /api/auth/login}) and sign-out ({@code POST /api/auth/logout}) are handled
 * by Spring Security; see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AppUserDetails currentUser) {
		return authService.me(currentUser.getId());
	}

	@PostMapping("/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal AppUserDetails currentUser,
			@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(currentUser.getId(), request.currentPassword(), request.newPassword());
	}

}
